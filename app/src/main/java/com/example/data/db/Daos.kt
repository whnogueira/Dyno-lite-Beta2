package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY isPrimary DESC, name ASC")
    fun getAllVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun getVehicleById(id: String): VehicleEntity?

    @Query("SELECT * FROM vehicles WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryVehicle(): VehicleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity)

    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity)

    @Query("DELETE FROM vehicles WHERE id = :id")
    suspend fun deleteVehicleById(id: String)

    @Query("UPDATE vehicles SET isPrimary = 0")
    suspend fun clearPrimaryFlags()

    @Query("UPDATE vehicles SET isPrimary = 1 WHERE id = :id")
    suspend fun setPrimaryVehicle(id: String)
}

@Dao
interface RunResultDao {
    @Query("SELECT * FROM run_results ORDER BY testDateTimestamp DESC")
    fun getAllResults(): Flow<List<RunResultEntity>>

    @Query("SELECT * FROM run_results WHERE vehicleId = :vehicleId ORDER BY testDateTimestamp DESC")
    fun getResultsForVehicle(vehicleId: String): Flow<List<RunResultEntity>>

    @Query("SELECT * FROM run_results WHERE id = :id LIMIT 1")
    suspend fun getResultById(id: String): RunResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: RunResultEntity)

    @Query("DELETE FROM run_results WHERE id = :id")
    suspend fun deleteResultById(id: String)
}

@Dao
interface PendingSessionDao {
    @Query("SELECT * FROM pending_sessions WHERE status != 'FINALIZED' ORDER BY startTimeMs DESC")
    fun getPendingSessions(): Flow<List<PendingSessionEntity>>

    @Query("SELECT * FROM pending_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): PendingSessionEntity?

    @Query("SELECT * FROM pending_sessions WHERE status != 'FINALIZED' ORDER BY startTimeMs DESC LIMIT 1")
    suspend fun getLatestPendingSession(): PendingSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PendingSessionEntity)

    @Update
    suspend fun updateSession(session: PendingSessionEntity)

    @Query("UPDATE pending_sessions SET status = 'FINALIZED' WHERE sessionId = :sessionId")
    suspend fun markSessionFinalized(sessionId: String)

    @Query("DELETE FROM pending_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    @Query("DELETE FROM pending_sessions WHERE status = 'FINALIZED'")
    suspend fun clearFinalizedSessions()
}
