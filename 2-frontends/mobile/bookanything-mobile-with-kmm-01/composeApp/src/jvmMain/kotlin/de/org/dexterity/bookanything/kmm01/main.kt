package de.org.dexterity.bookanything.kmm01

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "BookAnythingMobileWithKMM-01",
    ) {
        App()
    }
}