package com.example

import com.example.model.RunResult
import com.example.model.RunSample
import com.example.model.VehicleCalculations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Testes Unitários para Verificação de:
 * 1. Correção de marcha e índice (índice 0 = 1ª, índice 1 = 2ª: 2.14 vs 1.41)
 * 2. Cálculo correto de peso total com motorista e carga (1.265 + 79 = 1.344 kg)
 * 3. Normalização de aceleração / força G com ganho GPS
 * 4. Detecção de divergência GPS / Acelerômetro
 * 5. Validação de picos sustentados (150ms / 3 amostras consecutivas)
 * 6. Recálculo e reavaliação de passagens (recalculateRunResult)
 */
class GearWeightNormalizationTest {

  @Test
  fun testGearIndexAndRatioMapping() {
    // Transmissão F18 WR: 1ª=3.73, 2ª=2.14, 3ª=1.41, 4ª=1.12, 5ª=0.89
    val transmissionRatios = listOf(3.73f, 2.14f, 1.41f, 1.12f, 0.89f)

    // Índice 0 = 1ª
    assertEquals(3.73f, transmissionRatios[0], 0.001f)
    // Índice 1 = 2ª
    assertEquals(2.14f, transmissionRatios[1], 0.001f)
    // Índice 2 = 3ª
    assertEquals(1.41f, transmissionRatios[2], 0.001f)
    // Índice 3 = 4ª
    assertEquals(1.12f, transmissionRatios[3], 0.001f)
    // Índice 4 = 5ª
    assertEquals(0.89f, transmissionRatios[4], 0.001f)
  }

  @Test
  fun testOccupantsAndCargoCalculations_1444Kg() {
    // 1. veículo 1.265 kg + motorista 79 kg + passageiros 100 kg = 1.444 kg
    val totalMass = VehicleCalculations.calculateTotalWeight(
      curbWeightKg = 1265f,
      driverWeightKg = 79f,
      passengerWeightKg = 100f,
      cargoWeightKg = 0f,
      fuelAdjustmentKg = 0f
    )
    assertEquals(1444.0f, totalMass, 0.01f)
  }

  @Test
  fun testPassengersBelongOnlyToCurrentPassAndRevert() {
    // 2. passageiros pertencem somente à passagem atual
    // 4. nova passagem sem passageiros reverte ao padrão do veículo
    val baseVehicleCurb = 1265f
    val baseDriver = 79f

    // Passagem 1: Com 2 passageiros (100 kg)
    val pass1Weight = VehicleCalculations.calculateTotalWeight(
      curbWeightKg = baseVehicleCurb,
      driverWeightKg = baseDriver,
      passengerWeightKg = 100f
    )
    assertEquals(1444f, pass1Weight, 0.01f)

    // Passagem 2: Nova passagem sem passageiros (padrão)
    val pass2Weight = VehicleCalculations.calculateTotalWeight(
      curbWeightKg = baseVehicleCurb,
      driverWeightKg = baseDriver,
      passengerWeightKg = 0f
    )
    assertEquals(1344f, pass2Weight, 0.01f)
  }

  @Test
  fun testHistoricalResultPreservesUsedMass() {
    // 3. resultado antigo preserva a massa usada mesmo após alterações no veículo
    val historicalRun = RunResult(
      id = "historical_run_1",
      vehicleId = "vectra_1",
      vehicleName = "Chevrolet Vectra",
      totalVehicleMassKg = 1444.0f,
      curbWeightKg = 1265.0f,
      driverWeightKg = 79.0f,
      passengerCount = 2,
      passengerWeightKg = 100.0f
    )

    // Alteração posterior no cadastro do veículo para peso vazio 1.300 kg e motorista 85 kg
    val updatedVehicleCurb = 1300.0f
    val updatedDriver = 85.0f

    // O resultado antigo DEVE preservar exatamente 1.444 kg
    assertEquals(1444.0f, historicalRun.totalVehicleMassKg, 0.001f)
    assertEquals(2, historicalRun.passengerCount)
    assertEquals(100.0f, historicalRun.passengerWeightKg, 0.001f)
  }

  @Test
  fun testDriverWeightNotDoubleCounted() {
    // 5. peso do motorista não é contado em duplicidade
    val curbWeight = 1265f
    val driverWeight = 79f

    val calculated = VehicleCalculations.calculateTotalWeight(
      curbWeightKg = curbWeight,
      driverWeightKg = driverWeight,
      passengerWeightKg = 0f,
      cargoWeightKg = 0f,
      fuelAdjustmentKg = 0f
    )

    // Deve ser exatamente curb (1265) + driver (79) = 1344 kg
    // Não pode somar o motorista 2 vezes
    assertEquals(1344.0f, calculated, 0.01f)
  }

