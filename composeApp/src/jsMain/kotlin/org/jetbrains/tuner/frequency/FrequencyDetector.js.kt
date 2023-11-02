package org.jetbrains.tuner.frequency

actual fun createDefaultFrequencyDetector(): FrequencyDetector = StubFrequencyDetector()