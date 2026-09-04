package com.example.model

import java.util.UUID
import kotlin.math.PI

enum class AudioWeightPreset(
  val label: String,
  val estimatedWeightKg: Float,
  val minWeightKg: Float,
  val maxWeightKg: Float
) {
  NONE("Não, somente som original", 0f, 0f, 0f),
  LIGHT("Som leve (módulozinho / caixa selada)", 18f, 10f, 25f),
  MEDIUM("Som médio (caixa dutada + amplificador)", 43f, 25f, 60f),
  HEAVY("Som pesado (subwoofers potentes / baterias)", 90f, 60f, 120f),
  PAREDAO("Projeto de som / Paredão", 0f, 0f, 0f),
  CUSTOM("Informar peso exato", 0f, 0f, 0f)
}

enum class WeightConfidence(
  val label: String,
  val description: String
) {
  HIGH(
    "Alta Confiança",
    "Peso verificado diretamente em balança."
  ),
  GOOD(
    "Boa Confiança",
    "Veículo conhecido com dados de fábrica e sem grandes pesos estimados."
  ),
  ESTIMATED(
    "Estimada",
    "Parte do peso foi estimada. O resultado da potência terá uma margem maior."
  )
}

data class RunQualityEvaluation(
  val quality: String, // "BOA", "REGULAR", "DADOS INSUFICIENTES", "INVÁLIDA"
  val confidenceLevel: String, // "ALTA", "MEDIA", "BAIXA"
  val marginPercent: Float, // 10f, 15f, 25f
  val marginDisplay: String, // "±10%", "±15%", "acima de ±20%", "Não homologada"
  val invalidationReason: String? = null,
  val isPreliminary: Boolean = false,
  val canCompare: Boolean = false,
  val isEligibleForComparison: Boolean = canCompare
)

data class SustainedPeaks(
  val peakEnginePowerCv: Float,
  val peakWheelPowerCv: Float,
  val peakLongitudinalG: Float,
  val peakPowerRpm: Int?,
  val peakTorqueRpm: Int?,
  val peakPowerSpeedKmh: Float,
  val peakTorqueSpeedKmh: Float,
  val engineTorqueKgfm: Float,
  val wheelTorqueKgfm: Float
)

enum class CurveDisplayType {
  RPM,
  SPEED,
  INSUFFICIENT
}

data class TorqueResult(
  val torqueNm: Float = 0f,
  val torqueKgfm: Float = 0f
)

data class TransmissionProfile(
  val id: String,
  val manufacturer: String,
  val family: String,
  val code: String,
  val displayName: String,
  val gearRatios: List<Float>,
  val finalDrive: Float,
  val compatibleVehicleIds: List<String> = emptyList(),
  val isCustom: Boolean = false
)

data class VehicleProfile(
  val id: String = UUID.randomUUID().toString(),
  val manufacturer: String,
  val model: String,
  val year: Int,
  val version: String = "",
  val engine: String = "",
  val displacement: String = "",
  val factoryPowerCv: Float? = null,
  val factoryTorqueKgf: Float? = null,
  val curbWeightKg: Float = 1000f,
  val drivetrain: String = "Dianteira",
  val transmissionId: String? = null,
  val customTransmissionName: String? = null,
  val gearRatio: Float? = null,
  val finalDriveRatio: Float? = null,
  val customDrivetrainLossPercent: Float? = null,
  val tireWidthMm: Int = 185,
  val tireAspectRatio: Int = 70,
  val wheelDiameterInches: Int = 14,
  val tireCorrectionPercent: Float = 0.0f,
  val driverWeightKg: Float = 0f,
  val passengerWeightKg: Float = 0f,
  val cargoWeightKg: Float = 0f,
  val audioPreset: AudioWeightPreset = AudioWeightPreset.NONE,
  val audioWeightKg: Float = 0f,
  val gnvWeightKg: Float = 0f,
  val otherWeightKg: Float = 0f,
  val removedWeightKg: Float = 0f,
  val measuredTotalWeightKg: Float? = null,
  val useMeasuredWeight: Boolean = false,
  val frontalAreaM2: Float = 2.10f,
  val dragCoefficient: Float = 0.34f,
  val rollingResistanceCoeff: Float = 0.015f,
  val airDensityKgM3: Float = 1.225f,
  val slopeMode: String = "IGNORE", // "IGNORE", "ESTIMATED", "MANUAL"
  val manualSlopePercent: Float = 0.0f,
  val isPrimary: Boolean = false,
  val isCustom: Boolean = false
) {
  val totalWeightKg: Float
    get() = VehicleCalculations.calculateTotalWeight(
      curbWeightKg = curbWeightKg,
      driverWeightKg = driverWeightKg,
      passengerWeightKg = passengerWeightKg,
      cargoWeightKg = cargoWeightKg,
      audioWeightKg = audioWeightKg,
      gnvWeightKg = gnvWeightKg,
      otherWeightKg = otherWeightKg,
      removedWeightKg = removedWeightKg,
      measuredTotalWeightKg = measuredTotalWeightKg,
      useMeasuredWeight = useMeasuredWeight
    )
}

