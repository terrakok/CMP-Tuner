package org.jetbrains.tuner.frequency

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FrequencyDetectorTest {

    @Test
    fun testStub() = runTest(UnconfinedTestDispatcher()) {
        val stubFrequencyDetector = StubFrequencyDetector()
        val collected = mutableListOf<Float?>()
        val collectJob = launch {
            stubFrequencyDetector.frequencies().collect {
                collected.add(it)
            }
        }
        testScheduler.advanceTimeBy(1000)
        assertTrue(collected.isEmpty())

        stubFrequencyDetector.startDetector()
        testScheduler.advanceTimeBy(1000)
        assertEquals(11, collected.size)
        stubFrequencyDetector.stopDetector()

        testScheduler.advanceTimeBy(1000)
        assertEquals(11, collected.size)

        collectJob.cancel()
    }
}