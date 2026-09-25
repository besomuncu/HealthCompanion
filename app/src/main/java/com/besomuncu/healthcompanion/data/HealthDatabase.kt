package com.besomuncu.healthcompanion.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.besomuncu.healthcompanion.data.dao.HealthDao
import com.besomuncu.healthcompanion.data.model.*

@Database(
    entities = [
        User::class,
        LiquidType::class,
        LiquidIntake::class,
        Medication::class,
        BloodPressure::class,
        FavoriteIntake::class,
        DailySummary::class,
        AppSettings::class,
        MedicationLog::class
    ],
    version = 10,
    exportSchema = false
)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun healthDao(): HealthDao

    companion object {
        @Volatile
        private var INSTANCE: HealthDatabase? = null

        fun getDatabase(context: Context): HealthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthDatabase::class.java,
                    "health_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
