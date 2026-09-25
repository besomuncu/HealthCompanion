package com.besomuncu.healthcompanion.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val userId: Long = 0, // Linked to User.id. 0 is the default/fallback
    val use24HourFormat: Boolean = true,
    val dateFormat: String = "DD/MM/YYYY",
    val language: String = "English",
    val theme: String = "System",
    val textSizeMultiplier: Float = 1.0f,
    val lastResetTimestamp: Long = 0,
    val bpReminderInterval: Int = 3,
    val waterReminderInterval: Int = 3,
    val layoutMode: String = "Normal", // "Normal" or "Simplified"
    val autoBackupEnabled: Boolean = false,
    val lastBackupTimestamp: Long = 0
)
