package com.example.authentication.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.authentication.data.local.entity.MedicationEntity
import com.example.authentication.data.local.model.MedicationWithTimes
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleMedications(medications: List<MedicationWithTimes>) {
        medications.forEach { scheduleMedication(it) }
    }

    fun scheduleMedication(medication: MedicationWithTimes) {
        val med: MedicationEntity = medication.medication
        val patientId = med.patientId
        medication.times.forEach { time ->
            val nextMainTime = computeNextTriggerMillis(
                time.timeOfDay,
                med.startDate,
                med.endDate
            ) ?: return@forEach

            scheduleReminderPair(
                patientId = patientId,
                medicationId = med.id,
                medicationName = med.name,
                dosage = med.dosage,
                timeOfDay = time.timeOfDay,
                scheduledTimeMillis = nextMainTime,
                startDateMillis = med.startDate,
                endDateMillis = med.endDate
            )
        }
    }

    fun scheduleReminderPair(
        patientId: String,
        medicationId: String,
        medicationName: String,
        dosage: String,
        timeOfDay: String,
        scheduledTimeMillis: Long,
        startDateMillis: Long,
        endDateMillis: Long?
    ) {
        val mainPendingIntent = buildReminderPendingIntent(
            patientId = patientId,
            medicationId = medicationId,
            medicationName = medicationName,
            dosage = dosage,
            timeOfDay = timeOfDay,
            scheduledTimeMillis = scheduledTimeMillis,
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis,
            isPreReminder = false,
            requestCode = requestCode(medicationId, timeOfDay, MAIN_SUFFIX)
        )
        setExactAlarm(scheduledTimeMillis, mainPendingIntent)

        val preTime = scheduledTimeMillis - PRE_REMINDER_MS
        val preTrigger = if (preTime > System.currentTimeMillis()) preTime else scheduledTimeMillis + ONE_DAY_MS - PRE_REMINDER_MS
        val prePendingIntent = buildReminderPendingIntent(
            patientId = patientId,
            medicationId = medicationId,
            medicationName = medicationName,
            dosage = dosage,
            timeOfDay = timeOfDay,
            scheduledTimeMillis = scheduledTimeMillis,
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis,
            isPreReminder = true,
            requestCode = requestCode(medicationId, timeOfDay, PRE_SUFFIX)
        )
        setExactAlarm(preTrigger, prePendingIntent)
    }

    fun scheduleNextDay(
        patientId: String,
        medicationId: String,
        medicationName: String,
        dosage: String,
        timeOfDay: String,
        currentScheduledTimeMillis: Long,
        startDateMillis: Long,
        endDateMillis: Long?
    ) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentScheduledTimeMillis
            add(Calendar.DATE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val (hour, minute) = parseTime(timeOfDay) ?: return
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)

        if (endDateMillis != null && cal.timeInMillis > endDateMillis) return
        scheduleReminderPair(
            patientId = patientId,
            medicationId = medicationId,
            medicationName = medicationName,
            dosage = dosage,
            timeOfDay = timeOfDay,
            scheduledTimeMillis = cal.timeInMillis,
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis
        )
    }

    fun scheduleSnooze(
        patientId: String,
        medicationId: String,
        medicationName: String,
        dosage: String,
        timeOfDay: String,
        scheduledTimeMillis: Long,
        startDateMillis: Long,
        endDateMillis: Long?
    ) {
        val triggerAt = System.currentTimeMillis() + SNOOZE_MS
        val snoozePendingIntent = buildReminderPendingIntent(
            patientId = patientId,
            medicationId = medicationId,
            medicationName = medicationName,
            dosage = dosage,
            timeOfDay = timeOfDay,
            scheduledTimeMillis = scheduledTimeMillis,
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis,
            isPreReminder = false,
            requestCode = requestCode(medicationId, timeOfDay, SNOOZE_SUFFIX)
        )
        setExactAlarm(triggerAt, snoozePendingIntent)
    }

    fun cancelReminder(medicationId: String, timeOfDay: String) {
        listOf(MAIN_SUFFIX, PRE_SUFFIX, SNOOZE_SUFFIX).forEach { suffix ->
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode(medicationId, timeOfDay, suffix),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    private fun setExactAlarm(triggerAt: Long, pendingIntent: PendingIntent) {
        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        if (canScheduleExact) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
                return
            } catch (se: SecurityException) {
                Log.w(TAG, "Exact alarm not permitted, falling back to inexact", se)
            }
        } else {
            Log.w(TAG, "Exact alarm permission not granted, using inexact alarm")
        }

        // Fallback prevents crash when exact alarms are disallowed.
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
    }

    private fun buildReminderPendingIntent(
        patientId: String,
        medicationId: String,
        medicationName: String,
        dosage: String,
        timeOfDay: String,
        scheduledTimeMillis: Long,
        startDateMillis: Long,
        endDateMillis: Long?,
        isPreReminder: Boolean,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
            putExtra(ReminderReceiver.EXTRA_PATIENT_ID, patientId)
            putExtra(ReminderReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(ReminderReceiver.EXTRA_MEDICATION_NAME, medicationName)
            putExtra(ReminderReceiver.EXTRA_DOSAGE, dosage)
            putExtra(ReminderReceiver.EXTRA_TIME_OF_DAY, timeOfDay)
            putExtra(ReminderReceiver.EXTRA_SCHEDULED_TIME, scheduledTimeMillis)
            putExtra(ReminderReceiver.EXTRA_START_DATE, startDateMillis)
            putExtra(ReminderReceiver.EXTRA_END_DATE, endDateMillis ?: -1L)
            putExtra(ReminderReceiver.EXTRA_IS_PRE_REMINDER, isPreReminder)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun computeNextTriggerMillis(
        timeOfDay: String,
        startDateMillis: Long,
        endDateMillis: Long?
    ): Long? {
        val (hour, minute) = parseTime(timeOfDay) ?: return null
        val now = System.currentTimeMillis()

        val startOfStartDate = Calendar.getInstance().apply {
            timeInMillis = startDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endOfEndDate = endDateMillis?.let {
            Calendar.getInstance().apply {
                timeInMillis = it
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
        }

        val candidate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DATE, 1)
        }

        while (candidate.timeInMillis < startOfStartDate.timeInMillis) {
            candidate.add(Calendar.DATE, 1)
        }
        if (endOfEndDate != null && candidate.timeInMillis > endOfEndDate.timeInMillis) return null
        return candidate.timeInMillis
    }

    private fun parseTime(timeOfDay: String): Pair<Int, Int>? {
        return try {
            val parts = timeOfDay.split(":")
            if (parts.size != 2) return null
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            hour to minute
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val MAIN_SUFFIX = "main"
        private const val PRE_SUFFIX = "pre"
        private const val SNOOZE_SUFFIX = "snooze"
        private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
        private val PRE_REMINDER_MS = TimeUnit.MINUTES.toMillis(10)
        private val SNOOZE_MS = TimeUnit.MINUTES.toMillis(5)
        private const val TAG = "ReminderScheduler"

        fun requestCode(medicationId: String, timeOfDay: String, suffix: String): Int =
            (medicationId + timeOfDay + suffix).hashCode()
    }
}
