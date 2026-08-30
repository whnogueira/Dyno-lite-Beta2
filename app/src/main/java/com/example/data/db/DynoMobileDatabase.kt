package com.example.data.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [VehicleEntity::class, RunResultEntity::class, PendingSessionEntity::class],
    version = 3,
    exportSchema = false
)
abstract class DynoMobileDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun runResultDao(): RunResultDao
    abstract fun pendingSessionDao(): PendingSessionDao

    companion object {
        private const val TAG = "DynoMobileDB"
        const val DATABASE_NAME = "dyno_mobile_database.db"

        @Volatile
        private var INSTANCE: DynoMobileDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_sessions` (
                        `sessionId` TEXT NOT NULL PRIMARY KEY,
                        `vehicleId` TEXT NOT NULL,
                        `vehicleName` TEXT NOT NULL,
                        `startTimeMs` INTEGER NOT NULL,
                        `endTimeMs` INTEGER NOT NULL,
                        `sampleCount` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `errorMessage` TEXT,
                        `errorStage` TEXT,
                        `errorExceptionType` TEXT,
                        `invalidField` TEXT,
                        `samplesJson` TEXT NOT NULL,
                        `lastAttemptTimestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                try {
                    db.execSQL("ALTER TABLE `run_results` ADD COLUMN `qualityStatus` TEXT NOT NULL DEFAULT 'VALID'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `run_results` ADD COLUMN `technicalFailureReason` TEXT DEFAULT NULL")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columnsToAdd = listOf(
                    "totalVehicleMassKg REAL NOT NULL DEFAULT 0.0",
                    "gearUsed TEXT NOT NULL DEFAULT ''",
                    "gearRatioUsed REAL NOT NULL DEFAULT 1.0",
                    "finalDriveUsed REAL NOT NULL DEFAULT 1.0",
                    "drivetrainLossPercent REAL NOT NULL DEFAULT 0.0",
                    "cdUsed REAL NOT NULL DEFAULT 0.34",
                    "frontalAreaUsed REAL NOT NULL DEFAULT 2.10",
                    "crrUsed REAL NOT NULL DEFAULT 0.015",
                    "airDensityUsed REAL NOT NULL DEFAULT 1.225",
                    "slopeModeUsed TEXT NOT NULL DEFAULT 'FLAT'",
                    "slopePercentUsed REAL NOT NULL DEFAULT 0.0",
                    "configurationSnapshotJson TEXT NOT NULL DEFAULT '{}'"
                )
                for (col in columnsToAdd) {
                    try {
                        db.execSQL("ALTER TABLE `run_results` ADD COLUMN $col")
                    } catch (e: Exception) {
                        Log.w(TAG, "Coluna já presente ou erro ao adicionar $col: ${e.message}")
                    }
                }
            }
        }

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
            }
        }

        fun getDatabase(context: Context): DynoMobileDatabase {
            return INSTANCE ?: synchronized(this) {
                checkAndMigrateLegacyDatabase(context)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DynoMobileDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun checkAndMigrateLegacyDatabase(context: Context) {
            try {
                val legacyDbFile = context.getDatabasePath("DynoMobileDB")
                val currentDbFile = context.getDatabasePath(DATABASE_NAME)
                if (legacyDbFile.exists() && !currentDbFile.exists()) {
                    Log.i(TAG, "Detectado banco legado DynoMobileDB. Migrando arquivo para $DATABASE_NAME...")
                    legacyDbFile.copyTo(currentDbFile, overwrite = false)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Aviso ao verificar banco legado: ${e.message}")
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database.vehicleDao())
                    }
                }
            }

            private suspend fun populateInitialData(vehicleDao: VehicleDao) {
                val defaultCar = VehicleEntity(
                    id = "default_golf_gti",
                    name = "VW Golf GTI MK7",
                    brand = "Volkswagen",
                    model = "Golf GTI 2.0 TSI",
                    year = 2018,
                    curbWeightKg = 1370f,
                    driverWeightKg = 80f,
                    additionalWeightKg = 0f,
                    frontalAreaM2 = 2.19f,
                    dragCoefficientCd = 0.31f,
                    drivetrainLossPercent = 14.5f,
                    tireWidthMm = 225,
                    tireProfilePercent = 45,
                    tireRimInches = 17,
                    finalDriveRatio = 3.94f,
                    gearRatiosJson = "[3.78, 2.12, 1.46, 1.03, 0.86, 0.73]",
                    testGearIndex = 2,
                    engineDisplacementCc = 1984,
                    aspiration = "TURBOCHARGED",
                    fuelType = "GASOLINE",
                    revLimitRpm = 6800,
                    isPrimary = true
                )
                vehicleDao.insertVehicle(defaultCar)
            }
        }
    }
}
