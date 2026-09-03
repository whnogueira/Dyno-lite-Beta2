package com.example.model

import org.json.JSONObject
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.atan

/**
 * Configuração de parâmetros editáveis da passagem.
 * Permite corrigir apenas as propriedades físicas do veículo e do ambiente,
 * mantendo as leituras brutas de GPS, sensores e timestamps estritamente intocadas.
 */
data class DynoCorrectionConfig(
  val curbWeightKg: Float = 1265f,
  val driverWeightKg: Float = 79f,
  val passengerCount: Int = 0,
  val passengerWeightMode: PassengerWeightMode = PassengerWeightMode.INDIVIDUAL,
  val passengerIndividualWeightKg: Float = 70f,
  val passengerTotalWeightKg: Float = 0f,
  val cargoWeightKg: Float = 0f,
  val soundSystemWeightKg: Float = 0f,
  val cngWeightKg: Float = 0f,
  val fuelWeightKg: Float = 0f,
  val gearUsed: String = "2ª",
  val gearIndexUsed: Int = 1,
  val gearRatio: Float = 1.95f,
  val finalDriveRatio: Float = 4.10f,
  val tireWidthMm: Int = 195,
  val tireAspectRatio: Int = 55,
  val rimInches: Int = 15,
  val drivetrainLossPercent: Float = 12f,
  val crr: Float = 0.015f,
  val cd: Float = 0.34f,
  val frontalAreaM2: Float = 2.10f,
  val airDensityKgM3: Float = 1.225f,
  val slopeMode: String = "IGNORE",
  val slopePercent: Float = 0.0f,
  val note: String? = null
) {
  enum class PassengerWeightMode {
    INDIVIDUAL,
    TOTAL
  }

  val effectivePassengerWeightKg: Float
    get() = when (passengerWeightMode) {
      PassengerWeightMode.INDIVIDUAL -> (passengerCount * passengerIndividualWeightKg).coerceAtLeast(0f)
      PassengerWeightMode.TOTAL -> passengerTotalWeightKg.coerceAtLeast(0f)
    }

  val totalMassKg: Float
    get() = (curbWeightKg + driverWeightKg + effectivePassengerWeightKg +
      cargoWeightKg + soundSystemWeightKg + cngWeightKg + fuelWeightKg).coerceAtLeast(0f)

  val totalVehicleMassKg: Float
    get() = totalMassKg
}

/**
 * Resultado detalhado do recálculo ponto a ponto.
 */
data class DynoRecalculationResult(
  val recalculatedRun: RunResult,
  val recalculatedSamples: List<RunSample>,
  val peakWheelPowerCv: Float,
  val peakEnginePowerCv: Float,
  val peakTorqueNm: Float,
  val peakTorqueKgfm: Float,
  val peakEngineTorqueKgfm: Float = peakTorqueKgfm,
  val peakWheelTorqueKgfm: Float = 0f,
  val peakPowerRpm: Int?,
  val peakTorqueRpm: Int?,
  val peakPowerSpeedKmh: Float,
  val peakTorqueSpeedKmh: Float,
  val initialRpm: Int?,
  val finalRpm: Int?,
  val rpmRange: Pair<Int, Int>?,
  val totalMassKg: Float,
  val calculationWarnings: List<String> = emptyList()
)

/**
 * Motor puro e reutilizável de cálculo e recálculo de dinamômetro (Seção 11).
 * Centraliza a física: F = m*a, F_roll = Crr*m*g, F_aero = 0.5*rho*Cd*A*v^2,
 * P = F*v, P_engine = P_wheel / (1 - lossPercent), e derivação de RPM / Torque.
 */
object DynoRecalculationEngine {

  const val STANDARD_GRAVITY = 9.80665f
  const val WATTS_PER_CV = 735.49875f

