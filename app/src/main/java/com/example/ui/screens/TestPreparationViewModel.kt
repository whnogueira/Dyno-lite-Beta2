package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DynoRunState
import com.example.data.RunResultRepository
import com.example.model.FinishReason
import com.example.model.RunResult
import com.example.model.RunSample
import com.example.model.VehicleCalculations
import com.example.model.VehicleProfile
import com.example.model.WeightConfidence
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Configurações e constantes do algoritmo dinamométrico.
 */
object DynoConfig {
  const val ACCEL_DEAD_ZONE = 0.25f
  const val START_PROTECTION_MS = 3000L
  const val DECEL_SUSPECT_THRESHOLD = -0.30f
  const val DECEL_SUSTAIN_MS = 800L
  const val DECEL_RECOVERY_THRESHOLD = -0.05f
  const val GPS_MIN_DROP_KMH = 3.0f
  const val GPS_STRONG_DROP_KMH = 5.0f
  const val MIN_VALID_DURATION_MS = 4000L
  const val MIN_VALID_SAMPLES = 80
  const val MIN_GPS_UPDATES = 4
  const val MIN_SPEED_GAIN_KMH = 10.0f
}

/**
 * Registro de leitura de fix GPS para histórico de sincronização e detecção de desaceleração.
 */
data class GpsFixRecord(
  val timestampMs: Long,
  val speedKmh: Float,
  val accuracyM: Float,
  val elapsedRealtimeNs: Long,
  val runElapsedSec: Float
)

/**
 * Ponto inercial gravado em alta resolução para interpolação exata por timestamp com o GPS.
 */
data class InertialPoint(
  val elapsedRealtimeNs: Long,
  val speedKmh: Float,
  val speedMs: Float,
  val accelZ: Float,
  val isValid: Boolean
)

/**
 * Estado imutável completo exposto para a interface Compose via StateFlow.
 */
data class DynoUiState(
  val gpsSpeedKmh: Float = 0f,
  val integratedSpeedKmh: Float = 0f,
  val displaySpeedKmh: Float = 0f,
  val longitudinalG: Float = 0f,
  val peakLongitudinalG: Float = 0f,
  val livePowerCv: Float = 0f,
  val gpsAccuracyMeters: Float = 0f,
  val gpsTimestamp: Long = 0L,
  val gpsAgeMillis: Long = 0L,
  val locationUpdateCount: Int = 0,
  val gpsFrequencyHz: Float = 0f,
  val syncPairsCount: Int = 0,
  val averageSyncDiffKmh: Float = 0f,
  val maxSyncDiffKmh: Float = 0f,
  val maxGpsSpeedKmh: Float = 0f,
  val maxIntegratedSpeedKmh: Float = 0f,
  val maxDisplaySpeedKmh: Float = 0f,
  val testState: DynoRunState = DynoRunState.PARADO,
  val isCalibrated: Boolean = false,
  val isCalibrating: Boolean = false,
  val calibProgressPercent: Int = 0,
  val isGpsReady: Boolean = false,
  val hasGpsFix: Boolean = false,
  val isGpsProviderEnabled: Boolean = false,
  val isStoppedForTwoSeconds: Boolean = true,
  val isPhoneStable: Boolean = true,
  val hasPhoneMovedAfterCalib: Boolean = false,
  val runElapsedSeconds: Float = 0f,
  val startSpeedTriggerKmh: Float = 40.0f,
  val calibrationStatusText: String = "Não calibrado"
)

class TestPreparationViewModel(application: Application) : AndroidViewModel(application) {

  private val prefs: SharedPreferences =
    application.getSharedPreferences("dyno_lite_prefs", Context.MODE_PRIVATE)

  private val runResultRepository = RunResultRepository(application)
  private val fusedLocationClient: FusedLocationProviderClient =
    LocationServices.getFusedLocationProviderClient(application)
  private val sensorManager: SensorManager? =
    application.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

  private val linearAccelerationSensor: Sensor? =
    sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
  private val gyroscopeSensor: Sensor? =
    sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

  private val _uiState = MutableStateFlow(
    DynoUiState(
      isCalibrated = prefs.getBoolean("is_calibrated", false),
      startSpeedTriggerKmh = prefs.getFloat("start_speed_trigger_kmh", 40.0f),
      calibrationStatusText = if (prefs.getBoolean("is_calibrated", false)) "Calibração concluída" else "Não calibrado"
    )
  )
  val uiState: StateFlow<DynoUiState> = _uiState.asStateFlow()

  // Calibração
  private var offsetX = prefs.getFloat("offset_x", 0.0f)
  private var offsetY = prefs.getFloat("offset_y", 0.0f)
  private var offsetZ = prefs.getFloat("offset_z", 0.0f)
  private var calibratedNormalVibration = prefs.getFloat("calibrated_vibration", 0.12f)
  private var calibratedGyroDeviation = prefs.getFloat("calibrated_gyro", 0.08f)
  private var invertSignal = prefs.getBoolean("invert_longitudinal_signal", false)

  // Sensores brutos
  private var linearX = 0f
  private var linearY = 0f
  private var linearZ = 0f
  private var gyroX = 0f
  private var gyroY = 0f
  private var gyroZ = 0f

  // Filtragem longitudinal
  private val zMedianBuffer = mutableListOf<Float>()
  private var zFiltradoRun = 0f

  // 1. TRÊS VELOCIDADES SEPARADAS
  @Volatile private var gpsSpeedKmh: Float = 0f
  @Volatile private var integratedSpeedKmh: Float = 0f
  @Volatile private var displaySpeedKmh: Float = 0f
  @Volatile private var lastGpsAnchorSpeedMps: Float = 0f
  @Volatile private var lastGpsAnchorNanoTime: Long = 0L
  @Volatile private var integralSpeedSinceLastGpsMps: Float = 0f

  // GPS Tracking & Validação
  @Volatile private var lastProcessedGpsElapsedRealtimeNs: Long = 0L
  @Volatile private var lastProcessedGpsTimestamp: Long = 0L
  @Volatile private var lastGpsArrivalWallTimeMs: Long = 0L
  @Volatile private var lastGpsIntervalMs: Long = 0L
  @Volatile private var locationUpdateCount: Int = 0
  @Volatile private var validGpsUpdatesDuringRunCount: Int = 0
  @Volatile private var lastGpsAccuracyMeters: Float = 0f

  // Parado detection
  private var stoppedStartTimeMs: Long = SystemClock.elapsedRealtime()

  // Run Tracking
  private var runStartTimeNs: Long = 0L
  private var runEndTimeNs: Long = 0L
  private var lastSensorTimestampNs: Long = 0L
  private var lastSampleRecordedNs: Long = 0L
  private var armedEstimatedSpeedMs: Float = 0f
  private var armedLastNanoTime: Long = 0L

  private var startCalculatedKmh: Float = 40.0f
  private var startGpsKmh: Float = 0f
  private var maxGpsSpeedKmh: Float = 0f
  private var maxIntegratedSpeedKmh: Float = 0f
  private var maxDisplaySpeedKmh: Float = 0f
  private var finalGpsKmh: Float = 0f
  private var finalCalcSpeedKmh: Float = 0f

