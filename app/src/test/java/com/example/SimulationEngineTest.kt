package com.example

import com.example.model.DrivetrainType
import com.example.model.FinishReason
import com.example.model.RunResult
import com.example.model.RunSample
import com.example.model.ShiftSpeedType
import com.example.model.SimulationConfig
import com.example.model.SimulationEngine
import com.example.model.TireGripType
import com.example.model.VehicleCalculations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulationEngineTest {

  @Test
  fun testTireDimensionsCalculation() {
    val tire = VehicleCalculations.calculateTireDimensions(
      widthMm = 195,
      aspectRatio = 55,
      rimInches = 15
    )
    // 195 * 0.55 = 107.25 mm
    // rim = 15 * 25.4 = 381.0 mm
    // total diameter = 381 + 2 * 107.25 = 595.5 mm
    assertEquals(107.25, tire.lateralHeightMm, 0.1)
    assertEquals(381.0, tire.rimDiameterMm, 0.1)
    assertEquals(595.5, tire.totalDiameterMm, 0.5)
    assertTrue("Circumference should be around 1.87m", tire.circumferenceM in 1.80..1.95)
  }

  @Test
  fun testSimulationEngineBasicAcceleration() {
    val config = SimulationConfig(
      label = "Teste Simulado",
      vehicleName = "Carro Teste 150cv",
      vehicleCurbWeightKg = 1100f,
      driverWeightKg = 80f,
      additionalWeightKg = 0f,
      enginePowerCv = 150f,
      engineTorqueKgfm = 21.0f,
      peakPowerRpm = 5800,
      peakTorqueRpm = 3800,
      maxRpm = 6500,
      gearRatios = listOf(3.73f, 2.05f, 1.36f, 1.03f, 0.82f),
      finalDriveRatio = 4.10f,
      drivetrainLossPercent = 12f,
      drivetrainType = DrivetrainType.FWD,
      tireWidthMm = 195,
      tireAspectRatio = 55,
      rimDiameterInches = 15,
      shiftSpeed = ShiftSpeedType.FAST
    )

    val result = SimulationEngine.runSimulation(config)

    assertNotNull("0-100 km/h should be calculated", result.time0to100Kmh)
    assertTrue("0-100 time should be realistic (between 6s and 12s)", result.time0to100Kmh!! in 6.0f..12.0f)
    assertNotNull("402m should be calculated", result.time402m)
    assertTrue("402m time should be realistic (between 14s and 19s)", result.time402m!! in 14.0f..19.0f)
    assertTrue("Final speed should be above 130 km/h", (result.speedAt402mKmh ?: 0f) > 130f)
    assertTrue("Samples should not be empty", result.samples.isNotEmpty())
    assertTrue("Gear speed table should have 5 gears", result.gearSpeeds.size == 5)
  }

  @Test
  fun testSimulationWeightReductionEffect() {
    val baseConfig = SimulationConfig(
      vehicleCurbWeightKg = 1300f,
      driverWeightKg = 80f,
      enginePowerCv = 140f,
      engineTorqueKgfm = 18f
    )
    val lightConfig = baseConfig.copy(
      vehicleCurbWeightKg = 1100f // -200 kg
    )

    val resultBase = SimulationEngine.runSimulation(baseConfig)
    val resultLight = SimulationEngine.runSimulation(lightConfig)

    assertNotNull(resultBase.time0to100Kmh)
    assertNotNull(resultLight.time0to100Kmh)
    assertTrue(
      "Lighter car must accelerate faster 0-100",
      resultLight.time0to100Kmh!! < resultBase.time0to100Kmh!!
    )
    assertTrue(
      "Lighter car must finish 402m faster",
      (resultLight.time402m ?: 99f) < (resultBase.time402m ?: 99f)
    )
  }

  @Test
  fun testSimulationPowerIncreaseEffect() {
    val baseConfig = SimulationConfig(
      enginePowerCv = 120f,
      engineTorqueKgfm = 16f
    )
    val tunedConfig = baseConfig.copy(
      enginePowerCv = 180f, // +60 cv
      engineTorqueKgfm = 24f
    )

    val resultBase = SimulationEngine.runSimulation(baseConfig)
    val resultTuned = SimulationEngine.runSimulation(tunedConfig)

    assertNotNull(resultBase.time0to100Kmh)
    assertNotNull(resultTuned.time0to100Kmh)
    assertTrue(
      "Higher power car must accelerate faster 0-100",
      resultTuned.time0to100Kmh!! < resultBase.time0to100Kmh!!
    )
  }

  @Test
  fun testRecalculateRunResult() {
    val sample1 = RunSample(
      timestampMillis = 1000L,
      gpsSpeedKmh = 40.0f,
      gpsLatitude = 0.0,
      gpsLongitude = 0.0,
      gpsAccuracyMeters = 2.0f,
      filteredAccelG = 0.45f,
      wheelPowerCv = 80f,
      enginePowerCv = 95f,
      wheelTorqueKgfm = 18f,
      engineTorqueKgfm = 21f,
      estimatedRpm = 3200
    )
    val sample2 = RunSample(
      timestampMillis = 1500L,
      gpsSpeedKmh = 60.0f,
      gpsLatitude = 0.0,
      gpsLongitude = 0.0,
      gpsAccuracyMeters = 2.0f,
      filteredAccelG = 0.40f,
      wheelPowerCv = 90f,
      enginePowerCv = 105f,
      wheelTorqueKgfm = 17f,
      engineTorqueKgfm = 20f,
      estimatedRpm = 4800
    )

    val originalRun = RunResult(
      id = "run-test-1",
      vehicleId = "veh-1",
      vehicleName = "Carro Teste",
      maxWheelPowerCv = 90f,
      maxEnginePowerCv = 105f,
      maxWheelTorqueKgfm = 18f,
      maxEngineTorqueKgfm = 21f,
      peakPowerRpm = 4800,
      peakTorqueRpm = 3200,
      samples = listOf(sample1, sample2),
      finishReason = FinishReason.NORMAL_COMPLETION,
      gearRatioUsed = 1.95f,
      finalDriveUsed = 4.10f,
      totalVehicleMassKg = 1200f,
      drivetrainLossPercent = 12f
    )

    // Recalculate with 1400kg (+200kg mass)
    val recalculated = VehicleCalculations.recalculateRunResult(
      run = originalRun,
      correctedTotalMassKg = 1400f,
      correctedGearRatio = 1.95f,
      correctedFinalDrive = 4.10f,
      correctedTireWidthMm = 195,
      correctedTireAspectRatio = 55,
      correctedRimInches = 15,
      correctedLossPercent = 12f,
      correctedCd = 0.33f,
      correctedFrontalAreaM2 = 2.10f,
      correctedCrr = 0.015f
    )

    assertTrue(
      "Recalculated power with more mass must be higher than original",
      recalculated.maxEnginePowerCv > originalRun.maxEnginePowerCv
    )
    assertEquals(1400f, recalculated.totalVehicleMassKg, 0.1f)
  }
}