data class TireCalculation(
  val lateralHeightMm: Double,
  val rimDiameterMm: Double,
  val totalDiameterMm: Double,
  val circumferenceM: Double,
  val formattedMeasure: String
)

object VehicleCalculations {
  const val STANDARD_GRAVITY = 9.80665f
  const val WATTS_PER_CV = 735.49875f
  const val AIR_DENSITY_SEA_LEVEL = 1.225f
  const val DEFAULT_CRR = 0.015f
  const val DEFAULT_CD = 0.34f
  const val DEFAULT_FRONTAL_AREA = 2.10f

  fun mps2ToG(mps2: Float): Float = mps2 / STANDARD_GRAVITY

  fun gToMps2(g: Float): Float = g * STANDARD_GRAVITY

  fun calculateTireDimensions(
    widthMm: Int,
    aspectRatio: Int,
    rimInches: Int,
    tireCorrectionPercent: Float = 0.0f
  ): TireCalculation {
    val lateralHeight = widthMm * (aspectRatio / 100.0)
    val rimDiameter = rimInches * 25.4
    val totalDiameter = rimDiameter + 2.0 * lateralHeight
    val baseCircumference = (PI * totalDiameter) / 1000.0
    val correctedCircumference = baseCircumference * (1.0 + (tireCorrectionPercent / 100.0))

    return TireCalculation(
      lateralHeightMm = lateralHeight,
      rimDiameterMm = rimDiameter,
      totalDiameterMm = totalDiameter,
      circumferenceM = correctedCircumference,
      formattedMeasure = "$widthMm/${aspectRatio} R$rimInches"
    )
  }

  fun calculateTotalWeight(
    curbWeightKg: Float,
    driverWeightKg: Float,
    passengerWeightKg: Float = 0f,
    cargoWeightKg: Float = 0f,
    audioWeightKg: Float = 0f,
    gnvWeightKg: Float = 0f,
    otherWeightKg: Float = 0f,
    removedWeightKg: Float = 0f,
    measuredTotalWeightKg: Float? = null,
    useMeasuredWeight: Boolean = false,
    fuelAdjustmentKg: Float = 0f
  ): Float {
    if (useMeasuredWeight && measuredTotalWeightKg != null && measuredTotalWeightKg > 0f) {
      return measuredTotalWeightKg
    }
    val sum = curbWeightKg + driverWeightKg + passengerWeightKg + cargoWeightKg +
      audioWeightKg + gnvWeightKg + otherWeightKg + fuelAdjustmentKg - removedWeightKg
    return sum.coerceAtLeast(0f)
  }

  fun evaluateWeightConfidence(
    useMeasuredWeight: Boolean,
    audioPreset: AudioWeightPreset,
    hasGnv: Boolean,
    hasCargo: Boolean
  ): WeightConfidence {
    if (useMeasuredWeight) return WeightConfidence.HIGH
    if (audioPreset == AudioWeightPreset.PAREDAO ||
      audioPreset == AudioWeightPreset.HEAVY ||
      audioPreset == AudioWeightPreset.CUSTOM ||
      hasGnv || hasCargo) {
      return WeightConfidence.ESTIMATED
    }
    return WeightConfidence.GOOD
  }

  fun calculateRollingResistanceForce(
    totalMassKg: Float,
    crr: Float = DEFAULT_CRR
  ): Float {
    return (crr * totalMassKg * STANDARD_GRAVITY).coerceAtLeast(0f)
  }

