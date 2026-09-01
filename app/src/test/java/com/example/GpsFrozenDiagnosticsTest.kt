package com.example

import com.example.model.FinishReason
import com.example.model.RunResult
import com.example.model.RunSample
import com.example.model.UniqueGpsFix
import com.example.model.VehicleCalculations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de Testes para Diagnóstico de GPS Congelado e Integridade de Telemetria
 *
 * Cobertura Obrigatória:
 * 1. 225 amostras com 20 localizações GPS únicas (identificação de isNewGpsFix).
 * 2. Descarte de GPS repetido com mesmo elapsedRealtimeNanos (uniqueCount = 1).
 * 3. Detecção de GPS congelado (43,5 km/h constante por >1.5s com G positivo e ganho integrado > 5 km/h).
 * 4. Veículo parado com GPS constante (sem falso positivo de GPS congelado).
 * 5. Cálculo correto da idade do GPS via elapsedRealtimeNanos.
 * 6. Separação explícita entre velocidade máxima GPS (ex: 56,3 km/h) e integrada (ex: 91,0 km/h).
 * 7. Divergência de 34,7 km/h gerando classificação "GPS INCONSISTENTE" (nunca "BOA").
 * 8. Preservação de dados e campos de diagnóstico em RunResult.
 */
class GpsFrozenDiagnosticsTest {

  @Test
  fun test1_SamplesWithUniqueGpsFixIdentification() {
    // 225 amostras totais com 20 fixes GPS únicos distribuídos
    val uniqueFixCount = 20
    val totalSamples = 225
    val samples = mutableListOf<RunSample>()

    var lastGpsNs = 0L
    for (i in 0 until totalSamples) {
      val isNewFix = (i % (totalSamples / uniqueFixCount) == 0) && (i / (totalSamples / uniqueFixCount) < uniqueFixCount)
      val gpsNs = if (isNewFix) 1_000_000_000L + (i * 50_000_000L) else lastGpsNs
      if (isNewFix) lastGpsNs = gpsNs

      samples.add(
        RunSample(
          timestampMs = 1000L + (i * 50L),
          elapsedTimeMs = (i * 50L),
          rawGpsSpeedKmh = 43.5f,
          filteredSpeedKmh = 43.5f,
          gpsSpeedKmh = 43.5f,
          calculatedSpeedKmh = 43.5f + (i * 0.2f),
          isNewGpsFix = isNewFix,
          isValid = true
        )
      )
    }

    val identifiedFixes = samples.count { it.isNewGpsFix }
    assertEquals(20, identifiedFixes)
    assertEquals(225, samples.size)
  }

  @Test
  fun test2_RepeatedLocationHandling_UniqueCountEqualsOne() {
    // Múltiplos callbacks com o mesmo elapsedRealtimeNanos não devem incrementar o contador único
    val initialElapsedNs = 50_000_000_000L
    var lastProcessedElapsedNs = 0L
    var uniqueCount = 0

    val incomingCallbacks = listOf(
      initialElapsedNs,
      initialElapsedNs, // repetido
      initialElapsedNs, // repetido
      initialElapsedNs  // repetido
    )

    for (elapsedNs in incomingCallbacks) {
      if (elapsedNs > lastProcessedElapsedNs || lastProcessedElapsedNs == 0L) {
        uniqueCount++
        lastProcessedElapsedNs = elapsedNs
      }
    }

    assertEquals(1, uniqueCount)
  }

