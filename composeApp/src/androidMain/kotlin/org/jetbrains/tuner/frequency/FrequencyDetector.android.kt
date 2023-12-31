package org.jetbrains.tuner.frequency

import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val SAMPLE_RATE = 44100
private const val DEFAULT_FFT_SIZE = 8192
actual fun getMicFrequency(): Flow<Float> = callbackFlow {
    val dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(SAMPLE_RATE, DEFAULT_FFT_SIZE, 0)
    val pitchProcessor = PitchProcessor(
        PitchProcessor.PitchEstimationAlgorithm.YIN,
        SAMPLE_RATE.toFloat(),
        DEFAULT_FFT_SIZE
    ) { pitchDetectionResult, _ ->
        if (pitchDetectionResult.pitch != -1f) {
            trySendBlocking(pitchDetectionResult.pitch)
        }
    }
    dispatcher.addAudioProcessor(pitchProcessor)
    dispatcher.run()
    awaitClose {
        dispatcher.stop()
    }
}.flowOn(Dispatchers.IO)