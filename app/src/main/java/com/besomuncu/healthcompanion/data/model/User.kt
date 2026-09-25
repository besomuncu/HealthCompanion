package com.besomuncu.healthcompanion.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val gender: String = "",
    val weight: Float = 0f,
    val height: Float = 0f,
    val dailyWaterGoal: Int = 2000
)
