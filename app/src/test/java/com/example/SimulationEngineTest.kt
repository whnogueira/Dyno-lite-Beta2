package com.example

import com.example.model.AspirationType
import com.example.model.FuelType
import com.example.model.SimulationEngine
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulationEngineTest {

    @Test
    fun testStandardSimulation() {
        val state = SimulationEngine.simulate(
            displacementCc = 2000,
            cylinderCount = 4,
            aspiration = AspirationType.TURBOCHARGED,
            boostBar = 1.0f,
            injectorFlowLbH = 42.0f,
            fuelType = FuelType.GASOLINE,
            revLimitRpm = 6500,
            vehicleWeightKg = 1350f
        )

        assertNotNull(state.estimatedPowerCv)
        assertTrue(state.estimatedPowerCv!! > 50f)
        assertTrue(state.curvePoints.isNotEmpty())
    }

    @Test
    fun testSafeHandlingOfEdgeCases() {
        // Zero displacement
        val stateZeroDisp = SimulationEngine.simulate(
            displacementCc = 0,
            cylinderCount = 0,
            aspiration = AspirationType.NATURALLY_ASPIRATED,
            boostBar = Float.NaN,
            injectorFlowLbH = 0f,
            fuelType = FuelType.ETHANOL,
            revLimitRpm = 0,
            vehicleWeightKg = 0f
        )
        assertNotNull(stateZeroDisp.estimatedPowerCv)
        assertTrue(stateZeroDisp.estimatedPowerCv!! > 0f)

        // Negative boost
        val stateNegBoost = SimulationEngine.simulate(
            displacementCc = 1600,
            cylinderCount = 4,
            aspiration = AspirationType.NATURALLY_ASPIRATED,
            boostBar = -2.0f,
            injectorFlowLbH = -10f,
            fuelType = FuelType.ETHANOL
        )
        assertNotNull(stateNegBoost.estimatedPowerCv)
        assertTrue(stateNegBoost.estimatedPowerCv!! > 0f)
    }
}
