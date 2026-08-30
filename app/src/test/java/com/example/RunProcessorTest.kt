package com.example

import com.example.model.RunProcessor
import com.example.model.RunSample
import com.example.model.Vehicle
import com.example.model.finiteOrDefault
import com.example.model.finiteOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunProcessorTest {

    @Test
    fun testFiniteExtensions() {
        val nan = Float.NaN
        val inf = Float.POSITIVE_INFINITY
        val valid = 123.45f

        assertNull(nan.finiteOrNull())
        assertNull(inf.finiteOrNull())
        assertEquals(123.45f, valid.finiteOrNull())

        assertEquals(0f, nan.finiteOrDefault(0f))
        assertEquals(10f, inf.finiteOrDefault(10f))
        assertEquals(123.45f, valid.finiteOrDefault(0f))
    }

    @Test
    fun testProcessRun200SamplesSuccess() {
        val vehicle = Vehicle(
            id = "test_gti",
            name = "VW Golf GTI",
            curbWeightKg = 1370f,
            driverWeightKg = 80f
        )

        val samples = (0 until 200).map { i ->
            val t = i * 0.05f
            val speed = 20f + (i * 0.6f) // 20 to 140 km/h
            RunSample(
                timestampNs = (i * 50_000_000L),
                elapsedSeconds = t,
                speedKmh = speed,
                accelerationMps2 = 2.5f,
                longitudinalG = 0.35f,
                estimatedRpm = 2000 + (i * 25),
                wheelPowerCv = 150f + (i * 0.3f),
                enginePowerCv = 180f + (i * 0.4f),
                wheelTorqueKgm = 25f,
                engineTorqueKgm = 30f
            )
        }

        val result = RunProcessor.processRun(
            sessionId = "test_session_200",
            vehicle = vehicle,
            rawSamples = samples,
            durationOverride = 10f
        )

        assertEquals("test_session_200", result.id)
        assertEquals("test_gti", result.vehicleId)
        assertEquals(200, result.samples.size)
        assertTrue(result.peakEnginePowerCv!! > 180f)
        assertTrue(result.peakEngineTorqueKgm!! >= 30f)
        assertTrue(result.peakLongitudinalG!! >= 0.35f)
        assertEquals("VALID", result.qualityStatus)
        assertNull(result.technicalFailureReason)
    }

    @Test
    fun testProcessRunWithNaNsAndInfinities() {
        val vehicle = Vehicle(id = "corrupted_veh", name = "Test Car")

        val samples = listOf(
            RunSample(
                timestampNs = 0L,
                elapsedSeconds = Float.NaN,
                speedKmh = Float.POSITIVE_INFINITY,
                accelerationMps2 = Float.NaN,
                longitudinalG = Float.NEGATIVE_INFINITY,
                estimatedRpm = 99999,
                wheelPowerCv = Float.NaN,
                enginePowerCv = Float.NaN
            ),
            RunSample(
                timestampNs = 1_000_000_000L,
                elapsedSeconds = 1.0f,
                speedKmh = 60f,
                accelerationMps2 = 2.0f,
                longitudinalG = 0.3f,
                estimatedRpm = 3000,
                wheelPowerCv = 80f,
                enginePowerCv = 100f
            )
        )

        val result = RunProcessor.processRun(
            sessionId = "session_nan_safe",
            vehicle = vehicle,
            rawSamples = samples
        )

        assertNotNull(result)
        assertFalse(result.peakEnginePowerCv!!.isNaN())
        assertFalse(result.peakEnginePowerCv!!.isInfinite())
        assertTrue(result.peakEnginePowerCv!! >= 0f)
    }

    @Test
    fun testProcessRunWithNullVehicleFallback() {
        val samples = listOf(
            RunSample(
                timestampNs = 0L,
                elapsedSeconds = 0f,
                speedKmh = 30f,
                accelerationMps2 = 1.5f,
                longitudinalG = 0.2f,
                estimatedRpm = 2000,
                enginePowerCv = 90f
            )
        )

        val result = RunProcessor.processRun(
            sessionId = "session_null_veh",
            vehicle = null,
            rawSamples = samples
        )

        assertNotNull(result)
        assertEquals("session_null_veh", result.id)
        assertNotNull(result.vehicleName)
    }

    @Test
    fun testPartialResultWhenPowerIsMissing() {
        val vehicle = Vehicle(id = "car1", name = "Car 1")
        // Samples with speed and G but zero power calculation
        val samples = listOf(
            RunSample(timestampNs = 0L, elapsedSeconds = 0f, speedKmh = 10f, longitudinalG = 0f),
            RunSample(timestampNs = 500_000_000L, elapsedSeconds = 0.5f, speedKmh = 20f, longitudinalG = 0f)
        )

        val result = RunProcessor.processRun(
            sessionId = "session_partial",
            vehicle = vehicle,
            rawSamples = samples
        )

        assertNotNull(result)
        assertEquals("session_partial", result.id)
        assertEquals("DADOS INSUFICIENTES", result.qualityStatus)
        assertNotNull(result.technicalFailureReason)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testEmptySampleListThrowsException() {
        RunProcessor.processRun(
            sessionId = "empty_session",
            vehicle = null,
            rawSamples = emptyList()
        )
    }
}