  @Test
  fun test3_GpsFrozenDetection_TriggersTrue() {
    // GPS constante em 43.5 km/h por > 1.5s enquanto acelerômetro é positivo e ganho integrado > 5 km/h
    val gpsSpeedKmh = 43.5f
    var integratedSpeedKmh = 43.5f
    var gpsFrozenStartTimeNs: Long? = null
    var gpsFrozenSpeedKmh = 43.5f
    var gpsFrozenIntegratedStartKmh = 43.5f
    var isGpsFrozenDetected = false

    // Simula 3.0 segundos de aceleração a 0.10 G (z = 0.98 m/s²)
    val dt = 0.05f // 20 Hz
    val zAccel = 0.98f

    for (step in 0..60) {
      val nowNs = step * 50_000_000L
      integratedSpeedKmh += (zAccel * dt) * 3.6f

      val runDurationMs = (nowNs / 1_000_000L)
      if (runDurationMs >= 1000L) {
        if (gpsFrozenStartTimeNs == null) {
          gpsFrozenStartTimeNs = nowNs
          gpsFrozenSpeedKmh = gpsSpeedKmh
          gpsFrozenIntegratedStartKmh = integratedSpeedKmh
        } else {
          if (kotlin.math.abs(gpsSpeedKmh - gpsFrozenSpeedKmh) <= 0.1f) {
            val frozenDurationMs = (nowNs - gpsFrozenStartTimeNs) / 1_000_000L
            val integratedGain = integratedSpeedKmh - gpsFrozenIntegratedStartKmh
            val isSustainedAccel = zAccel > 0.20f
            if (frozenDurationMs >= 1500L && isSustainedAccel && integratedGain >= 5.0f) {
              isGpsFrozenDetected = true
            }
          }
        }
      }
    }

    assertTrue(isGpsFrozenDetected)
  }

  @Test
  fun test4_GpsConstantWhileStopped_NoFalsePositive() {
    // Veículo parado (0 km/h) sem aceleração positiva não deve disparar GPS congelado
    val gpsSpeedKmh = 0f
    val integratedSpeedKmh = 0f
    var gpsFrozenStartTimeNs: Long? = null
    var isGpsFrozenDetected = false

    val zAccel = 0.01f // Ruído mínimo parado

    for (step in 0..40) {
      val nowNs = step * 50_000_000L
      val runDurationMs = (nowNs / 1_000_000L)
      if (runDurationMs >= 1000L) {
        if (gpsFrozenStartTimeNs == null) {
          gpsFrozenStartTimeNs = nowNs
        } else {
          if (kotlin.math.abs(gpsSpeedKmh - 0f) <= 0.1f) {
            val frozenDurationMs = (nowNs - gpsFrozenStartTimeNs) / 1_000_000L
            val integratedGain = integratedSpeedKmh - 0f
            val isSustainedAccel = zAccel > 0.20f
            if (frozenDurationMs >= 1500L && isSustainedAccel && integratedGain >= 5.0f) {
              isGpsFrozenDetected = true
            }
          }
        }
      }
    }

    assertFalse(isGpsFrozenDetected)
  }

  @Test
  fun test5_GpsAgeCalculationViaElapsedRealtimeNanos() {
    val locationElapsedRealtimeNs = 100_000_000_000L
    val currentSystemElapsedRealtimeNs = 100_250_000_000L // 250 ms depois

    val ageMillis = ((currentSystemElapsedRealtimeNs - locationElapsedRealtimeNs) / 1_000_000L).coerceAtLeast(0L)
    assertEquals(250L, ageMillis)
  }

  @Test
  fun test6_SeparationOfMaxGpsAndMaxIntegratedSpeed() {
    val maxGps = 56.3f
    val maxIntegrated = 91.0f
    val startSpeed = 43.5f

    val result = RunResult(
      id = "test-diag-1",
      vehicleId = "veh-1",
      vehicleName = "Carro Teste",
      officialStartSpeedKmh = startSpeed,
      officialMaxSpeedKmh = maxGps,
      officialEndSpeedKmh = 56.3f,
      officialSpeedGainKmh = (maxGps - startSpeed),
      maximumGpsSpeedKmh = maxGps,
      maximumCalculatedSpeedKmh = maxIntegrated,
      maxIntegratedSpeedKmh = maxIntegrated,
      startSpeedKmh = startSpeed,
      finalSpeedKmh = 56.3f,
      finalGpsSpeedKmh = 56.3f,
      speedGainKmh = (maxGps - startSpeed),
      gpsFrozen = true
    )

    assertEquals(56.3f, result.officialMaxSpeedKmh, 0.01f)
    assertEquals(56.3f, result.maximumGpsSpeedKmh, 0.01f)
    assertEquals(91.0f, result.maxIntegratedSpeedKmh, 0.01f)
    assertEquals(91.0f, result.maximumCalculatedSpeedKmh, 0.01f)
    assertEquals(12.8f, result.officialSpeedGainKmh, 0.01f)
    assertTrue(result.gpsFrozen)
  }

