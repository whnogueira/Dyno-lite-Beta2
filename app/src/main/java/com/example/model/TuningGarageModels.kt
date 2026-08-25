package com.example.model

import java.util.UUID

// =========================================================================================
// MODELOS DA GARAGEM VIRTUAL DE PREPARAÇÃO AUTOMOTIVA (DYNO MOBILE)
// =========================================================================================

enum class TuningCategory(val title: String, val subtitle: String, val iconName: String) {
  MOTOR("Motor & Internos", "Pistões, bielas, junta e virabrequim", "engine"),
  ALIMENTACAO("Alimentação & Bicos", "Bicos injetores, bomba e pressão", "fuel_pump"),
  ASPIRACAO("Aspiração & Turbo", "Aspirado, turbo, supercharger e intercooler", "turbo"),
  COMBUSTIVEL("Combustível", "Gasolina, etanol, E85 ou metanol", "gas_station"),
  INJECAO("Injeção & Gerenciamento", "ECU original, programável e tipo de acerto", "chip"),
  IGNICAO("Ignição & Velas", "Bobinas, cabos, velas e avanço", "spark"),
  ADMISSAO("Admissão & Filtro", "Filtro cônico, CAI, coletor plenum e TBI", "air_filter"),
  ESCAPE("Escape & Coletor", "Coletor tubular 4x1 e diâmetro de escape", "exhaust"),
  CABECOTE("Cabeçote & Taxa", "Dutos retrabalhados, válvulas e taxa de compressão", "cylinder_head"),
  COMANDO("Comando de Válvulas", "Graduação e perfil de comando", "camshaft"),
  TRANSMISSAO("Transmissão & Embreagem", "Embreagem original, cerâmica ou multidisco", "transmission"),
  PNEUS_TRACAO("Pneus & Tração", "Composto do pneu, medidas e tração", "tire"),
  PESO("Alívio de Peso", "Bancos, forrações, estepe e fibra de carbono", "weight"),
  AERODINAMICA("Aerodinâmica", "Splitters dianteiros e asa traseira", "aero")
}

enum class TuningProjectLevel(val title: String, val description: String, val badgeColorHex: Long) {
  ORIGINAL("Original", "Configuração de fábrica sem alterações", 0xFF7A8290),
  LEVE("Stage 1 - Leve", "Modificações básicas de admissão, escape e acerto", 0xFF42C77A),
  STREET("Stage 2 - Street", "Turbo baixa pressão ou aspirado com comando", 0xFF39C6F4),
  STREET_FORTE("Stage 3 - Street Forte", "Turbo intermediário com intercooler e injeção", 0xFF2F80ED),
  TRACK("Stage 4 - Track Day", "Conjunto forjado, suspensão/pneus e alívio", 0xFF8B5CF6),
  COMPETICAO("Stage 5 - Competição", "Alta pressão, pneus slick e gerenciamento total", 0xFFFF8A3D),
  EXTREMO("Stage 6 - Extremo", "Mecânica no limite com metanol e alta pressão", 0xFFE35D62)
}

enum class ProjectTemplateType(val displayName: String, val description: String) {
  ORIGINAL("Aspirado Original", "Veículo 100% de fábrica"),
  ASPIRADO_PREPARADO("Aspirado Preparado", "Comando 276°, cabeçote trabalhado, coletor 4x1 e escape"),
  TURBO_BAIXA_PRESSAO("Turbo Baixa Pressão (0.5 bar)", "Kit turbo básico em miolo original com intercooler"),
  TURBO_INTERMEDIARIO("Turbo Intermediário (0.8 bar)", "Turbo médio, bicos maiores, bomba 255 L/h e ECU programável"),
  TURBO_FORJADO_ALTA("Turbo Forjado Alta Pressão (1.5 bar)", "Pistões/bielas forjadas, bicos 80 lb/h, intercooler grande e etanol"),
  SUPERCHARGER("Supercharger", "Compressor mecânico com torque imediato desde marcha-lenta"),
  CUSTOMIZADO("Projeto Personalizado", "Configuração totalmente customizada peça por peça")
}

// -----------------------------------------------------------------------------------------
// ENUMS DE PEÇAS E COMPONENTES
// -----------------------------------------------------------------------------------------

