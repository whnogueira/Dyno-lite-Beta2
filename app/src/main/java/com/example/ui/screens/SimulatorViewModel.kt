package com.example.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SimulationRepository
import com.example.data.TuningBuildRepository
import com.example.data.VehicleRepository
import com.example.model.DrivetrainType
import com.example.model.SimulationConfig
import com.example.model.SimulationConfiguration
import com.example.model.SimulationEngine
import com.example.model.SimulationResult
import com.example.model.Vehicle
import com.example.model.VehicleProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SimulatorUiState(
  val isLoading: Boolean = true,
  val selectedVehicle: Vehicle? = null,
  val allVehicles: List<Vehicle> = emptyList(),
  val configuration: SimulationConfiguration = SimulationConfiguration.default(),
  val result: SimulationResult? = null,
  val validationErrors: Map<String, String> = emptyMap(),
  val errorMessage: String? = null
)

class SimulatorViewModel(
  private val vehicleRepository: VehicleRepository,
  private val simulationRepository: SimulationRepository,
  private val tuningBuildRepository: TuningBuildRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow(SimulatorUiState())
  val uiState: StateFlow<SimulatorUiState> = _uiState.asStateFlow()

  init {
    loadInitialData()
  }

  fun loadInitialData() {
    viewModelScope.launch {
      try {
        val vehicles = vehicleRepository.getVehicles()
        val primary = vehicleRepository.getPrimaryVehicle() ?: vehicles.firstOrNull()

        if (primary != null) {
          val config = buildConfigFromVehicle(primary)
          _uiState.update {
            it.copy(
              isLoading = false,
              selectedVehicle = primary,
              allVehicles = vehicles,
              configuration = config,
              validationErrors = emptyMap(),
              errorMessage = null
            )
          }
        } else {
          _uiState.update {
            it.copy(
              isLoading = false,
              selectedVehicle = null,
              allVehicles = vehicles,
              validationErrors = emptyMap(),
              errorMessage = null
            )
          }
        }
      } catch (e: Exception) {
        Log.e("SimulatorViewModel", "Error loading initial vehicle data", e)
        _uiState.update {
          it.copy(
            isLoading = false,
            errorMessage = "Não foi possível carregar os dados do veículo."
          )
        }
      }
    }
  }

  fun selectVehicle(vehicle: VehicleProfile) {
    val config = buildConfigFromVehicle(vehicle)
    _uiState.update {
      it.copy(
        selectedVehicle = vehicle,
        configuration = config,
        validationErrors = emptyMap(),
        errorMessage = null
      )
    }
  }

  fun setAspirationTurbo(isTurbo: Boolean, boostBar: Float = 0.8f) {
    val currentConfig = _uiState.value.configuration
    val updated = currentConfig.copy(
      isTurboSimulated = isTurbo,
      turboBoostBar = if (isTurbo) boostBar else 0.0f
    )
    _uiState.update { it.copy(configuration = updated) }
  }

  fun updateTurboBoost(boostBar: Float) {
    val currentConfig = _uiState.value.configuration
    _uiState.update {
      it.copy(
        configuration = currentConfig.copy(
          isTurboSimulated = true,
          turboBoostBar = boostBar
        )
      )
    }
  }

  fun updateConfiguration(config: SimulationConfiguration) {
    _uiState.update { it.copy(configuration = config) }
  }

  fun updateNumericField(fieldName: String, rawValue: String): Boolean {
    val normalized = rawValue.trim().replace(",", ".")
    val num = normalized.toDoubleOrNull()
    if (rawValue.isNotBlank() && num == null) {
      _uiState.update {
        it.copy(validationErrors = it.validationErrors + (fieldName to "Valor numérico inválido"))
      }
      return false
    } else {
      _uiState.update {
        it.copy(validationErrors = it.validationErrors - fieldName)
      }
      return true
    }
  }

  fun validateAndSimulate(): Boolean {
    val config = _uiState.value.configuration
    val errors = mutableMapOf<String, String>()

    if (config.vehicleCurbWeightKg <= 0f) {
      errors["vehicleCurbWeightKg"] = "O peso do veículo deve ser maior que zero"
    }
    if (config.enginePowerCv <= 0f) {
      errors["enginePowerCv"] = "A potência deve ser maior que zero"
    }
    if (config.engineTorqueKgfm <= 0f) {
      errors["engineTorqueKgfm"] = "O torque deve ser maior que zero"
    }
    if (config.finalDriveRatio <= 0f) {
      errors["finalDriveRatio"] = "A relação do diferencial deve ser maior que zero"
    }
    if (config.gearRatios.isEmpty() || config.gearRatios.any { it <= 0f }) {
      errors["gearRatios"] = "Relações de marcha inválidas"
    }
    if (config.isTurboSimulated && config.turboBoostBar < 0f) {
      errors["turboBoostBar"] = "Pressão de turbo não pode ser negativa"
    }

    if (errors.isNotEmpty()) {
      _uiState.update { it.copy(validationErrors = errors) }
      return false
    }

    return try {
      val simResult = SimulationEngine.runSimulation(config)
      _uiState.update {
        it.copy(
          result = simResult,
          validationErrors = emptyMap(),
          errorMessage = null
        )
      }
      true
    } catch (e: Exception) {
      Log.e("SimulatorViewModel", "Error running simulation for config", e)
      _uiState.update {
        it.copy(
          errorMessage = "Não foi possível calcular esta configuração. Revise os dados informados."
        )
      }
      false
    }
  }

  private fun buildConfigFromVehicle(vehicle: VehicleProfile): SimulationConfig {
    val driveType = when (vehicle.drivetrain.trim().uppercase()) {
      "TRAÇÃO TRASEIRA", "RWD" -> DrivetrainType.RWD
      "TRAÇÃO INTEGRAL", "AWD", "4X4" -> DrivetrainType.AWD
      else -> DrivetrainType.FWD
    }

    val defaultGears = listOf(3.73f, 2.05f, 1.36f, 1.03f, 0.82f)

    return SimulationConfig(
      label = "Configuração - ${vehicle.model}",
      vehicleName = "${vehicle.manufacturer} ${vehicle.model}",
      vehicleCurbWeightKg = vehicle.curbWeightKg.coerceAtLeast(400f),
      driverWeightKg = 80f,
      enginePowerCv = (vehicle.factoryPowerCv ?: 100f).coerceAtLeast(20f),
      engineTorqueKgfm = (vehicle.factoryTorqueKgf ?: 15f).coerceAtLeast(3f),
      peakPowerRpm = 5600,
      peakTorqueRpm = 3800,
      maxRpm = 6500,
      gearRatios = defaultGears,
      finalDriveRatio = (vehicle.finalDriveRatio ?: 3.94f).coerceAtLeast(1.0f),
      drivetrainLossPercent = (vehicle.customDrivetrainLossPercent ?: 12f).coerceIn(5f, 35f),
      drivetrainType = driveType,
      tireWidthMm = vehicle.tireWidthMm,
      tireAspectRatio = vehicle.tireAspectRatio,
      rimDiameterInches = vehicle.wheelDiameterInches,
      cd = vehicle.dragCoefficient,
      frontalAreaM2 = vehicle.frontalAreaM2,
      isTurboSimulated = false,
      turboBoostBar = 0.0f
    )
  }
}

class SimulatorViewModelFactory(
  private val vehicleRepository: VehicleRepository,
  private val simulationRepository: SimulationRepository,
  private val tuningBuildRepository: TuningBuildRepository
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(SimulatorViewModel::class.java)) {
      return SimulatorViewModel(vehicleRepository, simulationRepository, tuningBuildRepository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}
