package com.portfolio.ai_challenge

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform