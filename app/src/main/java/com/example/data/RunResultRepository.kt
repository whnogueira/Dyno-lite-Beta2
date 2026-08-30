package com.example.data

import android.util.Log
import com.example.data.db.PendingSessionDao
import com.example.data.db.PendingSessionEntity
import com.example.data.db.RunResultDao
import com.example.data.db.RunResultEntity
import com.example.model.PendingSession
import com.example.model.RunProcessor
import com.example.model.RunResult
import com.example.model.RunSample
import com.example.model.Vehicle
import com.example.model.finiteOrDefault
import com.example.model.finiteOrNull
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RunResultRepository(
    private val runResultDao: RunResultDao,
    private val pendingSessionDao: PendingSessionDao
) {
    private val gson = Gson()
    private val TAG = "DynoMobile"

    val allResults: Flow<List<RunResult>> = runResultDao.getAllResults().map { entities ->
        entities.map { it.toDomain(gson) }
    }

    val pendingSessions: Flow<List<PendingSession>> = pendingSessionDao.getPendingSessions().map { entities ->
        entities.map { it.toDomain(gson) }
    }

    fun getResultsForVehicle(vehicleId: String): Flow<List<RunResult>> =
        runResultDao.getResultsForVehicle(vehicleId).map { entities ->
            entities.map { it.toDomain(gson) }
        }

    suspend fun getResultById(id: String): RunResult? {
        return runResultDao.getResultById(id)?.toDomain(gson)
    }

    suspend fun insertResult(result: RunResult) {
        val entity = result.toEntity(gson)
        Log.d(TAG, "Inserindo resultado no banco: ${result.id} | Potência: ${entity.peakEnginePowerCv} | Torque: ${entity.peakEngineTorqueKgm}")
        runResultDao.insertResult(entity)
    }

    suspend fun deleteResult(id: String) {
        runResultDao.deleteResultById(id)
    }

    suspend fun savePendingSession(session: PendingSession) {
        val entity = session.toEntity(gson)
        Log.d(TAG, "Gravando sessão pendente no banco: ${session.sessionId} (${session.sampleCount} amostras, status: ${session.status})")
        pendingSessionDao.insertSession(entity)
    }

    suspend fun getPendingSessionById(sessionId: String): PendingSession? {
        return pendingSessionDao.getSessionById(sessionId)?.toDomain(gson)
    }

    suspend fun getLatestPendingSession(): PendingSession? {
        return pendingSessionDao.getLatestPendingSession()?.toDomain(gson)
    }

    suspend fun markSessionFinalized(sessionId: String) {
        Log.i(TAG, "Marcando sessão como FINALIZADA: $sessionId")
        pendingSessionDao.markSessionFinalized(sessionId)
    }

    suspend fun deletePendingSession(sessionId: String) {
        Log.i(TAG, "Descartando sessão pendente: $sessionId")
        pendingSessionDao.deleteSessionById(sessionId)
    }

    /**
     * Recupera uma sessão pendente de forma atômica e segura.
     * Não inicia sensores nem GPS.
     * Não duplica amostras nem cria resultados duplicados.
     */
    suspend fun recoverPendingSession(
        sessionId: String,
        vehicle: Vehicle? = null
    ): Result<RunResult> {
        return try {
            Log.i(TAG, "Iniciando recuperação da sessão $sessionId...")
            val session = pendingSessionDao.getSessionById(sessionId)?.toDomain(gson)
                ?: throw NoSuchElementException("Sessão pendente $sessionId não encontrada no banco")

            val samples = session.samples
            if (samples.isEmpty()) {
                throw IllegalStateException("A sessão $sessionId não contém amostras gravadas")
            }

            Log.i(TAG, "Sessão $sessionId carregada com ${samples.size} amostras. Recalculando...")

            // Processamento, sanitização e cálculo com fallback seguro
            val calculatedResult = RunProcessor.processRun(
                sessionId = session.sessionId,
                vehicle = vehicle,
                rawSamples = samples
            )

            // Persistência no banco (Upsert)
            insertResult(calculatedResult)

            // Marca sessão como finalizada somente após salvar resultado com sucesso
            markSessionFinalized(sessionId)

            Log.i(TAG, "Sessão $sessionId recuperada e finalizada com sucesso!")
            Result.success(calculatedResult)
        } catch (e: Exception) {
            Log.e(TAG, "Falha na recuperação da sessão $sessionId: ${e.message}", e)
            // Atualiza sessão pendente com dados técnicos da falha
            try {
                val existing = pendingSessionDao.getSessionById(sessionId)
                if (existing != null) {
                    val updated = existing.copy(
                        status = "PENDING",
                        errorMessage = e.message ?: "Erro desconhecido",
                        errorStage = "Recuperação",
                        errorExceptionType = e.javaClass.simpleName,
                        lastAttemptTimestamp = System.currentTimeMillis()
                    )
                    pendingSessionDao.updateSession(updated)
                }
            } catch (_: Exception) {}
            Result.failure(e)
        }
    }
}

