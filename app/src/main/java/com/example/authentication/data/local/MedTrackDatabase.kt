package com.example.authentication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.authentication.data.local.dao.IntakeLogDao
import com.example.authentication.data.local.dao.MedicationDao
import com.example.authentication.data.local.dao.MedicationTimeDao
import com.example.authentication.data.local.entity.IntakeLogEntity
import com.example.authentication.data.local.entity.MedicationEntity
import com.example.authentication.data.local.entity.MedicationTimeEntity

@Database(
    entities = [
        MedicationEntity::class,
        MedicationTimeEntity::class,
        IntakeLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class MedTrackDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationTimeDao(): MedicationTimeDao
    abstract fun intakeLogDao(): IntakeLogDao

    companion object {
        private const val DB_NAME = "medtrack.db"

        fun build(context: Context): MedTrackDatabase =
            Room.databaseBuilder(context, MedTrackDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration()
                .build()
    }
}
