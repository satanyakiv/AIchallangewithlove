package com.portfolio.ai_challenge.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

actual fun createChatDatabase(context: Any?): ChatDatabase {
    return Room.databaseBuilder<ChatDatabase>(
        name = "chat.db",
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
