package com.example.model

data class SimulationRpmPoint(
    val rpm: Int,
    val powerCv: Float,
    val torqueKgm: Float,
    val boostBar: Float,
    val volumetricEfficiencyPercent: Float,
    val injectorDutyCyclePercent: Float
)

data class SimulationUiState(
    val isLoading: Boolean = false,
    val selectedVehicleId: String? = null,
    val vehicleName: String = "Simulação Personalizada",
    val engineDisplacementCc: Int = 2000,
    val cylinderCount: Int = 4,
    val aspiration: AspirationType = AspirationType.NATURALLY_ASPIRATED,
    val boostBar: Float = 0.0f,
    val injectorFlowLbH: Float = 28.0f,
    val fuelType: FuelType = FuelType.ETHANOL,
    val targetRpm: Int = 6500,
    val estimatedPowerCv: Float? = null,
    val estimatedWheelPowerCv: Float? = null,
    val estimatedTorqueKgm: Float? = null,
    val estimatedZeroToHundredSec: Float? = null,
    val estimatedQuarterMileSec: Float? = null,
    val injectorDutyCyclePercent: Float? = null,
    val curvePoints: List<SimulationRpmPoint> = emptyList(),
    val errorMessage: String? = null
)
