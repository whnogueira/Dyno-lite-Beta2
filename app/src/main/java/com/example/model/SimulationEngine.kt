package com.example.model

import java.util.Locale
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object SimulationEngine {

  const val GRAVITY = 9.80665f
  const val DELTA_T = 0.02f // 20ms steps
  const val MAX_SIM_TIME = 40.0f // 40 seconds max simulation duration

  /**
   * Calcula o multiplicador de potência teórica de turbo
   */
  fun calculateTurboMultiplier(boostBar: Float, efficiency: Float = 0.85f): Float {
    if (boostBar <= 0f) return 1.0f
    val pressureMult = 1.0f + boostBar
    return 1.0f + (pressureMult * efficiency - 1.0f).coerceAtLeast(0f)
  }

  /**
   * Gera a tabela de velocidades por marcha e rotação pós-troca (Seção 28)
   */
  fun calculateGearSpeeds(config: SimulationConfig): List<GearSpeedEntry> {
    val tireCirc = config.tireCircumferenceM
    val finalDrive = config.finalDriveRatio
    val maxRpm = config.maxRpm.toFloat()

    return config.gearRatios.mapIndexed { index, ratio ->
      val gearNum = index + 1
      fun speedAt(rpm: Float): Float {
        val wheelRpm = rpm / (ratio * finalDrive)
        val speedMs = (wheelRpm * tireCirc) / 60f
        return (speedMs * 3.6f).coerceAtLeast(0f)
      }

      val rpmAfterShift: Int? = if (index < config.gearRatios.size - 1) {
        val nextRatio = config.gearRatios[index + 1]
        // rpmDepoisTroca = rpmAntesTroca * (relacaoSeguinte / relacaoAtual)
        ((maxRpm * nextRatio) / ratio).toInt()
      } else {
        null
      }

      GearSpeedEntry(
        gearIndex = gearNum,
        gearName = "${gearNum}ª Marcha",
        ratio = ratio,
        speedAt2000RpmKmh = speedAt(2000f),
        speedAt3000RpmKmh = speedAt(3000f),
        speedAt4000RpmKmh = speedAt(4000f),
        speedAt5000RpmKmh = speedAt(5000f),
        speedAtCutoffKmh = speedAt(maxRpm),
        rpmAfterShift = rpmAfterShift
      )
    }
  }

  /**
   * Obtém a curva sintética ou interpolada de Potência (cv) e Torque (kgfm) para qualquer RPM
   */
  fun getEngineTorqueAtRpm(rpm: Int, config: SimulationConfig): Float {
    val safeRpm = rpm.coerceIn(800, config.maxRpm + 500)
    
    // Se temos pontos reais registrados da passagem
    if (config.isUsingRealRunCurve && config.customPowerCurvePoints.isNotEmpty()) {
      val pts = config.customPowerCurvePoints.sortedBy { it.first }
      val matchingPowerCv = when {
        safeRpm <= pts.first().first -> pts.first().second * (safeRpm.toFloat() / pts.first().first)
        safeRpm >= pts.last().first -> pts.last().second * 0.85f
        else -> {
          val idx = pts.indexOfFirst { it.first >= safeRpm }
          if (idx <= 0) pts.first().second
          else {
            val p0 = pts[idx - 1]
            val p1 = pts[idx]
            val factor = (safeRpm - p0.first).toFloat() / (p1.first - p0.first).coerceAtLeast(1)
            p0.second + factor * (p1.second - p0.second)
          }
        }
      }
      // Converte potência (cv) em torque (kgfm): T = (P * 716.2) / RPM
      val torqueKgfm = (matchingPowerCv * 716.2f) / safeRpm.coerceAtLeast(1000)
      return torqueKgfm.coerceAtLeast(1.0f)
    }

    // Curva Sintética baseada nos picos informados
    val peakTRpm = config.peakTorqueRpm.toFloat()
    val peakPRpm = config.peakPowerRpm.toFloat()
    val peakTorque = config.engineTorqueKgfm

    // Modelagem assimétrica suave do torque
    val r = safeRpm.toFloat()
    val torqueShape = if (r <= peakTRpm) {
      val norm = (r / peakTRpm).coerceIn(0.2f, 1.0f)
      0.65f + 0.35f * sin(norm * (Math.PI.toFloat() / 2f))
    } else {
      val span = (config.maxRpm - peakTRpm).coerceAtLeast(1000f)
      val dropFactor = ((r - peakTRpm) / span).coerceIn(0f, 1.5f)
      1.0f - 0.28f * (dropFactor.pow(1.6f))
    }

    val baseTorque = peakTorque * torqueShape

    // Multiplicador de turbo se ativado
    val turboMult = if (config.isTurboSimulated && config.turboBoostBar > 0f) {
      calculateTurboMultiplier(config.turboBoostBar, config.turboEfficiency)
    } else 1.0f

    return (baseTorque * turboMult).coerceAtLeast(2.0f)
  }

  fun getEnginePowerCvAtRpm(rpm: Int, config: SimulationConfig): Float {
    val torqueKgfm = getEngineTorqueAtRpm(rpm, config)
    // P (cv) = (Torque kgfm * RPM) / 716.2
    return ((torqueKgfm * rpm) / 716.2f).coerceAtLeast(0f)
  }

  /**
   * Força Trativa nas Rodas para dada Marcha e RPM (Seção 29)
   */
  fun calculateWheelTractiveForceN(
    gearRatio: Float,
    engineRpm: Int,
    config: SimulationConfig
  ): Float {
    val torqueMotorKgfm = getEngineTorqueAtRpm(engineRpm, config)
    val torqueMotorNm = torqueMotorKgfm * GRAVITY
    val effTrans = (1.0f - (config.drivetrainLossPercent / 100f)).coerceIn(0.50f, 1.0f)
    val torqueRodaNm = torqueMotorNm * gearRatio * config.finalDriveRatio * effTrans
    val dynamicRadiusM = config.tireDynamicRadiusM.coerceAtLeast(0.15f)
    return torqueRodaNm / dynamicRadiusM
  }

  /**
   * Limite de Aderência dos Pneus (Seção 30)
   */
  fun calculateMaxGripForceN(config: SimulationConfig): Float {
    val totalMass = config.totalWeightKg
    val massOnDriveAxle = totalMass * config.drivetrainType.weightOnDriveAxlePercent
    return massOnDriveAxle * GRAVITY * config.tireGripMu
  }

  /**
   * Encontra os Pontos Ideais de Troca de Marcha (Seção 33)
   * Compara a força nas rodas da marcha atual com a força na marcha seguinte.
   */
  fun findOptimalShiftPoints(config: SimulationConfig): List<GearShiftPoint> {
    val points = mutableListOf<GearShiftPoint>()
    val tireCirc = config.tireCircumferenceM
    val finalDrive = config.finalDriveRatio
    val maxRpm = config.maxRpm

    for (i in 0 until config.gearRatios.size - 1) {
      val currRatio = config.gearRatios[i]
      val nextRatio = config.gearRatios[i + 1]
      val gearNum = i + 1
      val nextGearNum = i + 2

      var bestShiftRpm = maxRpm - 150
      var foundCrossOver = false

      // Varre RPM de peakPowerRpm até maxRpm para ver onde a marcha seguinte entrega mais força
      val startScanRpm = max(config.peakPowerRpm - 400, config.peakTorqueRpm)
      for (scanRpm in startScanRpm..maxRpm step 50) {
        val currForce = calculateWheelTractiveForceN(currRatio, scanRpm, config)
        val rpmNext = ((scanRpm * nextRatio) / currRatio).toInt()
        val nextForce = calculateWheelTractiveForceN(nextRatio, rpmNext, config)

        if (nextForce >= currForce) {
          bestShiftRpm = scanRpm
          foundCrossOver = true
          break
        }
      }

      val rpmAfter = ((bestShiftRpm * nextRatio) / currRatio).toInt()
      val wheelRpm = bestShiftRpm.toFloat() / (currRatio * finalDrive)
      val speedMs = (wheelRpm * tireCirc) / 60f
      val shiftSpeedKmh = speedMs * 3.6f

      val reason = if (foundCrossOver) {
        "A ${nextGearNum}ª marcha passa a ter mais força trativa a partir de $bestShiftRpm RPM."
      } else {
        "Troca recomendada no limite de rotação para máxima multiplicação de torque."
      }

      points.add(
        GearShiftPoint(
          fromGear = gearNum,
          toGear = nextGearNum,
          recommendedShiftRpm = bestShiftRpm,
          shiftSpeedKmh = shiftSpeedKmh,
          rpmAfterShift = rpmAfter,
          explanation = reason
        )
      )
    }

    return points
  }

  /**
   * Executa a simulação completa passo-a-passo (Seções 31 e 32)
   */
  fun runSimulation(config: SimulationConfig): SimulationResult {
    val gearSpeeds = calculateGearSpeeds(config)
    val shiftPoints = findOptimalShiftPoints(config)
    val maxGripForceN = calculateMaxGripForceN(config)
    val totalMass = config.totalWeightKg
    val tireCirc = config.tireCircumferenceM
    val finalDrive = config.finalDriveRatio
    val headwindMs = (config.headwindSpeedKmh / 3.6f)
    val slopeRad = atan(config.trackSlopePercent / 100f)
    val slopeForceN = totalMass * GRAVITY * sin(slopeRad)
    val rollForceN = config.crr * totalMass * GRAVITY * cos(slopeRad)

    // Curva de Potência e Torque para o gráfico
    val rpmCurve = mutableListOf<Triple<Int, Float, Float>>()
    for (r in 1000..config.maxRpm step 100) {
      val t = getEngineTorqueAtRpm(r, config)
      val p = getEnginePowerCvAtRpm(r, config)
      rpmCurve.add(Triple(r, p, t))
    }

    val stepPoints = mutableListOf<SimulationStepPoint>()

    var currentTimeSec = 0f
    var currentSpeedMs = 0.5f // Ligeiro rolamento para inicialização numérica
    var currentDistanceM = 0f
    var currentGearIndex = 0
    var isShifting = false
    var shiftTimerSec = 0f
    var hasTractionLoss = false

    // Variáveis para gravação de parciais
    var time0to60: Float? = null
    var time0to100: Float? = null
    var time60to100: Float? = null
    var time80to120: Float? = null
    var time100to200: Float? = null

    var timeAt60Kmh: Float? = null
    var timeAt80Kmh: Float? = null
    var timeAt100Kmh: Float? = null

    var time100m: Float? = null
    var speed100m: Float? = null
    var time201m: Float? = null
    var speed201m: Float? = null
    var time402m: Float? = null
    var speed402m: Float? = null

    var peakG = 0f
    var topSpeedMs = 0f

    val maxSteps = (MAX_SIM_TIME / DELTA_T).toInt()

    for (step in 0 until maxSteps) {
      val currentSpeedKmh = currentSpeedMs * 3.6f
      topSpeedMs = max(topSpeedMs, currentSpeedMs)

      val currRatio = config.gearRatios[currentGearIndex]
      val nextRatio = if (currentGearIndex < config.gearRatios.size - 1) config.gearRatios[currentGearIndex + 1] else null
      val optimalShiftRpm = shiftPoints.getOrNull(currentGearIndex)?.recommendedShiftRpm ?: (config.maxRpm - 100)

      // Cálculo de RPM a partir da velocidade do veículo
      val wheelRpm = (currentSpeedMs * 60f) / tireCirc
      var engineRpm = (wheelRpm * currRatio * finalDrive).toInt()

      // Simulação de saída da inércia (embreagem/conversor segura rotação mínima)
      if (currentGearIndex == 0 && engineRpm < config.peakTorqueRpm - 600) {
        engineRpm = max(engineRpm, (config.peakTorqueRpm - 600).coerceAtLeast(1800))
      }
      engineRpm = engineRpm.coerceIn(900, config.maxRpm + 400)

      // Verifica se é hora de trocar de marcha
      if (!isShifting && nextRatio != null && (engineRpm >= optimalShiftRpm || engineRpm >= config.maxRpm)) {
        isShifting = true
        shiftTimerSec = config.shiftTimeSeconds
      }

      val tractiveForceN: Float
      val isTractionLimited: Boolean

      if (isShifting) {
        tractiveForceN = 0f
        isTractionLimited = false
        shiftTimerSec -= DELTA_T
        if (shiftTimerSec <= 0f) {
          isShifting = false
          currentGearIndex = min(currentGearIndex + 1, config.gearRatios.size - 1)
        }
      } else {
        val rawTractive = calculateWheelTractiveForceN(currRatio, engineRpm, config)
        if (rawTractive > maxGripForceN) {
          tractiveForceN = maxGripForceN
          isTractionLimited = true
          hasTractionLoss = true
        } else {
          tractiveForceN = rawTractive
          isTractionLimited = false
        }
      }

      // Forças Resistivas (Seção 31)
      val relAirSpeedMs = currentSpeedMs + headwindMs
      val aeroForceN = 0.5f * config.airDensityKgM3 * config.cd * config.frontalAreaM2 * (relAirSpeedMs * relAirSpeedMs)
      val totalResistForceN = rollForceN + aeroForceN + slopeForceN
      val netForceN = (tractiveForceN - totalResistForceN)

      val accelMps2 = (netForceN / totalMass).coerceAtLeast(if (currentSpeedMs > 5f) -3.0f else 0f)
      val gLong = accelMps2 / GRAVITY
      peakG = max(peakG, gLong)

      val powerEngine = getEnginePowerCvAtRpm(engineRpm, config)
      val powerWheel = powerEngine * (1f - (config.drivetrainLossPercent / 100f))
      val torqueEngine = getEngineTorqueAtRpm(engineRpm, config)

      // Gravação do ponto
      if (step % 2 == 0) { // grava a cada 0.04s para economizar memória
        stepPoints.add(
          SimulationStepPoint(
            timeSec = currentTimeSec,
            speedKmh = currentSpeedKmh,
            distanceMeters = currentDistanceM,
            currentGear = currentGearIndex + 1,
            engineRpm = engineRpm,
            enginePowerCv = powerEngine,
            wheelPowerCv = powerWheel,
            engineTorqueKgfm = torqueEngine,
            wheelTractiveForceN = tractiveForceN,
            effectiveForceN = netForceN.coerceAtLeast(0f),
            aeroForceN = aeroForceN,
            rollForceN = rollForceN,
            longitudinalG = gLong,
            isShifting = isShifting,
            isTractionLimited = isTractionLimited
          )
        )
      }

      // Registro de splits de velocidade
      if (currentSpeedKmh >= 60f && time0to60 == null) {
        time0to60 = currentTimeSec
        timeAt60Kmh = currentTimeSec
      }
      if (currentSpeedKmh >= 80f && timeAt80Kmh == null) {
        timeAt80Kmh = currentTimeSec
      }
      if (currentSpeedKmh >= 100f && time0to100 == null) {
        time0to100 = currentTimeSec
        timeAt100Kmh = currentTimeSec
        if (timeAt60Kmh != null) {
          time60to100 = currentTimeSec - timeAt60Kmh
        }
      }
      if (currentSpeedKmh >= 120f && time80to120 == null && timeAt80Kmh != null) {
        time80to120 = currentTimeSec - timeAt80Kmh
      }
      if (currentSpeedKmh >= 200f && time100to200 == null && timeAt100Kmh != null) {
        time100to200 = currentTimeSec - timeAt100Kmh
      }

      // Registro de splits de distância
      if (currentDistanceM >= 100f && time100m == null) {
        time100m = currentTimeSec
        speed100m = currentSpeedKmh
      }
      if (currentDistanceM >= 201.17f && time201m == null) {
        time201m = currentTimeSec
        speed201m = currentSpeedKmh
      }
      if (currentDistanceM >= 402.34f && time402m == null) {
        time402m = currentTimeSec
        speed402m = currentSpeedKmh
      }

      // Integração por passo de tempo (Euler/Verlet simples)
      val newSpeedMs = max(0f, currentSpeedMs + accelMps2 * DELTA_T)
      val newDistanceM = currentDistanceM + ((currentSpeedMs + newSpeedMs) / 2f) * DELTA_T
      
      currentSpeedMs = newSpeedMs
      currentDistanceM = newDistanceM
      currentTimeSec += DELTA_T

      // Condição de parada: passou dos 402m e estabilizou ou atingiu velocidade máxima
      if (currentDistanceM > 450f && (accelMps2 < 0.05f || currentSpeedKmh > 240f)) {
        break
      }
      if (currentGearIndex == config.gearRatios.size - 1 && accelMps2 <= 0.005f && currentTimeSec > 15f) {
        break
      }
    }

    val topSpeedKmh = topSpeedMs * 3.6f

    val confidence = when {
      config.isUsingRealRunCurve && !config.isTurboSimulated -> SimulationConfidence.HIGH
      config.isUsingRealRunCurve && config.isTurboSimulated -> SimulationConfidence.MEDIUM
      else -> SimulationConfidence.LOW
    }

    return SimulationResult(
      config = config,
      confidence = confidence,
      points = stepPoints,
      time0to60Kmh = time0to60,
      time0to100Kmh = time0to100,
      time60to100Kmh = time60to100,
      time80to120Kmh = time80to120,
      time100to200Kmh = time100to200,
      time100m = time100m,
      speedAt100mKmh = speed100m,
      time201m = time201m,
      speedAt201mKmh = speed201m,
      time402m = time402m,
      speedAt402mKmh = speed402m,
      topSpeedKmh = topSpeedKmh,
      peakLongitudinalG = peakG,
      gearSpeeds = gearSpeeds,
      optimalShiftPoints = shiftPoints,
      hasTractionLossWarning = hasTractionLoss,
      powerTorqueRpmCurve = rpmCurve
    )
  }
}