  /**
   * Recalcula a passagem ponto a ponto utilizando estritamente as amostras originais.
   */
  fun recalculate(
    originalSamples: List<RunSample>,
    correctedConfig: DynoCorrectionConfig,
    originalRun: RunResult
  ): DynoRecalculationResult {
    val warnings = mutableListOf<String>()

    val correctedMassKg = correctedConfig.totalMassKg
    if (correctedMassKg <= 0f) {
      warnings.add("Massa total corrigida deve ser maior que zero.")
    }

    // 1. Eficiência da transmissão: drivetrainEfficiency = 1 - lossPercent
    // Não aplicar: wheelPower * (1 + lossPercent). Usar divisão pela eficiência.
    val lossFraction = (correctedConfig.drivetrainLossPercent / 100f).coerceIn(0f, 0.40f)
    val drivetrainEfficiency = (1.0f - lossFraction).coerceIn(0.60f, 1.0f)

    // 2. Dimensões dos pneus
    val tireWidth = correctedConfig.tireWidthMm.coerceIn(125, 385)
    val tireAspect = correctedConfig.tireAspectRatio.coerceIn(20, 90)
    val rimInches = correctedConfig.rimInches.coerceIn(10, 26)

    val tireLateralHeightM = (tireWidth * (tireAspect / 100.0)) / 1000.0
    val rimDiameterM = (rimInches * 25.4) / 1000.0
    val tireDiameterM = rimDiameterM + (2.0 * tireLateralHeightM)
    val tireCircumferenceM = PI * tireDiameterM

    // 3. Força de rolamento constante: Crr * m * g
    val crr = correctedConfig.crr.coerceIn(0.005f, 0.050f)
    val rollingResistanceForceN = (crr * correctedMassKg * STANDARD_GRAVITY).coerceAtLeast(0f)

    // 4. Força de inclinação (se aplicável)
    val slopeForceN = if (correctedConfig.slopeMode != "IGNORE" && correctedConfig.slopePercent != 0f) {
      val angleRad = atan(correctedConfig.slopePercent / 100.0)
      (correctedMassKg * STANDARD_GRAVITY * sin(angleRad)).toFloat()
    } else 0f

    // 5. Constantes aerodinâmicas
    val airDensity = correctedConfig.airDensityKgM3.coerceIn(0.9f, 1.5f)
    val cd = correctedConfig.cd.coerceIn(0.15f, 1.0f)
    val frontalArea = correctedConfig.frontalAreaM2.coerceIn(1.0f, 5.0f)
    val aeroConstant = 0.5f * airDensity * cd * frontalArea

    val gearRatio = correctedConfig.gearRatio.coerceAtLeast(0.1f)
    val finalDriveRatio = correctedConfig.finalDriveRatio.coerceAtLeast(0.1f)

    // Recálculo ponto a ponto preservando exatamente as amostras originais
    val recalculatedSamples = originalSamples.map { sample ->
      // Velocidade original GPS (não fabricar nem alterar)
      val originalGpsSpeedMps = when {
        sample.filteredSpeedMs > 0.001f -> sample.filteredSpeedMs
        sample.rawGpsSpeedMs > 0.001f -> sample.rawGpsSpeedMs
        sample.rawGpsSpeedKmh > 0.001f -> sample.rawGpsSpeedKmh / 3.6f
        sample.filteredSpeedKmh > 0.001f -> sample.filteredSpeedKmh / 3.6f
        else -> (sample.gpsSpeedKmh.takeIf { it > 0f } ?: 0f) / 3.6f
      }.coerceAtLeast(0f)

      // Aceleração original corrigida pelos sensores/GPS (não fabricar nem alterar)
      val originalCorrectedAccelerationMps2 = when {
        sample.correctedAccelerationZ != 0f -> sample.correctedAccelerationZ
        sample.anchoredAccelerationMps2 > 0.001f -> sample.anchoredAccelerationMps2
        sample.finalAccelerationMps2 != 0f -> sample.finalAccelerationMps2
        sample.rawAccelerationMps2 != 0f -> sample.rawAccelerationMps2
        sample.longitudinalG != 0f -> sample.longitudinalG * STANDARD_GRAVITY
        else -> 0f
      }

      // accelerationForceN = correctedMassKg * originalCorrectedAccelerationMps2
      val accelerationForceN = (correctedMassKg * max(0f, originalCorrectedAccelerationMps2))

      // aeroForceN = 0.5 * airDensity * Cd * frontalAreaM2 * originalGpsSpeedMps²
      val aeroForceN = (aeroConstant * originalGpsSpeedMps * originalGpsSpeedMps)

      // tractiveForceN = accelerationForceN + rollingResistanceForceN + aeroForceN + slopeForceN
      val tractiveForceN = (accelerationForceN + rollingResistanceForceN + aeroForceN + slopeForceN).coerceAtLeast(0f)

      // wheelPowerWatts = tractiveForceN * originalGpsSpeedMps
      val wheelPowerWatts = (tractiveForceN * originalGpsSpeedMps).coerceAtLeast(0f)

      // wheelPowerCv = wheelPowerWatts / 735.49875
      val wheelPowerCv = if (wheelPowerWatts.isFinite()) (wheelPowerWatts / WATTS_PER_CV).coerceAtLeast(0f) else 0f

      // enginePowerCv = wheelPowerCv / drivetrainEfficiency
      val enginePowerCv = if (wheelPowerCv.isFinite() && drivetrainEfficiency > 0f) {
        (wheelPowerCv / drivetrainEfficiency).coerceAtLeast(0f)
      } else wheelPowerCv

      // Recálculo de RPM:
      // wheelRpm = originalGpsSpeedMps / tireCircumferenceM * 60
      // engineRpm = wheelRpm * gearRatio * finalDriveRatio
      val sampleEngineRpm: Int? = if (tireCircumferenceM > 0.0 && gearRatio > 0f && finalDriveRatio > 0f && originalGpsSpeedMps > 0.5f) {
        val wheelRpm = (originalGpsSpeedMps / tireCircumferenceM) * 60.0
        val rawRpm = wheelRpm * gearRatio * finalDriveRatio
        if (rawRpm.isFinite() && rawRpm in 400.0..14000.0) rawRpm.toInt() else null
      } else null

      // Torque:
      // angularVelocityRadS = engineRpm * 2 * PI / 60
      // torqueNm = powerWatts / angularVelocityRadS
      // torqueKgfm = torqueNm / 9.80665
      var sampleEngineTorqueNm = 0f
      var sampleEngineTorqueKgfm = 0f
      var sampleWheelTorqueNm = 0f
      var sampleWheelTorqueKgfm = 0f

      if (sampleEngineRpm != null && sampleEngineRpm > 100) {
        val angularVelocityRadS = sampleEngineRpm * (2.0 * PI / 60.0)
        val enginePowerWatts = enginePowerCv * WATTS_PER_CV
        if (angularVelocityRadS > 0.001 && enginePowerWatts > 0f) {
          val calcTorqueNm = (enginePowerWatts / angularVelocityRadS).toFloat()
          if (calcTorqueNm.isFinite() && calcTorqueNm in 0.1f..3000f) {
            sampleEngineTorqueNm = calcTorqueNm
            sampleEngineTorqueKgfm = calcTorqueNm / STANDARD_GRAVITY
            sampleWheelTorqueNm = calcTorqueNm * drivetrainEfficiency
            sampleWheelTorqueKgfm = sampleEngineTorqueKgfm * drivetrainEfficiency
          }
        }
      }

      // Atualiza valores derivados mantendo rigorosamente inalterados os dados do GPS e sensores
      sample.copy(
        accelerationForceN = accelerationForceN,
        rollingForceN = rollingResistanceForceN,
        aerodynamicForceN = aeroForceN,
        slopeForceN = slopeForceN,
        totalForceN = tractiveForceN,
        wheelPowerWatts = wheelPowerWatts,
        wheelPowerKw = wheelPowerWatts / 1000f,
        wheelPowerCv = wheelPowerCv,
        enginePowerCv = enginePowerCv,
        engineRpm = sampleEngineRpm,
        engineTorqueNm = sampleEngineTorqueNm,
        engineTorqueKgfm = sampleEngineTorqueKgfm,
        wheelTorqueNm = sampleWheelTorqueNm,
        wheelTorqueKgfm = sampleWheelTorqueKgfm
      )
    }

    // Identificação de picos
    val rpms = recalculatedSamples.mapNotNull { it.engineRpm }.filter { it > 500 }
    val rpmSpan = if (rpms.isNotEmpty()) (rpms.maxOrNull() ?: 0) - (rpms.minOrNull() ?: 0) else null
    val isRpmValid = rpms.size >= 6 && rpmSpan != null && rpmSpan >= 800

    val peaks = VehicleCalculations.findSustainedPeaks(recalculatedSamples, isRpmValid = isRpmValid)

    val initialRpm = recalculatedSamples.firstOrNull { it.engineRpm != null && it.engineRpm!! > 500 }?.engineRpm
    val finalRpm = recalculatedSamples.lastOrNull { it.engineRpm != null && it.engineRpm!! > 500 }?.engineRpm
    val rpmRange = if (initialRpm != null && finalRpm != null) Pair(initialRpm, finalRpm) else null

    val previousConfigJson = createConfigurationSnapshot(originalRun)
    val previousCalcJson = """{"wheelPowerCv":${originalRun.wheelPowerCv},"enginePowerCv":${originalRun.enginePowerCv},"engineTorqueKgfm":${originalRun.engineTorqueKgfm},"peakRpm":${originalRun.peakPowerRpm ?: 0},"totalMassKg":${originalRun.totalVehicleMassKg}}"""

    val normFactor = if (originalRun.normalizationFactor > 0f) originalRun.normalizationFactor else 1.0f
    val anchoredAvgG = if (originalRun.anchoredAverageLongitudinalG > 0f) {
      originalRun.anchoredAverageLongitudinalG
    } else {
      val validSamples = recalculatedSamples.filter { it.isValid }
      if (validSamples.isNotEmpty()) {
        validSamples.map { it.longitudinalG }.average().toFloat()
      } else 0f
    }

    // Monta o RunResult recalculado
    val recalculatedRun = originalRun.copy(
      isRecalculated = true,
      revisionNumber = originalRun.revisionNumber + 1,
      parentRunId = originalRun.parentRunId ?: originalRun.id,
      recalculationReason = "Dados da passagem corrigidos pelo usuário",
      previousConfigurationJson = previousConfigJson,
      previousCalculatedResultJson = previousCalcJson,
      normalizationFactor = normFactor,
      anchoredAverageLongitudinalG = anchoredAvgG,
      rawAverageLongitudinalG = if (originalRun.rawAverageLongitudinalG > 0f) originalRun.rawAverageLongitudinalG else anchoredAvgG,
      totalVehicleMassKg = correctedMassKg,
      curbWeightKg = correctedConfig.curbWeightKg,
      driverWeightKg = correctedConfig.driverWeightKg,
      passengerCount = correctedConfig.passengerCount,
      passengerWeightKg = correctedConfig.effectivePassengerWeightKg,
      additionalWeightKg = correctedConfig.cargoWeightKg,
      soundSystemWeightKg = correctedConfig.soundSystemWeightKg,
      cngWeightKg = correctedConfig.cngWeightKg,
      fuelAdjustmentKg = correctedConfig.fuelWeightKg,
      gearUsed = correctedConfig.gearUsed,
      gearIndexUsed = correctedConfig.gearIndexUsed,
      gearRatioUsed = correctedConfig.gearRatio,
      finalDriveUsed = correctedConfig.finalDriveRatio,
      tireWidthMm = correctedConfig.tireWidthMm,
      tireAspectRatio = correctedConfig.tireAspectRatio,
      rimInches = correctedConfig.rimInches,
      drivetrainLossPercent = correctedConfig.drivetrainLossPercent,
      crrUsed = correctedConfig.crr,
      cdUsed = correctedConfig.cd,
      frontalAreaUsed = correctedConfig.frontalAreaM2,
      airDensityUsed = correctedConfig.airDensityKgM3,
      slopeModeUsed = correctedConfig.slopeMode,
      slopePercentUsed = correctedConfig.slopePercent,
      // Potência e torque recalculados
      estimatedPowerCv = peaks.peakEnginePowerCv,
      enginePowerCv = peaks.peakEnginePowerCv,
      wheelPowerCv = peaks.peakWheelPowerCv,
      wheelPowerKw = peaks.peakWheelPowerCv * 0.73549875f,
      enginePowerKw = peaks.peakEnginePowerCv * 0.73549875f,
      estimatedTorqueKgfm = peaks.engineTorqueKgfm,
      engineTorqueKgfm = peaks.engineTorqueKgfm,
      wheelTorqueKgfm = peaks.wheelTorqueKgfm,
      engineTorqueNm = peaks.engineTorqueKgfm * STANDARD_GRAVITY,
      wheelTorqueNm = peaks.wheelTorqueKgfm * STANDARD_GRAVITY,
      peakPowerRpm = peaks.peakPowerRpm,
      peakTorqueRpm = peaks.peakTorqueRpm,
      peakPowerSpeedKmh = peaks.peakPowerSpeedKmh,
      peakTorqueSpeedKmh = peaks.peakTorqueSpeedKmh,
      // DADOS QUE DEVEM PERMANECER IDÊNTICOS (Requisito 5)
      startSpeedKmh = originalRun.startSpeedKmh,
      officialStartSpeedKmh = originalRun.officialStartSpeedKmh,
      maximumGpsSpeedKmh = originalRun.maximumGpsSpeedKmh,
      officialMaxSpeedKmh = originalRun.officialMaxSpeedKmh,
      finalSpeedKmh = originalRun.finalSpeedKmh,
      officialEndSpeedKmh = originalRun.officialEndSpeedKmh,
      speedGainKmh = originalRun.speedGainKmh,
      officialSpeedGainKmh = originalRun.officialSpeedGainKmh,
      elapsedSeconds = originalRun.elapsedSeconds,
      timestamp = originalRun.timestamp,
      gpsAccuracyMeters = originalRun.gpsAccuracyMeters,
      averageGpsAccuracyMeters = originalRun.averageGpsAccuracyMeters,
      totalSamples = originalRun.totalSamples,
      validSamplesCount = originalRun.validSamplesCount,
      uniqueGpsFixCount = originalRun.uniqueGpsFixCount,
      locationCallbackCount = originalRun.locationCallbackCount,
      gpsSpeedChangeCount = originalRun.gpsSpeedChangeCount,
      sensorSampleCount = originalRun.sensorSampleCount,
      peakLongitudinalG = originalRun.peakLongitudinalG,
      averageLongitudinalG = originalRun.averageLongitudinalG,
      finishReason = originalRun.finishReason,
      samples = recalculatedSamples,
      uniqueGpsFixes = originalRun.uniqueGpsFixes,
      accelerationSplits = originalRun.accelerationSplits
    )

    return DynoRecalculationResult(
      recalculatedRun = recalculatedRun,
      recalculatedSamples = recalculatedSamples,
      peakWheelPowerCv = peaks.peakWheelPowerCv,
      peakEnginePowerCv = peaks.peakEnginePowerCv,
      peakTorqueNm = peaks.engineTorqueKgfm * STANDARD_GRAVITY,
      peakTorqueKgfm = peaks.engineTorqueKgfm,
      peakEngineTorqueKgfm = peaks.engineTorqueKgfm,
      peakWheelTorqueKgfm = peaks.wheelTorqueKgfm,
      peakPowerRpm = peaks.peakPowerRpm,
      peakTorqueRpm = peaks.peakTorqueRpm,
      peakPowerSpeedKmh = peaks.peakPowerSpeedKmh,
      peakTorqueSpeedKmh = peaks.peakTorqueSpeedKmh,
      initialRpm = initialRpm,
      finalRpm = finalRpm,
      rpmRange = rpmRange,
      totalMassKg = correctedMassKg,
      calculationWarnings = warnings
    )
  }

