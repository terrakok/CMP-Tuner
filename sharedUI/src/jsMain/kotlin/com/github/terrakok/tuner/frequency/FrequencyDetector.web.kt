package com.github.terrakok.tuner.frequency


import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint8Array
import org.w3c.dom.mediacapture.MediaStream
import org.w3c.dom.mediacapture.MediaStreamConstraints

private const val DEFAULT_PROCESSING_DELAY_MS = 100L
private const val DEFAULT_FFT_SIZE = 8192
actual fun getMicFrequency(): Flow<Float> = callbackFlow {
    val stream = try {
        window.navigator.mediaDevices.getUserMedia(MediaStreamConstraints(audio = true)).await()
    } catch (t: Throwable) {
        console.log("Error accessing the microphone", t)
        throw t
    }
    val audioCtx = AudioContext()
    val analyser = audioCtx.createAnalyser().apply {
        fftSize = DEFAULT_FFT_SIZE
    }
    val dataArray = Float32Array(analyser.fftSize.toInt())

    val source = audioCtx.createMediaStreamSource(stream)
    source.connect(analyser)

    val job = launch {
        while (isActive) {
            analyser.getFloatTimeDomainData(dataArray)
            val maxFrequency = yin(dataArray, audioCtx.sampleRate)
            val isValid = js("!isNaN(maxFrequency)") as Boolean
            if (isValid) trySend(maxFrequency)
            delay(DEFAULT_PROCESSING_DELAY_MS)
        }
    }

    awaitClose { job.cancel() }
}

private external class AudioContext {
    val sampleRate: Number
    fun createMediaStreamSource(stream: MediaStream): MediaStreamAudioSourceNode
    fun createAnalyser(): AnalyserNode
}

private external interface MediaStreamAudioSourceNode {
    fun connect(analyser: Any)
}

private external interface AnalyserNode {
    var fftSize: Number
    /**
     * See https://developer.mozilla.org/en-US/docs/Web/API/AnalyserNode/getFloatTimeDomainData
     */
    fun getFloatTimeDomainData(array: Float32Array)
}

private external fun yin(data: Float32Array, sampleRate: Number): Float