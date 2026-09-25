package com.besomuncu.healthcompanion.data.model

import com.google.gson.annotations.SerializedName

data class BackupData(
    @SerializedName("users") val users: List<User> = emptyList(),
    @SerializedName("liquidTypes") val liquidTypes: List<LiquidType> = emptyList(),
    @SerializedName("liquidIntakes") val liquidIntakes: List<LiquidIntake> = emptyList(),
    @SerializedName("medications") val medications: List<Medication> = emptyList(),
    @SerializedName("bloodPressures") val bloodPressures: List<BloodPressure> = emptyList(),
    @SerializedName("favoriteIntakes") val favoriteIntakes: List<FavoriteIntake> = emptyList(),
    @SerializedName("dailySummaries") val dailySummaries: List<DailySummary> = emptyList(),
    @SerializedName("appSettings") val appSettings: List<AppSettings> = emptyList(),
    @SerializedName("medicationLogs") val medicationLogs: List<MedicationLog> = emptyList()
)
