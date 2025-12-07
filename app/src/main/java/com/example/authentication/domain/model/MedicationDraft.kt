package com.example.authentication.domain.model

data class MedicationDraft(
    val name: String,
    val dosage: String,
    val stomachCondition: String,
    val startDateMillis: Long,
    val endDateMillis: Long? = null,
    val frequencyType: String,
    val frequencyValue: Int? = null,
    val notes: String? = null,
    val selectedDays: List<Int>? = null,
    val alarmToneUri: String? = null,
    val timesOfDay: List<String>
)
