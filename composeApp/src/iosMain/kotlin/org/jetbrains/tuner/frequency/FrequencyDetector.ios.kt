package org.jetbrains.tuner.frequency

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioNodeBus
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioTime
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

actual fun createFrequencyDetector(): FrequencyDetector = IosFrequencyDetector()

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class IosFrequencyDetector : FrequencyDetector {
    private val session = AVAudioSession.sharedInstance()
    private val engine = AVAudioEngine()

    private val frequenciesFlow = MutableStateFlow(0f)

    init {
        memScoped {
            val error: ObjCObjectVar<NSError?> = alloc()
            if (!session.setCategory(AVAudioSessionCategoryPlayAndRecord, error.ptr)) {
                println("setCategory ERROR: " + error.value?.localizedDescription)
            }
        }
    }

    override fun frequencies(): Flow<Float?> = frequenciesFlow

    override suspend fun startDetector() {
        memScoped {
            val error: ObjCObjectVar<NSError?> = alloc()
            if (!session.setActive(true, error.ptr)) {
                println("setActive ERROR: " + error.value?.localizedDescription)
            }

            val inputNode = engine.inputNode
            val inputFormat = inputNode.inputFormatForBus(AVAudioNodeBus.MIN_VALUE)

            inputNode.installTapOnBus(
                AVAudioNodeBus.MIN_VALUE,
                1024u,
                inputFormat,
                ::processAudioBuffer
            )
            if (!engine.startAndReturnError(error.ptr)) {
                println("startAndReturnError ERROR: " + error.value?.localizedDescription)
            }
        }
    }

    //https://github.com/syedhali/EZAudio/blob/master/EZAudio/EZAudioFFT.m#L152
    private fun processAudioBuffer(buffer: AVAudioPCMBuffer?, time: AVAudioTime?) {
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
            frequenciesFlow.value = frequency

            vDSP_destroy_fftsetup(fftSetup)
        }
    }

    override suspend fun stopDetector() {
        memScoped {
            val error: ObjCObjectVar<NSError?> = alloc()
            if (!session.setActive(false, error.ptr)) {
                println("setActive ERROR: " + error.value?.localizedDescription)
            }
        }
    }
}