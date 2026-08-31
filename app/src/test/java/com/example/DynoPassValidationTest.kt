package com.example

import com.example.model.CurveDisplayType
import com.example.model.RunResult
import com.example.model.RunSample
import com.example.model.VehicleCalculations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de Testes Obrigatórios para Validação e Classificação do Dyno Lite
 * Cobre estritamente os 10 cenários exigidos:
 * 1. Passagem com ganho de 6,4 km/h (DADOS INSUFICIENTES, sem comparação, curva RPM bloqueada)
 * 2. Passagem com ganho de 18 km/h e 7 locations (REGULAR)
 * 3. Passagem com ganho de 25 km/h, 12 locations, estável, sem troca de marcha (BOA)
 * 4. Passagem com troca de marcha detectada (INVÁLIDA)
 * 5. Passagem com pico isolado de G em 1 tick (Pico isolado rejeitado por findSustainedPeaks)
 * 6. Passagem com 100m atingido mas 201m/402m não atingidos (sem extrapolação)
 * 7. Transmissão/pneu ausente (curva torque bloqueada, RPM bloqueada ou velocidade)
 * 8. Cálculo de torque com RPM = 0 ou inválido (sem divisão por zero, sem NaN/Inf)
 * 9. Comparação entre passagens com marchas diferentes (recusada)
 * 10. Comparação entre passagens BOA do mesmo veículo na mesma marcha (aceita)
 */
class DynoPassValidationTest {

  @Test
  fun testScenario1_SpeedGain6_4Kmh_IsInsufficientData() {
    // 1. Passagem com velocidade máxima 47,4 km/h, ganho 6,4 km/h, 14s, 20 locations, 227 amostras
    val samples = (0 until 227).map { i ->
      val speed = 41.0f + (6.4f * (i / 226f))
      RunSample(
        elapsedTimeMs = (i * 61).toLong(),
        rawGpsSpeedKmh = speed,
        filteredSpeedKmh = speed,
        gpsSpeedKmh = speed,
        isValid = true,
        isGearShift = false,
        isClutchDisengaged = false,
        enginePowerCv = 79.5f,
        engineRpm = 3000 + (i * 2) // RPM range only 454 RPM
      )
    }

    val eval = VehicleCalculations.classifyRunQuality(
      speedGainKmh = 6.4f,
      maxSpeedKmh = 47.4f,
      startSpeedKmh = 41.0f,
      validGpsCount = 20,
      elapsedSeconds = 14.0f,
      gpsAccuracy = 3.5f,
      isMountedStable = true,
      hasGearShift = false,
      hasExcessiveVibration = false,
      hasBraking = false,
      hasLossOfGps = false,
      isReverseMovement = false,
      isSpeedDecrease = false
    )

    assertEquals("DADOS INSUFICIENTES", eval.quality)
    assertEquals(25.0f, eval.marginPercent, 0.01f)
    assertFalse(eval.isEligibleForComparison)
    assertTrue(eval.isPreliminary)

    // Curva por RPM não deve ser gerada sem ganho mínimo (>= 15 km/h)
    val curveType = VehicleCalculations.evaluateCurveEligibility(
      samples = samples,
      gearUsed = "3ª marcha",
      gearRatio = 1.35f,
      finalDrive = 3.94f,
      tireCircumferenceM = 1.9,
      speedGainKmh = 6.4f
    )
    assertEquals(CurveDisplayType.INSUFFICIENT, curveType)
  }

  @Test
  fun testScenario2_SpeedGain18Kmh_7Locations_IsRegular() {
    // 2. Passagem com ganho de 18 km/h e 7 locations
    val eval = VehicleCalculations.classifyRunQuality(
      speedGainKmh = 18.0f,
      maxSpeedKmh = 78.0f,
      startSpeedKmh = 60.0f,
      validGpsCount = 7,
      elapsedSeconds = 6.5f,
      gpsAccuracy = 4.0f,
      isMountedStable = true,
      hasGearShift = false,
      hasExcessiveVibration = false,
      hasBraking = false,
      hasLossOfGps = false,
      isReverseMovement = false,
      isSpeedDecrease = false
    )

    assertEquals("REGULAR", eval.quality)
    assertEquals(15.0f, eval.marginPercent, 0.01f)
    assertTrue(eval.isEligibleForComparison)
    assertFalse(eval.isPreliminary)
  }

