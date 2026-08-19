package com.example.data

import com.example.model.TransmissionProfile
import com.example.model.VehicleProfile

object VehicleDatabase {

  val transmissions: List<TransmissionProfile> = listOf(
    TransmissionProfile(
      id = "gm_f17_ccw",
      manufacturer = "GM",
      family = "F17",
      code = "CCW",
      displayName = "GM F17 (CCW)",
      gearRatios = listOf(3.73f, 2.14f, 1.41f, 1.12f, 0.89f),
      finalDrive = 4.19f,
      compatibleVehicleIds = listOf("gm_corsa_10_2002", "gm_onix_10_2019")
    ),
    TransmissionProfile(
      id = "vw_mq200_02t",
      manufacturer = "Volkswagen",
      family = "MQ200",
      code = "02T",
      displayName = "VW MQ200 (02T)",
      gearRatios = listOf(3.77f, 2.10f, 1.36f, 0.97f, 0.80f),
      finalDrive = 4.35f,
      compatibleVehicleIds = listOf("vw_gol_10_2000", "vw_gol_16_2018")
    ),
    TransmissionProfile(
      id = "fiat_c513",
      manufacturer = "Fiat",
      family = "C513",
      code = "C513",
      displayName = "Fiat C513 (Manual)",
      gearRatios = listOf(3.91f, 2.16f, 1.34f, 0.97f, 0.77f),
      finalDrive = 4.07f,
      compatibleVehicleIds = listOf("fiat_uno_10_2006", "fiat_palio_14_2012")
    ),
    TransmissionProfile(
      id = "ford_ib5",
      manufacturer = "Ford",
      family = "IB5",
      code = "IB5",
      displayName = "Ford IB5 (Manual)",
      gearRatios = listOf(3.58f, 1.93f, 1.28f, 0.95f, 0.76f),
      finalDrive = 4.06f,
      compatibleVehicleIds = listOf("ford_ka_10_2017")
    ),
    TransmissionProfile(
      id = "toyota_k111",
      manufacturer = "Toyota",
      family = "CVT",
      code = "K111",
      displayName = "Toyota MultiDrive (K111)",
      gearRatios = listOf(2.48f, 1.52f, 1.00f, 0.72f, 0.53f),
      finalDrive = 5.69f,
      compatibleVehicleIds = listOf("toyota_corolla_20_2018")
    ),
    TransmissionProfile(
      id = "honda_spca",
      manufacturer = "Honda",
      family = "SPCA",
      code = "SPCA",
      displayName = "Honda Manual (SPCA)",
      gearRatios = listOf(3.14f, 1.87f, 1.24f, 0.95f, 0.76f),
      finalDrive = 4.29f,
      compatibleVehicleIds = listOf("honda_civic_18_2010")
    )
  )

