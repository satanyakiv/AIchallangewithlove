package com.portfolio.ai_challange_with_love

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform