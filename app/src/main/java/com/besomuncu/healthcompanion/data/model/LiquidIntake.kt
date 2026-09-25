package com.besomuncu.healthcompanion.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "liquid_intakes",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LiquidType::class,
            parentColumns = ["id"],
            childColumns = ["liquidTypeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("userId"), Index("liquidTypeId")]
)
data class LiquidIntake(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val liquidTypeId: Long?,
    val amountMl: Int,
    val effectiveHydrationMl: Int = amountMl,
    val label: String = "Water",
    val timestamp: Long = System.currentTimeMillis()
)
