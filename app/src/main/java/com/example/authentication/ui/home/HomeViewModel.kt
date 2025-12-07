package com.example.authentication.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.authentication.data.local.entity.IntakeLogEntity
import com.example.authentication.data.local.model.MedicationWithTimes
import com.example.authentication.data.repository.MedicationRepository
import com.example.authentication.data.repository.IntakeLogRepository
import com.example.authentication.domain.model.IntakeStatus
import com.example.authentication.reminders.ReminderScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

class HomeViewModel(
    private val medicationRepository: MedicationRepository,
    private val intakeLogRepository: IntakeLogRepository
) : ViewModel() {

    private val _medications = MutableStateFlow<List<HomeMedicationItem>>(emptyList())
    val medications: StateFlow<List<HomeMedicationItem>> = _medications.asStateFlow()

    private val todayRangeFlow = MutableStateFlow(computeTodayRange())
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

    init {
        startMidnightTicker()
        combine(
            medicationRepository.observeMedications(),
            todayRangeFlow.flatMapLatest { range ->
                intakeLogRepository.observeLogsBetween(range.first, range.second)
            },
            todayRangeFlow
        ) { meds, logs, range ->
            buildHomeItems(meds, logs, range)
        }
            .onEach { _medications.value = it }
            .launchIn(viewModelScope)
    }

    fun markTaken(item: HomeMedicationItem) {
        viewModelScope.launch {
            intakeLogRepository.recordIntake(
                medicationId = item.medicationId,
                scheduledTimeMillis = item.scheduledTimeMillis,
                takenTimeMillis = System.currentTimeMillis(),
                status = IntakeStatus.TAKEN
            )
        }
    }

    fun deleteMedication(item: HomeMedicationItem, reminderScheduler: ReminderScheduler) {
        viewModelScope.launch {
            val medicationWithTimes = medicationRepository.getMedication(item.medicationId)
            medicationWithTimes?.times?.forEach { time ->
                reminderScheduler.cancelReminder(item.medicationId, time.timeOfDay)
            }
            intakeLogRepository.clearForMedication(item.medicationId)
            medicationRepository.deleteMedication(item.medicationId)
        }
    }

    private fun buildHomeItems(
        medications: List<MedicationWithTimes>,
        logs: List<IntakeLogEntity>,
        todayRange: Pair<Long, Long>
    ): List<HomeMedicationItem> {
        val now = System.currentTimeMillis()
        val logLookup = logs.associateBy { logKey(it.medicationId, it.scheduledTime) }

        return medications.flatMap { medicationWithTimes ->
            if (!isActiveToday(medicationWithTimes, todayRange)) return@flatMap emptyList()
            medicationWithTimes.times.mapNotNull { timeEntity ->
                val scheduledTime = scheduledTimeForToday(timeEntity.timeOfDay)
                val log = logLookup[logKey(medicationWithTimes.medication.id, scheduledTime)]
                val status = resolveStatus(log, scheduledTime, now)

                HomeMedicationItem(
                    id = "${medicationWithTimes.medication.id}_${timeEntity.id}",
                    medicationId = medicationWithTimes.medication.id,
                    timeOfDay = toDisplayTime(timeEntity.timeOfDay),
                    name = medicationWithTimes.medication.name,
                    dosage = medicationWithTimes.medication.dosage,
                    stomachCondition = medicationWithTimes.medication.stomachCondition,
                    frequency = formatFrequency(
                        medicationWithTimes.medication.frequencyType,
                        medicationWithTimes.medication.frequencyValue,
                        medicationWithTimes.medication.selectedDays
                    ),
                    dateRange = formatDateRange(
                        medicationWithTimes.medication.startDate,
                        medicationWithTimes.medication.endDate
                    ),
                    timesText = formatTimes(medicationWithTimes),
                    notes = medicationWithTimes.medication.notes,
                    status = status,
                    scheduledTimeMillis = scheduledTime
                )
            }
        }.sortedBy { it.scheduledTimeMillis }
    }

    private fun scheduledTimeForToday(timeOfDay: String): Long {
        val (hour, minute) = parseHourMinute(timeOfDay) ?: 12 to 0
        val calendar = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.MINUTE, minute)
            set(Calendar.HOUR_OF_DAY, hour)
        }
        return calendar.timeInMillis
    }

    private fun parseHourMinute(timeOfDay: String): Pair<Int, Int>? {
        val raw = timeOfDay.trim().lowercase(Locale.US)
        if (raw.isEmpty()) return null
        val isPm = raw.contains("pm")
        val isAm = raw.contains("am")
        val digitsOnly = raw.replace(Regex("[^0-9:]"), "")
        val parts = digitsOnly.split(":")

        val (hour, minute) = when {
            parts.size >= 2 -> {
                val h = parts[0].toIntOrNull() ?: return null
                val m = parts[1].take(2).toIntOrNull() ?: 0
                h to m
            }
            digitsOnly.length >= 3 -> {
                val h = digitsOnly.dropLast(2).toIntOrNull() ?: return null
                val m = digitsOnly.takeLast(2).toIntOrNull() ?: 0
                h to m
            }
            else -> {
                val h = digitsOnly.toIntOrNull() ?: return null
                h to 0
            }
        }

        var adjustedHour = hour
        if (isPm && adjustedHour < 12) adjustedHour += 12
        if (isAm && adjustedHour == 12) adjustedHour = 0
        adjustedHour = adjustedHour.coerceIn(0, 23)
        val adjustedMinute = minute.coerceIn(0, 59)
        return adjustedHour to adjustedMinute
    }

    private fun isActiveToday(
        medication: MedicationWithTimes,
        todayRange: Pair<Long, Long>
    ): Boolean {
        val (startOfDay, endOfDay) = todayRange
        val startDate = medication.medication.startDate
        val endDate = medication.medication.endDate
        val startsOnOrBeforeToday = startDate <= endOfDay
        val noEndDateOrFuture = endDate == null || endDate >= startOfDay
        val matchesSelectedDays = medication.medication.selectedDays.isNullOrEmpty() ||
            isTodaySelected(medication.medication.selectedDays)
        return startsOnOrBeforeToday && noEndDateOrFuture && matchesSelectedDays
    }

    private fun logKey(medicationId: String, scheduledTime: Long): String =
        "${medicationId}_$scheduledTime"

    private fun resolveStatus(
        log: IntakeLogEntity?,
        scheduledTime: Long,
        now: Long
    ): HomeMedicationItem.Status {
        return when (log?.status) {
            IntakeStatus.TAKEN.name -> HomeMedicationItem.Status.TAKEN
            IntakeStatus.SNOOZED.name -> HomeMedicationItem.Status.SNOOZED
            IntakeStatus.SKIPPED.name -> HomeMedicationItem.Status.SKIPPED
            IntakeStatus.MISSED.name -> HomeMedicationItem.Status.MISSED
            IntakeStatus.SCHEDULED.name -> HomeMedicationItem.Status.SCHEDULED
            else -> when {
                now > scheduledTime + MISSED_GRACE_MS -> HomeMedicationItem.Status.MISSED
                now >= scheduledTime - DUE_SOON_WINDOW_MS -> HomeMedicationItem.Status.DUE_SOON
                else -> HomeMedicationItem.Status.SCHEDULED
            }
        }
    }

    private fun formatFrequency(
        frequencyType: String,
        frequencyValue: Int?,
        selectedDays: String?
    ): String {
        return when (frequencyType.lowercase(Locale.US)) {
            "hourly" -> "Every ${frequencyValue ?: "X"} hours"
            "selected_days" -> "On ${formatSelectedDays(selectedDays)}"
            else -> "Daily"
        }
    }

    private fun formatSelectedDays(selectedDays: String?): String {
        if (selectedDays.isNullOrEmpty()) return "all days"
        val labels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val indices = selectedDays.split(",").mapNotNull { it.trim().toIntOrNull() }
        return indices.mapNotNull { labels.getOrNull(it % labels.size) }
            .ifEmpty { listOf("all days") }
            .joinToString(", ")
    }

    private fun isTodaySelected(selectedDays: String): Boolean {
        val todayIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 // Sun = 0
        return selectedDays.split(",").mapNotNull { it.trim().toIntOrNull() }.any { it == todayIndex }
    }

    private fun formatDateRange(startDate: Long, endDate: Long?): String {
        val startText = dateFormat.format(startDate)
        val endText = endDate?.let { dateFormat.format(it) } ?: "No end date"
        return "$startText - $endText"
    }

    private fun formatTimes(medication: MedicationWithTimes): String =
        medication.times.sortedBy { it.timeOfDay }.joinToString(", ") { toDisplayTime(it.timeOfDay) }

    private fun computeTodayRange(): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        return start to end
    }

    private fun startMidnightTicker() {
        viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val nextMidnight = computeTodayRange().first + TimeUnit.DAYS.toMillis(1)
                val delayMs = max(0, nextMidnight - now)
                delay(delayMs)
                // Recompute the window so the "today" list refreshes right after midnight.
                todayRangeFlow.value = computeTodayRange()
            }
        }
    }

    private fun toDisplayTime(timeOfDay: String): String {
        val parsed = parseHourMinute(timeOfDay) ?: return timeOfDay
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, parsed.first)
            set(Calendar.MINUTE, parsed.second)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return SimpleDateFormat("h:mm a", Locale.US).format(cal.time)
    }

    class Factory(
        private val medicationRepository: MedicationRepository,
        private val intakeLogRepository: IntakeLogRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(medicationRepository, intakeLogRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    companion object {
        private val DUE_SOON_WINDOW_MS = TimeUnit.MINUTES.toMillis(15)
        private val MISSED_GRACE_MS = TimeUnit.MINUTES.toMillis(15)
    }
}
