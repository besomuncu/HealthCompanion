package com.besomuncu.healthcompanion.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.besomuncu.healthcompanion.receiver.OverdueReceiver

object AlarmUtils {
    fun scheduleOverdueAlarm(context: Context, medName: String, userId: Long) {
        val intent = Intent(context, OverdueReceiver::class.java).apply {
            putExtra("med_name", medName)
            putExtra("user_id", userId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medName.hashCode() + 100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Schedule for 10 minutes from now
        val triggerTime = System.currentTimeMillis() + (10 * 60 * 1000)
        
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    fun scheduleGenericReminder(context: Context, type: String, userId: Long) {
        val intent = Intent(context, com.besomuncu.healthcompanion.receiver.DelayedReminderReceiver::class.java).apply {
            putExtra("type", type)
            putExtra("user_id", userId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            type.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + (10 * 60 * 1000)
        
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }
}
