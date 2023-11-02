package org.jetbrains.tuner.frequency

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

interface FrequencyDetector {
    /**
     * A flow of detected fundamental frequencies from the audio stream.
     * Emits null or a special value to indicate silence or noise.
     */
    fun frequencies(): Flow<Float?>

    suspend fun startDetector()
    suspend fun stopDetector()
}

class StubFrequencyDetector : FrequencyDetector {
    companion object {
        // The frequency of E4 note in Hz
        private const val E4_FREQUENCY = 329.63f
        // Interval for emitting the frequency in milliseconds
        private const val EMIT_INTERVAL_MS = 92.88f // for 4096 frame size, 44.1kHZ sample rate
        private const val EMIT_INTERVAL_MS_LONG = EMIT_INTERVAL_MS.toLong()
    }

    private val mutex = Mutex(locked = true)

    override fun frequencies(): Flow<Float> = flow {
        while (true) {
            waitForStarted()
            emit(E4_FREQUENCY + Random.nextInt(-50, 50))
            delay(EMIT_INTERVAL_MS_LONG)
        }
    }

    private suspend fun waitForStarted() {
        // Acquire the lock, suspending until it's available, then immediately release it.
        // The lock is only available when `startDetector` has been called.
        mutex.withLock { }
    }

    override suspend fun startDetector() {
        mutex.unlock()
    }

    override suspend fun stopDetector() {
        if (mutex.isLocked) return
        mutex.lock()
    }
}