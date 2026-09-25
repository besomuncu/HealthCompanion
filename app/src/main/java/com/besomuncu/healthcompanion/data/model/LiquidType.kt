package com.besomuncu.healthcompanion.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "liquid_types")
data class LiquidType(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconResId: Int? = null,
    val hydrationFactor: Float = 1.0f // Water is 1.0, coffee might be 0.9, etc.
)
