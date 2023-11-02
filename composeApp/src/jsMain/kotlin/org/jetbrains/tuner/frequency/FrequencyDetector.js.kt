package org.jetbrains.tuner.frequency

import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.mediacapture.MediaStream
import org.w3c.dom.mediacapture.MediaStreamConstraints

actual fun createDefaultFrequencyDetector(): FrequencyDetector = RealFrequencyDetector()


class RealFrequencyDetector(val verbose: Boolean = false) : FrequencyDetector {

    companion object {
        private const val DEFAULT_PROCESSING_DELAY_MS = 100L
        private const val DEFAULT_FFT_SIZE = 2048
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
        // Create an AudioContext
        val audioCtx = AudioContext()

        // Create an AudioNode from the stream
        val source = audioCtx.createMediaStreamSource(stream)

        // Create an AnalyserNode
        val analyser = audioCtx.createAnalyser()
        analyser.fftSize = DEFAULT_FFT_SIZE
        val bufferLength = analyser.frequencyBinCount
        val dataArray = Uint8Array(bufferLength.toInt())

        // Connect the source to the analyser
        source.connect(analyser)

        detectorJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                analyser.getByteFrequencyData(dataArray)

                // Find the peak frequency
                val maxIndex = dataArray.maxIndex()
                val maxFrequency = maxIndex * (audioCtx.sampleRate.toDouble() / analyser.fftSize.toDouble())

                if (verbose) {
                    println("Peak frequency: ${maxFrequency.toFixed(2)} Hz")
                }
                // console.log(dataArray)

                frequencies.emit(maxFrequency.toFloat())

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
    val frequencyBinCount: Number

    /**
     * See https://developer.mozilla.org/en-US/docs/Web/API/AnalyserNode/getByteFrequencyData
     */
    fun getByteFrequencyData(array: Uint8Array)
}

// Since `toFixed` is not a standard Kotlin function, we create an extension function for Double
private fun Double.toFixed(digits: Int): String = asDynamic().toFixed(digits) as String

@JsName("maxIndexFun")
private fun Uint8Array.maxIndex(): Int {
    var mIx = 0
    var maxValue = 0
    for (i in (0 until this.length)) {
        val v = this[i].toInt()
        if (v > maxValue) {
            maxValue = v
            mIx = i
        }
    }
    return mIx
}