  private var suspectStartTimeNs: Long? = null
  private var suspectNegativeSampleCount: Int = 0
  private var clutchStartTimeNs: Long? = null

  // Sincronização GPS x Acelerômetro por Interpolação de Timestamp
  private val inertialHistory = mutableListOf<InertialPoint>()
  private val recordedSamples = mutableListOf<RunSample>()
  private val gpsFixHistory = mutableListOf<GpsFixRecord>()
  private val diagnosticLogs = mutableListOf<String>()

  private var syncDiffSum = 0.0
  private var syncDiffCount = 0
  private var maxSyncDiff = 0f
  private var totalInertialSamples = 0
  private var rejectedInertialSamples = 0
  private var resultSaved = false

  // Calibração
  private var isCalibrating = false
  private var calibSampleCount = 0
  private var calibSumX = 0.0
  private var calibSumY = 0.0
  private var calibSumZ = 0.0
  private var calibSumDevX = 0.0
  private var calibSumDevY = 0.0
  private var calibSumDevZ = 0.0
  private var calibSumGyroMag = 0.0

  private var screenStabilizedTimestampMs: Long = 0L
  private var persistentMovementCount: Int = 0
  private var hasPhoneMovedAfterCalib: Boolean = false

  private var peakLongitudinalG: Float = 0f
  private var liveLongitudinalG: Float = 0f

  // Listener GPS e Sensor Callbacks
  private var isGpsLocationCallbackActive = false
  private var isSensorListenerActive = false

  private var onRunCompletedCallback: ((Boolean) -> Unit)? = null

  init {
    screenStabilizedTimestampMs = SystemClock.elapsedRealtime()
    startDisplaySpeedUpdateLoop()
  }

  fun setOnRunCompletedCallback(callback: (Boolean) -> Unit) {
    this.onRunCompletedCallback = callback
  }

  fun setStartSpeedTrigger(speedKmh: Float) {
    if (_uiState.value.testState == DynoRunState.PARADO) {
      prefs.edit().putFloat("start_speed_trigger_kmh", speedKmh).apply()
      _uiState.update { it.copy(startSpeedTriggerKmh = speedKmh) }
    }
  }

