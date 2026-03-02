package com.portfolio.ai_challenge.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY id ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY id ASC")
    suspend fun getAll(): List<ChatMessageEntity>

    @Insert
    suspend fun insert(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()

    @Query("SELECT * FROM chat_messages WHERE role = 'context_summary' ORDER BY id DESC LIMIT 1")
    suspend fun getLatestSummary(): ChatMessageEntity?

    @Query("DELETE FROM chat_messages WHERE role = 'context_summary'")
    suspend fun clearSummaries()

    @Query("SELECT * FROM chat_messages WHERE role != 'context_summary' ORDER BY id ASC")
    suspend fun getAllExcludingSummary(): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE role != 'context_summary' ORDER BY id ASC")
    fun observeAllExcludingSummary(): Flow<List<ChatMessageEntity>>
}
