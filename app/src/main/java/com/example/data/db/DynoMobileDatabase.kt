package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    TestEntity::class,
    TestSampleEntity::class,
    TestRevisionEntity::class,
    VehicleEntity::class,
    SimulationEntity::class,
    SettingEntity::class
  ],
  version = 2,
  exportSchema = false
)
abstract class DynoMobileDatabase : RoomDatabase() {
  abstract fun testDao(): TestDao
  abstract fun testSampleDao(): TestSampleDao
  abstract fun testRevisionDao(): TestRevisionDao
  abstract fun vehicleDao(): VehicleDao
  abstract fun simulationDao(): SimulationDao
  abstract fun settingDao(): SettingDao

  companion object {
    @Volatile
    private var INSTANCE: DynoMobileDatabase? = null

    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `testRevisions` (
            `revisionId` TEXT NOT NULL,
            `testResultId` TEXT NOT NULL,
            `revisionNumber` INTEGER NOT NULL,
            `createdAt` TEXT NOT NULL,
            `reason` TEXT NOT NULL,
            `note` TEXT,
            `previousConfigurationJson` TEXT NOT NULL,
            `correctedConfigurationJson` TEXT NOT NULL,
            `previousCalculatedResultJson` TEXT NOT NULL,
            `correctedCalculatedResultJson` TEXT NOT NULL,
            PRIMARY KEY(`revisionId`)
          )
          """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_testRevisions_testResultId` ON `testRevisions` (`testResultId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_testRevisions_createdAt` ON `testRevisions` (`createdAt`)")
      }
    }

    fun getDatabase(context: Context): DynoMobileDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          DynoMobileDatabase::class.java,
          "DynoMobileDB"
        )
          .addMigrations(MIGRATION_1_2)
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
