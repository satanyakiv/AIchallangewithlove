package com.portfolio.ai_challange_with_love

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.portfolio.ai_challange_with_love.ui.screen.Day4Screen
import com.portfolio.ai_challange_with_love.ui.screen.MainScreen
import com.portfolio.ai_challange_with_love.ui.theme.AiChallengeTheme

private sealed class Screen {
    data object Main : Screen()
    data object Day4 : Screen()
}

@Composable
@Preview
fun App() {
    AiChallengeTheme {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }

        when (currentScreen) {
            Screen.Main -> MainScreen(onDayClick = { dayId ->
                if (dayId == 4) currentScreen = Screen.Day4
            })
            Screen.Day4 -> Day4Screen(onBack = { currentScreen = Screen.Main })
        }
    }
}
