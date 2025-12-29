package com.example.authentication.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.authentication.data.local.entity.MedicationEntity
import com.example.authentication.data.local.model.MedicationWithTimes
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(medication: MedicationEntity)

    @Update
    suspend fun update(medication: MedicationEntity)

    @Delete
    suspend fun delete(medication: MedicationEntity)

    @Transaction
    @Query("SELECT * FROM medications ORDER BY start_date ASC")
    fun observeAll(): Flow<List<MedicationWithTimes>>

    @Transaction
    @Query("SELECT * FROM medications WHERE patient_id = :patientId ORDER BY start_date ASC")
    fun observeAllForPatient(patientId: String): Flow<List<MedicationWithTimes>>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MedicationEntity?

    @Transaction
    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getWithTimes(id: String): MedicationWithTimes?

    @Transaction
    @Query("SELECT * FROM medications WHERE id = :id AND patient_id = :patientId")
    suspend fun getWithTimesForPatient(id: String, patientId: String): MedicationWithTimes?

    @Transaction
    @Query("SELECT * FROM medications")
    suspend fun getAllWithTimes(): List<MedicationWithTimes>

    @Transaction
    @Query("SELECT * FROM medications WHERE patient_id = :patientId")
    suspend fun getAllWithTimesForPatient(patientId: String): List<MedicationWithTimes>

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM medications WHERE patient_id = :patientId")
    suspend fun deleteByPatient(patientId: String)
}
