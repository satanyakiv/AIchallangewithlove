package com.portfolio.ai_challenge.database.di

import com.portfolio.ai_challenge.database.ChatDatabase
import com.portfolio.ai_challenge.database.ChatRepository
import com.portfolio.ai_challenge.database.createChatDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual val databaseModule: Module = module {
    single<ChatDatabase> { createChatDatabase() }
    single { get<ChatDatabase>().chatMessageDao() }
    single { ChatRepository(get()) }
}
