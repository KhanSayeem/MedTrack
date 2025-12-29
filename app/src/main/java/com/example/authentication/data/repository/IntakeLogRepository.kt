package com.example.authentication.data.repository

import com.example.authentication.data.local.MedTrackDatabase
import com.example.authentication.data.local.entity.IntakeLogEntity
import com.example.authentication.domain.model.IntakeStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class IntakeLogRepository(
    private val database: MedTrackDatabase
) {

    private val intakeLogDao = database.intakeLogDao()

    fun observeLogsBetween(startMillis: Long, endMillis: Long, patientId: String): Flow<List<IntakeLogEntity>> =
        intakeLogDao.observeBetweenForPatient(startMillis, endMillis, patientId)

    fun observeAllLogs(patientId: String): Flow<List<IntakeLogEntity>> = intakeLogDao.observeAllForPatient(patientId)

    fun observeLogsForMedication(medicationId: String, patientId: String): Flow<List<IntakeLogEntity>> =
        intakeLogDao.observeForMedicationAndPatient(medicationId, patientId)

    suspend fun recordIntake(
        patientId: String,
        medicationId: String,
        medicationName: String? = null,
        dosage: String? = null,
        scheduledTimeMillis: Long,
        takenTimeMillis: Long?,
        status: IntakeStatus
    ): String {
        val existing = intakeLogDao.getByMedicationAndTimeForPatient(medicationId, scheduledTimeMillis, patientId)
        val id = existing?.id ?: UUID.randomUUID().toString()
        val log = IntakeLogEntity(
            id = id,
            patientId = patientId,
            medicationId = medicationId,
            medicationName = medicationName ?: existing?.medicationName,
            dosage = dosage ?: existing?.dosage,
            scheduledTime = scheduledTimeMillis,
            takenTime = takenTimeMillis ?: existing?.takenTime,
            status = status.name
        )
        intakeLogDao.upsert(log)
        return id
    }

    suspend fun updateStatus(
        logId: String,
        status: IntakeStatus,
        takenTimeMillis: Long? = null,
        patientId: String
    ) {
        val existing = intakeLogDao.getById(logId) ?: return
        if (existing.patientId != patientId) return
        val updated = existing.copy(
            status = status.name,
            takenTime = takenTimeMillis ?: existing.takenTime
        )
        intakeLogDao.upsert(updated)
    }

    suspend fun clearForMedication(medicationId: String, patientId: String) {
        intakeLogDao.deleteByMedication(medicationId)
    }

    suspend fun clearForPatient(patientId: String) {
        intakeLogDao.deleteByPatient(patientId)
    }

    suspend fun getLogsForPatient(patientId: String): List<IntakeLogEntity> =
        intakeLogDao.getAllForPatient(patientId)
}
