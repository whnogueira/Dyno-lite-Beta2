package com.example

import com.example.model.*
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
      shiftTimeSeconds = 0.30f
    )

    val result = SimulationEngine.runSimulation(config)

    assertNotNull("0-100 km/h should be calculated", result.time0to100Kmh)
    assertTrue("0-100 time should be realistic (between 6s and 12s)", result.time0to100Kmh!! in 6.0f..12.0f)
    assertNotNull("402m should be calculated", result.time402m)
    assertTrue("402m time should be realistic (between 14s and 19s)", result.time402m!! in 14.0f..19.0f)
    assertTrue("Final speed should be above 130 km/h", (result.speedAt402mKmh ?: 0f) > 130f)
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
  fun testGarageTuningEngineCalculations() {
    val defaultBuild = GarageTuningEngine.createDefaultVectraBuild()
    val stockResult = GarageTuningEngine.calculateTuningBuild(defaultBuild)

    // Stock Vectra 2.2 8V with Ethanol tuning is around 130 cv
    assertTrue("Stock power should be around 120-140 cv", stockResult.estimatedEnginePowerCv in 115f..145f)
    assertTrue("Stock torque should be around 18-22 kgfm", stockResult.estimatedEngineTorqueKgfm in 18f..22f)
    assertTrue("Stock reliability should be high", stockResult.reliabilityScore >= 85)

    // Apply Turbo 0.8 bar Template
    val turboBuild = GarageTuningEngine.applyProjectTemplate(ProjectTemplateType.TURBO_INTERMEDIARIO, defaultBuild)
    val turboResult = GarageTuningEngine.calculateTuningBuild(turboBuild)

    assertTrue("Turbo power should be significantly higher (>200 cv)", turboResult.estimatedEnginePowerCv > 200f)
    assertTrue("Turbo torque should be higher (>30 kgfm)", turboResult.estimatedEngineTorqueKgfm > 30f)
    assertTrue("Boost pressure should be around 0.7-0.8 bar", turboResult.actualBoostBar >= 0.70f)

    // Test Dyno Track simulation
    val trackResult = GarageTuningEngine.runDynoTrackSimulation(turboResult)
    assertTrue("0-100 time should be faster with turbo (< 7.5s)", trackResult.time0to100Kmh < 7.5f)
    assertTrue("402m speed should be > 150 km/h", trackResult.speedAt402mKmh > 150f)
  }

  @Test
  fun testInjectorBottleneckFormula() {
    // Formula: novaVazao = vazaoOriginal * sqrt(novaPressao / pressaoOriginal)
    val baseFlow = 28f // lb/h
    val corrected = GarageTuningEngine.calculateCorrectedInjectorFlow(
      nominalFlowLbHr = baseFlow,
      nominalPressureBar = 3.0f,
      operatingPressureBar = 4.0f
    )
    val expected = (baseFlow * kotlin.math.sqrt(4.0f / 3.0f))
    assertEquals(expected, corrected, 0.1f)

    // Supported HP calculation
    val supportedHp = GarageTuningEngine.calculateMaxSupportedPowerByInjectorsCv(
      correctedFlowLbHr = corrected,
      injectorCount = 4,
      maxDutyCyclePercent = 85.0f,
      bsfc = 0.55f
    )
    assertTrue("Supported HP should be positive and realistic", supportedHp > 150f)
  }
}
