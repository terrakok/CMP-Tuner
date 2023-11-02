import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import org.jetbrains.tuner.App
import org.jetbrains.skiko.wasm.onWasmReady
import org.jetbrains.tuner.frequency.createDefaultFrequencyDetector

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    onWasmReady {
        CanvasBasedWindow("CMP-Tuner") {
            App()
        }
    }
}
