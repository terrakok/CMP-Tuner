package org.jetbrains.tuner

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.tuner.theme.AppTheme

@Preview(
    showBackground = true,
    device = "id:pixel_5"
)
@Composable
private fun FrequencyMeterPreview() {
    App()
}