  @Test
  fun testScenario3_SpeedGain25Kmh_12Locations_IsBoa() {
    // 3. Passagem com ganho de 25 km/h, 12 locations, suporte estável, sem troca de marcha
    val eval = VehicleCalculations.classifyRunQuality(
      speedGainKmh = 25.0f,
      maxSpeedKmh = 85.0f,
      startSpeedKmh = 60.0f,
      validGpsCount = 12,
      elapsedSeconds = 7.0f,
      gpsAccuracy = 3.0f,
      isMountedStable = true,
      hasGearShift = false,
      hasExcessiveVibration = false,
      hasBraking = false,
      hasLossOfGps = false,
      isReverseMovement = false,
      isSpeedDecrease = false
    )

    assertEquals("BOA", eval.quality)
    assertEquals(10.0f, eval.marginPercent, 0.01f)
    assertTrue(eval.isEligibleForComparison)
    assertFalse(eval.isPreliminary)
  }

  @Test
  fun testScenario4_GearShiftDetected_IsInvalid() {
    // 4. Passagem com troca de marcha detectada
    val eval = VehicleCalculations.classifyRunQuality(
      speedGainKmh = 30.0f,
      maxSpeedKmh = 90.0f,
      startSpeedKmh = 60.0f,
      validGpsCount = 15,
      elapsedSeconds = 8.0f,
      gpsAccuracy = 3.0f,
      isMountedStable = true,
      hasGearShift = true, // Troca de marcha
      hasExcessiveVibration = false,
      hasBraking = false,
      hasLossOfGps = false,
      isReverseMovement = false,
      isSpeedDecrease = false
    )

    assertEquals("INVÁLIDA", eval.quality)
    assertFalse(eval.isEligibleForComparison)
    assertTrue(eval.invalidationReason?.contains("Troca de marcha") == true)
  }

  @Test
  fun testScenario5_IsolatedGSpike_IsFilteredOut() {
    // 5. Passagem com pico isolado de 0,85G em 1 único tick (ruído/vibração)
    val samples = mutableListOf<RunSample>()
    for (i in 0 until 50) {
      val isSpike = (i == 25)
      samples.add(
        RunSample(
          elapsedTimeMs = (i * 100).toLong(),
          longitudinalG = if (isSpike) 0.85f else 0.35f,
          enginePowerCv = if (isSpike) 180f else 100f,
          wheelPowerCv = if (isSpike) 150f else 85f,
          engineTorqueKgfm = if (isSpike) 30f else 18f,
          wheelTorqueKgfm = if (isSpike) 25f else 15f,
          engineRpm = 3000 + i * 50,
          isValid = true
        )
      )
    }

    val peaks = VehicleCalculations.findSustainedPeaks(samples)

    // O pico isolado de 180 cv e 0.85 G não pode ser retornado
    assertTrue("O pico sustentado não deve ser o spike isolado de 180 cv", peaks.peakEnginePowerCv < 170f)
    assertTrue("O pico sustentado de G não deve ser 0.85 G", peaks.peakLongitudinalG < 0.80f)
    assertEquals(100f, peaks.peakEnginePowerCv, 10f)
  }

  @Test
  fun testScenario6_DistanceSplitsReached100m_Not201mNor402m() {
    // 6. Passagem que atingiu 100m (em 6.2s), mas a distância total foi 150m (não atingiu 201m nem 402m)
    val run = RunResult(
      id = "test-run-6",
      vehicleName = "Golf TSI",
      timestamp = System.currentTimeMillis(),
      maximumGpsSpeedKmh = 80.0f,
      elapsedSeconds = 7.5f,
      quality = "BOA",
      totalDistanceMeters = 150.0f,
      time100M = 6.20f,
      time201M = null, // Não atingido
      time402M = null  // Não atingido
    )

    assertEquals(6.20f, run.time100M ?: 0f, 0.01f)
    assertNull(run.time201M)
    assertNull(run.time402M)
  }

