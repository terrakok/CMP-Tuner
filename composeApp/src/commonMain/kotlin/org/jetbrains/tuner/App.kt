package org.jetbrains.tuner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.tuner.frequency.StubFrequencyDetector
import org.jetbrains.tuner.frequency.createDefaultFrequencyDetector
import org.jetbrains.tuner.theme.AppTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun App() = AppTheme {
    var freq by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val detector = createDefaultFrequencyDetector()
        launch {
            detector.frequencies().collect { freq = it ?: 0f }
        }
        detector.startDetector()
    }

    Column {
        Text("Freq=$freq")

        Freqometr(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            angle = 80f,
            min = 200f,
            max = 500f,
            current = freq,
            tickNumber = 61
        )
    }
}

@Composable
private fun Freqometr(
    modifier: Modifier = Modifier,
    angle: Float,
    min: Float,
    max: Float,
    current: Float,
    tickNumber: Int,
    tickColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Canvas(modifier) {
        scale(
            angle = angle,
            tickNumber = tickNumber,
            tickColor = tickColor,
            getTickHeight = { index ->
                when {
                    index % 10 == 0 -> 24.dp
                    index % 5 == 0 -> 16.dp
                    else -> 8.dp
                }
            }
        )
        val delta = (angle * current / (min + max)) - (angle / 2)
        arrow(
            delta = delta,
            arrowColor = tickColor
        )
    }
}

private fun DrawScope.scale(
    angle: Float,
    tickNumber: Int,
    tickColor: Color = Color.Black,
    tickWidth: Dp = 2.dp,
    getTickHeight: (Int) -> Dp = { 16.dp }
) {
    val radius = size.height
    val scaleCenterX = center.x
    val scaleCenterY = size.height

    val tickStepAngle = angle / (tickNumber - 1)
    val rotation = 90 - (angle / 2f)

    repeat(tickNumber) { tick ->
        val tickHeight = getTickHeight(tick).toPx()
        val tickAngle = tickStepAngle * tick
        val stepsStartOffset = Offset(
            x = scaleCenterX + (radius * cos((tickAngle + rotation) * (PI / 180f))).toFloat(),
            y = scaleCenterY - (radius * sin((tickAngle + rotation) * (PI / 180))).toFloat()
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