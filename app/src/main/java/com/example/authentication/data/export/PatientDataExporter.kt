package com.example.authentication.data.export

import android.content.Context
import android.net.Uri
import com.example.authentication.data.local.MedTrackDatabase
import com.example.authentication.data.local.entity.IntakeLogEntity
import com.example.authentication.data.local.entity.MedicationEntity
import com.example.authentication.data.local.model.MedicationWithTimes
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PatientDataExporter(
    private val context: Context,
    private val database: MedTrackDatabase
)
{
    data class ExportResult(val filePath: String)
    data class ImportResult(val medications: Int, val logs: Int)

    suspend fun exportPatient(patientId: String): ExportResult {
        val meds: List<MedicationWithTimes> = database.medicationDao().getAllWithTimesForPatient(patientId)
        val logs: List<IntakeLogEntity> = database.intakeLogDao().getAllForPatient(patientId)

        val root = JSONObject().apply {
            put("patientId", patientId)
            put("medications", meds.toMedicationJson())
            put("logs", logs.toLogsJson())
        }

        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "patient_${patientId}_$timestamp.json")
        file.writeText(root.toString())
        return ExportResult(file.absolutePath)
    }

    suspend fun importPatient(patientId: String, uri: Uri): ImportResult {
        val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalArgumentException("Unable to read selected file")
        val root = JSONObject(content)
        val medicationsJson = root.optJSONArray("medications") ?: JSONArray()
        val logsJson = root.optJSONArray("logs") ?: JSONArray()

        val medEntities = mutableListOf<MedicationEntity>()
        val timeEntities = mutableListOf<com.example.authentication.data.local.entity.MedicationTimeEntity>()
        for (i in 0 until medicationsJson.length()) {
            val medObj = medicationsJson.getJSONObject(i)
            val medId = medObj.getString("id")
            medEntities.add(
                MedicationEntity(
                    id = medId,
                    patientId = patientId,
                    name = medObj.getString("name"),
                    dosage = medObj.getString("dosage"),
                    stomachCondition = medObj.getString("stomachCondition"),
                    startDate = medObj.getLong("startDate"),
                    endDate = if (medObj.isNull("endDate")) null else medObj.getLong("endDate"),
                    frequencyType = medObj.getString("frequencyType"),
                    frequencyValue = if (medObj.isNull("frequencyValue")) null else medObj.getInt("frequencyValue"),
                    notes = medObj.optString("notes", null),
                    alarmToneUri = medObj.optString("alarmToneUri", null),
                    remoteId = medObj.optString("remoteId", null),
                    isSynced = medObj.optBoolean("isSynced", false),
                    selectedDays = medObj.optString("selectedDays", null),
                    createdAt = medObj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = medObj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
            val timesArr = medObj.getJSONArray("times")
            for (j in 0 until timesArr.length()) {
                val timeObj = timesArr.getJSONObject(j)
                timeEntities.add(
                    com.example.authentication.data.local.entity.MedicationTimeEntity(
                        id = timeObj.getString("id"),
                        patientId = patientId,
                        medicationId = medId,
                        timeOfDay = timeObj.getString("timeOfDay")
                    )
                )
            }
        }

        val logEntities = mutableListOf<IntakeLogEntity>()
        for (i in 0 until logsJson.length()) {
            val logObj = logsJson.getJSONObject(i)
            logEntities.add(
                IntakeLogEntity(
                    id = logObj.getString("id"),
                    patientId = patientId,
                    medicationId = logObj.getString("medicationId"),
                    medicationName = logObj.optString("medicationName", null),
                    dosage = logObj.optString("dosage", null),
                    scheduledTime = logObj.getLong("scheduledTime"),
                    takenTime = if (logObj.isNull("takenTime")) null else logObj.getLong("takenTime"),
                    status = logObj.getString("status"),
                    remoteId = logObj.optString("remoteId", null),
                    isSynced = logObj.optBoolean("isSynced", false),
                    createdAt = logObj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        database.withTransaction {
            database.medicationDao().deleteByPatient(patientId)
            database.medicationTimeDao().deleteByPatient(patientId)
            database.intakeLogDao().deleteByPatient(patientId)
            medEntities.forEach { database.medicationDao().upsert(it) }
            database.medicationTimeDao().upsertAll(timeEntities)
            logEntities.forEach { database.intakeLogDao().upsert(it) }
        }

        return ImportResult(medications = medEntities.size, logs = logEntities.size)
    }

    private fun List<MedicationWithTimes>.toMedicationJson(): JSONArray =
        JSONArray().apply {
            this@toMedicationJson.forEach { medWithTimes ->
                put(JSONObject().apply {
                    put("id", medWithTimes.medication.id)
                    put("name", medWithTimes.medication.name)
                    put("patientId", medWithTimes.medication.patientId)
                    put("dosage", medWithTimes.medication.dosage)
                    put("stomachCondition", medWithTimes.medication.stomachCondition)
                    put("startDate", medWithTimes.medication.startDate)
                    put("endDate", medWithTimes.medication.endDate)
                    put("frequencyType", medWithTimes.medication.frequencyType)
                    put("frequencyValue", medWithTimes.medication.frequencyValue)
                    put("notes", medWithTimes.medication.notes)
                    put("alarmToneUri", medWithTimes.medication.alarmToneUri)
                    put("remoteId", medWithTimes.medication.remoteId)
                    put("isSynced", medWithTimes.medication.isSynced)
                    put("selectedDays", medWithTimes.medication.selectedDays)
                    put("createdAt", medWithTimes.medication.createdAt)
                    put("updatedAt", medWithTimes.medication.updatedAt)
                    put("times", JSONArray().apply {
                        medWithTimes.times.forEach { time ->
                            put(JSONObject().apply {
                                put("id", time.id)
                                put("medicationId", time.medicationId)
                                put("patientId", time.patientId)
                                put("timeOfDay", time.timeOfDay)
                            })
                        }
                    })
                })
            }
        }

    private fun List<IntakeLogEntity>.toLogsJson(): JSONArray =
        JSONArray().apply {
            this@toLogsJson.forEach { log ->
                put(JSONObject().apply {
                    put("id", log.id)
                    put("patientId", log.patientId)
                    put("medicationId", log.medicationId)
                    put("medicationName", log.medicationName)
                    put("dosage", log.dosage)
                    put("scheduledTime", log.scheduledTime)
                    put("takenTime", log.takenTime)
                    put("status", log.status)
                    put("remoteId", log.remoteId)
                    put("isSynced", log.isSynced)
                    put("createdAt", log.createdAt)
                })
            }
        }
}
