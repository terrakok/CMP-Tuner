package org.jetbrains.tuner.frequency

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.jvm.JVMAudioInputStream
import be.tarsos.dsp.pitch.PitchProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

private const val SAMPLE_RATE = 44100
private const val DEFAULT_FFT_SIZE = 8192
actual fun getMicFrequency(): Flow<Float> = callbackFlow {
    val format = AudioFormat(SAMPLE_RATE.toFloat(), 16, 1, true, true)
    val line = AudioSystem.getLine(
        DataLine.Info(TargetDataLine::class.java, format)
    ) as TargetDataLine
    line.open(format, DEFAULT_FFT_SIZE)
    line.start()

    val dispatcher = AudioDispatcher(
        JVMAudioInputStream(AudioInputStream(line)),
        DEFAULT_FFT_SIZE,
        0
    )
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
        line.stop()
        dispatcher.stop()
    }
}.flowOn(Dispatchers.IO)