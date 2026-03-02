package com.portfolio.ai_challenge.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day10_messages")
data class Day10MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val strategyId: String,
    val branchId: String = "main",
    val role: String,
    val content: String,
    val timestamp: Long,
)
