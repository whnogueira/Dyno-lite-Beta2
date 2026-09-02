package com.example.model

import java.util.Locale

/**
 * Modos de teste disponíveis no Dyno Lite.
 */
enum class TestMode(val code: String, val title: String, val description: String) {
  DYNO("DYNO", "Dinamômetro", "Estima potência, torque e curva do motor/rodas."),
  ACCELERATION("ACCELERATION", "Aceleração", "Mede o tempo preciso entre duas velocidades.");

  companion object {
    fun fromCode(code: String): TestMode {
      return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: DYNO
    }
  }
}

/**
 * Parcial de aceleração registrada durante o teste.
 */
data class AccelerationSplit(
  val label: String,
  val startSpeedKmh: Float,
  val endSpeedKmh: Float,
  val timeSeconds: Float
)

/**
 * Faixa de velocidade pré-definida ou personalizada para o Teste de Aceleração.
 */
data class AccelerationRange(
  val startSpeedKmh: Float,
  val endSpeedKmh: Float,
  val label: String,
  val isCustom: Boolean = false
) {
  companion object {
    val PRESETS = listOf(
      AccelerationRange(0f, 60f, "0–60 km/h"),
      AccelerationRange(0f, 100f, "0–100 km/h"),
      AccelerationRange(40f, 100f, "40–100 km/h"),
      AccelerationRange(50f, 100f, "50–100 km/h"),
      AccelerationRange(60f, 120f, "60–120 km/h"),
      AccelerationRange(80f, 120f, "80–120 km/h"),
      AccelerationRange(100f, 200f, "100–200 km/h"),
      AccelerationRange(0f, 0f, "Personalizado", isCustom = true)
    )

    fun validateCustomRange(startKmh: Float, endKmh: Float): Pair<Boolean, String?> {
      if (startKmh < 0f || startKmh > 250f) {
        return Pair(false, "Velocidade inicial deve estar entre 0 e 250 km/h.")
      }
      if (endKmh < 10f || endKmh > 300f) {
        return Pair(false, "Velocidade final deve estar entre 10 e 300 km/h.")
      }
      if (endKmh <= startKmh) {
        return Pair(false, "Velocidade final deve ser maior que a velocidade inicial.")
      }
      if ((endKmh - startKmh) < 10f) {
        return Pair(false, "Diferença mínima entre inicial e final deve ser de pelo menos 10 km/h.")
      }
      return Pair(true, null)
    }
  }
}

/**
 * Motor de interpolação e cálculo do teste de aceleração.
 */
object AccelerationInterpolation {

  /**
   * Interpola o instante exato do cruzamento da velocidade alvo usando regra de três linear entre dois fixes GPS.
   *
   * fraction = (targetSpeed - previousSpeed) / (currentSpeed - previousSpeed)
   * crossingTime = previousTimestamp + fraction * (currentTimestamp - previousTimestamp)
   */
  fun interpolateCrossingTimeNs(
    previousSpeedKmh: Float,
    currentSpeedKmh: Float,
    targetSpeedKmh: Float,
    previousTimestampNs: Long,
    currentTimestampNs: Long
  ): Long {
    val deltaSpeed = currentSpeedKmh - previousSpeedKmh
    if (deltaSpeed <= 0f) return previousTimestampNs
    val fraction = ((targetSpeedKmh - previousSpeedKmh) / deltaSpeed).coerceIn(0f, 1f)
    val deltaNs = currentTimestampNs - previousTimestampNs
    return previousTimestampNs + (fraction * deltaNs).toLong()
  }

  /**
   * Calcula o tempo de travessia entre dois instantes interpolados em segundos.
   */
  fun calculateElapsedTimeSeconds(startCrossingTimeNs: Long, endCrossingTimeNs: Long): Float {
    if (endCrossingTimeNs <= startCrossingTimeNs) return 0f
    return (endCrossingTimeNs - startCrossingTimeNs) / 1_000_000_000.0f
  }

  /**
   * Converte velocidade entre km/h e mph.
   */
  fun kmhToMph(kmh: Float): Float = kmh * 0.621371192f
  fun mphToKmh(mph: Float): Float = mph * 1.609344f

  /**
   * Determina as parciais automáticas cruzadas durante a medição.
   * Exemplo: para 0–200: 0–60, 0–100, 0–160, 60–120, 80–120, 100–200.
   * Exemplo: para 50–150: 50–100, 100–150, 50–150.
   */
  fun calculateCrossedPartials(
    targetStartSpeedKmh: Float,
    targetEndSpeedKmh: Float,
    speedCrossingTimestampsNs: Map<Int, Long>
  ): List<AccelerationSplit> {
    val result = mutableListOf<AccelerationSplit>()

    // Lista de pares padrão de parciais candidatos
    val candidatePairs = listOf(
      Pair(0, 60),
      Pair(0, 100),
      Pair(0, 160),
      Pair(0, 200),
      Pair(40, 100),
      Pair(50, 100),
      Pair(50, 150),
      Pair(60, 120),
      Pair(80, 120),
      Pair(100, 150),
      Pair(100, 200)
    )

    for ((start, end) in candidatePairs) {
      val startF = start.toFloat()
      val endF = end.toFloat()

      // Apenas sub-faixas contidas no intervalo do teste realizado
      if (startF >= targetStartSpeedKmh - 0.5f && endF <= targetEndSpeedKmh + 0.5f) {
        val startNs = speedCrossingTimestampsNs[start]
        val endNs = speedCrossingTimestampsNs[end]

        if (startNs != null && endNs != null && endNs > startNs) {
          val timeSec = calculateElapsedTimeSeconds(startNs, endNs)
          result.add(
            AccelerationSplit(
              label = "$start–$end km/h",
              startSpeedKmh = startF,
              endSpeedKmh = endF,
              timeSeconds = timeSec
            )
          )
        }
      }
    }

    return result
  }

  /**
   * Avalia a qualidade do teste de aceleração conforme Requisito 11.
   */
  fun evaluateQuality(
    isCompleted: Boolean,
    interpolatedStart: Boolean,
    interpolatedEnd: Boolean,
    uniqueFixCount: Int,
    averageGpsAccuracyM: Float,
    averageGpsFrequencyHz: Float,
    isGpsFrozen: Boolean,
    isPhoneStable: Boolean
  ): Pair<String, String?> {
    if (!isCompleted) {
      return Pair("INVÁLIDA", "Velocidade final não foi atingida.")
    }
    if (isGpsFrozen) {
      return Pair("INVÁLIDA", "Sinal de GPS congelado durante a aceleração.")
    }
    if (!interpolatedStart || !interpolatedEnd) {
      return Pair("INVÁLIDA", "Cruzamentos de velocidade não puderam ser interpolados com precisão.")
    }
    if (!isPhoneStable) {
      return Pair("INVÁLIDA", "Movimento excessivo do suporte/celular detectado.")
    }
    if (uniqueFixCount < 3) {
      return Pair("INVÁLIDA", "Fixes de GPS insuficientes ($uniqueFixCount < 3).")
    }

    // Classificação BOA vs REGULAR
    return if (uniqueFixCount >= 5 && averageGpsAccuracyM <= 10.0f && averageGpsFrequencyHz >= 1.5f) {
      Pair("BOA", null)
    } else {
      Pair("REGULAR", "Frequência GPS baixa ou precisão reduzida durante a passagem.")
    }
  }
}
