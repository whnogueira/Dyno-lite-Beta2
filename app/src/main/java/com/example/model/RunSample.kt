package com.example.model

data class RunSample(
  val timestampMs: Long = 0L,
  val elapsedTimeMs: Long = 0L,
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
  val rawGpsSpeedMs: Float = 0f,
  val rawGpsSpeedKmh: Float = 0f,
  val filteredSpeedMs: Float = 0f,
  val filteredSpeedKmh: Float = 0f,
  val gpsSpeedKmh: Float = 0f, // Compatibilidade com código legado
  val calculatedSpeedKmh: Float = 0f, // Compatibilidade
  val speedDifferenceKmh: Float = 0f,
  val gpsAccuracyMeters: Float = 0f,
  val gpsSpeedAccuracyMps: Float = 0f,
  val gpsAccelerationMps2: Float = 0f,
  val sensorAccelerationMps2: Float = 0f,
  val finalAccelerationMps2: Float = 0f,
  val filteredAccelerationZ: Float = 0f, // Compatibilidade
  val correctedAccelerationZ: Float = 0f, // Compatibilidade
  val longitudinalG: Float = 0f,
  val gyroMagnitude: Float = 0f,
  val distanceMeters: Float = 0f,
  val engineRpm: Int? = null,
  val accelerationForceN: Float = 0f,
  val aerodynamicForceN: Float = 0f,
  val rollingForceN: Float = 0f,
  val slopeForceN: Float = 0f,
  val totalForceN: Float = 0f,
  val wheelPowerWatts: Float = 0f,
  val wheelPowerKw: Float = 0f,
  val wheelPowerCv: Float = 0f,
  val enginePowerCv: Float = 0f,
  val wheelTorqueKgfm: Float = 0f,
  val engineTorqueKgfm: Float = 0f,
  val wheelTorqueNm: Float = 0f,
  val engineTorqueNm: Float = 0f,
  val confidenceLevel: String = "ALTA", // "ALTA", "MEDIA", "BAIXA"
  val isValid: Boolean = true,
  val rejectionReason: String? = null
)
