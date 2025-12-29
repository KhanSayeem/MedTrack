package com.example.authentication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "patient_id") val patientId: String,
    val name: String,
    val dosage: String,
    @ColumnInfo(name = "stomach_condition") val stomachCondition: String,
    @ColumnInfo(name = "start_date") val startDate: Long,
    @ColumnInfo(name = "end_date") val endDate: Long? = null,
    @ColumnInfo(name = "frequency_type") val frequencyType: String,
    @ColumnInfo(name = "frequency_value") val frequencyValue: Int? = null,
    val notes: String? = null,
    @ColumnInfo(name = "alarm_tone_uri") val alarmToneUri: String? = null,
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
    @ColumnInfo(name = "selected_days") val selectedDays: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