  fun calculateAerodynamicForce(
    velocityMps: Float,
    cd: Float = DEFAULT_CD,
    frontalAreaM2: Float = DEFAULT_FRONTAL_AREA,
    airDensity: Float = AIR_DENSITY_SEA_LEVEL
  ): Float {
    val v = velocityMps.coerceAtLeast(0f)
    return (0.5f * airDensity * cd * frontalAreaM2 * v * v).coerceAtLeast(0f)
  }

  fun calculateSlopeForce(totalMassKg: Float, slopePercent: Float): Float {
    if (slopePercent == 0f) return 0f
    val angleRad = kotlin.math.atan(slopePercent / 100.0)
    return (totalMassKg * STANDARD_GRAVITY * kotlin.math.sin(angleRad)).toFloat()
  }

  fun calculateAccelerationForce(
    totalMassKg: Float,
    accelerationMps2: Float
  ): Float {
    return totalMassKg * accelerationMps2
  }

  fun calculateTractiveForce(
    accelForceN: Float,
    rollForceN: Float,
    aeroForceN: Float,
    slopeForceN: Float = 0f
  ): Float {
    return (accelForceN + rollForceN + aeroForceN + slopeForceN).coerceAtLeast(0f)
  }

  fun calculateTotalTractiveForce(
    accelForceN: Float,
    rollForceN: Float,
    aeroForceN: Float,
    slopeForceN: Float = 0f
  ): Float {
    return calculateTractiveForce(accelForceN, rollForceN, aeroForceN, slopeForceN)
  }

  fun calculateWheelPowerWatts(
    tractiveForceN: Float,
    velocityMps: Float
  ): Float {
    return (tractiveForceN * velocityMps.coerceAtLeast(0f)).coerceAtLeast(0f)
  }

  fun convertWattsToCv(powerWatts: Float): Float {
    return (powerWatts / WATTS_PER_CV).coerceAtLeast(0f)
  }

  fun convertCvToWatts(powerCv: Float): Float {
    return (powerCv * WATTS_PER_CV).coerceAtLeast(0f)
  }

  fun getDrivetrainEfficiency(
    drivetrain: String?,
    customLossPercent: Float? = null
  ): Float {
    if (customLossPercent != null && customLossPercent in 0f..50f) {
      return (1.0f - (customLossPercent / 100f)).coerceIn(0.5f, 1.0f)
    }
    return when (drivetrain?.lowercase()?.trim()) {
      "dianteira", "fwd" -> 0.88f // 12% perda
      "traseira", "rwd" -> 0.85f  // 15% perda
      "integral", "4x4", "awd", "4wd" -> 0.80f // 20% perda
      else -> 0.88f
    }
  }

  fun getDrivetrainLossPercent(
    drivetrain: String?,
    customLossPercent: Float? = null
  ): Float {
    if (customLossPercent != null && customLossPercent in 0f..50f) {
      return customLossPercent
    }
    val efficiency = getDrivetrainEfficiency(drivetrain, null)
    return (1.0f - efficiency) * 100f
  }

  fun calculateEnginePowerCv(
    wheelPowerCv: Float,
    drivetrain: String?,
    customLossPercent: Float? = null
  ): Float {
    val efficiency = getDrivetrainEfficiency(drivetrain, customLossPercent)
    if (efficiency <= 0f) return wheelPowerCv
    return (wheelPowerCv / efficiency).coerceAtLeast(0f)
  }

  fun calculateRpmFromSpeed(
    velocityMps: Float,
    tireCircumferenceM: Double,
    gearRatio: Float,
    finalDriveRatio: Float
  ): Float? {
    if (tireCircumferenceM <= 0.0 || gearRatio <= 0f || finalDriveRatio <= 0f) return null
    val wheelRps = velocityMps / tireCircumferenceM
    val wheelRpm = wheelRps * 60.0
    val engineRpm = (wheelRpm * gearRatio * finalDriveRatio).toFloat()
    return if (engineRpm in 400f..14000f) engineRpm else null
  }

  fun calculateTorqueFromPowerWatts(powerWatts: Double, rpm: Int): TorqueResult {
    if (rpm <= 0 || powerWatts <= 0.0 || powerWatts.isNaN() || powerWatts.isInfinite()) {
      return TorqueResult(0f, 0f)
    }
    val omega = rpm * (2.0 * kotlin.math.PI / 60.0)
    if (omega <= 0.0) return TorqueResult(0f, 0f)
    val torqueNm = (powerWatts / omega).toFloat()
    val torqueKgfm = torqueNm / 9.80665f
    if (torqueNm.isNaN() || torqueNm.isInfinite() || torqueKgfm.isNaN() || torqueKgfm.isInfinite()) {
      return TorqueResult(0f, 0f)
    }
    return TorqueResult(torqueNm, torqueKgfm)
  }

