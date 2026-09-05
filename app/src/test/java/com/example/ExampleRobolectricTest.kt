package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.VehicleCatalogRepository
import com.example.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Dyno Lite", appName)
  }

  @Test
  fun `catalog repository loads manufacturers and vehicles`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = VehicleCatalogRepository.getInstance(context)

    val manufacturers = repo.getManufacturers()
    assertTrue(manufacturers.any { it.id == "vw" || it.id == "volkswagen" })
    assertTrue(manufacturers.any { it.id == "gm" || it.id == "chevrolet" })

    // Test VW Vehicles (AP to Jetta)
    val vwVehicles = repo.getVehiclesForManufacturer("vw")
    assertTrue(vwVehicles.isNotEmpty())
    assertTrue(vwVehicles.any { it.model.contains("Gol", ignoreCase = true) })
    assertTrue(vwVehicles.any { it.model.contains("Jetta", ignoreCase = true) })

    // Test GM Vehicles (Family 1 and 2)
    val gmVehicles = repo.getVehiclesForManufacturer("gm")
    assertTrue(gmVehicles.isNotEmpty())
    assertTrue(gmVehicles.any { it.model.contains("Corsa", ignoreCase = true) })
    assertTrue(gmVehicles.any { it.model.contains("Vectra", ignoreCase = true) })

    // Test Search
    val apResults = repo.searchCatalog("AP")
    assertTrue(apResults.isNotEmpty())

    val vectraResults = repo.searchCatalog("Vectra")
    assertTrue(vectraResults.isNotEmpty())

    // Test Transmissions
    val transmissions = repo.getAllTransmissions()
    assertTrue(transmissions.isNotEmpty())
    assertTrue(transmissions.any { it.id.contains("gm_f") || it.displayName.contains("GM") })
    assertTrue(transmissions.any { it.id.contains("vw_020") || it.displayName.contains("VW") })

    // Test Conversion to VehicleProfile
    val firstVw = vwVehicles.first()
    val profile = firstVw.toVehicleProfile()
    assertEquals(firstVw.manufacturerName, profile.manufacturer)
    assertEquals(firstVw.model, profile.model)
    assertTrue(profile.curbWeightKg > 0)
  }

  @Test
  fun `run result repository save retrieve and filter comparison`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val runRepo = com.example.data.RunResultRepository(context)
    runRepo.clearAllResults()

    val run1 = com.example.model.RunResult(
      vehicleName = "Gol 1.6 AP",
      maximumGpsSpeedKmh = 105.0f,
      quality = "BOA"
    )
    val run2 = com.example.model.RunResult(
      vehicleName = "Gol 1.6 AP",
      maximumGpsSpeedKmh = 108.5f,
      quality = "BOA"
    )
    runRepo.saveResult(run1)
    runRepo.saveResult(run2)

    val results = runRepo.getResults()
    assertEquals(2, results.size)

    val validForGol = results.filter { it.vehicleName == "Gol 1.6 AP" && it.quality != "INVÁLIDA" }
    assertTrue(validForGol.size >= 2)
  }

  @Test
  fun `test simulator open crash reproduction`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tuningRepo = com.example.data.TuningBuildRepository(context)
    val builds = tuningRepo.getSavedBuilds()
    assertNotNull(builds)
    assertTrue(builds.isNotEmpty())
    val active = tuningRepo.getActiveBuild()
    assertNotNull(active)
  }

  @Test
  fun `test simulator view model empty state and vehicle selection`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vehicleRepo = com.example.data.VehicleRepository(context)
    val simRepo = com.example.data.SimulationRepository(context)
    val tuningRepo = com.example.data.TuningBuildRepository(context)

    vehicleRepo.getVehicles().forEach { vehicleRepo.deleteVehicle(it.id) }

    val vm = com.example.ui.screens.SimulatorViewModel(vehicleRepo, simRepo, tuningRepo)
    val state = vm.uiState.value

    assertEquals(false, state.isLoading)

    // Add a vehicle to vehicle repo and select it
    val testVehicle = com.example.model.VehicleProfile(
      id = "test-veh-1",
      manufacturer = "Chevrolet",
      model = "Vectra 2.0 8V",
      year = 1998,
      factoryPowerCv = 110.0f,
      factoryTorqueKgf = 17.3f,
      curbWeightKg = 1250.0f,
      drivetrain = "FWD"
    )
    vehicleRepo.saveVehicle(testVehicle)

    vm.loadInitialData()
    val updatedState = vm.uiState.value
    assertNotNull(updatedState.selectedVehicle)
    assertEquals("Vectra 2.0 8V", updatedState.selectedVehicle?.model)
    assertEquals(110.0f, updatedState.configuration.enginePowerCv)

    // Test simulation calculation
    val simSuccess = vm.validateAndSimulate()
    assertTrue(simSuccess)
    assertNotNull(vm.uiState.value.result)

    // Test turbo mode toggle
    vm.setAspirationTurbo(true, 0.8f)
    assertTrue(vm.uiState.value.configuration.isTurboSimulated)
    assertEquals(0.8f, vm.uiState.value.configuration.turboBoostBar)
  }
}
