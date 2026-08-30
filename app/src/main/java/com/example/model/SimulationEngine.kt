package com.example.model

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object SimulationEngine {

    /**
     * Realiza os cálculos físicos e termodinâmicos para simulação virtual da curva de potência e torque.
     * Totalmente imune a divisões por zero, NaN e valores inválidos.
     */
    fun simulate(
        displacementCc: Int,
        cylinderCount: Int,
        aspiration: AspirationType,
        boostBar: Float,
        injectorFlowLbH: Float,
        fuelType: FuelType,
        revLimitRpm: Int = 6800,
        vehicleWeightKg: Float = 1350f
    ): SimulationUiState {
        // Validação e sanitização segura
        val safeDisplacement = displacementCc.coerceIn(500, 10000)
        val safeCylinders = cylinderCount.coerceIn(1, 16)
        val safeBoost = if (boostBar.isFinite() && boostBar >= 0f) {
            if (aspiration == AspirationType.NATURALLY_ASPIRATED) 0f else boostBar.coerceIn(0f, 4.5f)
        } else 0f
        val safeInjectors = if (injectorFlowLbH.isFinite() && injectorFlowLbH > 0f) injectorFlowLbH.coerceIn(10f, 250f) else 28f
        val safeRevLimit = revLimitRpm.coerceIn(4000, 10000)
        val safeWeight = if (vehicleWeightKg.isFinite() && vehicleWeightKg > 400f) vehicleWeightKg else 1350f

        // Pressão absoluta do coletor em bar (1.0 bar atmosférico + boost)
        val mapBar = 1.0f + safeBoost

        // Eficiência volumétrica base dependendo do tipo de aspiração
        val peakVe = when (aspiration) {
            AspirationType.NATURALLY_ASPIRATED -> 0.88f
            AspirationType.TURBOCHARGED -> 0.95f
            AspirationType.SUPERCHARGED -> 0.92f
        }

        val rpmPoints = mutableListOf<SimulationRpmPoint>()
        var maxPowerCv = 0f
        var maxTorqueKgm = 0f
        var peakDutyCycle = 0f

        val stepRpm = 250
        for (rpm in 1500..safeRevLimit step stepRpm) {
            val normalizedRpm = rpm.toFloat() / safeRevLimit.toFloat()

            // Curva de VE realista (sino centrado no torque máximo ~65-75% da rotação)
            val veFactor = when {
                normalizedRpm < 0.35f -> 0.70f + (normalizedRpm / 0.35f) * 0.22f
                normalizedRpm < 0.70f -> 0.92f + ((normalizedRpm - 0.35f) / 0.35f) * 0.08f
                else -> 1.0f - ((normalizedRpm - 0.70f) / 0.30f).pow(1.5f) * 0.18f
            }
            val currentVe = (peakVe * veFactor).coerceIn(0.50f, 1.15f)

            // Densidade do ar e fluxo mássico de ar (kg/h)
            val airDensityKgM3 = 1.184f * mapBar
            val displacementM3 = safeDisplacement / 1_000_000.0f
            val airFlowKgH = (displacementM3 * (rpm / 120.0f) * currentVe * airDensityKgM3 * 3600.0f).toFloat()

            // Potência estimada a partir do fluxo de ar (Brake Specific Fuel Consumption e equivalência térmica)
            // 1 kg de ar gera aproximadamente ~0.080 - 0.088 CV dependendo do combustível
            val powerCoeff = when (fuelType) {
                FuelType.ETHANOL -> 0.089f
                FuelType.PREMIUM_GASOLINE -> 0.086f
                FuelType.GASOLINE -> 0.084f
                FuelType.E25 -> 0.085f
                FuelType.DIESEL -> 0.080f
            }

            val powerCv = (airFlowKgH * powerCoeff).coerceAtLeast(5.0f)
            // Torque (kgf.m) = (CV * 716.2) / RPM
            val torqueKgm = if (rpm > 0) ((powerCv * 716.2f) / rpm.toFloat()).coerceAtLeast(1.0f) else 0f

            // Cálculo do Duty Cycle dos bicos injetores
            // Vazão total dos bicos em lb/h convertida para combustível necessário
            val bsfcLbHp = if (fuelType == FuelType.ETHANOL) 0.65f else 0.48f
            val requiredFuelLbH = powerCv * bsfcLbHp
            val totalInjectorCapacityLbH = safeInjectors * safeCylinders
            val dutyCycle = if (totalInjectorCapacityLbH > 0f) {
                ((requiredFuelLbH / totalInjectorCapacityLbH) * 100.0f).coerceIn(0f, 150f)
            } else 0f

            if (powerCv > maxPowerCv) maxPowerCv = powerCv
            if (torqueKgm > maxTorqueKgm) maxTorqueKgm = torqueKgm
            if (dutyCycle > peakDutyCycle) peakDutyCycle = dutyCycle

            rpmPoints.add(
                SimulationRpmPoint(
                    rpm = rpm,
                    powerCv = powerCv,
                    torqueKgm = torqueKgm,
                    boostBar = safeBoost,
                    volumetricEfficiencyPercent = currentVe * 100.0f,
                    injectorDutyCyclePercent = dutyCycle
                )
            )
        }

        // Potência na roda estimada (~15% perda de transmissão)
        val wheelPowerCv = maxPowerCv * 0.85f

        // Estimativas de 0-100 e 1/4 de milha por relação peso/potência
        val pwrRatioKgCv = safeWeight / max(maxPowerCv, 10f)
        val estimated0to100 = (pwrRatioKgCv * 0.82f + 1.2f).coerceIn(2.5f, 25.0f)
        val estimatedQuarterMile = (5.825f * (safeWeight / max(maxPowerCv, 10f)).pow(0.333f)).coerceIn(8.5f, 25.0f)

        return SimulationUiState(
            isLoading = false,
            engineDisplacementCc = safeDisplacement,
            cylinderCount = safeCylinders,
            aspiration = aspiration,
            boostBar = safeBoost,
            injectorFlowLbH = safeInjectors,
            fuelType = fuelType,
            targetRpm = safeRevLimit,
            estimatedPowerCv = maxPowerCv,
            estimatedWheelPowerCv = wheelPowerCv,
            estimatedTorqueKgm = maxTorqueKgm,
            estimatedZeroToHundredSec = estimated0to100,
            estimatedQuarterMileSec = estimatedQuarterMile,
            injectorDutyCyclePercent = peakDutyCycle,
            curvePoints = rpmPoints,
            errorMessage = null
        )
    }
}
