package com.besomuncu.healthcompanion.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medications",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val illness: String,
    val intakeMethod: String, // e.g., "Pill", "Syrup"
    val scheduledTime: String, // Format "HH:mm"
    val foodContext: String = "doesn't matter", // "Before Meal", "After Meal", "Doesn't Matter"
    val initialPillCount: Int = 0,
    val pillCount: Int = 0,
    val dosagePerIntake: Int = 1,
    val isTakenToday: Boolean = false,
    val isActive: Boolean = true
)
