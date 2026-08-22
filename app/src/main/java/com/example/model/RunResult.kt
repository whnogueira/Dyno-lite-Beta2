package com.example.model

import java.util.UUID

enum class FinishReason(val code: String, val displayName: String) {
  USER_STOP("USER_STOP", "Finalização manual"),
  SENSOR_DECELERATION("SENSOR_DECELERATION", "Desaceleração detectada no eixo Z"),
  GPS_DECELERATION("GPS_DECELERATION", "Queda confirmada pelo GPS"),
  TIMEOUT("TIMEOUT", "Tempo de passagem excessivo (> 25s)"),
  CANCELLED("CANCELLED", "Passagem cancelada");

  companion object {
    fun fromCode(code: String): FinishReason {
      return entries.firstOrNull { it.code == code } ?: SENSOR_DECELERATION
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
  val elapsedSeconds: Float = 0f,
  val gpsAccuracyMeters: Float = 0f,
  val totalSamples: Int = 0,
  val rejectedSamples: Int = 0,
  val validSamplesCount: Int = 0,
  val averageSamplingRateHz: Float = 0f,
  val quality: String = "BOA",
  val finishReason: String = FinishReason.SENSOR_DECELERATION.code,
  val averageSpeedDifferenceKmh: Float = 0f,
  val maximumSpeedDifferenceKmh: Float = 0f,
  val invalidationReason: String? = null,
  val appVersion: String = "0.19.1",
  val samples: List<RunSample> = emptyList()
) {
  val peakSpeedDifferenceKmh: Float
    get() = kotlin.math.abs(maximumGpsSpeedKmh - maximumCalculatedSpeedKmh)

  fun getEffectiveInvalidationReason(): String {
    if (!invalidationReason.isNullOrBlank()) return invalidationReason
    if (quality != "INVÁLIDA" && quality != "INVALID") return ""
    return when {
      elapsedSeconds < 1.5f -> "Duração do teste muito curta (${String.format(java.util.Locale.US, "%.2f", elapsedSeconds)} s) para validação."
      finishReason == FinishReason.TIMEOUT.code -> "Tempo limite de medição atingido (> 25 s)."
      peakSpeedDifferenceKmh > 10.0f -> "Divergência entre velocidade máxima GPS (${String.format(java.util.Locale.US, "%.1f", maximumGpsSpeedKmh)} km/h) e calculada (${String.format(java.util.Locale.US, "%.1f", maximumCalculatedSpeedKmh)} km/h)."
      averageSpeedDifferenceKmh > 10.0f -> "Diferença média entre GPS e aceleração inercial elevada (±${String.format(java.util.Locale.US, "%.1f", averageSpeedDifferenceKmh)} km/h)."
      maximumSpeedDifferenceKmh > 18.0f -> "Pico de divergência momentânea excessivo (${String.format(java.util.Locale.US, "%.1f", maximumSpeedDifferenceKmh)} km/h)."
      maximumGpsSpeedKmh < (runStartGpsSpeedKmh + 5f) -> "Velocidade máxima atingida insuficiente para teste de aceleração."
      else -> "Inconsistência na detecção inercial ou dados insuficientes de GPS."
    }
  }
}
