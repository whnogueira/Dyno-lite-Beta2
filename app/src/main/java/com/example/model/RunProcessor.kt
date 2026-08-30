package com.example.model

import android.util.Log
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

fun Float.finiteOrNull(): Float? = if (isFinite()) this else null
fun Float?.finiteOrDefault(default: Float = 0f): Float = if (this != null && this.isFinite()) this else default

fun Double.finiteOrNull(): Double? = if (isFinite()) this else null
fun Double?.finiteOrDefault(default: Double = 0.0): Double = if (this != null && this.isFinite()) this else default

object RunProcessor {
    private const val TAG = "DynoMobile"

    fun sanitizeSample(sample: RunSample): RunSample {
        return sample.copy(
            timestampNs = if (sample.timestampNs > 0L) sample.timestampNs else System.nanoTime(),
            elapsedSeconds = sample.elapsedSeconds.finiteOrDefault(0f).coerceAtLeast(0f),
            speedKmh = sample.speedKmh.finiteOrDefault(0f).coerceAtLeast(0f),
            accelerationMps2 = sample.accelerationMps2.finiteOrDefault(0f),
            longitudinalG = sample.longitudinalG.finiteOrDefault(0f).coerceIn(-3.0f, 3.0f),
            estimatedRpm = sample.estimatedRpm.coerceIn(0, 15000),
            wheelPowerCv = sample.wheelPowerCv.finiteOrDefault(0f).coerceAtLeast(0f),
            enginePowerCv = sample.enginePowerCv.finiteOrDefault(0f).coerceAtLeast(0f),
            wheelTorqueKgm = sample.wheelTorqueKgm.finiteOrDefault(0f).coerceAtLeast(0f),
            engineTorqueKgm = sample.engineTorqueKgm.finiteOrDefault(0f).coerceAtLeast(0f),
            aeroLossCv = sample.aeroLossCv.finiteOrDefault(0f).coerceAtLeast(0f),
            rollLossCv = sample.rollLossCv.finiteOrDefault(0f).coerceAtLeast(0f),
            drivetrainLossCv = sample.drivetrainLossCv.finiteOrDefault(0f).coerceAtLeast(0f),
            inertialLossCv = sample.inertialLossCv.finiteOrDefault(0f).coerceAtLeast(0f),
            gpsAccuracyMeters = sample.gpsAccuracyMeters.finiteOrDefault(99f).coerceAtLeast(0f),
            latitude = sample.latitude.finiteOrDefault(0.0),
            longitude = sample.longitude.finiteOrDefault(0.0)
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

        // 1. Sanitização rigorosa de cada amostra
        val sanitizedSamples = rawSamples.map { sanitizeSample(it) }

        val startTimestampNs = sanitizedSamples.firstOrNull()?.timestampNs ?: 0L
        val endTimestampNs = sanitizedSamples.lastOrNull()?.timestampNs ?: startTimestampNs
        val computedDuration = if (endTimestampNs > startTimestampNs) {
            ((endTimestampNs - startTimestampNs) / 1_000_000_000f).finiteOrDefault(0f)
        } else {
            (sanitizedSamples.size * 0.05f)
        }
        val durationSeconds = durationOverride?.finiteOrDefault(computedDuration)?.coerceAtLeast(0.01f) ?: computedDuration.coerceAtLeast(0.01f)

        val startSpeedKmh = sanitizedSamples.firstOrNull()?.speedKmh ?: 0f
        val endSpeedKmh = sanitizedSamples.lastOrNull()?.speedKmh ?: 0f
        val maxSpeedKmh = sanitizedSamples.maxOfOrNull { it.speedKmh } ?: 0f

        Log.d(TAG, "Processando sessão $sessionId: ${sanitizedSamples.size} amostras | Vel Inicial: $startSpeedKmh | Vel Máx: $maxSpeedKmh | Duração: $durationSeconds s")

        // 2. Cálculo / Recálculo de Potência e Torque para amostras que possam estar zeradas ou inconsistentes
        var isPartialResult = false
        var technicalReason: String? = null

        val processedSamples = sanitizedSamples.mapIndexed { index, sample ->
            var samplePower = sample.enginePowerCv
            var sampleWheelPower = sample.wheelPowerCv
            var sampleTorque = sample.engineTorqueKgm

            // Se amostra não tem potência calculada válida, tentar derivar fisicamente
            if (samplePower <= 0f && sample.speedKmh > 5f && sample.longitudinalG > 0f) {
                try {
                    val speedMps = (sample.speedKmh / 3.6f).finiteOrDefault(0f)
                    val massKg = effectiveVehicle.totalMassKg.coerceIn(400f, 10000f)
                    val accelMps2 = (sample.longitudinalG * 9.81f).finiteOrDefault(0f).coerceAtLeast(0f)

                    val areaM2 = effectiveVehicle.frontalAreaM2.finiteOrDefault(2.15f).coerceIn(1.0f, 5.0f)
                    val cd = effectiveVehicle.dragCoefficientCd.finiteOrDefault(0.31f).coerceIn(0.15f, 1.2f)
                    val fAero = 0.5f * 1.2f * areaM2 * cd * speedMps.pow(2)
                    val fRoll = massKg * 9.81f * 0.015f
                    val fInertia = massKg * accelMps2
                    val fTotal = (fInertia + fAero + fRoll).coerceAtLeast(0f)

                    val wheelWatts = fTotal * speedMps
                    val wheelCv = ((wheelWatts / 735.5f)).finiteOrDefault(0f)
                    val lossPct = effectiveVehicle.drivetrainLossPercent.finiteOrDefault(15f).coerceIn(0f, 50f)
                    val engCv = (wheelCv / max(1.0f - (lossPct / 100f), 0.5f)).finiteOrDefault(0f)

                    val rpm = sample.estimatedRpm.takeIf { it > 500 } ?: effectiveVehicle.calculateRpmFromSpeedKmh(sample.speedKmh)
                    val torqueKgm = if (rpm > 0) ((engCv * 716.2f) / rpm).finiteOrDefault(0f) else 0f

                    sampleWheelPower = wheelCv
                    samplePower = engCv
                    sampleTorque = torqueKgm
                } catch (e: Exception) {
                    Log.w(TAG, "Falha ao interpolar potência da amostra $index: ${e.message}")
                }
            }

            sample.copy(
                enginePowerCv = samplePower.finiteOrDefault(0f),
                wheelPowerCv = sampleWheelPower.finiteOrDefault(0f),
                engineTorqueKgm = sampleTorque.finiteOrDefault(0f)
            )
        }

        var peakEngineCv = processedSamples.maxOfOrNull { it.enginePowerCv.finiteOrDefault(0f) } ?: 0f
        var peakWheelCv = processedSamples.maxOfOrNull { it.wheelPowerCv.finiteOrDefault(0f) } ?: 0f
        var peakTorque = processedSamples.maxOfOrNull { it.engineTorqueKgm.finiteOrDefault(0f) } ?: 0f
        val peakG = processedSamples.maxOfOrNull { it.longitudinalG.finiteOrDefault(0f) } ?: 0f

        val peakSample = processedSamples.maxByOrNull { it.enginePowerCv.finiteOrDefault(0f) }
        val peakRpm = peakSample?.estimatedRpm?.coerceIn(0, 15000) ?: 0
        val peakSpeed = peakSample?.speedKmh?.finiteOrDefault(0f) ?: 0f

        val peakTorqueSample = processedSamples.maxByOrNull { it.engineTorqueKgm.finiteOrDefault(0f) }
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

        Log.i(TAG, "Resultado da Sessão $sessionId calculado com sucesso -> Potência: $peakEngineCv cv @ $peakRpm RPM | Torque: $peakTorque kgfm | G: $peakG")

        return RunResult(
            id = sessionId,
            vehicleId = effectiveVehicle.id,
            vehicleName = effectiveVehicle.name,
            testDateTimestamp = System.currentTimeMillis(),
            peakEnginePowerCv = peakEngineCv.finiteOrNull() ?: 0f,
            peakEnginePowerRpm = peakRpm,
            peakEnginePowerSpeedKmh = peakSpeed.finiteOrNull() ?: 0f,
            peakWheelPowerCv = peakWheelCv.finiteOrNull() ?: 0f,
            peakEngineTorqueKgm = peakTorque.finiteOrNull() ?: 0f,
            peakEngineTorqueRpm = peakTorqueRpm,
            peakLongitudinalG = peakG.finiteOrNull() ?: 0f,
            startSpeedKmh = startSpeedKmh.finiteOrNull() ?: 0f,
            endSpeedKmh = endSpeedKmh.finiteOrNull() ?: 0f,
            testGear = (effectiveVehicle.testGearIndex + 1).coerceIn(1, 8),
            durationSeconds = durationSeconds.finiteOrNull() ?: 0f,
            zeroToHundredSeconds = t0to100?.finiteOrNull(),
            eightyToOneTwentySeconds = t80to120?.finiteOrNull(),
            oneHundredToTwoHundredSeconds = t100to200?.finiteOrNull(),
            quarterMileSeconds = null,
            quarterMileSpeedKmh = null,
            temperatureCelsius = 25.0f,
            pressureHpa = 1013.25f,
            saeCorrectionFactor = 1.0f,
            samples = processedSamples,
            qualityStatus = if (isPartialResult) "DADOS INSUFICIENTES" else "VALID",
            technicalFailureReason = technicalReason
        )
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

        val timeDiff = (endSample.elapsedSeconds - startSample.elapsedSeconds).finiteOrDefault(0f)
        return if (timeDiff > 0.1f && timeDiff < 60.0f) timeDiff else null
    }
}
