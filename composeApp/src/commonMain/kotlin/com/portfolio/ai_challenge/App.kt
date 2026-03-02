package com.portfolio.ai_challenge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.portfolio.ai_challenge.ui.screen.Day10BranchingScreen
import com.portfolio.ai_challenge.ui.screen.Day10ComparisonScreen
import com.portfolio.ai_challenge.ui.screen.Day10Destination
import com.portfolio.ai_challenge.ui.screen.Day10FactsScreen
import com.portfolio.ai_challenge.ui.screen.Day10HubScreen
import com.portfolio.ai_challenge.ui.screen.Day10SlidingScreen
import com.portfolio.ai_challenge.ui.screen.Day4Screen
import com.portfolio.ai_challenge.ui.screen.Day5Screen
import com.portfolio.ai_challenge.ui.screen.Day6Screen
import com.portfolio.ai_challenge.ui.screen.Day7Screen
import com.portfolio.ai_challenge.ui.screen.Day8Screen
import com.portfolio.ai_challenge.ui.screen.Day9Screen
import com.portfolio.ai_challenge.ui.screen.MainScreen
import com.portfolio.ai_challenge.ui.theme.AiChallengeTheme

private sealed class Screen {
    data object Main : Screen()
    data object Day4 : Screen()
    data object Day5 : Screen()
    data object Day6 : Screen()
    data object Day7 : Screen()
    data object Day8 : Screen()
    data object Day9 : Screen()
    data object Day10Hub : Screen()
    data object Day10Sliding : Screen()
    data object Day10Facts : Screen()
    data object Day10Branching : Screen()
    data object Day10Comparison : Screen()
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
                    7 -> currentScreen = Screen.Day7
                    8 -> currentScreen = Screen.Day8
                    9 -> currentScreen = Screen.Day9
                    10 -> currentScreen = Screen.Day10Hub
                }
            })
            Screen.Day4 -> Day4Screen(onBack = { currentScreen = Screen.Main })
            Screen.Day5 -> Day5Screen(onBack = { currentScreen = Screen.Main })
            Screen.Day6 -> Day6Screen(onBack = { currentScreen = Screen.Main })
            Screen.Day7 -> Day7Screen(onBack = { currentScreen = Screen.Main })
            Screen.Day8 -> Day8Screen(onBack = { currentScreen = Screen.Main })
            Screen.Day9 -> Day9Screen(onBack = { currentScreen = Screen.Main })
            Screen.Day10Hub -> Day10HubScreen(
                onBack = { currentScreen = Screen.Main },
                onNavigate = { dest ->
                    when (dest) {
                        Day10Destination.Sliding -> currentScreen = Screen.Day10Sliding
                        Day10Destination.Facts -> currentScreen = Screen.Day10Facts
                        Day10Destination.Branching -> currentScreen = Screen.Day10Branching
                        Day10Destination.Comparison -> currentScreen = Screen.Day10Comparison
                    }
                },
            )
            Screen.Day10Sliding -> Day10SlidingScreen(onBack = { currentScreen = Screen.Day10Hub })
            Screen.Day10Facts -> Day10FactsScreen(onBack = { currentScreen = Screen.Day10Hub })
            Screen.Day10Branching -> Day10BranchingScreen(onBack = { currentScreen = Screen.Day10Hub })
            Screen.Day10Comparison -> Day10ComparisonScreen(onBack = { currentScreen = Screen.Day10Hub })
        }
    }
}