enum class PistonType(val displayName: String, val maxHpLimit: Float, val priceBrl: Double) {
  ORIGINAL("Pistões Fundidos Originais", 230f, 0.0),
  FORJADO_RUA("Pistões Forjados Street (Iapel/AFP)", 550f, 1800.0),
  FORJADO_COMPETICAO("Pistões Forjados Competição (Wiseco/CP)", 900f, 3200.0)
}

enum class RodsType(val displayName: String, val maxHpLimit: Float, val priceBrl: Double) {
  ORIGINAL("Bielas Originais", 220f, 0.0),
  FORJADA_H("Bielas Forjadas Perfil H (Scat/Super A)", 600f, 1900.0),
  FORJADA_I_COMPETICAO("Bielas Forjadas Perfil I Heavy Duty (Carrillo)", 1000f, 4500.0)
}

enum class StudsType(val displayName: String, val maxHpLimit: Float, val priceBrl: Double) {
  ORIGINAL("Parafusos de Cabeçote Originais", 250f, 0.0),
  REFORCADO_ARP("Prisioneiros Forjados ARP", 800f, 1200.0)
}

enum class HeadGasketType(val displayName: String, val maxBoostBar: Float, val priceBrl: Double) {
  ORIGINAL("Junta Convencional Original", 0.6f, 0.0),
  REFORCADA_MLS("Junta de Aço Multilâminas (MLS)", 2.5f, 450.0),
  COBRE_ORING("Junta com O-Ring e Cobre", 4.0f, 1100.0)
}

enum class CrankshaftType(val displayName: String, val maxHpLimit: Float, val priceBrl: Double) {
  ORIGINAL("Virabrequim Original Fundido", 350f, 0.0),
  FORJADO_ALIVIADO("Virabrequim Forjado / Nitretado e Balanceado", 800f, 2800.0)
}

enum class FuelTypeOption(
  val displayName: String,
  val bsfcNa: Float,
  val bsfcTurbo: Float,
  val octaneRon: Float,
  val densityKgL: Float,
  val knockResistanceFactor: Float,
  val coldStartReliability: Float
) {
  GASOLINA_COMUM("Gasolina Comum", 0.50f, 0.60f, 92f, 0.74f, 0.70f, 1.0f),
  GASOLINA_PREMIUM("Gasolina Premium / Podium", 0.48f, 0.58f, 102f, 0.75f, 0.85f, 1.0f),
  ETANOL("Etanol Hidratado (E100)", 0.65f, 0.80f, 110f, 0.79f, 1.00f, 0.85f),
  E85("E85 (85% Etanol / 15% Gasolina)", 0.62f, 0.75f, 108f, 0.78f, 0.98f, 0.92f),
  METANOL("Metanol Puro (M100)", 1.10f, 1.35f, 120f, 0.79f, 1.20f, 0.70f)
}

enum class AspirationType(val displayName: String) {
  ASPIRADO("Aspirado Natural (N/A)"),
  TURBO_PEQUENO("Turbo Pequeno (.36/.48 - Pegada rápida)"),
  TURBO_MEDIO("Turbo Médio (.50/.48 - Equilíbrio Rua)"),
  TURBO_GRANDE("Turbo Grande (.70/.84 - Alta Potência)"),
  SUPERCHARGER("Supercharger (Compressor Mecânico)"),
  TURBO_CUSTOM("Turbo Customizado")
}

enum class IntercoolerType(
  val displayName: String,
  val tempDropC: Float,
  val maxHpEfficiency: Float,
  val pressureDropBar: Float,
  val spoolLagMs: Int,
  val priceBrl: Double
) {
  SEM_INTERCOOLER("Sem Intercooler", 0f, 190f, 0.0f, 0, 0.0),
  PEQUENO_FRONTAL("Intercooler Pequeno Frontal", 25f, 320f, 0.04f, 30, 950.0),
  MEDIO_ALTA_EFICIENCIA("Intercooler Médio Alta Eficiência", 42f, 550f, 0.06f, 60, 1600.0),
  GRANDE_COMPETICAO("Intercooler Grande Competição", 55f, 900f, 0.09f, 110, 2600.0)
}

enum class EcuType(
  val displayName: String,
  val allowsBoost: Boolean,
  val maxSafeHp: Float,
  val tunePrecisionFactor: Float,
  val priceBrl: Double
) {
  ORIGINAL("ECU Original de Fábrica", false, 160f, 1.00f, 0.0),
  PIGGYBACK("Módulo Piggyback / Clamper", true, 240f, 1.02f, 850.0),
  PROGRAMAVEL_BASICA("ECU Programável Básica (ex: FT300 / InjePro)", true, 500f, 1.05f, 2500.0),
  PROGRAMAVEL_COMPLETA("ECU Programável Completa (ex: FT550 / Hondata / Haltech)", true, 1200f, 1.08f, 5800.0)
}

