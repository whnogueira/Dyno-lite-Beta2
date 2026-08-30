package com.example.model

data class RunSample(
    val timestampNs: Long = 0L,
    val elapsedSeconds: Float = 0f,
    val speedKmh: Float = 0f,
    val accelerationMps2: Float = 0f,
    val longitudinalG: Float = 0f,
    val estimatedRpm: Int = 0,
    val wheelPowerCv: Float = 0f,
    val enginePowerCv: Float = 0f,
    val wheelTorqueKgm: Float = 0f,
    val engineTorqueKgm: Float = 0f,
    val aeroLossCv: Float = 0f,
    val rollLossCv: Float = 0f,
    val drivetrainLossCv: Float = 0f,
    val inertialLossCv: Float = 0f,
    val gpsAccuracyMeters: Float = 0f,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
