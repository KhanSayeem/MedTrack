package com.example.authentication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.authentication.data.local.entity.MedicationTimeEntity

@Dao
interface MedicationTimeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(times: List<MedicationTimeEntity>)

    @Query("SELECT * FROM medication_times WHERE medication_id = :medicationId")
    suspend fun getByMedicationId(medicationId: String): List<MedicationTimeEntity>

    @Query("SELECT * FROM medication_times WHERE medication_id = :medicationId AND patient_id = :patientId")
    suspend fun getByMedicationIdForPatient(medicationId: String, patientId: String): List<MedicationTimeEntity>

    @Query("DELETE FROM medication_times WHERE medication_id = :medicationId")
    suspend fun deleteByMedicationId(medicationId: String)

    @Query("DELETE FROM medication_times WHERE patient_id = :patientId")
    suspend fun deleteByPatient(patientId: String)
}
