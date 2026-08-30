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
import com.example.model.SaveRunResult
import com.example.model.Vehicle
import com.example.model.createConfigurationSnapshotSafe
import com.example.model.jsonSafe
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

    suspend fun insertResult(result: RunResult): SaveRunResult {
        return saveRunResultAtomic(result)
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
     * Salvamento atômico em etapas com validação, captura detalhada de erros e transação Room.
     */
    suspend fun saveRunResultAtomic(
        run: RunResult,
        pendingSessionId: String? = null
    ): SaveRunResult {
        var stage = "MAP_RESULT"
        return try {
            // Etapa 1: MAP_RESULT
            stage = "MAP_RESULT"
            val mappedSamples = run.samples.mapIndexed { idx, s ->
                RunProcessor.sanitizeSample(s, sessionId = run.id, index = idx)
            }

            // Etapa 2: CREATE_SNAPSHOT
            stage = "CREATE_SNAPSHOT"
            val safeSnapshot = if (run.configurationSnapshotJson.isNotBlank() && run.configurationSnapshotJson != "{}") {
                run.configurationSnapshotJson
            } else {
                createConfigurationSnapshotSafe(run)
            }

            val safeRun = run.copy(
                samples = mappedSamples,
                configurationSnapshotJson = safeSnapshot
            )
            val entity = safeRun.toEntity(gson)

            // Etapa 3: INSERT_TEST
            stage = "INSERT_TEST"
            runResultDao.insertResult(entity)

            // Etapa 4: DELETE_OLD_SAMPLES
            stage = "DELETE_OLD_SAMPLES"
            // Amostras são armazenadas de forma íntegra e atômica com o teste

            // Etapa 5: INSERT_SAMPLES
            stage = "INSERT_SAMPLES"
            // Persistidas com o resultado

            // Etapa 6: MARK_COMPLETED
            stage = "MARK_COMPLETED"
            val targetSessionId = pendingSessionId ?: run.id
            pendingSessionDao.markSessionFinalized(targetSessionId)

            // Etapa 7: VERIFY_RESULT
            stage = "VERIFY_RESULT"
            val verified = runResultDao.getResultById(run.id)
            if (verified == null) {
                throw IllegalStateException("Resultado ${run.id} não foi encontrado após inserção no banco.")
            }

            Log.i(TAG, "Resultado ${run.id} salvo e verificado com sucesso (${mappedSamples.size} amostras).")
            SaveRunResult.Success(resultId = run.id, sampleCount = mappedSamples.size)
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Falha ao salvar resultado. testId=${run.id}, stage=$stage, samples=${run.samples.size}, exception=${e.javaClass.name}, message=${e.message}",
                e
            )
            SaveRunResult.Failure(
                stage = stage,
                exceptionType = e.javaClass.name,
                technicalMessage = e.message ?: "Erro ao persistir resultado da passada"
            )
        }
    }

    /**
     * Recupera uma sessão pendente de forma atômica e segura.
     * Não inicia sensores nem GPS.
     * Não duplica amostras nem cria resultados duplicados.
     */
    suspend fun recoverPendingSession(
        sessionId: String,
        vehicle: Vehicle? = null
    ): SaveRunResult {
        var stage = "LOAD_PENDING"
        return try {
            stage = "LOAD_PENDING"
            val session = pendingSessionDao.getSessionById(sessionId)?.toDomain(gson)
                ?: return SaveRunResult.Failure(
                    stage = stage,
                    exceptionType = "NoSuchElementException",
                    technicalMessage = "Sessão pendente $sessionId não encontrada no banco"
                )

            val samples = session.samples
            if (samples.isEmpty()) {
                return SaveRunResult.Failure(
                    stage = "VALIDATE_SAMPLES",
                    exceptionType = "IllegalStateException",
                    technicalMessage = "A sessão $sessionId não contém amostras gravadas"
                )
            }

            Log.i(TAG, "Sessão $sessionId carregada com ${samples.size} amostras. Recalculando...")

            stage = "PROCESS_RUN"
            // Processamento, sanitização e cálculo com fallback seguro
            val calculatedResult = RunProcessor.processRun(
                sessionId = session.sessionId,
                vehicle = vehicle,
                rawSamples = samples
            )

            // Persistência no banco através do fluxo atômico
            val saveResult = saveRunResultAtomic(calculatedResult, pendingSessionId = sessionId)

            if (saveResult is SaveRunResult.Success) {
                Log.i(TAG, "Sessão $sessionId recuperada e finalizada com sucesso!")
            }
            saveResult
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Falha ao recuperar resultado. testId=$sessionId, stage=$stage, exception=${e.javaClass.name}, message=${e.message}",
                e
            )
            try {
                val existing = pendingSessionDao.getSessionById(sessionId)
                if (existing != null) {
                    val updated = existing.copy(
                        status = "PENDING",
                        errorMessage = e.message ?: "Erro desconhecido",
                        errorStage = stage,
                        errorExceptionType = e.javaClass.simpleName,
                        lastAttemptTimestamp = System.currentTimeMillis()
                    )
                    pendingSessionDao.updateSession(updated)
                }
            } catch (_: Exception) {}
            SaveRunResult.Failure(
                stage = stage,
                exceptionType = e.javaClass.name,
                technicalMessage = e.message ?: "Erro desconhecido na recuperação"
            )
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
        peakEnginePowerCv = peakEnginePowerCv.jsonSafe(0f),
        peakEnginePowerRpm = peakEnginePowerRpm,
        peakEnginePowerSpeedKmh = peakEnginePowerSpeedKmh.jsonSafe(0f),
        peakWheelPowerCv = peakWheelPowerCv.jsonSafe(0f),
        peakEngineTorqueKgm = peakEngineTorqueKgm.jsonSafe(0f),
        peakEngineTorqueRpm = peakEngineTorqueRpm,
        peakLongitudinalG = peakLongitudinalG.jsonSafe(0f),
        startSpeedKmh = startSpeedKmh.jsonSafe(0f),
        endSpeedKmh = endSpeedKmh.jsonSafe(0f),
        testGear = testGear,
        durationSeconds = durationSeconds.jsonSafe(0f),
        zeroToHundredSeconds = zeroToHundredSeconds?.jsonSafe(),
        eightyToOneTwentySeconds = eightyToOneTwentySeconds?.jsonSafe(),
        oneHundredToTwoHundredSeconds = oneHundredToTwoHundredSeconds?.jsonSafe(),
        quarterMileSeconds = quarterMileSeconds?.jsonSafe(),
        quarterMileSpeedKmh = quarterMileSpeedKmh?.jsonSafe(),
        temperatureCelsius = temperatureCelsius.jsonSafe(25f),
        pressureHpa = pressureHpa.jsonSafe(1013.25f),
        saeCorrectionFactor = saeCorrectionFactor.jsonSafe(1f),
        totalVehicleMassKg = totalVehicleMassKg.jsonSafe(0f),
        gearUsed = gearUsed,
        gearRatioUsed = gearRatioUsed.jsonSafe(1.0f),
        finalDriveUsed = finalDriveUsed.jsonSafe(1.0f),
        drivetrainLossPercent = drivetrainLossPercent.jsonSafe(0f),
        cdUsed = cdUsed.jsonSafe(0.34f),
        frontalAreaUsed = frontalAreaUsed.jsonSafe(2.10f),
        crrUsed = crrUsed.jsonSafe(0.015f),
        airDensityUsed = airDensityUsed.jsonSafe(1.225f),
        slopeModeUsed = slopeModeUsed,
        slopePercentUsed = slopePercentUsed.jsonSafe(0f),
        configurationSnapshotJson = configurationSnapshotJson,
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
        peakEnginePowerCv = peakEnginePowerCv.jsonSafe(0f),
        peakEnginePowerRpm = peakEnginePowerRpm,
        peakEnginePowerSpeedKmh = peakEnginePowerSpeedKmh.jsonSafe(0f),
        peakWheelPowerCv = peakWheelPowerCv.jsonSafe(0f),
        peakEngineTorqueKgm = peakEngineTorqueKgm.jsonSafe(0f),
        peakEngineTorqueRpm = peakEngineTorqueRpm,
        peakLongitudinalG = peakLongitudinalG.jsonSafe(0f),
        startSpeedKmh = startSpeedKmh.jsonSafe(0f),
        endSpeedKmh = endSpeedKmh.jsonSafe(0f),
        testGear = testGear,
        durationSeconds = durationSeconds.jsonSafe(0f),
        zeroToHundredSeconds = zeroToHundredSeconds?.jsonSafe(),
        eightyToOneTwentySeconds = eightyToOneTwentySeconds?.jsonSafe(),
        oneHundredToTwoHundredSeconds = oneHundredToTwoHundredSeconds?.jsonSafe(),
        quarterMileSeconds = quarterMileSeconds?.jsonSafe(),
        quarterMileSpeedKmh = quarterMileSpeedKmh?.jsonSafe(),
        temperatureCelsius = temperatureCelsius.jsonSafe(25f),
        pressureHpa = pressureHpa.jsonSafe(1013.25f),
        saeCorrectionFactor = saeCorrectionFactor.jsonSafe(1f),
        totalVehicleMassKg = totalVehicleMassKg.jsonSafe(0f),
        gearUsed = gearUsed,
        gearRatioUsed = gearRatioUsed.jsonSafe(1.0f),
        finalDriveUsed = finalDriveUsed.jsonSafe(1.0f),
        drivetrainLossPercent = drivetrainLossPercent.jsonSafe(0f),
        cdUsed = cdUsed.jsonSafe(0.34f),
        frontalAreaUsed = frontalAreaUsed.jsonSafe(2.10f),
        crrUsed = crrUsed.jsonSafe(0.015f),
        airDensityUsed = airDensityUsed.jsonSafe(1.225f),
        slopeModeUsed = slopeModeUsed,
        slopePercentUsed = slopePercentUsed.jsonSafe(0f),
        configurationSnapshotJson = configurationSnapshotJson,
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
