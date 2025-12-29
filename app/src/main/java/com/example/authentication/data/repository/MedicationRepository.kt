package com.example.authentication.data.repository

import androidx.room.withTransaction
import com.example.authentication.data.local.MedTrackDatabase
import com.example.authentication.data.local.entity.MedicationEntity
import com.example.authentication.data.local.entity.MedicationTimeEntity
import com.example.authentication.data.local.model.MedicationWithTimes
import com.example.authentication.domain.model.MedicationDraft
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID

class MedicationRepository(
    private val database: MedTrackDatabase
) {

    private val medicationDao = database.medicationDao()
    private val medicationTimeDao = database.medicationTimeDao()

    fun observeMedicationsForPatient(patientId: String): Flow<List<MedicationWithTimes>> =
        medicationDao.observeAllForPatient(patientId)

    suspend fun getMedication(id: String): MedicationWithTimes? = medicationDao.getWithTimes(id)

    suspend fun getMedicationForPatient(id: String, patientId: String): MedicationWithTimes? =
        medicationDao.getWithTimesForPatient(id, patientId)

    suspend fun createMedication(draft: MedicationDraft, patientId: String): String {
        val medicationId = UUID.randomUUID().toString()
        database.withTransaction {
            val medication = draft.toEntity(medicationId, patientId)
            medicationDao.upsert(medication)
            val times = draft.timesOfDay.map { time ->
                MedicationTimeEntity(
                    patientId = patientId,
                    medicationId = medicationId,
                    timeOfDay = time
                )
            }
            medicationTimeDao.deleteByMedicationId(medicationId)
            medicationTimeDao.upsertAll(times)
        }
        return medicationId
    }

    suspend fun updateMedication(id: String, draft: MedicationDraft, patientId: String) {
        database.withTransaction {
            val existing = medicationDao.getById(id) ?: return@withTransaction
            val updated = draft.toEntity(id, patientId).copy(createdAt = existing.createdAt)
            medicationDao.upsert(updated)
            val times = draft.timesOfDay.map { time ->
                MedicationTimeEntity(
                    patientId = patientId,
                    medicationId = id,
                    timeOfDay = time
                )
            }
            medicationTimeDao.deleteByMedicationId(id)
            medicationTimeDao.upsertAll(times)
        }
    }

    suspend fun deleteMedication(id: String) {
        // We archive instead of deleting to preserve intake history.
        endMedication(id)
    }

    suspend fun endMedication(id: String) {
        database.withTransaction {
            val existing = medicationDao.getById(id) ?: return@withTransaction
            val ended = existing.copy(
                endDate = pastEndDate(),
                updatedAt = System.currentTimeMillis()
            )
            medicationDao.upsert(ended)
        }
    }

    suspend fun upsertFromRemote(patientId: String, medications: List<MedicationDraft>) {
        database.withTransaction {
            medicationDao.deleteByPatient(patientId)
            medicationTimeDao.deleteByPatient(patientId)
            medications.forEach { draft ->
                val medId = UUID.randomUUID().toString()
                val medication = draft.toEntity(medId, patientId)
                medicationDao.upsert(medication)
                val times = draft.timesOfDay.map { time ->
                    MedicationTimeEntity(
                        patientId = patientId,
                        medicationId = medId,
                        timeOfDay = time
                    )
                }
                medicationTimeDao.upsertAll(times)
            }
        }
    }

    private fun MedicationDraft.toEntity(id: String, patientId: String): MedicationEntity {
        val selectedDaysValue = selectedDays?.takeIf { it.isNotEmpty() }?.joinToString(",")
        return MedicationEntity(
            id = id,
            patientId = patientId,
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

    private fun pastEndDate(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return cal.timeInMillis
    }
}
