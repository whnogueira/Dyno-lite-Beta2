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
    gpsDeltaVMps: Float = 0f
  ): RunQualityEvaluation {
    val effectiveGpsCount = if (validGpsCount != 10) validGpsCount else validGpsLocationsCount
    val effectiveElapsedSec = if (elapsedSeconds != 5.0f) elapsedSeconds else elapsedSec
    val effectiveAccuracy = if (gpsAccuracy != 5f) gpsAccuracy else lastGpsAccuracyMeters
    val effectiveStable = isPhoneStable && isMountedStable && !hasExcessiveVibration
    val effectiveGearShift = gearShiftDetected || hasGearShift
    val effectiveFinalSpeed = if (maxSpeedKmh != 50f) maxSpeedKmh else finalGpsSpeedKmh
    val effectiveStartSpeed = if (startSpeedKmh != 40f) startSpeedKmh else startGpsSpeedKmh

    // 0. Detecção de Divergência Acelerômetro vs GPS / GPS Congelado
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

    val diffSpeed = kotlin.math.abs(maxGpsSpeedKmh - maxIntegratedSpeedKmh)
    if (gpsFrozen || (diffSpeed > 10.0f && gpsFrozen)) {
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

    // 1. Condições de INVÁLIDA
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
    if (effectiveGearShift) {
      return RunQualityEvaluation(
        quality = "INVÁLIDA",
        confidenceLevel = "BAIXA",
        marginPercent = 0f,
        marginDisplay = "Não homologada",
        invalidationReason = "Troca de marcha detectada durante a medição.",
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

    // 2. Condições de DADOS INSUFICIENTES (ex: ganho de 6,4 km/h ou 14,9 km/h)
    if (speedGainKmh < 15.0f || effectiveElapsedSec < 3.0f || effectiveGpsCount < 6 || (rpmSpan != null && rpmSpan < 800)) {
      val reasonDesc = when {
        speedGainKmh < 15.0f -> "Ganho de velocidade GPS insuficiente (${String.format(java.util.Locale.US, "%.1f", speedGainKmh)} km/h < 15.0 km/h)."
        effectiveElapsedSec < 3.0f -> "Duração da passagem insuficiente (${String.format(java.util.Locale.US, "%.2f", effectiveElapsedSec)}s < 3.00s)."
        effectiveGpsCount < 6 -> "Poucas leituras de GPS válidas ($effectiveGpsCount < 6)."
        rpmSpan != null && rpmSpan < 800 -> "Faixa de RPM insuficiente ($rpmSpan RPM < 800 RPM)."
        else -> "Faixa de aceleração insuficiente."
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

    // 3. Condições de BOA
    val isGpsGood = effectiveAccuracy <= 10.0f && avgSyncDiffKmh <= 8.0f && rejectionRatio <= 0.20f
    if (speedGainKmh >= 20.0f && effectiveGpsCount >= 8 && effectiveElapsedSec in 3.0f..25.0f && isGpsGood && effectiveStable) {
      return RunQualityEvaluation(
        quality = "BOA",
        confidenceLevel = "ALTA",
        marginPercent = 10.0f,
        marginDisplay = "±10%",
        invalidationReason = null,
        isPreliminary = false,
        canCompare = true
      )
    }

    // 4. Caso contrário: REGULAR (ganho entre 15.0 e 19.9 km/h ou dados utilizáveis)
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
   * (Seção 37: "Corrigir dados da passagem")
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
    val tireCalc = calculateTireDimensions(
      widthMm = correctedTireWidthMm,
      aspectRatio = correctedTireAspectRatio,
      rimInches = correctedRimInches
    )

    val rollForce = calculateRollingResistanceForce(correctedTotalMassKg, correctedCrr)
    val slopeForce = calculateSlopeForce(correctedTotalMassKg, run.slopePercentUsed)
    val efficiency = (1.0f - (correctedLossPercent / 100f)).coerceIn(0.5f, 1.0f)

    // Cálculo da janela válida de aceleração GPS para ancoramento
    val startGps = if (run.officialStartSpeedKmh > 0f) run.officialStartSpeedKmh else run.startSpeedKmh
    val maxGps = if (run.officialMaxSpeedKmh > 0f) run.officialMaxSpeedKmh else run.maximumGpsSpeedKmh
    val gpsDeltaVMps = (maxGps - startGps).coerceAtLeast(0f) / 3.6f

    val validAccelerationSamples = run.samples.filter {
      it.isValid && it.filteredSpeedKmh >= (startGps - 2f) && it.filteredSpeedKmh <= (maxGps + 1f)
    }
    val samplePool = if (validAccelerationSamples.isNotEmpty()) validAccelerationSamples else run.samples.filter { it.isValid }

    val rawSensorAccels = samplePool.map {
      if (it.rawAccelerationMps2 > 0.001f) it.rawAccelerationMps2
      else if (it.sensorAccelerationMps2 > 0.001f) it.sensorAccelerationMps2
      else it.finalAccelerationMps2
    }
    val rawAvgG = if (rawSensorAccels.isNotEmpty()) {
      rawSensorAccels.average().toFloat() / STANDARD_GRAVITY
    } else if (run.rawAverageLongitudinalG > 0f) run.rawAverageLongitudinalG else 0.18f

    val elapsedSec = if (run.elapsedSeconds > 0.1f) run.elapsedSeconds else 9.01f
    val sensorDeltaVMps = if (run.sensorDeltaVMps > 0.1f) {
      run.sensorDeltaVMps
    } else {
      rawAvgG * STANDARD_GRAVITY * elapsedSec
    }

    val normalizationFactor = if (sensorDeltaVMps > 0.1f && gpsDeltaVMps > 0.1f) {
      (gpsDeltaVMps / sensorDeltaVMps).coerceIn(0.50f, 1.50f)
    } else 1.0f

    val anchoredAvgG = rawAvgG * normalizationFactor

    val updatedSamples = run.samples.map { sample ->
      val rawA = if (sample.rawAccelerationMps2 > 0.001f) sample.rawAccelerationMps2
                 else if (sample.sensorAccelerationMps2 > 0.001f) sample.sensorAccelerationMps2
                 else sample.finalAccelerationMps2
      val aMps2 = rawA * normalizationFactor
      val g = aMps2 / STANDARD_GRAVITY
      val vMps = sample.filteredSpeedMs
      val fAero = calculateAerodynamicForce(vMps, correctedCd, correctedFrontalAreaM2, run.airDensityUsed)
      val fAccel = calculateAccelerationForce(correctedTotalMassKg, kotlin.math.max(0f, aMps2))
      val fTractive = calculateTotalTractiveForce(fAccel, rollForce, fAero, slopeForce)
      val wWatts = calculateWheelPowerWatts(fTractive, vMps)
      val sampleWheelCv = convertWattsToCv(wWatts)
      val sampleEngineCv = if (efficiency > 0f) (sampleWheelCv / efficiency).coerceAtLeast(0f) else sampleWheelCv

      val sampleRpm = calculateRpmFromSpeed(vMps, tireCalc.circumferenceM, correctedGearRatio, correctedFinalDrive)?.toInt()
      val sampleEngineTorqueKgfm = if (sampleRpm != null && sampleRpm > 500) {
        calculateTorqueKgfm(sampleEngineCv, sampleRpm.toFloat()) ?: 0f
      } else 0f
      val sampleWheelTorqueKgfm = if (sampleRpm != null && sampleRpm > 500) {
        calculateTorqueKgfm(sampleWheelCv, sampleRpm.toFloat()) ?: 0f
      } else 0f

      sample.copy(
        rawAccelerationMps2 = rawA,
        anchoredAccelerationMps2 = aMps2,
        normalizationFactorApplied = normalizationFactor,
        finalAccelerationMps2 = aMps2,
        longitudinalG = g,
        accelerationForceN = fAccel,
        aerodynamicForceN = fAero,
        rollingForceN = rollForce,
        slopeForceN = slopeForce,
        totalForceN = fTractive,
        wheelPowerWatts = wWatts,
        wheelPowerKw = wWatts / 1000f,
        wheelPowerCv = sampleWheelCv,
        enginePowerCv = sampleEngineCv,
        wheelTorqueKgfm = sampleWheelTorqueKgfm,
        engineTorqueKgfm = sampleEngineTorqueKgfm,
        wheelTorqueNm = sampleWheelTorqueKgfm * STANDARD_GRAVITY,
        engineTorqueNm = sampleEngineTorqueKgfm * STANDARD_GRAVITY,
        engineRpm = sampleRpm
      )
    }

    val sampleRpms = updatedSamples.mapNotNull { it.engineRpm }.filter { it > 500 }
    val sampleRpmSpan = if (sampleRpms.isNotEmpty()) ((sampleRpms.maxOrNull() ?: 0) - (sampleRpms.minOrNull() ?: 0)) else null
    val isRpmValid = (sampleRpms.size >= 6 && sampleRpmSpan != null && sampleRpmSpan >= 800)

    val peaks = findSustainedPeaks(updatedSamples, isRpmValid = isRpmValid)

    // Reavaliar qualidade com divergência de sensores
    val qualityEval = classifyRunQuality(
      speedGainKmh = (maxGps - startGps).coerceAtLeast(0f),
      validGpsLocationsCount = run.validGpsLocationsCount,
      elapsedSec = elapsedSec,
      lastGpsAccuracyMeters = run.gpsAccuracyMeters,
      avgSyncDiffKmh = run.averageSpeedDifferenceKmh,
      rejectionRatio = if (run.totalSamples > 0) run.rejectedSamples.toFloat() / run.totalSamples else 0.05f,
      finishReason = FinishReason.fromCode(run.finishReason),
      isPhoneStable = true,
      gearShiftDetected = false,
      finalGpsSpeedKmh = maxGps,
      startGpsSpeedKmh = startGps,
      rpmSpan = sampleRpmSpan,
      gpsFrozen = run.gpsFrozen,
      maxIntegratedSpeedKmh = run.maxIntegratedSpeedKmh,
      maxGpsSpeedKmh = maxGps,
      sensorDeltaVMps = sensorDeltaVMps,
      gpsDeltaVMps = gpsDeltaVMps
    )

    return run.copy(
      totalVehicleMassKg = correctedTotalMassKg,
      gearRatioUsed = correctedGearRatio,
      finalDriveUsed = correctedFinalDrive,
      drivetrainLossPercent = correctedLossPercent,
      cdUsed = correctedCd,
      frontalAreaUsed = correctedFrontalAreaM2,
      crrUsed = correctedCrr,
      gpsDeltaVMps = gpsDeltaVMps,
      sensorDeltaVMps = sensorDeltaVMps,
      normalizationFactor = normalizationFactor,
      rawAverageLongitudinalG = rawAvgG,
      anchoredAverageLongitudinalG = anchoredAvgG,
      wheelPowerCv = peaks.peakWheelPowerCv,
      enginePowerCv = peaks.peakEnginePowerCv,
      wheelPowerKw = peaks.peakWheelPowerCv * 0.73549875f,
      enginePowerKw = peaks.peakEnginePowerCv * 0.73549875f,
      estimatedPowerCv = peaks.peakEnginePowerCv,
      wheelTorqueKgfm = peaks.wheelTorqueKgfm,
      engineTorqueKgfm = peaks.engineTorqueKgfm,
      wheelTorqueNm = peaks.wheelTorqueKgfm * STANDARD_GRAVITY,
      engineTorqueNm = peaks.engineTorqueKgfm * STANDARD_GRAVITY,
      estimatedTorqueKgfm = peaks.engineTorqueKgfm,
      peakPowerRpm = peaks.peakPowerRpm,
      peakTorqueRpm = peaks.peakTorqueRpm,
      peakPowerSpeedKmh = peaks.peakPowerSpeedKmh,
      peakTorqueSpeedKmh = peaks.peakTorqueSpeedKmh,
      peakLongitudinalG = peaks.peakLongitudinalG,
      averageLongitudinalG = anchoredAvgG,
      quality = qualityEval.quality,
      confidenceLevel = qualityEval.confidenceLevel,
      estimatedMarginPercent = qualityEval.marginPercent,
      invalidationReason = qualityEval.invalidationReason,
      isPreliminary = qualityEval.isPreliminary,
      samples = updatedSamples
    )
  }
}
