package com.besomuncu.healthcompanion.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.core.app.NotificationCompat
import com.besomuncu.healthcompanion.MainActivity
import com.besomuncu.healthcompanion.R
import com.besomuncu.healthcompanion.data.HealthDatabase
import com.besomuncu.healthcompanion.util.AlarmUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class MedicationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medName = intent.getStringExtra("med_name") ?: "Medication"
        val userId = intent.getLongExtra("user_id", -1)

        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val activeUserId = prefs.getLong("active_user_id", -1L)

        if (userId != -1L && userId != activeUserId) {
            // This alarm was for a user who is no longer active in the app
            return
        }

        val dao = HealthDatabase.getDatabase(context).healthDao()
        
        CoroutineScope(Dispatchers.IO).launch {
            val settings = if (userId != -1L) dao.getSettingsForUserOnce(userId) else null
            val locale = if (settings?.language == "Turkish") Locale("tr") else Locale("en")
            
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            val localizedContext = context.createConfigurationContext(config)

            val channelId = "medication_reminders"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "Medication Reminders", NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }

            val mainIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("med_name", medName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                medName.hashCode(),
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val takenIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = "ACTION_TAKEN"
                putExtra("med_name", medName)
                putExtra("user_id", userId)
                putExtra("notification_id", medName.hashCode())
            }
            val takenPendingIntent = PendingIntent.getBroadcast(
                context,
                medName.hashCode() + 200,
                takenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val laterIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = "ACTION_REMIND_LATER"
                putExtra("med_name", medName)
                putExtra("user_id", userId)
                putExtra("notification_id", medName.hashCode())
            }
            val laterPendingIntent = PendingIntent.getBroadcast(
                context,
                medName.hashCode() + 300,
                laterIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(localizedContext.getString(R.string.notif_med_title))
                .setContentText(localizedContext.getString(R.string.notif_med_text, medName))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_menu_edit, localizedContext.getString(R.string.notif_action_taken), takenPendingIntent)
                .addAction(android.R.drawable.ic_menu_recent_history, localizedContext.getString(R.string.notif_action_remind_later), laterPendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(medName.hashCode(), notification)

            // Schedule Nagging if not taken
            if (userId != -1L) {
                AlarmUtils.scheduleOverdueAlarm(context, medName, userId)
            }
        }
    }
}
