package org.jetbrains.tuner

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.tuner.frequency.StubFrequencyDetector
import org.jetbrains.tuner.frequency.createDefaultFrequencyDetector
import org.jetbrains.tuner.theme.AppTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val METER_ANGLE = 160

data class Tone(
    val name: String,
    val frequency: Float
)

sealed class Instrument {
    abstract val tones: List<Tone>

    data object ClassicGuitar : Instrument() {
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

@Composable
internal fun App() = AppTheme {
    var freq by remember { mutableFloatStateOf(0f) }
    val instrument = Instrument.ClassicGuitar
    var selectedTone by remember { mutableStateOf(instrument.tones.first()) }

    LaunchedEffect(Unit) {
        val detector = createDefaultFrequencyDetector()
        launch {
            detector.frequencies().collect { freq = it ?: 0f }
        }
        detector.startDetector()
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().aspectRatio(0.7f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val delta = remember(freq, selectedTone) {
                val ideal = selectedTone.frequency
                freq - ideal
            }
            Freqometr(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
                    .padding(vertical = 24.dp, horizontal = 8.dp),
                delta = delta
            )
            Text(
                text = when {
                    delta in -5f..5f -> "Tuned!"
                    delta < -5f -> "Low"
                    else -> "High"
                },
                style = MaterialTheme.typography.displayMedium
            )
            Text(text = "$freq Hz")
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Instrument.ClassicGuitar.tones.forEach { tone ->
                    val selected = selectedTone == tone
                    Text(
                        modifier = Modifier
                            .padding(8.dp)
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(
                                color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { selectedTone = tone }
                            .wrapContentHeight(),
                        text = tone.name,
                        color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onTertiaryContainer,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun ToneView(
    tone: Tone,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            text = tone.name,
            textAlign = TextAlign.Center
        )
        Divider(Modifier.fillMaxWidth())
        Divider(Modifier.fillMaxHeight())
    }
}

@Composable
private fun Freqometr(
    modifier: Modifier = Modifier,
    delta: Float,
    tickColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier) {
        scale(
            tickColor = tickColor,
            textMeasurer = textMeasurer
        )
        arrow(
            delta = delta,
            arrowColor = tickColor
        )
    }
}

private fun DrawScope.scale(
    tickColor: Color = Color.Black,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle = TextStyle(color = tickColor.copy(0.5f))
) {

    val angle = METER_ANGLE
    val tickNumber = 17
    val radius = size.height
    val scaleCenterX = center.x
    val scaleCenterY = size.height

    val tickWidth: Dp = 2.dp
    val getTickHeight: (Int) -> Dp = {
        if (it == 8) 12.dp else 4.dp
    }
    val getTickLabel: (Int) -> String = {
        if (it % 2 == 1) ""
        else (80 - it * 10).toString()
    }
    val labelRadius = radius + 24.dp.toPx()
    val tickStepAngle = angle / (tickNumber - 1)
    val rotation = 90 - (angle / 2f)

    repeat(tickNumber) { tick ->
        val tickHeight = getTickHeight(tick).toPx()
        val tickAngle = tickStepAngle * tick
        val stepsStartOffset = Offset(
            x = scaleCenterX + (radius + tickHeight) * cos((tickAngle + rotation) * (PI / 180f)).toFloat(),
            y = scaleCenterY - (radius + tickHeight) * sin((tickAngle + rotation) * (PI / 180)).toFloat()
        )
        val stepsEndOffset = Offset(
            x = scaleCenterX + (radius - tickHeight) * cos((tickAngle + rotation) * (PI / 180)).toFloat(),
            y = scaleCenterY - (radius - tickHeight) * sin((tickAngle + rotation) * (PI / 180)).toFloat()
        )
        drawLine(
            color = tickColor,
            start = stepsStartOffset,
            end = stepsEndOffset,
            strokeWidth = tickWidth.toPx(),
            cap = StrokeCap.Round
        )

        val label = getTickLabel(tick)
        if (label.isNotBlank()) {
            val labelTextLayout = textMeasurer.measure(
                text = label,
                style = textStyle
            )
            val labelOffset = Offset(
                x = scaleCenterX + labelRadius * cos((tickAngle + rotation) * (PI / 180)).toFloat(),
                y = scaleCenterY - labelRadius * sin((tickAngle + rotation) * (PI / 180)).toFloat()
            )
            val labelTopLeft = Offset(
                labelOffset.x - (labelTextLayout.size.width / 2f),
                labelOffset.y - (labelTextLayout.size.height / 2f)
            )

            drawText(
                textMeasurer = textMeasurer,
                text = label,
                topLeft = labelTopLeft,
                style = textStyle
            )
        }
    }
}


private fun DrawScope.arrow(
    delta: Float,
    arrowColor: Color = Color.Black,
    arrowWidth: Dp = 2.dp
) {
    val radius = size.height
    val scaleCenterX = center.x
    val scaleCenterY = size.height
    val rotation = 90

    val start = Offset(scaleCenterX, scaleCenterY)
    val end = Offset(
        x = scaleCenterX + (radius * cos((-delta + rotation) * (PI / 180f))).toFloat(),
        y = scaleCenterY - (radius * sin((-delta + rotation) * (PI / 180))).toFloat()
    )
    drawLine(
        color = arrowColor,
        start = start,
        end = end,
        strokeWidth = arrowWidth.toPx(),
        cap = StrokeCap.Round
    )
}

internal expect fun openUrl(url: String?)