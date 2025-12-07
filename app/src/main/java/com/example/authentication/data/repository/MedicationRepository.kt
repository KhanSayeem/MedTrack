package com.example.authentication.data.repository

import androidx.room.withTransaction
import com.example.authentication.data.local.MedTrackDatabase
import com.example.authentication.data.local.entity.MedicationEntity
import com.example.authentication.data.local.entity.MedicationTimeEntity
import com.example.authentication.data.local.model.MedicationWithTimes
import com.example.authentication.domain.model.MedicationDraft
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MedicationRepository(
    private val database: MedTrackDatabase
) {

    private val medicationDao = database.medicationDao()
    private val medicationTimeDao = database.medicationTimeDao()

    fun observeMedications(): Flow<List<MedicationWithTimes>> = medicationDao.observeAll()

    suspend fun getMedication(id: String): MedicationWithTimes? = medicationDao.getWithTimes(id)

    suspend fun createMedication(draft: MedicationDraft): String {
        val medicationId = UUID.randomUUID().toString()
        database.withTransaction {
            val medication = draft.toEntity(medicationId)
            medicationDao.upsert(medication)
            val times = draft.timesOfDay.map { time ->
                MedicationTimeEntity(
                    medicationId = medicationId,
                    timeOfDay = time
                )
            }
            medicationTimeDao.deleteByMedicationId(medicationId)
            medicationTimeDao.upsertAll(times)
        }
        return medicationId
    }

    suspend fun updateMedication(id: String, draft: MedicationDraft) {
        database.withTransaction {
            val existing = medicationDao.getById(id) ?: return@withTransaction
            val updated = draft.toEntity(id).copy(createdAt = existing.createdAt)
            medicationDao.upsert(updated)
            val times = draft.timesOfDay.map { time ->
                MedicationTimeEntity(
                    medicationId = id,
                    timeOfDay = time
                )
            }
            medicationTimeDao.deleteByMedicationId(id)
            medicationTimeDao.upsertAll(times)
        }
    }

    suspend fun deleteMedication(id: String) {
        database.withTransaction {
            medicationTimeDao.deleteByMedicationId(id)
            medicationDao.deleteById(id)
        }
    }

    private fun MedicationDraft.toEntity(id: String): MedicationEntity {
        val selectedDaysValue = selectedDays?.takeIf { it.isNotEmpty() }?.joinToString(",")
        return MedicationEntity(
            id = id,
            name = name,
            dosage = dosage,
            stomachCondition = stomachCondition,
            startDate = startDateMillis,
            endDate = endDateMillis,
            frequencyType = frequencyType,
            frequencyValue = frequencyValue,
            notes = notes,
            alarmToneUri = alarmToneUri,
            selectedDays = selectedDaysValue,
            updatedAt = System.currentTimeMillis()
        )
    }
}
