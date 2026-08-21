package com.example.data

import com.example.model.TransmissionProfile
import com.example.model.VehicleProfile

object VehicleDatabase {

  val transmissions: List<TransmissionProfile> = listOf(
    TransmissionProfile(
      id = "gm_f17_ccw",
      manufacturer = "Chevrolet / Opel",
      family = "F17",
      code = "CCW",
      displayName = "GM F17 CCW (Manual 5M)",
      gearRatios = listOf(3.73f, 2.14f, 1.41f, 1.12f, 0.89f),
      finalDrive = 4.19f,
      compatibleVehicleIds = listOf("gm_corsa_10_2002", "gm_onix_10_2019", "gm-classic-10-vhce", "gm-celta-10-vhc")
    ),
    TransmissionProfile(
      id = "gm_f13",
      manufacturer = "Chevrolet / Opel",
      family = "F13",
      code = "F13",
      displayName = "GM F13 (Manual 5M)",
      gearRatios = listOf(3.73f, 2.14f, 1.41f, 1.12f, 0.89f),
      finalDrive = 4.19f,
      compatibleVehicleIds = listOf("gm-corsa-g1-10-wind")
    ),
    TransmissionProfile(
      id = "gm_f15",
      manufacturer = "Chevrolet / Opel",
      family = "F15",
      code = "F15",
      displayName = "GM F15 (Manual 5M)",
      gearRatios = listOf(3.73f, 2.14f, 1.41f, 1.12f, 0.89f),
      finalDrive = 3.94f,
      compatibleVehicleIds = listOf("gm-corsa-g1-16-16v-gsi")
    ),
    TransmissionProfile(
      id = "gm_f16",
      manufacturer = "Chevrolet / Opel",
      family = "F16",
      code = "F16",
      displayName = "GM F16 (Manual 5M)",
      gearRatios = listOf(3.55f, 2.16f, 1.48f, 1.12f, 0.89f),
      finalDrive = 3.94f,
      compatibleVehicleIds = listOf("gm-monza-20-efi", "gm-kadett-gsi-20-mpfi")
    ),
    TransmissionProfile(
      id = "gm_f17",
      manufacturer = "Chevrolet / Opel",
      family = "F17",
      code = "F17",
      displayName = "GM F17 (Manual 5M)",
      gearRatios = listOf(3.73f, 2.14f, 1.41f, 1.12f, 0.89f),
      finalDrive = 4.19f,
      compatibleVehicleIds = listOf("gm-corsa-g2-18-flexpower", "gm-montana-g1-18-flexpower", "gm-meriva-18-8v-flexpower")
    ),
    TransmissionProfile(
      id = "gm_f18",
      manufacturer = "Chevrolet / Opel",
      family = "F18",
      code = "F18",
      displayName = "GM F18 (Manual 5M)",
      gearRatios = listOf(3.58f, 1.87f, 1.23f, 0.92f, 0.74f),
      finalDrive = 3.94f,
      compatibleVehicleIds = listOf("gm-vectra-b-1999-gls-22-8v", "gm-vectra-b-22-16v-cd")
    ),
    TransmissionProfile(
      id = "gm_f20",
      manufacturer = "Chevrolet / Opel",
      family = "F20",
      code = "F20",
      displayName = "GM F20 (Manual 5M)",
      gearRatios = listOf(3.42f, 2.16f, 1.48f, 1.12f, 0.89f),
      finalDrive = 3.55f,
      compatibleVehicleIds = listOf("gm-calibra-20-16v", "gm-vectra-a-gsi-20-16v")
    ),
    TransmissionProfile(
      id = "gm_f23",
      manufacturer = "Chevrolet / Opel",
      family = "F23",
      code = "F23",
      displayName = "GM F23 (Manual 5M a Cabo)",
      gearRatios = listOf(3.58f, 2.02f, 1.35f, 0.98f, 0.79f),
      finalDrive = 3.95f,
      compatibleVehicleIds = listOf("gm-astra-g-20-8v", "gm-vectra-c-20-flexpower", "gm-zafira-20-flexpower")
    ),
    TransmissionProfile(
      id = "vw_020",
      manufacturer = "Volkswagen",
      family = "020",
      code = "020",
      displayName = "VW 020 (Manual 5M AP)",
      gearRatios = listOf(3.45f, 1.94f, 1.29f, 0.91f, 0.75f),
      finalDrive = 4.11f,
      compatibleVehicleIds = listOf("vw-gol-g1-ap-16-carb", "vw-gol-g1-ap-18-gts", "vw-gol-g1-ap-20-gti", "vw-gol-g2-ap-16-mi", "vw-gol-g3-ap-16-totalflex", "vw-santana-ap-20-mi")
    ),
    TransmissionProfile(
      id = "vw_02a",
      manufacturer = "Volkswagen",
      family = "02A",
      code = "02A",
      displayName = "VW 02A (Manual 5M)",
      gearRatios = listOf(3.78f, 2.12f, 1.36f, 0.97f, 0.77f),
      finalDrive = 3.68f,
      compatibleVehicleIds = listOf("vw-gol-g2-ap-20-gti", "vw-parati-g2-ap-20-gti")
    ),
    TransmissionProfile(
      id = "vw_mq200_02t",
      manufacturer = "Volkswagen",
      family = "MQ200",
      code = "02T",
      displayName = "VW MQ200 (02T Manual 5M)",
      gearRatios = listOf(3.77f, 2.10f, 1.36f, 0.97f, 0.80f),
      finalDrive = 4.35f,
      compatibleVehicleIds = listOf("vw_gol_10_2000", "vw_gol_16_2018", "vw-gol-g4-ea111-10", "vw-gol-g5-ea111-10", "vw-polo-ea111-16")
    ),
    TransmissionProfile(
      id = "vw_mq200",
      manufacturer = "Volkswagen",
      family = "MQ200",
      code = "02T",
      displayName = "VW MQ200 (Manual 5M EA111/EA211)",
      gearRatios = listOf(3.77f, 2.10f, 1.36f, 0.97f, 0.80f),
      finalDrive = 4.35f,
      compatibleVehicleIds = listOf("vw-gol-g5-ea111-16", "vw-voyage-mod-ea111-16", "vw-saveiro-mod-ea211-16-cross")
    ),
    TransmissionProfile(
      id = "vw_mq250",
      manufacturer = "Volkswagen",
      family = "MQ250",
      code = "02S",
      displayName = "VW MQ250 (Manual 5/6M)",
      gearRatios = listOf(3.78f, 2.06f, 1.35f, 0.97f, 0.77f),
      finalDrive = 3.65f,
      compatibleVehicleIds = listOf("vw-golf-mk4-ea113-20", "vw-golf-mk4-ea113-18t-gti", "vw-bora-ea113-20")
    ),
    TransmissionProfile(
      id = "vw_09g_tiptronic",
      manufacturer = "Volkswagen / Aisin",
      family = "09G",
      code = "AQ250",
      displayName = "VW 09G Tiptronic (Automático 6M)",
      gearRatios = listOf(4.15f, 2.37f, 1.56f, 1.16f, 0.86f, 0.69f),
      finalDrive = 3.87f,
      compatibleVehicleIds = listOf("vw-jetta-mk5-25-20v", "vw-jetta-mk6-20-8v", "vw-jetta-mk6-14-tsi", "vw-polo-ea211-10-tsi", "vw-virtus-ea211-10-tsi")
    ),
    TransmissionProfile(
      id = "vw_dsg_dq200",
      manufacturer = "Volkswagen",
      family = "DSG",
      code = "DQ200",
      displayName = "VW DSG DQ200 (Dupla Embreagem 7M a Seco)",
      gearRatios = listOf(3.76f, 2.27f, 1.53f, 1.12f, 0.88f, 0.73f, 0.60f),
      finalDrive = 4.44f,
      compatibleVehicleIds = listOf("vw-golf-mk7-ea211-14-tsi")
    ),
    TransmissionProfile(
      id = "vw_dsg_dq250",
      manufacturer = "Volkswagen",
      family = "DSG",
      code = "DQ250",
      displayName = "VW DSG DQ250 (Dupla Embreagem 6M Banhada a Óleo)",
      gearRatios = listOf(3.46f, 2.05f, 1.44f, 1.08f, 1.09f, 0.92f),
      finalDrive = 4.06f,
      compatibleVehicleIds = listOf("vw-jetta-mk6-20-tsi-highline", "vw-golf-mk7-ea888-20-gti")
    ),
    TransmissionProfile(
      id = "fiat_c513",
      manufacturer = "Fiat",
      family = "C513",
      code = "C513",
      displayName = "Fiat C513 (Manual 5M)",
      gearRatios = listOf(3.91f, 2.16f, 1.34f, 0.97f, 0.77f),
      finalDrive = 4.07f,
      compatibleVehicleIds = listOf("fiat_uno_10_2006", "fiat_palio_14_2012")
    ),
    TransmissionProfile(
      id = "ford_ib5",
      manufacturer = "Ford",
      family = "IB5",
      code = "IB5",
      displayName = "Ford IB5 (Manual 5M)",
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
      id = "gm-vectra-b-1999-gls-22-8v",
      manufacturer = "Chevrolet",
      model = "Vectra",
      year = 1999,
      version = "GLS",
      engine = "2.2 8V MPFI",
      displacement = "2.2",
      factoryPowerCv = 123f,
      factoryTorqueKgf = 19.4f,
      curbWeightKg = 1265f,
      drivetrain = "Dianteira",
      transmissionId = "gm_f18",
      tireWidthMm = 195,
      tireAspectRatio = 60,
      wheelDiameterInches = 15,
      isCustom = false
    ),
    VehicleProfile(
      id = "vw-gol-g1-ap-18-gts",
      manufacturer = "Volkswagen",
      model = "Gol",
      year = 1992,
      version = "GTS",
      engine = "AP 1.8S 8V",
      displacement = "1.8",
      factoryPowerCv = 99f,
      factoryTorqueKgf = 15.0f,
      curbWeightKg = 950f,
      drivetrain = "Dianteira",
      transmissionId = "vw_020",
      tireWidthMm = 185,
      tireAspectRatio = 60,
      wheelDiameterInches = 14,
      isCustom = false
    ),
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

