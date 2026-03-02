package com.portfolio.ai_challenge.database.di

import com.portfolio.ai_challenge.database.ChatDatabase
import com.portfolio.ai_challenge.database.ChatRepository
import com.portfolio.ai_challenge.database.Day10Repository
import com.portfolio.ai_challenge.database.createChatDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val databaseModule: Module = module {
    single<ChatDatabase> { createChatDatabase(androidContext()) }
    single { get<ChatDatabase>().chatMessageDao() }
    single { get<ChatDatabase>().day10Dao() }
    single { ChatRepository(get()) }
    single { Day10Repository(get()) }
}