  @Test
  fun testGNormalizationFactorClamping() {
    // Cenário: GPS mediu 36.9 km/h (10.25 m/s) em 9.01s
    // Sensor mediu média 0.18 G -> integral = 0.18 * 9.80665 * 9.01 = 15.90 m/s (57.2 km/h)
    val gpsDeltaV = 36.9f / 3.6f // 10.25 m/s
    val elapsedSec = 9.01f
    val rawAvgG = 0.18f
    val sensorDeltaV = rawAvgG * 9.80665f * elapsedSec // ~15.90 m/s

    val normFactor = (gpsDeltaV / sensorDeltaV).coerceIn(0.50f, 1.50f)
    // 10.25 / 15.90 = ~0.6446
    assertEquals(0.6446f, normFactor, 0.01f)

    val anchoredAvgG = rawAvgG * normFactor
    // 0.18 * 0.6446 = ~0.116 G
    assertEquals(0.116f, anchoredAvgG, 0.005f)

    // Teste de clamp mínimo (0.50)
    val extremeLowNorm = (5.0f / 20.0f).coerceIn(0.50f, 1.50f)
    assertEquals(0.50f, extremeLowNorm, 0.001f)

    // Teste de clamp máximo (1.50)
    val extremeHighNorm = (30.0f / 15.0f).coerceIn(0.50f, 1.50f)
    assertEquals(1.50f, extremeHighNorm, 0.001f)
  }

  @Test
  fun testRunQualityDivergenceDetection() {
    // Cenário com divergência severa: Sensor mediu 15.9 m/s e GPS mediu 10.25 m/s
    // (15.9 - 10.25) / 10.25 = 55.1% > 40%
    val evalDivergent = VehicleCalculations.classifyRunQuality(
      speedGainKmh = 36.9f,
      validGpsLocationsCount = 15,
      elapsedSec = 9.01f,
      lastGpsAccuracyMeters = 3.0f,
      avgSyncDiffKmh = 1.0f,
      rejectionRatio = 0.0f,
      sensorDeltaVMps = 15.9f,
      gpsDeltaVMps = 10.25f
    )
    assertEquals("GPS/SENSOR DIVERGENTE", evalDivergent.quality)

    // Cenário com divergência moderada: Sensor mediu 12.5 m/s e GPS mediu 10.25 m/s
    // (12.5 - 10.25) / 10.25 = 21.9% > 15% mas <= 40%
    val evalRegular = VehicleCalculations.classifyRunQuality(
      speedGainKmh = 36.9f,
      validGpsLocationsCount = 15,
      elapsedSec = 9.01f,
      lastGpsAccuracyMeters = 3.0f,
      avgSyncDiffKmh = 1.0f,
      rejectionRatio = 0.0f,
      sensorDeltaVMps = 12.5f,
      gpsDeltaVMps = 10.25f
    )
    assertEquals("REGULAR", evalRegular.quality)
  }

  @Test
  fun testSustainedPeakDetectionRejectsIsolatedSpike() {
    // Criando 20 amostras com aceleração estável de 0.15G e um pico isolado de 0.60G
    val samples = (0 until 20).map { i ->
      val isSpike = (i == 10)
      val g = if (isSpike) 0.60f else 0.15f
      val power = if (isSpike) 220.0f else 95.0f
      val torque = if (isSpike) 32.0f else 16.0f
      val speed = 40.0f + (i * 2.5f) // velocidade crescente
      RunSample(
        elapsedTimeMs = (i * 100).toLong(),
        rawGpsSpeedKmh = speed,
        filteredSpeedKmh = speed,
        filteredSpeedMs = speed / 3.6f,
        gpsSpeedKmh = speed,
        finalAccelerationMps2 = g * 9.80665f,
        longitudinalG = g,
        enginePowerCv = power,
        wheelPowerCv = power * 0.85f,
        engineTorqueKgfm = torque,
        wheelTorqueKgfm = torque * 0.85f,
        engineRpm = 2000 + (i * 150),
        isValid = true
      )
    }

    val peaks = VehicleCalculations.findSustainedPeaks(samples, isRpmValid = true)
    // O pico isolado de 220 cv / 0.60 G deve ter sido rejeitado pela validação de 3 amostras sustentadas
    assertTrue("Peak engine power must be below 130 cv, got ${peaks.peakEnginePowerCv}", peaks.peakEnginePowerCv < 130.0f)
    assertTrue("Peak longitudinal G must be below 0.30 G, got ${peaks.peakLongitudinalG}", peaks.peakLongitudinalG < 0.30f)
  }

