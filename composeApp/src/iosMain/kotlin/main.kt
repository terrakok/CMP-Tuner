import androidx.compose.ui.window.ComposeUIViewController
import org.jetbrains.tuner.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