fun RunResultEntity.toDomain(gson: Gson): RunResult {
    val samples: List<RunSample> = try {
        val type = object : TypeToken<List<RunSample>>() {}.type
        gson.fromJson<List<RunSample>>(samplesJson, type) ?: emptyList()
    } catch (e: Exception) {
        Log.e("DynoMobile", "Erro ao desserializar amostras da entidade ${this.id}: ${e.message}")
        emptyList()
    }

    return RunResult(
        id = id,
        vehicleId = vehicleId,
        vehicleName = vehicleName,
        testDateTimestamp = testDateTimestamp,
        peakEnginePowerCv = peakEnginePowerCv.finiteOrDefault(0f),
        peakEnginePowerRpm = peakEnginePowerRpm,
        peakEnginePowerSpeedKmh = peakEnginePowerSpeedKmh.finiteOrDefault(0f),
        peakWheelPowerCv = peakWheelPowerCv.finiteOrDefault(0f),
        peakEngineTorqueKgm = peakEngineTorqueKgm.finiteOrDefault(0f),
        peakEngineTorqueRpm = peakEngineTorqueRpm,
        peakLongitudinalG = peakLongitudinalG.finiteOrDefault(0f),
        startSpeedKmh = startSpeedKmh.finiteOrDefault(0f),
        endSpeedKmh = endSpeedKmh.finiteOrDefault(0f),
        testGear = testGear,
        durationSeconds = durationSeconds.finiteOrDefault(0f),
        zeroToHundredSeconds = zeroToHundredSeconds?.finiteOrNull(),
        eightyToOneTwentySeconds = eightyToOneTwentySeconds?.finiteOrNull(),
        oneHundredToTwoHundredSeconds = oneHundredToTwoHundredSeconds?.finiteOrNull(),
        quarterMileSeconds = quarterMileSeconds?.finiteOrNull(),
        quarterMileSpeedKmh = quarterMileSpeedKmh?.finiteOrNull(),
        temperatureCelsius = temperatureCelsius.finiteOrDefault(25f),
        pressureHpa = pressureHpa.finiteOrDefault(1013.25f),
        saeCorrectionFactor = saeCorrectionFactor.finiteOrDefault(1f),
        samples = samples,
        qualityStatus = qualityStatus,
        technicalFailureReason = technicalFailureReason
    )
}

fun RunResult.toEntity(gson: Gson): RunResultEntity {
    return RunResultEntity(
        id = id,
        vehicleId = vehicleId,
        vehicleName = vehicleName,
        testDateTimestamp = testDateTimestamp,
        peakEnginePowerCv = peakEnginePowerCv?.finiteOrDefault(0f) ?: 0f,
        peakEnginePowerRpm = peakEnginePowerRpm ?: 0,
        peakEnginePowerSpeedKmh = peakEnginePowerSpeedKmh?.finiteOrDefault(0f) ?: 0f,
        peakWheelPowerCv = peakWheelPowerCv?.finiteOrDefault(0f) ?: 0f,
        peakEngineTorqueKgm = peakEngineTorqueKgm?.finiteOrDefault(0f) ?: 0f,
        peakEngineTorqueRpm = peakEngineTorqueRpm ?: 0,
        peakLongitudinalG = peakLongitudinalG?.finiteOrDefault(0f) ?: 0f,
        startSpeedKmh = startSpeedKmh.finiteOrDefault(0f),
        endSpeedKmh = endSpeedKmh.finiteOrDefault(0f),
        testGear = testGear,
        durationSeconds = durationSeconds.finiteOrDefault(0f),
        zeroToHundredSeconds = zeroToHundredSeconds?.finiteOrNull(),
        eightyToOneTwentySeconds = eightyToOneTwentySeconds?.finiteOrNull(),
        oneHundredToTwoHundredSeconds = oneHundredToTwoHundredSeconds?.finiteOrNull(),
        quarterMileSeconds = quarterMileSeconds?.finiteOrNull(),
        quarterMileSpeedKmh = quarterMileSpeedKmh?.finiteOrNull(),
        temperatureCelsius = temperatureCelsius.finiteOrDefault(25f),
        pressureHpa = pressureHpa.finiteOrDefault(1013.25f),
        saeCorrectionFactor = saeCorrectionFactor.finiteOrDefault(1f),
        samplesJson = try { gson.toJson(samples) } catch (_: Exception) { "[]" },
        qualityStatus = qualityStatus,
        technicalFailureReason = technicalFailureReason
    )
}

fun PendingSessionEntity.toDomain(gson: Gson): PendingSession {
    val samples: List<RunSample> = try {
        val type = object : TypeToken<List<RunSample>>() {}.type
        gson.fromJson<List<RunSample>>(samplesJson, type) ?: emptyList()
    } catch (e: Exception) {
        Log.e("DynoMobile", "Erro ao desserializar amostras da sessão pendente ${this.sessionId}: ${e.message}")
        emptyList()
    }

    return PendingSession(
        sessionId = sessionId,
        vehicleId = vehicleId,
        vehicleName = vehicleName,
        startTimeMs = startTimeMs,
        endTimeMs = endTimeMs,
        sampleCount = sampleCount,
        status = status,
        errorMessage = errorMessage,
        errorStage = errorStage,
        errorExceptionType = errorExceptionType,
        invalidField = invalidField,
        samples = samples,
        lastAttemptTimestamp = lastAttemptTimestamp
    )
}

fun PendingSession.toEntity(gson: Gson): PendingSessionEntity {
    return PendingSessionEntity(
        sessionId = sessionId,
        vehicleId = vehicleId,
        vehicleName = vehicleName,
        startTimeMs = startTimeMs,
        endTimeMs = endTimeMs,
        sampleCount = sampleCount,
        status = status,
        errorMessage = errorMessage,
        errorStage = errorStage,
        errorExceptionType = errorExceptionType,
        invalidField = invalidField,
        samplesJson = try { gson.toJson(samples) } catch (_: Exception) { "[]" },
        lastAttemptTimestamp = lastAttemptTimestamp
    )
}
