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
    passengerWeightKg: Float,
    cargoWeightKg: Float,
    audioWeightKg: Float,
    gnvWeightKg: Float,
    otherWeightKg: Float,
    removedWeightKg: Float,
    measuredTotalWeightKg: Float? = null,
    useMeasuredWeight: Boolean = false
  ): Float {
    if (useMeasuredWeight && measuredTotalWeightKg != null && measuredTotalWeightKg > 0f) {
      return measuredTotalWeightKg
    }
    val sum = curbWeightKg + driverWeightKg + passengerWeightKg + cargoWeightKg +
      audioWeightKg + gnvWeightKg + otherWeightKg - removedWeightKg
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

  fun calculateTorqueKgfm(powerCv: Float, rpm: Float): Float? {
    if (rpm <= 500f || powerCv <= 0f) return null
    // T (kgf.m) = (P (cv) * 716.2) / RPM
    val torque = (powerCv * 716.2f) / rpm
    return if (torque in 0.1f..1000f) torque else null
  }

  fun calculateTorqueNm(torqueKgfm: Float): Float {
    return torqueKgfm * STANDARD_GRAVITY
  }

  fun calculateTorqueNmFromPower(powerWatts: Float, rpm: Float): Float? {
    if (rpm <= 500f || powerWatts <= 0f) return null
    val omega = (rpm * PI / 30.0).toFloat()
    val torqueNm = powerWatts / omega
    return if (torqueNm in 1f..10000f) torqueNm else null
  }
}
