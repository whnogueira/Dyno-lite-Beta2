package com.example.data

import com.example.model.AspirationType
import com.example.model.FuelType
import com.example.model.SimulationEngine
import com.example.model.SimulationUiState
import com.example.model.Vehicle

class SimulationRepository(private val vehicleRepository: VehicleRepository) {

    suspend fun getInitialSimulationState(vehicleId: String? = null): SimulationUiState {
        val vehicle = if (vehicleId != null) {
            vehicleRepository.getVehicleById(vehicleId)
        } else {
            vehicleRepository.getPrimaryVehicle()
        }

        return if (vehicle != null) {
            fromVehicle(vehicle)
        } else {
            SimulationEngine.simulate(
                displacementCc = 2000,
                cylinderCount = 4,
                aspiration = AspirationType.NATURALLY_ASPIRATED,
                boostBar = 0.0f,
                injectorFlowLbH = 28.0f,
                fuelType = FuelType.ETHANOL,
                revLimitRpm = 6500,
                vehicleWeightKg = 1350f
            ).copy(
                selectedVehicleId = null,
                vehicleName = "Simulação Manual"
            )
        }
    }

    fun fromVehicle(vehicle: Vehicle): SimulationUiState {
        val defaultBoost = if (vehicle.aspiration == AspirationType.TURBOCHARGED) 1.0f else 0.0f
        val defaultInjectors = if (vehicle.aspiration == AspirationType.TURBOCHARGED) 42.0f else 28.0f

        val result = SimulationEngine.simulate(
            displacementCc = vehicle.engineDisplacementCc,
            cylinderCount = 4,
            aspiration = vehicle.aspiration,
            boostBar = defaultBoost,
            injectorFlowLbH = defaultInjectors,
            fuelType = vehicle.fuelType,
            revLimitRpm = vehicle.revLimitRpm,
            vehicleWeightKg = vehicle.totalMassKg
        )

        return result.copy(
            selectedVehicleId = vehicle.id,
            vehicleName = vehicle.name
        )
    }
}
