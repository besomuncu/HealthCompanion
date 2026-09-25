package com.besomuncu.healthcompanion.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import com.besomuncu.healthcompanion.data.HealthDatabase
import com.besomuncu.healthcompanion.data.repository.HealthRepository
import com.besomuncu.healthcompanion.data.model.MedicationLog
import com.besomuncu.healthcompanion.util.AlarmUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val userId = intent.getLongExtra("user_id", -1L)
        val medName = intent.getStringExtra("med_name")
        val notificationId = intent.getIntExtra("notification_id", -1)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationId != -1) {
            notificationManager.cancel(notificationId)
        }

        when (action) {
            "ACTION_TAKEN" -> {
                if (userId != -1L && medName != null) {
                    val dao = HealthDatabase.getDatabase(context).healthDao()
                    val repository = HealthRepository(dao)
                    CoroutineScope(Dispatchers.IO).launch {
                        val meds = repository.getMedsForUserOnce(userId)
                        val med = meds.find { it.name == medName }
                        if (med != null && !med.isTakenToday) {
                            val updatedMed = med.copy(
                                isTakenToday = true,
                                pillCount = if (med.initialPillCount > 0) (med.pillCount - med.dosagePerIntake).coerceAtLeast(0) else med.pillCount
                            )
                            repository.updateMedication(updatedMed)
                            repository.insertMedicationLog(MedicationLog(userId = userId, medicationId = med.id, medName = med.name))
                            
                            com.besomuncu.healthcompanion.util.BackupUtils.triggerAutoBackup(context)

                            // Cancel overdue alarms
                            notificationManager.cancel(med.name.hashCode() + 100)
                        }
                    }
                }
            }
            "ACTION_REMIND_LATER" -> {
                if (userId != -1L && medName != null) {
                    AlarmUtils.scheduleOverdueAlarm(context, medName, userId)
                } else if (userId != -1L) {
                    // For Water/BP, we just reuse the overdue logic for 10 mins
                    val type = intent.getStringExtra("type") ?: "generic"
                    AlarmUtils.scheduleGenericReminder(context, type, userId)
                }
            }
        }
    }
}
