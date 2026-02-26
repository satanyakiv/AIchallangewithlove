package com.portfolio.ai_challenge

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.portfolio.ai_challenge.database.di.databaseModule
import com.portfolio.ai_challenge.di.appModule
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(appModule, databaseModule)
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
