package com.github.terrakok.tuner

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.terrakok.tuner.frequency.getMicFrequency
import com.github.terrakok.tuner.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.transform
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import tuner.sharedui.generated.resources.Res
import tuner.sharedui.generated.resources.classic_guitar

data class Tone(
    val name: String,
    val frequency: Float
)

sealed class Instrument {
    abstract val name: StringResource
    abstract val tones: List<Tone>

    data object ClassicGuitar : Instrument() {
        override val name = Res.string.classic_guitar
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

@Preview
@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}
) = AppTheme(onThemeChanged) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 600.dp)
                .padding(16.dp)
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

            // Top: note letter and cents diff
            val noteLetter = remember(selectedTone) { selectedTone.name.replace(Regex("\\d"), "") }
            val centsDiff = remember(frequency, selectedTone) { currentCentsDiff(frequency, selectedTone.frequency) }
            Spacer(Modifier.height(8.dp))
            Text(text = noteLetter, style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(4.dp))
            Text(text = centsDiff, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            // Middle: gauge
            FrequencyMeterView(
                selectedTone = selectedTone,
                currentFrequency = frequency,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f)
                    .fillMaxWidth()
            )

            // Instrument name
            Text(
                text = stringResource(selectedInstrument.name),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))

            // Bottom: strings horizontally
            InstrumentView(
                instrument = selectedInstrument,
                selectedTone = selectedTone,
                onToneClick = { selectedTone = it }
            )
        }
    }
}

private fun currentCentsDiff(current: Float, base: Float): String {
    if (current <= 0f || base <= 0f) return "0 cents"
    val ratio = current / base
    val cents = (1200.0 * kotlin.math.ln(ratio.toDouble()) / kotlin.math.ln(2.0)).toInt()
    val sign = if (cents > 0) "+" else ""
    return "$sign$cents cents"
}