  fun calculateTorqueFromPowerWatts(powerWatts: Float, rpm: Float): Pair<Float, Float>? {
    if (rpm <= 500f || rpm > 14000f || powerWatts <= 0f || rpm.isNaN() || powerWatts.isNaN()) return null
    val res = calculateTorqueFromPowerWatts(powerWatts.toDouble(), rpm.toInt())
    return if (res.torqueKgfm in 0.1f..2000f) Pair(res.torqueNm, res.torqueKgfm) else null
  }

  fun calculateTorqueKgfm(powerCv: Float, rpm: Float): Float? {
    if (rpm <= 500f || powerCv <= 0f || rpm.isNaN() || powerCv.isNaN()) return null
    val powerWatts = convertCvToWatts(powerCv)
    return calculateTorqueFromPowerWatts(powerWatts, rpm)?.second
  }

  fun calculateTorqueNm(powerCv: Float, rpm: Float): Float? {
    if (rpm <= 500f || powerCv <= 0f || rpm.isNaN() || powerCv.isNaN()) return null
    val powerWatts = convertCvToWatts(powerCv)
    return calculateTorqueFromPowerWatts(powerWatts, rpm)?.first
  }

  fun calculateTorqueNm(torqueKgfm: Float): Float {
    if (torqueKgfm <= 0f || torqueKgfm.isNaN() || torqueKgfm.isInfinite()) return 0f
    return torqueKgfm * 9.80665f
  }

  fun calculateTorqueKgfmFromPower(powerCv: Float, rpm: Float): Float? {
    return calculateTorqueKgfm(powerCv, rpm)
  }

