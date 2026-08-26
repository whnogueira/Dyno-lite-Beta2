package com.example.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Entidade da tabela "tests" conforme especificação DynoMobileDB v1.
 */
@Entity(
  tableName = "tests",
  indices = [
    Index(value = ["vehicleId"]),
    Index(value = ["status"]),
    Index(value = ["createdAt"])
  ]
)
data class TestEntity(
  @PrimaryKey
  val id: String,
  val vehicleId: String? = null,
  val name: String = "",
  val createdAt: String = currentIsoUtc(),
  val completedAt: String? = null,
  val status: String = "recording", // "recording", "completed", "cancelled", "interrupted"
  val startSpeed: Float = 0f,
  val endSpeed: Float = 0f,
  val elapsedTime: Float = 0f,
  val distance: Float = 0f,
  val maxWheelPowerCv: Float = 0f,
  val estimatedEnginePowerCv: Float = 0f,
  val maxTorqueKgfm: Float = 0f,
  val maxRpm: Int? = null,
  val maxG: Float = 0f,
  val averageGpsAccuracy: Float = 0f,
  val confidence: String = "ALTA",
  val sampleCount: Int = 0,
  val quality: String = "BOA",
  val finishReason: String? = null,
  val invalidationReason: String? = null,
  val averageSpeedDifferenceKmh: Float = 0f,
  val maximumSpeedDifferenceKmh: Float = 0f,
  val time0to60Kmh: Float? = null,
  val time0to100Kmh: Float? = null,
  val time60to100Kmh: Float? = null,
  val time80to120Kmh: Float? = null,
  val time100to200Kmh: Float? = null,
  val time60Feet: Float? = null,
  val time100M: Float? = null,
  val time201M: Float? = null,
  val time402M: Float? = null,
  val appVersion: String = "0.20.0",
  val configurationSnapshot: String = "{}"
)

/**
 * Entidade da tabela "testSamples" indexada por testId.
 */
@Entity(
  tableName = "testSamples",
  indices = [
    Index(value = ["testId"]),
    Index(value = ["testId", "sampleIndex"])
  ]
)
data class TestSampleEntity(
  @PrimaryKey
  val id: String,
  val testId: String,
  val sampleIndex: Int = 0,
  val timestamp: String = currentIsoUtc(),
  val elapsedTimeMs: Long = 0L,
  val speed: Float = 0f,
  val filteredSpeed: Float = 0f,
  val acceleration: Float = 0f,
  val filteredAccelerationZ: Float = 0f,
  val correctedAccelerationZ: Float = 0f,
  val longitudinalG: Float = 0f,
  val rpm: Int? = null,
  val distance: Float = 0f,
  val wheelPowerCv: Float = 0f,
  val enginePowerCv: Float = 0f,
  val torqueKgfm: Float = 0f,
  val gpsAccuracy: Float = 0f,
  val gyroMagnitude: Float = 0f,
  val confidence: String = "ALTA",
  val isValid: Boolean = true,
  val rejectionReason: String? = null
)

/**
 * Entidade da tabela "vehicles".
 */
@Entity(tableName = "vehicles")
data class VehicleEntity(
  @PrimaryKey
  val id: String,
  val name: String = "",
  val manufacturer: String = "",
  val model: String = "",
  val year: Int = 2010,
  val isPrimary: Boolean = false,
  val fullJson: String = "{}"
)

/**
 * Entidade da tabela "simulations".
 */
@Entity(tableName = "simulations")
data class SimulationEntity(
  @PrimaryKey
  val id: String,
  val vehicleId: String? = null,
  val name: String = "",
  val createdAt: String = currentIsoUtc(),
  val powerGainCv: Float = 0f,
  val torqueGainKgfm: Float = 0f,
  val payloadJson: String = "{}"
)

/**
 * Entidade da tabela "settings".
 */
@Entity(tableName = "settings")
data class SettingEntity(
  @PrimaryKey
  val key: String,
  val value: String = ""
)

fun currentIsoUtc(): String {
  val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
  sdf.timeZone = TimeZone.getTimeZone("UTC")
  return sdf.format(Date())
}

fun Long.toIsoUtc(): String {
  val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
  sdf.timeZone = TimeZone.getTimeZone("UTC")
  return sdf.format(Date(this))
}

fun String.isoToTimestampMs(): Long {
  return try {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    sdf.parse(this)?.time ?: System.currentTimeMillis()
  } catch (e: Exception) {
    try {
      val sdf2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
      sdf2.timeZone = TimeZone.getTimeZone("UTC")
      sdf2.parse(this)?.time ?: System.currentTimeMillis()
    } catch (e2: Exception) {
      System.currentTimeMillis()
    }
  }
}
