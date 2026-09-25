package com.besomuncu.healthcompanion.data.repository

import com.besomuncu.healthcompanion.data.dao.HealthDao
import com.besomuncu.healthcompanion.data.model.*
import kotlinx.coroutines.flow.Flow

class HealthRepository(private val healthDao: HealthDao) {

    // User
    suspend fun insertUser(user: User): Long = healthDao.insertUser(user)
    fun getAllUsers(): Flow<List<User>> = healthDao.getAllUsers()
    suspend fun getUsersOnce(): List<User> = healthDao.getUsersOnce()
    suspend fun getUserById(id: Long): User? = healthDao.getUserById(id)
    suspend fun deleteUser(user: User) = healthDao.deleteUser(user)

    // Liquid Type
    suspend fun insertLiquidType(type: LiquidType): Long = healthDao.insertLiquidType(type)
    fun getAllLiquidTypes(): Flow<List<LiquidType>> = healthDao.getAllLiquidTypes()
    suspend fun deleteLiquidType(type: LiquidType) = healthDao.deleteLiquidType(type)

    // Liquid Intake
    suspend fun insertIntake(intake: LiquidIntake) = healthDao.insertIntake(intake)
    fun getIntakesForUser(userId: Long): Flow<List<LiquidIntake>> = healthDao.getIntakesForUser(userId)
    fun getDailyTotal(userId: Long): Flow<Int?> {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return healthDao.getDailyTotal(userId, calendar.timeInMillis)
    }
    suspend fun deleteIntake(intake: LiquidIntake) = healthDao.deleteIntake(intake)
    suspend fun resetDailyIntake(userId: Long, startDay: Long) = healthDao.resetDailyIntake(userId, startDay)

    // Favorite Intakes
    suspend fun insertFavoriteIntake(favorite: FavoriteIntake) = healthDao.insertFavoriteIntake(favorite)
    fun getFavoriteIntakes(userId: Long): Flow<List<FavoriteIntake>> = healthDao.getFavoriteIntakes(userId)
    suspend fun deleteFavoriteIntake(favorite: FavoriteIntake) = healthDao.deleteFavoriteIntake(favorite)

    // Medication
    suspend fun insertMedication(medication: Medication) = healthDao.insertMedication(medication)
    suspend fun updateMedication(medication: Medication) = healthDao.updateMedication(medication)
    fun getMedicationsForUser(userId: Long): Flow<List<Medication>> = healthDao.getMedicationsForUser(userId)
    suspend fun getMedicationsForUserOnce(userId: Long): List<Medication> = healthDao.getMedicationsForUserOnce(userId)
    suspend fun resetMedicationsForDay(userId: Long) = healthDao.resetMedicationsForDay(userId)
    suspend fun deleteMedication(medication: Medication) = healthDao.deleteMedication(medication)
    suspend fun getMedsForUserOnce(userId: Long) = healthDao.getMedsForUserOnce(userId)

    // Medication Log
    suspend fun insertMedicationLog(log: MedicationLog) = healthDao.insertMedicationLog(log)
    suspend fun getMedicationLogsInRange(userId: Long, start: Long, end: Long) = healthDao.getMedicationLogsInRange(userId, start, end)
    suspend fun deleteTodayMedicationLog(userId: Long, medicationId: Long, startDay: Long) = healthDao.deleteTodayMedicationLog(userId, medicationId, startDay)

    // Blood Pressure
    suspend fun insertBloodPressure(reading: BloodPressure) = healthDao.insertBloodPressure(reading)
    suspend fun updateBloodPressure(reading: BloodPressure) = healthDao.updateBloodPressure(reading)
    suspend fun deleteBloodPressure(reading: BloodPressure) = healthDao.deleteBloodPressure(reading)
    fun getBloodPressureHistory(userId: Long): Flow<List<BloodPressure>> = healthDao.getBloodPressureHistory(userId)

    // Daily Summary
    suspend fun insertDailySummary(summary: DailySummary) = healthDao.insertDailySummary(summary)
    fun getDailySummaries(userId: Long): Flow<List<DailySummary>> = healthDao.getDailySummaries(userId)
    suspend fun deleteDailySummary(summary: DailySummary) = healthDao.deleteDailySummary(summary)

