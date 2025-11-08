package com.github.terrakok.tuner.frequency

import kotlinx.coroutines.flow.Flow

expect fun getMicFrequency(): Flow<Float>