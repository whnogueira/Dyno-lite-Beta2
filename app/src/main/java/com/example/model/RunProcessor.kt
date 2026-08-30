package com.example.model

import android.util.Log
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

fun Float.jsonSafe(default: Float = 0f): Float =
    if (isFinite()) this else default

fun Double.jsonSafe(default: Double = 0.0): Double =
    if (isFinite()) this else default

fun Float.finiteOrNull(): Float? = if (isFinite()) this else null
fun Float?.finiteOrDefault(default: Float = 0f): Float = if (this != null && this.isFinite()) this else default

fun Double.finiteOrNull(): Double? = if (isFinite()) this else null
fun Double?.finiteOrDefault(default: Double = 0.0): Double = if (this != null && this.isFinite()) this else default

fun createConfigurationSnapshotSafe(
    result: RunResult
): String {
    return try {
        JSONObject().apply {
            put("totalMassKg", result.totalVehicleMassKg.toDouble().jsonSafe())
            put("gearUsed", result.gearUsed.ifBlank { "Não informado" })
            put("gearRatio", result.gearRatioUsed.toDouble().jsonSafe(1.0))
            put("finalDrive", result.finalDriveUsed.toDouble().jsonSafe(1.0))
            put("drivetrainLossPercent", result.drivetrainLossPercent.toDouble().jsonSafe())
            put("cd", result.cdUsed.toDouble().jsonSafe(0.34))
            put("frontalAreaM2", result.frontalAreaUsed.toDouble().jsonSafe(2.10))
            put("crr", result.crrUsed.toDouble().jsonSafe(0.015))
            put("airDensityKgM3", result.airDensityUsed.toDouble().jsonSafe(1.225))
            put("slopeMode", result.slopeModeUsed.ifBlank { "FLAT" })
            put("slopePercent", result.slopePercentUsed.toDouble().jsonSafe())
            put("startSpeedKmh", result.startSpeedKmh.toDouble().jsonSafe())
            put("endSpeedKmh", result.endSpeedKmh.toDouble().jsonSafe())
        }.toString()
    } catch (e: Exception) {
        Log.e(
            "DynoStorage",
            "Falha ao criar snapshot: ${e.javaClass.simpleName}: ${e.message}",
            e
        )
        "{}"
    }
}

object RunProcessor {
    private const val TAG = "DynoMobile"

    fun sanitizeSample(sample: RunSample, sessionId: String = "", index: Int = 0): RunSample {
        val deterministicId = if (sample.sampleId.isNotBlank()) sample.sampleId else if (sessionId.isNotBlank()) "$sessionId-$index" else "$index"
        val deterministicIndex = if (sample.sampleIndex > 0) sample.sampleIndex else index

        return sample.copy(
            sampleId = deterministicId,
            sampleIndex = deterministicIndex,
            timestampNs = if (sample.timestampNs > 0L) sample.timestampNs else System.nanoTime(),
            elapsedSeconds = sample.elapsedSeconds.jsonSafe(0f).coerceAtLeast(0f),
            speedKmh = sample.speedKmh.jsonSafe(0f).coerceAtLeast(0f),
            accelerationMps2 = sample.accelerationMps2.jsonSafe(0f),
            longitudinalG = sample.longitudinalG.jsonSafe(0f).coerceIn(-3.0f, 3.0f),
            estimatedRpm = sample.estimatedRpm.coerceIn(0, 15000),
            wheelPowerCv = sample.wheelPowerCv.jsonSafe(0f).coerceAtLeast(0f),
            enginePowerCv = sample.enginePowerCv.jsonSafe(0f).coerceAtLeast(0f),
            wheelTorqueKgm = sample.wheelTorqueKgm.jsonSafe(0f).coerceAtLeast(0f),
            engineTorqueKgm = sample.engineTorqueKgm.jsonSafe(0f).coerceAtLeast(0f),
            aeroLossCv = sample.aeroLossCv.jsonSafe(0f).coerceAtLeast(0f),
            rollLossCv = sample.rollLossCv.jsonSafe(0f).coerceAtLeast(0f),
            drivetrainLossCv = sample.drivetrainLossCv.jsonSafe(0f).coerceAtLeast(0f),
            inertialLossCv = sample.inertialLossCv.jsonSafe(0f).coerceAtLeast(0f),
            gpsAccuracyMeters = sample.gpsAccuracyMeters.jsonSafe(99f).coerceAtLeast(0f),
            latitude = sample.latitude.jsonSafe(0.0),
            longitude = sample.longitude.jsonSafe(0.0)
        )
    }

