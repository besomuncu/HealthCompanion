package com.besomuncu.healthcompanion.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.besomuncu.healthcompanion.R
import com.besomuncu.healthcompanion.data.HealthDatabase
import com.besomuncu.healthcompanion.data.model.BackupData
import com.besomuncu.healthcompanion.data.repository.HealthRepository
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

object BackupUtils {

    suspend fun triggerAutoBackup(context: Context) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("global_auto_backup_enabled", false)
        if (enabled) {
            exportDataInternal(context, null, isAuto = true)
        }
    }

    suspend fun exportDataInternal(context: Context, userId: Long?, isAuto: Boolean) {
        try {
            val dao = HealthDatabase.getDatabase(context).healthDao()
            val repository = HealthRepository(dao)
            
            val backupData = if (userId != null) {
                BackupData(
                    users = listOfNotNull(dao.getUserById(userId)),
                    liquidTypes = dao.getAllLiquidTypesOnce(),
                    liquidIntakes = dao.getLiquidIntakesByUserId(userId),
                    medications = dao.getMedicationsForUserOnce(userId),
                    bloodPressures = dao.getBloodPressuresByUserId(userId),
                    favoriteIntakes = dao.getFavoriteIntakesByUserId(userId),
                    dailySummaries = dao.getDailySummariesByUserId(userId),
                    appSettings = listOfNotNull(dao.getSettingsForUserOnce(userId)),
                    medicationLogs = dao.getMedicationLogsByUserId(userId)
                )
            } else {
                BackupData(
                    users = dao.getUsersOnce(),
                    liquidTypes = dao.getAllLiquidTypesOnce(),
                    liquidIntakes = dao.getAllLiquidIntakesOnce(),
                    medications = dao.getAllMedicationsOnce(),
                    bloodPressures = dao.getAllBloodPressuresOnce(),
                    favoriteIntakes = dao.getAllFavoriteIntakesOnce(),
                    dailySummaries = dao.getAllDailySummariesOnce(),
                    appSettings = dao.getAllAppSettingsOnce(),
                    medicationLogs = dao.getAllMedicationLogsOnce()
                )
            }
            
            val json = Gson().toJson(backupData)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            
            val userName = if (userId != null) {
                dao.getUserById(userId)?.name ?: "User_$userId"
            } else "ALL"
            
            val fileName = if (isAuto) "Health_AutoBackup_${userName}.json" else "Health_Backup_${userName}_$timestamp.json"
            
            val resolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Files.getContentUri("external")
            }
            
            if (isAuto) {
                val projection = arrayOf(MediaStore.MediaColumns._ID)
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf(fileName)
                try {
                    resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                            val deleteUri = android.content.ContentUris.withAppendedId(collection, id)
                            resolver.delete(deleteUri, null, null)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BackupUtils", "Failed to delete old auto-backup", e)
                }
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }
            
            val uri = resolver.insert(collection, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { 
                    it.write(json.toByteArray())
                }
                
                val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("last_backup_uri", uri.toString()).apply()
                
                val now = System.currentTimeMillis()
                prefs.edit().putLong("global_last_backup_timestamp", now).apply()

                if (!isAuto) {
                    Toast.makeText(context, context.getString(R.string.backup_saved_downloads, fileName), Toast.LENGTH_LONG).show()
                    
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_backup))
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }
            }
        } catch (e: Exception) {
            Log.e("BackupUtils", "Export failed", e)
        }
    }
}
