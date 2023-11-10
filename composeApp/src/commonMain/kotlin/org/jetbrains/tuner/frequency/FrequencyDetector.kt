package org.jetbrains.tuner.frequency

import kotlinx.coroutines.flow.Flow

interface FrequencyDetector {
    /**
     * A flow of detected fundamental frequencies from the audio stream.
     * Emits null or a special value to indicate silence or noise.
     */
    fun frequencies(): Flow<Float?>

    suspend fun startDetector()
    suspend fun stopDetector()
}

expect fun createFrequencyDetector(): FrequencyDetector