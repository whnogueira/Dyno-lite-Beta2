package com.example.model

import java.util.UUID

enum class FinishReason(val code: String, val displayName: String) {
  USER_STOP("USER_STOP", "Teste encerrado manualmente"),
  SENSOR_DECELERATION("SENSOR_DECELERATION", "Fim da aceleração confirmado"),
  GPS_DECELERATION("GPS_DECELERATION", "Desaceleração confirmada pelo GPS"),
  TARGET_REACHED("TARGET_REACHED", "Velocidade/RPM alvo atingida"),
  GPS_LOST("GPS_LOST", "Sinal GPS perdido"),
  TIMEOUT("TIMEOUT", "Tempo máximo atingido"),
  CANCELLED("CANCELLED", "Passagem cancelada"),
  GEAR_SHIFT("GEAR_SHIFT", "Troca de marcha detectada. A passagem foi encerrada e poderá ser considerada incompleta."),
  CLUTCH_DISENGAGED("CLUTCH_DISENGAGED", "Acionamento de embreagem detectado"),
  PREMATURE_TERMINATION("PREMATURE_TERMINATION", "Aceleração interrompida antes da faixa necessária");

  companion object {
    fun fromCode(code: String): FinishReason {
      return entries.firstOrNull { it.code == code } ?: GPS_DECELERATION
    }
  }
}

data class UniqueGpsFix(
  val elapsedRealtimeNanos: Long = 0L,
  val timestamp: Long = 0L,
  val speedKmh: Float = 0f,
  val speedAccuracyMetersPerSecond: Float = 0f,
  val accuracyMeters: Float = 0f,
  val ageMillis: Long = 0L,
  val provider: String = "gps",
  val hasSpeed: Boolean = true,
  val isMock: Boolean = false,
  val speedDifferenceKmh: Float = 0f,
  val intervalSinceLastFixMs: Long = 0L
)