  val catalogVehicles: List<VehicleProfile> = listOf(
    VehicleProfile(
      id = "gm_corsa_10_2002",
      manufacturer = "Chevrolet",
      model = "Corsa",
      year = 2002,
      version = "Wind / Maxx",
      engine = "1.0 8V VHC",
      displacement = "1.0",
      factoryPowerCv = 71f,
      factoryTorqueKgf = 9.2f,
      curbWeightKg = 915f,
      drivetrain = "Dianteira",
      transmissionId = "gm_f17_ccw",
      tireWidthMm = 165,
      tireAspectRatio = 70,
      wheelDiameterInches = 13,
      isCustom = false
    ),
    VehicleProfile(
      id = "gm_onix_10_2019",
      manufacturer = "Chevrolet",
      model = "Onix",
      year = 2019,
      version = "LT / Joy",
      engine = "1.0 8V SPE/4",
      displacement = "1.0",
      factoryPowerCv = 80f,
      factoryTorqueKgf = 9.8f,
      curbWeightKg = 1012f,
      drivetrain = "Dianteira",
      transmissionId = "gm_f17_ccw",
      tireWidthMm = 185,
      tireAspectRatio = 65,
      wheelDiameterInches = 14,
      isCustom = false
    ),
    VehicleProfile(
      id = "vw_gol_10_2000",
      manufacturer = "Volkswagen",
      model = "Gol",
      year = 2000,
      version = "G3 MI",
      engine = "1.0 8V AT",
      displacement = "1.0",
      factoryPowerCv = 68f,
      factoryTorqueKgf = 8.8f,
      curbWeightKg = 920f,
      drivetrain = "Dianteira",
      transmissionId = "vw_mq200_02t",
      tireWidthMm = 175,
      tireAspectRatio = 70,
      wheelDiameterInches = 13,
      isCustom = false
    ),
    VehicleProfile(
      id = "vw_gol_16_2018",
      manufacturer = "Volkswagen",
      model = "Gol",
      year = 2018,
      version = "Comfortline / Trendline",
      engine = "1.6 8V EA111",
      displacement = "1.6",
      factoryPowerCv = 104f,
      factoryTorqueKgf = 15.6f,
      curbWeightKg = 1015f,
      drivetrain = "Dianteira",
      transmissionId = "vw_mq200_02t",
      tireWidthMm = 195,
      tireAspectRatio = 55,
      wheelDiameterInches = 15,
      isCustom = false
    ),
    VehicleProfile(
      id = "fiat_uno_10_2006",
      manufacturer = "Fiat",
      model = "Uno",
      year = 2006,
      version = "Mille Fire",
      engine = "1.0 8V Fire",
      displacement = "1.0",
      factoryPowerCv = 65f,
      factoryTorqueKgf = 9.1f,
      curbWeightKg = 830f,
      drivetrain = "Dianteira",
      transmissionId = "fiat_c513",
      tireWidthMm = 165,
      tireAspectRatio = 70,
      wheelDiameterInches = 13,
      isCustom = false
    ),
    VehicleProfile(
      id = "fiat_palio_14_2012",
      manufacturer = "Fiat",
      model = "Palio",
      year = 2012,
      version = "Attractive",
      engine = "1.4 8V Fire EVO",
      displacement = "1.4",
      factoryPowerCv = 86f,
      factoryTorqueKgf = 12.4f,
      curbWeightKg = 995f,
      drivetrain = "Dianteira",
      transmissionId = "fiat_c513",
      tireWidthMm = 175,
      tireAspectRatio = 65,
      wheelDiameterInches = 14,
      isCustom = false
    ),
    VehicleProfile(
      id = "ford_ka_10_2017",
      manufacturer = "Ford",
      model = "Ka",
      year = 2017,
      version = "SE / SEL",
      engine = "1.0 12V 3C Ti-VCT",
      displacement = "1.0",
      factoryPowerCv = 85f,
      factoryTorqueKgf = 10.7f,
      curbWeightKg = 1009f,
      drivetrain = "Dianteira",
      transmissionId = "ford_ib5",
      tireWidthMm = 175,
      tireAspectRatio = 65,
      wheelDiameterInches = 14,
      isCustom = false
    ),
    VehicleProfile(
      id = "toyota_corolla_20_2018",
      manufacturer = "Toyota",
      model = "Corolla",
      year = 2018,
      version = "XEi",
      engine = "2.0 16V Dual VVT-i",
      displacement = "2.0",
      factoryPowerCv = 154f,
      factoryTorqueKgf = 20.7f,
      curbWeightKg = 1320f,
      drivetrain = "Dianteira",
      transmissionId = "toyota_k111",
      tireWidthMm = 205,
      tireAspectRatio = 55,
      wheelDiameterInches = 16,
      isCustom = false
    ),
    VehicleProfile(
      id = "honda_civic_18_2010",
      manufacturer = "Honda",
      model = "Civic",
      year = 2010,
      version = "LXS",
      engine = "1.8 16V i-VTEC",
      displacement = "1.8",
      factoryPowerCv = 140f,
      factoryTorqueKgf = 17.7f,
      curbWeightKg = 1245f,
      drivetrain = "Dianteira",
      transmissionId = "honda_spca",
      tireWidthMm = 205,
      tireAspectRatio = 55,
      wheelDiameterInches = 16,
      isCustom = false
    )
  )

  fun findTransmission(id: String?): TransmissionProfile? {
    if (id == null) return null
    return transmissions.firstOrNull { it.id == id }
  }

  fun getTransmission(id: String?): TransmissionProfile? = findTransmission(id)

  fun searchCatalog(query: String): List<VehicleProfile> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return catalogVehicles
    return catalogVehicles.filter {
      it.manufacturer.lowercase().contains(q) ||
        it.model.lowercase().contains(q) ||
        it.version.lowercase().contains(q) ||
        it.engine.lowercase().contains(q) ||
        it.year.toString().contains(q)
    }
  }

  fun searchVehicles(query: String): List<VehicleProfile> = searchCatalog(query)
}
