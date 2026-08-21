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
    assertTrue(manufacturers.any { it.id == "volkswagen" })
    assertTrue(manufacturers.any { it.id == "chevrolet" })

    // Test VW Vehicles (AP to Jetta)
    val vwVehicles = repo.getVehiclesForManufacturer("volkswagen")
    assertTrue(vwVehicles.isNotEmpty())
    assertTrue(vwVehicles.any { it.model.contains("Gol", ignoreCase = true) })
    assertTrue(vwVehicles.any { it.model.contains("Jetta", ignoreCase = true) })

    // Test GM Vehicles (Family 1 and 2)
    val gmVehicles = repo.getVehiclesForManufacturer("chevrolet")
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
    assertTrue(transmissions.any { it.id.contains("gm_f17") || it.code.contains("F17") })
    assertTrue(transmissions.any { it.id.contains("vw_pv") || it.code.contains("PV") })

    // Test Conversion to VehicleProfile
    val firstVw = vwVehicles.first()
    val profile = firstVw.toVehicleProfile()
    assertEquals(firstVw.manufacturerName, profile.manufacturer)
    assertEquals(firstVw.model, profile.model)
    assertTrue(profile.curbWeightKg > 0)
  }
}