  fun classifyRunQuality(
    speedGainKmh: Float,
    validGpsLocationsCount: Int = 10,
    elapsedSec: Float = 5.0f,
    lastGpsAccuracyMeters: Float = 5f,
    avgSyncDiffKmh: Float = 2f,
    rejectionRatio: Float = 0.05f,
    finishReason: FinishReason = FinishReason.GPS_DECELERATION,
    isPhoneStable: Boolean = true,
    gearShiftDetected: Boolean = false,
    finalGpsSpeedKmh: Float = 50f,
    startGpsSpeedKmh: Float = 40f,
    rpmSpan: Int? = null,
    maxSpeedKmh: Float = finalGpsSpeedKmh,
    startSpeedKmh: Float = startGpsSpeedKmh,
    validGpsCount: Int = validGpsLocationsCount,
    elapsedSeconds: Float = elapsedSec,
    gpsAccuracy: Float = lastGpsAccuracyMeters,
    isMountedStable: Boolean = isPhoneStable,
    hasGearShift: Boolean = gearShiftDetected,
    hasExcessiveVibration: Boolean = false,
    hasBraking: Boolean = false,
    hasLossOfGps: Boolean = false,
    isReverseMovement: Boolean = false,
    isSpeedDecrease: Boolean = false,
    gpsFrozen: Boolean = false,
    maxIntegratedSpeedKmh: Float = maxSpeedKmh,
    maxGpsSpeedKmh: Float = maxSpeedKmh,
    sensorDeltaVMps: Float = 0f,
    gpsDeltaVMps: Float = 0f,
    syncPairsCount: Int = 10,
    targetEndSpeedKmh: Float? = null
  ): RunQualityEvaluation {
    val effectiveGpsCount = if (validGpsCount != 10) validGpsCount else validGpsLocationsCount
    val effectiveElapsedSec = if (elapsedSeconds != 5.0f) elapsedSeconds else elapsedSec
    val effectiveAccuracy = if (gpsAccuracy != 5f) gpsAccuracy else lastGpsAccuracyMeters
    val effectiveStable = isPhoneStable && isMountedStable && !hasExcessiveVibration
    val effectiveGearShift = gearShiftDetected || hasGearShift
    val effectiveFinalSpeed = if (maxSpeedKmh != 50f) maxSpeedKmh else finalGpsSpeedKmh
    val effectiveStartSpeed = if (startSpeedKmh != 40f) startSpeedKmh else startGpsSpeedKmh

    // =========================================================================
    // 0. ERROS FATAIS / INVÁLIDA (Sinal perdido, frenagem, ré, corrupção)
    // =========================================================================
    if (finishReason == FinishReason.CANCELLED || finishReason == FinishReason.GPS_LOST || hasLossOfGps) {
      return RunQualityEvaluation(
        quality = "INVÁLIDA",
        confidenceLevel = "BAIXA",
        marginPercent = 0f,
        marginDisplay = "Não homologada",
        invalidationReason = "Teste cancelado ou sinal GPS perdido.",
        isPreliminary = false,
        canCompare = false
      )
    }
    if (hasBraking || isReverseMovement || isSpeedDecrease) {
      return RunQualityEvaluation(
        quality = "INVÁLIDA",
        confidenceLevel = "BAIXA",
        marginPercent = 0f,
        marginDisplay = "Não homologada",
        invalidationReason = "Frenagem, redução de velocidade ou movimento reverso detectado.",
        isPreliminary = false,
        canCompare = false
      )
    }
    if (effectiveElapsedSec <= 0.05f) {
      return RunQualityEvaluation(
        quality = "INVÁLIDA",
        confidenceLevel = "BAIXA",
        marginPercent = 0f,
        marginDisplay = "Não homologada",
        invalidationReason = "Duração zero ou dados corrompidos.",
        isPreliminary = false,
        canCompare = false
      )
    }
    if (effectiveGpsCount < 4) {
      return RunQualityEvaluation(
        quality = "INVÁLIDA",
        confidenceLevel = "BAIXA",
        marginPercent = 0f,
        marginDisplay = "Não homologada",
        invalidationReason = "Menos de 4 leituras GPS válidas ($effectiveGpsCount < 4).",
        isPreliminary = false,
        canCompare = false
      )
    }
    if (effectiveAccuracy > 25f) {
      return RunQualityEvaluation(
        quality = "INVÁLIDA",
        confidenceLevel = "BAIXA",
        marginPercent = 0f,
        marginDisplay = "Não homologada",
        invalidationReason = "Precisão horizontal do GPS degradada (> 25m).",
        isPreliminary = false,
        canCompare = false
      )
    }
    if (effectiveFinalSpeed < (effectiveStartSpeed - 1.0f) || speedGainKmh <= 0f) {
      return RunQualityEvaluation(
        quality = "INVÁLIDA",
        confidenceLevel = "BAIXA",
        marginPercent = 0f,
        marginDisplay = "Não homologada",
        invalidationReason = "Velocidade final menor que a inicial.",
        isPreliminary = false,
        canCompare = false
      )
    }
    if (gpsFrozen) {
      return RunQualityEvaluation(
        quality = "GPS INCONSISTENTE",
        confidenceLevel = "BAIXA",
        marginPercent = 25.0f,
        marginDisplay = "acima de ±20%",
        invalidationReason = "GPS congelado durante a aceleração. Potência preliminar — GPS não acompanhou toda a aceleração.",
        isPreliminary = true,
        canCompare = false
      )
    }

    // =========================================================================
    // 1. PRIORIDADE 1: PASSAGEM INCOMPLETA
    // - ganho de velocidade insuficiente (< 25.0 km/h para estimar potência máxima)
    // - faixa de RPM insuficiente (< 800 RPM)
    // - aceleração interrompida
    // - troca de marcha
    // - término antes da faixa configurada
    // =========================================================================
    val isIncompleteGearShift = effectiveGearShift || finishReason == FinishReason.GEAR_SHIFT
    val isPrematureSpeedGain = speedGainKmh < 25.0f
    val isInsufficientRpmSpan = rpmSpan != null && rpmSpan < 800
    val isInterrupted = finishReason == FinishReason.CLUTCH_DISENGAGED || finishReason == FinishReason.PREMATURE_TERMINATION
    val isBeforeTargetSpeed = targetEndSpeedKmh != null && effectiveFinalSpeed < (targetEndSpeedKmh - 2.0f)

    if (isIncompleteGearShift || isPrematureSpeedGain || isInsufficientRpmSpan || isInterrupted || isBeforeTargetSpeed) {
      val incompleteReason = when {
        isIncompleteGearShift -> "Troca de marcha detectada. A passagem foi encerrada e poderá ser considerada incompleta."
        isBeforeTargetSpeed -> "A aceleração terminou antes da faixa necessária para estimar a potência máxima."
        isPrematureSpeedGain -> "A aceleração terminou antes da faixa necessária para estimar a potência máxima."
        isInsufficientRpmSpan -> "Faixa de RPM insuficiente ($rpmSpan RPM < 800 RPM)."
        isInterrupted -> "Aceleração interrompida antes da conclusão da faixa."
        else -> "A aceleração terminou antes da faixa necessária para estimar a potência máxima."
      }
      return RunQualityEvaluation(
        quality = "PASSAGEM INCOMPLETA",
        confidenceLevel = "BAIXA",
        marginPercent = 25.0f,
        marginDisplay = "acima de ±20%",
        invalidationReason = incompleteReason,
        isPreliminary = true,
        canCompare = false
      )
    }

    // =========================================================================
    // 2. PRIORIDADE 2: DADOS INSUFICIENTES
    // - menos de 8 atualizações GPS válidas
    // - duração insuficiente (< 3.0s)
    // - poucos pares sincronizados GPS × sensor (< 4)
    // =========================================================================
    if (effectiveGpsCount < 8 || effectiveElapsedSec < 3.0f || syncPairsCount < 4) {
      val reasonDesc = when {
        effectiveGpsCount < 8 -> "Menos de 8 atualizações GPS válidas ($effectiveGpsCount < 8)."
        effectiveElapsedSec < 3.0f -> "Duração da passagem insuficiente (${String.format(java.util.Locale.US, "%.2f", effectiveElapsedSec)}s < 3.00s)."
        syncPairsCount < 4 -> "Poucos pares sincronizados GPS × sensores ($syncPairsCount < 4)."
        else -> "Dados insuficientes para cálculo preciso."
      }
      return RunQualityEvaluation(
        quality = "DADOS INSUFICIENTES",
        confidenceLevel = "BAIXA",
        marginPercent = 25.0f,
        marginDisplay = "acima de ±20%",
        invalidationReason = reasonDesc,
        isPreliminary = true,
        canCompare = false
      )
    }

    // =========================================================================
    // 3. PRIORIDADE 3: GPS/SENSOR DIVERGENTE
    // - somente avaliar quando houver dados suficientes (>= 8 GPS e >= 4 pares)
    // - não usar essa classificação apenas porque a passagem foi curta
    // =========================================================================
    val effectiveGpsDeltaV = if (gpsDeltaVMps > 0.1f) gpsDeltaVMps else ((maxGpsSpeedKmh - startSpeedKmh).coerceAtLeast(0f) / 3.6f)
    if (sensorDeltaVMps > 0.5f && effectiveGpsDeltaV > 0.5f) {
      val deltaVDivergence = kotlin.math.abs(sensorDeltaVMps - effectiveGpsDeltaV) / effectiveGpsDeltaV
      if (deltaVDivergence > 0.40f) {
        return RunQualityEvaluation(
          quality = "GPS/SENSOR DIVERGENTE",
          confidenceLevel = "BAIXA",
          marginPercent = 20.0f,
          marginDisplay = "±20%",
          invalidationReason = "Divergência severa entre acelerômetro e GPS (${(deltaVDivergence * 100).toInt()}% > 15%). Aceleração normalizada pelo GPS.",
          isPreliminary = true,
          canCompare = false
        )
      } else if (deltaVDivergence > 0.15f) {
        return RunQualityEvaluation(
          quality = "REGULAR",
          confidenceLevel = "MEDIA",
          marginPercent = 15.0f,
          marginDisplay = "±15%",
          invalidationReason = "Divergência moderada entre acelerômetro e GPS (${(deltaVDivergence * 100).toInt()}% > 15%).",
          isPreliminary = false,
          canCompare = true
        )
      }
    }

    // =========================================================================
    // 4. PRIORIDADE 4: REGULAR ou BOA
    // - somente quando a janela for suficiente
    // =========================================================================
    val isGpsGood = effectiveAccuracy <= 10.0f && avgSyncDiffKmh <= 8.0f && rejectionRatio <= 0.20f
    if (speedGainKmh >= 25.0f && effectiveGpsCount >= 10 && effectiveElapsedSec in 3.0f..25.0f && isGpsGood && effectiveStable) {
      return RunQualityEvaluation(
        quality = "BOA",
        confidenceLevel = "ALTA",
        marginPercent = 10.0f,
        marginDisplay = "±10%",
        invalidationReason = null,
        isPreliminary = false,
        canCompare = true
      )
    } else {
      return RunQualityEvaluation(
        quality = "REGULAR",
        confidenceLevel = "MEDIA",
        marginPercent = 15.0f,
        marginDisplay = "±15%",
        invalidationReason = null,
        isPreliminary = false,
        canCompare = true
      )
    }
  }