    // App Settings
    suspend fun saveSettings(settings: AppSettings) = healthDao.saveSettings(settings)
    fun getSettingsForUser(userId: Long): Flow<AppSettings?> = healthDao.getSettingsForUser(userId)
    suspend fun getSettingsForUserOnce(userId: Long): AppSettings? = healthDao.getSettingsForUserOnce(userId)

    suspend fun getIntakesInRange(userId: Long, start: Long, end: Long) = healthDao.getIntakesInRange(userId, start, end)

    // Simulation
    suspend fun simulateDayPassed(userId: Long) = healthDao.simulateDayPassed(userId)
    suspend fun simulateDayPassedLogs(userId: Long) = healthDao.simulateDayPassedLogs(userId)
    suspend fun simulateDayPassedSummaries(userId: Long) = healthDao.simulateDayPassedSummaries(userId)

    // Backup & Restore
    suspend fun getBackupData(userId: Long? = null): BackupData {
        return if (userId != null) {
            BackupData(
                users = listOfNotNull(healthDao.getUserById(userId)),
                liquidTypes = healthDao.getAllLiquidTypesOnce(),
                liquidIntakes = healthDao.getLiquidIntakesByUserId(userId),
                medications = healthDao.getMedicationsForUserOnce(userId),
                bloodPressures = healthDao.getBloodPressuresByUserId(userId),
                favoriteIntakes = healthDao.getFavoriteIntakesByUserId(userId),
                dailySummaries = healthDao.getDailySummariesByUserId(userId),
                appSettings = listOfNotNull(healthDao.getSettingsForUserOnce(userId)),
                medicationLogs = healthDao.getMedicationLogsByUserId(userId)
            )
        } else {
            BackupData(
                users = healthDao.getUsersOnce(),
                liquidTypes = healthDao.getAllLiquidTypesOnce(),
                liquidIntakes = healthDao.getAllLiquidIntakesOnce(),
                medications = healthDao.getAllMedicationsOnce(),
                bloodPressures = healthDao.getAllBloodPressuresOnce(),
                favoriteIntakes = healthDao.getAllFavoriteIntakesOnce(),
                dailySummaries = healthDao.getAllDailySummariesOnce(),
                appSettings = healthDao.getAllAppSettingsOnce(),
                medicationLogs = healthDao.getAllMedicationLogsOnce()
            )
        }
    }

    suspend fun restoreBackupData(data: BackupData) {
        if (data.users.isNotEmpty()) healthDao.insertUsers(data.users)
        if (data.liquidTypes.isNotEmpty()) healthDao.insertLiquidTypes(data.liquidTypes)
        if (data.liquidIntakes.isNotEmpty()) healthDao.insertLiquidIntakes(data.liquidIntakes)
        if (data.medications.isNotEmpty()) healthDao.insertMedications(data.medications)
        if (data.bloodPressures.isNotEmpty()) healthDao.insertBloodPressures(data.bloodPressures)
        if (data.favoriteIntakes.isNotEmpty()) healthDao.insertFavoriteIntakes(data.favoriteIntakes)
        if (data.dailySummaries.isNotEmpty()) healthDao.insertDailySummaries(data.dailySummaries)
        if (data.appSettings.isNotEmpty()) healthDao.insertAppSettings(data.appSettings)
        if (data.medicationLogs.isNotEmpty()) healthDao.insertMedicationLogs(data.medicationLogs)
    }

    suspend fun saveSettingsBatch(settings: List<AppSettings>) = healthDao.insertAppSettings(settings)
    suspend fun insertMedications(meds: List<Medication>) = healthDao.insertMedications(meds)
    suspend fun insertBloodPressures(readings: List<BloodPressure>) = healthDao.insertBloodPressures(readings)
    suspend fun insertLiquidIntakes(intakes: List<LiquidIntake>) = healthDao.insertLiquidIntakes(intakes)
    suspend fun insertFavoriteIntakes(favorites: List<FavoriteIntake>) = healthDao.insertFavoriteIntakes(favorites)
    suspend fun insertDailySummaries(summaries: List<DailySummary>) = healthDao.insertDailySummaries(summaries)
    suspend fun insertMedicationLogs(logs: List<MedicationLog>) = healthDao.insertMedicationLogs(logs)
}
