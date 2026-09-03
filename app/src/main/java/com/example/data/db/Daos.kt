package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TestDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTest(test: TestEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTests(tests: List<TestEntity>): List<Long>

  @Update
  suspend fun updateTest(test: TestEntity): Int

  @Query("SELECT * FROM tests WHERE id = :id LIMIT 1")
  suspend fun getTestById(id: String): TestEntity?

  @Query("SELECT * FROM tests WHERE id = :id LIMIT 1")
  fun getTestByIdFlow(id: String): Flow<TestEntity?>

  @Query("SELECT * FROM tests ORDER BY createdAt DESC")
  fun getAllTestsFlow(): Flow<List<TestEntity>>

  @Query("SELECT * FROM tests ORDER BY createdAt DESC")
  suspend fun getAllTests(): List<TestEntity>

  @Query("SELECT * FROM tests WHERE status = 'completed' ORDER BY createdAt DESC")
  fun getCompletedTestsFlow(): Flow<List<TestEntity>>

  @Query("SELECT * FROM tests WHERE status = 'completed' ORDER BY createdAt DESC")
  suspend fun getCompletedTests(): List<TestEntity>

  @Query("SELECT * FROM tests WHERE status = 'recording' OR status = 'interrupted' ORDER BY createdAt DESC")
  suspend fun getIncompleteTests(): List<TestEntity>

  @Query("SELECT * FROM tests WHERE status = 'recording' OR status = 'interrupted' ORDER BY createdAt DESC")
  fun getIncompleteTestsFlow(): Flow<List<TestEntity>>

  @Query("DELETE FROM tests WHERE id = :id")
  suspend fun deleteTestById(id: String): Int

  @Query("DELETE FROM tests")
  suspend fun deleteAllTests(): Int

  @Query("SELECT COUNT(*) FROM tests")
  suspend fun getTestCount(): Int
}

@Dao
interface TestSampleDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSample(sample: TestSampleEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSamples(samples: List<TestSampleEntity>): List<Long>

  @Query("SELECT * FROM testSamples WHERE testId = :testId ORDER BY sampleIndex ASC")
  suspend fun getSamplesForTest(testId: String): List<TestSampleEntity>

  @Query("SELECT * FROM testSamples WHERE testId = :testId ORDER BY sampleIndex ASC")
  fun getSamplesForTestFlow(testId: String): Flow<List<TestSampleEntity>>

  @Query("DELETE FROM testSamples WHERE testId = :testId")
  suspend fun deleteSamplesForTest(testId: String): Int

  @Transaction
  suspend fun replaceSamplesForTest(testId: String, samples: List<TestSampleEntity>) {
    deleteSamplesForTest(testId)
    if (samples.isNotEmpty()) {
      insertSamples(samples)
    }
  }

  @Query("DELETE FROM testSamples")
  suspend fun deleteAllSamples(): Int

  @Query("SELECT COUNT(*) FROM testSamples WHERE testId = :testId")
  suspend fun countSamplesForTest(testId: String): Int
}

@Dao
interface TestRevisionDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRevision(revision: TestRevisionEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRevisions(revisions: List<TestRevisionEntity>): List<Long>

  @Query("SELECT * FROM testRevisions WHERE testResultId = :testResultId ORDER BY revisionNumber DESC")
  suspend fun getRevisionsForTest(testResultId: String): List<TestRevisionEntity>

  @Query("SELECT * FROM testRevisions WHERE testResultId = :testResultId ORDER BY revisionNumber DESC")
  fun getRevisionsForTestFlow(testResultId: String): Flow<List<TestRevisionEntity>>

  @Query("SELECT * FROM testRevisions WHERE testResultId = :testResultId ORDER BY revisionNumber ASC LIMIT 1")
  suspend fun getFirstRevisionForTest(testResultId: String): TestRevisionEntity?

  @Query("SELECT COUNT(*) FROM testRevisions WHERE testResultId = :testResultId")
  suspend fun getRevisionCount(testResultId: String): Int

  @Query("DELETE FROM testRevisions WHERE testResultId = :testResultId")
  suspend fun deleteRevisionsForTest(testResultId: String): Int

  @Query("DELETE FROM testRevisions")
  suspend fun deleteAllRevisions(): Int
}

@Dao
interface VehicleDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVehicle(vehicle: VehicleEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVehicles(vehicles: List<VehicleEntity>): List<Long>

  @Query("SELECT * FROM vehicles ORDER BY name ASC")
  suspend fun getAllVehicles(): List<VehicleEntity>

  @Query("SELECT * FROM vehicles ORDER BY name ASC")
  fun getAllVehiclesFlow(): Flow<List<VehicleEntity>>

  @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
  suspend fun getVehicleById(id: String): VehicleEntity?

  @Query("DELETE FROM vehicles WHERE id = :id")
  suspend fun deleteVehicleById(id: String): Int

  @Query("DELETE FROM vehicles")
  suspend fun deleteAllVehicles(): Int
}

@Dao
interface SimulationDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSimulation(simulation: SimulationEntity): Long

  @Query("SELECT * FROM simulations ORDER BY createdAt DESC")
  suspend fun getAllSimulations(): List<SimulationEntity>

  @Query("SELECT * FROM simulations WHERE id = :id LIMIT 1")
  suspend fun getSimulationById(id: String): SimulationEntity?

  @Query("DELETE FROM simulations WHERE id = :id")
  suspend fun deleteSimulationById(id: String): Int
}

@Dao
interface SettingDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun setSetting(setting: SettingEntity): Long

  @Query("SELECT value FROM settings WHERE `key` = :key LIMIT 1")
  suspend fun getSetting(key: String): String?

  @Query("DELETE FROM settings WHERE `key` = :key")
  suspend fun deleteSetting(key: String): Int
}
