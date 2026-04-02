package com.portfolio.ai_challenge.config

data class ServerConfig(
    val deepseek: DeepSeekConfig,
)

data class DeepSeekConfig(
    val apiKey: String,
    val apiUrl: String = "https://api.deepseek.com/chat/completions",
    val model: String = "deepseek-chat",
    val requestTimeoutMs: Long = 120_000,
)
