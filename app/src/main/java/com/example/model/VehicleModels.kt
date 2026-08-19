package com.example.model

import java.util.UUID
import kotlin.math.PI

enum class AudioWeightPreset(
  val label: String,
  val estimatedWeightKg: Float,
  val minWeightKg: Float,
  val maxWeightKg: Float
) {
  NONE("Não, somente som original", 0f, 0f, 0f),
  LIGHT("Som leve (módulozinho / caixa selada)", 18f, 10f, 25f),
  MEDIUM("Som médio (caixa dutada + amplificador)", 43f, 25f, 60f),
  HEAVY("Som pesado (subwoofers potentes / baterias)", 90f, 60f, 120f),
  PAREDAO("Projeto de som / Paredão", 0f, 0f, 0f),
  CUSTOM("Informar peso exato", 0f, 0f, 0f)
}

enum class WeightConfidence(
  val label: String,
  val description: String
) {
  HIGH(
    "Alta Confiança",
    "Peso verificado diretamente em balança."
  ),
  GOOD(
    "Boa Confiança",
    "Veículo conhecido com dados de fábrica e sem grandes pesos estimados."
  ),
  ESTIMATED(
    "Estimada",
    "Parte do peso foi estimada. O resultado da potência terá uma margem maior."
  )
}

data class TransmissionProfile(
  val id: String,
  val manufacturer: String,
  val family: String,
  val code: String,
  val displayName: String,
  val gearRatios: List<Float>,
  val finalDrive: Float,
  val compatibleVehicleIds: List<String> = emptyList(),
  val isCustom: Boolean = false
)

data class VehicleProfile(
  val id: String = UUID.randomUUID().toString(),
  val manufacturer: String,
  val model: String,
  val year: Int,
  val version: String = "",
  val engine: String = "",
  val displacement: String = "",
  val factoryPowerCv: Float? = null,
  val factoryTorqueKgf: Float? = null,
  val curbWeightKg: Float = 1000f,
  val drivetrain: String = "Dianteira",
  val transmissionId: String? = null,
  val customTransmissionName: String? = null,
  val tireWidthMm: Int = 185,
  val tireAspectRatio: Int = 70,
  val wheelDiameterInches: Int = 14,
  val driverWeightKg: Float = 0f,
  val passengerWeightKg: Float = 0f,
  val cargoWeightKg: Float = 0f,
  val audioPreset: AudioWeightPreset = AudioWeightPreset.NONE,
  val audioWeightKg: Float = 0f,
  val gnvWeightKg: Float = 0f,
  val otherWeightKg: Float = 0f,
  val removedWeightKg: Float = 0f,
  val measuredTotalWeightKg: Float? = null,
  val useMeasuredWeight: Boolean = false,
  val isPrimary: Boolean = false,
  val isCustom: Boolean = false
)

data class TireCalculation(
  val lateralHeightMm: Double,
  val rimDiameterMm: Double,
  val totalDiameterMm: Double,
  val circumferenceM: Double,
  val formattedMeasure: String
)

object VehicleCalculations {
  fun calculateTireDimensions(widthMm: Int, aspectRatio: Int, rimInches: Int): TireCalculation {
    val lateralHeight = widthMm * (aspectRatio / 100.0)
    val rimDiameter = rimInches * 25.4
    val totalDiameter = rimDiameter + 2.0 * lateralHeight
    val circumference = (PI * totalDiameter) / 1000.0

    return TireCalculation(
      lateralHeightMm = lateralHeight,
      rimDiameterMm = rimDiameter,
      totalDiameterMm = totalDiameter,
      circumferenceM = circumference,
      formattedMeasure = "$widthMm/${aspectRatio} R$rimInches"
    )
  }

  fun calculateTotalWeight(
    curbWeightKg: Float,
    driverWeightKg: Float,
    passengerWeightKg: Float,
    cargoWeightKg: Float,
    audioWeightKg: Float,
    gnvWeightKg: Float,
    otherWeightKg: Float,
    removedWeightKg: Float,
    measuredTotalWeightKg: Float? = null,
    useMeasuredWeight: Boolean = false
  ): Float {
    if (useMeasuredWeight && measuredTotalWeightKg != null && measuredTotalWeightKg > 0f) {
      return measuredTotalWeightKg
    }
    val sum = curbWeightKg + driverWeightKg + passengerWeightKg + cargoWeightKg +
      audioWeightKg + gnvWeightKg + otherWeightKg - removedWeightKg
    return sum.coerceAtLeast(0f)
  }

  fun evaluateWeightConfidence(
    useMeasuredWeight: Boolean,
    audioPreset: AudioWeightPreset,
    hasGnv: Boolean,
    hasCargo: Boolean
  ): WeightConfidence {
    if (useMeasuredWeight) return WeightConfidence.HIGH
    if (audioPreset == AudioWeightPreset.PAREDAO ||
      audioPreset == AudioWeightPreset.HEAVY ||
      audioPreset == AudioWeightPreset.CUSTOM ||
      hasGnv || hasCargo) {
      return WeightConfidence.ESTIMATED
    }
    return WeightConfidence.GOOD
  }
}