  /**
   * 4. LOOP DE ATUALIZAÇÃO EM TEMPO REAL DO VELOCÍMETRO (10 a 20 Hz / ~60ms)
   * displaySpeedKmh = displaySpeedKmh + alpha * (targetSpeed - displaySpeedKmh)
   */
  private fun startDisplaySpeedUpdateLoop() {
    viewModelScope.launch {
      while (isActive) {
        delay(60L) // ~16.6 Hz

        val nowNs = SystemClock.elapsedRealtimeNanos()
        val nowMs = SystemClock.elapsedRealtime()
        val currentState = _uiState.value.testState

        val gpsAge = if (lastProcessedGpsElapsedRealtimeNs > 0L) {
          ((nowNs - lastProcessedGpsElapsedRealtimeNs) / 1_000_000L).coerceAtLeast(0L)
        } else 9999L

        val isStopped = gpsSpeedKmh < 1.0f && (nowMs - stoppedStartTimeMs >= 2000L)

        val targetSpeed: Float = when {
          isStopped -> 0f
          currentState == DynoRunState.MEDINDO_PROTEGIDO || currentState == DynoRunState.MEDINDO -> {
            // Durante medição: o velocímetro acompanha a integração inercial com suavização
            integratedSpeedKmh
          }
          currentState == DynoRunState.AGUARDANDO_INICIO -> {
            // Armado: combina último GPS com a evolução inercial desde a última leitura
            val predictedMps = (lastGpsAnchorSpeedMps + integralSpeedSinceLastGpsMps).coerceAtLeast(0f)
            val predictedKmh = predictedMps * 3.6f
            if (gpsAge < 1000L) {
              gpsSpeedKmh * 0.70f + predictedKmh * 0.30f
            } else {
              predictedKmh
            }
          }
          else -> {
            // Parado ou normal: acompanha o GPS suavemente
            if (gpsSpeedKmh < 0.8f) 0f else gpsSpeedKmh
          }
        }

        // Correção visual suave com alpha entre 0.20 e 0.35
        val alpha = if (isStopped) 0.40f else 0.28f
        val newDisplaySpeed = (displaySpeedKmh + alpha * (targetSpeed - displaySpeedKmh)).coerceAtLeast(0f)
        val finalDisplaySpeed = if (isStopped && newDisplaySpeed < 0.5f) 0f else newDisplaySpeed
        displaySpeedKmh = finalDisplaySpeed

        if (finalDisplaySpeed > maxDisplaySpeedKmh) {
          maxDisplaySpeedKmh = finalDisplaySpeed
        }

        val gpsFreqHz = if (lastGpsIntervalMs > 0L) (1000f / lastGpsIntervalMs).coerceIn(0.1f, 20f) else 0f
        val avgDiff = if (syncDiffCount > 0) (syncDiffSum / syncDiffCount).toFloat() else 0f

        val currentGyroMag = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)
        val isPhoneStable = currentGyroMag <= 0.65f
        val isGpsReady = lastGpsAccuracyMeters in 0.1f..15.0f && gpsAge < 3000L && locationUpdateCount > 0

        _uiState.update { current ->
          current.copy(
            gpsSpeedKmh = gpsSpeedKmh,
            integratedSpeedKmh = integratedSpeedKmh,
            displaySpeedKmh = finalDisplaySpeed,
            longitudinalG = liveLongitudinalG,
            peakLongitudinalG = peakLongitudinalG,
            gpsAccuracyMeters = lastGpsAccuracyMeters,
            gpsTimestamp = lastProcessedGpsTimestamp,
            gpsAgeMillis = gpsAge,
            locationUpdateCount = locationUpdateCount,
            gpsFrequencyHz = gpsFreqHz,
            syncPairsCount = syncDiffCount,
            averageSyncDiffKmh = avgDiff,
            maxSyncDiffKmh = maxSyncDiff,
            maxGpsSpeedKmh = maxGpsSpeedKmh,
            maxIntegratedSpeedKmh = maxIntegratedSpeedKmh,
            maxDisplaySpeedKmh = maxDisplaySpeedKmh,
            testState = currentState,
            isGpsReady = isGpsReady,
            hasGpsFix = locationUpdateCount > 0 && gpsAge < 5000L,
            isStoppedForTwoSeconds = isStopped,
            isPhoneStable = isPhoneStable,
            hasPhoneMovedAfterCalib = hasPhoneMovedAfterCalib,
            runElapsedSeconds = if (runStartTimeNs > 0L && (currentState == DynoRunState.MEDINDO_PROTEGIDO || currentState == DynoRunState.MEDINDO || currentState == DynoRunState.SUSPEITA_DESACELERACAO)) {
              ((System.nanoTime() - runStartTimeNs) / 1_000_000L) / 1000f
            } else current.runElapsedSeconds
          )
        }
      }
    }
  }

  // 2. CONFIGURAÇÃO DO FUSED LOCATION PROVIDER CLIENT
  private val locationCallback = object : LocationCallback() {
    override fun onLocationResult(result: LocationResult) {
      val locations = result.locations
      if (locations.isNullOrEmpty()) return

      for (location in locations) {
        processNewLocation(location)
      }
    }
  }

  @SuppressLint("MissingPermission")
  fun startLocationUpdates() {
    if (isGpsLocationCallbackActive) return

    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 200L)
      .setMinUpdateIntervalMillis(100L)
      .setMaxUpdateDelayMillis(0L)
      .setWaitForAccurateLocation(false)
      .build()

    try {
      fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
      isGpsLocationCallbackActive = true
      _uiState.update { it.copy(isGpsProviderEnabled = true) }
    } catch (e: Exception) {
      isGpsLocationCallbackActive = false
    }
  }

  fun stopLocationUpdates() {
    if (isGpsLocationCallbackActive) {
      try {
        fusedLocationClient.removeLocationUpdates(locationCallback)
      } catch (e: Exception) {}
      isGpsLocationCallbackActive = false
    }
  }

  /**
   * 3. VALIDAR LEITURA GPS NOVA
   * Cada Location deve ser identificada por location.elapsedRealtimeNanos
   */
  private fun processNewLocation(location: Location) {
    val elapsedRealtimeNs = location.elapsedRealtimeNanos
    val locationTime = location.time

    // Validar se a Location é estritamente mais nova que a anterior
    if (elapsedRealtimeNs <= lastProcessedGpsElapsedRealtimeNs && lastProcessedGpsElapsedRealtimeNs != 0L) {
      return // Leitura repetida ou desordenada descartada
    }

    val nowWallMs = SystemClock.elapsedRealtime()
    if (lastGpsArrivalWallTimeMs > 0L) {
      lastGpsIntervalMs = nowWallMs - lastGpsArrivalWallTimeMs
    }
    lastGpsArrivalWallTimeMs = nowWallMs

    lastProcessedGpsElapsedRealtimeNs = elapsedRealtimeNs
    lastProcessedGpsTimestamp = locationTime
    locationUpdateCount++

    val rawSpeedMps = if (location.hasSpeed()) location.speed else 0f
    val speedKmh = (rawSpeedMps * 3.6f).coerceAtLeast(0f)
    gpsSpeedKmh = speedKmh
    lastGpsAccuracyMeters = if (location.hasAccuracy()) location.accuracy else 99f

    // Ancorar a velocidade inercial auxiliar estritamente na velocidade GPS real
    if (speedKmh > 0f && lastGpsAccuracyMeters <= 15.0f) {
      integratedSpeedKmh = speedKmh
    }

    // Atualiza âncora para previsão inercial entre GPS
    lastGpsAnchorSpeedMps = rawSpeedMps
    lastGpsAnchorNanoTime = System.nanoTime()
    integralSpeedSinceLastGpsMps = 0f

    if (speedKmh >= 1.0f) {
      stoppedStartTimeMs = nowWallMs
    }

    val currentState = _uiState.value.testState

    if (currentState == DynoRunState.AGUARDANDO_INICIO) {
      val targetTrigger = _uiState.value.startSpeedTriggerKmh
      val isCalib = _uiState.value.isCalibrated
      // Gatilho de início baseado estritamente na velocidade GPS oficial
      if (speedKmh >= targetTrigger && lastGpsAccuracyMeters <= 12.0f && isCalib) {
        val nowNs = System.nanoTime()
        triggerOfficialRunStart(nowNs, speedKmh)
      }
    } else if (currentState == DynoRunState.MEDINDO_PROTEGIDO ||
      currentState == DynoRunState.MEDINDO ||
      currentState == DynoRunState.SUSPEITA_DESACELERACAO) {

      if (speedKmh > maxGpsSpeedKmh) {
        maxGpsSpeedKmh = speedKmh
      }

      val gpsAgeMillis = ((SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNs) / 1_000_000L).coerceAtLeast(0L)

      if (location.hasSpeed() && lastGpsAccuracyMeters <= 12.0f) {
        validGpsUpdatesDuringRunCount++
        val runElapsedSec = ((System.nanoTime() - runStartTimeNs) / 1_000_000L) / 1000f

        val fixRecord = GpsFixRecord(
          timestampMs = locationTime,
          speedKmh = speedKmh,
          accuracyM = lastGpsAccuracyMeters,
          elapsedRealtimeNs = elapsedRealtimeNs,
          runElapsedSec = runElapsedSec
        )
        gpsFixHistory.add(fixRecord)

        // 7 e 8. SINCRONIZAÇÃO GPS x ACELERÔMETRO POR INTERPOLAÇÃO DE TIMESTAMP
        // Executar comparação sincronizada apenas durante aceleração ativa (antes da suspeita confirmada de desaceleração)
        if (currentState == DynoRunState.MEDINDO_PROTEGIDO || currentState == DynoRunState.MEDINDO) {
          if (gpsAgeMillis <= 1500L) {
            interpolateAndCompareGpsWithInertial(elapsedRealtimeNs, speedKmh)
          }
        }

        // Checagem de Finalização Confirmada por GPS (apenas após o período protegido de 3 segundos)
        val runDurationMs = ((System.nanoTime() - runStartTimeNs) / 1_000_000L)
        if (runDurationMs >= DynoConfig.START_PROTECTION_MS) {
          checkGpsDecelerationConditions(speedKmh, locationTime)
        }
      }
    }
  }

  /**
   * 7. SINCRONIZAÇÃO GPS x ACELERÔMETRO POR INTERPOLAÇÃO
   * Localiza as duas amostras calculadas imediatamente antes e depois do timestamp da Location
   * e interpola a velocidade calculada naquele mesmo instante.
   */
  private fun interpolateAndCompareGpsWithInertial(gpsElapsedRealtimeNs: Long, gpsSpeedKmh: Float) {
    if (inertialHistory.size < 2) return

    var sampleBefore: InertialPoint? = null
    var sampleAfter: InertialPoint? = null

    // Procura as amostras que circundam o timestamp exato do GPS
    for (i in 0 until inertialHistory.size - 1) {
      val p1 = inertialHistory[i]
      val p2 = inertialHistory[i + 1]
      if (p1.elapsedRealtimeNs <= gpsElapsedRealtimeNs && p2.elapsedRealtimeNs >= gpsElapsedRealtimeNs) {
        sampleBefore = p1
        sampleAfter = p2
        break
      }
    }

    if (sampleBefore != null && sampleAfter != null) {
      val dt = sampleAfter.elapsedRealtimeNs - sampleBefore.elapsedRealtimeNs
      if (dt > 0L) {
        val fraction = (gpsElapsedRealtimeNs - sampleBefore.elapsedRealtimeNs).toFloat() / dt.toFloat()
        val calculatedSpeedAtGpsTime = sampleBefore.speedKmh + (sampleAfter.speedKmh - sampleBefore.speedKmh) * fraction

        val diff = abs(gpsSpeedKmh - calculatedSpeedAtGpsTime)
        syncDiffSum += diff
        syncDiffCount++
        if (diff > maxSyncDiff) {
          maxSyncDiff = diff
        }
        diagnosticLogs.add("Sincronização GPS: gpsSpeed=${String.format(Locale.US, "%.1f", gpsSpeedKmh)} km/h, calcSpeed=${String.format(Locale.US, "%.1f", calculatedSpeedAtGpsTime)} km/h, diff=${String.format(Locale.US, "%.2f", diff)} km/h")
      }
    }
  }

  private fun checkGpsDecelerationConditions(currentGpsKmh: Float, locationTimeMs: Long) {
    val history = gpsFixHistory
    val historySize = history.size

    var conditionAMet = false
    if (historySize >= 3) {
      val lastFix = history[historySize - 1]
      val prevFix = history[historySize - 2]
      val prevPrevFix = history[historySize - 3]

      val isConsecutiveDecreasing = (lastFix.speedKmh < prevFix.speedKmh) && (prevFix.speedKmh <= prevPrevFix.speedKmh)
      val dropFromPrevPrev = prevPrevFix.speedKmh - lastFix.speedKmh
      val timeIntervalMs = lastFix.timestampMs - prevPrevFix.timestampMs

      if (isConsecutiveDecreasing && dropFromPrevPrev >= DynoConfig.GPS_MIN_DROP_KMH && timeIntervalMs >= 600L) {
        conditionAMet = true
        diagnosticLogs.add("Condição A satisfeita: queda=${dropFromPrevPrev}km/h em ${timeIntervalMs}ms")
      }
    } else if (historySize == 2) {
      val lastFix = history[historySize - 1]
      val prevFix = history[historySize - 2]
      val drop = prevFix.speedKmh - lastFix.speedKmh
      val timeIntervalMs = lastFix.timestampMs - prevFix.timestampMs
      if (drop >= DynoConfig.GPS_MIN_DROP_KMH && timeIntervalMs >= 600L) {
        conditionAMet = true
        diagnosticLogs.add("Condição A satisfeita (2 fixes): queda=${drop}km/h em ${timeIntervalMs}ms")
      }
    }

    var conditionBMet = false
    val dropFromMax = maxGpsSpeedKmh - currentGpsKmh
    if (dropFromMax >= DynoConfig.GPS_STRONG_DROP_KMH) {
      if (historySize >= 2) {
        val lastFix = history[historySize - 1]
        val prevFix = history[historySize - 2]
        if (lastFix.speedKmh <= prevFix.speedKmh + 0.5f) {
          conditionBMet = true
          diagnosticLogs.add("Condição B satisfeita: queda=${dropFromMax}km/h de max ($maxGpsSpeedKmh -> $currentGpsKmh)")
        }
      } else {
        conditionBMet = true
        diagnosticLogs.add("Condição B satisfeita: queda=${dropFromMax}km/h de max")
      }
    }

    val currentState = _uiState.value.testState
    if ((conditionAMet || conditionBMet) && (currentState == DynoRunState.SUSPEITA_DESACELERACAO || zFiltradoRun <= 0.05f)) {
      _uiState.update { it.copy(testState = DynoRunState.FINALIZANDO) }
      diagnosticLogs.add("GPS confirmou desaceleração (CondA=$conditionAMet, CondB=$conditionBMet). Finalizando teste.")
      finalizeRun(FinishReason.GPS_DECELERATION)
    }
  }

  // SENSORES (ACELERÔMETRO E GIROSCÓPIO)
  private val sensorEventListener = object : SensorEventListener {
    override fun onSensorChanged(event: SensorEvent?) {
      when (event?.sensor?.type) {
        Sensor.TYPE_LINEAR_ACCELERATION -> {
          if (event.values.size >= 3) {
            linearX = event.values[0]
            linearY = event.values[1]
            linearZ = event.values[2]

            val rawLinearZ = event.values[2]
            val rawCorrigidoZ = (rawLinearZ - offsetZ) * (if (invertSignal) -1f else 1f)

            // Filtro mediano de 5 amostras
            zMedianBuffer.add(rawCorrigidoZ)
            if (zMedianBuffer.size > 5) {
              zMedianBuffer.removeAt(0)
            }
            val sortedZ = zMedianBuffer.sorted()
            val medianaZ = if (sortedZ.isNotEmpty()) sortedZ[sortedZ.size / 2] else 0f

            // Filtro passa-baixas (~350ms)
            zFiltradoRun += 0.12f * (medianaZ - zFiltradoRun)

            // Zona morta no eixo longitudinal [-0.25, +0.25] m/s²
            val zDeadZone = if (abs(zFiltradoRun) <= DynoConfig.ACCEL_DEAD_ZONE) 0f else zFiltradoRun

            val currentG = zFiltradoRun / 9.80665f
            liveLongitudinalG = currentG
            if (currentG > peakLongitudinalG && (_uiState.value.testState == DynoRunState.MEDINDO_PROTEGIDO || _uiState.value.testState == DynoRunState.MEDINDO)) {
              peakLongitudinalG = currentG
            }

            val nowNs = System.nanoTime()
            val elapsedRealtimeNs = SystemClock.elapsedRealtimeNanos()

            // Detecção de movimentação física do aparelho pós-calibração
            val isScreenStable = (SystemClock.elapsedRealtime() - screenStabilizedTimestampMs) > 1500L
            if (_uiState.value.isCalibrated && !isCalibrating && _uiState.value.testState == DynoRunState.PARADO && gpsSpeedKmh < 3f && isScreenStable) {
              val gMag = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)
              if (gMag > 3.2f) {
                persistentMovementCount++
                if (persistentMovementCount >= 25) {
                  hasPhoneMovedAfterCalib = true
                  persistentMovementCount = 0
                  _uiState.update { it.copy(isCalibrated = false, hasPhoneMovedAfterCalib = true, calibrationStatusText = "O celular mudou de posição. Calibre novamente.") }
                  prefs.edit().putBoolean("is_calibrated", false).apply()
                }
              } else {
                if (persistentMovementCount > 0) persistentMovementCount--
              }
            }

            val currentState = _uiState.value.testState

            if (currentState == DynoRunState.AGUARDANDO_INICIO) {
              if (armedLastNanoTime != 0L) {
                val dt = (nowNs - armedLastNanoTime) / 1_000_000_000f
                if (dt in 0.001f..0.1f) {
                  armedEstimatedSpeedMs = (armedEstimatedSpeedMs + zDeadZone * dt).coerceAtLeast(0f)
                  integralSpeedSinceLastGpsMps += zDeadZone * dt
                }
              }
              armedLastNanoTime = nowNs

              val targetTrigger = _uiState.value.startSpeedTriggerKmh
              val isCalib = _uiState.value.isCalibrated
              // Gatilho oficial baseado estritamente na velocidade GPS
              if (gpsSpeedKmh >= targetTrigger && lastGpsAccuracyMeters <= 12.0f && isCalib) {
                triggerOfficialRunStart(nowNs, gpsSpeedKmh)
              }
            } else if (currentState == DynoRunState.MEDINDO_PROTEGIDO ||
              currentState == DynoRunState.MEDINDO ||
              currentState == DynoRunState.SUSPEITA_DESACELERACAO) {

              val runDurationMs = (nowNs - runStartTimeNs) / 1_000_000L
              val elapsedSec = runDurationMs / 1000f

              // Integração inercial contínua da velocidade
              var currentCalcKmh = integratedSpeedKmh
              var currentVelocityMs = integratedSpeedKmh / 3.6f

              if (lastSensorTimestampNs != 0L) {
                val dt = (nowNs - lastSensorTimestampNs) / 1_000_000_000f
                if (dt in 0.001f..0.1f) {
                  if (zDeadZone != 0f) {
                    currentVelocityMs = (currentVelocityMs + zDeadZone * dt).coerceAtLeast(0f)
                  }
                  currentCalcKmh = currentVelocityMs * 3.6f
                  integratedSpeedKmh = currentCalcKmh

                  if (currentCalcKmh > maxIntegratedSpeedKmh) {
                    maxIntegratedSpeedKmh = currentCalcKmh
                  }
                }
              }
              lastSensorTimestampNs = nowNs

              val corrX = abs(event.values[0] - offsetX)
              val corrY = abs(event.values[1] - offsetY)
              val gyroMag = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)
              val maxNormalVib = max(3.5f, calibratedNormalVibration * 2.5f)
              val maxNormalGyro = max(2.5f, calibratedGyroDeviation * 3.0f)

              totalInertialSamples++
              val isSampleValid = !(corrX > maxNormalVib || corrY > maxNormalVib || gyroMag > maxNormalGyro)
              if (!isSampleValid) {
                rejectedInertialSamples++
              }

              // Salva ponto inercial de alta resolução para interpolação GPS
              inertialHistory.add(
                InertialPoint(
                  elapsedRealtimeNs = elapsedRealtimeNs,
                  speedKmh = currentCalcKmh,
                  speedMs = currentVelocityMs,
                  accelZ = zFiltradoRun,
                  isValid = isSampleValid
                )
              )
              if (inertialHistory.size > 2500) {
                inertialHistory.removeAt(0)
              }

              // Gravação da série temporal (~20 Hz)
              if (nowNs - lastSampleRecordedNs >= 50_000_000L && recordedSamples.size < 500) {
                lastSampleRecordedNs = nowNs
                val diff = abs(currentCalcKmh - gpsSpeedKmh)

                val rejReason = if (!isSampleValid) {
                  when {
                    corrX > maxNormalVib -> "Vibração lateral X excessiva"
                    corrY > maxNormalVib -> "Vibração vertical Y excessiva"
                    else -> "Giroscópio elevado"
                  }
                } else null

                val samplePoint = RunSample(
                  elapsedTimeMs = runDurationMs,
                  filteredAccelerationZ = zFiltradoRun,
                  correctedAccelerationZ = rawCorrigidoZ,
                  longitudinalG = currentG,
                  gpsSpeedKmh = gpsSpeedKmh,
                  calculatedSpeedKmh = currentCalcKmh,
                  speedDifferenceKmh = diff,
                  gpsAccuracyMeters = lastGpsAccuracyMeters,
                  gyroMagnitude = gyroMag,
                  isValid = isSampleValid,
                  rejectionReason = rejReason
                )
                recordedSamples.add(samplePoint)
              }

              // 2. PERÍODO DE PROTEÇÃO DE INÍCIO (3 SEGUNDOS)
              if (runDurationMs < DynoConfig.START_PROTECTION_MS) {
                if (currentState != DynoRunState.MEDINDO_PROTEGIDO) {
                  _uiState.update { it.copy(testState = DynoRunState.MEDINDO_PROTEGIDO) }
                }
                suspectStartTimeNs = null
                suspectNegativeSampleCount = 0
                clutchStartTimeNs = null
              } else {
                if (currentState == DynoRunState.MEDINDO_PROTEGIDO) {
                  _uiState.update { it.copy(testState = DynoRunState.MEDINDO) }
                  diagnosticLogs.add("Período de proteção (3s) concluído. Estado: MEDINDO")
                }

                // 4. SUSPEITA DE DESACELERAÇÃO
                if (currentState == DynoRunState.MEDINDO) {
                  if (zDeadZone < DynoConfig.DECEL_SUSPECT_THRESHOLD) {
                    suspectNegativeSampleCount++
                    if (suspectStartTimeNs == null) {
                      suspectStartTimeNs = nowNs
                    } else {
                      val suspectMs = (nowNs - suspectStartTimeNs!!) / 1_000_000L
                      if (suspectMs >= DynoConfig.DECEL_SUSTAIN_MS && suspectNegativeSampleCount >= 15) {
                        _uiState.update { it.copy(testState = DynoRunState.SUSPEITA_DESACELERACAO) }
                        diagnosticLogs.add("SUSPEITA DE DESACELERAÇÃO: z=$zFiltradoRun, amostras=$suspectNegativeSampleCount, dur=${suspectMs}ms")
                      }
                    }
                  } else if (zDeadZone > DynoConfig.DECEL_RECOVERY_THRESHOLD) {
                    suspectStartTimeNs = null
                    suspectNegativeSampleCount = 0
                  }
                } else if (currentState == DynoRunState.SUSPEITA_DESACELERACAO) {
                  if (zDeadZone > DynoConfig.DECEL_RECOVERY_THRESHOLD) {
                    _uiState.update { it.copy(testState = DynoRunState.MEDINDO) }
                    suspectStartTimeNs = null
                    suspectNegativeSampleCount = 0
                    diagnosticLogs.add("Recuperação da aceleração (z=$zFiltradoRun > ${DynoConfig.DECEL_RECOVERY_THRESHOLD}). Retornando para MEDINDO.")
                  }
                }
              }

              // Detecção de embreagem
              val wasAccelerating = maxGpsSpeedKmh >= (startGpsKmh + 3.0f)
              if (wasAccelerating && zDeadZone < DynoConfig.DECEL_SUSPECT_THRESHOLD && gpsSpeedKmh <= maxGpsSpeedKmh) {
                if (clutchStartTimeNs == null) clutchStartTimeNs = nowNs
              } else if (gpsSpeedKmh > maxGpsSpeedKmh) {
                clutchStartTimeNs = null
              }

              // Timeout de segurança após 25 segundos
              if (elapsedSec > 25.0f) {
                diagnosticLogs.add("Timeout de segurança de 25s atingido.")
                finalizeRun(FinishReason.TIMEOUT)
              }
            }

            // Calibração
            if (isCalibrating) {
              val count = calibSampleCount
              if (count > 10) {
                val partialAvgX = (calibSumX / count).toFloat()
                val partialAvgY = (calibSumY / count).toFloat()
                val partialAvgZ = (calibSumZ / count).toFloat()
                val gyroMag = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)

                if (abs(event.values[0] - partialAvgX) > 2.2f ||
                  abs(event.values[1] - partialAvgY) > 2.2f ||
                  abs(event.values[2] - partialAvgZ) > 2.2f ||
                  gyroMag > 2.0f) {
                  resetCalibrationCollector()
                  isCalibrating = false
                  _uiState.update { it.copy(isCalibrating = false, calibrationStatusText = "O aparelho se moveu durante a calibração") }
                }
              }

              if (isCalibrating) {
                calibSumX += event.values[0]
                calibSumY += event.values[1]
                calibSumZ += event.values[2]

                val currentAvgX = (calibSumX / (count + 1)).toFloat()
                val currentAvgY = (calibSumY / (count + 1)).toFloat()
                val currentAvgZ = (calibSumZ / (count + 1)).toFloat()

                calibSumDevX += abs(event.values[0] - currentAvgX)
                calibSumDevY += abs(event.values[1] - currentAvgY)
                calibSumDevZ += abs(event.values[2] - currentAvgZ)

                val gMag = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)
                calibSumGyroMag += gMag

                calibSampleCount++
                val newCount = calibSampleCount
                val progress = (newCount * 100) / 150

                _uiState.update { it.copy(calibProgressPercent = progress, calibrationStatusText = "CALIBRANDO $progress%") }

                if (newCount >= 150) {
                  val avgX = (calibSumX / 150.0).toFloat()
                  val avgY = (calibSumY / 150.0).toFloat()
                  val avgZ = (calibSumZ / 150.0).toFloat()

                  val normVib = ((calibSumDevX + calibSumDevY + calibSumDevZ) / 450.0).toFloat()
                  val avgGyroDev = (calibSumGyroMag / 150.0).toFloat()

                  offsetX = avgX
                  offsetY = avgY
                  offsetZ = avgZ
                  calibratedNormalVibration = normVib
                  calibratedGyroDeviation = avgGyroDev
                  hasPhoneMovedAfterCalib = false
                  isCalibrating = false
                  resetCalibrationCollector()

                  prefs.edit()
                    .putFloat("offset_x", avgX)
                    .putFloat("offset_y", avgY)
                    .putFloat("offset_z", avgZ)
                    .putFloat("calibrated_vibration", normVib)
                    .putFloat("calibrated_gyro", avgGyroDev)
                    .putBoolean("is_calibrated", true)
                    .apply()

                  _uiState.update {
                    it.copy(
                      isCalibrated = true,
                      isCalibrating = false,
                      hasPhoneMovedAfterCalib = false,
                      calibrationStatusText = "Calibração concluída"
                    )
                  }
                }
              }
            }
          }
        }
        Sensor.TYPE_GYROSCOPE -> {
          if (event.values.size >= 3) {
            gyroX = event.values[0]
            gyroY = event.values[1]
            gyroZ = event.values[2]
          }
        }
      }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
  }

  fun startSensorUpdates() {
    if (isSensorListenerActive) return
    if (sensorManager != null) {
      if (linearAccelerationSensor != null) {
        sensorManager.registerListener(sensorEventListener, linearAccelerationSensor, SensorManager.SENSOR_DELAY_GAME)
      }
      if (gyroscopeSensor != null) {
        sensorManager.registerListener(sensorEventListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME)
      }
      isSensorListenerActive = true
    }
  }

  fun stopSensorUpdates() {
    if (isSensorListenerActive) {
      sensorManager?.unregisterListener(sensorEventListener)
      isSensorListenerActive = false
    }
  }

  private fun resetCalibrationCollector() {
    calibSampleCount = 0
    calibSumX = 0.0
    calibSumY = 0.0
    calibSumZ = 0.0
    calibSumDevX = 0.0
    calibSumDevY = 0.0
    calibSumDevZ = 0.0
    calibSumGyroMag = 0.0
  }

  fun startCalibration() {
    if (!isCalibrating && _uiState.value.testState == DynoRunState.PARADO) {
      resetCalibrationCollector()
      isCalibrating = true
      _uiState.update { it.copy(isCalibrating = true, calibProgressPercent = 0, calibrationStatusText = "CALIBRANDO 0%") }
    }
  }

  fun armTest() {
    if (_uiState.value.isCalibrated && _uiState.value.testState == DynoRunState.PARADO) {
      resetRunData()
      _uiState.update { it.copy(testState = DynoRunState.AGUARDANDO_INICIO) }
      armedEstimatedSpeedMs = 0f
      armedLastNanoTime = System.nanoTime()
      resultSaved = false
    }
  }

  fun cancelTest() {
    resetRunData()
    _uiState.update { it.copy(testState = DynoRunState.PARADO) }
  }

  private fun triggerOfficialRunStart(nowNs: Long, availableGpsKmh: Float) {
    val actualGpsSpeed = availableGpsKmh.coerceAtLeast(0f)

    runStartTimeNs = nowNs
    lastSensorTimestampNs = nowNs
    lastSampleRecordedNs = nowNs

    startCalculatedKmh = actualGpsSpeed
    startGpsKmh = actualGpsSpeed
    integratedSpeedKmh = actualGpsSpeed
    maxIntegratedSpeedKmh = actualGpsSpeed
    maxGpsSpeedKmh = actualGpsSpeed
    maxDisplaySpeedKmh = actualGpsSpeed

    suspectStartTimeNs = null
    suspectNegativeSampleCount = 0
    clutchStartTimeNs = null

    gpsFixHistory.clear()
    diagnosticLogs.clear()
    diagnosticLogs.add("Início oficial da passagem: gatilho=${_uiState.value.startSpeedTriggerKmh} km/h, gpsInicial=$actualGpsSpeed km/h")

    totalInertialSamples = 0
    rejectedInertialSamples = 0
    syncDiffSum = 0.0
    syncDiffCount = 0
    maxSyncDiff = 0f
    validGpsUpdatesDuringRunCount = 1
    resultSaved = false
    recordedSamples.clear()
    inertialHistory.clear()

    val firstSample = RunSample(
      elapsedTimeMs = 0L,
      filteredAccelerationZ = zFiltradoRun,
      correctedAccelerationZ = (linearZ - offsetZ) * (if (invertSignal) -1f else 1f),
      gpsSpeedKmh = actualGpsSpeed,
      calculatedSpeedKmh = actualGpsSpeed,
      speedDifferenceKmh = 0.0f,
      gpsAccuracyMeters = lastGpsAccuracyMeters,
      gyroMagnitude = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ),
      isValid = true
    )
    recordedSamples.add(firstSample)
    totalInertialSamples++

    _uiState.update { it.copy(testState = DynoRunState.MEDINDO_PROTEGIDO) }
  }

  fun finalizeRun(reason: FinishReason, vehicle: VehicleProfile? = null) {
    val currentState = _uiState.value.testState
    val isMeasuring = currentState == DynoRunState.MEDINDO_PROTEGIDO ||
      currentState == DynoRunState.MEDINDO ||
      currentState == DynoRunState.SUSPEITA_DESACELERACAO ||
      currentState == DynoRunState.FINALIZANDO

    if (isMeasuring && !resultSaved) {
      val nowNs = System.nanoTime()
      runEndTimeNs = nowNs
      finalGpsKmh = gpsSpeedKmh
      finalCalcSpeedKmh = integratedSpeedKmh
      _uiState.update { it.copy(testState = DynoRunState.FINALIZADO) }

      val elapsedSec = if (runStartTimeNs > 0L) ((nowNs - runStartTimeNs) / 1_000_000L) / 1000f else 0f
      diagnosticLogs.add("Finalizando teste: motivo=${reason.displayName}, tempo=${elapsedSec}s, maxGps=$maxGpsSpeedKmh km/h, maxCalc=$maxIntegratedSpeedKmh km/h")

      // 10. DIFERENÇA NO PICO: abs(maxGpsSpeedKmh - maxIntegratedSpeedKmh)
      val peakDiff = abs(maxGpsSpeedKmh - maxIntegratedSpeedKmh)
      val avgDiff = if (syncDiffCount > 0) (syncDiffSum / syncDiffCount).toFloat() else 0f

      val finalSamples = recordedSamples.take(500).toList()
      val validCount = finalSamples.count { it.isValid }
      val rejectedCount = finalSamples.count { !it.isValid }
      val rejectionRatio = if (totalInertialSamples > 0) rejectedInertialSamples.toFloat() / totalInertialSamples.toFloat() else 0f
      val speedGainKmh = maxGpsSpeedKmh - startGpsKmh

      var invalidReasonText: String? = null

      val isCompletePass = elapsedSec >= (DynoConfig.MIN_VALID_DURATION_MS / 1000f) &&
        validCount >= DynoConfig.MIN_VALID_SAMPLES &&
        validGpsUpdatesDuringRunCount >= DynoConfig.MIN_GPS_UPDATES &&
        speedGainKmh >= DynoConfig.MIN_SPEED_GAIN_KMH

      // 13. CLASSIFICAÇÃO DA PASSAGEM
      val runQualityStr = when {
        reason == FinishReason.CANCELLED || (reason == FinishReason.USER_STOP && !isCompletePass) -> {
          invalidReasonText = "Teste encerrado manualmente antes de atingir os critérios mínimos de validação."
          "INVÁLIDA"
        }
        reason == FinishReason.TIMEOUT -> {
          invalidReasonText = "Tempo de passagem excessivo (> 25s)."
          "INVÁLIDA"
        }
        elapsedSec < (DynoConfig.MIN_VALID_DURATION_MS / 1000f) -> {
          invalidReasonText = "Duração insuficiente (${String.format(Locale.US, "%.2f", elapsedSec)}s < 4.00s) para registrar curva de potência completa."
          "INVÁLIDA"
        }
        speedGainKmh < DynoConfig.MIN_SPEED_GAIN_KMH -> {
          invalidReasonText = "Ganho de velocidade GPS insuficiente (${String.format(Locale.US, "%.1f", speedGainKmh)} km/h < 10.0 km/h) após o gatilho."
          "INVÁLIDA"
        }
        validCount < DynoConfig.MIN_VALID_SAMPLES -> {
          invalidReasonText = "Quantidade insuficiente de amostras válidas ($validCount < ${DynoConfig.MIN_VALID_SAMPLES})."
          "INVÁLIDA"
        }
        validGpsUpdatesDuringRunCount < DynoConfig.MIN_GPS_UPDATES -> {
          invalidReasonText = "Poucas leituras de GPS válidas ($validGpsUpdatesDuringRunCount < ${DynoConfig.MIN_GPS_UPDATES}) durante a medição."
          "INVÁLIDA"
        }
        lastGpsAccuracyMeters > 12f -> {
          invalidReasonText = "Precisão do GPS insuficiente (${String.format(Locale.US, "%.1f", lastGpsAccuracyMeters)}m > 12m)."
          "INVÁLIDA"
        }
        rejectionRatio > 0.25f -> {
          invalidReasonText = "Mais de 25% das amostras rejeitadas por vibração excessiva."
          "INVÁLIDA"
        }
        peakDiff > 16.0f -> {
          invalidReasonText = "Divergência entre velocidade máxima GPS (${String.format(Locale.US, "%.1f", maxGpsSpeedKmh)} km/h) e calculada (${String.format(Locale.US, "%.1f", maxIntegratedSpeedKmh)} km/h)."
          "INVÁLIDA"
        }
        // Se houver menos de 4 pares sincronizados: qualidade DADOS INSUFICIENTES ou REGULAR (não acusa média elevada artificial)
        syncDiffCount < 4 -> {
          invalidReasonText = "Poucos pares sincronizados GPS × Acelerômetro ($syncDiffCount < 4) para cálculo estatístico de precisão."
          "REGULAR"
        }
        avgDiff > 12.0f -> {
          invalidReasonText = "Diferença média sincronizada entre GPS e acelerômetro elevada (±${String.format(Locale.US, "%.1f", avgDiff)} km/h)."
          "INVÁLIDA"
        }
        // Passagem BOA
        avgDiff <= 6.0f && maxSyncDiff <= 12.0f && peakDiff <= 10.0f &&
          validGpsUpdatesDuringRunCount >= DynoConfig.MIN_GPS_UPDATES &&
          lastGpsAccuracyMeters <= 6f && elapsedSec in 4f..20f && rejectionRatio <= 0.10f &&
          speedGainKmh >= DynoConfig.MIN_SPEED_GAIN_KMH -> "BOA"

        // Passagem REGULAR
        avgDiff <= 12.0f && maxSyncDiff <= 20.0f && peakDiff <= 16.0f &&
          validGpsUpdatesDuringRunCount >= DynoConfig.MIN_GPS_UPDATES &&
          lastGpsAccuracyMeters <= 10f && elapsedSec <= 25f && rejectionRatio <= 0.25f -> "REGULAR"

        else -> {
          invalidReasonText = "Inconsistência na detecção inercial ou divergência de velocidade."
          "INVÁLIDA"
        }
      }

      val avgHz = if (elapsedSec > 0f) finalSamples.size / elapsedSec else 0f
      val gpsFreqHz = if (elapsedSec > 0f) validGpsUpdatesDuringRunCount / elapsedSec else 0f

      var wheelPowerCv = 0f
      var enginePowerCv = 0f
      var wheelTorqueKgfm = 0f
      var engineTorqueKgfm = 0f
      var peakLongG = peakLongitudinalG
      var avgLongG = 0f
      var peakPowerRpm: Int? = null
      var peakTorqueRpm: Int? = null
      var peakPowerSpeedKmh = maxGpsSpeedKmh
      var peakTorqueSpeedKmh = startGpsKmh + (maxGpsSpeedKmh - startGpsKmh) * 0.45f
      var totalMassKg = 0f
      var drivetrainLossPercent = 15f
      var marginPercent = 10f

      var computedSamples = finalSamples

      if (vehicle != null) {
        totalMassKg = vehicle.totalWeightKg
        val frontalArea = vehicle.frontalAreaM2
        val cd = vehicle.dragCoefficient
        val cr = vehicle.rollingResistanceCoeff
        val drivetrain = vehicle.drivetrain
        val efficiency = VehicleCalculations.getDrivetrainEfficiency(drivetrain)
        drivetrainLossPercent = VehicleCalculations.getDrivetrainLossPercent(drivetrain)

        val confidence = VehicleCalculations.evaluateWeightConfidence(
          useMeasuredWeight = vehicle.useMeasuredWeight,
          audioPreset = vehicle.audioPreset,
          hasGnv = vehicle.gnvWeightKg > 0f,
          hasCargo = vehicle.cargoWeightKg > 0f
        )
        marginPercent = when (confidence) {
          WeightConfidence.HIGH -> 7.0f
          WeightConfidence.GOOD -> 10.0f
          WeightConfidence.ESTIMATED -> 14.0f
        }

        val tireCalc = VehicleCalculations.calculateTireDimensions(
          widthMm = vehicle.tireWidthMm,
          aspectRatio = vehicle.tireAspectRatio,
          rimInches = vehicle.wheelDiameterInches
        )

        // Relação estimada padrão de 2ª marcha
        val gearRatio = 1.95f
        val finalDrive = 4.10f
        val rollForce = VehicleCalculations.calculateRollingResistanceForce(totalMassKg, cr)

        // Processar cada amostra para gerar curvas completas
        computedSamples = finalSamples.map { sample ->
          val aMps2 = sample.filteredAccelerationZ
          val g = aMps2 / 9.80665f
          val vMps = (sample.gpsSpeedKmh / 3.6f).coerceAtLeast(0f)
          val fAero = VehicleCalculations.calculateAerodynamicForce(vMps, cd, frontalArea)
          val fAccel = VehicleCalculations.calculateAccelerationForce(totalMassKg, aMps2)
          val fTractive = VehicleCalculations.calculateTractiveForce(fAccel, rollForce, fAero)
          val wWatts = VehicleCalculations.calculateWheelPowerWatts(fTractive, vMps)
          val sampleWheelPowerCv = VehicleCalculations.convertWattsToCv(wWatts)
          val sampleEnginePowerCv = VehicleCalculations.calculateEnginePowerCv(sampleWheelPowerCv, drivetrain)

          val sampleRpm = VehicleCalculations.calculateRpmFromSpeed(vMps, tireCalc.circumferenceM, gearRatio, finalDrive)?.toInt()
          val sampleEngineTorqueKgfm = if (sampleRpm != null && sampleRpm > 800) {
            VehicleCalculations.calculateTorqueKgfm(sampleEnginePowerCv, sampleRpm.toFloat()) ?: 0f
          } else 0f
          val sampleWheelTorqueKgfm = sampleEngineTorqueKgfm * efficiency

          sample.copy(
            longitudinalG = g,
            wheelPowerCv = sampleWheelPowerCv,
            enginePowerCv = sampleEnginePowerCv,
            wheelTorqueKgfm = sampleWheelTorqueKgfm,
            engineTorqueKgfm = sampleEngineTorqueKgfm,
            engineRpm = sampleRpm
          )
        }

        val validComputed = computedSamples.filter { it.isValid && it.filteredAccelerationZ > 0.1f }
        if (validComputed.isNotEmpty()) {
          peakLongG = maxOf(peakLongG, validComputed.map { it.longitudinalG }.maxOrNull() ?: 0f)
          avgLongG = validComputed.map { it.longitudinalG }.average().toFloat()

          // Amostra com maior potência do motor
          val maxPowerSample = validComputed.maxByOrNull { it.enginePowerCv }
          if (maxPowerSample != null) {
            wheelPowerCv = maxPowerSample.wheelPowerCv
            enginePowerCv = maxPowerSample.enginePowerCv
            peakPowerRpm = maxPowerSample.engineRpm
            peakPowerSpeedKmh = maxPowerSample.gpsSpeedKmh
          }

          // Amostra com maior torque do motor
          val maxTorqueSample = validComputed.filter { (it.engineRpm ?: 0) in 1500..6500 }.maxByOrNull { it.engineTorqueKgfm }
            ?: validComputed.maxByOrNull { it.engineTorqueKgfm }
          if (maxTorqueSample != null) {
            wheelTorqueKgfm = maxTorqueSample.wheelTorqueKgfm
            engineTorqueKgfm = maxTorqueSample.engineTorqueKgfm
            peakTorqueRpm = maxTorqueSample.engineRpm
            peakTorqueSpeedKmh = maxTorqueSample.gpsSpeedKmh
          }
        }
      }

      if (vehicle != null) {
        val result = RunResult(
          vehicleId = vehicle.id,
          vehicleName = "${vehicle.manufacturer} ${vehicle.model} ${vehicle.engine}".trim(),
          runStartCalculatedSpeedKmh = startCalculatedKmh,
          runStartGpsSpeedKmh = startGpsKmh,
          maximumGpsSpeedKmh = maxGpsSpeedKmh,
          maximumCalculatedSpeedKmh = maxIntegratedSpeedKmh,
          finalGpsSpeedKmh = finalGpsKmh,
          finalCalculatedSpeedKmh = finalCalcSpeedKmh,
          speedGainKmh = speedGainKmh,
          estimatedPowerCv = enginePowerCv,
          estimatedTorqueKgfm = engineTorqueKgfm,
          wheelPowerCv = wheelPowerCv,
          enginePowerCv = enginePowerCv,
          wheelTorqueKgfm = wheelTorqueKgfm,
          engineTorqueKgfm = engineTorqueKgfm,
          peakLongitudinalG = peakLongG,
          averageLongitudinalG = avgLongG,
          peakPowerRpm = peakPowerRpm,
          peakTorqueRpm = peakTorqueRpm,
          peakPowerSpeedKmh = peakPowerSpeedKmh,
          peakTorqueSpeedKmh = peakTorqueSpeedKmh,
          totalVehicleMassKg = totalMassKg,
          drivetrainLossPercent = drivetrainLossPercent,
          estimatedMarginPercent = marginPercent,
          gearUsed = "2ª",
          isAerodynamicsEstimated = true,
          elapsedSeconds = elapsedSec,
          gpsAccuracyMeters = lastGpsAccuracyMeters,
          totalSamples = computedSamples.size,
          rejectedSamples = rejectedCount,
          validSamplesCount = validCount,
          validGpsLocationsCount = validGpsUpdatesDuringRunCount,
          averageSamplingRateHz = avgHz,
          averageGpsFrequencyHz = gpsFreqHz,
          quality = runQualityStr,
          finishReason = reason.code,
          averageSpeedDifferenceKmh = avgDiff,
          maximumSpeedDifferenceKmh = maxSyncDiff,
          invalidationReason = invalidReasonText,
          appVersion = "0.20.0",
          samples = computedSamples
        )
        runResultRepository.saveResult(result)
        resultSaved = true
      }

      onRunCompletedCallback?.invoke(resultSaved)
    }
  }

  private fun resetRunData() {
    zMedianBuffer.clear()
    zFiltradoRun = 0f
    peakLongitudinalG = 0f
    liveLongitudinalG = 0f
    suspectStartTimeNs = null
    suspectNegativeSampleCount = 0
    clutchStartTimeNs = null
    runStartTimeNs = 0L
    runEndTimeNs = 0L
    lastSensorTimestampNs = 0L
    lastSampleRecordedNs = 0L

    armedEstimatedSpeedMs = 0f
    armedLastNanoTime = 0L

    startCalculatedKmh = _uiState.value.startSpeedTriggerKmh
    startGpsKmh = 0f
    maxGpsSpeedKmh = 0f
    maxIntegratedSpeedKmh = 0f
    maxDisplaySpeedKmh = 0f
    finalGpsKmh = 0f
    finalCalcSpeedKmh = 0f
    integratedSpeedKmh = 0f

    totalInertialSamples = 0
    rejectedInertialSamples = 0

    validGpsUpdatesDuringRunCount = 0
    gpsFixHistory.clear()
    diagnosticLogs.clear()
    inertialHistory.clear()
    recordedSamples.clear()

    syncDiffSum = 0.0
    syncDiffCount = 0
    maxSyncDiff = 0f
  }

  override fun onCleared() {
    super.onCleared()
    stopLocationUpdates()
    stopSensorUpdates()
  }
}
