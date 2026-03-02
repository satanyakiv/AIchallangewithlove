package com.portfolio.ai_challenge.database

import kotlinx.coroutines.flow.Flow

class ChatRepository(private val dao: ChatMessageDao) {

    fun observeMessages(): Flow<List<ChatMessageEntity>> = dao.observeAll()

    suspend fun getAllMessages(): List<ChatMessageEntity> = dao.getAll()

    suspend fun saveUserMessage(content: String) = dao.insert(
        ChatMessageEntity(
            role = "user",
            content = content,
            timestamp = System.currentTimeMillis(),
        )
    )

    suspend fun saveAssistantMessage(content: String) = dao.insert(
        ChatMessageEntity(
            role = "assistant",
            content = content,
            timestamp = System.currentTimeMillis(),
        )
    )

    suspend fun clearHistory() = dao.deleteAll()

    suspend fun getLatestSummary(): ChatMessageEntity? = dao.getLatestSummary()

    suspend fun saveSummary(content: String) {
        dao.clearSummaries()
        dao.insert(
            ChatMessageEntity(
                role = "context_summary",
                content = content,
                timestamp = System.currentTimeMillis(),
            )
        )
    }

    suspend fun clearSummaries() = dao.clearSummaries()

    suspend fun getAllExcludingSummary(): List<ChatMessageEntity> = dao.getAllExcludingSummary()

    fun observeAllExcludingSummary(): Flow<List<ChatMessageEntity>> = dao.observeAllExcludingSummary()
}