  fun findSustainedPeaks(
    samples: List<RunSample>,
    isRpmValid: Boolean = true
  ): SustainedPeaks {
    val validSamples = samples.filter { it.isValid && (it.finalAccelerationMps2 > 0.02f || it.longitudinalG > 0.005f || it.enginePowerCv > 1f) }
    if (validSamples.isEmpty()) {
      return SustainedPeaks(
        peakEnginePowerCv = 0f,
        peakWheelPowerCv = 0f,
        peakLongitudinalG = 0f,
        peakPowerRpm = null,
        peakTorqueRpm = null,
        peakPowerSpeedKmh = 0f,
        peakTorqueSpeedKmh = 0f,
        engineTorqueKgfm = 0f,
        wheelTorqueKgfm = 0f
      )
    }

    // Validação de pico sustentado por pelo menos 3 amostras consecutivas (~150 ms) e GPS crescente
    val candidates = mutableListOf<RunSample>()
    for (i in validSamples.indices) {
      val sample = validSamples[i]
      val prev = validSamples.getOrNull(i - 1)
      val next = validSamples.getOrNull(i + 1)

      val isSpeedGrowing = when {
        prev != null && next != null -> {
          sample.filteredSpeedKmh >= (prev.filteredSpeedKmh - 0.3f) &&
            next.filteredSpeedKmh >= (sample.filteredSpeedKmh - 0.3f)
        }
        prev != null -> sample.filteredSpeedKmh >= (prev.filteredSpeedKmh - 0.3f)
        next != null -> next.filteredSpeedKmh >= (sample.filteredSpeedKmh - 0.3f)
        else -> true
      }

      val isSustainedG = when {
        prev != null && next != null -> {
          val maxNeighborG = kotlin.math.max(prev.longitudinalG, next.longitudinalG)
          val avgNeighborG = (prev.longitudinalG + next.longitudinalG) / 2f
          sample.longitudinalG <= (maxNeighborG * 1.35f + 0.03f) && sample.longitudinalG <= (avgNeighborG * 1.45f + 0.04f)
        }
        else -> true
      }

      val isSustainedPower = when {
        prev != null && next != null -> {
          val maxNeighborP = kotlin.math.max(prev.enginePowerCv, next.enginePowerCv)
          val avgNeighborP = (prev.enginePowerCv + next.enginePowerCv) / 2f
          sample.enginePowerCv <= (maxNeighborP * 1.35f + 3f) && sample.enginePowerCv <= (avgNeighborP * 1.45f + 4f)
        }
        else -> true
      }

      if (isSpeedGrowing && isSustainedG && isSustainedPower) {
        candidates.add(sample)
      }
    }

    val pool = if (candidates.size >= 3) candidates else validSamples

    val maxPowerSample = pool.maxByOrNull { it.enginePowerCv } ?: pool.first()
    val maxEnginePowerCv = maxPowerSample.enginePowerCv
    val maxWheelPowerCv = maxPowerSample.wheelPowerCv
    val peakPowerRpm = if (isRpmValid) maxPowerSample.engineRpm else null
    val peakPowerSpeedKmh = maxPowerSample.gpsSpeedKmh
    val maxG = pool.map { it.longitudinalG }.maxOrNull() ?: 0f

    var maxEngineTorqueKgfm = 0f
    var maxWheelTorqueKgfm = 0f
    var peakTorqueRpm: Int? = null
    var peakTorqueSpeedKmh = peakPowerSpeedKmh

    if (isRpmValid) {
      val torquePool = pool.filter { (it.engineRpm ?: 0) in 1000..7500 && it.engineTorqueKgfm > 0f }
      val maxTorqueSample = torquePool.maxByOrNull { it.engineTorqueKgfm } ?: pool.filter { it.engineTorqueKgfm > 0f }.maxByOrNull { it.engineTorqueKgfm }
      if (maxTorqueSample != null && (maxTorqueSample.engineRpm ?: 0) > 500) {
        maxEngineTorqueKgfm = maxTorqueSample.engineTorqueKgfm
        maxWheelTorqueKgfm = maxTorqueSample.wheelTorqueKgfm
        peakTorqueRpm = maxTorqueSample.engineRpm
        peakTorqueSpeedKmh = maxTorqueSample.gpsSpeedKmh
      }
    }

    return SustainedPeaks(
      peakEnginePowerCv = maxEnginePowerCv,
      peakWheelPowerCv = maxWheelPowerCv,
      peakLongitudinalG = maxG,
      peakPowerRpm = peakPowerRpm,
      peakTorqueRpm = peakTorqueRpm,
      peakPowerSpeedKmh = peakPowerSpeedKmh,
      peakTorqueSpeedKmh = peakTorqueSpeedKmh,
      engineTorqueKgfm = maxEngineTorqueKgfm,
      wheelTorqueKgfm = maxWheelTorqueKgfm
    )
  }