enum class TuneMapType(val displayName: String, val powerGainFactor: Float, val knockRiskMultiplier: Float, val reliabilityFactor: Float) {
  SEM_ACERTO("Sem Acerto (Mapa padrão descalibrado)", 0.80f, 2.5f, 0.40f),
  CONSERVADOR("Acerto Conservador (Margem segura de ponto e mistura rica)", 0.94f, 0.6f, 0.98f),
  RUA_EQUILIBRADO("Acerto de Rua (Equilíbrio potência x segurança)", 1.00f, 1.0f, 0.92f),
  POTENCIA_AGRESSIVO("Acerto de Potência Agressivo (Ponto no limite e mistura estequiométrica)", 1.06f, 1.8f, 0.75f)
}

enum class IgnitionCoilType(val displayName: String, val maxBoostBar: Float, val priceBrl: Double) {
  ORIGINAL("Bobina Original", 0.6f, 0.0),
  ALTA_POTENCIA("Bobina Dupla de Alta Energia", 1.4f, 450.0),
  INDIVIDUAIS_R8("Bobinas Individuais por Cilindro (Audi R8 / MSD)", 3.5f, 1400.0)
}

enum class SparkPlugType(val displayName: String, val heatRange: String, val priceBrl: Double) {
  ORIGINAL("Velas Originais Convencionais", "Grau Médio (6)", 0.0),
  IRIDIUM_MEDIO("Velas Iridium Grau Frio 7", "Grau Frio (7)", 220.0),
  RACING_IRIDIUM("Velas Racing Iridium Grau Frio 8/9", "Grau Muito Frio (8/9)", 450.0)
}

enum class IntakeType(val displayName: String, val powerGainHp: Float, val restrictionFactor: Float, val priceBrl: Double) {
  FILTRO_ORIGINAL("Filtro de Ar de Fábrica com Caixa", 0f, 1.00f, 0.0),
  FILTRO_CONICO_ESPORTIVO("Filtro Cônico Esportivo Inbox/Cônico", 4f, 0.92f, 250.0),
  COLD_AIR_INTAKE("Cold Air Intake (CAI) Direto", 8f, 0.85f, 650.0),
  COLETOR_PLENUM_DIMENSIONADO("Coletor de Admissão Plenum + CAI", 16f, 0.72f, 2200.0)
}

enum class ThrottleBodyType(val displayName: String, val diameterMm: Int, val maxHpFlow: Float, val priceBrl: Double) {
  ORIGINAL("TBI Original de Fábrica", 52, 170f, 0.0),
  AUMENTADA_60MM("Corpo de Borboleta (TBI) 60mm", 60, 280f, 450.0),
  RACING_70MM("Corpo de Borboleta (TBI) 70mm Billet", 70, 600f, 950.0)
}

enum class ExhaustHeaderType(val displayName: String, val lowRpmTorqueGain: Float, val highRpmPowerGain: Float, val priceBrl: Double) {
  ORIGINAL("Coletor de Escape Original Fundido", 0f, 0f, 0.0),
  TUBULAR_4X1_MEDIO("Coletor Tubular 4x1 Dimensionado Street", 0.5f, 9.0f, 1100.0),
  TUBULAR_4X1_RACING("Coletor Tubular 4x1 Racing Inox", -0.5f, 16.0f, 2200.0)
}

enum class ExhaustSystemType(val displayName: String, val diameterInches: Float, val maxHpFlow: Float, val priceBrl: Double) {
  ORIGINAL("Escape Original com Catalisador e Silenciosos", 1.875f, 160f, 0.0),
  ESPORTIVO_2_POLEGADAS("Escape 2.0\" com Abafador Esportivo", 2.0f, 220f, 750.0),
  DIMENSIONADO_2_5_POLEGADAS("Escape 2.5\" Dimensionado de Alta Vazão", 2.5f, 380f, 1400.0),
  RACING_3_POLEGADAS("Escape 3.0\" Direto Inox sem Restrição", 3.0f, 800f, 2100.0)
}