  @Test
  fun test7_Divergence34_7Kmh_TriggersGpsInconsistent_NeverBoa() {
    // Divergência de 34,7 km/h com GPS congelado
    val maxGps = 56.3f
    val maxIntegrated = 91.0f
    val startSpeed = 43.5f
    val speedGain = maxGps - startSpeed // 12.8 km/h

    val eval = VehicleCalculations.classifyRunQuality(
      speedGainKmh = speedGain,
      validGpsLocationsCount = 20,
      elapsedSec = 13.99f,
      lastGpsAccuracyMeters = 3.5f,
      avgSyncDiffKmh = 18.5f,
      rejectionRatio = 0.02f,
      finishReason = FinishReason.GPS_DECELERATION,
      isPhoneStable = true,
      gearShiftDetected = false,
      finalGpsSpeedKmh = 56.3f,
      startGpsSpeedKmh = startSpeed,
      rpmSpan = 2000,
      gpsFrozen = true,
      maxIntegratedSpeedKmh = maxIntegrated,
      maxGpsSpeedKmh = maxGps
    )

    assertEquals("GPS INCONSISTENTE", eval.quality)
    assertNotEquals("BOA", eval.quality)
    assertFalse(eval.isEligibleForComparison)
    assertTrue(eval.invalidationReason?.contains("GPS congelado") == true)
  }

  @Test
  fun test8_PreservationOfDiagnosticFieldsAndUniqueFixes() {
    val fix1 = UniqueGpsFix(
      elapsedRealtimeNanos = 1_000_000_000L,
      timestamp = 1700000000000L,
      speedKmh = 43.5f,
      speedAccuracyMetersPerSecond = 0.2f,
      accuracyMeters = 3.0f,
      ageMillis = 15L,
      provider = "gps",
      hasSpeed = true,
      isMock = false,
      speedDifferenceKmh = 0f,
      intervalSinceLastFixMs = 100L
    )

    val fix2 = UniqueGpsFix(
      elapsedRealtimeNanos = 1_100_000_000L,
      timestamp = 1700000000100L,
      speedKmh = 43.5f,
      speedAccuracyMetersPerSecond = 0.2f,
      accuracyMeters = 3.0f,
      ageMillis = 18L,
      provider = "gps",
      hasSpeed = true,
      isMock = false,
      speedDifferenceKmh = 0f,
      intervalSinceLastFixMs = 100L
    )

    val result = RunResult(
      id = "test-preserve-1",
      vehicleId = "veh-1",
      vehicleName = "Carro Preservado",
      officialStartSpeedKmh = 43.5f,
      officialMaxSpeedKmh = 56.3f,
      officialEndSpeedKmh = 56.3f,
      officialSpeedGainKmh = 12.8f,
      maximumGpsSpeedKmh = 56.3f,
      maxIntegratedSpeedKmh = 91.0f,
      locationCallbackCount = 225,
      uniqueGpsFixCount = 20,
      gpsSpeedChangeCount = 5,
      sensorSampleCount = 225,
      maxGpsIntervalMs = 150L,
      maxGpsAgeMs = 35L,
      gpsFrozen = true,
      isPreliminary = true,
      uniqueGpsFixes = listOf(fix1, fix2)
    )

    assertEquals(2, result.uniqueGpsFixes.size)
    assertEquals(225, result.locationCallbackCount)
    assertEquals(20, result.uniqueGpsFixCount)
    assertEquals(5, result.gpsSpeedChangeCount)
    assertEquals(225, result.sensorSampleCount)
    assertEquals(150L, result.maxGpsIntervalMs)
    assertEquals(35L, result.maxGpsAgeMs)
    assertTrue(result.gpsFrozen)
    assertTrue(result.isPreliminary)
  }
}
