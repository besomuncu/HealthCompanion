package com.besomuncu.healthcompanion.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_intakes")
data class FavoriteIntake(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val amountMl: Int,
    val label: String,
    val liquidTypeId: Long? = null
)
