package com.portfolio.ai_challenge.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [
        ChatMessageEntity::class,
        Day10MessageEntity::class,
        Day10BranchEntity::class,
        Day10FactEntity::class,
    ],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun day10Dao(): Day10Dao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object ChatDatabaseConstructor : RoomDatabaseConstructor<ChatDatabase> {
    override fun initialize(): ChatDatabase
}
