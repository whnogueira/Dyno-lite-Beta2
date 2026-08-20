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
  val runStartCalculatedSpeedKmh: Float = 30.0f,
  val runStartGpsSpeedKmh: Float = 0f,
  val maximumGpsSpeedKmh: Float = 0f,
  val maximumCalculatedSpeedKmh: Float = 30.0f,
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
  val appVersion: String = "0.15.0",
  val samples: List<RunSample> = emptyList()
)
