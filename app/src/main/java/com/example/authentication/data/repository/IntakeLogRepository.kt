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

    fun observeLogsBetween(startMillis: Long, endMillis: Long): Flow<List<IntakeLogEntity>> =
        intakeLogDao.observeBetween(startMillis, endMillis)

    fun observeAllLogs(): Flow<List<IntakeLogEntity>> = intakeLogDao.observeAll()

    fun observeLogsForMedication(medicationId: String): Flow<List<IntakeLogEntity>> =
        intakeLogDao.observeForMedication(medicationId)

    suspend fun recordIntake(
        medicationId: String,
        medicationName: String? = null,
        dosage: String? = null,
        scheduledTimeMillis: Long,
        takenTimeMillis: Long?,
        status: IntakeStatus
    ): String {
        val existing = intakeLogDao.getByMedicationAndTime(medicationId, scheduledTimeMillis)
        val id = existing?.id ?: UUID.randomUUID().toString()
        val log = IntakeLogEntity(
            id = id,
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
        takenTimeMillis: Long? = null
    ) {
        val existing = intakeLogDao.getById(logId) ?: return
        val updated = existing.copy(
            status = status.name,
            takenTime = takenTimeMillis ?: existing.takenTime
        )
        intakeLogDao.upsert(updated)
    }

    suspend fun clearForMedication(medicationId: String) {
        intakeLogDao.deleteByMedication(medicationId)
    }
}
