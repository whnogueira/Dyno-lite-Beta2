package com.example.model

import java.util.UUID

enum class FinishReason(val code: String, val displayName: String) {
  USER_STOP("USER_STOP", "Teste encerrado manualmente"),
  SENSOR_DECELERATION("SENSOR_DECELERATION", "Fim da aceleração confirmado"),
  GPS_DECELERATION("GPS_DECELERATION", "Desaceleração confirmada pelo GPS"),
  GPS_LOST("GPS_LOST", "Sinal GPS perdido"),
  TIMEOUT("TIMEOUT", "Tempo máximo atingido"),
  CANCELLED("CANCELLED", "Passagem cancelada");

  companion object {
    fun fromCode(code: String): FinishReason {
      return entries.firstOrNull { it.code == code } ?: GPS_DECELERATION
    }
  }
}

data class RunResult(
  val id: String = UUID.randomUUID().toString(),
  val timestamp: Long = System.currentTimeMillis(),
  val vehicleId: String? = null,
  val vehicleName: String = "",
  val runStartCalculatedSpeedKmh: Float = 40.0f,
  val runStartGpsSpeedKmh: Float = 0f,
  val maximumGpsSpeedKmh: Float = 0f,
  val maximumCalculatedSpeedKmh: Float = 40.0f,
  val finalGpsSpeedKmh: Float = 0f,
  val finalCalculatedSpeedKmh: Float = 0f,
  val speedGainKmh: Float = 0f,
  val estimatedPowerCv: Float = 0f,
  val estimatedTorqueKgfm: Float = 0f,
  val wheelPowerCv: Float = 0f,
  val enginePowerCv: Float = 0f,
  val wheelTorqueKgfm: Float = 0f,
  val engineTorqueKgfm: Float = 0f,
  val peakLongitudinalG: Float = 0f,
  val averageLongitudinalG: Float = 0f,
  val peakPowerRpm: Int? = null,
  val peakTorqueRpm: Int? = null,
  val peakPowerSpeedKmh: Float = 0f,
  val peakTorqueSpeedKmh: Float = 0f,
  val totalVehicleMassKg: Float = 0f,
  val drivetrainLossPercent: Float = 15f,
  val estimatedMarginPercent: Float = 10f,
  val gearUsed: String = "2ª",
  val isAerodynamicsEstimated: Boolean = true,
  val elapsedSeconds: Float = 0f,
  val gpsAccuracyMeters: Float = 0f,
  val totalSamples: Int = 0,
  val rejectedSamples: Int = 0,
  val validSamplesCount: Int = 0,
  val validGpsLocationsCount: Int = 0,
  val averageSamplingRateHz: Float = 0f,
  val averageGpsFrequencyHz: Float = 0f,
  val quality: String = "BOA",
  val finishReason: String = FinishReason.GPS_DECELERATION.code,
  val averageSpeedDifferenceKmh: Float = 0f,
  val maximumSpeedDifferenceKmh: Float = 0f,
  val invalidationReason: String? = null,
  val appVersion: String = "0.20.0",
  val samples: List<RunSample> = emptyList()
) {
  val peakSpeedDifferenceKmh: Float
    get() = kotlin.math.abs(maximumGpsSpeedKmh - maximumCalculatedSpeedKmh)

  fun getEffectiveInvalidationReason(): String {
    if (!invalidationReason.isNullOrBlank()) return invalidationReason
    if (quality != "INVÁLIDA" && quality != "INVALID") return ""
    return when {
      elapsedSeconds < 4.0f -> "Duração do teste insuficiente (${String.format(java.util.Locale.US, "%.2f", elapsedSeconds)} s < 4.00 s) para validação."
      validGpsLocationsCount < 4 -> "Poucas leituras de GPS válidas ($validGpsLocationsCount < 4) durante a medição."
      finishReason == FinishReason.TIMEOUT.code -> "Tempo limite de medição atingido (> 25 s)."
      maximumGpsSpeedKmh < (runStartGpsSpeedKmh + 10f) -> "Ganho de velocidade GPS insuficiente (${String.format(java.util.Locale.US, "%.1f", maximumGpsSpeedKmh - runStartGpsSpeedKmh)} km/h < 10.0 km/h)."
      else -> "Sinal GPS insuficiente, perda de dados ou movimentação grave do celular."
    }
  }
}