enum class CylinderHeadType(val displayName: String, val flowGainPercent: Float, val maxHpFlow: Float, val priceBrl: Double) {
  ORIGINAL("Cabeçote Original de Fábrica", 0f, 180f, 0.0),
  DUTOS_POLIDOS_STREET("Dutos Polidos e Ângulos de Válvula (Stage 1)", 12f, 260f, 1600.0),
  FLUXO_CRUZADO_VALVULAS_MAIORES("Cabeçote Trabalhado em Banco de Fluxo + Válvulas Maiores", 25f, 480f, 3800.0)
}

enum class CamshaftProfile(
  val displayName: String,
  val durationDegrees: Int,
  val lowRpmTorqueMod: Float,
  val highRpmPowerGain: Float,
  val idleStability: Float,
  val priceBrl: Double
) {
  ORIGINAL("Comando de Válvulas Original", 240, 0.0f, 0.0f, 1.0f, 0.0),
  LEVE_260("Comando Leve 260° (Street/Torque em Média)", 260, 0.6f, 7.0f, 0.95f, 950.0),
  MEDIO_276("Comando Médio 276° (Esportivo Rua - Lenta Levemente Embaralhada)", 276, -0.8f, 15.0f, 0.85f, 1250.0),
  BRAVO_288("Comando Bravo 288°+ (Pista/Alta Rotação - Perda de Baixa)", 288, -2.4f, 26.0f, 0.65f, 1650.0)
}

enum class ClutchType(val displayName: String, val maxTorqueKgfm: Float, val comfortLevel: Float, val priceBrl: Double) {
  ORIGINAL("Embreagem Original Orgânica", 22f, 1.00f, 0.0),
  CERAMICA_4_PASTILHAS("Embreagem de Cerâmica 4 Pastilhas com Molas", 45f, 0.70f, 980.0),
  CERAMICA_6_PASTILHAS("Embreagem de Cerâmica 6 Pastilhas Heavy Duty", 60f, 0.55f, 1350.0),
  MULTIDISCO_CARBONO("Embreagem Multidisco de Carbono Racing", 100f, 0.40f, 3900.0)
}

enum class TireCompound(val displayName: String, val frictionMu: Float, val pricePerSetBrl: Double) {
  RUA_CONVENCIONAL("Pneus de Rua Convencionais (μ 0.80)", 0.80f, 0.0),
  ESPORTIVO_UHP("Pneus Ultra High Performance (μ 0.95)", 0.95f, 1800.0),
  SEMI_SLICK("Pneus Semi-Slick R-Compound (μ 1.15)", 1.15f, 3200.0),
  SLICK_ARRANCADA("Pneus Slick de Arrancada (μ 1.40)", 1.40f, 5400.0)
}

enum class WeightReductionStage(val displayName: String, val weightRemovedKg: Float, val comfortImpact: Float, val priceBrl: Double) {
  ORIGINAL("Sem Alívio de Peso (Original)", 0f, 1.0f, 0.0),
  LEVE("Alívio Leve (Estepe, macaco e ferramentas - 25 kg)", 25f, 0.95f, 0.0),
  MODERADO("Alívio Moderado (Bancos traseiros e forrações - 70 kg)", 70f, 0.75f, 0.0),
  PISTA_RADICAL("Alívio Radical Pista (Interior depenado, sem ar/som - 130 kg)", 130f, 0.40f, 500.0),
  FIBRA_CARBONO("Painéis em Fibra de Vidro/Carbono e Policarbonato (180 kg)", 180f, 0.30f, 4800.0)
}

enum class AeroPackage(val displayName: String, val dragCdDelta: Float, val frontalAreaDelta: Float, val topSpeedGripBonus: Float, val priceBrl: Double) {
  ORIGINAL("Aerodinâmica Original de Fábrica", 0.0f, 0.0f, 0.0f, 0.0),
  SPOILER_DISCRETO("Spoiler Traseiro Discreto e Defletores", 0.01f, 0.0f, 0.05f, 450.0),
  KIT_AERO_TRACK("Kit Asa Traseira Ajustável + Splitter Dianteiro", 0.04f, 0.05f, 0.15f, 2200.0)
}

// -----------------------------------------------------------------------------------------
// ESTRUTURA COMPLETA DO PROJETO DE PREPARAÇÃO (BUILD)
// -----------------------------------------------------------------------------------------

