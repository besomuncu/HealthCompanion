package com.besomuncu.healthcompanion.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.besomuncu.healthcompanion.MainActivity
import com.besomuncu.healthcompanion.R
import com.besomuncu.healthcompanion.data.HealthDatabase
import kotlinx.coroutines.flow.first
import java.util.*

class BpReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val userId = inputData.getLong("user_id", -1L)
        val prefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val activeUserId = prefs.getLong("active_user_id", -1L)

        if (userId != -1L && userId != activeUserId) {
            // This worker is for a user who is no longer active
            return Result.success()
        }

        val dao = HealthDatabase.getDatabase(applicationContext).healthDao()
        val settings = if (userId != -1L) dao.getSettingsForUserOnce(userId) else null
        val locale = if (settings?.language == "Turkish") Locale("tr") else Locale("en")
        val config = Configuration(applicationContext.resources.configuration)
        config.setLocale(locale)
        val localizedContext = applicationContext.createConfigurationContext(config)

        val channelId = "bp_reminders"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = localizedContext.getString(R.string.notif_bp_title)
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("navigate_to", "blood_pressure")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            2001,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val laterIntent = Intent(applicationContext, com.besomuncu.healthcompanion.receiver.NotificationActionReceiver::class.java).apply {
            action = "ACTION_REMIND_LATER"
            putExtra("type", "bp")
            putExtra("user_id", userId)
            putExtra("notification_id", 2001)
        }
        val laterPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            20012,
            laterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(localizedContext.getString(R.string.notif_bp_title))
            .setContentText(localizedContext.getString(R.string.notif_bp_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_view, localizedContext.getString(R.string.notif_action_open), pendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, localizedContext.getString(R.string.notif_action_remind_later), laterPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2001, notification)

        return Result.success()
    }
}
