package com.besomuncu.healthcompanion.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.besomuncu.healthcompanion.worker.WaterReminderWorker
import com.besomuncu.healthcompanion.worker.BpReminderWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data

class DelayedReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("type") ?: return
        val userId = intent.getLongExtra("user_id", -1L)
        if (userId == -1L) return

        val data = Data.Builder().putLong("user_id", userId).build()
        val request = if (type == "water") {
            OneTimeWorkRequestBuilder<WaterReminderWorker>().setInputData(data).build()
        } else {
            OneTimeWorkRequestBuilder<BpReminderWorker>().setInputData(data).build()
        }
        WorkManager.getInstance(context).enqueue(request)
    }
}
