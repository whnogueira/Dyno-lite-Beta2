package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.data.db.DynoMobileDatabase
import com.example.data.db.TestEntity
import com.example.data.db.TestRevisionEntity
import com.example.data.db.TestSampleEntity
import com.example.data.db.currentIsoUtc
import com.example.data.db.isoToTimestampMs
import com.example.data.db.toIsoUtc
import com.example.model.AccelerationSplit
import com.example.model.RunResult
import com.example.model.RunSample
import com.example.model.TestResultRevision
import com.example.model.UniqueGpsFix
import com.example.model.VehicleProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val TAG = "DynoStorage"

class RunResultRepository(context: Context) {

  private val database = DynoMobileDatabase.getDatabase(context)
  private val testDao = database.testDao()
  private val testSampleDao = database.testSampleDao()
  private val testRevisionDao = database.testRevisionDao()
  private val legacyPrefs = context.getSharedPreferences("dyno_lite_runs_store", Context.MODE_PRIVATE)

  init {
    // Migração de dados legados do SharedPreferences para o Room (DynoMobileDB)
    CoroutineScope(Dispatchers.IO).launch {
      migrateLegacyRunsIfNeeded()
    }
  }

  private suspend fun migrateLegacyRunsIfNeeded() = withContext(Dispatchers.IO) {
    try {
      val isMigrated = legacyPrefs.getBoolean("is_migrated_to_room_v1", false)
      if (isMigrated) return@withContext

      val jsonStr = legacyPrefs.getString("key_runs_json", null)
      if (!jsonStr.isNullOrBlank()) {
        val jsonArray = JSONArray(jsonStr)
        Log.i(TAG, "[DynoStorage] Iniciando migração de ${jsonArray.length()} testes legados do SharedPreferences para DynoMobileDB...")
        for (i in 0 until jsonArray.length()) {
          val obj = jsonArray.getJSONObject(i)
          val run = deserializeLegacyJson(obj)
          val testEntity = mapRunResultToTestEntity(run, status = "completed")
          testDao.insertTest(testEntity)
          if (run.samples.isNotEmpty()) {
            val sampleEntities = run.samples.mapIndexed { idx, s ->
              mapRunSampleToEntity(s, run.id, idx)
            }
            testSampleDao.insertSamples(sampleEntities)
          }
        }
        Log.i(TAG, "[DynoStorage] Migração de testes concluída com sucesso.")
      }
      legacyPrefs.edit().putBoolean("is_migrated_to_room_v1", true).apply()
    } catch (e: Exception) {
      Log.e(TAG, "[DynoStorage] Erro durante migração de testes legados: ${e.message}", e)
    }
  }

  /**
   * Cria registro inicial de teste com status "recording".
   */
  suspend fun startRecordingTest(
    testId: String,
    vehicleId: String?,
    vehicleName: String,
    snapshotJson: String,
    startSpeedKmh: Float
  ): Boolean = withContext(Dispatchers.IO) {
    try {
      val nowIso = currentIsoUtc()
      val entity = TestEntity(
        id = testId,
        vehicleId = vehicleId,
        name = vehicleName.ifBlank { "Passagem" },
        createdAt = nowIso,
        completedAt = null,
        status = "recording",
        startSpeed = startSpeedKmh.sanitize(),
        configurationSnapshot = snapshotJson
      )
      testDao.insertTest(entity)
      Log.i(TAG, "[DynoStorage] Teste criado: $testId (status: recording)")
      true
    } catch (e: Exception) {
      Log.e(TAG, "[DynoStorage] Falha ao iniciar teste $testId: ${e.message}", e)
      false
    }
  }

  /**
   * Salva lote de amostras durante o teste em andamento.
   */
  suspend fun saveSampleBatch(
    testId: String,
    samples: List<RunSample>,
    startingIndex: Int
  ): Boolean = withContext(Dispatchers.IO) {
    if (samples.isEmpty()) return@withContext true
    try {
      val entities = samples.mapIndexed { idx, sample ->
        mapRunSampleToEntity(sample, testId, startingIndex + idx)
      }
      testSampleDao.insertSamples(entities)
      Log.d(TAG, "[DynoStorage] Amostras gravadas (${entities.size} amostras, idx=$startingIndex): $testId")
      true
    } catch (e: Exception) {
      Log.e(TAG, "[DynoStorage] Falha ao gravar lote de amostras para $testId: ${e.message}", e)
      false
    }
  }

  /**
   * Salva o teste completo ou atualiza um teste existente para "completed".
   */
  suspend fun saveResultSuspending(run: RunResult, status: String = "completed"): Boolean = withContext(Dispatchers.IO) {
    try {
      val testEntity = mapRunResultToTestEntity(run, status = status)
      val rowId = testDao.insertTest(testEntity)

      if (run.samples.isNotEmpty()) {
        // Enforce max 500 samples per run
        val sampleList = if (run.samples.size > 500) run.samples.take(500) else run.samples
        val sampleEntities = sampleList.mapIndexed { idx, sample ->
          mapRunSampleToEntity(sample, run.id, idx)
        }
        testSampleDao.replaceSamplesForTest(run.id, sampleEntities)
      }

      Log.i(TAG, "[DynoStorage] Teste salvo com sucesso: ${run.id} (status: $status, row: $rowId, amostras: ${run.samples.size})")
      true
    } catch (e: Exception) {
      Log.e(TAG, "[DynoStorage] Falha ao salvar teste ${run.id}: ${e.message}", e)
      false
    }
  }

  /**
   * Wrapper síncrono para interoperabilidade com telas e componentes existentes.
   */
  fun saveResult(run: RunResult, status: String = "completed"): Boolean {
    return try {
      runBlocking(Dispatchers.IO) {
        saveResultSuspending(run, status)
      }
    } catch (e: Exception) {
      Log.e(TAG, "[DynoStorage] Erro no saveResult síncrono: ${e.message}", e)
      false
    }
  }

  /**
   * Retorna Flow reativo de todos os testes concluídos ordenados do mais recente para o mais antigo.
   */
  fun getResultsFlow(): Flow<List<RunResult>> {
    return testDao.getCompletedTestsFlow().map { entities ->
      Log.d(TAG, "[DynoStorage] Flow emitiu ${entities.size} testes concluídos")
      entities.map { entity ->
        val samples = testSampleDao.getSamplesForTest(entity.id).map { mapEntityToRunSample(it) }
        mapTestEntityToRunResult(entity, samples)
      }
    }
  }

