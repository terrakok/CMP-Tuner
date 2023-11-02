package org.jetbrains.tuner.frequency

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine


actual fun createDefaultFrequencyDetector(): FrequencyDetector = RealFrequencyDetector()

class RealFrequencyDetector(val verbose: Boolean = false) : FrequencyDetector {

    companion object {
        private const val SAMPLE_RATE = 44100f
        private const val DEFAULT_FFT_SIZE = 4096
        private const val DEFAULT_PROCESSING_DELAY_MS = 100L
    }

    private val format = AudioFormat(SAMPLE_RATE, 16, 1, true, false)

    private val info: DataLine.Info = DataLine.Info(TargetDataLine::class.java, format)
    private val line: TargetDataLine = (AudioSystem.getLine(info) as TargetDataLine).apply {
        open(format, DEFAULT_FFT_SIZE)
    }
    private val fft = FastFourierTransformer(DftNormalization.STANDARD)
    private val frequencies = MutableSharedFlow<Float?>()
    private var detectorJob: Job? = null

    override fun frequencies() = frequencies.asSharedFlow()

    override suspend fun startDetector() {
        val info = AudioSystem.getMixerInfo().joinToString(separator = "\n") {
             it.toString()
        }
        println(info)
        stopDetector()
        detectorJob = CoroutineScope(Dispatchers.IO).launch {
            line.start()
            val buffer = ByteArray(DEFAULT_FFT_SIZE)
            while (isActive) {
                val numBytesRead = line.read(buffer, 0, buffer.size)

                if (numBytesRead <= 0) continue

                val micBufferData = DoubleArray(numBytesRead / 2) { i ->
                    val index = i * 2
                    val audioSample = (buffer[index].toInt() shl 8) or (buffer[index + 1].toInt() and 0xFF)
                    audioSample / 32768.0 // Normalized between -1.0 and 1.0
                }.let {
                    lowPassFilter(it)
                }

                val complexData = fft.transform(micBufferData, TransformType.FORWARD)
                val result = calculateFrequencies(complexData)
                val fundamentalFrequency = findFundamentalFreq(result, SAMPLE_RATE, DEFAULT_FFT_SIZE)

                if (fundamentalFrequency > 0f) {
                    // Let's skip 0Hz for now
                    frequencies.emit(fundamentalFrequency)
                }

                if (verbose) {
                    println("F = $fundamentalFrequency")
                }
            }
        }
    }

    override suspend fun stopDetector() {
        line.stop()
        detectorJob?.cancelAndJoin()
    }

    private fun calculateFrequencies(complexTransformed: Array<Complex>): DoubleArray {
        return complexTransformed.map { it.abs() }.toDoubleArray()
    }

    @Suppress("SameParameterValue")
    private fun findFundamentalFreq(frequencies: DoubleArray, sampleRate: Float, bufferSize: Int): Float {
        val maxIndex = frequencies.indices.maxByOrNull { frequencies[it] } ?: 0
        return maxIndex * (sampleRate / bufferSize)
    }

    // A primitive low pass filter. Try something advanced. Look at TarsosDSP lib
    // cutoffFrequency default is 2kHz - should be enough for a guitar tuner
    private fun lowPassFilter(signal: DoubleArray, cutoffFrequency: Double = 2000.0, sampleRate: Float = SAMPLE_RATE): DoubleArray {
        val rc = 1.0 / (cutoffFrequency * 2 * Math.PI)
        val dt = 1.0 / sampleRate
        val alpha = dt / (rc + dt)

        var previousFilteredValue = signal[0]
        val filteredSignal = DoubleArray(signal.size)

        signal.forEachIndexed { index, value ->
            val currentFilteredValue = alpha * value + (1 - alpha) * previousFilteredValue
            filteredSignal[index] = currentFilteredValue
            previousFilteredValue = currentFilteredValue
        }

        return filteredSignal
    }
}
