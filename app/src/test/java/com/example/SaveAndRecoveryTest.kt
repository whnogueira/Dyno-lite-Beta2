package com.example

import com.example.data.RunResultRepository
import com.example.data.db.PendingSessionDao
import com.example.data.db.PendingSessionEntity
import com.example.data.db.RunResultDao
import com.example.data.db.RunResultEntity
import com.example.data.toEntity
import com.example.model.RunProcessor
import com.example.model.RunResult
import com.example.model.RunSample
import com.example.model.SaveRunResult
import com.example.model.Vehicle
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaveAndRecoveryTest {

    private lateinit var mockRunResultDao: FakeRunResultDao
    private lateinit var mockPendingSessionDao: FakePendingSessionDao
    private lateinit var repository: RunResultRepository
    private val gson = Gson()

    @Before
    fun setUp() {
        mockRunResultDao = FakeRunResultDao()
        mockPendingSessionDao = FakePendingSessionDao()
        repository = RunResultRepository(mockRunResultDao, mockPendingSessionDao)
    }

    // 9. saveRunResultAtomic com 200 amostras válidas deve persistir com sucesso e marcar pendente como FINALIZED
    @Test
    fun testSaveRunResultAtomic200SamplesSuccess() = runBlocking {
        val samples = (0 until 200).map { i ->
            RunSample(
                timestampNs = i * 50_000_000L,
                elapsedSeconds = i * 0.05f,
                speedKmh = 20f + (i * 0.6f),
                longitudinalG = 0.35f,
                enginePowerCv = 150f + i,
                wheelPowerCv = 130f + i,
                engineTorqueKgm = 25f
            )
        }

        val run = RunResult(
            id = "test_run_200",
            vehicleId = "veh_1",
            vehicleName = "Test Car",
            samples = samples
        )

        // Simula sessão pendente anterior
        mockPendingSessionDao.insertSession(
            PendingSessionEntity(
                sessionId = "test_run_200",
                vehicleId = "veh_1",
                vehicleName = "Test Car",
                startTimeMs = 1000L,
                endTimeMs = 2000L,
                sampleCount = 200,
                status = "PENDING",
                samplesJson = gson.toJson(samples),
                lastAttemptTimestamp = 1000L
            )
        )

        val result = repository.saveRunResultAtomic(run, pendingSessionId = "test_run_200")
        assertTrue("Resultado deve ser sucesso", result is SaveRunResult.Success)
        val success = result as SaveRunResult.Success
        assertEquals("test_run_200", success.resultId)
        assertEquals(200, success.sampleCount)

        // Verifica se no banco o status foi alterado para FINALIZED
        val sessionInDb = mockPendingSessionDao.getSessionById("test_run_200")
        assertEquals("FINALIZED", sessionInDb?.status)

        // Verifica se o resultado está presente
        val runInDb = mockRunResultDao.getResultById("test_run_200")
        assertNotNull(runInDb)
    }

    // 10. saveRunResultAtomic com falha simulada deve retornar SaveRunResult.Failure com a etapa exata
    @Test
    fun testSaveRunResultAtomicFailureStage() = runBlocking {
        mockRunResultDao.shouldThrowOnInsert = true

        val run = RunResult(
            id = "test_run_fail",
            vehicleId = "veh_1",
            samples = listOf(RunSample(speedKmh = 50f))
        )

        val result = repository.saveRunResultAtomic(run)
        assertTrue("Resultado deve ser falha", result is SaveRunResult.Failure)
        val failure = result as SaveRunResult.Failure
        assertEquals("INSERT_TEST", failure.stage)
        assertNotNull(failure.technicalMessage)
    }

    // 11. recoverPendingSession com 200 amostras brutas sem potência deve recalcular e salvar sem erro
    @Test
    fun testRecoverPendingSession200RawSamplesCalculatesAndSaves() = runBlocking {
        val rawSamples = (0 until 200).map { i ->
            RunSample(
                timestampNs = i * 50_000_000L,
                elapsedSeconds = i * 0.05f,
                speedKmh = 20f + (i * 0.6f),
                longitudinalG = 0.35f,
                enginePowerCv = 0f,
                wheelPowerCv = 0f,
                engineTorqueKgm = 0f
            )
        }

        mockPendingSessionDao.insertSession(
            PendingSessionEntity(
                sessionId = "pending_raw_200",
                vehicleId = "veh_golf",
                vehicleName = "VW Golf GTI",
                startTimeMs = 1000L,
                endTimeMs = 2000L,
                sampleCount = 200,
                status = "PENDING",
                samplesJson = gson.toJson(rawSamples),
                lastAttemptTimestamp = 1000L
            )
        )

        val vehicle = Vehicle(id = "veh_golf", name = "VW Golf GTI", curbWeightKg = 1370f, driverWeightKg = 80f)
        val result = repository.recoverPendingSession("pending_raw_200", vehicle)

        assertTrue(result is SaveRunResult.Success)
        val success = result as SaveRunResult.Success
        assertEquals(200, success.sampleCount)

        val savedEntity = mockRunResultDao.getResultById("pending_raw_200")
        assertNotNull(savedEntity)
        assertTrue(savedEntity!!.peakEnginePowerCv > 0f)
    }

    // 12. recoverPendingSession com dados insuficientes não deve falhar o salvamento, apenas marcar qualidade parcial
    @Test
    fun testRecoverPendingSessionInsufficientDataMarksPartial() = runBlocking {
        val emptyDynamicSamples = listOf(
            RunSample(timestampNs = 0L, elapsedSeconds = 0f, speedKmh = 0f, longitudinalG = 0f),
            RunSample(timestampNs = 100_000_000L, elapsedSeconds = 0.1f, speedKmh = 0f, longitudinalG = 0f)
        )

        mockPendingSessionDao.insertSession(
            PendingSessionEntity(
                sessionId = "pending_insufficient",
                vehicleId = "veh_1",
                vehicleName = "Car",
                startTimeMs = 1000L,
                endTimeMs = 2000L,
                sampleCount = 2,
                status = "PENDING",
                samplesJson = gson.toJson(emptyDynamicSamples),
                lastAttemptTimestamp = 1000L
            )
        )

        val result = repository.recoverPendingSession("pending_insufficient", null)
        assertTrue(result is SaveRunResult.Success)

        val saved = mockRunResultDao.getResultById("pending_insufficient")
        assertNotNull(saved)
        assertEquals("DADOS INSUFICIENTES", saved!!.qualityStatus)
        assertNotNull(saved.technicalFailureReason)
    }

    // 13. recovery idempotente: chamar recoverPendingSession duas vezes para o mesmo sessionId não deve duplicar amostras
    @Test
    fun testRecoveryIsIdempotent() = runBlocking {
        val samples = (0 until 50).map { i ->
            RunSample(
                timestampNs = i * 50_000_000L,
                elapsedSeconds = i * 0.05f,
                speedKmh = 20f + (i * 0.5f),
                longitudinalG = 0.3f
            )
        }

        mockPendingSessionDao.insertSession(
            PendingSessionEntity(
                sessionId = "idempotent_session",
                vehicleId = "veh_1",
                vehicleName = "Car",
                startTimeMs = 1000L,
                endTimeMs = 2000L,
                sampleCount = 50,
                status = "PENDING",
                samplesJson = gson.toJson(samples),
                lastAttemptTimestamp = 1000L
            )
        )

        // Primeira recuperação
        val result1 = repository.recoverPendingSession("idempotent_session", null)
        assertTrue(result1 is SaveRunResult.Success)

        // Segunda recuperação
        val result2 = repository.recoverPendingSession("idempotent_session", null)
        assertTrue(result2 is SaveRunResult.Success)

        val saved = mockRunResultDao.getResultById("idempotent_session")
        assertNotNull(saved)
        // Amostras continuam exatamente 50
        val deserializedSamples: List<RunSample> = gson.fromJson(saved!!.samplesJson, object : com.google.gson.reflect.TypeToken<List<RunSample>>() {}.type)
        assertEquals(50, deserializedSamples.size)
    }

    // 14. IDs de amostra são determinísticos: formato $testId-$sampleIndex
    @Test
    fun testDeterministicSampleIds() {
        val rawSample = RunSample(
            speedKmh = 60f,
            longitudinalG = 0.3f
        )

        val sanitized = RunProcessor.sanitizeSample(rawSample, sessionId = "test_run_123", index = 42)
        assertEquals("test_run_123-42", sanitized.sampleId)
        assertEquals(42, sanitized.sampleIndex)
    }

    // Fakes for testing
    private class FakeRunResultDao : RunResultDao {
        val storage = mutableMapOf<String, RunResultEntity>()
        var shouldThrowOnInsert = false

        override fun getAllResults(): Flow<List<RunResultEntity>> = flowOf(storage.values.toList())
        override fun getResultsForVehicle(vehicleId: String): Flow<List<RunResultEntity>> =
            flowOf(storage.values.filter { it.vehicleId == vehicleId })

        override suspend fun getResultById(id: String): RunResultEntity? = storage[id]

        override suspend fun insertResult(result: RunResultEntity) {
            if (shouldThrowOnInsert) throw RuntimeException("Simulated SQLite constraint failure")
            storage[result.id] = result
        }

        override suspend fun deleteResultById(id: String) {
            storage.remove(id)
        }
    }

    private class FakePendingSessionDao : PendingSessionDao {
        val storage = mutableMapOf<String, PendingSessionEntity>()

        override fun getPendingSessions(): Flow<List<PendingSessionEntity>> =
            flowOf(storage.values.filter { it.status != "FINALIZED" })

        override suspend fun getSessionById(sessionId: String): PendingSessionEntity? = storage[sessionId]

        override suspend fun getLatestPendingSession(): PendingSessionEntity? =
            storage.values.filter { it.status != "FINALIZED" }.maxByOrNull { it.startTimeMs }

        override suspend fun insertSession(session: PendingSessionEntity) {
            storage[session.sessionId] = session
        }

        override suspend fun updateSession(session: PendingSessionEntity) {
            storage[session.sessionId] = session
        }

        override suspend fun markSessionFinalized(sessionId: String) {
            val existing = storage[sessionId]
            if (existing != null) {
                storage[sessionId] = existing.copy(status = "FINALIZED")
            }
        }

        override suspend fun deleteSessionById(sessionId: String) {
            storage.remove(sessionId)
        }

        override suspend fun clearFinalizedSessions() {
            storage.entries.removeIf { it.value.status == "FINALIZED" }
        }
    }
}
