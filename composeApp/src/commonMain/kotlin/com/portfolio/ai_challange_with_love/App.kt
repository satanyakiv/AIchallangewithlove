package com.portfolio.ai_challange_with_love

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.portfolio.ai_challange_with_love.ui.screen.Day4Screen
import com.portfolio.ai_challange_with_love.ui.screen.Day5Screen
import com.portfolio.ai_challange_with_love.ui.screen.Day6Screen
import com.portfolio.ai_challange_with_love.ui.screen.MainScreen
import com.portfolio.ai_challange_with_love.ui.theme.AiChallengeTheme

private sealed class Screen {
    data object Main : Screen()
    data object Day4 : Screen()
    data object Day5 : Screen()
    data object Day6 : Screen()
}

@Composable
fun App() {
    AiChallengeTheme {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }

        when (currentScreen) {
            Screen.Main -> MainScreen(onDayClick = { dayId ->
                when (dayId) {
                    4 -> currentScreen = Screen.Day4
                    5 -> currentScreen = Screen.Day5
                    6 -> currentScreen = Screen.Day6
                }
            })
            Screen.Day4 -> Day4Screen(onBack = { currentScreen = Screen.Main })
            Screen.Day5 -> Day5Screen(onBack = { currentScreen = Screen.Main })
            Screen.Day6 -> Day6Screen(onBack = { currentScreen = Screen.Main })
        }
    }
}
