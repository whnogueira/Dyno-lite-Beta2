package com.example.model

data class RunSample(
  val elapsedTimeMs: Long = 0L,
  val filteredAccelerationZ: Float = 0f,
  val correctedAccelerationZ: Float = 0f,
  val longitudinalG: Float = 0f,
  val gpsSpeedKmh: Float = 0f,
  val calculatedSpeedKmh: Float = 0f,
  val speedDifferenceKmh: Float = 0f,
  val gpsAccuracyMeters: Float = 0f,
  val gyroMagnitude: Float = 0f,
  val wheelPowerCv: Float = 0f,
  val enginePowerCv: Float = 0f,
  val wheelTorqueKgfm: Float = 0f,
  val engineTorqueKgfm: Float = 0f,
  val engineRpm: Int? = null,
  val isValid: Boolean = true,
  val rejectionReason: String? = null
)
