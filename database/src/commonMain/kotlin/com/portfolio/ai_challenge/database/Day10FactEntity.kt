package com.portfolio.ai_challenge.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day10_facts")
data class Day10FactEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long,
)
