package com.portfolio.ai_challange_with_love

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AIchallangewithlove",
    ) {
        App()
    }
}