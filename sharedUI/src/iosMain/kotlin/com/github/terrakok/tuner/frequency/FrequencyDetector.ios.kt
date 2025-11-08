package com.github.terrakok.tuner.frequency

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVarOf
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.ULongVarOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioNodeBus
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.setActive
import platform.Accelerate.DSPComplex
import platform.Accelerate.DSPSplitComplex
import platform.Accelerate.FFT_FORWARD
import platform.Accelerate.FFT_INVERSE
import platform.Accelerate.FFT_RADIX2
import platform.Accelerate.vDSP_Length
import platform.Accelerate.vDSP_create_fftsetup
import platform.Accelerate.vDSP_ctoz
import platform.Accelerate.vDSP_destroy_fftsetup
import platform.Accelerate.vDSP_fft_zrip
import platform.Accelerate.vDSP_maxvi
import platform.Accelerate.vDSP_vsmul
import platform.Accelerate.vDSP_ztoc
import platform.Accelerate.vDSP_zvmags
import platform.Foundation.NSError
import platform.posix.log2f

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun getMicFrequency(): Flow<Float> = callbackFlow {
    memScoped {
        val session = AVAudioSession.sharedInstance()
        val engine = AVAudioEngine()
        val error: ObjCObjectVar<NSError?> = alloc()

        if (!session.setCategory(AVAudioSessionCategoryPlayAndRecord, error.ptr)) {
            println("setCategory ERROR: " + error.value?.localizedDescription)
        }
        if (!session.setActive(true, error.ptr)) {
            println("setActive ERROR: " + error.value?.localizedDescription)
        }

        val inputNode = engine.inputNode
        val inputFormat = inputNode.inputFormatForBus(AVAudioNodeBus.MIN_VALUE)

        inputNode.installTapOnBus(
            AVAudioNodeBus.MIN_VALUE,
            1024u,
            inputFormat
        ) { buffer, _ -> processAudioBuffer(buffer) }
        if (!engine.startAndReturnError(error.ptr)) {
            println("startAndReturnError ERROR: " + error.value?.localizedDescription)
        }

        awaitClose {
            if (!session.setActive(false, error.ptr)) {
                println("setActive ERROR: " + error.value?.localizedDescription)
            }
        }
    }
}.flowOn(Dispatchers.Default)

//https://github.com/syedhali/EZAudio/blob/master/EZAudio/EZAudioFFT.m#L152
@OptIn(ExperimentalForeignApi::class)
private fun ProducerScope<Float>.processAudioBuffer(buffer: AVAudioPCMBuffer?) {
    memScoped {
        val data = buffer?.floatChannelData?.get(0) ?: return@memScoped
        val audioSamples: CPointer<DSPComplex>? = interpretCPointer(data.rawValue)

        val bufferSize = buffer.frameLength
        val sampleRate = buffer.format.sampleRate
        val nOver2 = (bufferSize / 2u).toULong()
        val nyquistMaxFreq = (sampleRate / 2.0).toFloat()
        val log2n: vDSP_Length = log2f(bufferSize.toFloat()).toULong()
        val fftSetup = vDSP_create_fftsetup(log2n, FFT_RADIX2.toInt())
        val fftNormFactor: FloatVarOf<Float> = alloc(10f / (2f * bufferSize.toFloat()))
        val magnitude = allocArray<FloatVarOf<Float>>(nOver2.toInt())
        val maxFrequencyMagnitude: FloatVarOf<Float> = alloc()
        val maxFrequencyIndex: ULongVarOf<vDSP_Length> = alloc()
        val output = alloc<DSPSplitComplex> {
            realp = allocArray<FloatVarOf<Float>>(nOver2.toInt())
            imagp = allocArray<FloatVarOf<Float>>(nOver2.toInt())
        }

        vDSP_ctoz(audioSamples, 2L, output.ptr, 1L, nOver2)
        vDSP_fft_zrip(fftSetup, output.ptr, 1, log2n, FFT_FORWARD)
        vDSP_vsmul(output.realp, 1, fftNormFactor.ptr, output.realp, 1, nOver2)
        vDSP_vsmul(output.imagp, 1, fftNormFactor.ptr, output.imagp, 1, nOver2)
        vDSP_zvmags(output.ptr, 1, magnitude, 1, nOver2)
        vDSP_fft_zrip(fftSetup, output.ptr, 1, log2n, FFT_INVERSE)
        vDSP_ztoc(output.ptr, 1, audioSamples, 2, nOver2)
        vDSP_maxvi(magnitude, 1, maxFrequencyMagnitude.ptr, maxFrequencyIndex.ptr, nOver2)

        val frequency = (maxFrequencyIndex.value.toFloat() / nOver2.toFloat()) * nyquistMaxFreq
        trySend(frequency)

        vDSP_destroy_fftsetup(fftSetup)
    }
}