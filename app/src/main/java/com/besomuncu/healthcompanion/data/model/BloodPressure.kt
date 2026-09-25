package com.besomuncu.healthcompanion.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blood_pressure_readings",
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
data class BloodPressure(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
    val timestamp: Long = System.currentTimeMillis()
)