data class RunResult(
  val id: String = UUID.randomUUID().toString(),
  val timestamp: Long = System.currentTimeMillis(),
  val vehicleId: String? = null,
  val vehicleName: String = "",
  val officialStartSpeedKmh: Float = 0f,
  val officialMaxSpeedKmh: Float = 0f,
  val officialEndSpeedKmh: Float = 0f,
  val officialSpeedGainKmh: Float = 0f,
  val runStartCalculatedSpeedKmh: Float = 0f,
  val runStartGpsSpeedKmh: Float = 0f,
  val startSpeedKmh: Float = 0f,
  val maximumGpsSpeedKmh: Float = 0f,
  val maximumCalculatedSpeedKmh: Float = 0f,
  val maxIntegratedSpeedKmh: Float = 0f,
  val finalGpsSpeedKmh: Float = 0f,
  val finalCalculatedSpeedKmh: Float = 0f,
  val finalIntegratedSpeedKmh: Float = 0f,
  val finalSpeedKmh: Float = 0f,
  val speedGainKmh: Float = 0f,
  val totalDistanceMeters: Float = 0f,
  val estimatedPowerCv: Float = 0f,
  val estimatedTorqueKgfm: Float = 0f,
  val wheelPowerCv: Float = 0f, // Potência nas rodas (cv)
  val enginePowerCv: Float = 0f, // Potência estimada no motor (cv)
  val wheelPowerKw: Float = 0f, // Potência nas rodas (kW)
  val enginePowerKw: Float = 0f, // Potência estimada no motor (kW)
  val wheelTorqueKgfm: Float = 0f, // Torque nas rodas (kgfm)
  val engineTorqueKgfm: Float = 0f, // Torque estimado no motor (kgfm)
  val wheelTorqueNm: Float = 0f, // Torque nas rodas (Nm)
  val engineTorqueNm: Float = 0f, // Torque estimado no motor (Nm)
  val peakLongitudinalG: Float = 0f,
  val averageLongitudinalG: Float = 0f,
  val peakPowerRpm: Int? = null,
  val peakTorqueRpm: Int? = null,
  val peakPowerSpeedKmh: Float = 0f,
  val peakTorqueSpeedKmh: Float = 0f,
  val totalVehicleMassKg: Float = 0f,
  val curbWeightKg: Float = 0f,
  val driverWeightKg: Float = 0f,
  val passengerCount: Int = 0,
  val passengerWeightKg: Float = 0f,
  val additionalWeightKg: Float = 0f,
  val soundSystemWeightKg: Float = 0f,
  val cngWeightKg: Float = 0f,
  val fuelAdjustmentKg: Float = 0f,
  val tireWidthMm: Int = 195,
  val tireAspectRatio: Int = 55,
  val rimInches: Int = 15,
  val isRecalculated: Boolean = false,
  val revisionNumber: Int = 1,
  val parentRunId: String? = null,
  val recalculationReason: String? = null,
  val recalculationNote: String? = null,
  val previousConfigurationJson: String? = null,
  val previousCalculatedResultJson: String? = null,
  val drivetrainLossPercent: Float = 12f,
  val estimatedMarginPercent: Float = 10f,
  val gearUsed: String = "2ª",
  val gearIndexUsed: Int = 1,
  val gearRatioUsed: Float = 1.95f,
  val finalDriveUsed: Float = 4.10f,
  val gpsDeltaVMps: Float = 0f,
  val sensorDeltaVMps: Float = 0f,
  val normalizationFactor: Float = 1.0f,
  val rawAverageLongitudinalG: Float = 0f,
  val anchoredAverageLongitudinalG: Float = 0f,
  val isAerodynamicsEstimated: Boolean = true,
  val cdUsed: Float = 0.34f,
  val frontalAreaUsed: Float = 2.10f,
  val crrUsed: Float = 0.015f,
  val airDensityUsed: Float = 1.225f,
  val slopeModeUsed: String = "IGNORE",
  val slopePercentUsed: Float = 0f,
  val confidenceLevel: String = "ALTA", // "ALTA", "MEDIA", "BAIXA"
  val elapsedSeconds: Float = 0f,
  val gpsAccuracyMeters: Float = 0f,
  val averageGpsAccuracyMeters: Float = 0f,
  val totalSamples: Int = 0,
  val rejectedSamples: Int = 0,
  val validSamplesCount: Int = 0,
  val validGpsLocationsCount: Int = 0,
  val locationCallbackCount: Int = 0,
  val uniqueGpsFixCount: Int = 0,
  val gpsSpeedChangeCount: Int = 0,
  val sensorSampleCount: Int = 0,
  val maxGpsIntervalMs: Long = 0L,
  val maxGpsAgeMs: Long = 0L,
  val gpsFrozen: Boolean = false,
  val isPreliminary: Boolean = false,
  val averageSamplingRateHz: Float = 0f,
  val averageGpsFrequencyHz: Float = 0f,
  val quality: String = "BOA",
  val finishReason: String = FinishReason.GPS_DECELERATION.code,
  val averageSpeedDifferenceKmh: Float = 0f,
  val maximumSpeedDifferenceKmh: Float = 0f,
  val invalidationReason: String? = null,
  val appVersion: String = "0.20.0",
  val testMode: String = "DYNO", // "DYNO" ou "ACCELERATION"
  val targetStartSpeedKmh: Float = 0f,
  val targetEndSpeedKmh: Float = 0f,
  val accelRangeLabel: String = "",
  val gearShiftCount: Int = 0,
  val speedUnit: String = "km/h",
  val estimatedMarginSeconds: Float = 0.08f,
  val accelerationSplits: List<AccelerationSplit> = emptyList(),
  // Tempos de aceleração (splits de velocidade)
  val time0to60Kmh: Float? = null,
  val time0to100Kmh: Float? = null,
  val time60to100Kmh: Float? = null,
  val time80to120Kmh: Float? = null,
  val time100to200Kmh: Float? = null,
  // Tempos de distância (splits de distância)
  val time60Feet: Float? = null, // 18.288 m
  val time100M: Float? = null,
  val time201M: Float? = null, // 1/8 de milha
  val time402M: Float? = null, // 1/4 de milha
  val samples: List<RunSample> = emptyList(),
  val uniqueGpsFixes: List<UniqueGpsFix> = emptyList()
) {
  val peakSpeedDifferenceKmh: Float
    get() = kotlin.math.abs(maximumGpsSpeedKmh - maximumCalculatedSpeedKmh)

  val peakWheelPowerCv: Float
    get() = wheelPowerCv

  val peakEnginePowerCv: Float
    get() = enginePowerCv

  val peakWheelTorqueKgfm: Float
    get() = wheelTorqueKgfm

  val peakEngineTorqueKgfm: Float
    get() = engineTorqueKgfm

  val peakWheelTorqueNm: Float
    get() = if (wheelTorqueNm > 0f) wheelTorqueNm else wheelTorqueKgfm * 9.80665f

  val peakEngineTorqueNm: Float
    get() = if (engineTorqueNm > 0f) engineTorqueNm else engineTorqueKgfm * 9.80665f

  fun getEffectiveInvalidationReason(): String {
    if (!invalidationReason.isNullOrBlank()) return invalidationReason
    if (quality != "INVÁLIDA" && quality != "INVALID") return ""
    return when {
      elapsedSeconds < 2.5f -> "Duração do teste insuficiente (${String.format(java.util.Locale.US, "%.2f", elapsedSeconds)} s < 2.50 s) para validação."
      validGpsLocationsCount < 3 -> "Poucas leituras de GPS válidas ($validGpsLocationsCount < 3) durante a medição."
      finishReason == FinishReason.TIMEOUT.code -> "Tempo limite de medição atingido (> 25 s)."
      maximumGpsSpeedKmh < (runStartGpsSpeedKmh + 8f) -> "Ganho de velocidade GPS insuficiente (${String.format(java.util.Locale.US, "%.1f", maximumGpsSpeedKmh - runStartGpsSpeedKmh)} km/h < 8.0 km/h)."
      else -> "Sinal GPS insuficiente, perda de dados ou condições de pista desfavoráveis."
    }
  }
}
