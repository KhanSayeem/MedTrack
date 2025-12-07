package com.example.authentication.ui.home

import androidx.annotation.ColorRes

data class HomeMedicationItem(
    val id: String,
    val medicationId: String,
    val timeOfDay: String,
    val name: String,
    val dosage: String,
    val stomachCondition: String,
    val frequency: String,
    val dateRange: String,
    val timesText: String,
    val notes: String?,
    val status: Status,
    val scheduledTimeMillis: Long
) {
    enum class Status {
        TAKEN,
        DUE_SOON,
        MISSED,
        SCHEDULED,
        SNOOZED,
        SKIPPED
    }
}
