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

actual fun createDefaultFrequencyDetector(): FrequencyDetector = RealFrequencyDetector()

class RealFrequencyDetector : FrequencyDetector {
    private val format = AudioFormat(44100f, 16, 1, true, false)
    private val defaultMixer = (AudioSystem.getMixerInfo().firstOrNull { it.toString().contains("WEBCAM") }).also {
            println("Mixed = $it")
    }

    private val line = AudioSystem.getTargetDataLine(format, defaultMixer).apply {
        open(format)
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
            var bufferSize = 4096//line.bufferSize
            var power = 1
            while (power < bufferSize) power *= 2
            val buffer = ByteArray(power)
            while (isActive) {
                val numBytesRead = line.read(buffer, 0, bufferSize)
                println("numBytesRead = $numBytesRead")
                if (numBytesRead > 0) {
                    val samples = buffer.map { it.toDouble() }.toDoubleArray()
                    println("Samples size = ${samples.size}")
                    val complexData = fft.transform(samples, TransformType.FORWARD)
                    val frequency = computeFrequency(complexData)
                    frequencies.emit(frequency)
                }
            }
        }
    }

    override suspend fun stopDetector() {
        line.stop()
        detectorJob?.cancelAndJoin()
    }

    private fun computeFrequency(data: Array<Complex>): Float? {
        var maxAmp = 0.0
        var maxIndex = -1

        for (i in 1 until data.size / 2) {
            val real = data[i].real
            val imaginary = data[i].imaginary
            val magnitude = Math.sqrt(real * real + imaginary * imaginary)

            if (magnitude > maxAmp) {
                maxAmp = magnitude
                maxIndex = i
            }
        }

        return if (maxIndex >= 0) computeFrequencyForIndex(maxIndex).toFloat() else null
    }

    private fun computeFrequencyForIndex(index: Int): Double {
        return index.toDouble() * format.sampleRate.toDouble() / line.bufferSize.toDouble()
    }
}