data class TuningBuild(
  val id: String = UUID.randomUUID().toString(),
  val projectName: String = "Meu Projeto",
  val vehicleName: String = "Chevrolet Vectra 2.2 8V 1999",
  val baseRunId: String? = null,
  val isDemonstrativeVehicle: Boolean = false,
  
  // 1. Dados Base do Motor Original
  val displacementCc: Int = 2198,
  val cylindersCount: Int = 4,
  val factoryEnginePowerCv: Float = 123f,
  val factoryEngineTorqueKgfm: Float = 19.4f,
  val factoryPeakPowerRpm: Int = 5200,
  val factoryPeakTorqueRpm: Int = 2800,
  val factoryRedlineRpm: Int = 6200,
  val baseVehicleCurbWeightKg: Float = 1260f,
  val driverWeightKg: Float = 80f,
  val baseDrivetrain: DrivetrainType = DrivetrainType.FWD,
  val baseCompressionRatio: Float = 9.2f,
  
  // Transmissão Base
  val gearRatios: List<Float> = listOf(3.73f, 1.96f, 1.32f, 0.95f, 0.76f),
  val finalDriveRatio: Float = 3.94f,
  val drivetrainLossPercent: Float = 12f,
  val shiftSpeed: ShiftSpeedType = ShiftSpeedType.MANUAL_FAST,
  
  // Pneus Base
  val tireWidthMm: Int = 195,
  val tireAspectRatio: Int = 60,
  val rimInches: Int = 15,
  
  // Aerodinâmica Base
  val baseCd: Float = 0.31f,
  val baseFrontalAreaM2: Float = 2.05f,
  
  // 2. Modificações Selecionadas (Peça por Peça)
  val pistons: PistonType = PistonType.ORIGINAL,
  val rods: RodsType = RodsType.ORIGINAL,
  val studs: StudsType = StudsType.ORIGINAL,
  val headGasket: HeadGasketType = HeadGasketType.ORIGINAL,
  val crankshaft: CrankshaftType = CrankshaftType.ORIGINAL,
  val extraCompressionRatio: Float = 0.0f, // Aumento de taxa manual
  
  // Alimentação
  val injectorFlowLbHr: Float = 28.0f,
  val injectorBasePressureBar: Float = 3.0f,
  val injectorOperatingPressureBar: Float = 3.0f,
  val injectorCount: Int = 4,
  val maxInjectorDutyCyclePercent: Float = 85.0f,
  val fuelPumpFlowLph: Float = 100.0f,
  val fuelPumpCount: Int = 1,
  
  // Aspiração & Turbo
  val aspiration: AspirationType = AspirationType.ASPIRADO,
  val turboBoostBar: Float = 0.0f,
  val turboSpoolStartRpm: Int = 2200,
  val turboFullBoostRpm: Int = 3200,
  val turboMaxFlowHp: Float = 260f,
  val turboEfficiency: Float = 0.85f,
  val intercooler: IntercoolerType = IntercoolerType.SEM_INTERCOOLER,
  
  // Combustível
  val fuelType: FuelTypeOption = FuelTypeOption.ETANOL,
  
  // Injeção & Ignição
  val ecu: EcuType = EcuType.ORIGINAL,
  val tuneMap: TuneMapType = TuneMapType.RUA_EQUILIBRADO,
  val ignitionCoil: IgnitionCoilType = IgnitionCoilType.ORIGINAL,
  val sparkPlugs: SparkPlugType = SparkPlugType.ORIGINAL,
  val timingAdvanceDegrees: Float = 0.0f,
  
  // Admissão & Escape
  val intake: IntakeType = IntakeType.FILTRO_ORIGINAL,
  val throttleBody: ThrottleBodyType = ThrottleBodyType.ORIGINAL,
  val exhaustHeader: ExhaustHeaderType = ExhaustHeaderType.ORIGINAL,
  val exhaustSystem: ExhaustSystemType = ExhaustSystemType.ORIGINAL,
  
  // Cabeçote & Comando
  val cylinderHead: CylinderHeadType = CylinderHeadType.ORIGINAL,
  val camshaft: CamshaftProfile = CamshaftProfile.ORIGINAL,
  val customRedlineRpm: Int? = null,
  
  // Transmissão & Embreagem
  val clutch: ClutchType = ClutchType.ORIGINAL,
  
  // Pneus, Peso & Aero
  val tireCompound: TireCompound = TireCompound.RUA_CONVENCIONAL,
  val weightReduction: WeightReductionStage = WeightReductionStage.ORIGINAL,
  val aero: AeroPackage = AeroPackage.ORIGINAL,
  
  // Custos Personalizados Cadastrados
  val customPartPrices: Map<String, Double> = emptyMap(),
  val laborCostBrl: Double = 0.0
)