  /**
   * Serializa a configuração de uma passagem em JSON para histórico de revisão.
   */
  fun createConfigurationSnapshot(r: RunResult): String {
    return "{" +
      "\"totalMassKg\":${r.totalVehicleMassKg}," +
      "\"curbWeightKg\":${r.curbWeightKg}," +
      "\"driverWeightKg\":${r.driverWeightKg}," +
      "\"passengerCount\":${r.passengerCount}," +
      "\"passengerWeightKg\":${r.passengerWeightKg}," +
      "\"additionalWeightKg\":${r.additionalWeightKg}," +
      "\"soundSystemWeightKg\":${r.soundSystemWeightKg}," +
      "\"cngWeightKg\":${r.cngWeightKg}," +
      "\"fuelAdjustmentKg\":${r.fuelAdjustmentKg}," +
      "\"tireWidthMm\":${r.tireWidthMm}," +
      "\"tireAspectRatio\":${r.tireAspectRatio}," +
      "\"rimInches\":${r.rimInches}," +
      "\"gearUsed\":\"${r.gearUsed}\"," +
      "\"gearIndex\":${r.gearIndexUsed}," +
      "\"gearRatio\":${r.gearRatioUsed}," +
      "\"finalDrive\":${r.finalDriveUsed}," +
      "\"drivetrainLossPercent\":${r.drivetrainLossPercent}," +
      "\"cd\":${r.cdUsed}," +
      "\"frontalAreaM2\":${r.frontalAreaUsed}," +
      "\"crr\":${r.crrUsed}," +
      "\"airDensityKgM3\":${r.airDensityUsed}," +
      "\"slopeMode\":\"${r.slopeModeUsed}\"," +
      "\"slopePercent\":${r.slopePercentUsed}" +
      "}"
  }

