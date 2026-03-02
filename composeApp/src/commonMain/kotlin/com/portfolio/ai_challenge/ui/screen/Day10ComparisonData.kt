package com.portfolio.ai_challenge.ui.screen

data class StrategyMetrics(
    val strategyName: String,
    val avgTokensPerMessage: Int,
    val contextRetentionPercent: Int,
    val memoryOverheadScore: Int,
    val memoryOverheadNote: String,
)

object Day10ComparisonData {
    // Real results from integration tests — scenario "Task Management App" (5 messages)
    // Run date: 2026-03-02  |  ./gradlew :server:test -Pday10.integration=true
    // Sliding: rounds 42→561→1418→1914→2364 prompt tokens, N=5 window
    // Facts:   rounds 64→390→968→1687→2480 prompt tokens, 9 facts extracted
    // Branching: rounds 42→538→1570→2738→4264 prompt tokens, full history
    val strategies = listOf(
        StrategyMetrics(
            strategyName = "Sliding Window",
            avgTokensPerMessage = 1259,
            contextRetentionPercent = 50,
            memoryOverheadScore = 1,
            memoryOverheadNote = "zero overhead — pure trim",
        ),
        StrategyMetrics(
            strategyName = "Sticky Facts",
            avgTokensPerMessage = 1117,
            contextRetentionPercent = 90,
            memoryOverheadScore = 3,
            memoryOverheadNote = "extra API call per round + KV store",
        ),
        StrategyMetrics(
            strategyName = "Branching",
            avgTokensPerMessage = 1830,
            contextRetentionPercent = 100,
            memoryOverheadScore = 4,
            memoryOverheadNote = "branch table + checkpoint tracking",
        ),
    )

    val maxAvgTokens: Int = strategies.maxOf { it.avgTokensPerMessage }
}
