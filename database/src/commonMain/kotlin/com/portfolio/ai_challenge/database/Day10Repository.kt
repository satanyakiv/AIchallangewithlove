package com.portfolio.ai_challenge.database

import kotlinx.coroutines.flow.Flow

class Day10Repository(private val dao: Day10Dao) {

    // ────────── Sliding ──────────

    fun observeSlidingMessages(): Flow<List<Day10MessageEntity>> =
        dao.observeMessages("sliding")

    suspend fun getSlidingMessages(): List<Day10MessageEntity> =
        dao.getSlidingMessages()

    suspend fun saveSlidingUserMessage(content: String): Long =
        dao.insertMessage(
            Day10MessageEntity(
                strategyId = "sliding",
                role = "user",
                content = content,
                timestamp = System.currentTimeMillis(),
            )
        )

    suspend fun saveSlidingAssistantMessage(content: String): Long =
        dao.insertMessage(
            Day10MessageEntity(
                strategyId = "sliding",
                role = "assistant",
                content = content,
                timestamp = System.currentTimeMillis(),
            )
        )

    suspend fun clearSlidingHistory() = dao.deleteMessagesForStrategy("sliding")

    // ────────── Facts ──────────

    fun observeFactsMessages(): Flow<List<Day10MessageEntity>> =
        dao.observeMessages("facts")

    suspend fun getFactsMessages(): List<Day10MessageEntity> =
        dao.getMessages("facts")

    suspend fun saveFactsUserMessage(content: String): Long =
        dao.insertMessage(
            Day10MessageEntity(
                strategyId = "facts",
                role = "user",
                content = content,
                timestamp = System.currentTimeMillis(),
            )
        )

    suspend fun saveFactsAssistantMessage(content: String): Long =
        dao.insertMessage(
            Day10MessageEntity(
                strategyId = "facts",
                role = "assistant",
                content = content,
                timestamp = System.currentTimeMillis(),
            )
        )

    fun observeFacts(): Flow<List<Day10FactEntity>> = dao.observeFacts()

    suspend fun getAllFacts(): List<Day10FactEntity> = dao.getAllFacts()

    suspend fun upsertFact(key: String, value: String) = dao.upsertFact(
        Day10FactEntity(
            key = key,
            value = value,
            updatedAt = System.currentTimeMillis(),
        )
    )

    suspend fun clearFactsHistory() {
        dao.deleteMessagesForStrategy("facts")
        dao.deleteAllFacts()
    }

    // ────────── Branching ──────────

    fun observeBranches(): Flow<List<Day10BranchEntity>> = dao.observeBranches()

    suspend fun getBranches(): List<Day10BranchEntity> = dao.getBranches()

    suspend fun getBranch(branchId: String): Day10BranchEntity? = dao.getBranch(branchId)

    suspend fun getBranchMessagesForContext(branchId: String): List<Day10MessageEntity> {
        if (branchId == "main") {
            return dao.getMainBranchMessages()
        }
        val branch = dao.getBranch(branchId) ?: return emptyList()
        return dao.getBranchMessages(branchId, branch.checkpointMessageId)
    }

    fun observeBranchMessages(branchId: String): Flow<List<Day10MessageEntity>> =
        dao.observeBranchMessages(branchId)

    suspend fun saveBranchingMessage(
        branchId: String,
        role: String,
        content: String,
    ): Long = dao.insertMessage(
        Day10MessageEntity(
            strategyId = "branching",
            branchId = branchId,
            role = role,
            content = content,
            timestamp = System.currentTimeMillis(),
        )
    )

    suspend fun saveBranchingUserMessage(branchId: String, content: String): Long =
        saveBranchingMessage(branchId, "user", content)

    suspend fun saveBranchingAssistantMessage(branchId: String, content: String): Long =
        saveBranchingMessage(branchId, "assistant", content)

    suspend fun createBranch(id: String, name: String, checkpointMessageId: Long) =
        dao.insertBranch(
            Day10BranchEntity(
                id = id,
                name = name,
                checkpointMessageId = checkpointMessageId,
                createdAt = System.currentTimeMillis(),
            )
        )

    suspend fun getLastMainMessageId(): Long {
        val messages = dao.getMainBranchMessages()
        return messages.lastOrNull()?.id ?: 0L
    }

    suspend fun clearBranchingHistory() {
        dao.deleteAllBranchingMessages()
        dao.deleteAllBranches()
    }
}