  /**
   * Extrai a configuração atual de uma passagem existente para pré-preencher a tela de correção.
   */
  fun extractConfigFromRun(run: RunResult): DynoCorrectionConfig {
    val curb = if (run.curbWeightKg > 0f) run.curbWeightKg else (run.totalVehicleMassKg - (run.driverWeightKg + run.passengerWeightKg)).coerceAtLeast(300f)
    val driver = if (run.driverWeightKg > 0f) run.driverWeightKg else 80f
    val passCount = run.passengerCount
    val passWeight = run.passengerWeightKg
    val individual = if (passCount > 0) (passWeight / passCount).coerceAtLeast(40f) else 70f

    return DynoCorrectionConfig(
      curbWeightKg = curb,
      driverWeightKg = driver,
      passengerCount = passCount,
      passengerWeightMode = if (passCount > 0) DynoCorrectionConfig.PassengerWeightMode.INDIVIDUAL else DynoCorrectionConfig.PassengerWeightMode.TOTAL,
      passengerIndividualWeightKg = individual,
      passengerTotalWeightKg = passWeight,
      cargoWeightKg = run.additionalWeightKg,
      soundSystemWeightKg = run.soundSystemWeightKg,
      cngWeightKg = run.cngWeightKg,
      fuelWeightKg = run.fuelAdjustmentKg,
      gearUsed = run.gearUsed.ifBlank { "2ª" },
      gearIndexUsed = run.gearIndexUsed,
      gearRatio = if (run.gearRatioUsed > 0f) run.gearRatioUsed else 1.95f,
      finalDriveRatio = if (run.finalDriveUsed > 0f) run.finalDriveUsed else 4.10f,
      tireWidthMm = if (run.tireWidthMm in 125..385) run.tireWidthMm else 195,
      tireAspectRatio = if (run.tireAspectRatio in 20..90) run.tireAspectRatio else 55,
      rimInches = if (run.rimInches in 10..26) run.rimInches else 15,
      drivetrainLossPercent = if (run.drivetrainLossPercent in 0f..40f) run.drivetrainLossPercent else 12f,
      crr = if (run.crrUsed in 0.005f..0.05f) run.crrUsed else 0.015f,
      cd = if (run.cdUsed in 0.15f..1.0f) run.cdUsed else 0.34f,
      frontalAreaM2 = if (run.frontalAreaUsed in 1.0f..5.0f) run.frontalAreaUsed else 2.10f,
      airDensityKgM3 = if (run.airDensityUsed in 0.9f..1.5f) run.airDensityUsed else 1.225f,
      slopeMode = run.slopeModeUsed,
      slopePercent = run.slopePercentUsed
    )
  }
}
