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
import com.besomuncu.healthcompanion.data.repository.HealthRepository
import com.besomuncu.healthcompanion.util.AlarmUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class OverdueReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medName = intent.getStringExtra("med_name") ?: return
        val userId = intent.getLongExtra("user_id", -1)
        if (userId == -1L) return

        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val activeUserId = prefs.getLong("active_user_id", -1L)

        if (userId != activeUserId) {
            // This reminder was for a user who is no longer active in the app
            return
        }

        val dao = HealthDatabase.getDatabase(context).healthDao()
        val repository = HealthRepository(dao)

        CoroutineScope(Dispatchers.IO).launch {
            val settings = if (userId != -1L) dao.getSettingsForUserOnce(userId) else null
            val locale = if (settings?.language == "Turkish") Locale("tr") else Locale("en")
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            val localizedContext = context.createConfigurationContext(config)

            val meds = repository.getMedsForUserOnce(userId)
            val med = meds.find { it.name == medName }

            if (med != null && !med.isTakenToday) {
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
                    medName.hashCode() + 100,
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val takenIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = "ACTION_TAKEN"
                    putExtra("med_name", medName)
                    putExtra("user_id", userId)
                    putExtra("notification_id", medName.hashCode() + 100)
                }
                val takenPendingIntent = PendingIntent.getBroadcast(
                    context,
                    medName.hashCode() + 400,
                    takenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val laterIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = "ACTION_REMIND_LATER"
                    putExtra("med_name", medName)
                    putExtra("user_id", userId)
                    putExtra("notification_id", medName.hashCode() + 100)
                }
                val laterPendingIntent = PendingIntent.getBroadcast(
                    context,
                    medName.hashCode() + 500,
                    laterIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle(localizedContext.getString(R.string.med_warning_title))
                    .setContentText(localizedContext.getString(R.string.med_warning_text, medName))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .addAction(android.R.drawable.ic_menu_edit, localizedContext.getString(R.string.notif_action_taken), takenPendingIntent)
                    .addAction(android.R.drawable.ic_menu_recent_history, localizedContext.getString(R.string.notif_action_remind_later), laterPendingIntent)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(medName.hashCode() + 100, notification)

                // Reschedule for next 10 mins
                AlarmUtils.scheduleOverdueAlarm(context, medName, userId)
            }
        }
    }
}
