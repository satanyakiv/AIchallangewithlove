package com.portfolio.ai_challenge.navigation

sealed class AppScreen {
    data object Main : AppScreen()

    // Day 4
    data object Day4 : AppScreen()

    // Day 5
    data object Day5 : AppScreen()

    // Day 6
    data object Day6 : AppScreen()

    // Day 7
    data object Day7 : AppScreen()

    // Day 8
    data object Day8 : AppScreen()

    // Day 9
    data object Day9 : AppScreen()

    // Day 10
    data object Day10Hub : AppScreen()
    data object Day10Sliding : AppScreen()
    data object Day10Facts : AppScreen()
    data object Day10Branching : AppScreen()
    data object Day10Comparison : AppScreen()
}