package com.portfolio.ai_challenge.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day10_branches")
data class Day10BranchEntity(
    @PrimaryKey val id: String,
    val name: String,
    val checkpointMessageId: Long,
    val createdAt: Long,
)
