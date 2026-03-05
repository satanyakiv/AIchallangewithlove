package com.portfolio.ai_challenge

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import com.portfolio.ai_challenge.navigation.RouteDay10Branching
import com.portfolio.ai_challenge.navigation.RouteDay10Comparison
import com.portfolio.ai_challenge.navigation.RouteDay10Facts
import com.portfolio.ai_challenge.navigation.RouteDay10Hub
import com.portfolio.ai_challenge.navigation.RouteDay10Sliding
import com.portfolio.ai_challenge.navigation.RouteDay11
import com.portfolio.ai_challenge.navigation.RouteDay12
import com.portfolio.ai_challenge.navigation.RouteDay13
import com.portfolio.ai_challenge.navigation.RouteDay4
import com.portfolio.ai_challenge.navigation.RouteDay5
import com.portfolio.ai_challenge.navigation.RouteDay6
import com.portfolio.ai_challenge.navigation.RouteDay7
import com.portfolio.ai_challenge.navigation.RouteDay8
import com.portfolio.ai_challenge.navigation.RouteDay9
import com.portfolio.ai_challenge.navigation.RouteMain
import com.portfolio.ai_challenge.ui.screen.Day10BranchingScreen
import com.portfolio.ai_challenge.ui.screen.Day10ComparisonScreen
import com.portfolio.ai_challenge.ui.screen.Day10FactsScreen
import com.portfolio.ai_challenge.ui.screen.Day10HubScreen
import com.portfolio.ai_challenge.ui.screen.Day10SlidingScreen
import com.portfolio.ai_challenge.ui.screen.Day11Screen
import com.portfolio.ai_challenge.ui.screen.Day12Screen
import com.portfolio.ai_challenge.ui.screen.Day13Screen
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
        Surface(modifier = Modifier.fillMaxSize()) {
            val backStack = remember { mutableStateListOf<Any>(RouteMain) }

            NavDisplay(
                backStack = backStack,
                onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<RouteMain> {
                        MainScreen(onDayClick = { id ->
                            val route = when (id) {
                                4 -> RouteDay4
                                5 -> RouteDay5
                                6 -> RouteDay6
                                7 -> RouteDay7
                                8 -> RouteDay8
                                9 -> RouteDay9
                                10 -> RouteDay10Hub
                                11 -> RouteDay11
                                12 -> RouteDay12
                                13 -> RouteDay13
                                else -> return@MainScreen
                            }
                            backStack.add(route)
                        })
                    }
                    entry<RouteDay4>  { Day4Screen(onBack = { backStack.removeLastOrNull() }) }
                    entry<RouteDay5>  { Day5Screen(onBack = { backStack.removeLastOrNull() }) }
                    entry<RouteDay6>  { Day6Screen(onBack = { backStack.removeLastOrNull() }) }
                    entry<RouteDay7>  { Day7Screen(onBack = { backStack.removeLastOrNull() }) }
                    entry<RouteDay8>  { Day8Screen(onBack = { backStack.removeLastOrNull() }) }
                    entry<RouteDay9>  { Day9Screen(onBack = { backStack.removeLastOrNull() }) }
                    entry<RouteDay10Hub> {
                        Day10HubScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onSlidingClick = { backStack.add(RouteDay10Sliding) },
                            onFactsClick = { backStack.add(RouteDay10Facts) },
                            onBranchingClick = { backStack.add(RouteDay10Branching) },
                            onComparisonClick = { backStack.add(RouteDay10Comparison) },
                        )
                    }
                    entry<RouteDay10Sliding>    { Day10SlidingScreen(onBack = { backStack.removeLastOrNull() }) }
                    entry<RouteDay10Facts>      { Day10FactsScreen(onBack = { backStack.removeLastOrNull() }) }
                    entry<RouteDay10Branching>  { Day10BranchingScreen(onBack = { backStack.removeLastOrNull() }) }
                    entry<RouteDay10Comparison> { Day10ComparisonScreen(onBack = { backStack.removeLastOrNull() }) }
                    entry<RouteDay11>           { Day11Screen(onBack = { backStack.removeLastOrNull() }) }
                    entry<RouteDay12>           { Day12Screen(onBack = { backStack.removeLastOrNull() }) }
                    entry<RouteDay13>           { Day13Screen(onBack = { backStack.removeLastOrNull() }) }
                },
            )
        }
    }
}