// -----------------------------------------------------------------------------------------
// RESULTADO DETALHADO DO CÁLCULO DA PREPARAÇÃO
// -----------------------------------------------------------------------------------------

data class TuningGauges(
  val airFlowPercent: Float,
  val fuelFlowPercent: Float,
  val turboCapacityPercent: Float,
  val injectorDutyPercent: Float,
  val fuelPumpUsagePercent: Float,
  val engineStressPercent: Float,
  val clutchStressPercent: Float,
  val gripPercent: Float,
  val reliabilityPercent: Float
)

data class TuningCalculationResult(
  val build: TuningBuild,
  val projectLevel: TuningProjectLevel,
  
  // Potências e Torques Estimados
  val estimatedEnginePowerCv: Float,
  val estimatedWheelPowerCv: Float,
  val estimatedEngineTorqueKgfm: Float,
  val estimatedWheelTorqueKgfm: Float,
  val peakPowerRpm: Int,
  val peakTorqueRpm: Int,
  val effectiveRedlineRpm: Int,
  val actualBoostBar: Float,
  
  // Massa e Dinâmica
  val totalVehicleMassKg: Float,
  val weightToPowerRatioKgCv: Float,
  
  // Sistema de Combustível e Bicos (Seções 5, 6, 7)
  val correctedInjectorFlowLbHr: Float,
  val correctedInjectorFlowCcMin: Float,
  val maxSupportedPowerByInjectorsCv: Float,
  val injectorDutyCyclePercent: Float,
  val injectorStatusDescription: String,
  val requiredFuelPumpFlowLph: Float,
  val availableFuelPumpFlowLph: Float,
  val fuelPumpUsagePercent: Float,
  val fuelPumpStatusDescription: String,
  
  // Limites Mecânicos e Riscos (Seções 14, 15)
  val engineStructuralLimitHp: Float,
  val engineStressPercent: Float,
  val structuralRiskLevel: String,
  val clutchTorqueCapacityKgfm: Float,
  val isClutchSlipping: Boolean,
  
  // Gargalos Identificados
  val primaryBottleneckTitle: String,
  val primaryBottleneckDescription: String,
  val allWarnings: List<String>,
  
  // Pontuações do Projeto (0 a 100) (Seção 19)
  val performanceScore: Int,
  val spoolResponseScore: Int,
  val reliabilityScore: Int,
  val costBenefitScore: Int,
  val dailyDriveScore: Int,
  
  // Orçamento (Seção 20)
  val totalPartsCostBrl: Double,
  val totalLaborCostBrl: Double,
  val grandTotalCostBrl: Double,
  val costPerCvGainedBrl: Double,
  val missingMandatoryParts: List<String>,
  val nextRecommendedUpgrade: String,
  
  // Medidores Visuais (Seção 16)
  val gauges: TuningGauges,
  
  // Curvas de Dinamômetro (RPM -> (Potência cv, Torque kgfm, Pressão bar, Bicos %))
  val powerCurvePoints: List<Pair<Int, Float>>,
  val torqueCurvePoints: List<Pair<Int, Float>>,
  val boostCurvePoints: List<Pair<Int, Float>>,
  val injectorDutyCurvePoints: List<Pair<Int, Float>>
)

// -----------------------------------------------------------------------------------------
// RESULTADO DO TESTE DE PISTA / DINAMÔMETRO ANIMADO (Seção 17)
// -----------------------------------------------------------------------------------------

data class DynoTestAccelerationResult(
  val time0to60Kmh: Float,
  val time0to100Kmh: Float,
  val time60to100Kmh: Float,
  val time80to120Kmh: Float,
  val time100to200Kmh: Float?,
  val time60ft: Float,
  val time100m: Float,
  val speedAt100mKmh: Float,
  val time201m: Float,
  val speedAt201mKmh: Float,
  val time402m: Float,
  val speedAt402mKmh: Float,
  val estimatedTopSpeedKmh: Float,
  val peakLongitudinalG: Float,
  val fuelConsumptionWotLph: Float,
  val shiftPoints: List<GearShiftPoint>,
  val speedTimePoints: List<Pair<Float, Float>> // (tempo em seg, velocidade km/h)
)
