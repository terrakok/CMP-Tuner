package org.jetbrains.tuner.frequency

import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint8Array
import org.w3c.dom.mediacapture.MediaStream
import org.w3c.dom.mediacapture.MediaStreamConstraints

actual fun createDefaultFrequencyDetector(): FrequencyDetector = RealFrequencyDetector()


class RealFrequencyDetector(val verbose: Boolean = false) : FrequencyDetector {

    companion object {
        private const val DEFAULT_PROCESSING_DELAY_MS = 100L
        private const val DEFAULT_FFT_SIZE = 8192
    }

    private val frequencies = MutableSharedFlow<Float?>()
    override fun frequencies(): Flow<Float?> = frequencies.asSharedFlow()

    private var detectorJob: Job? = null
    override suspend fun startDetector() {
        val stream = try {
            window.navigator.mediaDevices.getUserMedia(MediaStreamConstraints(audio = true)).await()
        } catch (t: Throwable) {
            console.log("Error accessing the microphone", t)
            throw t
        }
        val audioCtx = AudioContext()


        // Create an AnalyserNode
        val analyser = audioCtx.createAnalyser().apply {
            fftSize = DEFAULT_FFT_SIZE
        }
        val dataArray = Float32Array(analyser.fftSize.toInt())

        val source = audioCtx.createMediaStreamSource(stream)
        source.connect(analyser)

        detectorJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                analyser.getFloatTimeDomainData(dataArray)

                val maxFrequency = yin(dataArray, audioCtx.sampleRate)

                if (verbose) {
                    println("Peak frequency: $maxFrequency Hz")
                }

                val isValid = js("!isNaN(maxFrequency)") as Boolean
                if (isValid) {
                    frequencies.emit(maxFrequency)
                }

                delay(DEFAULT_PROCESSING_DELAY_MS)
            }
        }
    }

    override suspend fun stopDetector() {
        detectorJob?.cancelAndJoin()
    }
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
    val frequencyBinCount: Int

    /**
     * See https://developer.mozilla.org/en-US/docs/Web/API/AnalyserNode/getFloatTimeDomainData
     */
    fun getFloatTimeDomainData(array: Float32Array)

    fun getByteFrequencyData(array: Uint8Array)
    fun getFloatFrequencyData(array: Float32Array)
}

private external fun yin(data: Float32Array, sampleRate: Number): Float