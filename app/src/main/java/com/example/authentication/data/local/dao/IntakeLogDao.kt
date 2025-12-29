package com.example.authentication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.authentication.data.local.entity.IntakeLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IntakeLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: IntakeLogEntity)

    @Update
    suspend fun update(log: IntakeLogEntity)

    @Query("SELECT * FROM intake_logs WHERE medication_id = :medicationId ORDER BY scheduled_time DESC")
    fun observeForMedication(medicationId: String): Flow<List<IntakeLogEntity>>

    @Query("SELECT * FROM intake_logs WHERE medication_id = :medicationId AND patient_id = :patientId ORDER BY scheduled_time DESC")
    fun observeForMedicationAndPatient(medicationId: String, patientId: String): Flow<List<IntakeLogEntity>>

    @Query("SELECT * FROM intake_logs WHERE scheduled_time BETWEEN :startMillis AND :endMillis ORDER BY scheduled_time DESC")
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<IntakeLogEntity>>

    @Query("SELECT * FROM intake_logs WHERE scheduled_time BETWEEN :startMillis AND :endMillis AND patient_id = :patientId ORDER BY scheduled_time DESC")
    fun observeBetweenForPatient(startMillis: Long, endMillis: Long, patientId: String): Flow<List<IntakeLogEntity>>

    @Query("SELECT * FROM intake_logs ORDER BY scheduled_time DESC")
    fun observeAll(): Flow<List<IntakeLogEntity>>

    @Query("SELECT * FROM intake_logs WHERE patient_id = :patientId ORDER BY scheduled_time DESC")
    fun observeAllForPatient(patientId: String): Flow<List<IntakeLogEntity>>

    @Query("SELECT * FROM intake_logs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): IntakeLogEntity?

    @Query("SELECT * FROM intake_logs WHERE medication_id = :medicationId AND scheduled_time = :scheduledTime LIMIT 1")
    suspend fun getByMedicationAndTime(medicationId: String, scheduledTime: Long): IntakeLogEntity?

    @Query("SELECT * FROM intake_logs WHERE patient_id = :patientId")
    suspend fun getAllForPatient(patientId: String): List<IntakeLogEntity>

    @Query("SELECT * FROM intake_logs WHERE medication_id = :medicationId AND scheduled_time = :scheduledTime AND patient_id = :patientId LIMIT 1")
    suspend fun getByMedicationAndTimeForPatient(medicationId: String, scheduledTime: Long, patientId: String): IntakeLogEntity?

    @Query("DELETE FROM intake_logs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM intake_logs WHERE medication_id = :medicationId")
    suspend fun deleteByMedication(medicationId: String)

    @Query("DELETE FROM intake_logs WHERE patient_id = :patientId")
    suspend fun deleteByPatient(patientId: String)
}
