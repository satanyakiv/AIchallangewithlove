package com.portfolio.ai_challenge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.portfolio.ai_challenge.navigation.AppScreen
import com.portfolio.ai_challenge.ui.screen.Day10BranchingScreen
import com.portfolio.ai_challenge.ui.screen.Day10ComparisonScreen
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

@Composable
fun App() {
    AiChallengeTheme {
        val backStack = remember { mutableStateListOf<AppScreen>(AppScreen.Main) }

        fun navigate(screen: AppScreen) = backStack.add(screen)
        fun back() { if (backStack.size > 1) backStack.removeLast() }

        when (val current = backStack.last()) {
            AppScreen.Main -> MainScreen(onDayClick = { dayId ->
                when (dayId) {
                    4 -> navigate(AppScreen.Day4)
                    5 -> navigate(AppScreen.Day5)
                    6 -> navigate(AppScreen.Day6)
                    7 -> navigate(AppScreen.Day7)
                    8 -> navigate(AppScreen.Day8)
                    9 -> navigate(AppScreen.Day9)
                    10 -> navigate(AppScreen.Day10Hub)
                }
            })
            AppScreen.Day4 -> Day4Screen(onBack = ::back)
            AppScreen.Day5 -> Day5Screen(onBack = ::back)
            AppScreen.Day6 -> Day6Screen(onBack = ::back)
            AppScreen.Day7 -> Day7Screen(onBack = ::back)
            AppScreen.Day8 -> Day8Screen(onBack = ::back)
            AppScreen.Day9 -> Day9Screen(onBack = ::back)
            AppScreen.Day10Hub -> Day10HubScreen(
                onBack = ::back,
                onNavigate = ::navigate,
            )
            AppScreen.Day10Sliding -> Day10SlidingScreen(onBack = ::back)
            AppScreen.Day10Facts -> Day10FactsScreen(onBack = ::back)
            AppScreen.Day10Branching -> Day10BranchingScreen(onBack = ::back)
            AppScreen.Day10Comparison -> Day10ComparisonScreen(onBack = ::back)
        }
    }
}
