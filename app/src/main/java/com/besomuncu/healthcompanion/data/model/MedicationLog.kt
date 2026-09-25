package com.besomuncu.healthcompanion.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_logs",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("medicationId")]
)
data class MedicationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val medicationId: Long,
    val medName: String,
    val timestamp: Long = System.currentTimeMillis()
)
