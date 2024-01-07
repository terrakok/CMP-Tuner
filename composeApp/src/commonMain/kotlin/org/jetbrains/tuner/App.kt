package org.jetbrains.tuner

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.jetbrains.tuner.frequency.getMicFrequency
import org.jetbrains.tuner.theme.AppTheme

private const val METER_ANGLE = 160

data class Tone(
    val name: String,
    val frequency: Float
)

sealed class Instrument {
    abstract val name: String
    abstract val tones: List<Tone>

    data object ClassicGuitar : Instrument() {
        override val name = "6-string guitar"
        override val tones: List<Tone> = listOf(
            Tone("E2", 82.41f),
            Tone("A2", 110.0f),
            Tone("D3", 146.83f),
            Tone("G3", 196.0f),
            Tone("B3", 246.94f),
            Tone("E4", 329.63f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun App() = AppTheme {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tuner") }
            )
        },
        content = { cotentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(cotentPadding)
                    .consumeWindowInsets(WindowInsets.systemBars),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var frequency by remember { mutableStateOf(0f) }
                LaunchedEffect(Unit) {
                    val stepTime = 500L
                    val stepCount = 20
                    val stepDelay = stepTime / stepCount
                    getMicFrequency()
                        .filter { it > 50 }
                        .sample(stepTime)
                        .runningFold(Pair(0f, 0f)) { acc, next ->
                            Pair(acc.second, next)
                        }
                        .transform { (prev, next) ->
                            val step = (next - prev) / stepCount
                            for (i in 1..stepCount) {
                                emit(prev + step * i)
                                delay(stepDelay)
                            }
                        }
                        .collect { frequency = it }
                }
                val selectedInstrument = remember { Instrument.ClassicGuitar }
                var selectedTone: Tone by remember { mutableStateOf(selectedInstrument.tones.first()) }
                FrequencyMeterView(
                    selectedTone = selectedTone,
                    currentFrequency = frequency,
                    modifier = Modifier.padding(8.dp).weight(1f)
                )
                InstrumentView(selectedInstrument, selectedTone, { selectedTone = it })
            }
        }
    )
}

internal expect fun openUrl(url: String?)