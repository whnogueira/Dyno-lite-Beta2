package com.example.model

import java.util.UUID

enum class SimulationSourceType {
  REAL_RUN,
  VEHICLE_GARAGE,
  MANUAL_INPUT
}

enum class DrivetrainType(val displayName: String, val weightOnDriveAxlePercent: Float, val defaultLossPercent: Float) {
  FWD("Dianteira (FWD)", 0.62f, 12f),
  RWD("Traseira (RWD)", 0.50f, 15f),
  AWD("Integral (AWD/4x4)", 1.00f, 20f);

  companion object {
    fun fromString(value: String): DrivetrainType {
      return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.displayName.contains(value, ignoreCase = true) } ?: FWD
    }
  }
}

enum class TireGripType(val displayName: String, val frictionCoefficient: Float) {
  STREET_DRY("Pneu comum seco (μ 0.80)", 0.80f),
  SPORT_DRY("Pneu esportivo seco (μ 0.95)", 0.95f),
  SEMI_SLICK("Semislick (μ 1.10)", 1.10f),
  WET_TRACK("Pista molhada (μ 0.55)", 0.55f),
  DIRT_GRAVEL("Terra / Cascalho (μ 0.45)", 0.45f);

  companion object {
    fun fromCoefficient(mu: Float): TireGripType {
      return entries.minByOrNull { kotlin.math.abs(it.frictionCoefficient - mu) } ?: STREET_DRY
    }
  }
}

enum class ShiftSpeedType(val displayName: String, val shiftTimeSeconds: Float) {
  MANUAL_COMMON("Manual comum (0.50s)", 0.50f),
  MANUAL_FAST("Manual rápida (0.30s)", 0.30f),
  AUTOMATIC("Automático convencional (0.35s)", 0.35f),
  DUAL_CLUTCH("Dupla embreagem / DSG (0.15s)", 0.15f);

  companion object {
    fun fromSeconds(sec: Float): ShiftSpeedType {
      return entries.minByOrNull { kotlin.math.abs(it.shiftTimeSeconds - sec) } ?: MANUAL_COMMON
    }
  }
}

enum class SimulationConfidence(val title: String, val description: String) {
  HIGH("ALTA CONFIANÇA", "Baseada em passagem real de boa qualidade e alterações mecânicas de peso, pneu ou relação."),
  MEDIUM("MÉDIA CONFIANÇA", "Baseada em curva real medida, porém com alteração estimada de potência/turbo."),
  LOW("BAIXA CONFIANÇA", "Criada a partir de valores de potência e torque informados manualmente com curva sintética.")
}

data class SimulationConfig(
  val label: String = "Configuração",
  val vehicleName: String = "Veículo",
  // Massa
  val vehicleCurbWeightKg: Float = 1200f,
  val driverWeightKg: Float = 80f,
  val additionalWeightKg: Float = 0f, // passageiros, carga, som, gnv
  // Motor
  val enginePowerCv: Float = 130f,
  val engineTorqueKgfm: Float = 18.0f,
  val peakPowerRpm: Int = 5800,
  val peakTorqueRpm: Int = 3800,
  val maxRpm: Int = 6500,
  // Câmbio e Transmissão
  val gearRatios: List<Float> = listOf(3.73f, 2.05f, 1.36f, 1.03f, 0.82f),
  val finalDriveRatio: Float = 4.19f,
  val drivetrainLossPercent: Float = 12f,
  val drivetrainType: DrivetrainType = DrivetrainType.FWD,
  val shiftTimeSeconds: Float = 0.50f,
  // Pneus
  val tireWidthMm: Int = 195,
  val tireAspectRatio: Int = 55,
  val rimDiameterInches: Int = 15,
  // Aerodinâmica e Rolamento
  val cd: Float = 0.33f,
  val frontalAreaM2: Float = 2.10f,
  val crr: Float = 0.015f,
  val airDensityKgM3: Float = 1.225f,
  // Pista e Ambiente
  val tireGripMu: Float = 0.80f,
  val trackSlopePercent: Float = 0f,
  val headwindSpeedKmh: Float = 0f,
  // Turbo simulado
  val isTurboSimulated: Boolean = false,
  val turboBoostBar: Float = 0.0f,
  val turboEfficiency: Float = 0.85f,
  // Curva de potência de referência (RPM -> cv nas rodas)
  val customPowerCurvePoints: List<Pair<Int, Float>> = emptyList(),
  val isUsingRealRunCurve: Boolean = false,
  val baseRunId: String? = null
) {
  val totalWeightKg: Float
    get() = (vehicleCurbWeightKg + driverWeightKg + additionalWeightKg).coerceAtLeast(300f)

  val tireDynamicRadiusM: Float
    get() {
      val tire = VehicleCalculations.calculateTireDimensions(tireWidthMm, tireAspectRatio, rimDiameterInches)
      return (tire.totalDiameterMm / 2000.0).toFloat() * 0.97f
    }

  val tireCircumferenceM: Float
    get() = VehicleCalculations.calculateTireDimensions(tireWidthMm, tireAspectRatio, rimDiameterInches).circumferenceM.toFloat()

  val weightToPowerRatioKgCv: Float
    get() = if (enginePowerCv > 0f) totalWeightKg / enginePowerCv else 0f

  val estimatedWheelPowerCv: Float
    get() = enginePowerCv * (1f - (drivetrainLossPercent / 100f).coerceIn(0f, 0.40f))

  val estimatedWheelTorqueKgfm: Float
    get() = engineTorqueKgfm * (1f - (drivetrainLossPercent / 100f).coerceIn(0f, 0.40f))

  companion object {
    fun default(): SimulationConfig = SimulationConfig()
  }
}

