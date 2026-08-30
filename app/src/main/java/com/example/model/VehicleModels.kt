package com.example.model

enum class FuelType(val displayName: String, val densityKgL: Float, val stoichiometricRatio: Float, val energyDensityMjKg: Float) {
    GASOLINE("Gasolina Comum", 0.74f, 14.7f, 44.0f),
    PREMIUM_GASOLINE("Gasolina Podium / Premium", 0.75f, 14.7f, 44.5f),
    ETHANOL("Etanol (E100)", 0.79f, 9.0f, 26.8f),
    DIESEL("Diesel S10", 0.83f, 14.5f, 43.0f),
    E25("Gasolina Brasileira (E27)", 0.755f, 13.2f, 39.5f)
}

enum class AspirationType(val displayName: String) {
    NATURALLY_ASPIRATED("Aspirado"),
    TURBOCHARGED("Turbo"),
    SUPERCHARGED("Supercharger")
}

enum class DriveType(val displayName: String) {
    FWD("Dianteira (FWD)"),
    RWD("Traseira (RWD)"),
    AWD("Integral (AWD)")
}

data class TireSpec(
    val widthMm: Int = 205,
    val profilePercent: Int = 55,
    val rimInches: Int = 16
) {
    val rollingRadiusMeters: Float
        get() {
            val sidewallMm = widthMm * (profilePercent / 100.0f)
            val totalDiameterMm = (rimInches * 25.4f) + (2 * sidewallMm)
            return (totalDiameterMm / 2000.0f) * 0.975f // 2.5% de deflexão do pneu
        }
    val rollingCircumferenceMeters: Float
        get() = 2f * Math.PI.toFloat() * rollingRadiusMeters
}

data class Vehicle(
    val id: String = "default_vehicle",
    val name: String = "Carro Padrão",
    val brand: String = "Genérico",
    val model: String = "2.0 Turbo",
    val year: Int = 2022,
    val curbWeightKg: Float = 1350f,
    val driverWeightKg: Float = 80f,
    val additionalWeightKg: Float = 0f,
    val frontalAreaM2: Float = 2.15f,
    val dragCoefficientCd: Float = 0.31f,
    val drivetrainLossPercent: Float = 15.0f,
    val tireSpec: TireSpec = TireSpec(),
    val finalDriveRatio: Float = 3.94f,
    val gearRatios: List<Float> = listOf(3.78f, 2.12f, 1.46f, 1.03f, 0.86f, 0.73f),
    val testGearIndex: Int = 2, // 3ª marcha (índice 2)
    val engineDisplacementCc: Int = 2000,
    val aspiration: AspirationType = AspirationType.TURBOCHARGED,
    val fuelType: FuelType = FuelType.GASOLINE,
    val revLimitRpm: Int = 6500,
    val isPrimary: Boolean = true
) {
    val totalMassKg: Float
        get() = curbWeightKg + driverWeightKg + additionalWeightKg

    val testGearRatio: Float
        get() = gearRatios.getOrElse(testGearIndex) { 1.46f }

    val totalGearReduction: Float
        get() = testGearRatio * finalDriveRatio

    fun calculateRpmFromSpeedKmh(speedKmh: Float): Int {
        if (speedKmh <= 0f) return 800
        val speedMps = speedKmh / 3.6f
        val wheelRps = speedMps / (2.0 * Math.PI * tireSpec.rollingRadiusMeters)
        val engineRps = wheelRps * totalGearReduction
        return (engineRps * 60.0).toInt().coerceIn(600, 9500)
    }

    fun calculateSpeedKmhFromRpm(rpm: Int): Float {
        val engineRps = rpm / 60.0
        val wheelRps = engineRps / totalGearReduction
        val speedMps = wheelRps * (2.0 * Math.PI * tireSpec.rollingRadiusMeters)
        return (speedMps * 3.6).toFloat().coerceAtLeast(0f)
    }
}

enum class DynoRunState {
    PARADO,
    AGUARDANDO_INICIO,
    MEDINDO_PROTEGIDO,
    MEDINDO,
    SUSPEITA_DESACELERACAO,
    CONCLUIDO,
    CANCELADO
}
