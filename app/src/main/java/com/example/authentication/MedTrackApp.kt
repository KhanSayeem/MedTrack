package com.example.authentication

import android.app.Application
import com.example.authentication.data.local.MedTrackDatabase
import com.example.authentication.data.repository.IntakeLogRepository
import com.example.authentication.data.repository.MedicationRepository
import com.example.authentication.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MedTrackApp : Application() {
    lateinit var database: MedTrackDatabase
        private set

    lateinit var medicationRepository: MedicationRepository
        private set

    lateinit var intakeLogRepository: IntakeLogRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = MedTrackDatabase.build(this)
        medicationRepository = MedicationRepository(database)
        intakeLogRepository = IntakeLogRepository(database)
        initializeReminders()
    }

    private fun initializeReminders() {
        appScope.launch {
            val medications = database.medicationDao().getAllWithTimes()
            ReminderScheduler(this@MedTrackApp).scheduleMedications(medications)
        }
    }
}
