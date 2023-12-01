package org.jetbrains.tuner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun InstrumentView(
    instrument: Instrument,
    selectedTone: Tone,
    onToneClick: (Tone) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            instrument.tones.take(instrument.tones.size / 2).reversed().forEach { tone ->
                val isSelected = remember(instrument, selectedTone) { selectedTone == tone }
                ToneView(tone, isSelected, onToneClick)
            }
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                modifier = Modifier.padding(vertical = 8.dp),
                text = instrument.name,
                style = MaterialTheme.typography.titleLarge
            )
        }
        Column {
            instrument.tones.takeLast(instrument.tones.size / 2).forEach { tone ->
                val isSelected = remember(instrument, selectedTone) { selectedTone == tone }
                ToneView(tone, isSelected, onToneClick)
            }
        }
    }
}

@Composable
private fun ToneView(
    tone: Tone,
    isSelected: Boolean,
    onToneClick: (Tone) -> Unit
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    val color = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.primary
    }
    Text(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(50))
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            .clickable { onToneClick(tone) }
            .background(bgColor)
            .size(60.dp)
            .wrapContentHeight(),
        text = tone.name,
        style = MaterialTheme.typography.titleLarge,
        color = color,
        textAlign = TextAlign.Center
    )
}
