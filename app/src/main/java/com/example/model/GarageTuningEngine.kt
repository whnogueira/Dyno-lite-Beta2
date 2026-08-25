package com.example.model

import java.util.Locale
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GarageTuningEngine {

  const val GRAVITY = 9.80665f
  const val DELTA_T = 0.02f // 20ms steps
  const val MAX_SIM_TIME = 45.0f

  // ---------------------------------------------------------------------------------------
  // 1. CÁLCULOS DE BICOS E ALIMENTAÇÃO (Seções 5, 6, 7)
  // ---------------------------------------------------------------------------------------

  /**
   * Correção da vazão do bico pela pressão da linha de combustível
   * novaVazao = vazaoOriginal * sqrt(novaPressao / pressaoOriginal)
   */
  fun calculateCorrectedInjectorFlow(
    nominalFlowLbHr: Float,
    nominalPressureBar: Float,
    operatingPressureBar: Float
  ): Float {
    val baseP = nominalPressureBar.coerceAtLeast(1.0f)
    val opP = operatingPressureBar.coerceAtLeast(1.0f)
    return nominalFlowLbHr * sqrt(opP / baseP)
  }

  /**
   * Conversão de lb/h para cc/min com ajuste pela densidade do combustível
   */
  fun convertLbHrToCcMin(flowLbHr: Float, fuel: FuelTypeOption): Float {
    val gasFactor = 10.5f
    val densityRatio = fuel.densityKgL / 0.74f
    return flowLbHr * gasFactor * densityRatio
  }

  /**
   * Potência máxima suportada pelos bicos injetores (cv)
   * HP = (vazaoLbHr * qtdBicos * dutyCycleMax) / BSFC
   * CV = HP * 1.01387
   */
  fun calculateMaxSupportedPowerByInjectorsCv(
    correctedFlowLbHr: Float,
    injectorCount: Int,
    maxDutyCyclePercent: Float,
    bsfc: Float
  ): Float {
    val dutyDecimal = (maxDutyCyclePercent / 100f).coerceIn(0.1f, 1.0f)
    val safeBsfc = bsfc.coerceAtLeast(0.35f)
    val hpSupported = (correctedFlowLbHr * injectorCount * dutyDecimal) / safeBsfc
    return hpSupported * 1.01387f
  }

  /**
   * Uso percentual real dos bicos injetores para uma potência solicitada
   * Duty % = (potenciaSolicitadaHp * BSFC) / (vazaoLbHr * qtdBicos) * 100
   */
  fun calculateInjectorDutyCyclePercent(
    demandedEngineHp: Float,
    correctedFlowLbHr: Float,
    injectorCount: Int,
    bsfc: Float
  ): Float {
    val totalCapacity = (correctedFlowLbHr * injectorCount).coerceAtLeast(1.0f)
    return ((demandedEngineHp * bsfc) / totalCapacity) * 100f
  }

  /**
   * Vazão de bomba de combustível necessária em L/h para uma potência no motor
   */
  fun calculateRequiredPumpFlowLph(
    engineHp: Float,
    bsfc: Float,
    fuelDensityKgL: Float
  ): Float {
    // 1 lb = 0.453592 kg
    val fuelMassKgPerHour = engineHp * bsfc * 0.453592f
    val volumeLph = fuelMassKgPerHour / fuelDensityKgL.coerceAtLeast(0.6f)
    return volumeLph * 1.20f // 20% de margem de retorno
  }

  // ---------------------------------------------------------------------------------------
  // 2. CÁLCULO COMPLETO DA PREPARAÇÃO (MOTOR, ASPIRAÇÃO, LIMITES, GARGALOS)
  // ---------------------------------------------------------------------------------------

  fun calculateTuningBuild(build: TuningBuild): TuningCalculationResult {
    val isTurbo = build.aspiration != AspirationType.ASPIRADO && build.aspiration != AspirationType.SUPERCHARGER
    val isSupercharger = build.aspiration == AspirationType.SUPERCHARGER

    // 1. Determina o BSFC do combustível e aspiração
    val bsfc = if (isTurbo || isSupercharger) build.fuelType.bsfcTurbo else build.fuelType.bsfcNa

    // 2. Correção de fluxo dos bicos injetores
    val correctedFlowLbHr = calculateCorrectedInjectorFlow(
      nominalFlowLbHr = build.injectorFlowLbHr,
      nominalPressureBar = build.injectorBasePressureBar,
      operatingPressureBar = build.injectorOperatingPressureBar
    )
    val correctedFlowCcMin = convertLbHrToCcMin(correctedFlowLbHr, build.fuelType)

    // Capacidade máxima dos bicos em CV
    val maxInjectorsCv = calculateMaxSupportedPowerByInjectorsCv(
      correctedFlowLbHr = correctedFlowLbHr,
      injectorCount = build.injectorCount,
      maxDutyCyclePercent = build.maxInjectorDutyCyclePercent,
      bsfc = bsfc
    )

    // 3. Capacidade da bomba de combustível
    val totalPumpCapacityLph = build.fuelPumpFlowLph * build.fuelPumpCount
    // Limite em HP suportado pela bomba (considerando 1.20 de retorno)
    val maxPumpHp = (totalPumpCapacityLph * build.fuelType.densityKgL) / (bsfc * 0.453592f * 1.15f)
    val maxPumpCv = maxPumpHp * 1.01387f

    // 4. Potência Base Aspirada Modificada (Cálculo de Admissão, Cabeçote, Comando, Escape, Taxa)
    val displacementFactor = build.displacementCc.toFloat() / 2000f
    var baseNaPowerCv = build.factoryEnginePowerCv
    var baseNaTorqueKgfm = build.factoryEngineTorqueKgfm

    // Aporte do Cabeçote
    val headGainPercent = build.cylinderHead.flowGainPercent
    baseNaPowerCv *= (1.0f + (headGainPercent / 100f) * 0.85f)

    // Aporte do Comando de Válvulas
    val camGain = build.camshaft.highRpmPowerGain * (build.displacementCc / 2000f)
    val camTorqueMod = build.camshaft.lowRpmTorqueMod * (build.displacementCc / 2000f)
    baseNaPowerCv += camGain
    baseNaTorqueKgfm += camTorqueMod

    // Aporte da Admissão e TBI
    val intakeGain = build.intake.powerGainHp * 1.01387f
    baseNaPowerCv += intakeGain

    // Aporte do Escape
    val headerGain = build.exhaustHeader.highRpmPowerGain * 1.01387f
    val headerTorqueMod = build.exhaustHeader.lowRpmTorqueGain
    baseNaPowerCv += headerGain
    baseNaTorqueKgfm += headerTorqueMod

    // Aporte do Aumento de Taxa de Compressão (beneficia combustíveis de alta octanagem como Etanol)
    val totalCompression = build.baseCompressionRatio + build.extraCompressionRatio
    val compressionDelta = (totalCompression - build.baseCompressionRatio).coerceAtLeast(0f)
    val octaneBonus = if (build.fuelType == FuelTypeOption.ETANOL || build.fuelType == FuelTypeOption.E85 || build.fuelType == FuelTypeOption.METANOL) 1.25f else 0.85f
    val compressionGainFactor = 1.0f + (compressionDelta * 0.035f * octaneBonus)
    baseNaPowerCv *= compressionGainFactor
    baseNaTorqueKgfm *= compressionGainFactor

    // 5. Dimensionamento de Aspiração / Turbo / Supercharger
    var targetBoostBar = 0.0f
    var turboSpoolRpm = build.turboSpoolStartRpm
    var turboFullRpm = build.turboFullBoostRpm
    var maxTurboFlowCv = 1200f
    var turboEff = build.turboEfficiency

    when (build.aspiration) {
      AspirationType.ASPIRADO -> {
        targetBoostBar = 0.0f
      }
      AspirationType.TURBO_PEQUENO -> {
        targetBoostBar = if (build.turboBoostBar > 0f) build.turboBoostBar else 0.6f
        turboSpoolRpm = 2100
        turboFullRpm = 2900
        maxTurboFlowCv = 270f
        turboEff = 0.82f
      }
      AspirationType.TURBO_MEDIO -> {
        targetBoostBar = if (build.turboBoostBar > 0f) build.turboBoostBar else 0.9f
        turboSpoolRpm = 3000
        turboFullRpm = 3800
        maxTurboFlowCv = 440f
        turboEff = 0.86f
      }
      AspirationType.TURBO_GRANDE -> {
        targetBoostBar = if (build.turboBoostBar > 0f) build.turboBoostBar else 1.5f
        turboSpoolRpm = 4100
        turboFullRpm = 4900
        maxTurboFlowCv = 720f
        turboEff = 0.88f
      }
      AspirationType.SUPERCHARGER -> {
        targetBoostBar = if (build.turboBoostBar > 0f) build.turboBoostBar else 0.6f
        turboSpoolRpm = 1200
        turboFullRpm = 2000
        maxTurboFlowCv = 380f
        turboEff = 0.78f // Consome potência no virabrequim
      }
      AspirationType.TURBO_CUSTOM -> {
        targetBoostBar = build.turboBoostBar.coerceAtLeast(0.1f)
        turboSpoolRpm = build.turboSpoolStartRpm
        turboFullRpm = build.turboFullBoostRpm
        maxTurboFlowCv = build.turboMaxFlowHp * 1.01387f
        turboEff = build.turboEfficiency
      }
    }

    // Intercooler: Ganho de densidade do ar e redução de temperatura (IAT)
    var intercoolerDensityGain = 1.0f
    var intercoolerMaxHpLimit = 1200f
    if (isTurbo || isSupercharger) {
      intercoolerDensityGain = 1.0f + (build.intercooler.tempDropC * 0.0025f)
      intercoolerMaxHpLimit = build.intercooler.maxHpEfficiency * 1.01387f
      // Queda de pressão no intercooler
      targetBoostBar = (targetBoostBar - build.intercooler.pressureDropBar).coerceAtLeast(0.0f)
    }

    // Potência Teórica com Turbo/Supercharger antes dos gargalos
    var unconstrainedPowerCv = if (isTurbo) {
      val pressureAbs = 1.0f + targetBoostBar
      val airMult = 1.0f + ((pressureAbs * turboEff - 1.0f).coerceAtLeast(0f) * intercoolerDensityGain)
      baseNaPowerCv * airMult
    } else if (isSupercharger) {
      val pressureAbs = 1.0f + targetBoostBar
      val superchargerDrawCv = 18f * (targetBoostBar / 0.6f)
      (baseNaPowerCv * (1.0f + targetBoostBar * 0.75f) * intercoolerDensityGain) - superchargerDrawCv
    } else {
      baseNaPowerCv
    }

    // Ajuste de Ignição e Acerto da ECU
    unconstrainedPowerCv *= build.ecu.tunePrecisionFactor
    unconstrainedPowerCv *= build.tuneMap.powerGainFactor
    if (build.timingAdvanceDegrees > 0f) {
      unconstrainedPowerCv *= (1.0f + (build.timingAdvanceDegrees * 0.008f))
    }

    // 6. APLICAÇÃO RÍGIDA DOS GARGALOS (O componente mais fraco limita o resultado final!)
    val bottlenecks = mutableListOf<String>()
    val missingParts = mutableListOf<String>()
    var primaryBottleneck = "Nenhum gargalo severo identificado"
    var primaryBottleneckDesc = "O conjunto mecânico está bem balanceado para a potência atual."

    // Limites de fluxo dos componentes
    val exhaustFlowLimitCv = build.exhaustSystem.maxHpFlow * 1.01387f
    val throttleBodyLimitCv = build.throttleBody.maxHpFlow * 1.01387f
    val cylinderHeadLimitCv = build.cylinderHead.maxHpFlow * 1.01387f

    // Limite estrutural dos internos do motor (pistões, bielas, parafusos, junta)
    val pistonLimitCv = build.pistons.maxHpLimit * 1.01387f
    val rodsLimitCv = build.rods.maxHpLimit * 1.01387f
    val studsLimitCv = build.studs.maxHpLimit * 1.01387f
    val crankshaftLimitCv = build.crankshaft.maxHpLimit * 1.01387f
    val engineStructuralLimitHp = min(min(pistonLimitCv, rodsLimitCv), min(studsLimitCv, crankshaftLimitCv))

    var constrainedPowerCv = unconstrainedPowerCv

    // Gargalo 1: Bicos Injetores
    if (constrainedPowerCv > maxInjectorsCv) {
      bottlenecks.add("Bicos Injetores: Capacidade máxima de ${String.format(Locale.US, "%.0f", maxInjectorsCv)} cv atingida.")
      if (constrainedPowerCv - maxInjectorsCv > 15f) {
        primaryBottleneck = "Bicos Injetores (Alimentação)"
        primaryBottleneckDesc = "Os bicos atuais de ${build.injectorFlowLbHr} lb/h não fornecem vazão suficiente para a demanda de ar. Aumente a vazão dos bicos ou a pressão de linha."
      }
      constrainedPowerCv = maxInjectorsCv
    }

    // Gargalo 2: Bomba de Combustível
    if (constrainedPowerCv > maxPumpCv) {
      bottlenecks.add("Bomba de Combustível: Limite de vazão de ${String.format(Locale.US, "%.0f", maxPumpCv)} cv.")
      if (constrainedPowerCv - maxPumpCv > 10f) {
        primaryBottleneck = "Bomba de Combustível"
        primaryBottleneckDesc = "A bomba de ${build.fuelPumpFlowLph} L/h não consegue manter a vazão e pressão necessárias em carga máxima."
      }
      constrainedPowerCv = min(constrainedPowerCv, maxPumpCv)
    }

    // Gargalo 3: Turbo/Compressor
    if (isTurbo && constrainedPowerCv > maxTurboFlowCv) {
      bottlenecks.add("Vazão do Turbo: Rotor no limite de fluxo (${String.format(Locale.US, "%.0f", maxTurboFlowCv)} cv).")
      primaryBottleneck = "Tamanho do Turbo"
      primaryBottleneckDesc = "O turbo atingiu sua rotação e vazão máximas (choke line). Para mais potência é necessário um turbo de carcaça maior."
      constrainedPowerCv = min(constrainedPowerCv, maxTurboFlowCv)
    }

    // Gargalo 4: Intercooler
    if (isTurbo && constrainedPowerCv > intercoolerMaxHpLimit) {
      bottlenecks.add("Intercooler: Saturação térmica em alta pressão.")
      constrainedPowerCv = min(constrainedPowerCv, intercoolerMaxHpLimit)
    }

    // Gargalo 5: Escape
    if (constrainedPowerCv > exhaustFlowLimitCv) {
      bottlenecks.add("Escape: Restrição de contrapressão no escapamento.")
      constrainedPowerCv = min(constrainedPowerCv, exhaustFlowLimitCv)
    }

    // Gargalo 6: ECU sem controle de turbo
    if (isTurbo && build.ecu == EcuType.ORIGINAL) {
      bottlenecks.add("ECU Original: Não gerencia mapa de pressão de turbo e corta injeção por sobrepressão.")
      missingParts.add("ECU Programável para gerenciar turbo")
      constrainedPowerCv = min(constrainedPowerCv, 160f)
      primaryBottleneck = "Injeção Eletrônica (ECU Original)"
      primaryBottleneckDesc = "A ECU de fábrica não possui sensor MAP para ler pressão positiva de turbo e cortará o sinal por sobrepressão."
    }

    // Alertas de Peças Obrigatórias Ausentes
    if (isTurbo) {
      if (build.intercooler == IntercoolerType.SEM_INTERCOOLER && targetBoostBar >= 0.6f) {
        missingParts.add("Intercooler frontal (arrefecimento de admissão)")
        bottlenecks.add("Temperatura de Admissão Elevada: Risco severo de detonação sem intercooler.")
      }
      if (build.ignitionCoil == IgnitionCoilType.ORIGINAL && targetBoostBar >= 0.8f) {
        missingParts.add("Bobinas de alta energia (Audi R8 ou MSD)")
        bottlenecks.add("Ignição: Bobina original pode soprar a centelha em pressão acima de 0.8 bar.")
      }
      if (build.sparkPlugs == SparkPlugType.ORIGINAL && targetBoostBar >= 0.5f) {
        missingParts.add("Velas com grau térmico frio (Grau 7/8)")
        bottlenecks.add("Velas Originais: Risco de pré-ignição por aquecimento do eletrodo.")
      }
      if (build.headGasket == HeadGasketType.ORIGINAL && targetBoostBar >= 0.8f) {
        missingParts.add("Junta de cabeçote reforçada MLS")
        bottlenecks.add("Junta de Cabeçote: Junta original pode queimar com pressão superior a 0.8 bar.")
      }
    }

    // 7. Torque Final e Rotações de Pico
    var peakPowerRpm = build.factoryPeakPowerRpm
    var peakTorqueRpm = build.factoryPeakTorqueRpm
    var effectiveRedlineRpm = build.customRedlineRpm ?: build.factoryRedlineRpm

    if (build.camshaft == CamshaftProfile.LEVE_260) {
      peakPowerRpm += 200
      peakTorqueRpm += 150
    } else if (build.camshaft == CamshaftProfile.MEDIO_276) {
      peakPowerRpm += 500
      peakTorqueRpm += 400
      effectiveRedlineRpm += 400
    } else if (build.camshaft == CamshaftProfile.BRAVO_288) {
      peakPowerRpm += 900
      peakTorqueRpm += 800
      effectiveRedlineRpm += 800
    }

    val finalEnginePowerCv = constrainedPowerCv.coerceAtLeast(40f)
    val finalEngineTorqueKgfm = if (isTurbo) {
      val boostTorqueMultiplier = 1.0f + (targetBoostBar * 0.82f)
      (baseNaTorqueKgfm * boostTorqueMultiplier * (finalEnginePowerCv / unconstrainedPowerCv)).coerceAtLeast(8f)
    } else {
      (baseNaTorqueKgfm * (finalEnginePowerCv / baseNaPowerCv)).coerceAtLeast(8f)
    }

    // Potência e Torque nas Rodas
    val drivetrainLossFraction = (build.drivetrainLossPercent / 100f).coerceIn(0.05f, 0.35f)
    val finalWheelPowerCv = finalEnginePowerCv * (1f - drivetrainLossFraction)
    val finalWheelTorqueKgfm = finalEngineTorqueKgfm * (1f - drivetrainLossFraction)

    // Uso dos Bicos e Bomba em Carga Máxima
    val finalEngineHp = finalEnginePowerCv / 1.01387f
    val injectorDutyPercent = calculateInjectorDutyCyclePercent(
      demandedEngineHp = finalEngineHp,
      correctedFlowLbHr = correctedFlowLbHr,
      injectorCount = build.injectorCount,
      bsfc = bsfc
    )

    val requiredPumpLph = calculateRequiredPumpFlowLph(
      engineHp = finalEngineHp,
      bsfc = bsfc,
      fuelDensityKgL = build.fuelType.densityKgL
    )
    val pumpUsagePercent = ((requiredPumpLph / totalPumpCapacityLph) * 100f).coerceIn(0f, 200f)

    // Status dos Bicos
    val injectorStatus = when {
      injectorDutyPercent <= 80f -> "Adequado (${String.format(Locale.US, "%.1f", injectorDutyPercent)}% duty)"
      injectorDutyPercent <= 90f -> "Próximo do limite (${String.format(Locale.US, "%.1f", injectorDutyPercent)}% duty)"
      injectorDutyPercent <= 100f -> "Insuficiente (${String.format(Locale.US, "%.1f", injectorDutyPercent)}% duty)"
      else -> "Configuração impossível (${String.format(Locale.US, "%.1f", injectorDutyPercent)}% duty)"
    }

    val pumpStatus = when {
      pumpUsagePercent <= 80f -> "Adequada (${String.format(Locale.US, "%.0f", requiredPumpLph)} L/h de ${String.format(Locale.US, "%.0f", totalPumpCapacityLph)} L/h)"
      pumpUsagePercent <= 95f -> "Próxima do limite (${String.format(Locale.US, "%.0f", requiredPumpLph)} L/h)"
      else -> "Insuficiente (${String.format(Locale.US, "%.0f", requiredPumpLph)} L/h necessários)"
    }

    // 8. Limites Estruturais e Embreagem
    val engineStressPercent = ((finalEnginePowerCv / engineStructuralLimitHp) * 100f).coerceIn(0f, 250f)
    val structuralRiskLevel = when {
      engineStressPercent <= 80f -> "Seguro (Margem mecânica excelente)"
      engineStressPercent <= 100f -> "Atenção (Próximo do limite dos internos originais)"
      engineStressPercent <= 125f -> "Alto Risco (Risco de quebra de biela/pistão)"
      else -> "Crítico (Quebra iminente sob carga plena)"
    }

    val clutchLimitKgfm = build.clutch.maxTorqueKgfm
    val isClutchSlipping = finalEngineTorqueKgfm > clutchLimitKgfm
    val clutchStressPercent = ((finalEngineTorqueKgfm / clutchLimitKgfm) * 100f).coerceIn(0f, 200f)
    if (isClutchSlipping) {
      bottlenecks.add("Embreagem: Torque do motor (${String.format(Locale.US, "%.1f", finalEngineTorqueKgfm)} kgfm) excede o limite da embreagem (${String.format(Locale.US, "%.1f", clutchLimitKgfm)} kgfm).")
      missingParts.add("Embreagem de cerâmica ou multidisco")
    }

    // 9. Nível do Projeto (Stage)
    val projectLevel = when {
      finalEnginePowerCv <= build.factoryEnginePowerCv + 10f && !isTurbo -> TuningProjectLevel.ORIGINAL
      finalEnginePowerCv < 170f && !isTurbo -> TuningProjectLevel.LEVE
      finalEnginePowerCv < 230f && (!isTurbo || targetBoostBar <= 0.6f) -> TuningProjectLevel.STREET
      finalEnginePowerCv < 320f -> TuningProjectLevel.STREET_FORTE
      finalEnginePowerCv < 450f -> TuningProjectLevel.TRACK
      finalEnginePowerCv < 650f -> TuningProjectLevel.COMPETICAO
      else -> TuningProjectLevel.EXTREMO
    }

    // 10. Pontuações de Jogo (0 a 100) (Seção 19)
    // Desempenho
    val perfScore = ((finalEnginePowerCv / (build.factoryEnginePowerCv * 3.5f)) * 100f).coerceIn(10f, 100f).toInt()
    
    // Resposta / Spool
    val spoolScore = if (isTurbo) {
      when (build.aspiration) {
        AspirationType.TURBO_PEQUENO -> 90
        AspirationType.TURBO_MEDIO -> 75
        AspirationType.TURBO_GRANDE -> 45
        else -> 60
      } - if (build.intercooler == IntercoolerType.GRANDE_COMPETICAO) 8 else 0
    } else if (isSupercharger) {
      95
    } else {
      (98 - (build.camshaft.durationDegrees - 240) * 0.4f).toInt().coerceIn(40, 100)
    }

    // Confiabilidade (0 a 100)
    var reliab = 100f
    if (engineStressPercent > 80f) reliab -= (engineStressPercent - 80f) * 1.5f
    if (injectorDutyPercent > 85f) reliab -= (injectorDutyPercent - 85f) * 1.8f
    if (pumpUsagePercent > 85f) reliab -= (pumpUsagePercent - 85f) * 1.2f
    if (build.tuneMap == TuneMapType.SEM_ACERTO) reliab -= 40f
    if (build.tuneMap == TuneMapType.POTENCIA_AGRESSIVO) reliab -= 15f
    if (build.fuelType == FuelTypeOption.GASOLINA_COMUM && targetBoostBar > 0.6f) reliab -= 25f
    if (isClutchSlipping) reliab -= 15f
    if (missingParts.isNotEmpty()) reliab -= (missingParts.size * 10f)
    val reliabilityScore = reliab.coerceIn(5f, 100f).toInt()

    // Custo-Benefício
    val costBenefitScore = if (finalEnginePowerCv > build.factoryEnginePowerCv) {
      val cvGain = finalEnginePowerCv - build.factoryEnginePowerCv
      val costPerCv = if (cvGain > 0) (calculateTotalPartsCost(build) / cvGain) else 999.0
      when {
        costPerCv < 60.0 -> 95
        costPerCv < 120.0 -> 82
        costPerCv < 200.0 -> 68
        costPerCv < 350.0 -> 50
        else -> 35
      }
    } else 80

    // Uso Diário
    var daily = 95f
    daily -= (build.camshaft.durationDegrees - 240) * 0.6f
    if (build.clutch == ClutchType.CERAMICA_4_PASTILHAS) daily -= 20f
    if (build.clutch == ClutchType.CERAMICA_6_PASTILHAS) daily -= 30f
    if (build.clutch == ClutchType.MULTIDISCO_CARBONO) daily -= 45f
    if (build.weightReduction == WeightReductionStage.PISTA_RADICAL) daily -= 40f
    if (build.weightReduction == WeightReductionStage.FIBRA_CARBONO) daily -= 50f
    if (build.exhaustSystem == ExhaustSystemType.RACING_3_POLEGADAS) daily -= 25f
    if (build.tireCompound == TireCompound.SLICK_ARRANCADA) daily -= 40f
    val dailyDriveScore = daily.coerceIn(10f, 100f).toInt()

    // 11. Orçamento e Próxima Melhoria Recomendada
    val partsCost = calculateTotalPartsCost(build)
    val laborCost = build.laborCostBrl
    val grandTotal = partsCost + laborCost
    val cvGained = (finalEnginePowerCv - build.factoryEnginePowerCv).coerceAtLeast(0f)
    val costPerCv = if (cvGained > 0.1f) grandTotal / cvGained else 0.0

    val nextUpgrade = when {
      missingParts.isNotEmpty() -> "Instalar itens essenciais pendentes: ${missingParts.first()}"
      injectorDutyPercent > 80f -> "Aumentar vazão dos bicos injetores para alimentar com segurança"
      pumpUsagePercent > 80f -> "Instalar bomba de combustível de maior vazão (ex: 255 L/h ou 340 L/h)"
      isClutchSlipping -> "Substituir por embreagem de cerâmica para suportar o torque"
      engineStressPercent > 90f -> "Instalar pistões e bielas forjadas para suportar mais pressão"
      build.ecu == EcuType.ORIGINAL && isTurbo -> "Adicionar ECU programável (FT550/InjePro) para controle de ponto e boost"
      !isTurbo && build.aspiration == AspirationType.ASPIRADO -> "Kit Turbo intermediário com intercooler"
      else -> "Ajuste fino de mapa de injeção em dinamômetro de rolos"
    }

    // 12. Massa Total do Veículo
    val currentTotalMass = (build.baseVehicleCurbWeightKg + build.driverWeightKg - build.weightReduction.weightRemovedKg).coerceAtLeast(500f)
    val weightToPower = currentTotalMass / finalEnginePowerCv

    // 13. Medidores Visuais
    val gauges = TuningGauges(
      airFlowPercent = ((finalEnginePowerCv / (if (isTurbo) maxTurboFlowCv else 250f)) * 100f).coerceIn(10f, 100f),
      fuelFlowPercent = injectorDutyPercent.coerceIn(5f, 120f),
      turboCapacityPercent = if (isTurbo) ((finalEnginePowerCv / maxTurboFlowCv) * 100f).coerceIn(0f, 100f) else 0f,
      injectorDutyPercent = injectorDutyPercent.coerceIn(0f, 120f),
      fuelPumpUsagePercent = pumpUsagePercent.coerceIn(0f, 120f),
      engineStressPercent = engineStressPercent.coerceIn(0f, 150f),
      clutchStressPercent = clutchStressPercent.coerceIn(0f, 150f),
      gripPercent = (build.tireCompound.frictionMu / 1.40f * 100f).coerceIn(20f, 100f),
      reliabilityPercent = reliabilityScore.toFloat()
    )

    // 14. Geração das Curvas por RPM (Curva de Potência, Torque, Boost e Bicos)
    val powerCurve = mutableListOf<Pair<Int, Float>>()
    val torqueCurve = mutableListOf<Pair<Int, Float>>()
    val boostCurve = mutableListOf<Pair<Int, Float>>()
    val dutyCurve = mutableListOf<Pair<Int, Float>>()

    val stepRpm = 200
    val startRpm = 1000
    val endRpm = effectiveRedlineRpm + 200

    for (rpm in startRpm..endRpm step stepRpm) {
      // Curva de Pressão de Turbo por RPM
      val localBoost = if (isTurbo && targetBoostBar > 0f) {
        when {
          rpm < turboSpoolRpm -> 0.0f
          rpm < turboFullRpm -> {
            val progress = (rpm - turboSpoolRpm).toFloat() / (turboFullRpm - turboSpoolRpm)
            targetBoostBar * progress.pow(1.5f)
          }
          rpm <= peakPowerRpm -> targetBoostBar
          else -> {
            val drop = (rpm - peakPowerRpm).toFloat() / (effectiveRedlineRpm - peakPowerRpm).coerceAtLeast(500)
            targetBoostBar * (1.0f - 0.18f * drop.pow(1.3f)).coerceAtLeast(0.5f)
          }
        }
      } else if (isSupercharger) {
        targetBoostBar * (rpm.toFloat() / effectiveRedlineRpm).coerceIn(0.5f, 1.0f)
      } else 0.0f

      // Perfil de Torque por RPM
      val normRpm = rpm.toFloat()
      val torqueShape = if (normRpm <= peakTorqueRpm) {
        val f = (normRpm / peakTorqueRpm).coerceIn(0.3f, 1.0f)
        0.60f + 0.40f * sin(f * (Math.PI.toFloat() / 2f))
      } else {
        val span = (effectiveRedlineRpm - peakTorqueRpm).coerceAtLeast(1000).toFloat()
        val drop = ((normRpm - peakTorqueRpm) / span).coerceIn(0f, 1.5f)
        1.0f - 0.32f * drop.pow(1.5f)
      }

      val localBaseTorque = finalEngineTorqueKgfm * torqueShape
      val localBoostFactor = if (isTurbo && targetBoostBar > 0f) (1.0f + localBoost * 0.80f) / (1.0f + targetBoostBar * 0.80f) else 1.0f
      val currentRpmTorque = (localBaseTorque * localBoostFactor).coerceAtLeast(1.0f)
      val currentRpmPower = ((currentRpmTorque * rpm) / 716.2f).coerceAtLeast(0f)
      val localHp = currentRpmPower / 1.01387f
      val localDuty = calculateInjectorDutyCyclePercent(localHp, correctedFlowLbHr, build.injectorCount, bsfc).coerceIn(0f, 150f)

      powerCurve.add(rpm to currentRpmPower)
      torqueCurve.add(rpm to currentRpmTorque)
      boostCurve.add(rpm to localBoost)
      dutyCurve.add(rpm to localDuty)
    }

    return TuningCalculationResult(
      build = build,
      projectLevel = projectLevel,
      estimatedEnginePowerCv = finalEnginePowerCv,
      estimatedWheelPowerCv = finalWheelPowerCv,
      estimatedEngineTorqueKgfm = finalEngineTorqueKgfm,
      estimatedWheelTorqueKgfm = finalWheelTorqueKgfm,
      peakPowerRpm = peakPowerRpm,
      peakTorqueRpm = peakTorqueRpm,
      effectiveRedlineRpm = effectiveRedlineRpm,
      actualBoostBar = targetBoostBar,
      totalVehicleMassKg = currentTotalMass,
      weightToPowerRatioKgCv = weightToPower,
      correctedInjectorFlowLbHr = correctedFlowLbHr,
      correctedInjectorFlowCcMin = correctedFlowCcMin,
      maxSupportedPowerByInjectorsCv = maxInjectorsCv,
      injectorDutyCyclePercent = injectorDutyPercent,
      injectorStatusDescription = injectorStatus,
      requiredFuelPumpFlowLph = requiredPumpLph,
      availableFuelPumpFlowLph = totalPumpCapacityLph,
      fuelPumpUsagePercent = pumpUsagePercent,
      fuelPumpStatusDescription = pumpStatus,
      engineStructuralLimitHp = engineStructuralLimitHp,
      engineStressPercent = engineStressPercent,
      structuralRiskLevel = structuralRiskLevel,
      clutchTorqueCapacityKgfm = clutchLimitKgfm,
      isClutchSlipping = isClutchSlipping,
      primaryBottleneckTitle = primaryBottleneck,
      primaryBottleneckDescription = primaryBottleneckDesc,
      allWarnings = bottlenecks,
      performanceScore = perfScore,
      spoolResponseScore = spoolScore,
      reliabilityScore = reliabilityScore,
      costBenefitScore = costBenefitScore,
      dailyDriveScore = dailyDriveScore,
      totalPartsCostBrl = partsCost,
      totalLaborCostBrl = laborCost,
      grandTotalCostBrl = grandTotal,
      costPerCvGainedBrl = costPerCv,
      missingMandatoryParts = missingParts,
      nextRecommendedUpgrade = nextUpgrade,
      gauges = gauges,
      powerCurvePoints = powerCurve,
      torqueCurvePoints = torqueCurve,
      boostCurvePoints = boostCurve,
      injectorDutyCurvePoints = dutyCurve
    )
  }

  // ---------------------------------------------------------------------------------------
  // 3. SIMULAÇÃO DE ACELERAÇÃO E DINAMÔMETRO DE PISTA (Seção 17)
  // ---------------------------------------------------------------------------------------

  fun runDynoTrackSimulation(result: TuningCalculationResult): DynoTestAccelerationResult {
    val build = result.build
    val totalMass = result.totalVehicleMassKg
    val finalDrive = build.finalDriveRatio
    val gearRatios = build.gearRatios
    val maxRpm = result.effectiveRedlineRpm
    val drivetrainLoss = build.drivetrainLossPercent / 100f

    // Pneus
    val tire = VehicleCalculations.calculateTireDimensions(build.tireWidthMm, build.tireAspectRatio, build.rimInches)
    val tireCircM = tire.circumferenceM.toFloat()
    val tireDynamicRadiusM = (tire.totalDiameterMm / 2000.0).toFloat() * 0.97f

    // Aerodinâmica
    val cd = build.baseCd + build.aero.dragCdDelta
    val area = build.baseFrontalAreaM2 + build.aero.frontalAreaDelta
    val airDensity = 1.225f
    val crr = 0.015f

    // Limite de Aderência (Grip)
    val driveAxleWeightRatio = build.baseDrivetrain.weightOnDriveAxlePercent
    val baseGripMu = build.tireCompound.frictionMu + build.aero.topSpeedGripBonus
    val maxGripForceN = totalMass * driveAxleWeightRatio * GRAVITY * baseGripMu

    var time = 0.0f
    var distance = 0.0f
    var speedMs = 0.05f // velocidade inicial
    var currentGearIdx = 0
    var isShifting = false
    var shiftTimeRemaining = 0.0f
    val shiftDuration = build.shiftSpeed.shiftTimeSeconds

    // Métricas registradas
    var time0to60: Float? = null
    var time0to100: Float? = null
    var time60to100: Float? = null
    var time80to120: Float? = null
    var time100to200: Float? = null
    var t60Start: Float? = null
    var t80Start: Float? = null
    var t100Start: Float? = null

    var time60ft: Float? = null
    var time100m: Float? = null
    var speed100m: Float? = null
    var time201m: Float? = null
    var speed201m: Float? = null
    var time402m: Float? = null
    var speed402m: Float? = null

    var peakG = 0.0f
    val speedTimePoints = mutableListOf<Pair<Float, Float>>()
    speedTimePoints.add(0.0f to 0.0f)

    while (time < MAX_SIM_TIME && (distance < 500f || time < 18.0f)) {
      val speedKmh = speedMs * 3.6f

      // Checa passagens de velocidade
      if (time0to60 == null && speedKmh >= 60f) time0to60 = time
      if (t60Start == null && speedKmh >= 60f) t60Start = time
      if (t80Start == null && speedKmh >= 80f) t80Start = time
      if (t100Start == null && speedKmh >= 100f) t100Start = time
      if (time0to100 == null && speedKmh >= 100f) time0to100 = time
      if (time60to100 == null && speedKmh >= 100f && t60Start != null) time60to100 = time - t60Start
      if (time80to120 == null && speedKmh >= 120f && t80Start != null) time80to120 = time - t80Start
      if (time100to200 == null && speedKmh >= 200f && t100Start != null) time100to200 = time - t100Start

      // Checa passagens de distância
      if (time60ft == null && distance >= 18.288f) time60ft = time
      if (time100m == null && distance >= 100f) {
        time100m = time
        speed100m = speedKmh
      }
      if (time201m == null && distance >= 201.168f) {
        time201m = time
        speed201m = speedKmh
      }
      if (time402m == null && distance >= 402.336f) {
        time402m = time
        speed402m = speedKmh
      }

      // Rotação do motor na marcha atual
      val currentRatio = gearRatios[currentGearIdx]
      val wheelRps = speedMs / tireCircM
      val calculatedRpm = (wheelRps * 60f * currentRatio * finalDrive).toInt()
      val effectiveRpm = calculatedRpm.coerceIn(1200, maxRpm + 400)

      // Troca de marcha
      if (effectiveRpm >= maxRpm && currentGearIdx < gearRatios.size - 1 && !isShifting) {
        isShifting = true
        shiftTimeRemaining = shiftDuration
      }

      var tractiveForceN = 0.0f

      if (isShifting) {
        shiftTimeRemaining -= DELTA_T
        if (shiftTimeRemaining <= 0f) {
          isShifting = false
          currentGearIdx++
        }
      } else {
        // Obter torque do motor para a RPM calculada
        val torqueMotorKgfm = interpolateCurve(result.torqueCurvePoints, effectiveRpm)
        val torqueMotorNm = torqueMotorKgfm * GRAVITY
        val torqueRodaNm = torqueMotorNm * currentRatio * finalDrive * (1.0f - drivetrainLoss)
        var wheelForceN = torqueRodaNm / tireDynamicRadiusM

        // Limita pela aderência dos pneus
        if (wheelForceN > maxGripForceN) {
          wheelForceN = maxGripForceN
        }
        tractiveForceN = wheelForceN
      }

      // Forças resistentes (Aerodinâmica + Rolamento)
      val fAero = 0.5f * airDensity * cd * area * (speedMs * speedMs)
      val fRoll = totalMass * GRAVITY * crr
      val fResist = fAero + fRoll
      val netForceN = (tractiveForceN - fResist).coerceAtLeast(-500f)

      val accelMps2 = (netForceN / totalMass).coerceAtLeast(0f)
      val currentG = accelMps2 / GRAVITY
      if (currentG > peakG) peakG = currentG

      speedMs += accelMps2 * DELTA_T
      distance += speedMs * DELTA_T
      time += DELTA_T

      if ((time * 100).toInt() % 10 == 0) { // grava a cada 0.1s
        speedTimePoints.add(time to (speedMs * 3.6f))
      }
    }

    // Consumo estimado em plena carga (WOT) em L/h
    val fuelWotLph = (result.estimatedEnginePowerCv / 1.01387f) * build.fuelType.bsfcTurbo * 0.453592f / build.fuelType.densityKgL

    // Pontos de troca
    val shiftPoints = SimulationEngine.findOptimalShiftPoints(
      SimulationConfig(
        enginePowerCv = result.estimatedEnginePowerCv,
        engineTorqueKgfm = result.estimatedEngineTorqueKgfm,
        maxRpm = result.effectiveRedlineRpm,
        gearRatios = build.gearRatios,
        finalDriveRatio = build.finalDriveRatio,
        tireWidthMm = build.tireWidthMm,
        tireAspectRatio = build.tireAspectRatio,
        rimDiameterInches = build.rimInches
      )
    )

    return DynoTestAccelerationResult(
      time0to60Kmh = time0to60 ?: 4.5f,
      time0to100Kmh = time0to100 ?: 8.5f,
      time60to100Kmh = time60to100 ?: 4.0f,
      time80to120Kmh = time80to120 ?: 5.2f,
      time100to200Kmh = time100to200,
      time60ft = time60ft ?: 2.4f,
      time100m = time100m ?: 6.2f,
      speedAt100mKmh = speed100m ?: 102f,
      time201m = time201m ?: 9.8f,
      speedAt201mKmh = speed201m ?: 125f,
      time402m = time402m ?: 15.2f,
      speedAt402mKmh = speed402m ?: 155f,
      estimatedTopSpeedKmh = speedTimePoints.maxOfOrNull { it.second } ?: 195f,
      peakLongitudinalG = peakG,
      fuelConsumptionWotLph = fuelWotLph,
      shiftPoints = shiftPoints,
      speedTimePoints = speedTimePoints
    )
  }

  // ---------------------------------------------------------------------------------------
  // 4. PRESETS DO VEÍCULO DEMONSTRATIVO (Vectra 2.2 8V 1999) E MODELOS DE PROJETO
  // ---------------------------------------------------------------------------------------

  fun createDefaultVectraBuild(): TuningBuild {
    return TuningBuild(
      projectName = "Vectra 2.2 Street",
      vehicleName = "Chevrolet Vectra 2.2 8V 1999",
      isDemonstrativeVehicle = true,
      displacementCc = 2198,
      cylindersCount = 4,
      factoryEnginePowerCv = 123f,
      factoryEngineTorqueKgfm = 19.4f,
      factoryPeakPowerRpm = 5200,
      factoryPeakTorqueRpm = 2800,
      factoryRedlineRpm = 6200,
      baseVehicleCurbWeightKg = 1260f,
      driverWeightKg = 80f,
      baseDrivetrain = DrivetrainType.FWD,
      baseCompressionRatio = 9.2f,
      gearRatios = listOf(3.73f, 1.96f, 1.32f, 0.95f, 0.76f),
      finalDriveRatio = 3.94f,
      drivetrainLossPercent = 12f,
      tireWidthMm = 195,
      tireAspectRatio = 60,
      rimInches = 15,
      injectorFlowLbHr = 28.0f,
      injectorBasePressureBar = 3.0f,
      injectorOperatingPressureBar = 3.0f,
      injectorCount = 4,
      fuelPumpFlowLph = 100.0f,
      fuelPumpCount = 1,
      fuelType = FuelTypeOption.ETANOL
    )
  }

  fun applyProjectTemplate(template: ProjectTemplateType, current: TuningBuild): TuningBuild {
    return when (template) {
      ProjectTemplateType.ORIGINAL -> current.copy(
        projectName = "Original de Fábrica",
        aspiration = AspirationType.ASPIRADO,
        turboBoostBar = 0.0f,
        intercooler = IntercoolerType.SEM_INTERCOOLER,
        ecu = EcuType.ORIGINAL,
        tuneMap = TuneMapType.RUA_EQUILIBRADO,
        pistons = PistonType.ORIGINAL,
        rods = RodsType.ORIGINAL,
        studs = StudsType.ORIGINAL,
        headGasket = HeadGasketType.ORIGINAL,
        crankshaft = CrankshaftType.ORIGINAL,
        injectorFlowLbHr = 28f,
        injectorOperatingPressureBar = 3.0f,
        fuelPumpFlowLph = 100f,
        intake = IntakeType.FILTRO_ORIGINAL,
        throttleBody = ThrottleBodyType.ORIGINAL,
        exhaustHeader = ExhaustHeaderType.ORIGINAL,
        exhaustSystem = ExhaustSystemType.ORIGINAL,
        cylinderHead = CylinderHeadType.ORIGINAL,
        camshaft = CamshaftProfile.ORIGINAL,
        clutch = ClutchType.ORIGINAL,
        tireCompound = TireCompound.RUA_CONVENCIONAL,
        weightReduction = WeightReductionStage.ORIGINAL,
        extraCompressionRatio = 0.0f
      )
      ProjectTemplateType.ASPIRADO_PREPARADO -> current.copy(
        projectName = "Aspirado 276° Stage 2",
        aspiration = AspirationType.ASPIRADO,
        turboBoostBar = 0.0f,
        intercooler = IntercoolerType.SEM_INTERCOOLER,
        ecu = EcuType.PROGRAMAVEL_BASICA,
        tuneMap = TuneMapType.RUA_EQUILIBRADO,
        pistons = PistonType.ORIGINAL,
        rods = RodsType.ORIGINAL,
        headGasket = HeadGasketType.REFORCADA_MLS,
        injectorFlowLbHr = 42f,
        injectorOperatingPressureBar = 3.5f,
        fuelPumpFlowLph = 150f,
        intake = IntakeType.COLD_AIR_INTAKE,
        throttleBody = ThrottleBodyType.AUMENTADA_60MM,
        exhaustHeader = ExhaustHeaderType.TUBULAR_4X1_MEDIO,
        exhaustSystem = ExhaustSystemType.ESPORTIVO_2_POLEGADAS,
        cylinderHead = CylinderHeadType.DUTOS_POLIDOS_STREET,
        camshaft = CamshaftProfile.MEDIO_276,
        extraCompressionRatio = 1.0f,
        clutch = ClutchType.CERAMICA_4_PASTILHAS,
        tireCompound = TireCompound.ESPORTIVO_UHP,
        fuelType = FuelTypeOption.ETANOL
      )
      ProjectTemplateType.TURBO_BAIXA_PRESSAO -> current.copy(
        projectName = "Kit Turbo 0.5 Bar Miolo Original",
        aspiration = AspirationType.TURBO_PEQUENO,
        turboBoostBar = 0.5f,
        intercooler = IntercoolerType.PEQUENO_FRONTAL,
        ecu = EcuType.PROGRAMAVEL_BASICA,
        tuneMap = TuneMapType.RUA_EQUILIBRADO,
        pistons = PistonType.ORIGINAL,
        rods = RodsType.ORIGINAL,
        headGasket = HeadGasketType.REFORCADA_MLS,
        injectorFlowLbHr = 42f,
        injectorOperatingPressureBar = 3.5f,
        fuelPumpFlowLph = 255f,
        intake = IntakeType.FILTRO_CONICO_ESPORTIVO,
        throttleBody = ThrottleBodyType.ORIGINAL,
        exhaustHeader = ExhaustHeaderType.ORIGINAL,
        exhaustSystem = ExhaustSystemType.DIMENSIONADO_2_5_POLEGADAS,
        cylinderHead = CylinderHeadType.ORIGINAL,
        camshaft = CamshaftProfile.ORIGINAL,
        clutch = ClutchType.CERAMICA_4_PASTILHAS,
        ignitionCoil = IgnitionCoilType.ALTA_POTENCIA,
        sparkPlugs = SparkPlugType.IRIDIUM_MEDIO,
        tireCompound = TireCompound.ESPORTIVO_UHP,
        fuelType = FuelTypeOption.ETANOL
      )
      ProjectTemplateType.TURBO_INTERMEDIARIO -> current.copy(
        projectName = "Turbo Médio 0.8 Bar Street Forte",
        aspiration = AspirationType.TURBO_MEDIO,
        turboBoostBar = 0.8f,
        intercooler = IntercoolerType.MEDIO_ALTA_EFICIENCIA,
        ecu = EcuType.PROGRAMAVEL_COMPLETA,
        tuneMap = TuneMapType.RUA_EQUILIBRADO,
        pistons = PistonType.ORIGINAL,
        rods = RodsType.FORJADA_H,
        studs = StudsType.REFORCADO_ARP,
        headGasket = HeadGasketType.REFORCADA_MLS,
        injectorFlowLbHr = 60f,
        injectorOperatingPressureBar = 3.8f,
        fuelPumpFlowLph = 255f,
        intake = IntakeType.COLD_AIR_INTAKE,
        throttleBody = ThrottleBodyType.AUMENTADA_60MM,
        exhaustHeader = ExhaustHeaderType.TUBULAR_4X1_MEDIO,
        exhaustSystem = ExhaustSystemType.DIMENSIONADO_2_5_POLEGADAS,
        cylinderHead = CylinderHeadType.DUTOS_POLIDOS_STREET,
        camshaft = CamshaftProfile.LEVE_260,
        clutch = ClutchType.CERAMICA_6_PASTILHAS,
        ignitionCoil = IgnitionCoilType.INDIVIDUAIS_R8,
        sparkPlugs = SparkPlugType.RACING_IRIDIUM,
        tireCompound = TireCompound.SEMI_SLICK,
        fuelType = FuelTypeOption.ETANOL
      )
      ProjectTemplateType.TURBO_FORJADO_ALTA -> current.copy(
        projectName = "Turbo Forjado 1.5 Bar Extremo",
        aspiration = AspirationType.TURBO_GRANDE,
        turboBoostBar = 1.5f,
        intercooler = IntercoolerType.GRANDE_COMPETICAO,
        ecu = EcuType.PROGRAMAVEL_COMPLETA,
        tuneMap = TuneMapType.POTENCIA_AGRESSIVO,
        pistons = PistonType.FORJADO_COMPETICAO,
        rods = RodsType.FORJADA_I_COMPETICAO,
        studs = StudsType.REFORCADO_ARP,
        headGasket = HeadGasketType.COBRE_ORING,
        crankshaft = CrankshaftType.FORJADO_ALIVIADO,
        injectorFlowLbHr = 80f,
        injectorOperatingPressureBar = 4.0f,
        fuelPumpFlowLph = 340f,
        fuelPumpCount = 2,
        intake = IntakeType.COLETOR_PLENUM_DIMENSIONADO,
        throttleBody = ThrottleBodyType.RACING_70MM,
        exhaustHeader = ExhaustHeaderType.TUBULAR_4X1_RACING,
        exhaustSystem = ExhaustSystemType.RACING_3_POLEGADAS,
        cylinderHead = CylinderHeadType.FLUXO_CRUZADO_VALVULAS_MAIORES,
        camshaft = CamshaftProfile.MEDIO_276,
        clutch = ClutchType.MULTIDISCO_CARBONO,
        ignitionCoil = IgnitionCoilType.INDIVIDUAIS_R8,
        sparkPlugs = SparkPlugType.RACING_IRIDIUM,
        tireCompound = TireCompound.SLICK_ARRANCADA,
        weightReduction = WeightReductionStage.MODERADO,
        fuelType = FuelTypeOption.ETANOL
      )
      ProjectTemplateType.SUPERCHARGER -> current.copy(
        projectName = "Supercharger Roots 0.6 Bar",
        aspiration = AspirationType.SUPERCHARGER,
        turboBoostBar = 0.6f,
        intercooler = IntercoolerType.PEQUENO_FRONTAL,
        ecu = EcuType.PROGRAMAVEL_BASICA,
        tuneMap = TuneMapType.RUA_EQUILIBRADO,
        pistons = PistonType.ORIGINAL,
        rods = RodsType.ORIGINAL,
        headGasket = HeadGasketType.REFORCADA_MLS,
        injectorFlowLbHr = 42f,
        injectorOperatingPressureBar = 3.5f,
        fuelPumpFlowLph = 255f,
        intake = IntakeType.COLD_AIR_INTAKE,
        throttleBody = ThrottleBodyType.AUMENTADA_60MM,
        exhaustSystem = ExhaustSystemType.DIMENSIONADO_2_5_POLEGADAS,
        clutch = ClutchType.CERAMICA_4_PASTILHAS,
        tireCompound = TireCompound.ESPORTIVO_UHP,
        fuelType = FuelTypeOption.ETANOL
      )
      ProjectTemplateType.CUSTOMIZADO -> current
    }
  }

  fun calculateTotalPartsCost(build: TuningBuild): Double {
    var sum = 0.0
    sum += build.customPartPrices["pistons"] ?: build.pistons.priceBrl
    sum += build.customPartPrices["rods"] ?: build.rods.priceBrl
    sum += build.customPartPrices["studs"] ?: build.studs.priceBrl
    sum += build.customPartPrices["headGasket"] ?: build.headGasket.priceBrl
    sum += build.customPartPrices["crankshaft"] ?: build.crankshaft.priceBrl
    sum += build.customPartPrices["intercooler"] ?: build.intercooler.priceBrl
    sum += build.customPartPrices["ecu"] ?: build.ecu.priceBrl
    sum += build.customPartPrices["ignitionCoil"] ?: build.ignitionCoil.priceBrl
    sum += build.customPartPrices["sparkPlugs"] ?: build.sparkPlugs.priceBrl
    sum += build.customPartPrices["intake"] ?: build.intake.priceBrl
    sum += build.customPartPrices["throttleBody"] ?: build.throttleBody.priceBrl
    sum += build.customPartPrices["exhaustHeader"] ?: build.exhaustHeader.priceBrl
    sum += build.customPartPrices["exhaustSystem"] ?: build.exhaustSystem.priceBrl
    sum += build.customPartPrices["cylinderHead"] ?: build.cylinderHead.priceBrl
    sum += build.customPartPrices["camshaft"] ?: build.camshaft.priceBrl
    sum += build.customPartPrices["clutch"] ?: build.clutch.priceBrl
    sum += build.customPartPrices["tireCompound"] ?: build.tireCompound.pricePerSetBrl
    sum += build.customPartPrices["weightReduction"] ?: build.weightReduction.priceBrl
    sum += build.customPartPrices["aero"] ?: build.aero.priceBrl

    if (build.aspiration != AspirationType.ASPIRADO) {
      val turboKitCost = when (build.aspiration) {
        AspirationType.TURBO_PEQUENO -> 3500.0
        AspirationType.TURBO_MEDIO -> 4800.0
        AspirationType.TURBO_GRANDE -> 7200.0
        AspirationType.SUPERCHARGER -> 6500.0
        else -> 5000.0
      }
      sum += build.customPartPrices["turboKit"] ?: turboKitCost
    }

    if (build.injectorFlowLbHr > 28f) {
      sum += build.customPartPrices["injectors"] ?: (build.injectorCount * 220.0)
    }
    if (build.fuelPumpFlowLph > 100f) {
      sum += build.customPartPrices["fuelPump"] ?: (build.fuelPumpCount * 450.0)
    }

    return sum
  }

  private fun interpolateCurve(points: List<Pair<Int, Float>>, targetRpm: Int): Float {
    if (points.isEmpty()) return 10f
    val sorted = points.sortedBy { it.first }
    if (targetRpm <= sorted.first().first) return sorted.first().second
    if (targetRpm >= sorted.last().first) return sorted.last().second
    val idx = sorted.indexOfFirst { it.first >= targetRpm }
    if (idx <= 0) return sorted.first().second
    val p0 = sorted[idx - 1]
    val p1 = sorted[idx]
    val factor = (targetRpm - p0.first).toFloat() / (p1.first - p0.first).coerceAtLeast(1)
    return p0.second + factor * (p1.second - p0.second)
  }
}
