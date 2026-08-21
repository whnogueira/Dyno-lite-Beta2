package com.example.model

import java.util.UUID

enum class VerificationStatus(
  val label: String,
  val userMessage: String
) {
  VERIFIED(
    "Dados conferidos",
    "Informações verificadas de manuais e fichas técnicas oficiais."
  ),
  PARTIAL(
    "Dados parciais",
    "Algumas informações precisam ser confirmadas."
  ),
  UNVERIFIED(
    "Não verificado",
    "Utilize estes dados apenas como referência."
  );

  companion object {
    fun fromString(value: String?): VerificationStatus {
      return when (value?.uppercase()?.trim()) {
        "VERIFIED" -> VERIFIED
        "PARTIAL" -> PARTIAL
        "UNVERIFIED" -> UNVERIFIED
        else -> UNVERIFIED
      }
    }
  }
}

enum class DynoCompatibility(
  val label: String,
  val note: String
) {
  RECOMMENDED(
    "Recomendado",
    "Transmissão adequada para passagens em dinamômetro e testes em pista."
  ),
  MANUAL_MODE_REQUIRED(
    "Modo manual obrigatório",
    "Trave a marcha selecionada no modo manual/Tiptronic para evitar reduções involuntárias (kickdown)."
  ),
  NOT_RECOMMENDED(
    "Não recomendado",
    "Transmissão propensa a escorregamento ou trocas automáticas durante aceleração plena."
  ),
  UNKNOWN(
    "Verificar compatibilidade",
    "Consulte o manual do veículo para realizar passagens sem trocas de marcha indesejadas."
  );

  companion object {
    fun fromString(value: String?): DynoCompatibility {
      return when (value?.uppercase()?.trim()) {
        "RECOMMENDED" -> RECOMMENDED
        "MANUAL_MODE_REQUIRED" -> MANUAL_MODE_REQUIRED
        "NOT_RECOMMENDED" -> NOT_RECOMMENDED
        "UNKNOWN" -> UNKNOWN
        else -> UNKNOWN
      }
    }
  }
}

data class ManufacturerManifestEntry(
  val id: String,
  val name: String,
  val file: String
)

data class CatalogManifest(
  val catalogVersion: Int,
  val country: String,
  val updatedAt: String,
  val manufacturers: List<ManufacturerManifestEntry>,
  val transmissionFiles: List<String>
)

data class VehicleCatalogEntry(
  val id: String,
  val manufacturerId: String,
  val manufacturerName: String,
  val model: String,
  val generation: String? = null,
  val bodyType: String? = null,
  val startYear: Int,
  val endYear: Int,
  val trim: String? = null,
  val engineFamily: String? = null,
  val engineCode: String? = null,
  val engineDescription: String,
  val displacementCc: Int? = null,
  val valves: Int? = null,
  val aspiration: String? = null, // NATURALLY_ASPIRATED, TURBO, SUPERCHARGED
  val fuelType: String? = null, // GASOLINE, ETHANOL, FLEX, DIESEL
  val factoryPowerCv: Double? = null,
  val factoryPowerRpm: Int? = null,
  val factoryTorqueKgf: Double? = null,
  val factoryTorqueRpm: Int? = null,
  val curbWeightKg: Double? = null,
  val drivetrain: String? = "FWD", // FWD, RWD, AWD, UNKNOWN
  val originalTransmissionIds: List<String> = emptyList(),
  val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
  val sourceName: String? = null,
  val sourceReference: String? = null,
  val notes: String? = null
) {
  fun matchesYear(year: Int): Boolean {
    return year in startYear..endYear
  }

  fun toVehicleProfile(
    selectedTransmissionId: String? = originalTransmissionIds.firstOrNull(),
    selectedYear: Int? = null
  ): VehicleProfile {
    val yr = selectedYear ?: endYear
    val dispStr = displacementCc?.let { String.format("%.1f", it / 1000.0) } ?: ""
    val drivetrainLabel = when (drivetrain?.uppercase()) {
      "RWD" -> "Traseira"
      "AWD", "4WD" -> "Integral (4x4/AWD)"
      else -> "Dianteira"
    }

    return VehicleProfile(
      id = UUID.randomUUID().toString(),
      manufacturer = manufacturerName,
      model = model,
      year = yr,
      version = listOfNotNull(generation, trim).joinToString(" • ").ifBlank { trim ?: "" },
      engine = engineDescription,
      displacement = dispStr,
      factoryPowerCv = factoryPowerCv?.toFloat(),
      factoryTorqueKgf = factoryTorqueKgf?.toFloat(),
      curbWeightKg = curbWeightKg?.toFloat() ?: 1000f,
      drivetrain = drivetrainLabel,
      transmissionId = selectedTransmissionId,
      tireWidthMm = 185,
      tireAspectRatio = 65,
      wheelDiameterInches = 15,
      isCustom = false
    )
  }
}

data class TransmissionCatalogEntry(
  val id: String,
  val manufacturer: String,
  val family: String,
  val code: String? = null,
  val displayName: String,
  val type: String = "MANUAL", // MANUAL, AUTOMATIC, AUTOMATED, DCT, CVT
  val numberOfGears: Int,
  val gearRatios: List<Double?> = emptyList(),
  val finalDrive: Double? = null,
  val compatibleEngineFamilies: List<String> = emptyList(),
  val compatibleVehicleIds: List<String> = emptyList(),
  val dynoCompatibility: DynoCompatibility = DynoCompatibility.RECOMMENDED,
  val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
  val sourceName: String? = null,
  val sourceReference: String? = null,
  val notes: String? = null
) {
  fun toTransmissionProfile(): TransmissionProfile {
    // If ratios contain nulls, convert to 0f or keep valid floats
    val safeRatios = gearRatios.map { it?.toFloat() ?: 0f }
    return TransmissionProfile(
      id = id,
      manufacturer = manufacturer,
      family = family,
      code = code ?: family,
      displayName = displayName,
      gearRatios = safeRatios,
      finalDrive = finalDrive?.toFloat() ?: 0f,
      compatibleVehicleIds = compatibleVehicleIds,
      isCustom = false
    )
  }
}
