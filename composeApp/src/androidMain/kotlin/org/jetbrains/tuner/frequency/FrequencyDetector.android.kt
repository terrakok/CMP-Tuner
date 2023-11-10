package org.jetbrains.tuner.frequency

import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


actual fun createFrequencyDetector(): FrequencyDetector = AndroidFrequencyDetector()

private class AndroidFrequencyDetector(val verbose: Boolean = false) : FrequencyDetector {

    companion object {
        const val SAMPLE_RATE = 44100
        const val DEFAULT_FFT_SIZE = 8192
    }

    private val frequencies = MutableSharedFlow<Float?>(
        onBufferOverflow = BufferOverflow.DROP_OLDEST, extraBufferCapacity = 1
    )
    private var detectorJob: Job? = null

    override fun frequencies() = frequencies.asSharedFlow()

    override suspend fun startDetector() {
        stopDetector()

        val dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(
            SAMPLE_RATE,
            DEFAULT_FFT_SIZE,
            0
        )
        val resultHandler = PitchDetectionHandler { pitchDetectionResult, _ ->
            if (pitchDetectionResult.pitch != -1f) {
                if (verbose) {
                    println("Pitch = ${pitchDetectionResult.pitch}")
                }
                frequencies.tryEmit(pitchDetectionResult.pitch)
            }
        }
        val pitchProcessor = PitchProcessor(
            PitchProcessor.PitchEstimationAlgorithm.YIN,
            SAMPLE_RATE.toFloat(),
            DEFAULT_FFT_SIZE,
            resultHandler
        )

        dispatcher.addAudioProcessor(pitchProcessor)

        detectorJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                dispatcher.run()
            }
        }
    }

    override suspend fun stopDetector() {
        detectorJob?.cancelAndJoin()
    }
}
