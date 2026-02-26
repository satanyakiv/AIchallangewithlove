package com.portfolio.ai_challenge.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

actual fun createChatDatabase(context: Any?): ChatDatabase {
    val ctx = requireNotNull(context as? Context) { "Android context required" }
    return Room.databaseBuilder<ChatDatabase>(
        context = ctx,
        name = "chat.db",
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