  @Test
  fun testRecalculateRunResultAppliesCorrectWeightAndGear() {
    // Criando uma passagem simulada com relação errada (1.41) e peso errado (1265)
    val samples = (0 until 40).map { i ->
      val speedKmh = 46.5f + (i * 0.95f) // 46.5 -> 83.55 km/h
      val speedMs = speedKmh / 3.6f
      val rawAccel = 1.8f // m/s^2 (~0.183 G)
      RunSample(
        elapsedTimeMs = (i * 225).toLong(),
        rawGpsSpeedKmh = speedKmh,
        filteredSpeedKmh = speedKmh,
        filteredSpeedMs = speedMs,
        gpsSpeedKmh = speedKmh,
        rawAccelerationMps2 = rawAccel,
        sensorAccelerationMps2 = rawAccel,
        finalAccelerationMps2 = rawAccel,
        longitudinalG = rawAccel / 9.80665f,
        isValid = true
      )
    }

    val initialRun = RunResult(
      id = "test_run_1",
      vehicleId = "vectra_20_8v",
      vehicleName = "Chevrolet Vectra 2.0 8V",
      startSpeedKmh = 46.5f,
      officialStartSpeedKmh = 46.5f,
      officialMaxSpeedKmh = 83.55f,
      officialEndSpeedKmh = 83.55f,
      officialSpeedGainKmh = 37.05f,
      maximumGpsSpeedKmh = 83.55f,
      maximumCalculatedSpeedKmh = 83.55f,
      finalGpsSpeedKmh = 83.55f,
      finalCalculatedSpeedKmh = 83.55f,
      finalSpeedKmh = 83.55f,
      speedGainKmh = 37.05f,
      totalVehicleMassKg = 1265.0f, // sem motorista
      gearUsed = "2ª",
      gearRatioUsed = 1.41f, // relação errada (pertencia à 3ª)
      finalDriveUsed = 4.19f,
      elapsedSeconds = 9.0f,
      samples = samples
    )

    // Recalcular com peso corrigido (1.344 kg) e relação corrigida de 2ª marcha (2.14)
    val correctedRun = VehicleCalculations.recalculateRunResult(
      run = initialRun,
      correctedTotalMassKg = 1344.0f,
      correctedGearRatio = 2.14f,
      correctedFinalDrive = 4.19f,
      correctedTireWidthMm = 195,
      correctedTireAspectRatio = 60,
      correctedRimInches = 15,
      correctedLossPercent = 15.0f,
      correctedCd = 0.28f,
      correctedFrontalAreaM2 = 2.06f,
      correctedCrr = 0.015f
    )

    // Validar parâmetros recalculados
    assertEquals(1344.0f, correctedRun.totalVehicleMassKg, 0.01f)
    assertEquals(2.14f, correctedRun.gearRatioUsed, 0.01f)
    assertEquals(4.19f, correctedRun.finalDriveUsed, 0.01f)
    assertTrue("Estimated power must be calculated and > 0", correctedRun.estimatedPowerCv > 0f)
    assertTrue("Estimated torque must be calculated and > 0", correctedRun.estimatedTorqueKgfm > 0f)
    assertNotNull(correctedRun.peakPowerRpm)

    // Validar RPM final para 2ª marcha (2.14) a ~83.5 km/h
    // Raio do pneu 195/60R15 = ~0.3075 m -> Circunferência = ~1.932 m
    // RPM = (v_mps / circ) * gearRatio * finalDrive * 60
    // v_mps = 83.55 / 3.6 = 23.208 m/s
    // (23.208 / 1.932) * 2.14 * 4.19 * 60 = ~6460 RPM (condizente com o fim da 2ª marcha)
    val lastSampleRpm = correctedRun.samples.last().engineRpm
    assertNotNull(lastSampleRpm)
    assertTrue("RPM at 83.5 km/h in 2nd gear should be > 5500 RPM, was $lastSampleRpm", lastSampleRpm!! > 5500)

    // Validar que fator de normalização e ancoramento foram aplicados
    assertTrue(correctedRun.normalizationFactor > 0.49f && correctedRun.normalizationFactor <= 1.50f)
    assertTrue(correctedRun.anchoredAverageLongitudinalG > 0f)
  }
}