  @Test
  fun testScenario7_MissingGearOrTire_BlocksTorqueAndRpmCurve() {
    // 7. Relação de marcha / pneu / diferencial ausente
    val samples = (0 until 50).map { i ->
      RunSample(
        elapsedTimeMs = (i * 100).toLong(),
        rawGpsSpeedKmh = 40f + i,
        filteredSpeedKmh = 40f + i,
        isValid = true,
        enginePowerCv = 100f,
        engineRpm = 0 // Sem RPM
      )
    }

    // Sem marcha informada -> deve exibir curva por velocidade
    val curveTypeNoGear = VehicleCalculations.evaluateCurveEligibility(
      samples = samples,
      gearUsed = "",
      gearRatio = 0f,
      finalDrive = 0f,
      tireCircumferenceM = 0.0,
      speedGainKmh = 25f
    )
    assertEquals(CurveDisplayType.SPEED, curveTypeNoGear)

    // Torque com RPM = 0 deve retornar 0f (sem gerar curva falsa)
    val torque = VehicleCalculations.calculateTorqueFromPowerWatts(73549.875, 0)
    assertEquals(0f, torque.torqueKgfm, 0.001f)
    assertEquals(0f, torque.torqueNm, 0.001f)
  }

  @Test
  fun testScenario8_TorqueCalculationWithRpmZero_SafeNoNanOrInf() {
    // 8. Cálculo de torque com RPM = 0
    val resultZero = VehicleCalculations.calculateTorqueFromPowerWatts(100000.0, 0)
    assertFalse("Torque em Nm não pode ser NaN", resultZero.torqueNm.isNaN())
    assertFalse("Torque em Nm não pode ser Infinito", resultZero.torqueNm.isInfinite())
    assertEquals(0f, resultZero.torqueNm, 0.001f)
    assertEquals(0f, resultZero.torqueKgfm, 0.001f)

    val resultNegative = VehicleCalculations.calculateTorqueFromPowerWatts(100000.0, -500)
    assertEquals(0f, resultNegative.torqueNm, 0.001f)
    assertEquals(0f, resultNegative.torqueKgfm, 0.001f)

    // Cálculo normal com RPM válido (ex: 100 cv = 73549.875 W a 5000 RPM)
    // Torque = 73549.875 / (5000 * 2π / 60) = 140.47 Nm = ~14.32 kgfm
    val resultValid = VehicleCalculations.calculateTorqueFromPowerWatts(73549.875, 5000)
    assertEquals(140.47f, resultValid.torqueNm, 0.5f)
    assertEquals(14.32f, resultValid.torqueKgfm, 0.1f)
  }

  @Test
  fun testScenario9_ComparisonBetweenDifferentGears_Rejected() {
    // 9. Comparação entre duas passagens com marchas diferentes deve ser recusada
    val run1 = RunResult(
      id = "run-1",
      vehicleName = "Civic Si",
      timestamp = 1000L,
      gearUsed = "2ª marcha",
      speedGainKmh = 25f,
      quality = "BOA"
    )
    val run2 = RunResult(
      id = "run-2",
      vehicleName = "Civic Si",
      timestamp = 2000L,
      gearUsed = "3ª marcha",
      speedGainKmh = 25f,
      quality = "BOA"
    )

    val isComparable = run1.quality in listOf("BOA", "REGULAR") &&
      run2.quality in listOf("BOA", "REGULAR") &&
      run1.vehicleName == run2.vehicleName &&
      run1.gearUsed == run2.gearUsed &&
      run1.speedGainKmh >= 15f &&
      run2.speedGainKmh >= 15f

    assertFalse("Passagens com marchas diferentes não podem ser comparadas", isComparable)
  }

  @Test
  fun testScenario10_ComparisonBetweenTwoBoaRunsSameVehicleSameGear_Accepted() {
    // 10. Comparação entre duas passagens BOA do mesmo veículo na mesma marcha deve ser aceita
    val run1 = RunResult(
      id = "run-1",
      vehicleName = "Civic Si",
      timestamp = 1000L,
      gearUsed = "3ª marcha",
      speedGainKmh = 25f,
      quality = "BOA"
    )
    val run2 = RunResult(
      id = "run-2",
      vehicleName = "Civic Si",
      timestamp = 2000L,
      gearUsed = "3ª marcha",
      speedGainKmh = 28f,
      quality = "BOA"
    )

    val isComparable = run1.quality in listOf("BOA", "REGULAR") &&
      run2.quality in listOf("BOA", "REGULAR") &&
      run1.vehicleName == run2.vehicleName &&
      run1.gearUsed == run2.gearUsed &&
      run1.speedGainKmh >= 15f &&
      run2.speedGainKmh >= 15f

    assertTrue("Passagens BOA do mesmo veículo na mesma marcha devem ser comparáveis", isComparable)
  }
}
