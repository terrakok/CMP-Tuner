package org.jetbrains.tuner.frequency

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.jvm.JVMAudioInputStream
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.sound.sampled.*


actual fun createDefaultFrequencyDetector(): FrequencyDetector = RealFrequencyDetector()

class RealFrequencyDetector(val verbose: Boolean = false) : FrequencyDetector {

    companion object {
        private const val SAMPLE_RATE = 44100f
        private const val DEFAULT_FFT_SIZE = 4096
    }

    private var line: TargetDataLine? = null

    private val frequencies = MutableSharedFlow<Float?>(
        onBufferOverflow = BufferOverflow.DROP_OLDEST, extraBufferCapacity = 1
    )
    private var detectorJob: Job? = null

    override fun frequencies() = frequencies.asSharedFlow()

    override suspend fun startDetector() {
        stopDetector()

        val format = AudioFormat(SAMPLE_RATE, 16, 1, true, true)
        val info: DataLine.Info = DataLine.Info(TargetDataLine::class.java, format)

        line = (AudioSystem.getLine(info) as TargetDataLine)
        val line = line!!

        line.open(format, DEFAULT_FFT_SIZE)
        line.start()

        val stream = AudioInputStream(line)
        val audioStream = JVMAudioInputStream(stream)
        val dispatcher = AudioDispatcher(audioStream, DEFAULT_FFT_SIZE, 0)

        val resultHandler = PitchDetectionHandler { pitchDetectionResult, _ ->
            if (pitchDetectionResult.pitch != -1f) {
                if (verbose) {
                    println("Pitch = ${pitchDetectionResult.pitch}")
                }
                frequencies.tryEmit(pitchDetectionResult.pitch)
            }
        }

        dispatcher.addAudioProcessor(
            PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.YIN, SAMPLE_RATE, DEFAULT_FFT_SIZE, resultHandler)
        )

        detectorJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                dispatcher.run()
            }
        }
    }

    override suspend fun stopDetector() {
        line?.stop()
        detectorJob?.cancelAndJoin()
    }
}