    fun processRun(
        sessionId: String,
        vehicle: Vehicle?,
        rawSamples: List<RunSample>,
        durationOverride: Float? = null
    ): RunResult {
        if (rawSamples.isEmpty()) {
            throw IllegalArgumentException("A lista de amostras está vazia para a sessão $sessionId")
        }

        val effectiveVehicle = vehicle ?: Vehicle(
            id = "recovered_vehicle",
            name = "Veículo do Teste",
            curbWeightKg = 1350f,
            driverWeightKg = 80f
        )

        // 1. Sanitização rigorosa de cada amostra com ID determinístico
        val sanitizedSamples = rawSamples.mapIndexed { idx, s ->
            sanitizeSample(s, sessionId = sessionId, index = idx)
        }

        val startTimestampNs = sanitizedSamples.firstOrNull()?.timestampNs ?: 0L
        val endTimestampNs = sanitizedSamples.lastOrNull()?.timestampNs ?: startTimestampNs
        val computedDuration = if (endTimestampNs > startTimestampNs) {
            ((endTimestampNs - startTimestampNs) / 1_000_000_000f).jsonSafe(0f)
        } else {
            (sanitizedSamples.size * 0.05f)
        }
        val durationSeconds = durationOverride?.jsonSafe(computedDuration)?.coerceAtLeast(0.01f) ?: computedDuration.coerceAtLeast(0.01f)

        val startSpeedKmh = sanitizedSamples.firstOrNull()?.speedKmh ?: 0f
        val endSpeedKmh = sanitizedSamples.lastOrNull()?.speedKmh ?: 0f
        val maxSpeedKmh = sanitizedSamples.maxOfOrNull { it.speedKmh } ?: 0f

        Log.d(TAG, "Processando sessão $sessionId: ${sanitizedSamples.size} amostras | Vel Inicial: $startSpeedKmh | Vel Máx: $maxSpeedKmh | Duração: $durationSeconds s")

        // 2. Cálculo / Recálculo de Potência e Torque para amostras
        var isPartialResult = false
        var technicalReason: String? = null

        val processedSamples = sanitizedSamples.mapIndexed { index, sample ->
            var samplePower = sample.enginePowerCv
            var sampleWheelPower = sample.wheelPowerCv
            var sampleTorque = sample.engineTorqueKgm

            // Se amostra não tem potência calculada válida, tentar derivar fisicamente
            if (samplePower <= 0f && sample.speedKmh > 5f && sample.longitudinalG > 0f) {
                try {
                    val speedMps = (sample.speedKmh / 3.6f).jsonSafe(0f)
                    val massKg = effectiveVehicle.totalMassKg.coerceIn(400f, 10000f)
                    val accelMps2 = (sample.longitudinalG * 9.81f).jsonSafe(0f).coerceAtLeast(0f)

                    val areaM2 = effectiveVehicle.frontalAreaM2.jsonSafe(2.15f).coerceIn(1.0f, 5.0f)
                    val cd = effectiveVehicle.dragCoefficientCd.jsonSafe(0.31f).coerceIn(0.15f, 1.2f)
                    val fAero = 0.5f * 1.2f * areaM2 * cd * speedMps.pow(2)
                    val fRoll = massKg * 9.81f * 0.015f
                    val fInertia = massKg * accelMps2
                    val fTotal = (fInertia + fAero + fRoll).coerceAtLeast(0f)

                    val wheelWatts = fTotal * speedMps
                    val wheelCv = ((wheelWatts / 735.5f)).jsonSafe(0f)
                    val lossPct = effectiveVehicle.drivetrainLossPercent.jsonSafe(15f).coerceIn(0f, 50f)
                    val engCv = (wheelCv / max(1.0f - (lossPct / 100f), 0.5f)).jsonSafe(0f)

                    val rpm = sample.estimatedRpm.takeIf { it > 500 } ?: effectiveVehicle.calculateRpmFromSpeedKmh(sample.speedKmh)
                    val torqueKgm = if (rpm > 0) ((engCv * 716.2f) / rpm).jsonSafe(0f) else 0f

                    sampleWheelPower = wheelCv
                    samplePower = engCv
                    sampleTorque = torqueKgm
                } catch (e: Exception) {
                    Log.w(TAG, "Falha ao interpolar potência da amostra $index: ${e.message}")
                }
            }

            sample.copy(
                enginePowerCv = samplePower.jsonSafe(0f),
                wheelPowerCv = sampleWheelPower.jsonSafe(0f),
                engineTorqueKgm = sampleTorque.jsonSafe(0f)
            )
        }

        val peakEngineCv = processedSamples.maxOfOrNull { it.enginePowerCv.jsonSafe(0f) } ?: 0f
        val peakWheelCv = processedSamples.maxOfOrNull { it.wheelPowerCv.jsonSafe(0f) } ?: 0f
        val peakTorque = processedSamples.maxOfOrNull { it.engineTorqueKgm.jsonSafe(0f) } ?: 0f
        val peakG = processedSamples.maxOfOrNull { it.longitudinalG.jsonSafe(0f) } ?: 0f

        val peakSample = processedSamples.maxByOrNull { it.enginePowerCv.jsonSafe(0f) }
        val peakRpm = peakSample?.estimatedRpm?.coerceIn(0, 15000) ?: 0
        val peakSpeed = peakSample?.speedKmh?.jsonSafe(0f) ?: 0f

        val peakTorqueSample = processedSamples.maxByOrNull { it.engineTorqueKgm.jsonSafe(0f) }
        val peakTorqueRpm = peakTorqueSample?.estimatedRpm?.coerceIn(0, 15000) ?: 0

        if (peakEngineCv <= 0f && peakTorque <= 0f) {
            isPartialResult = true
            technicalReason = "Dados dinâmicos insuficientes para cálculo de potência de pico. Valores de velocidade e G preservados."
            Log.w(TAG, "Sessão $sessionId classificada como recuperação parcial: $technicalReason")
        }

        // 3. Cálculo seguro de tempos de aceleração
        val t0to100 = calculateSpeedIntervalTime(processedSamples, 0f, 100f)
        val t80to120 = calculateSpeedIntervalTime(processedSamples, 80f, 120f)
        val t100to200 = calculateSpeedIntervalTime(processedSamples, 100f, 200f)

        val gearRatio = effectiveVehicle.gearRatios.getOrNull(effectiveVehicle.testGearIndex) ?: 1.0f

        val preliminaryResult = RunResult(
            id = sessionId,
            vehicleId = effectiveVehicle.id,
            vehicleName = effectiveVehicle.name,
            testDateTimestamp = System.currentTimeMillis(),
            peakEnginePowerCv = peakEngineCv.jsonSafe(0f),
            peakEnginePowerRpm = peakRpm,
            peakEnginePowerSpeedKmh = peakSpeed.jsonSafe(0f),
            peakWheelPowerCv = peakWheelCv.jsonSafe(0f),
            peakEngineTorqueKgm = peakTorque.jsonSafe(0f),
            peakEngineTorqueRpm = peakTorqueRpm,
            peakLongitudinalG = peakG.jsonSafe(0f),
            startSpeedKmh = startSpeedKmh.jsonSafe(0f),
            endSpeedKmh = endSpeedKmh.jsonSafe(0f),
            testGear = (effectiveVehicle.testGearIndex + 1).coerceIn(1, 8),
            durationSeconds = durationSeconds.jsonSafe(0f),
            zeroToHundredSeconds = t0to100?.jsonSafe(),
            eightyToOneTwentySeconds = t80to120?.jsonSafe(),
            oneHundredToTwoHundredSeconds = t100to200?.jsonSafe(),
            quarterMileSeconds = null,
            quarterMileSpeedKmh = null,
            temperatureCelsius = 25.0f,
            pressureHpa = 1013.25f,
            saeCorrectionFactor = 1.0f,
            totalVehicleMassKg = effectiveVehicle.totalMassKg.jsonSafe(1350f),
            gearUsed = "${effectiveVehicle.testGearIndex + 1}ª Marcha",
            gearRatioUsed = gearRatio.jsonSafe(1.0f),
            finalDriveUsed = effectiveVehicle.finalDriveRatio.jsonSafe(1.0f),
            drivetrainLossPercent = effectiveVehicle.drivetrainLossPercent.jsonSafe(15.0f),
            cdUsed = effectiveVehicle.dragCoefficientCd.jsonSafe(0.31f),
            frontalAreaUsed = effectiveVehicle.frontalAreaM2.jsonSafe(2.15f),
            crrUsed = 0.015f,
            airDensityUsed = 1.225f,
            slopeModeUsed = "FLAT",
            slopePercentUsed = 0.0f,
            samples = processedSamples,
            qualityStatus = if (isPartialResult) "DADOS INSUFICIENTES" else "VALID",
            technicalFailureReason = technicalReason
        )

        val snapshotJson = createConfigurationSnapshotSafe(preliminaryResult)
        return preliminaryResult.copy(configurationSnapshotJson = snapshotJson)
    }

    private fun calculateSpeedIntervalTime(
        samples: List<RunSample>,
        targetStartKmh: Float,
        targetEndKmh: Float
    ): Float? {
        if (samples.size < 2) return null
        val startIndex = samples.indexOfFirst { it.speedKmh >= targetStartKmh }
        if (startIndex == -1) return null

        val endIndex = samples.indexOfFirst { it.speedKmh >= targetEndKmh }
        if (endIndex == -1 || endIndex <= startIndex) return null

        val startSample = samples[startIndex]
        val endSample = samples[endIndex]

        val timeDiff = (endSample.elapsedSeconds - startSample.elapsedSeconds).jsonSafe(0f)
        return if (timeDiff > 0.1f && timeDiff < 60.0f) timeDiff else null
    }
}
