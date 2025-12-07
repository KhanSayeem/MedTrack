package com.example.authentication.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.authentication.data.local.entity.MedicationEntity
import com.example.authentication.data.local.entity.MedicationTimeEntity

data class MedicationWithTimes(
    @Embedded val medication: MedicationEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "medication_id"
    )
    val times: List<MedicationTimeEntity>
)
