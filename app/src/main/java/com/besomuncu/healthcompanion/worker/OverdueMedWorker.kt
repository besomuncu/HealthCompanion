package com.besomuncu.healthcompanion.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.besomuncu.healthcompanion.MainActivity
import com.besomuncu.healthcompanion.data.HealthDatabase
import com.besomuncu.healthcompanion.data.repository.HealthRepository
import java.util.*

class OverdueMedWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val dao = HealthDatabase.getDatabase(applicationContext).healthDao()
        val repository = HealthRepository(dao)
        
        val users = repository.getUsersOnce()
        for (user in users) {
            val medications = repository.getMedsForUserOnce(user.id)
            val now = Calendar.getInstance()
            val currentHour = now.get(Calendar.HOUR_OF_DAY)
            val currentMin = now.get(Calendar.MINUTE)
            
            for (med in medications) {
                if (!med.isTakenToday && med.pillCount > 0) {
                    if (isOverdue(med.scheduledTime, currentHour, currentMin, 10)) {
                        sendOverdueNotification(med.name)
                    }
                }
            }
        }
        
        return Result.success()
    }

    private fun isOverdue(scheduledTime: String, currentHour: Int, currentMin: Int, thresholdMins: Int): Boolean {
        val parts = scheduledTime.split(":")
        val sHour = parts[0].toInt()
        val sMin = parts[1].toInt()
        
        val scheduledTotal = sHour * 60 + sMin
        val currentTotal = currentHour * 60 + currentMin
        
        return currentTotal >= scheduledTotal + thresholdMins
    }

    private fun sendOverdueNotification(medName: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "overdue_meds"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Medication Warnings", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        
        val mainIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "dashboard")
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            medName.hashCode() + 100,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Medication Warning")
            .setContentText("Warning: You forgot to take your medication: $medName!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(medName.hashCode() + 100, notification)
    }
}
