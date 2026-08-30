package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brand: String,
    val model: String,
    val year: Int,
    val curbWeightKg: Float,
    val driverWeightKg: Float,
    val additionalWeightKg: Float,
    val frontalAreaM2: Float,
    val dragCoefficientCd: Float,
    val drivetrainLossPercent: Float,
    val tireWidthMm: Int,
    val tireProfilePercent: Int,
    val tireRimInches: Int,
    val finalDriveRatio: Float,
    val gearRatiosJson: String,
    val testGearIndex: Int,
    val engineDisplacementCc: Int,
    val aspiration: String,
    val fuelType: String,
    val revLimitRpm: Int,
    val isPrimary: Boolean
)

@Entity(tableName = "run_results")
data class RunResultEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val vehicleName: String,
    val testDateTimestamp: Long,
    val peakEnginePowerCv: Float = 0f,
    val peakEnginePowerRpm: Int = 0,
    val peakEnginePowerSpeedKmh: Float = 0f,
    val peakWheelPowerCv: Float = 0f,
    val peakEngineTorqueKgm: Float = 0f,
    val peakEngineTorqueRpm: Int = 0,
    val peakLongitudinalG: Float = 0f,
    val startSpeedKmh: Float = 0f,
    val endSpeedKmh: Float = 0f,
    val testGear: Int = 3,
    val durationSeconds: Float = 0f,
    val zeroToHundredSeconds: Float? = null,
    val eightyToOneTwentySeconds: Float? = null,
    val oneHundredToTwoHundredSeconds: Float? = null,
    val quarterMileSeconds: Float? = null,
    val quarterMileSpeedKmh: Float? = null,
    val temperatureCelsius: Float = 25f,
    val pressureHpa: Float = 1013.25f,
    val saeCorrectionFactor: Float = 1f,
    val samplesJson: String = "[]",
    val qualityStatus: String = "VALID",
    val technicalFailureReason: String? = null
)

@Entity(tableName = "pending_sessions")
data class PendingSessionEntity(
    @PrimaryKey val sessionId: String,
    val vehicleId: String,
    val vehicleName: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val sampleCount: Int,
    val status: String,
    val errorMessage: String? = null,
    val errorStage: String? = null,
    val errorExceptionType: String? = null,
    val invalidField: String? = null,
    val samplesJson: String,
    val lastAttemptTimestamp: Long = System.currentTimeMillis()
)
