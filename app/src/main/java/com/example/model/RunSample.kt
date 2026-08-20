package com.example.model

data class RunSample(
  val elapsedTimeMs: Long = 0L,
  val filteredAccelerationZ: Float = 0f,
  val correctedAccelerationZ: Float = 0f,
  val gpsSpeedKmh: Float = 0f,
  val calculatedSpeedKmh: Float = 0f,
  val speedDifferenceKmh: Float = 0f,
  val gpsAccuracyMeters: Float = 0f,
  val gyroMagnitude: Float = 0f,
  val isValid: Boolean = true,
  val rejectionReason: String? = null
)
