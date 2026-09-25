package com.besomuncu.healthcompanion.data.dao

import androidx.room.*
import com.besomuncu.healthcompanion.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {

    // User
    @Upsert
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users")
    suspend fun getUsersOnce(): List<User>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: Long): User?

    @Delete
    suspend fun deleteUser(user: User)

    // Liquid Type
    @Insert
    suspend fun insertLiquidType(type: LiquidType): Long

    @Query("SELECT * FROM liquid_types")
    fun getAllLiquidTypes(): Flow<List<LiquidType>>

    @Delete
    suspend fun deleteLiquidType(type: LiquidType)

    // Liquid Intake
    @Insert
    suspend fun insertIntake(intake: LiquidIntake)

    @Query("SELECT * FROM liquid_intakes WHERE userId = :userId ORDER BY timestamp DESC")
    fun getIntakesForUser(userId: Long): Flow<List<LiquidIntake>>

    @Query("SELECT SUM(effectiveHydrationMl) FROM liquid_intakes WHERE userId = :userId AND timestamp >= :startDay")
    fun getDailyTotal(userId: Long, startDay: Long): Flow<Int?>

    @Delete
    suspend fun deleteIntake(intake: LiquidIntake)

    // Favorite Intakes
    @Insert
    suspend fun insertFavoriteIntake(favorite: FavoriteIntake)

    @Query("SELECT * FROM favorite_intakes WHERE userId = :userId")
    fun getFavoriteIntakes(userId: Long): Flow<List<FavoriteIntake>>

    @Delete
    suspend fun deleteFavoriteIntake(favorite: FavoriteIntake)

    // Medication
    @Insert
    suspend fun insertMedication(medication: Medication)

    @Update
    suspend fun updateMedication(medication: Medication)

    @Query("SELECT * FROM medications WHERE userId = :userId ORDER BY scheduledTime ASC")
    fun getMedicationsForUser(userId: Long): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE userId = :userId")
    suspend fun getMedicationsForUserOnce(userId: Long): List<Medication>

    @Query("UPDATE medications SET isTakenToday = 0 WHERE userId = :userId")
    suspend fun resetMedicationsForDay(userId: Long)

    @Delete
    suspend fun deleteMedication(medication: Medication)

    // Medication Log
    @Insert
    suspend fun insertMedicationLog(log: MedicationLog)

    @Query("SELECT * FROM medication_logs WHERE userId = :userId AND timestamp >= :start AND timestamp < :end")
    suspend fun getMedicationLogsInRange(userId: Long, start: Long, end: Long): List<MedicationLog>

    @Query("DELETE FROM medication_logs WHERE userId = :userId AND medicationId = :medicationId AND timestamp >= :startDay")
    suspend fun deleteTodayMedicationLog(userId: Long, medicationId: Long, startDay: Long)

    // Blood Pressure
    @Insert
    suspend fun insertBloodPressure(reading: BloodPressure)

    @Update
    suspend fun updateBloodPressure(reading: BloodPressure)

    @Delete
    suspend fun deleteBloodPressure(reading: BloodPressure)

    @Query("SELECT * FROM blood_pressure_readings WHERE userId = :userId ORDER BY timestamp DESC")
    fun getBloodPressureHistory(userId: Long): Flow<List<BloodPressure>>
    
    // Daily Summary
    @Insert
    suspend fun insertDailySummary(summary: DailySummary)

    @Query("SELECT * FROM daily_summaries WHERE userId = :userId ORDER BY date DESC, id DESC")
    fun getDailySummaries(userId: Long): Flow<List<DailySummary>>

    @Delete
    suspend fun deleteDailySummary(summary: DailySummary)

    // App Settings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettings)

    @Query("SELECT * FROM app_settings WHERE userId = :userId")
    fun getSettingsForUser(userId: Long): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE userId = :userId")
    suspend fun getSettingsForUserOnce(userId: Long): AppSettings?

    @Query("SELECT * FROM liquid_intakes WHERE userId = :userId AND timestamp >= :start AND timestamp < :end")
    suspend fun getIntakesInRange(userId: Long, start: Long, end: Long): List<LiquidIntake>

    @Query("SELECT * FROM medications WHERE userId = :userId")
    suspend fun getMedsForUserOnce(userId: Long): List<Medication>
    @Query("UPDATE daily_summaries SET date = date - 86400000 WHERE userId = :userId")
    suspend fun simulateDayPassedSummaries(userId: Long)

    @Query("UPDATE medication_logs SET timestamp = timestamp - 86400000 WHERE userId = :userId")
    suspend fun simulateDayPassedLogs(userId: Long)

    @Query("UPDATE liquid_intakes SET timestamp = timestamp - 86400000 WHERE userId = :userId")
    suspend fun simulateDayPassed(userId: Long)

    @Query("DELETE FROM liquid_intakes WHERE userId = :userId AND timestamp >= :startDay")
    suspend fun resetDailyIntake(userId: Long, startDay: Long)

    // Backup & Restore
    @Query("SELECT * FROM liquid_types")
    suspend fun getAllLiquidTypesOnce(): List<LiquidType>

    @Query("SELECT * FROM liquid_intakes")
    suspend fun getAllLiquidIntakesOnce(): List<LiquidIntake>

    @Query("SELECT * FROM medications")
    suspend fun getAllMedicationsOnce(): List<Medication>

    @Query("SELECT * FROM blood_pressure_readings")
    suspend fun getAllBloodPressuresOnce(): List<BloodPressure>

    @Query("SELECT * FROM favorite_intakes")
    suspend fun getAllFavoriteIntakesOnce(): List<FavoriteIntake>

    @Query("SELECT * FROM daily_summaries")
    suspend fun getAllDailySummariesOnce(): List<DailySummary>

    @Query("SELECT * FROM app_settings")
    suspend fun getAllAppSettingsOnce(): List<AppSettings>

    @Query("SELECT * FROM medication_logs")
    suspend fun getAllMedicationLogsOnce(): List<MedicationLog>

    @Query("SELECT * FROM blood_pressure_readings WHERE userId = :userId")
    suspend fun getBloodPressuresByUserId(userId: Long): List<BloodPressure>

    @Query("SELECT * FROM liquid_intakes WHERE userId = :userId")
    suspend fun getLiquidIntakesByUserId(userId: Long): List<LiquidIntake>

    @Query("SELECT * FROM favorite_intakes WHERE userId = :userId")
    suspend fun getFavoriteIntakesByUserId(userId: Long): List<FavoriteIntake>

    @Query("SELECT * FROM daily_summaries WHERE userId = :userId")
    suspend fun getDailySummariesByUserId(userId: Long): List<DailySummary>

    @Query("SELECT * FROM medication_logs WHERE userId = :userId")
    suspend fun getMedicationLogsByUserId(userId: Long): List<MedicationLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiquidTypes(types: List<LiquidType>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiquidIntakes(intakes: List<LiquidIntake>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedications(medications: List<Medication>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBloodPressures(readings: List<BloodPressure>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteIntakes(favorites: List<FavoriteIntake>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySummaries(summaries: List<DailySummary>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppSettings(settings: List<AppSettings>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicationLogs(logs: List<MedicationLog>)
}