typealias SimulationConfiguration = SimulationConfig
typealias Vehicle = VehicleProfile

data class GearSpeedEntry(
  val gearIndex: Int,
  val gearName: String,
  val ratio: Float,
  val speedAt2000RpmKmh: Float,
  val speedAt3000RpmKmh: Float,
  val speedAt4000RpmKmh: Float,
  val speedAt5000RpmKmh: Float,
  val speedAtCutoffKmh: Float,
  val rpmAfterShift: Int?
)

data class GearShiftPoint(
  val fromGear: Int,
  val toGear: Int,
  val recommendedShiftRpm: Int,
  val shiftSpeedKmh: Float,
  val rpmAfterShift: Int,
  val explanation: String
)

data class SimulationStepPoint(
  val timeSec: Float,
  val speedKmh: Float,
  val distanceMeters: Float,
  val currentGear: Int,
  val engineRpm: Int,
  val enginePowerCv: Float,
  val wheelPowerCv: Float,
  val engineTorqueKgfm: Float,
  val wheelTractiveForceN: Float,
  val effectiveForceN: Float,
  val aeroForceN: Float,
  val rollForceN: Float,
  val longitudinalG: Float,
  val isShifting: Boolean,
  val isTractionLimited: Boolean
)

data class SimulationResult(
  val config: SimulationConfig,
  val confidence: SimulationConfidence,
  val points: List<SimulationStepPoint> = emptyList(),
  // Métricas de Tempo e Velocidade
  val time0to60Kmh: Float? = null,
  val time0to100Kmh: Float? = null,
  val time60to100Kmh: Float? = null,
  val time80to120Kmh: Float? = null,
  val time100to200Kmh: Float? = null,
  // Distâncias
  val time100m: Float? = null,
  val speedAt100mKmh: Float? = null,
  val time201m: Float? = null,
  val speedAt201mKmh: Float? = null,
  val time402m: Float? = null,
  val speedAt402mKmh: Float? = null,
  // Máximos
  val topSpeedKmh: Float = 0f,
  val peakLongitudinalG: Float = 0f,
  val gearSpeeds: List<GearSpeedEntry> = emptyList(),
  val optimalShiftPoints: List<GearShiftPoint> = emptyList(),
  val hasTractionLossWarning: Boolean = false,
  val powerTorqueRpmCurve: List<Triple<Int, Float, Float>> = emptyList() // RPM, PowerCv, TorqueKgfm
)

data class SavedSimulationProject(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val createdAt: Long = System.currentTimeMillis(),
  val vehicleName: String,
  val baseRunId: String? = null,
  val baseRunDateFormatted: String? = null,
  val notes: String = "",
  val configA: SimulationConfig,
  val configB: SimulationConfig,
  val confidence: SimulationConfidence = SimulationConfidence.MEDIUM
)
