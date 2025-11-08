package com.github.terrakok.tuner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
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
    FrequencyMeter(
        baseValue = selectedTone.frequency,
        currentValue = currentFrequency,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun FrequencyMeter(
    baseValue: Float,
    currentValue: Float,
    modifier: Modifier = Modifier
) {
    val tickNumber = 21
    val scaleAngle = 70f
    val startRotation = 90f - (scaleAngle / 2)
    val minValue = baseValue - (5 * MATCH_DELTA / 2)
    val maxValue = baseValue + (5 * MATCH_DELTA / 2)
    val tickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    val arrowColor = Color(0xFFB00020) // red
    val centerAccent = MaterialTheme.colorScheme.primary
    val centerOuter = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    val centerInner = MaterialTheme.colorScheme.surface

    Canvas(modifier) {
        val scalePadding = 8.dp.toPx()
        val tickHeight = 10.dp.toPx()
        val originSize = size
        val size = originSize.selectBestSize(aspectRatio = 0.8f)
        val chord = size.width - 2 * scalePadding
        val radius = chord / (2f * sin((scaleAngle / 2f) * (PI / 180f))).toFloat()
        val scaleCenterX = originSize.width / 2
        val scaleCenterY = (originSize.height + radius) / 2

        // scale ticks
        repeat(tickNumber) { tick ->
            val tickAngle = (scaleAngle / (tickNumber - 1)) * tick
            val isCenter = tick == (tickNumber - 1) / 2
            val isCenterAccent = isCenter
            val height = if (isCenterAccent) tickHeight * 1.8f else tickHeight
            val width = if (isCenterAccent) 2.dp.toPx() else 1.dp.toPx()
            val color = if (isCenterAccent) centerAccent else tickColor
            val start = Offset(
                x = scaleCenterX + radius * cos((tickAngle + startRotation) * (PI / 180f)).toFloat(),
                y = scaleCenterY - radius * sin((tickAngle + startRotation) * (PI / 180)).toFloat()
            )
            val end = Offset(
                x = scaleCenterX + (radius - height) * cos((tickAngle + startRotation) * (PI / 180)).toFloat(),
                y = scaleCenterY - (radius - height) * sin((tickAngle + startRotation) * (PI / 180)).toFloat()
            )
            drawLine(
                color = color,
                start = start,
                end = end,
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }
        // add a second thin center accent tick next to the center
        val centerTickAngle = (scaleAngle / (tickNumber - 1)) * ((tickNumber - 1) / 2)
        val offsetAngle = 1f
        listOf(centerTickAngle - offsetAngle, centerTickAngle + offsetAngle).forEach { angle ->
            val start = Offset(
                x = scaleCenterX + radius * cos((angle + startRotation) * (PI / 180f)).toFloat(),
                y = scaleCenterY - radius * sin((angle + startRotation) * (PI / 180)).toFloat()
            )
            val end = Offset(
                x = scaleCenterX + (radius - tickHeight * 1.4f) * cos((angle + startRotation) * (PI / 180)).toFloat(),
                y = scaleCenterY - (radius - tickHeight * 1.4f) * sin((angle + startRotation) * (PI / 180)).toFloat()
            )
            drawLine(
                color = centerAccent,
                start = start,
                end = end,
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // needle
        val currentAngle = if (currentValue >= maxValue) {
            startRotation
        } else if (currentValue <= minValue) {
            startRotation + scaleAngle
        } else {
            ((maxValue - currentValue) * scaleAngle / (maxValue - minValue)) + startRotation
        }
        val innerRadius = 12.dp.toPx()
        val needleStart = Offset(
            x = scaleCenterX + innerRadius * cos((currentAngle) * (PI / 180f)).toFloat(),
            y = scaleCenterY - innerRadius * sin((currentAngle) * (PI / 180)).toFloat()
        )
        val needleEnd = Offset(
            x = scaleCenterX + (radius - tickHeight) * cos((currentAngle) * (PI / 180f)).toFloat(),
            y = scaleCenterY - (radius - tickHeight) * sin((currentAngle) * (PI / 180)).toFloat()
        )
        drawLine(
            color = arrowColor,
            start = needleStart,
            end = needleEnd,
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        // center knob
        drawCircle(
            color = centerOuter,
            radius = innerRadius,
            center = Offset(scaleCenterX, scaleCenterY)
        )
        drawCircle(
            color = centerInner,
            radius = innerRadius * 0.6f,
            center = Offset(scaleCenterX, scaleCenterY)
        )
    }
}

private fun Size.selectBestSize(aspectRatio: Float) =
    if (width / aspectRatio <= height) {
        copy(height = width / aspectRatio)
    } else {
        copy(width = height * aspectRatio)
    }