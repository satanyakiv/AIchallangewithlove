package com.portfolio.ai_challenge.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface Day10Dao {

    // --- Messages ---

    @Query("SELECT * FROM day10_messages WHERE strategyId = :strategyId ORDER BY id ASC")
    fun observeMessages(strategyId: String): Flow<List<Day10MessageEntity>>

    @Query("SELECT * FROM day10_messages WHERE strategyId = :strategyId ORDER BY id ASC")
    suspend fun getMessages(strategyId: String): List<Day10MessageEntity>

    // For sliding window: all messages for this strategy
    @Query("SELECT * FROM day10_messages WHERE strategyId = 'sliding' ORDER BY id ASC")
    suspend fun getSlidingMessages(): List<Day10MessageEntity>

    // For branching: get messages visible from a given branch
    // = (branch_id='main' AND id<=checkpointId) OR branch_id=branchId
    @Query(
        "SELECT * FROM day10_messages WHERE " +
            "(branchId = 'main' AND id <= :checkpointMessageId) OR branchId = :branchId " +
            "ORDER BY id ASC"
    )
    suspend fun getBranchMessages(branchId: String, checkpointMessageId: Long): List<Day10MessageEntity>

    @Query("SELECT * FROM day10_messages WHERE strategyId = 'branching' AND branchId = 'main' ORDER BY id ASC")
    suspend fun getMainBranchMessages(): List<Day10MessageEntity>

    @Query(
        "SELECT * FROM day10_messages WHERE strategyId = 'branching' AND branchId = :branchId ORDER BY id ASC"
    )
    fun observeBranchMessages(branchId: String): Flow<List<Day10MessageEntity>>

    @Insert
    suspend fun insertMessage(message: Day10MessageEntity): Long

    @Query("DELETE FROM day10_messages WHERE strategyId = :strategyId")
    suspend fun deleteMessagesForStrategy(strategyId: String)

    @Query("DELETE FROM day10_messages WHERE strategyId = 'branching'")
    suspend fun deleteAllBranchingMessages()

    // --- Branches ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: Day10BranchEntity)

    @Query("SELECT * FROM day10_branches ORDER BY createdAt ASC")
    fun observeBranches(): Flow<List<Day10BranchEntity>>

    @Query("SELECT * FROM day10_branches ORDER BY createdAt ASC")
    suspend fun getBranches(): List<Day10BranchEntity>

    @Query("SELECT * FROM day10_branches WHERE id = :branchId")
    suspend fun getBranch(branchId: String): Day10BranchEntity?

    @Query("DELETE FROM day10_branches")
    suspend fun deleteAllBranches()

    // --- Facts ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFact(fact: Day10FactEntity)

    @Query("SELECT * FROM day10_facts ORDER BY updatedAt DESC")
    fun observeFacts(): Flow<List<Day10FactEntity>>

    @Query("SELECT * FROM day10_facts ORDER BY updatedAt DESC")
    suspend fun getAllFacts(): List<Day10FactEntity>

    @Query("DELETE FROM day10_facts")
    suspend fun deleteAllFacts()
}