  fun evaluateCurveEligibility(
    samples: List<RunSample>,
    gearUsed: String?,
    gearRatio: Float?,
    finalDrive: Float?,
    tireCircumferenceM: Double?,
    speedGainKmh: Float
  ): CurveDisplayType {
    val validSamples = samples.filter {
      it.isValid &&
      it.filteredSpeedKmh > 0f &&
      (it.enginePowerCv > 0f || it.wheelPowerCv > 0f) &&
      !it.filteredSpeedKmh.isNaN() &&
      !it.filteredSpeedKmh.isInfinite()
    }
    if (validSamples.size < 3 || speedGainKmh < 10.0f) {
      return CurveDisplayType.INSUFFICIENT
    }

    val hasValidGear = !gearUsed.isNullOrBlank() && (gearRatio ?: 0f) > 0f && (finalDrive ?: 0f) > 0f && (tireCircumferenceM ?: 0.0) > 0.0
    val rpms = validSamples.mapNotNull { it.engineRpm }.filter { it > 500 }
    val rpmSpan = if (rpms.isNotEmpty()) ((rpms.maxOrNull() ?: 0) - (rpms.minOrNull() ?: 0)) else 0

    if (hasValidGear && speedGainKmh >= 15.0f && rpmSpan >= 800 && rpms.size >= 8) {
      return CurveDisplayType.RPM
    }

    if (speedGainKmh >= 10.0f && validSamples.size >= 6) {
      return CurveDisplayType.SPEED
    }

    return CurveDisplayType.INSUFFICIENT
  }

