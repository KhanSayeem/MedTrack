package com.example.authentication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.authentication.data.local.dao.IntakeLogDao
import com.example.authentication.data.local.dao.MedicationDao
import com.example.authentication.data.local.dao.MedicationTimeDao
import com.example.authentication.data.local.dao.UserDao
import com.example.authentication.data.local.entity.IntakeLogEntity
import com.example.authentication.data.local.entity.MedicationEntity
import com.example.authentication.data.local.entity.MedicationTimeEntity
import com.example.authentication.data.local.entity.UserEntity

@Database(
    entities = [
        MedicationEntity::class,
        MedicationTimeEntity::class,
        IntakeLogEntity::class,
        UserEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class MedTrackDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationTimeDao(): MedicationTimeDao
    abstract fun intakeLogDao(): IntakeLogDao
    abstract fun userDao(): UserDao

    companion object {
        private const val DB_NAME = "medtrack.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS intake_logs_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        medication_id TEXT NOT NULL,
                        medication_name TEXT,
                        dosage TEXT,
                        scheduled_time INTEGER NOT NULL,
                        taken_time INTEGER,
                        status TEXT NOT NULL,
                        remote_id TEXT,
                        is_synced INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO intake_logs_new (
                        id,
                        medication_id,
                        scheduled_time,
                        taken_time,
                        status,
                        remote_id,
                        is_synced,
                        created_at
                    )
                    SELECT
                        id,
                        medication_id,
                        scheduled_time,
                        taken_time,
                        status,
                        remote_id,
                        is_synced,
                        created_at
                    FROM intake_logs
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE intake_logs")
                db.execSQL("ALTER TABLE intake_logs_new RENAME TO intake_logs")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_intake_logs_medication_id ON intake_logs(medication_id)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medications ADD COLUMN patient_id TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medication_times ADD COLUMN patient_id TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE intake_logs ADD COLUMN patient_id TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                        uid TEXT NOT NULL PRIMARY KEY,
                        email TEXT NOT NULL,
                        role TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun build(context: Context): MedTrackDatabase =
            Room.databaseBuilder(context, MedTrackDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
    }
}
