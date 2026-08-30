package com.example

import com.example.model.RunProcessor
import com.example.model.RunResult
import com.example.model.RunSample
import com.example.model.createConfigurationSnapshotSafe
import com.example.model.jsonSafe
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonSafeAndSnapshotTest {

    // 1. jsonSafe com Float.NaN deve retornar 0.0f
    @Test
    fun testJsonSafeFloatNaN() {
        val value = Float.NaN
        assertEquals(0.0f, value.jsonSafe(), 0.0001f)
    }

    // 2. jsonSafe com Float.POSITIVE_INFINITY deve retornar 0.0f
    @Test
    fun testJsonSafeFloatPositiveInfinity() {
        val value = Float.POSITIVE_INFINITY
        assertEquals(0.0f, value.jsonSafe(), 0.0001f)
    }

    // 3. jsonSafe com Float.NEGATIVE_INFINITY deve retornar 0.0f
    @Test
    fun testJsonSafeFloatNegativeInfinity() {
        val value = Float.NEGATIVE_INFINITY
        assertEquals(0.0f, value.jsonSafe(), 0.0001f)
    }

    // 4. jsonSafe com Double.NaN deve retornar 0.0
    @Test
    fun testJsonSafeDoubleNaN() {
        val value = Double.NaN
        assertEquals(0.0, value.jsonSafe(), 0.0001)
    }

    // 5. jsonSafe com Double.POSITIVE_INFINITY deve retornar 0.0
    @Test
    fun testJsonSafeDoublePositiveInfinity() {
        val value = Double.POSITIVE_INFINITY
        assertEquals(0.0, value.jsonSafe(), 0.0001)
    }

    // 6. jsonSafe com Double.NEGATIVE_INFINITY deve retornar 0.0
    @Test
    fun testJsonSafeDoubleNegativeInfinity() {
        val value = Double.NEGATIVE_INFINITY
        assertEquals(0.0, value.jsonSafe(), 0.0001)
    }

    // 7. createConfigurationSnapshotSafe com valores normais deve gerar JSON válido com todas as 13 chaves
    @Test
    fun testCreateConfigurationSnapshotSafeValidValues() {
        val result = RunResult(
            id = "test_snap_1",
            totalVehicleMassKg = 1450f,
            gearUsed = "3ª Marcha",
            gearRatioUsed = 1.46f,
            finalDriveUsed = 3.94f,
            drivetrainLossPercent = 15.0f,
            cdUsed = 0.31f,
            frontalAreaUsed = 2.19f,
            crrUsed = 0.015f,
            airDensityUsed = 1.225f,
            slopeModeUsed = "FLAT",
            slopePercentUsed = 0.0f,
            startSpeedKmh = 30.0f,
            endSpeedKmh = 140.0f
        )

        val jsonString = createConfigurationSnapshotSafe(result)
        assertNotNull(jsonString)
        assertFalse(jsonString.isBlank())

        val json = JSONObject(jsonString)
        val expectedKeys = listOf(
            "totalMassKg", "gearUsed", "gearRatio", "finalDrive",
            "drivetrainLossPercent", "cd", "frontalAreaM2", "crr",
            "airDensityKgM3", "slopeMode", "slopePercent", "startSpeedKmh", "endSpeedKmh"
        )

        for (key in expectedKeys) {
            assertTrue("JSON deve conter a chave '$key'", json.has(key))
        }

        assertEquals(1450.0, json.getDouble("totalMassKg"), 0.1)
        assertEquals("3ª Marcha", json.getString("gearUsed"))
        assertEquals(1.46, json.getDouble("gearRatio"), 0.01)
        assertEquals(3.94, json.getDouble("finalDrive"), 0.01)
    }

    // 8. createConfigurationSnapshotSafe com NaN e Infinity não deve lançar exceção
    @Test
    fun testCreateConfigurationSnapshotSafeWithNaNAndInfinity() {
        val result = RunResult(
            id = "test_snap_corrupted",
            totalVehicleMassKg = Float.NaN,
            gearUsed = "",
            gearRatioUsed = Float.POSITIVE_INFINITY,
            finalDriveUsed = Float.NEGATIVE_INFINITY,
            drivetrainLossPercent = Float.NaN,
            cdUsed = Float.NaN,
            frontalAreaUsed = Float.POSITIVE_INFINITY,
            crrUsed = Float.NaN,
            airDensityUsed = Float.NaN,
            slopeModeUsed = "",
            slopePercentUsed = Float.NaN,
            startSpeedKmh = Float.POSITIVE_INFINITY,
            endSpeedKmh = Float.NEGATIVE_INFINITY
        )

        val jsonString = createConfigurationSnapshotSafe(result)
        assertNotNull(jsonString)

        val json = JSONObject(jsonString)
        assertEquals(0.0, json.getDouble("totalMassKg"), 0.0001)
        assertEquals("Não informado", json.getString("gearUsed"))
        assertEquals(1.0, json.getDouble("gearRatio"), 0.0001)
        assertEquals(1.0, json.getDouble("finalDrive"), 0.0001)
        assertEquals("FLAT", json.getString("slopeMode"))
    }
}