  /**
   * Retorna lista de todos os testes concluídos de forma síncrona.
   */
  fun getResults(): List<RunResult> {
    return try {
      runBlocking(Dispatchers.IO) {
        val entities = testDao.getCompletedTests()
        Log.d(TAG, "[DynoStorage] Consulta getResults() retornou ${entities.size} testes concluídos")
        entities.map { entity ->
          val samples = testSampleDao.getSamplesForTest(entity.id).map { mapEntityToRunSample(it) }
          mapTestEntityToRunResult(entity, samples)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "[DynoStorage] Erro ao consultar getResults: ${e.message}", e)
      emptyList()
    }
  }

  /**
   * Retorna lista de testes não concluídos (status 'recording' ou 'interrupted').
   */
  suspend fun getIncompleteTests(): List<RunResult> = withContext(Dispatchers.IO) {
    try {
      val entities = testDao.getIncompleteTests()
      Log.i(TAG, "[DynoStorage] Consulta getIncompleteTests() retornou ${entities.size} testes incompletos")
      entities.map { entity ->
        val samples = testSampleDao.getSamplesForTest(entity.id).map { mapEntityToRunSample(it) }
        mapTestEntityToRunResult(entity, samples)
      }
    } catch (e: Exception) {
      Log.e(TAG, "[DynoStorage] Erro ao consultar getIncompleteTests: ${e.message}", e)
      emptyList()
    }
  }

  /**
   * Busca um teste específico por ID.
   */
  suspend fun getResultByIdSuspending(id: String): RunResult? = withContext(Dispatchers.IO) {
    try {
      val entity = testDao.getTestById(id) ?: return@withContext null
      val samples = testSampleDao.getSamplesForTest(id).map { mapEntityToRunSample(it) }
      mapTestEntityToRunResult(entity, samples)
    } catch (e: Exception) {
      Log.e(TAG, "[DynoStorage] Erro ao buscar resultado por id $id: ${e.message}", e)
      null
    }
  }

  fun getResultById(id: String): RunResult? {
    return try {
      runBlocking(Dispatchers.IO) {
        getResultByIdSuspending(id)
      }
    } catch (e: Exception) {
      Log.e(TAG, "[DynoStorage] Erro no getResultById síncrono: ${e.message}", e)
      null
    }
  }

  /**
   * Retorna amostras limpas e ordenadas de um teste.
   */
  fun getOrderedRunSamples(resultId: String): List<RunSample> {
    val run = getResultById(resultId) ?: return emptyList()
    val rawSamples = run.samples
    if (rawSamples.isEmpty()) return emptyList()

    val cleanedSamples = mutableListOf<RunSample>()
    var lastTimeMs = -1L
    val sorted = rawSamples.sortedBy { it.elapsedTimeMs }

    for (sample in sorted) {
      val timeMs = sample.elapsedTimeMs
      if (timeMs <= lastTimeMs) continue

      cleanedSamples.add(
        sample.copy(
          elapsedTimeMs = timeMs,
          filteredAccelerationZ = sample.filteredAccelerationZ.sanitize(),
          correctedAccelerationZ = sample.correctedAccelerationZ.sanitize(),
          gpsSpeedKmh = sample.gpsSpeedKmh.sanitize().coerceAtLeast(0f),
          calculatedSpeedKmh = sample.calculatedSpeedKmh.sanitize().coerceAtLeast(0f),
          speedDifferenceKmh = sample.speedDifferenceKmh.sanitize().coerceAtLeast(0f),
          gpsAccuracyMeters = sample.gpsAccuracyMeters.sanitize().coerceAtLeast(0f),
          gyroMagnitude = sample.gyroMagnitude.sanitize().coerceAtLeast(0f)
        )
      )
      lastTimeMs = timeMs
    }

    return cleanedSamples
  }

  /**
   * Exclui um teste e suas amostras.
   */
  suspend fun deleteResultSuspending(id: String): Boolean = withContext(Dispatchers.IO) {
    try {
      testSampleDao.deleteSamplesForTest(id)
      val count = testDao.deleteTestById(id)
      Log.i(TAG, "[DynoStorage] Teste $id excluído (linhas afetadas: $count)")
      true
    } catch (e: Exception) {
      Log.e(TAG, "[DynoStorage] Erro ao excluir teste $id: ${e.message}", e)
      false
    }
  }

  fun deleteResult(id: String) {
    runBlocking(Dispatchers.IO) {
      deleteResultSuspending(id)
    }
  }

  /**
   * Limpa todos os testes e amostras do banco.
   */
  suspend fun clearAllResultsSuspending(): Boolean = withContext(Dispatchers.IO) {
    try {
      testSampleDao.deleteAllSamples()
      testDao.deleteAllTests()
      Log.i(TAG, "[DynoStorage] Todos os testes e amostras foram limpos do DynoMobileDB")
      true
    } catch (e: Exception) {
      Log.e(TAG, "[DynoStorage] Erro ao limpar todos os resultados: ${e.message}", e)
      false
    }
  }

  fun clearAllResults() {
    runBlocking(Dispatchers.IO) {
      clearAllResultsSuspending()
    }
  }

  /**
   * Salva a correção de uma passagem existente mantendo o mesmo ID e guardando revisão anterior (Requisito 7 e 8).
   * Executa em transação segura no banco: se houver erro, mantém os dados originais intactos.
   */
  suspend fun saveCorrectionSuspending(
    originalRunId: String,
    correctedRun: RunResult,
    note: String? = null
  ): Result<RunResult> = withContext(Dispatchers.IO) {
    try {
      val originalEntity = testDao.getTestById(originalRunId)
        ?: return@withContext Result.failure(IllegalStateException("Não foi possível salvar a correção: passagem não encontrada"))
      val existingRevisions = testRevisionDao.getRevisionsForTest(originalRunId)
      val nextRevNumber = (existingRevisions.maxOfOrNull { it.revisionNumber } ?: 1) + 1

      val previousConfigJson = originalEntity.configurationSnapshot
      val previousCalcJson = JSONObject().apply {
        put("wheelPowerCv", originalEntity.maxWheelPowerCv)
        put("enginePowerCv", originalEntity.estimatedEnginePowerCv)
        put("engineTorqueKgfm", originalEntity.maxTorqueKgfm)
        put("peakRpm", originalEntity.maxRpm)
        put("totalMassKg", JSONObject(previousConfigJson.ifBlank { "{}" }).optDouble("totalMassKg", 0.0))
      }.toString()

      val updatedRun = correctedRun.copy(
        id = originalRunId,
        isRecalculated = true,
        revisionNumber = nextRevNumber,
        recalculationReason = "Dados da passagem corrigidos pelo usuário",
        recalculationNote = note?.ifBlank { null },
        previousConfigurationJson = previousConfigJson,
        previousCalculatedResultJson = previousCalcJson
      )

      val correctedConfigJson = createConfigurationSnapshot(updatedRun)
      val correctedCalcJson = JSONObject().apply {
        put("wheelPowerCv", updatedRun.wheelPowerCv)
        put("enginePowerCv", updatedRun.enginePowerCv)
        put("engineTorqueKgfm", updatedRun.engineTorqueKgfm)
        put("peakRpm", updatedRun.peakPowerRpm)
        put("totalMassKg", updatedRun.totalVehicleMassKg)
      }.toString()

      val revisionEntity = TestRevisionEntity(
        revisionId = UUID.randomUUID().toString(),
        testResultId = originalRunId,
        revisionNumber = nextRevNumber,
        createdAt = currentIsoUtc(),
        reason = "Dados da passagem corrigidos pelo usuário",
        note = note?.ifBlank { null },
        previousConfigurationJson = previousConfigJson,
        correctedConfigurationJson = correctedConfigJson,
        previousCalculatedResultJson = previousCalcJson,
        correctedCalculatedResultJson = correctedCalcJson
      )

      val updatedTestEntity = mapRunResultToTestEntity(updatedRun, status = originalEntity.status)

      database.withTransaction {
        testRevisionDao.insertRevision(revisionEntity)
        testDao.updateTest(updatedTestEntity)
        if (updatedRun.samples.isNotEmpty()) {
          val sampleEntities = updatedRun.samples.mapIndexed { idx, s ->
            mapRunSampleToEntity(s, originalRunId, idx)
          }
          testSampleDao.replaceSamplesForTest(originalRunId, sampleEntities)
        }
      }

      Log.i(TAG, "[DynoStorage] Correção salva com sucesso na passagem $originalRunId (Revisão $nextRevNumber)")
      Result.success(updatedRun)
    } catch (e: Exception) {
      Log.e(TAG, "[DynoStorage] Não foi possível salvar a correção: ${e.message}", e)
      Result.failure(Exception("Não foi possível salvar a correção.", e))
    }
  }

  fun saveCorrection(originalRunId: String, correctedRun: RunResult, note: String? = null): Boolean {
    return runBlocking(Dispatchers.IO) {
      saveCorrectionSuspending(originalRunId, correctedRun, note).isSuccess
    }
  }

  /**
   * Salva a passagem corrigida como uma NOVA versão, criando novo ID e mantendo referência à original (Requisito 7 e 8).
   * Executa em transação segura no banco.
   */
  suspend fun saveAsNewVersionSuspending(
    originalRun: RunResult,
    correctedRun: RunResult,
    note: String? = null
  ): Result<RunResult> = withContext(Dispatchers.IO) {
    try {
      val newId = UUID.randomUUID().toString()
      val previousConfigJson = createConfigurationSnapshot(originalRun)
      val previousCalcJson = JSONObject().apply {
        put("wheelPowerCv", originalRun.wheelPowerCv)
        put("enginePowerCv", originalRun.enginePowerCv)
        put("engineTorqueKgfm", originalRun.engineTorqueKgfm)
        put("peakRpm", originalRun.peakPowerRpm)
        put("totalMassKg", originalRun.totalVehicleMassKg)
      }.toString()

      val newRun = correctedRun.copy(
        id = newId,
        vehicleName = "${originalRun.vehicleName} (v2)",
        parentRunId = originalRun.id,
        isRecalculated = true,
        revisionNumber = 2,
        recalculationReason = "Dados da passagem corrigidos pelo usuário (nova versão)",
        recalculationNote = note?.ifBlank { null },
        previousConfigurationJson = previousConfigJson,
        previousCalculatedResultJson = previousCalcJson,
        timestamp = System.currentTimeMillis()
      )

      val correctedConfigJson = createConfigurationSnapshot(newRun)
      val correctedCalcJson = JSONObject().apply {
        put("wheelPowerCv", newRun.wheelPowerCv)
        put("enginePowerCv", newRun.enginePowerCv)
        put("engineTorqueKgfm", newRun.engineTorqueKgfm)
        put("peakRpm", newRun.peakPowerRpm)
        put("totalMassKg", newRun.totalVehicleMassKg)
      }.toString()

      val revisionEntity = TestRevisionEntity(
        revisionId = UUID.randomUUID().toString(),
        testResultId = newId,
        revisionNumber = 2,
        createdAt = currentIsoUtc(),
        reason = "Nova versão derivada da passagem ${originalRun.id}",
        note = note?.ifBlank { null },
        previousConfigurationJson = previousConfigJson,
        correctedConfigurationJson = correctedConfigJson,
        previousCalculatedResultJson = previousCalcJson,
        correctedCalculatedResultJson = correctedCalcJson
      )

      val newTestEntity = mapRunResultToTestEntity(newRun, status = "completed")

      database.withTransaction {
        testDao.insertTest(newTestEntity)
        testRevisionDao.insertRevision(revisionEntity)
        if (newRun.samples.isNotEmpty()) {
          val sampleEntities = newRun.samples.mapIndexed { idx, s ->
            mapRunSampleToEntity(s, newId, idx)
          }
          testSampleDao.insertSamples(sampleEntities)
        }
      }

      Log.i(TAG, "[DynoStorage] Nova versão salva com sucesso: $newId (original: ${originalRun.id})")
      Result.success(newRun)
    } catch (e: Exception) {
      Log.e(TAG, "[DynoStorage] Não foi possível salvar a nova versão: ${e.message}", e)
      Result.failure(Exception("Não foi possível salvar a correção.", e))
    }
  }

  fun saveAsNewVersion(originalRun: RunResult, correctedRun: RunResult, note: String? = null): Boolean {
    return runBlocking(Dispatchers.IO) {
      saveAsNewVersionSuspending(originalRun, correctedRun, note).isSuccess
    }
  }

  /**
   * Busca as revisões salvas de uma passagem para exibir no histórico ou "VER CONFIGURAÇÃO ORIGINAL".
   */
  suspend fun getRevisionsForTest(testResultId: String): List<TestResultRevision> = withContext(Dispatchers.IO) {
    try {
      testRevisionDao.getRevisionsForTest(testResultId).map { entity ->
        TestResultRevision(
          revisionId = entity.revisionId,
          testResultId = entity.testResultId,
          revisionNumber = entity.revisionNumber,
          createdAt = entity.createdAt,
          reason = entity.reason,
          note = entity.note,
          previousConfigurationJson = entity.previousConfigurationJson,
          correctedConfigurationJson = entity.correctedConfigurationJson,
          previousCalculatedResultJson = entity.previousCalculatedResultJson,
          correctedCalculatedResultJson = entity.correctedCalculatedResultJson
        )
      }
    } catch (e: Exception) {
      Log.e(TAG, "[DynoStorage] Erro ao buscar revisões para $testResultId: ${e.message}", e)
      emptyList()
    }
  }

  /**
   * Teste de armazenamento obrigatório para diagnóstico (Modo Dev).
   * 1. Cria um teste de teste
   * 2. Salva 10 amostras
   * 3. Marca como concluído
   * 4. Lê do banco
   * 5. Confere todos os campos
   * 6. Remove o registro de teste
   * 7. Retorna Pair(sucesso, mensagem)
   */
  suspend fun runStorageSelfTest(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val dummyTestId = "dev-selftest-${UUID.randomUUID()}"
    try {
      Log.i(TAG, "[DynoStorage] === INICIANDO TESTE DE AUTO-DIAGNÓSTICO DO ARMAZENAMENTO ===")
      // 1. Criar registro
      val dummyEntity = TestEntity(
        id = dummyTestId,
        vehicleId = "dev-vehicle",
        name = "Teste de Diagnóstico de Armazenamento",
        createdAt = currentIsoUtc(),
        completedAt = null,
        status = "recording",
        startSpeed = 40.0f,
        configurationSnapshot = "{\"test\":true}"
      )
      testDao.insertTest(dummyEntity)
      Log.i(TAG, "[DynoStorage] SelfTest: registro inicial inserido")

      // 2. Salvar 10 amostras
      val dummySamples = (0 until 10).map { i ->
        TestSampleEntity(
          id = UUID.randomUUID().toString(),
          testId = dummyTestId,
          sampleIndex = i,
          timestamp = currentIsoUtc(),
          elapsedTimeMs = i * 100L,
          speed = 40f + i * 2f,
          filteredSpeed = 40f + i * 2f,
          acceleration = 2.5f,
          longitudinalG = 0.25f,
          rpm = 2500 + i * 200,
          distance = i * 5f,
          wheelPowerCv = 120f + i * 5f,
          enginePowerCv = 140f + i * 6f,
          torqueKgfm = 20f,
          gpsAccuracy = 3.5f,
          confidence = "ALTA",
          isValid = true
        )
      }
      testSampleDao.insertSamples(dummySamples)
      Log.i(TAG, "[DynoStorage] SelfTest: 10 amostras inseridas")

      // 3. Concluir teste
      val completedEntity = dummyEntity.copy(
        completedAt = currentIsoUtc(),
        status = "completed",
        endSpeed = 80.0f,
        elapsedTime = 3.5f,
        maxWheelPowerCv = 170.0f,
        estimatedEnginePowerCv = 200.0f,
        maxTorqueKgfm = 24.5f,
        sampleCount = 10
      )
      testDao.updateTest(completedEntity)
      Log.i(TAG, "[DynoStorage] SelfTest: registro atualizado para completed")

      // 4. Ler do banco e verificar
      val readBackTest = testDao.getTestById(dummyTestId)
        ?: return@withContext Pair(false, "Falha: teste não encontrado após gravação")
      val readBackSamples = testSampleDao.getSamplesForTest(dummyTestId)

      if (readBackSamples.size != 10) {
        return@withContext Pair(false, "Falha: esperava 10 amostras, obteve ${readBackSamples.size}")
      }
      if (readBackTest.status != "completed") {
        return@withContext Pair(false, "Falha: status do teste é ${readBackTest.status} (esperado completed)")
      }
      if (readBackTest.estimatedEnginePowerCv != 200.0f) {
        return@withContext Pair(false, "Falha: valor da potência divergente (${readBackTest.estimatedEnginePowerCv})")
      }

      // 5. Limpar teste de teste
      testSampleDao.deleteSamplesForTest(dummyTestId)
      testDao.deleteTestById(dummyTestId)
      Log.i(TAG, "[DynoStorage] SelfTest: registro temporário limpo com sucesso")
      Log.i(TAG, "[DynoStorage] === TESTE DE AUTO-DIAGNÓSTICO CONCLUÍDO COM SUCESSO ===")

      Pair(true, "Armazenamento persistente (DynoMobileDB) funcionando perfeitamente! Leitura, gravação e transações verificadas.")
    } catch (e: Exception) {
      Log.e(TAG, "[DynoStorage] Falha no teste de auto-diagnóstico: ${e.message}", e)
      // Cleanup attempt
      try {
        testSampleDao.deleteSamplesForTest(dummyTestId)
        testDao.deleteTestById(dummyTestId)
      } catch (_: Exception) {}
      Pair(false, "Falha no armazenamento: ${e.localizedMessage ?: e.message}")
    }
  }

  // --- Mapeamentos e Sanitização ---

  private fun mapRunResultToTestEntity(r: RunResult, status: String): TestEntity {
    val snapshot = createConfigurationSnapshot(r)
    return TestEntity(
      id = r.id,
      vehicleId = r.vehicleId,
      name = r.vehicleName.ifBlank { "Passagem" },
      createdAt = r.timestamp.toIsoUtc(),
      completedAt = currentIsoUtc(),
      status = status,
      startSpeed = r.startSpeedKmh.sanitize(),
      maxSpeed = r.maximumGpsSpeedKmh.sanitize(),
      endSpeed = r.finalSpeedKmh.sanitize(),
      speedGain = r.speedGainKmh.sanitize(),
      officialStartSpeed = (if (r.officialStartSpeedKmh > 0f) r.officialStartSpeedKmh else r.startSpeedKmh).sanitize(),
      officialMaxSpeed = (if (r.officialMaxSpeedKmh > 0f) r.officialMaxSpeedKmh else r.maximumGpsSpeedKmh).sanitize(),
      officialEndSpeed = (if (r.officialEndSpeedKmh > 0f) r.officialEndSpeedKmh else r.finalSpeedKmh).sanitize(),
      officialSpeedGain = (if (r.officialSpeedGainKmh > 0f) r.officialSpeedGainKmh else r.speedGainKmh).sanitize(),
      elapsedTime = r.elapsedSeconds.sanitize(),
      distance = r.totalDistanceMeters.sanitize(),
      maxWheelPowerCv = r.wheelPowerCv.sanitize(),
      estimatedEnginePowerCv = r.enginePowerCv.sanitize(),
      maxTorqueKgfm = r.engineTorqueKgfm.sanitize(),
      maxRpm = r.peakPowerRpm,
      maxG = r.peakLongitudinalG.sanitize(),
      averageGpsAccuracy = r.averageGpsAccuracyMeters.sanitize(),
      confidence = r.confidenceLevel.ifBlank { "ALTA" },
      sampleCount = r.samples.size.takeIf { it > 0 } ?: r.totalSamples,
      quality = r.quality.ifBlank { "BOA" },
      finishReason = r.finishReason,
      invalidationReason = r.invalidationReason,
      averageSpeedDifferenceKmh = r.averageSpeedDifferenceKmh.sanitize(),
      maximumSpeedDifferenceKmh = r.maximumSpeedDifferenceKmh.sanitize(),
      time0to60Kmh = r.time0to60Kmh?.sanitize(),
      time0to100Kmh = r.time0to100Kmh?.sanitize(),
      time60to100Kmh = r.time60to100Kmh?.sanitize(),
      time80to120Kmh = r.time80to120Kmh?.sanitize(),
      time100to200Kmh = r.time100to200Kmh?.sanitize(),
      time60Feet = r.time60Feet?.sanitize(),
      time100M = r.time100M?.sanitize(),
      time201M = r.time201M?.sanitize(),
      time402M = r.time402M?.sanitize(),
      appVersion = r.appVersion.ifBlank { "0.20.0" },
      configurationSnapshot = snapshot
    )
  }

  private fun mapTestEntityToRunResult(entity: TestEntity, samples: List<RunSample>): RunResult {
    val startGps = entity.startSpeed
    val maxSpeed = entity.endSpeed.coerceAtLeast(startGps)
    val powerCv = entity.estimatedEnginePowerCv
    val torqueKgfm = entity.maxTorqueKgfm
    val wheelPower = entity.maxWheelPowerCv

    // Parse snapshot if available
    var gearUsed = "2ª"
    var gearIndex = 1
    var gearRatio = 2.14f
    var finalDrive = 4.19f
    var mass = 0f
    var curbWeight = 0f
    var driverWeight = 0f
    var passengerCount = 0
    var passengerWeight = 0f
    var additionalWeight = 0f
    var soundSystemWeight = 0f
    var cngWeight = 0f
    var fuelAdjustment = 0f
    var tireWidth = 195
    var tireAspect = 55
    var rimInches = 15
    var isRecalculated = false
    var revisionNumber = 1
    var parentRunId: String? = null
    var recalculationReason: String? = null
    var recalculationNote: String? = null
    var previousConfigJson: String? = null
    var previousCalcJson: String? = null
    var gpsDeltaV = 0f
    var sensorDeltaV = 0f
    var normFactor = 1.0f
    var rawAvgG = 0f
    var anchoredAvgG = 0f
    var drivetrainLoss = 12f
    var cd = 0.34f
    var frontalArea = 2.10f
    var crr = 0.015f
    var airDensity = 1.225f
    var slopeMode = "IGNORE"
    var slopePercent = 0f

    var officialStart = entity.officialStartSpeed
    var officialMax = entity.officialMaxSpeed
    var officialEnd = entity.officialEndSpeed
    var officialGain = entity.officialSpeedGain
    var maxGpsSpeed = entity.maxSpeed
    var maxCalcSpeed = entity.maxSpeed
    var maxIntegratedSpeed = entity.maxSpeed
    var finalGpsSpeed = entity.endSpeed
    var finalCalcSpeed = entity.endSpeed
    var finalIntegratedSpeed = entity.endSpeed
    var locationCallbackCount = 0
    var uniqueGpsFixCount = 0
    var gpsSpeedChangeCount = 0
    var sensorSampleCount = 0
    var maxGpsIntervalMs = 0L
    var maxGpsAgeMs = 0L
    var gpsFrozen = false
    var isPreliminary = false
    var avgGpsFreq = 1.0f
    var testMode = "DYNO"
    var targetStartSpeed = 0f
    var targetEndSpeed = 0f
    var accelRangeLabel = ""
    var gearShiftCount = 0
    var speedUnit = "km/h"
    var estimatedMarginSeconds = 0.08f
    val accelerationSplitsList = mutableListOf<AccelerationSplit>()
    val uniqueGpsFixesList = mutableListOf<UniqueGpsFix>()

    try {
      if (entity.configurationSnapshot.isNotBlank() && entity.configurationSnapshot != "{}") {
        val snapObj = JSONObject(entity.configurationSnapshot)
        testMode = snapObj.optString("testMode", "DYNO")
        targetStartSpeed = snapObj.optDouble("targetStartSpeedKmh", 0.0).toFloat()
        targetEndSpeed = snapObj.optDouble("targetEndSpeedKmh", 0.0).toFloat()
        accelRangeLabel = snapObj.optString("accelRangeLabel", "")
        gearShiftCount = snapObj.optInt("gearShiftCount", 0)
        speedUnit = snapObj.optString("speedUnit", "km/h")
        estimatedMarginSeconds = snapObj.optDouble("estimatedMarginSeconds", 0.08).toFloat()

        val splitsArr = snapObj.optJSONArray("accelerationSplits")
        if (splitsArr != null) {
          for (i in 0 until splitsArr.length()) {
            val sObj = splitsArr.getJSONObject(i)
            accelerationSplitsList.add(
              AccelerationSplit(
                label = sObj.optString("label", ""),
                startSpeedKmh = sObj.optDouble("startSpeedKmh", 0.0).toFloat(),
                endSpeedKmh = sObj.optDouble("endSpeedKmh", 0.0).toFloat(),
                timeSeconds = sObj.optDouble("timeSeconds", 0.0).toFloat()
              )
            )
          }
        }

        gearUsed = snapObj.optString("gearUsed", "2ª")
        gearIndex = snapObj.optInt("gearIndex", 1)
        gearRatio = snapObj.optDouble("gearRatio", 2.14).toFloat()
        finalDrive = snapObj.optDouble("finalDrive", 4.19).toFloat()
        mass = snapObj.optDouble("totalMassKg", 0.0).toFloat()
        curbWeight = snapObj.optDouble("curbWeightKg", 0.0).toFloat()
        driverWeight = snapObj.optDouble("driverWeightKg", 0.0).toFloat()
        passengerCount = snapObj.optInt("passengerCount", 0)
        passengerWeight = snapObj.optDouble("passengerWeightKg", 0.0).toFloat()
        additionalWeight = snapObj.optDouble("additionalWeightKg", 0.0).toFloat()
        soundSystemWeight = snapObj.optDouble("soundSystemWeightKg", 0.0).toFloat()
        cngWeight = snapObj.optDouble("cngWeightKg", 0.0).toFloat()
        fuelAdjustment = snapObj.optDouble("fuelAdjustmentKg", 0.0).toFloat()
        tireWidth = snapObj.optInt("tireWidthMm", 195)
        tireAspect = snapObj.optInt("tireAspectRatio", 55)
        rimInches = snapObj.optInt("rimInches", snapObj.optInt("wheelDiameterInches", 15))
        isRecalculated = snapObj.optBoolean("isRecalculated", false)
        revisionNumber = snapObj.optInt("revisionNumber", 1)
        parentRunId = snapObj.optString("parentRunId", "").ifBlank { null }
        recalculationReason = snapObj.optString("recalculationReason", "").ifBlank { null }
        recalculationNote = snapObj.optString("recalculationNote", "").ifBlank { null }
        previousConfigJson = snapObj.optString("previousConfigurationJson", "").ifBlank { null }
        previousCalcJson = snapObj.optString("previousCalculatedResultJson", "").ifBlank { null }
        gpsDeltaV = snapObj.optDouble("gpsDeltaVMps", 0.0).toFloat()
        sensorDeltaV = snapObj.optDouble("sensorDeltaVMps", 0.0).toFloat()
        normFactor = snapObj.optDouble("normalizationFactor", 1.0).toFloat()
        rawAvgG = snapObj.optDouble("rawAverageLongitudinalG", 0.0).toFloat()
        anchoredAvgG = snapObj.optDouble("anchoredAverageLongitudinalG", 0.0).toFloat()
        drivetrainLoss = snapObj.optDouble("drivetrainLossPercent", 12.0).toFloat()
        cd = snapObj.optDouble("cd", 0.34).toFloat()
        frontalArea = snapObj.optDouble("frontalAreaM2", 2.10).toFloat()
        crr = snapObj.optDouble("crr", 0.015).toFloat()
        airDensity = snapObj.optDouble("airDensityKgM3", 1.225).toFloat()
        slopeMode = snapObj.optString("slopeMode", "IGNORE")
        slopePercent = snapObj.optDouble("slopePercent", 0.0).toFloat()

        if (officialStart <= 0f) officialStart = snapObj.optDouble("officialStartSpeedKmh", 0.0).toFloat()
        if (officialMax <= 0f) officialMax = snapObj.optDouble("officialMaxSpeedKmh", 0.0).toFloat()
        if (officialEnd <= 0f) officialEnd = snapObj.optDouble("officialEndSpeedKmh", 0.0).toFloat()
        if (officialGain <= 0f) officialGain = snapObj.optDouble("officialSpeedGainKmh", 0.0).toFloat()
        maxGpsSpeed = snapObj.optDouble("maximumGpsSpeedKmh", maxGpsSpeed.toDouble()).toFloat()
        maxCalcSpeed = snapObj.optDouble("maximumCalculatedSpeedKmh", maxCalcSpeed.toDouble()).toFloat()
        maxIntegratedSpeed = snapObj.optDouble("maxIntegratedSpeedKmh", maxCalcSpeed.toDouble()).toFloat()
        finalGpsSpeed = snapObj.optDouble("finalGpsSpeedKmh", finalGpsSpeed.toDouble()).toFloat()
        finalCalcSpeed = snapObj.optDouble("finalCalculatedSpeedKmh", finalCalcSpeed.toDouble()).toFloat()
        finalIntegratedSpeed = snapObj.optDouble("finalIntegratedSpeedKmh", finalCalcSpeed.toDouble()).toFloat()
        locationCallbackCount = snapObj.optInt("locationCallbackCount", 0)
        uniqueGpsFixCount = snapObj.optInt("uniqueGpsFixCount", 0)
        gpsSpeedChangeCount = snapObj.optInt("gpsSpeedChangeCount", 0)
        sensorSampleCount = snapObj.optInt("sensorSampleCount", 0)
        maxGpsIntervalMs = snapObj.optLong("maxGpsIntervalMs", 0L)
        maxGpsAgeMs = snapObj.optLong("maxGpsAgeMs", 0L)
        gpsFrozen = snapObj.optBoolean("gpsFrozen", false)
        isPreliminary = snapObj.optBoolean("isPreliminary", false)
        avgGpsFreq = snapObj.optDouble("averageGpsFrequencyHz", 1.0).toFloat()

        val fixesArr = snapObj.optJSONArray("uniqueGpsFixes")
        if (fixesArr != null) {
          for (i in 0 until fixesArr.length()) {
            val fObj = fixesArr.getJSONObject(i)
            uniqueGpsFixesList.add(
              UniqueGpsFix(
                elapsedRealtimeNanos = fObj.optLong("elapsedRealtimeNanos"),
                timestamp = fObj.optLong("timestamp"),
                speedKmh = fObj.optDouble("speedKmh").toFloat(),
                speedAccuracyMetersPerSecond = fObj.optDouble("speedAccuracyMps").toFloat(),
                accuracyMeters = fObj.optDouble("accuracyM").toFloat(),
                ageMillis = fObj.optLong("ageMillis"),
                provider = fObj.optString("provider", "gps"),
                hasSpeed = fObj.optBoolean("hasSpeed", true),
                isMock = fObj.optBoolean("isMock", false),
                speedDifferenceKmh = fObj.optDouble("speedDiffKmh").toFloat(),
                intervalSinceLastFixMs = fObj.optLong("intervalMs")
              )
            )
          }
        }
      }
    } catch (_: Exception) {}

    val effectiveStartSpeed = if (officialStart > 0f) officialStart else startGps
    val effectiveMaxGps = if (officialMax > 0f) officialMax else maxSpeed
    val effectiveEndSpeed = if (officialEnd > 0f) officialEnd else entity.endSpeed
    val effectiveGain = if (officialGain > 0f) officialGain else (effectiveMaxGps - effectiveStartSpeed).coerceAtLeast(0f)

    return RunResult(
      id = entity.id,
      timestamp = entity.createdAt.isoToTimestampMs(),
      vehicleId = entity.vehicleId,
      vehicleName = entity.name,
      officialStartSpeedKmh = effectiveStartSpeed,
      officialMaxSpeedKmh = effectiveMaxGps,
      officialEndSpeedKmh = effectiveEndSpeed,
      officialSpeedGainKmh = effectiveGain,
      runStartCalculatedSpeedKmh = entity.startSpeed,
      runStartGpsSpeedKmh = entity.startSpeed,
      startSpeedKmh = effectiveStartSpeed,
      maximumGpsSpeedKmh = effectiveMaxGps,
      maximumCalculatedSpeedKmh = maxCalcSpeed,
      maxIntegratedSpeedKmh = maxIntegratedSpeed,
      finalGpsSpeedKmh = effectiveEndSpeed,
      finalCalculatedSpeedKmh = finalCalcSpeed,
      finalIntegratedSpeedKmh = finalIntegratedSpeed,
      finalSpeedKmh = effectiveEndSpeed,
      speedGainKmh = effectiveGain,
      totalDistanceMeters = entity.distance,
      estimatedPowerCv = powerCv,
      estimatedTorqueKgfm = torqueKgfm,
      wheelPowerCv = wheelPower,
      enginePowerCv = powerCv,
      wheelPowerKw = (wheelPower * 735.49875f) / 1000f,
      enginePowerKw = (powerCv * 735.49875f) / 1000f,
      wheelTorqueKgfm = (torqueKgfm * 0.88f).sanitize(),
      engineTorqueKgfm = torqueKgfm,
      wheelTorqueNm = (torqueKgfm * 0.88f * 9.80665f).sanitize(),
      engineTorqueNm = (torqueKgfm * 9.80665f).sanitize(),
      peakLongitudinalG = entity.maxG,
      averageLongitudinalG = (entity.maxG * 0.6f).sanitize(),
      peakPowerRpm = entity.maxRpm,
      peakTorqueRpm = entity.maxRpm?.let { (it * 0.75f).toInt() },
      peakPowerSpeedKmh = maxSpeed,
      peakTorqueSpeedKmh = startGps + (maxSpeed - startGps) * 0.45f,
      totalVehicleMassKg = mass,
      curbWeightKg = curbWeight,
      driverWeightKg = driverWeight,
      passengerCount = passengerCount,
      passengerWeightKg = passengerWeight,
      additionalWeightKg = additionalWeight,
      soundSystemWeightKg = soundSystemWeight,
      cngWeightKg = cngWeight,
      fuelAdjustmentKg = fuelAdjustment,
      tireWidthMm = tireWidth,
      tireAspectRatio = tireAspect,
      rimInches = rimInches,
      isRecalculated = isRecalculated,
      revisionNumber = revisionNumber,
      parentRunId = parentRunId,
      recalculationReason = recalculationReason,
      recalculationNote = recalculationNote,
      previousConfigurationJson = previousConfigJson,
      previousCalculatedResultJson = previousCalcJson,
      drivetrainLossPercent = drivetrainLoss,
      estimatedMarginPercent = if (isPreliminary) 25f else 10f,
      gearUsed = gearUsed,
      gearIndexUsed = gearIndex,
      gearRatioUsed = gearRatio,
      finalDriveUsed = finalDrive,
      gpsDeltaVMps = gpsDeltaV,
      sensorDeltaVMps = sensorDeltaV,
      normalizationFactor = normFactor,
      rawAverageLongitudinalG = rawAvgG,
      anchoredAverageLongitudinalG = anchoredAvgG,
      isAerodynamicsEstimated = true,
      cdUsed = cd,
      frontalAreaUsed = frontalArea,
      crrUsed = crr,
      airDensityUsed = airDensity,
      slopeModeUsed = slopeMode,
      slopePercentUsed = slopePercent,
      confidenceLevel = entity.confidence,
      elapsedSeconds = entity.elapsedTime,
      gpsAccuracyMeters = entity.averageGpsAccuracy,
      averageGpsAccuracyMeters = entity.averageGpsAccuracy,
      totalSamples = entity.sampleCount,
      rejectedSamples = samples.count { !it.isValid },
      validSamplesCount = samples.count { it.isValid }.takeIf { it > 0 } ?: entity.sampleCount,
      validGpsLocationsCount = if (uniqueGpsFixCount > 0) uniqueGpsFixCount else (entity.elapsedTime * 1.5f).toInt().coerceAtLeast(1),
      locationCallbackCount = locationCallbackCount,
      uniqueGpsFixCount = if (uniqueGpsFixCount > 0) uniqueGpsFixCount else uniqueGpsFixesList.size,
      gpsSpeedChangeCount = gpsSpeedChangeCount,
      sensorSampleCount = if (sensorSampleCount > 0) sensorSampleCount else samples.size,
      maxGpsIntervalMs = maxGpsIntervalMs,
      maxGpsAgeMs = maxGpsAgeMs,
      gpsFrozen = gpsFrozen,
      isPreliminary = isPreliminary,
      averageSamplingRateHz = if (entity.elapsedTime > 0f) entity.sampleCount / entity.elapsedTime else 0f,
      averageGpsFrequencyHz = avgGpsFreq,
      quality = entity.quality,
      finishReason = entity.finishReason ?: "GPS_DECELERATION",
      averageSpeedDifferenceKmh = entity.averageSpeedDifferenceKmh,
      maximumSpeedDifferenceKmh = entity.maximumSpeedDifferenceKmh,
      invalidationReason = entity.invalidationReason,
      appVersion = entity.appVersion,
      testMode = testMode,
      targetStartSpeedKmh = targetStartSpeed,
      targetEndSpeedKmh = targetEndSpeed,
      accelRangeLabel = accelRangeLabel,
      gearShiftCount = gearShiftCount,
      speedUnit = speedUnit,
      estimatedMarginSeconds = estimatedMarginSeconds,
      accelerationSplits = accelerationSplitsList,
      time0to60Kmh = entity.time0to60Kmh,
      time0to100Kmh = entity.time0to100Kmh,
      time60to100Kmh = entity.time60to100Kmh,
      time80to120Kmh = entity.time80to120Kmh,
      time100to200Kmh = entity.time100to200Kmh,
      time60Feet = entity.time60Feet,
      time100M = entity.time100M,
      time201M = entity.time201M,
      time402M = entity.time402M,
      samples = samples,
      uniqueGpsFixes = uniqueGpsFixesList
    )
  }

  private fun mapRunSampleToEntity(s: RunSample, testId: String, index: Int): TestSampleEntity {
    return TestSampleEntity(
      id = "$testId-$index",
      testId = testId,
      sampleIndex = index,
      timestamp = (s.timestampMs.takeIf { it > 0L } ?: System.currentTimeMillis()).toIsoUtc(),
      elapsedTimeMs = s.elapsedTimeMs,
      speed = s.rawGpsSpeedKmh.takeIf { it > 0f } ?: s.gpsSpeedKmh.safeFiniteFloat(),
      filteredSpeed = s.filteredSpeedKmh.takeIf { it > 0f } ?: s.calculatedSpeedKmh.safeFiniteFloat(),
      acceleration = s.finalAccelerationMps2.safeFiniteFloat(),
      filteredAccelerationZ = s.filteredAccelerationZ.safeFiniteFloat(),
      correctedAccelerationZ = s.correctedAccelerationZ.safeFiniteFloat(),
      longitudinalG = s.longitudinalG.safeFiniteFloat(),
      rpm = s.engineRpm,
      distance = s.distanceMeters.safeFiniteFloat(),
      wheelPowerCv = s.wheelPowerCv.safeFiniteFloat(),
      enginePowerCv = s.enginePowerCv.safeFiniteFloat(),
      torqueKgfm = s.engineTorqueKgfm.safeFiniteFloat(),
      gpsAccuracy = s.gpsAccuracyMeters.safeFiniteFloat(),
      gyroMagnitude = s.gyroMagnitude.safeFiniteFloat(),
      confidence = s.confidenceLevel.ifBlank { "ALTA" },
      isValid = s.isValid,
      rejectionReason = s.rejectionReason
    )
  }

  private fun mapEntityToRunSample(entity: TestSampleEntity): RunSample {
    return RunSample(
      timestampMs = entity.timestamp.isoToTimestampMs(),
      elapsedTimeMs = entity.elapsedTimeMs,
      rawGpsSpeedKmh = entity.speed,
      filteredSpeedKmh = entity.filteredSpeed,
      gpsSpeedKmh = entity.speed,
      calculatedSpeedKmh = entity.filteredSpeed,
      finalAccelerationMps2 = entity.acceleration,
      filteredAccelerationZ = entity.filteredAccelerationZ,
      correctedAccelerationZ = entity.correctedAccelerationZ,
      longitudinalG = entity.longitudinalG,
      engineRpm = entity.rpm,
      distanceMeters = entity.distance,
      wheelPowerCv = entity.wheelPowerCv,
      enginePowerCv = entity.enginePowerCv,
      wheelTorqueKgfm = entity.torqueKgfm * 0.88f,
      engineTorqueKgfm = entity.torqueKgfm,
      wheelTorqueNm = entity.torqueKgfm * 0.88f * 9.80665f,
      engineTorqueNm = entity.torqueKgfm * 9.80665f,
      gpsAccuracyMeters = entity.gpsAccuracy,
      gyroMagnitude = entity.gyroMagnitude,
      confidenceLevel = entity.confidence,
      isValid = entity.isValid,
      rejectionReason = entity.rejectionReason
    )
  }

  private fun createConfigurationSnapshot(r: RunResult): String {
    return try {
      val obj = JSONObject()
      obj.put("totalMassKg", r.totalVehicleMassKg.safeFinite(0.0))
      obj.put("curbWeightKg", r.curbWeightKg.safeFinite(0.0))
      obj.put("driverWeightKg", r.driverWeightKg.safeFinite(0.0))
      obj.put("passengerCount", r.passengerCount)
      obj.put("passengerWeightKg", r.passengerWeightKg.safeFinite(0.0))
      obj.put("additionalWeightKg", r.additionalWeightKg.safeFinite(0.0))
      obj.put("soundSystemWeightKg", r.soundSystemWeightKg.safeFinite(0.0))
      obj.put("cngWeightKg", r.cngWeightKg.safeFinite(0.0))
      obj.put("fuelAdjustmentKg", r.fuelAdjustmentKg.safeFinite(0.0))
      obj.put("tireWidthMm", r.tireWidthMm)
      obj.put("tireAspectRatio", r.tireAspectRatio)
      obj.put("rimInches", r.rimInches)
      obj.put("isRecalculated", r.isRecalculated)
      obj.put("revisionNumber", r.revisionNumber)
      r.parentRunId?.let { obj.put("parentRunId", it) }
      r.recalculationReason?.let { obj.put("recalculationReason", it) }
      r.recalculationNote?.let { obj.put("recalculationNote", it) }
      r.previousConfigurationJson?.let { obj.put("previousConfigurationJson", it) }
      r.previousCalculatedResultJson?.let { obj.put("previousCalculatedResultJson", it) }
      obj.put("gearUsed", r.gearUsed)
      obj.put("gearIndex", r.gearIndexUsed)
      obj.put("gearRatio", r.gearRatioUsed.safeFinite(2.14))
      obj.put("finalDrive", r.finalDriveUsed.safeFinite(4.19))
      obj.put("gpsDeltaVMps", r.gpsDeltaVMps.safeFinite(0.0))
      obj.put("sensorDeltaVMps", r.sensorDeltaVMps.safeFinite(0.0))
      obj.put("normalizationFactor", r.normalizationFactor.safeFinite(1.0))
      obj.put("rawAverageLongitudinalG", r.rawAverageLongitudinalG.safeFinite(0.0))
      obj.put("anchoredAverageLongitudinalG", r.anchoredAverageLongitudinalG.safeFinite(0.0))
      obj.put("drivetrainLossPercent", r.drivetrainLossPercent.safeFinite(15.0))
      obj.put("cd", r.cdUsed.safeFinite(0.34))
      obj.put("frontalAreaM2", r.frontalAreaUsed.safeFinite(2.10))
      obj.put("crr", r.crrUsed.safeFinite(0.015))
      obj.put("airDensityKgM3", r.airDensityUsed.safeFinite(1.225))
      obj.put("slopeMode", r.slopeModeUsed)
      obj.put("slopePercent", r.slopePercentUsed.safeFinite(0.0))
      obj.put("testMode", r.testMode)
      obj.put("targetStartSpeedKmh", r.targetStartSpeedKmh.safeFinite(0.0))
      obj.put("targetEndSpeedKmh", r.targetEndSpeedKmh.safeFinite(0.0))
      obj.put("accelRangeLabel", r.accelRangeLabel)
      obj.put("gearShiftCount", r.gearShiftCount)
      obj.put("speedUnit", r.speedUnit)
      obj.put("estimatedMarginSeconds", r.estimatedMarginSeconds.safeFinite(0.08))

      if (r.accelerationSplits.isNotEmpty()) {
        val splitsArr = JSONArray()
        for (split in r.accelerationSplits) {
          val sObj = JSONObject()
          sObj.put("label", split.label)
          sObj.put("startSpeedKmh", split.startSpeedKmh.safeFinite(0.0))
          sObj.put("endSpeedKmh", split.endSpeedKmh.safeFinite(0.0))
          sObj.put("timeSeconds", split.timeSeconds.safeFinite(0.0))
          splitsArr.put(sObj)
        }
        obj.put("accelerationSplits", splitsArr)
      }

      obj.put("startSpeedKmh", r.startSpeedKmh.safeFinite(40.0))
      obj.put("endSpeedKmh", r.finalSpeedKmh.safeFinite(0.0))
      obj.put("officialStartSpeedKmh", r.officialStartSpeedKmh.safeFinite(0.0))
      obj.put("officialMaxSpeedKmh", r.officialMaxSpeedKmh.safeFinite(0.0))
      obj.put("officialEndSpeedKmh", r.officialEndSpeedKmh.safeFinite(0.0))
      obj.put("officialSpeedGainKmh", r.officialSpeedGainKmh.safeFinite(0.0))
      obj.put("maximumGpsSpeedKmh", r.maximumGpsSpeedKmh.safeFinite(0.0))
      obj.put("maximumCalculatedSpeedKmh", r.maximumCalculatedSpeedKmh.safeFinite(0.0))
      obj.put("maxIntegratedSpeedKmh", r.maxIntegratedSpeedKmh.safeFinite(0.0))
      obj.put("finalGpsSpeedKmh", r.finalGpsSpeedKmh.safeFinite(0.0))
      obj.put("finalCalculatedSpeedKmh", r.finalCalculatedSpeedKmh.safeFinite(0.0))
      obj.put("finalIntegratedSpeedKmh", r.finalIntegratedSpeedKmh.safeFinite(0.0))
      obj.put("locationCallbackCount", r.locationCallbackCount)
      obj.put("uniqueGpsFixCount", r.uniqueGpsFixCount)
      obj.put("gpsSpeedChangeCount", r.gpsSpeedChangeCount)
      obj.put("sensorSampleCount", r.sensorSampleCount)
      obj.put("maxGpsIntervalMs", r.maxGpsIntervalMs)
      obj.put("maxGpsAgeMs", r.maxGpsAgeMs)
      obj.put("gpsFrozen", r.gpsFrozen)
      obj.put("isPreliminary", r.isPreliminary)
      obj.put("averageGpsFrequencyHz", r.averageGpsFrequencyHz.safeFinite(0.0))

      if (r.uniqueGpsFixes.isNotEmpty()) {
        val fixesArr = JSONArray()
        for (fix in r.uniqueGpsFixes) {
          val fObj = JSONObject()
          fObj.put("elapsedRealtimeNanos", fix.elapsedRealtimeNanos)
          fObj.put("timestamp", fix.timestamp)
          fObj.put("speedKmh", fix.speedKmh.safeFinite(0.0))
          fObj.put("speedAccuracyMps", fix.speedAccuracyMetersPerSecond.safeFinite(0.0))
          fObj.put("accuracyM", fix.accuracyMeters.safeFinite(0.0))
          fObj.put("ageMillis", fix.ageMillis)
          fObj.put("provider", fix.provider)
          fObj.put("hasSpeed", fix.hasSpeed)
          fObj.put("isMock", fix.isMock)
          fObj.put("speedDiffKmh", fix.speedDifferenceKmh.safeFinite(0.0))
          fObj.put("intervalMs", fix.intervalSinceLastFixMs)
          fixesArr.put(fObj)
        }
        obj.put("uniqueGpsFixes", fixesArr)
      }
      obj.toString()
    } catch (e: Exception) {
      Log.e(TAG, "[DynoStorage] Falha ao criar configurationSnapshot: ${e.message}", e)
      "{}"
    }
  }

  fun createSnapshotFromProfile(profile: VehicleProfile?, selectedGearRatio: Float, selectedFinalDrive: Float, selectedGear: String, slopeMode: String, slopePercent: Float): String {
    return try {
      val obj = JSONObject()
      if (profile != null) {
        obj.put("totalMassKg", profile.totalWeightKg.safeFinite(1000.0))
        obj.put("tireWidthMm", profile.tireWidthMm)
        obj.put("tireAspectRatio", profile.tireAspectRatio)
        obj.put("wheelDiameterInches", profile.wheelDiameterInches)
        obj.put("tireCorrectionPercent", profile.tireCorrectionPercent.safeFinite(0.0))
        obj.put("drivetrain", profile.drivetrain)
        obj.put("drivetrainLossPercent", profile.customDrivetrainLossPercent.safeFinite(12.0))
        obj.put("cd", profile.dragCoefficient.safeFinite(0.34))
        obj.put("frontalAreaM2", profile.frontalAreaM2.safeFinite(2.10))
        obj.put("crr", profile.rollingResistanceCoeff.safeFinite(0.015))
        obj.put("airDensityKgM3", profile.airDensityKgM3.safeFinite(1.225))
      }
      obj.put("gearUsed", selectedGear)
      obj.put("gearRatio", selectedGearRatio.safeFinite(1.95))
      obj.put("finalDrive", selectedFinalDrive.safeFinite(4.19))
      obj.put("slopeMode", slopeMode)
      obj.put("slopePercent", slopePercent.safeFinite(0.0))
      obj.toString()
    } catch (e: Exception) {
      Log.e(TAG, "[DynoStorage] Falha ao criar snapshot do perfil: ${e.message}", e)
      "{}"
    }
  }

  private fun deserializeLegacyJson(obj: JSONObject): RunResult {
    val samplesList = mutableListOf<RunSample>()
    if (obj.has("samples")) {
      val samplesArray = obj.getJSONArray("samples")
      for (j in 0 until samplesArray.length()) {
        val sObj = samplesArray.getJSONObject(j)
        val filtAz = sObj.optDouble("az", 0.0).toFloat()
        samplesList.add(
          RunSample(
            elapsedTimeMs = sObj.optLong("t", 0L),
            filteredAccelerationZ = filtAz,
            correctedAccelerationZ = sObj.optDouble("cz", 0.0).toFloat(),
            longitudinalG = sObj.optDouble("g", (filtAz / 9.80665).toDouble()).toFloat(),
            gpsSpeedKmh = sObj.optDouble("gps", 0.0).toFloat(),
            calculatedSpeedKmh = sObj.optDouble("calc", 0.0).toFloat(),
            speedDifferenceKmh = sObj.optDouble("diff", 0.0).toFloat(),
            gpsAccuracyMeters = sObj.optDouble("acc", 0.0).toFloat(),
            gyroMagnitude = sObj.optDouble("gyro", 0.0).toFloat(),
            wheelPowerCv = sObj.optDouble("wp", 0.0).toFloat(),
            enginePowerCv = sObj.optDouble("ep", 0.0).toFloat(),
            wheelTorqueKgfm = sObj.optDouble("wt", 0.0).toFloat(),
            engineTorqueKgfm = sObj.optDouble("et", 0.0).toFloat(),
            engineRpm = if (sObj.has("rpm")) sObj.getInt("rpm") else null,
            isValid = sObj.optBoolean("val", true),
            rejectionReason = if (sObj.has("rej")) sObj.getString("rej") else null
          )
        )
      }
    }

    val total = obj.optInt("totalSamples", samplesList.size)
    val rej = obj.optInt("rejectedSamples", samplesList.count { !it.isValid })
    val valid = obj.optInt("validSamplesCount", if (total >= rej) total - rej else 0)
    val maxGps = obj.optDouble("maximumGpsSpeedKmh", 0.0).toFloat()
    val startGps = obj.optDouble("runStartGpsSpeedKmh", 0.0).toFloat()
    val estPower = obj.optDouble("estimatedPowerCv", 0.0).toFloat()
    val estTorque = obj.optDouble("estimatedTorqueKgfm", 0.0).toFloat()

    return RunResult(
      id = obj.optString("id", UUID.randomUUID().toString()),
      timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
      vehicleId = if (obj.has("vehicleId")) obj.getString("vehicleId") else null,
      vehicleName = obj.optString("vehicleName", ""),
      runStartCalculatedSpeedKmh = obj.optDouble("runStartCalculatedSpeedKmh", 40.0).toFloat(),
      runStartGpsSpeedKmh = startGps,
      startSpeedKmh = startGps,
      maximumGpsSpeedKmh = maxGps,
      maximumCalculatedSpeedKmh = obj.optDouble("maximumCalculatedSpeedKmh", 40.0).toFloat(),
      finalGpsSpeedKmh = obj.optDouble("finalGpsSpeedKmh", 0.0).toFloat(),
      finalCalculatedSpeedKmh = obj.optDouble("finalCalculatedSpeedKmh", 0.0).toFloat(),
      finalSpeedKmh = obj.optDouble("finalGpsSpeedKmh", 0.0).toFloat(),
      speedGainKmh = obj.optDouble("speedGainKmh", (maxGps - startGps).coerceAtLeast(0f).toDouble()).toFloat(),
      estimatedPowerCv = estPower,
      estimatedTorqueKgfm = estTorque,
      wheelPowerCv = obj.optDouble("wheelPowerCv", (estPower * 0.85).toDouble()).toFloat(),
      enginePowerCv = obj.optDouble("enginePowerCv", estPower.toDouble()).toFloat(),
      wheelTorqueKgfm = obj.optDouble("wheelTorqueKgfm", (estTorque * 0.85).toDouble()).toFloat(),
      engineTorqueKgfm = obj.optDouble("engineTorqueKgfm", estTorque.toDouble()).toFloat(),
      peakLongitudinalG = obj.optDouble("peakLongitudinalG", 0.0).toFloat(),
      averageLongitudinalG = obj.optDouble("averageLongitudinalG", 0.0).toFloat(),
      peakPowerRpm = if (obj.has("peakPowerRpm")) obj.getInt("peakPowerRpm") else null,
      peakTorqueRpm = if (obj.has("peakTorqueRpm")) obj.getInt("peakTorqueRpm") else null,
      peakPowerSpeedKmh = obj.optDouble("peakPowerSpeedKmh", maxGps.toDouble()).toFloat(),
      peakTorqueSpeedKmh = obj.optDouble("peakTorqueSpeedKmh", (startGps + (maxGps - startGps) * 0.45).toDouble()).toFloat(),
      totalVehicleMassKg = obj.optDouble("totalVehicleMassKg", 0.0).toFloat(),
      drivetrainLossPercent = obj.optDouble("drivetrainLossPercent", 15.0).toFloat(),
      estimatedMarginPercent = obj.optDouble("estimatedMarginPercent", 10.0).toFloat(),
      gearUsed = obj.optString("gearUsed", "2ª"),
      isAerodynamicsEstimated = obj.optBoolean("isAerodynamicsEstimated", true),
      elapsedSeconds = obj.optDouble("elapsedSeconds", 0.0).toFloat(),
      gpsAccuracyMeters = obj.optDouble("gpsAccuracyMeters", 0.0).toFloat(),
      totalSamples = total,
      rejectedSamples = rej,
      validSamplesCount = valid,
      validGpsLocationsCount = obj.optInt("validGpsLocationsCount", 4),
      averageSamplingRateHz = obj.optDouble("averageSamplingRateHz", 0.0).toFloat(),
      averageGpsFrequencyHz = obj.optDouble("averageGpsFrequencyHz", 0.0).toFloat(),
      quality = obj.optString("quality", "BOA"),
      finishReason = obj.optString("finishReason", "GPS_DECELERATION"),
      averageSpeedDifferenceKmh = obj.optDouble("averageSpeedDifferenceKmh", 0.0).toFloat(),
      maximumSpeedDifferenceKmh = obj.optDouble("maximumSpeedDifferenceKmh", 0.0).toFloat(),
      invalidationReason = if (obj.has("invalidationReason")) obj.getString("invalidationReason") else null,
      appVersion = obj.optString("appVersion", "0.20.0"),
      samples = samplesList
    )
  }
}

fun Float?.safeFinite(default: Double = 0.0): Double {
  if (this == null || this.isNaN() || this.isInfinite()) return default
  return this.toDouble()
}

fun Double?.safeFinite(default: Double = 0.0): Double {
  if (this == null || this.isNaN() || this.isInfinite()) return default
  return this
}

fun Float?.safeFiniteFloat(default: Float = 0f): Float {
  if (this == null || this.isNaN() || this.isInfinite()) return default
  return this
}

fun Double?.safeFiniteFloat(default: Float = 0f): Float {
  if (this == null || this.isNaN() || this.isInfinite()) return default
  return this.toFloat()
}

private fun Float?.sanitize(): Float {
  if (this == null || this.isNaN() || this.isInfinite()) return 0f
  return this
}
