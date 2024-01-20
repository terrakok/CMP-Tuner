package org.jetbrains.tuner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import cmp_tuner.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val MATCH_DELTA = 15

@Composable
internal fun FrequencyMeterView(
    selectedTone: Tone,
    currentFrequency: Float,
    modifier: Modifier = Modifier
) {
    val freqText = remember(currentFrequency) { currentFrequency.toLabel() }
    val toneText = remember(selectedTone) { selectedTone.frequency.toLabel() }
    val freqLabel =
        if (currentFrequency < selectedTone.frequency - MATCH_DELTA) stringResource(Res.string.low)
        else if (currentFrequency > selectedTone.frequency + MATCH_DELTA) stringResource(Res.string.high)
        else stringResource(Res.string.match)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = toneText,
            style = MaterialTheme.typography.labelMedium
        )
        FrequencyMeter(
            selectedTone.frequency,
            currentFrequency,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        Text(
            text = freqLabel,
            style = MaterialTheme.typography.displaySmall
        )
        Text(
            text = freqText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun FrequencyMeter(
    baseValue: Float,
    currentValue: Float,
    modifier: Modifier = Modifier
) {
    val tickNumber = 17
    val scaleAngle = 140f
    val startRotation = 90f - (scaleAngle / 2)
    val minValue = baseValue - (5 * MATCH_DELTA / 2)
    val maxValue = baseValue + (5 * MATCH_DELTA / 2)
    val tickColor = MaterialTheme.colorScheme.onSurface
    val arrowColor = Color.Green

    Canvas(modifier) {
        val scalePadding = 4.dp.toPx()
        val tickHeight = 8.dp.toPx()
        val centerSpace = 30.dp.toPx()
        val centerSpaceAngle = 10f
        val tickWidth = 1.dp.toPx()
        val originSize = size
        val size = originSize.selectBestSize(aspectRatio = 2.0f)
        val radius = (size.width / 2) - scalePadding
        val scaleCenterX = originSize.width / 2
        val scaleCenterY = (originSize.height + size.height) / 2

        //scale
        repeat(tickNumber) { tick ->
            val tickAngle = (scaleAngle / (tickNumber - 1)) * tick
            val stepsStartOffset = Offset(
                x = scaleCenterX + radius * cos((tickAngle + startRotation) * (PI / 180f)).toFloat(),
                y = scaleCenterY - radius * sin((tickAngle + startRotation) * (PI / 180)).toFloat()
            )
            val stepsEndOffset = Offset(
                x = scaleCenterX + (radius - tickHeight) * cos((tickAngle + startRotation) * (PI / 180)).toFloat(),
                y = scaleCenterY - (radius - tickHeight) * sin((tickAngle + startRotation) * (PI / 180)).toFloat()
            )
            drawLine(
                color = tickColor,
                start = stepsStartOffset,
                end = stepsEndOffset,
                strokeWidth = tickWidth,
                cap = StrokeCap.Round
            )
        }

        //arrow
        val currentAngle = if (currentValue >= maxValue) {
            startRotation
        } else if (currentValue <= minValue) {
            startRotation + scaleAngle
        } else {
            ((maxValue - currentValue) * scaleAngle / (maxValue - minValue)) + startRotation
        }
        val path = Path().apply {
            moveTo(
                x = scaleCenterX + (radius - tickHeight) * cos((currentAngle-0.1f) * (PI / 180f)).toFloat(),
                y = scaleCenterY - (radius - tickHeight) * sin((currentAngle-0.1f) * (PI / 180)).toFloat()
            )
            lineTo(
                x = scaleCenterX + (radius - tickHeight) * cos((currentAngle+0.1f) * (PI / 180f)).toFloat(),
                y = scaleCenterY - (radius - tickHeight) * sin((currentAngle+0.1f) * (PI / 180)).toFloat()
            )
            lineTo(
                x = scaleCenterX + centerSpace * cos((currentAngle + centerSpaceAngle) * (PI / 180f)).toFloat(),
                y = scaleCenterY - centerSpace * sin((currentAngle + centerSpaceAngle) * (PI / 180)).toFloat()
            )
            lineTo(
                x = scaleCenterX + centerSpace * cos((currentAngle - centerSpaceAngle) * (PI / 180f)).toFloat(),
                y = scaleCenterY - centerSpace * sin((currentAngle - centerSpaceAngle) * (PI / 180)).toFloat()
            )
            close()
        }
        drawPath(path, arrowColor)
    }
}

private fun Size.selectBestSize(aspectRatio: Float) =
    if (width / aspectRatio <= height) {
        copy(height = width / aspectRatio)
    } else {
        copy(width = height * aspectRatio)
    }

private fun Float.toLabel(): String {
    val splitted = this.toString().split('.')
    val a = splitted[0]
    val b = if (splitted.size > 1) {
        splitted[1].take(2).let {
            if (it.length == 1) "${it}0" else it.take(2)
        }
    } else {
        "00"
    }
    return "$a.$b Hz"
}