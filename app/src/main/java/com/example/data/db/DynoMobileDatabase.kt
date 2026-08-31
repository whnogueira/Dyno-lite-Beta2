package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    TestEntity::class,
    TestSampleEntity::class,
    VehicleEntity::class,
    SimulationEntity::class,
    SettingEntity::class
  ],
  version = 1,
  exportSchema = false
)
abstract class DynoMobileDatabase : RoomDatabase() {
  abstract fun testDao(): TestDao
  abstract fun testSampleDao(): TestSampleDao
  abstract fun vehicleDao(): VehicleDao
  abstract fun simulationDao(): SimulationDao
  abstract fun settingDao(): SettingDao

  companion object {
    @Volatile
    private var INSTANCE: DynoMobileDatabase? = null

    fun getDatabase(context: Context): DynoMobileDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          DynoMobileDatabase::class.java,
          "DynoMobileDB"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
