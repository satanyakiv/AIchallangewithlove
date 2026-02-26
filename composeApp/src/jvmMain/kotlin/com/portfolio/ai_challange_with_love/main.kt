package com.portfolio.ai_challange_with_love

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.portfolio.ai_challange_with_love.di.appModule
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(appModule)
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "AI Challenge",
        ) {
            App()
        }
    }
}
