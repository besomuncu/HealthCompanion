package com.besomuncu.healthcompanion.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_summaries")
data class DailySummary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val date: Long,
    val totalWaterMl: Int,
    val totalOtherMl: Int = 0,
    val medicationsTakenCount: Int,
    val medicationsUntakenNames: String = "", // Comma-separated names
    val averageSystolic: Int,
    val averageDiastolic: Int
)
