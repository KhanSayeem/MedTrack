package com.example.authentication.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.authentication.data.local.MedTrackDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val db = MedTrackDatabase.build(context.applicationContext)
            val medications = db.medicationDao().getAllWithTimes()
            ReminderScheduler(context).scheduleMedications(medications)
            pendingResult.finish()
        }
    }
}
