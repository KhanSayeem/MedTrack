package com.example.authentication.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.authentication.MainActivity
import com.example.authentication.MedTrackApp
import com.example.authentication.R
import com.example.authentication.data.local.MedTrackDatabase
import com.example.authentication.data.repository.IntakeLogRepository
import com.example.authentication.domain.model.IntakeStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REMINDER -> handleReminder(context, intent)
            ACTION_MARK_TAKEN -> handleTaken(context, intent)
            ACTION_SNOOZE -> handleSnooze(context, intent)
            ACTION_OPEN_APP -> handleOpen(context, intent)
        }
    }

    private fun handleReminder(context: Context, intent: Intent) {
        val data = intent.toPayload() ?: return
        ensureChannel(context)

        val notificationId = ReminderScheduler.requestCode(data.medicationId, data.timeOfDay, "notify")
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val takenPending = actionPendingIntent(context, data, ACTION_MARK_TAKEN, "taken")
        val snoozePending = actionPendingIntent(context, data, ACTION_SNOOZE, "snooze_action")
        val openPending = actionPendingIntent(context, data, ACTION_OPEN_APP, "open_action")

        val title = if (data.isPreReminder) {
            context.getString(R.string.reminder_due_soon, data.medicationName)
        } else {
            context.getString(R.string.reminder_time_to_take, data.medicationName)
        }
        val description = context.getString(R.string.reminder_dosage_format, data.dosage, data.timeOfDay)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .setAutoCancel(false)
            .setOngoing(!data.isPreReminder)
            .addAction(R.drawable.ic_launcher_foreground, context.getString(R.string.action_taken), takenPending)
            .addAction(R.drawable.ic_launcher_foreground, context.getString(R.string.action_snooze), snoozePending)
            .addAction(R.drawable.ic_launcher_foreground, context.getString(R.string.action_open), openPending)

        NotificationManagerCompat.from(context).notify(data.medicationId, notificationId, builder.build())

        if (!data.isPreReminder) {
            ReminderScheduler(context).scheduleNextDay(
                medicationId = data.medicationId,
                medicationName = data.medicationName,
                dosage = data.dosage,
                timeOfDay = data.timeOfDay,
                currentScheduledTimeMillis = data.scheduledTimeMillis,
                startDateMillis = data.startDateMillis,
                endDateMillis = data.endDateMillis
            )
        }
    }

    private fun handleTaken(context: Context, intent: Intent) {
        val data = intent.toPayload() ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            provideIntakeLogRepository(context).recordIntake(
                medicationId = data.medicationId,
                scheduledTimeMillis = data.scheduledTimeMillis,
                takenTimeMillis = System.currentTimeMillis(),
                status = IntakeStatus.TAKEN
            )
            ReminderScheduler(context).cancelReminder(data.medicationId, data.timeOfDay)
            ReminderScheduler(context).scheduleNextDay(
                medicationId = data.medicationId,
                medicationName = data.medicationName,
                dosage = data.dosage,
                timeOfDay = data.timeOfDay,
                currentScheduledTimeMillis = data.scheduledTimeMillis,
                startDateMillis = data.startDateMillis,
                endDateMillis = data.endDateMillis
            )
            cancelNotification(context, data)
            pendingResult.finish()
        }
    }

    private fun handleSnooze(context: Context, intent: Intent) {
        val data = intent.toPayload() ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            provideIntakeLogRepository(context).recordIntake(
                medicationId = data.medicationId,
                scheduledTimeMillis = data.scheduledTimeMillis,
                takenTimeMillis = null,
                status = IntakeStatus.SNOOZED
            )
            ReminderScheduler(context).scheduleSnooze(
                medicationId = data.medicationId,
                medicationName = data.medicationName,
                dosage = data.dosage,
                timeOfDay = data.timeOfDay,
                scheduledTimeMillis = data.scheduledTimeMillis,
                startDateMillis = data.startDateMillis,
                endDateMillis = data.endDateMillis
            )
            cancelNotification(context, data)
            pendingResult.finish()
        }
    }

    private fun handleOpen(context: Context, intent: Intent) {
        val data = intent.toPayload() ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            provideIntakeLogRepository(context).recordIntake(
                medicationId = data.medicationId,
                scheduledTimeMillis = data.scheduledTimeMillis,
                takenTimeMillis = null,
                status = IntakeStatus.SCHEDULED
            )
            cancelNotification(context, data)
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            ContextCompat.startActivity(context, launchIntent, null)
            pendingResult.finish()
        }
    }

    private fun actionPendingIntent(
        context: Context,
        data: ReminderPayload,
        action: String,
        suffix: String
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            putExtras(data.toBundle())
        }
        return PendingIntent.getBroadcast(
            context,
            ReminderScheduler.requestCode(data.medicationId, data.timeOfDay, suffix),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelNotification(context: Context, data: ReminderPayload) {
        val notificationId = ReminderScheduler.requestCode(data.medicationId, data.timeOfDay, "notify")
        NotificationManagerCompat.from(context).cancel(data.medicationId, notificationId)
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
            enableLights(true)
            lightColor = Color.BLUE
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    private fun provideIntakeLogRepository(context: Context): IntakeLogRepository {
        val app = context.applicationContext as? MedTrackApp
        return app?.intakeLogRepository ?: IntakeLogRepository(MedTrackDatabase.build(context.applicationContext))
    }

    private fun Intent.toPayload(): ReminderPayload? {
        val medicationId = getStringExtra(EXTRA_MEDICATION_ID) ?: return null
        val medicationName = getStringExtra(EXTRA_MEDICATION_NAME) ?: return null
        val dosage = getStringExtra(EXTRA_DOSAGE) ?: return null
        val timeOfDay = getStringExtra(EXTRA_TIME_OF_DAY) ?: return null
        val scheduledTime = getLongExtra(EXTRA_SCHEDULED_TIME, -1L)
        val startDate = getLongExtra(EXTRA_START_DATE, -1L)
        val endDateRaw = getLongExtra(EXTRA_END_DATE, -1L)
        val isPreReminder = getBooleanExtra(EXTRA_IS_PRE_REMINDER, false)
        if (scheduledTime <= 0 || startDate <= 0) return null
        val endDate = if (endDateRaw <= 0) null else endDateRaw
        return ReminderPayload(
            medicationId = medicationId,
            medicationName = medicationName,
            dosage = dosage,
            timeOfDay = timeOfDay,
            scheduledTimeMillis = scheduledTime,
            startDateMillis = startDate,
            endDateMillis = endDate,
            isPreReminder = isPreReminder
        )
    }

    data class ReminderPayload(
        val medicationId: String,
        val medicationName: String,
        val dosage: String,
        val timeOfDay: String,
        val scheduledTimeMillis: Long,
        val startDateMillis: Long,
        val endDateMillis: Long?,
        val isPreReminder: Boolean
    ) {
        fun toBundle() = android.os.Bundle().apply {
            putString(EXTRA_MEDICATION_ID, medicationId)
            putString(EXTRA_MEDICATION_NAME, medicationName)
            putString(EXTRA_DOSAGE, dosage)
            putString(EXTRA_TIME_OF_DAY, timeOfDay)
            putLong(EXTRA_SCHEDULED_TIME, scheduledTimeMillis)
            putLong(EXTRA_START_DATE, startDateMillis)
            putLong(EXTRA_END_DATE, endDateMillis ?: -1L)
            putBoolean(EXTRA_IS_PRE_REMINDER, isPreReminder)
        }
    }

    companion object {
        const val ACTION_REMINDER = "com.example.authentication.REMINDER"
        const val ACTION_MARK_TAKEN = "com.example.authentication.ACTION_TAKEN"
        const val ACTION_SNOOZE = "com.example.authentication.ACTION_SNOOZE"
        const val ACTION_OPEN_APP = "com.example.authentication.ACTION_OPEN_APP"

        const val EXTRA_MEDICATION_ID = "extra_medication_id"
        const val EXTRA_MEDICATION_NAME = "extra_medication_name"
        const val EXTRA_DOSAGE = "extra_dosage"
        const val EXTRA_TIME_OF_DAY = "extra_time_of_day"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
        const val EXTRA_START_DATE = "extra_start_date"
        const val EXTRA_END_DATE = "extra_end_date"
        const val EXTRA_IS_PRE_REMINDER = "extra_is_pre_reminder"

        const val CHANNEL_ID = "medtrack_reminders"
    }
}