  /**
   * Recalcula os resultados de uma medição a partir de correções em seus parâmetros de cadastro
   * (Seção 37: "Corrigir dados da passagem", centralizado via DynoRecalculationEngine).
   */
  fun recalculateRunResult(
    run: RunResult,
    correctedTotalMassKg: Float,
    correctedGearRatio: Float,
    correctedFinalDrive: Float,
    correctedTireWidthMm: Int,
    correctedTireAspectRatio: Int,
    correctedRimInches: Int,
    correctedLossPercent: Float,
    correctedCd: Float,
    correctedFrontalAreaM2: Float,
    correctedCrr: Float
  ): RunResult {
    val baseConfig = DynoRecalculationEngine.extractConfigFromRun(run)
    val accessoriesWeight = (baseConfig.driverWeightKg + baseConfig.effectivePassengerWeightKg +
      baseConfig.cargoWeightKg + baseConfig.soundSystemWeightKg + baseConfig.cngWeightKg + baseConfig.fuelWeightKg)
    val curbWeight = (correctedTotalMassKg - accessoriesWeight).coerceAtLeast(100f)

    val config = baseConfig.copy(
      curbWeightKg = curbWeight,
      gearRatio = correctedGearRatio,
      finalDriveRatio = correctedFinalDrive,
      tireWidthMm = correctedTireWidthMm,
      tireAspectRatio = correctedTireAspectRatio,
      rimInches = correctedRimInches,
      drivetrainLossPercent = correctedLossPercent,
      cd = correctedCd,
      frontalAreaM2 = correctedFrontalAreaM2,
      crr = correctedCrr
    )

    return DynoRecalculationEngine.recalculate(run.samples, config, run).recalculatedRun
  }
}
