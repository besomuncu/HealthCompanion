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
import com.besomuncu.healthcompanion.receiver.NotificationActionReceiver
import kotlinx.coroutines.flow.first
import java.util.*

class WaterReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
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

        val channelId = "water_reminders"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Water Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("navigate_to", "liquids")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            2002,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
            action = "ACTION_OPEN_APP" // Handled by contentIntent, but can be explicit
            putExtra("notification_id", 2002)
        }
        val openPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            20021,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val laterIntent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
            action = "ACTION_REMIND_LATER"
            putExtra("type", "water")
            putExtra("user_id", userId)
            putExtra("notification_id", 2002)
        }
        val laterPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            20022,
            laterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle(localizedContext.getString(R.string.notif_hydration_title))
            .setContentText(localizedContext.getString(R.string.notif_hydration_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_view, localizedContext.getString(R.string.notif_action_open), pendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, localizedContext.getString(R.string.notif_action_remind_later), laterPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2002, notification)

        return Result.success()
    }
}
