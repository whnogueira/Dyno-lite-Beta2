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
import com.example.model.UniqueGpsFix
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
import kotlin.math.min
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

/**
 * Constantes de calibração e limiares de validação.
 */
object DynoConfig {
  const val ACCEL_DEAD_ZONE = 0.15f
  const val START_PROTECTION_MS = 2500L
  const val DECEL_SUSPECT_THRESHOLD = -0.35f
  const val DECEL_SUSTAIN_MS = 800L
  const val DECEL_RECOVERY_THRESHOLD = -0.05f
  const val GPS_MIN_DROP_KMH = 3.0f
  const val GPS_STRONG_DROP_KMH = 5.0f
  const val MIN_VALID_DURATION_MS = 2500L
  const val MIN_VALID_SAMPLES = 40
  const val MIN_GPS_UPDATES = 3
  const val MIN_SPEED_GAIN_KMH = 8.0f
  const val GPS_EXP_FILTER_ALPHA = 0.25f
  const val FUSION_GPS_WEIGHT = 0.80f
  const val FUSION_SENSOR_WEIGHT = 0.20f
}

/**
 * Registro de leitura de fix GPS para histórico de sincronização e cálculo de aceleração.
 */
data class GpsFixRecord(
  val timestampMs: Long,
  val speedKmh: Float,
  val speedMps: Float,
  val accuracyM: Float,
  val speedAccuracyMps: Float,
  val elapsedRealtimeNs: Long,
  val runElapsedSec: Float,
  val latitude: Double = 0.0,
  val longitude: Double = 0.0
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
 * Estado de movimento do veículo baseado em média móvel de 5 amostras e histerese (Requisito 1).
 */
enum class VehicleMotionState {
  STOPPED,
  UNCERTAIN,
  MOVING
}

/**
 * Estado imutável completo exposto para a interface Compose via StateFlow.
 */
data class DynoUiState(
  val gpsSpeedKmh: Float = 0f,
  val avgGpsSpeedKmh: Float = 0f,
  val integratedSpeedKmh: Float = 0f,
  val displaySpeedKmh: Float = 0f,
  val longitudinalG: Float = 0f,
  val peakLongitudinalG: Float = 0f,
  val livePowerCv: Float = 0f,
  val liveEnginePowerCv: Float = 0f,
  val liveTorqueKgfm: Float = 0f,
  val liveTorqueNm: Float = 0f,
  val liveRpm: Int? = null,
  val liveDistanceMeters: Float = 0f,
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
  val vehicleMotionState: VehicleMotionState = VehicleMotionState.STOPPED,
  val isStoppedForTwoSeconds: Boolean = false,
  val isPhoneStable: Boolean = true,
  val blockingReason: String = "Aguardando GPS",
  val isReadyToArm: Boolean = false,
  val hasPhoneMovedAfterCalib: Boolean = false,
  val runElapsedSeconds: Float = 0f,
  val startSpeedTriggerKmh: Float = 40.0f,
  val selectedGear: String = "2ª",
  val selectedGearRatio: Float = 1.95f,
  val selectedFinalDrive: Float = 4.10f,
  val slopeMode: String = "IGNORE",
  val manualSlopePercent: Float = 0.0f,
  val calibrationStatusText: String = "Não calibrado",
  val validationIssues: List<String> = emptyList()
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
      selectedGear = prefs.getString("selected_gear_name", "2ª") ?: "2ª",
      selectedGearRatio = prefs.getFloat("selected_gear_ratio", 1.95f),
      selectedFinalDrive = prefs.getFloat("selected_final_drive", 4.10f),
      slopeMode = prefs.getString("slope_mode", "IGNORE") ?: "IGNORE",
      manualSlopePercent = prefs.getFloat("manual_slope_percent", 0.0f),
      calibrationStatusText = if (prefs.getBoolean("is_calibrated", false)) "Calibração concluída" else "Não calibrado"
    )
  )
  val uiState: StateFlow<DynoUiState> = _uiState.asStateFlow()

  // Calibração de offsets dos sensores
  private var offsetX = prefs.getFloat("offset_x", 0.0f)
  private var offsetY = prefs.getFloat("offset_y", 0.0f)
  private var offsetZ = prefs.getFloat("offset_z", 0.0f)
  private var calibratedNormalVibration = prefs.getFloat("calibrated_vibration", 0.12f)
  private var calibratedGyroDeviation = prefs.getFloat("calibrated_gyro", 0.08f)
  private var invertSignal = prefs.getBoolean("invert_longitudinal_signal", false)

  // Leituras brutas dos sensores
  private var linearX = 0f
  private var linearY = 0f
  private var linearZ = 0f
  private var gyroX = 0f
  private var gyroY = 0f
  private var gyroZ = 0f

  // Filtragem longitudinal do acelerômetro
  private val zMedianBuffer = mutableListOf<Float>()
  private var zFiltradoRun = 0f

  // 1. VELOCIDADES E RASTREAMENTO GPS (FONTE PRINCIPAL)
  @Volatile private var gpsSpeedKmh: Float = 0f
  @Volatile private var gpsSpeedMs: Float = 0f
  @Volatile private var integratedSpeedKmh: Float = 0f
  @Volatile private var displaySpeedKmh: Float = 0f
  @Volatile private var lastGpsAnchorSpeedMps: Float = 0f
  @Volatile private var lastGpsAnchorNanoTime: Long = 0L
  @Volatile private var integralSpeedSinceLastGpsMps: Float = 0f

  // Buffer de Média Móvel de Velocidade GPS (últimas 5 amostras) e Histerese (Requisito 1)
  private val gpsSpeedMovingAverageBuffer = mutableListOf<Float>()
  private var vehicleMotionState: VehicleMotionState = VehicleMotionState.STOPPED
  private var consecutiveHighSpeedCount: Int = 0
  private var stoppedStartTimeMs: Long = SystemClock.elapsedRealtime()

  // Estabilidade do Aparelho por Média Móvel de ~1s e Sustentação (Requisito 4)
  private data class GyroSample(val timestampNs: Long, val magnitude: Float)
  private val gyroWindow = mutableListOf<GyroSample>()
  private var gyroUnstableStartTimeNs: Long = 0L
  @Volatile private var isPhoneStable: Boolean = true

  private var previousGpsSpeedMs: Float = 0f
  private var previousGpsElapsedNs: Long = 0L
  private var filteredGpsAccelerationMps2: Float = 0f

  // GPS Tracking & Validação
  @Volatile private var lastProcessedLocationElapsedRealtimeNanos: Long = 0L
  @Volatile private var locationCallbackCount: Int = 0
  @Volatile private var uniqueGpsFixCount: Int = 0
  @Volatile private var gpsSpeedChangeCount: Int = 0
  @Volatile private var sensorSampleCount: Int = 0
  @Volatile private var lastUniqueGpsSpeedKmh: Float = -1f
  @Volatile private var lastUniqueGpsElapsedRealtimeNs: Long = 0L
  @Volatile private var maxGpsIntervalMs: Long = 0L
  @Volatile private var maxGpsAgeMs: Long = 0L
  @Volatile private var isGpsFrozenDetected: Boolean = false
  @Volatile private var gpsFrozenStartTimeNs: Long? = null
  @Volatile private var gpsFrozenSpeedKmh: Float = 0f
  @Volatile private var gpsFrozenIntegratedStartKmh: Float = 0f
  @Volatile private var lastProcessedLocationForSampleNs: Long = 0L
  private val uniqueGpsFixes = mutableListOf<UniqueGpsFix>()
  private var officialStartSpeedKmh: Float = 0f
  private var officialMaxSpeedKmh: Float = 0f

  @Volatile private var lastProcessedGpsElapsedRealtimeNs: Long = 0L
  @Volatile private var lastProcessedGpsTimestamp: Long = 0L
  @Volatile private var lastGpsArrivalWallTimeMs: Long = 0L
  @Volatile private var lastGpsIntervalMs: Long = 0L
  @Volatile private var locationUpdateCount: Int = 0
  @Volatile private var validGpsUpdatesDuringRunCount: Int = 0
  @Volatile private var lastGpsAccuracyMeters: Float = 99f
  @Volatile private var lastGpsSpeedAccuracyMps: Float = 99f
  @Volatile private var lastLatitude: Double = 0.0
  @Volatile private var lastLongitude: Double = 0.0

  // Run Tracking & Splits
  private var runStartTimeNs: Long = 0L
  private var runEndTimeNs: Long = 0L
  private var lastSensorTimestampNs: Long = 0L
  private var lastSampleRecordedNs: Long = 0L
  private var armedEstimatedSpeedMs: Float = 0f
  private var armedLastNanoTime: Long = 0L

  private var positiveAccelDurationMs: Long = 0L
  private var lastAccelCheckNs: Long = 0L

  private var startCalculatedKmh: Float = 40.0f
  private var startGpsKmh: Float = 0f
  private var maxGpsSpeedKmh: Float = 0f
  private var maxIntegratedSpeedKmh: Float = 0f
  private var maxDisplaySpeedKmh: Float = 0f
  private var finalGpsKmh: Float = 0f
  private var finalCalcSpeedKmh: Float = 0f

  // Integração Trapezoidal da Distância
  private var totalRunDistanceMeters: Float = 0f
  private var lastDistanceIntegrationNs: Long = 0L
  private var lastDistanceSpeedMs: Float = 0f

  // Splits de Aceleração por Faixa de Velocidade e Distância
  private var splitTime0to60: Float? = null
  private var splitTime0to100: Float? = null
  private var splitCross60Ns: Long? = null
  private var splitCross80Ns: Long? = null
  private var splitCross100Ns: Long? = null
  private var splitTime60to100: Float? = null
  private var splitTime80to120: Float? = null
  private var splitTime100to200: Float? = null

  private var splitTime60Feet: Float? = null
  private var splitTime100M: Float? = null
  private var splitTime201M: Float? = null
  private var splitTime402M: Float? = null

  // Detecção de Queda Contínua de Velocidade e Desaceleração
  private var speedDropStartTimeNs: Long? = null
  private var lastMaxGpsSpeedCheckNs: Long = 0L
  private var invalidContinuousAnomalyStartTimeNs: Long? = null

  private var suspectStartTimeNs: Long? = null
  private var suspectNegativeSampleCount: Int = 0
  private var clutchStartTimeNs: Long? = null

  // Sincronização GPS x Acelerômetro por Interpolação de Timestamp
  private val inertialHistory = mutableListOf<InertialPoint>()
  private val recordedSamples = mutableListOf<RunSample>()
  private val gpsFixHistory = mutableListOf<GpsFixRecord>()
  private val diagnosticLogs = mutableListOf<String>()

  private var currentTestId: String? = null
  private var persistedSampleCount: Int = 0
  private var lastSampleFlushTimeMs: Long = 0L

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

  // Callbacks de Hardware
  private var isGpsLocationCallbackActive = false
  private var isSensorListenerActive = false
  private var onRunCompletedCallback: ((Boolean) -> Unit)? = null

  // Perfil do Veículo Atual em Uso
  private var activeVehicleProfile: VehicleProfile? = null

  init {
    screenStabilizedTimestampMs = SystemClock.elapsedRealtime()
    startDisplaySpeedUpdateLoop()
  }

  fun setActiveVehicle(vehicle: VehicleProfile?) {
    activeVehicleProfile = vehicle
    if (vehicle != null) {
      val availableGears = getAvailableGears(vehicle)
      val savedGearName = prefs.getString("selected_gear_name", null)
      val savedGearRatio = prefs.getFloat("selected_gear_ratio", 0f)

      val matchingGear = if (!savedGearName.isNullOrBlank() && savedGearRatio > 0f) {
        availableGears.firstOrNull { it.first == savedGearName } ?: Pair(savedGearName, savedGearRatio)
      } else {
        if (availableGears.size >= 2) availableGears[1] else availableGears.firstOrNull() ?: Pair("2ª", 2.14f)
      }

      val ratio = if (savedGearRatio > 0f) savedGearRatio else (vehicle.gearRatio ?: matchingGear.second)
      val gearName = savedGearName ?: matchingGear.first
      val finalDrive = vehicle.finalDriveRatio ?: _uiState.value.selectedFinalDrive
      _uiState.update {
        it.copy(
          selectedGear = gearName,
          selectedGearRatio = ratio,
          selectedFinalDrive = finalDrive,
          slopeMode = vehicle.slopeMode,
          manualSlopePercent = vehicle.manualSlopePercent
        )
      }
      validatePreConditions(vehicle)
    }
  }

  fun getAvailableGears(vehicle: VehicleProfile? = activeVehicleProfile): List<Pair<String, Float>> {
    val transId = vehicle?.transmissionId
    val transmission = com.example.data.VehicleDatabase.transmissions.firstOrNull { it.id == transId }
    val ratios = transmission?.gearRatios ?: listOf(3.73f, 2.14f, 1.41f, 1.12f, 0.89f)
    return ratios.mapIndexed { index, ratio ->
      Pair("${index + 1}ª", ratio)
    }
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

  fun setSelectedGear(gearName: String, gearRatio: Float) {
    if (_uiState.value.testState == DynoRunState.PARADO) {
      prefs.edit()
        .putString("selected_gear_name", gearName)
        .putFloat("selected_gear_ratio", gearRatio)
        .apply()
      _uiState.update {
        it.copy(selectedGear = gearName, selectedGearRatio = gearRatio)
      }
    }
  }

  fun setSlopeConfiguration(mode: String, percent: Float) {
    prefs.edit()
      .putString("slope_mode", mode)
      .putFloat("manual_slope_percent", percent)
      .apply()
    _uiState.update {
      it.copy(slopeMode = mode, manualSlopePercent = percent)
    }
  }

  /**
   * Validação de pré-requisitos antes de liberar o botão Iniciar (Seção 20 da especificação).
   */
  fun validatePreConditions(vehicle: VehicleProfile?): List<String> {
    val issues = mutableListOf<String>()
    if (vehicle == null) {
      issues.add("Nenhum veículo selecionado.")
      _uiState.update { it.copy(validationIssues = issues) }
      return issues
    }

    if (vehicle.totalWeightKg < 300f) {
      issues.add("Massa total (${vehicle.totalWeightKg.toInt()} kg) é inferior ao mínimo de 300 kg.")
    }
    val gearRatio = _uiState.value.selectedGearRatio
    if (gearRatio <= 0f) {
      issues.add("Relação de marcha inválida (deve ser > 0).")
    }
    val finalDrive = _uiState.value.selectedFinalDrive
    if (finalDrive <= 0f) {
      issues.add("Relação do diferencial inválida (deve ser > 0).")
    }
    if (vehicle.tireWidthMm < 100 || vehicle.tireAspectRatio < 20 || vehicle.wheelDiameterInches < 10) {
      issues.add("Dimensões do pneu inválidas.")
    }
    if (!_uiState.value.isGpsProviderEnabled || !_uiState.value.hasGpsFix) {
      issues.add("Aguardando sinal e fix do GPS.")
    } else if (lastGpsAccuracyMeters > 25.0f) {
      issues.add("Precisão horizontal do GPS insuficiente (${String.format(Locale.US, "%.1f", lastGpsAccuracyMeters)} m > 25 m).")
    }
    if (!_uiState.value.isCalibrated) {
      issues.add("Aparelho não calibrado na posição do suporte.")
    }

    _uiState.update { it.copy(validationIssues = issues) }
    return issues
  }

  /**
   * LOOP DE ATUALIZAÇÃO DO VELOCÍMETRO E TELEMETRIA EM TEMPO REAL (~16.6 Hz / ~60ms)
   * Baseado diretamente na velocidade GPS em tempo real, detecção com média móvel e histerese.
   */
  private fun startDisplaySpeedUpdateLoop() {
    viewModelScope.launch {
      while (isActive) {
        delay(60L)

        val nowNs = SystemClock.elapsedRealtimeNanos()
        val nowMs = SystemClock.elapsedRealtime()
        val currentState = _uiState.value.testState

        val gpsAge = if (lastProcessedGpsElapsedRealtimeNs > 0L) {
          ((nowNs - lastProcessedGpsElapsedRealtimeNs) / 1_000_000L).coerceAtLeast(0L)
        } else 9999L

        val avgMovingSpeed = if (gpsSpeedMovingAverageBuffer.isNotEmpty()) {
          gpsSpeedMovingAverageBuffer.average().toFloat()
        } else gpsSpeedKmh

        // 1. Detecção de Veículo Parado (Requisito 1: média <= 3 km/h e 2s contínuos)
        val isStoppedForTwoSeconds = (vehicleMotionState == VehicleMotionState.STOPPED) && (nowMs - stoppedStartTimeMs >= 2000L)

        // 2. Status do GPS (Requisito 2: até 5000 ms e precisão <= 25m é válido para armar)
        val isGpsReady = locationUpdateCount > 0 && gpsAge <= 5000L && lastGpsAccuracyMeters <= 25.0f

        // A velocidade exibida prioriza a velocidade GPS oficial do Android
        val targetSpeed: Float = when {
          isStoppedForTwoSeconds -> 0f
          currentState == DynoRunState.MEDINDO_PROTEGIDO || currentState == DynoRunState.MEDINDO || currentState == DynoRunState.SUSPEITA_DESACELERACAO -> {
            if (gpsAge < 1000L) {
              gpsSpeedKmh * 0.85f + integratedSpeedKmh * 0.15f
            } else {
              integratedSpeedKmh
            }
          }
          currentState == DynoRunState.AGUARDANDO_INICIO -> {
            val predictedMps = (lastGpsAnchorSpeedMps + integralSpeedSinceLastGpsMps).coerceAtLeast(0f)
            val predictedKmh = predictedMps * 3.6f
            if (gpsAge < 1000L) {
              gpsSpeedKmh * 0.80f + predictedKmh * 0.20f
            } else {
              predictedKmh
            }
          }
          else -> {
            if (avgMovingSpeed <= 3.0f && isStoppedForTwoSeconds) 0f else gpsSpeedKmh
          }
        }

        // Suavização visual moderada
        val alpha = if (isStoppedForTwoSeconds) 0.45f else 0.32f
        val newDisplaySpeed = (displaySpeedKmh + alpha * (targetSpeed - displaySpeedKmh)).coerceAtLeast(0f)
        val finalDisplaySpeed = if (isStoppedForTwoSeconds && newDisplaySpeed < 0.5f) 0f else newDisplaySpeed
        displaySpeedKmh = finalDisplaySpeed

        if (finalDisplaySpeed > maxDisplaySpeedKmh) {
          maxDisplaySpeedKmh = finalDisplaySpeed
        }

        val gpsFreqHz = if (lastGpsIntervalMs > 0L) (1000f / lastGpsIntervalMs).coerceIn(0.1f, 20f) else 0f
        val avgDiff = if (syncDiffCount > 0) (syncDiffSum / syncDiffCount).toFloat() else 0f

        // 3. Liberação do Botão Iniciar e Bloqueio atual (Requisitos 2 e 3)
        val blockingReason = when {
          !_uiState.value.isCalibrated -> "Aguardando calibração"
          locationUpdateCount == 0 || gpsAge > 5000L -> "Aguardando GPS"
          lastGpsAccuracyMeters > 25.0f -> "Precisão GPS insuficiente"
          vehicleMotionState != VehicleMotionState.STOPPED || !isStoppedForTwoSeconds -> "Aguardando veículo parar"
          !isPhoneStable -> "Celular se movimentando"
          else -> "Pronto para iniciar"
        }

        val isReadyToArm = _uiState.value.isCalibrated &&
          isGpsReady &&
          vehicleMotionState == VehicleMotionState.STOPPED &&
          isStoppedForTwoSeconds &&
          isPhoneStable &&
          currentState == DynoRunState.PARADO

        // Cálculos de Potência e RPM em tempo real para o display
        var liveWheelPower = 0f
        var liveEnginePower = 0f
        var liveTorqueKg = 0f
        var liveTorqueNmVal = 0f
        var liveRpmVal: Int? = null

        val vehicle = activeVehicleProfile
        if (vehicle != null && (currentState == DynoRunState.MEDINDO_PROTEGIDO || currentState == DynoRunState.MEDINDO || currentState == DynoRunState.SUSPEITA_DESACELERACAO)) {
          val vMs = (displaySpeedKmh / 3.6f).coerceAtLeast(0f)
          val totalMass = vehicle.totalWeightKg
          val cr = vehicle.rollingResistanceCoeff
          val cd = vehicle.dragCoefficient
          val area = vehicle.frontalAreaM2
          val density = vehicle.airDensityKgM3
          val slopePercent = if (_uiState.value.slopeMode == "MANUAL") _uiState.value.manualSlopePercent else 0f

          val fAero = VehicleCalculations.calculateAerodynamicForce(vMs, cd, area, density)
          val fRoll = VehicleCalculations.calculateRollingResistanceForce(totalMass, cr)
          val fSlope = VehicleCalculations.calculateSlopeForce(totalMass, slopePercent)
          val fAccel = VehicleCalculations.calculateAccelerationForce(totalMass, max(0f, zFiltradoRun))

          val fTotal = VehicleCalculations.calculateTotalTractiveForce(fAccel, fRoll, fAero, fSlope)
          val pWatts = VehicleCalculations.calculateWheelPowerWatts(fTotal, vMs)
          liveWheelPower = VehicleCalculations.convertWattsToCv(pWatts)
          liveEnginePower = VehicleCalculations.calculateEnginePowerCv(liveWheelPower, vehicle.drivetrain, vehicle.customDrivetrainLossPercent)

          val tireCalc = VehicleCalculations.calculateTireDimensions(vehicle.tireWidthMm, vehicle.tireAspectRatio, vehicle.wheelDiameterInches, vehicle.tireCorrectionPercent)
          liveRpmVal = VehicleCalculations.calculateRpmFromSpeed(vMs, tireCalc.circumferenceM, _uiState.value.selectedGearRatio, _uiState.value.selectedFinalDrive)?.toInt()

          if (liveRpmVal != null && liveRpmVal > 500) {
            val tEngine = VehicleCalculations.calculateTorqueKgfm(liveEnginePower, liveRpmVal.toFloat()) ?: 0f
            liveTorqueKg = tEngine
            liveTorqueNmVal = VehicleCalculations.calculateTorqueNm(tEngine)
          }
        }

        _uiState.update { current ->
          current.copy(
            gpsSpeedKmh = gpsSpeedKmh,
            avgGpsSpeedKmh = avgMovingSpeed,
            integratedSpeedKmh = integratedSpeedKmh,
            displaySpeedKmh = finalDisplaySpeed,
            longitudinalG = liveLongitudinalG,
            peakLongitudinalG = peakLongitudinalG,
            livePowerCv = liveWheelPower,
            liveEnginePowerCv = liveEnginePower,
            liveTorqueKgfm = liveTorqueKg,
            liveTorqueNm = liveTorqueNmVal,
            liveRpm = liveRpmVal,
            liveDistanceMeters = totalRunDistanceMeters,
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
            vehicleMotionState = vehicleMotionState,
            isStoppedForTwoSeconds = isStoppedForTwoSeconds,
            isPhoneStable = isPhoneStable,
            blockingReason = blockingReason,
            isReadyToArm = isReadyToArm,
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
      locationCallbackCount++
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

    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100L)
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
   * 3. PROCESSAMENTO DE LOCALIZAÇÃO GPS REAL (FONTE PRIMÁRIA DE VELOCIDADE)
   */
  private fun processNewLocation(location: Location) {
    val elapsedRealtimeNs = location.elapsedRealtimeNanos
    val locationTime = location.time

    // 1. NÃO CONTAR GPS REPETIDO COMO FIX NOVO
    // Cada Location deve possuir identidade própria baseada em location.elapsedRealtimeNanos
    if (elapsedRealtimeNs <= lastProcessedLocationElapsedRealtimeNanos && lastProcessedLocationElapsedRealtimeNanos != 0L) {
      return
    }

    val intervalSinceLastFixMs = if (lastUniqueGpsElapsedRealtimeNs > 0L) {
      (elapsedRealtimeNs - lastUniqueGpsElapsedRealtimeNs) / 1_000_000L
    } else 0L

    if (intervalSinceLastFixMs > maxGpsIntervalMs && lastUniqueGpsElapsedRealtimeNs > 0L) {
      maxGpsIntervalMs = intervalSinceLastFixMs
    }

    val gpsAgeMillis = ((SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNs) / 1_000_000L).coerceAtLeast(0L)
    if (gpsAgeMillis > maxGpsAgeMs) {
      maxGpsAgeMs = gpsAgeMillis
    }

    val nowWallMs = SystemClock.elapsedRealtime()
    val nowNano = System.nanoTime()

    if (lastGpsArrivalWallTimeMs > 0L) {
      lastGpsIntervalMs = nowWallMs - lastGpsArrivalWallTimeMs
    }
    lastGpsArrivalWallTimeMs = nowWallMs

    val rawSpeedMps = if (location.hasSpeed()) location.speed else 0f
    val speedKmh = (rawSpeedMps * 3.6f).coerceAtLeast(0f)
    val horizontalAccuracy = if (location.hasAccuracy()) location.accuracy else 99f
    val speedAccuracyMps = if (location.hasSpeedAccuracy()) location.speedAccuracyMetersPerSecond else 99f

    val speedDiff = if (lastUniqueGpsSpeedKmh >= 0f) speedKmh - lastUniqueGpsSpeedKmh else 0f
    if (lastUniqueGpsSpeedKmh >= 0f && kotlin.math.abs(speedKmh - lastUniqueGpsSpeedKmh) > 0.05f) {
      gpsSpeedChangeCount++
    }

    lastUniqueGpsSpeedKmh = speedKmh
    lastUniqueGpsElapsedRealtimeNs = elapsedRealtimeNs
    lastProcessedLocationElapsedRealtimeNanos = elapsedRealtimeNs
    uniqueGpsFixCount++

    // Média móvel das últimas 5 amostras de velocidade GPS (Requisito 1)
    gpsSpeedMovingAverageBuffer.add(speedKmh)
    if (gpsSpeedMovingAverageBuffer.size > 5) {
      gpsSpeedMovingAverageBuffer.removeAt(0)
    }
    val avgMovingSpeedKmh = gpsSpeedMovingAverageBuffer.average().toFloat()

    // Histerese de estado de movimento:
    // - <= 3 km/h -> STOPPED
    // - > 5 km/h -> MOVING
    // - Entre 3 e 5 km/h -> mantém estado anterior
    if (avgMovingSpeedKmh <= 3.0f) {
      vehicleMotionState = VehicleMotionState.STOPPED
    } else if (avgMovingSpeedKmh > 5.0f) {
      vehicleMotionState = VehicleMotionState.MOVING
    }

    // Reiniciar o contador de parado de 2s somente se pelo menos 2 leituras consecutivas superarem 5 km/h ou MOVING
    if (speedKmh > 5.0f) {
      consecutiveHighSpeedCount++
    } else {
      consecutiveHighSpeedCount = 0
    }

    if (consecutiveHighSpeedCount >= 2 || vehicleMotionState == VehicleMotionState.MOVING) {
      stoppedStartTimeMs = nowWallMs
    }

    // 3. CÁLCULO DA ACELERAÇÃO GPS POR TIMESTAMP REAL: (v2 - v1) / dt
    if (previousGpsElapsedNs > 0L && previousGpsSpeedMs >= 0f) {
      val dtSeconds = (elapsedRealtimeNs - previousGpsElapsedNs) / 1_000_000_000.0
      if (dtSeconds in 0.05..1.5) {
        val rawGpsAccel = ((rawSpeedMps - previousGpsSpeedMs) / dtSeconds).toFloat()
        // Filtro exponencial: alpha * aAtual + (1 - alpha) * aAnterior
        filteredGpsAccelerationMps2 = DynoConfig.GPS_EXP_FILTER_ALPHA * rawGpsAccel +
          (1.0f - DynoConfig.GPS_EXP_FILTER_ALPHA) * filteredGpsAccelerationMps2
      }
    }
    previousGpsSpeedMs = rawSpeedMps
    previousGpsElapsedNs = elapsedRealtimeNs

    lastProcessedGpsElapsedRealtimeNs = elapsedRealtimeNs
    lastProcessedGpsTimestamp = locationTime
    locationUpdateCount++
    lastGpsAccuracyMeters = horizontalAccuracy
    lastGpsSpeedAccuracyMps = speedAccuracyMps
    lastLatitude = location.latitude
    lastLongitude = location.longitude

    gpsSpeedMs = rawSpeedMps
    gpsSpeedKmh = speedKmh

    val currentState = _uiState.value.testState

    // Ancorar a velocidade inercial auxiliar na velocidade GPS real apenas quando NÃO estiver medindo
    if (currentState != DynoRunState.MEDINDO_PROTEGIDO &&
      currentState != DynoRunState.MEDINDO &&
      currentState != DynoRunState.SUSPEITA_DESACELERACAO) {
      if (speedKmh > 0f && horizontalAccuracy <= 25.0f) {
        integratedSpeedKmh = speedKmh
      }
    }

    lastGpsAnchorSpeedMps = rawSpeedMps
    lastGpsAnchorNanoTime = nowNano
    integralSpeedSinceLastGpsMps = 0f

    // 15. LÓGICA DE INÍCIO DA PASSADA (Requisito 5)
    if (currentState == DynoRunState.AGUARDANDO_INICIO) {
      val targetTrigger = _uiState.value.startSpeedTriggerKmh
      val isCalib = _uiState.value.isCalibrated

      // Checa aceleração positiva (> 0.25 m/s² por GPS ou acelerômetro por pelo menos 300 ms)
      val isPositiveAccel = (filteredGpsAccelerationMps2 > 0.25f) || (zFiltradoRun > 0.25f)
      if (isPositiveAccel) {
        if (lastAccelCheckNs == 0L) {
          lastAccelCheckNs = nowNano
        } else {
          positiveAccelDurationMs = (nowNano - lastAccelCheckNs) / 1_000_000L
        }
      } else {
        lastAccelCheckNs = nowNano
        positiveAccelDurationMs = 0L
      }

      val isAccelConfirmed = (filteredGpsAccelerationMps2 > 0.25f) || (positiveAccelDurationMs >= 300L)

      if (speedKmh >= targetTrigger && horizontalAccuracy <= 25.0f && isCalib && isAccelConfirmed) {
        triggerOfficialRunStart(nowNano, speedKmh)
      }
    } else if (currentState == DynoRunState.MEDINDO_PROTEGIDO ||
      currentState == DynoRunState.MEDINDO ||
      currentState == DynoRunState.SUSPEITA_DESACELERACAO) {

      if (speedKmh > maxGpsSpeedKmh) {
        maxGpsSpeedKmh = speedKmh
      }
      if (speedKmh > officialMaxSpeedKmh) {
        officialMaxSpeedKmh = speedKmh
      }

      if (location.hasSpeed() && horizontalAccuracy <= 25.0f) {
        validGpsUpdatesDuringRunCount++
        val runElapsedSec = ((System.nanoTime() - runStartTimeNs) / 1_000_000L) / 1000f

        val fixRecord = GpsFixRecord(
          timestampMs = locationTime,
          speedKmh = speedKmh,
          speedMps = rawSpeedMps,
          accuracyM = horizontalAccuracy,
          speedAccuracyMps = speedAccuracyMps,
          elapsedRealtimeNs = elapsedRealtimeNs,
          runElapsedSec = runElapsedSec,
          latitude = location.latitude,
          longitude = location.longitude
        )
        gpsFixHistory.add(fixRecord)

        val uniqueFix = UniqueGpsFix(
          elapsedRealtimeNanos = elapsedRealtimeNs,
          timestamp = locationTime,
          speedKmh = speedKmh,
          speedAccuracyMetersPerSecond = speedAccuracyMps,
          accuracyMeters = horizontalAccuracy,
          ageMillis = gpsAgeMillis,
          provider = location.provider ?: "gps",
          hasSpeed = location.hasSpeed(),
          isMock = location.isFromMockProvider,
          speedDifferenceKmh = speedDiff,
          intervalSinceLastFixMs = intervalSinceLastFixMs
        )
        uniqueGpsFixes.add(uniqueFix)

        // 4. FUSÃO GPS E ACELERÔMETRO (0.80 GPS / 0.20 SENSOR)
        val finalFusionAccel = (DynoConfig.FUSION_GPS_WEIGHT * filteredGpsAccelerationMps2) +
          (DynoConfig.FUSION_SENSOR_WEIGHT * zFiltradoRun)

        // Interpolação e sincronização
        if (gpsAgeMillis <= 1500L) {
          interpolateAndCompareGpsWithInertial(elapsedRealtimeNs, speedKmh)
        }

        // 17. INTEGRAÇÃO TRAPEZOIDAL DA DISTÂNCIA
        if (lastDistanceIntegrationNs > 0L) {
          val dtSec = (nowNano - lastDistanceIntegrationNs) / 1_000_000_000.0f
          if (dtSec in 0.01f..1.5f) {
            val distInc = ((lastDistanceSpeedMs + rawSpeedMps) / 2.0f) * dtSec
            totalRunDistanceMeters += distInc
            checkSplits(nowNano, speedKmh, totalRunDistanceMeters)
          }
        }
        lastDistanceIntegrationNs = nowNano
        lastDistanceSpeedMs = rawSpeedMps

        // 16. DETECÇÃO DE FINAL DA PASSADA POR DESACELERAR MAIS DE 2s OU GPS PERDIDO
        val runDurationMs = ((System.nanoTime() - runStartTimeNs) / 1_000_000L)
        if (runDurationMs >= DynoConfig.START_PROTECTION_MS) {
          checkGpsDecelerationConditions(speedKmh, locationTime, nowNano)
        }
      }
    }
  }

  /**
   * Registro dos tempos intermediários (Splits de velocidade e distância).
   */
  private fun checkSplits(nowNs: Long, currentSpeedKmh: Float, totalDistM: Float) {
    if (runStartTimeNs == 0L) return
    val elapsedSec = ((nowNs - runStartTimeNs) / 1_000_000L) / 1000f

    // 0-60 km/h e 0-100 km/h (válidos apenas se partida <= 15 km/h)
    if (startGpsKmh <= 15.0f) {
      if (splitTime0to60 == null && currentSpeedKmh >= 60.0f) {
        splitTime0to60 = elapsedSec
      }
      if (splitTime0to100 == null && currentSpeedKmh >= 100.0f) {
        splitTime0to100 = elapsedSec
      }
    }

    if (splitCross60Ns == null && currentSpeedKmh >= 60.0f) {
      splitCross60Ns = nowNs
    }
    if (splitCross80Ns == null && currentSpeedKmh >= 80.0f) {
      splitCross80Ns = nowNs
    }
    if (splitCross100Ns == null && currentSpeedKmh >= 100.0f) {
      splitCross100Ns = nowNs
      if (splitCross60Ns != null && splitTime60to100 == null) {
        splitTime60to100 = ((nowNs - splitCross60Ns!!) / 1_000_000L) / 1000f
      }
    }
    if (splitTime80to120 == null && currentSpeedKmh >= 120.0f && splitCross80Ns != null) {
      splitTime80to120 = ((nowNs - splitCross80Ns!!) / 1_000_000L) / 1000f
    }
    if (splitTime100to200 == null && currentSpeedKmh >= 200.0f && splitCross100Ns != null) {
      splitTime100to200 = ((nowNs - splitCross100Ns!!) / 1_000_000L) / 1000f
    }

    // Splits de distância
    if (splitTime60Feet == null && totalDistM >= 18.288f && startGpsKmh <= 10.0f) {
      splitTime60Feet = elapsedSec
    }
    if (splitTime100M == null && totalDistM >= 100.0f) {
      splitTime100M = elapsedSec
    }
    if (splitTime201M == null && totalDistM >= 201.168f) {
      splitTime201M = elapsedSec
    }
    if (splitTime402M == null && totalDistM >= 402.336f) {
      splitTime402M = elapsedSec
    }
  }

  /**
   * Sincronização GPS x Acelerômetro por interpolação temporal.
   */
  private fun interpolateAndCompareGpsWithInertial(gpsElapsedRealtimeNs: Long, gpsSpeedKmh: Float) {
    if (inertialHistory.size < 2) return

    var sampleBefore: InertialPoint? = null
    var sampleAfter: InertialPoint? = null

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
      }
    }
  }

  /**
   * Checagem de desaceleração contínua por mais de 2 segundos.
   */
  private fun checkGpsDecelerationConditions(currentGpsKmh: Float, locationTimeMs: Long, nowNs: Long) {
    val dropFromMax = maxGpsSpeedKmh - currentGpsKmh

    if (dropFromMax >= DynoConfig.GPS_MIN_DROP_KMH) {
      if (speedDropStartTimeNs == null) {
        speedDropStartTimeNs = nowNs
      } else {
        val dropDurationMs = (nowNs - speedDropStartTimeNs!!) / 1_000_000L
        if (dropDurationMs >= 2000L) {
          _uiState.update { it.copy(testState = DynoRunState.FINALIZANDO) }
          diagnosticLogs.add("Desaceleração contínua confirmada por ${dropDurationMs}ms (queda de ${dropFromMax} km/h). Finalizando.")
          finalizeRun(FinishReason.GPS_DECELERATION, activeVehicleProfile)
        }
      }
    } else {
      speedDropStartTimeNs = null
    }
  }

  // 4. SENSORES DE APOIO (ACELERÔMETRO E GIROSCÓPIO)
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

            // Filtro mediano
            zMedianBuffer.add(rawCorrigidoZ)
            if (zMedianBuffer.size > 5) {
              zMedianBuffer.removeAt(0)
            }
            val sortedZ = zMedianBuffer.sorted()
            val medianaZ = if (sortedZ.isNotEmpty()) sortedZ[sortedZ.size / 2] else 0f

            // Filtro passa-baixas (~350ms)
            zFiltradoRun += 0.15f * (medianaZ - zFiltradoRun)

            val zDeadZone = if (abs(zFiltradoRun) <= DynoConfig.ACCEL_DEAD_ZONE) 0f else zFiltradoRun
            val currentG = zFiltradoRun / 9.80665f
            liveLongitudinalG = currentG
            if (currentG > peakLongitudinalG && (_uiState.value.testState == DynoRunState.MEDINDO_PROTEGIDO || _uiState.value.testState == DynoRunState.MEDINDO)) {
              peakLongitudinalG = currentG
            }

            val nowNs = System.nanoTime()
            val elapsedRealtimeNs = SystemClock.elapsedRealtimeNanos()

            // Detecção de movimentação física do aparelho
            val isScreenStable = (SystemClock.elapsedRealtime() - screenStabilizedTimestampMs) > 1500L
            if (_uiState.value.isCalibrated && !isCalibrating && _uiState.value.testState == DynoRunState.PARADO && gpsSpeedKmh < 3f && isScreenStable) {
              val gMag = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)
              if (gMag > 3.8f) {
                persistentMovementCount++
                if (persistentMovementCount >= 30) {
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
            } else if (currentState == DynoRunState.MEDINDO_PROTEGIDO ||
              currentState == DynoRunState.MEDINDO ||
              currentState == DynoRunState.SUSPEITA_DESACELERACAO) {

              val runDurationMs = (nowNs - runStartTimeNs) / 1_000_000L
              val elapsedSec = runDurationMs / 1000f

              // Estimativa inercial limitada a no máximo 1.0s caso o GPS atrase
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
              val maxNormalVib = max(4.0f, calibratedNormalVibration * 2.8f)
              val maxNormalGyro = max(3.0f, calibratedGyroDeviation * 3.2f)

              totalInertialSamples++
              val isSampleValid = !(corrX > maxNormalVib || corrY > maxNormalVib || gyroMag > maxNormalGyro)
              if (!isSampleValid) {
                rejectedInertialSamples++
              }

              // Detecção de GPS Congelado em Tempo Real
              if (runDurationMs >= 1000L) {
                if (gpsFrozenStartTimeNs == null) {
                  gpsFrozenStartTimeNs = nowNs
                  gpsFrozenSpeedKmh = gpsSpeedKmh
                  gpsFrozenIntegratedStartKmh = currentCalcKmh
                } else {
                  if (abs(gpsSpeedKmh - gpsFrozenSpeedKmh) <= 0.1f) {
                    val frozenDurationMs = (nowNs - gpsFrozenStartTimeNs!!) / 1_000_000L
                    val integratedGain = currentCalcKmh - gpsFrozenIntegratedStartKmh
                    val isSustainedAccel = zFiltradoRun > 0.20f || currentG > 0.02f
                    if (frozenDurationMs >= 1500L && isSustainedAccel && integratedGain >= 5.0f) {
                      isGpsFrozenDetected = true
                    }
                  } else {
                    gpsFrozenStartTimeNs = nowNs
                    gpsFrozenSpeedKmh = gpsSpeedKmh
                    gpsFrozenIntegratedStartKmh = currentCalcKmh
                  }
                }
              }

              // Salva ponto inercial de alta resolução
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

              // Gravação da série temporal de telemetria (~20 Hz)
              if (nowNs - lastSampleRecordedNs >= 50_000_000L && recordedSamples.size < 600) {
                lastSampleRecordedNs = nowNs
                sensorSampleCount++

                val isNewGps = (lastProcessedLocationElapsedRealtimeNanos > lastProcessedLocationForSampleNs && lastProcessedLocationElapsedRealtimeNanos != 0L)
                if (isNewGps) {
                  lastProcessedLocationForSampleNs = lastProcessedLocationElapsedRealtimeNanos
                }

                val diff = abs(currentCalcKmh - gpsSpeedKmh)

                val rejReason = if (!isSampleValid) {
                  when {
                    corrX > maxNormalVib -> "Vibração lateral X excessiva"
                    corrY > maxNormalVib -> "Vibração vertical Y excessiva"
                    else -> "Giroscópio elevado"
                  }
                } else null

                val gpsAgeNs = elapsedRealtimeNs - lastProcessedGpsElapsedRealtimeNs
                val isGpsRecent = gpsAgeNs in 0..1_000_000_000L
                val confidence = if (isGpsRecent && lastGpsAccuracyMeters <= 8f && isSampleValid) "ALTA" else if (isGpsRecent && lastGpsAccuracyMeters <= 14f) "MEDIA" else "BAIXA"

                val fusionAccel = if (isGpsRecent) {
                  (DynoConfig.FUSION_GPS_WEIGHT * filteredGpsAccelerationMps2) + (DynoConfig.FUSION_SENSOR_WEIGHT * zFiltradoRun)
                } else {
                  zFiltradoRun
                }

                // Cálculo das Forças e Potência para a Amostra
                val vehicle = activeVehicleProfile
                val totalMass = vehicle?.totalWeightKg ?: 1100f
                val cd = vehicle?.dragCoefficient ?: 0.34f
                val area = vehicle?.frontalAreaM2 ?: 2.10f
                val density = vehicle?.airDensityKgM3 ?: 1.225f
                val cr = vehicle?.rollingResistanceCoeff ?: 0.015f
                val slopePercent = if (_uiState.value.slopeMode == "MANUAL") _uiState.value.manualSlopePercent else 0f

                val vMs = (gpsSpeedKmh / 3.6f).coerceAtLeast(0f)
                val fAccel = VehicleCalculations.calculateAccelerationForce(totalMass, max(0f, fusionAccel))
                val fAero = VehicleCalculations.calculateAerodynamicForce(vMs, cd, area, density)
                val fRoll = VehicleCalculations.calculateRollingResistanceForce(totalMass, cr)
                val fSlope = VehicleCalculations.calculateSlopeForce(totalMass, slopePercent)
                val fTotal = VehicleCalculations.calculateTotalTractiveForce(fAccel, fRoll, fAero, fSlope)

                val pWatts = VehicleCalculations.calculateWheelPowerWatts(fTotal, vMs)
                val pWheelCv = VehicleCalculations.convertWattsToCv(pWatts)
                val pEngineCv = VehicleCalculations.calculateEnginePowerCv(pWheelCv, vehicle?.drivetrain, vehicle?.customDrivetrainLossPercent)

                val tireCalc = if (vehicle != null) {
                  VehicleCalculations.calculateTireDimensions(vehicle.tireWidthMm, vehicle.tireAspectRatio, vehicle.wheelDiameterInches, vehicle.tireCorrectionPercent)
                } else null
                val sampleRpm = if (tireCalc != null) {
                  VehicleCalculations.calculateRpmFromSpeed(vMs, tireCalc.circumferenceM, _uiState.value.selectedGearRatio, _uiState.value.selectedFinalDrive)?.toInt()
                } else null

                val sampleEngineTorqueKgfm = if (sampleRpm != null && sampleRpm > 500) {
                  VehicleCalculations.calculateTorqueKgfm(pEngineCv, sampleRpm.toFloat()) ?: 0f
                } else 0f
                val sampleWheelTorqueKgfm = if (sampleRpm != null && sampleRpm > 500) {
                  VehicleCalculations.calculateTorqueKgfm(pWheelCv, sampleRpm.toFloat()) ?: 0f
                } else 0f

                val samplePoint = RunSample(
                  timestampMs = lastProcessedGpsTimestamp,
                  elapsedTimeMs = runDurationMs,
                  latitude = lastLatitude,
                  longitude = lastLongitude,
                  rawGpsSpeedMs = gpsSpeedMs,
                  rawGpsSpeedKmh = gpsSpeedKmh,
                  filteredSpeedMs = vMs,
                  filteredSpeedKmh = gpsSpeedKmh,
                  gpsSpeedKmh = gpsSpeedKmh,
                  calculatedSpeedKmh = currentCalcKmh,
                  speedDifferenceKmh = diff,
                  gpsAccuracyMeters = lastGpsAccuracyMeters,
                  gpsSpeedAccuracyMps = lastGpsSpeedAccuracyMps,
                  gpsAccelerationMps2 = filteredGpsAccelerationMps2,
                  sensorAccelerationMps2 = zFiltradoRun,
                  finalAccelerationMps2 = fusionAccel,
                  filteredAccelerationZ = zFiltradoRun,
                  correctedAccelerationZ = rawCorrigidoZ,
                  longitudinalG = currentG,
                  gyroMagnitude = gyroMag,
                  distanceMeters = totalRunDistanceMeters,
                  engineRpm = sampleRpm,
                  accelerationForceN = fAccel,
                  aerodynamicForceN = fAero,
                  rollingForceN = fRoll,
                  slopeForceN = fSlope,
                  totalForceN = fTotal,
                  wheelPowerWatts = pWatts,
                  wheelPowerKw = pWatts / 1000f,
                  wheelPowerCv = pWheelCv,
                  enginePowerCv = pEngineCv,
                  wheelTorqueKgfm = sampleWheelTorqueKgfm,
                  engineTorqueKgfm = sampleEngineTorqueKgfm,
                  wheelTorqueNm = sampleWheelTorqueKgfm * 9.80665f,
                  engineTorqueNm = sampleEngineTorqueKgfm * 9.80665f,
                  confidenceLevel = confidence,
                  isValid = isSampleValid,
                  rejectionReason = rejReason,
                  isNewGpsFix = isNewGps
                )
                recordedSamples.add(samplePoint)

                // Salva amostras em lotes pequenos no DynoMobileDB (a cada 20 amostras ou 2 segundos)
                val unpersistedCount = recordedSamples.size - persistedSampleCount
                val nowWallMs = SystemClock.elapsedRealtime()
                if (unpersistedCount >= 20 || (nowWallMs - lastSampleFlushTimeMs >= 2000L && unpersistedCount > 0)) {
                  val batch = recordedSamples.subList(persistedSampleCount, recordedSamples.size).toList()
                  val startIdx = persistedSampleCount
                  persistedSampleCount = recordedSamples.size
                  lastSampleFlushTimeMs = nowWallMs
                  val tid = currentTestId
                  if (tid != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                      runResultRepository.saveSampleBatch(tid, batch, startIdx)
                    }
                  }
                }
              }

              // Proteção de início
              if (runDurationMs < DynoConfig.START_PROTECTION_MS) {
                if (currentState != DynoRunState.MEDINDO_PROTEGIDO) {
                  _uiState.update { it.copy(testState = DynoRunState.MEDINDO_PROTEGIDO) }
                }
                suspectStartTimeNs = null
                suspectNegativeSampleCount = 0
              } else {
                if (currentState == DynoRunState.MEDINDO_PROTEGIDO) {
                  _uiState.update { it.copy(testState = DynoRunState.MEDINDO) }
                }

                // Suspeita de desaceleração inercial
                if (currentState == DynoRunState.MEDINDO) {
                  if (zDeadZone < DynoConfig.DECEL_SUSPECT_THRESHOLD) {
                    suspectNegativeSampleCount++
                    if (suspectStartTimeNs == null) {
                      suspectStartTimeNs = nowNs
                    } else {
                      val suspectMs = (nowNs - suspectStartTimeNs!!) / 1_000_000L
                      if (suspectMs >= DynoConfig.DECEL_SUSTAIN_MS && suspectNegativeSampleCount >= 15) {
                        _uiState.update { it.copy(testState = DynoRunState.SUSPEITA_DESACELERACAO) }
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
                  }
                }
              }

              // Timeout de segurança após 25 segundos
              if (elapsedSec > 25.0f) {
                finalizeRun(FinishReason.TIMEOUT, activeVehicleProfile)
              }
            }

            // Calibração dos Sensores
            if (isCalibrating) {
              val count = calibSampleCount
              if (count > 10) {
                val partialAvgX = (calibSumX / count).toFloat()
                val partialAvgY = (calibSumY / count).toFloat()
                val partialAvgZ = (calibSumZ / count).toFloat()
                val gyroMag = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)

                if (abs(event.values[0] - partialAvgX) > 2.5f ||
                  abs(event.values[1] - partialAvgY) > 2.5f ||
                  abs(event.values[2] - partialAvgZ) > 2.5f ||
                  gyroMag > 2.2f) {
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

            // Estabilidade do Aparelho (Requisito 4):
            // Média móvel de ~1 segundo nas leituras de giroscópio.
            // Considerar instável apenas se a média superar o limite continuamente por mais de 1 segundo.
            val nowNs = System.nanoTime()
            val rawGyroMag = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)
            synchronized(gyroWindow) {
              gyroWindow.add(GyroSample(nowNs, rawGyroMag))
              val oneSecAgo = nowNs - 1_000_000_000L
              while (gyroWindow.isNotEmpty() && gyroWindow.first().timestampNs < oneSecAgo) {
                gyroWindow.removeAt(0)
              }
              val avgGyroMag = if (gyroWindow.isNotEmpty()) {
                gyroWindow.map { it.magnitude }.average().toFloat()
              } else rawGyroMag

              if (avgGyroMag > 1.2f) {
                if (gyroUnstableStartTimeNs == 0L) {
                  gyroUnstableStartTimeNs = nowNs
                } else if (nowNs - gyroUnstableStartTimeNs > 1_000_000_000L) {
                  isPhoneStable = false
                }
              } else {
                gyroUnstableStartTimeNs = 0L
                isPhoneStable = true
              }
            }
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
      positiveAccelDurationMs = 0L
      lastAccelCheckNs = 0L
      resultSaved = false
    }
  }

  fun cancelTest() {
    val tid = currentTestId
    if (tid != null) {
      viewModelScope.launch(Dispatchers.IO) {
        val existing = runResultRepository.getResultByIdSuspending(tid)
        if (existing != null) {
          runResultRepository.saveResultSuspending(existing, status = "cancelled")
        }
      }
    }
    resetRunData()
    _uiState.update { it.copy(testState = DynoRunState.PARADO) }
  }

  private fun triggerOfficialRunStart(nowNs: Long, availableGpsKmh: Float) {
    val actualGpsSpeed = availableGpsKmh.coerceAtLeast(0f)
    val testId = java.util.UUID.randomUUID().toString()
    currentTestId = testId
    persistedSampleCount = 0
    lastSampleFlushTimeMs = SystemClock.elapsedRealtime()

    runStartTimeNs = nowNs
    lastSensorTimestampNs = nowNs
    lastSampleRecordedNs = nowNs
    lastDistanceIntegrationNs = nowNs
    lastDistanceSpeedMs = actualGpsSpeed / 3.6f

    totalRunDistanceMeters = 0f
    officialStartSpeedKmh = actualGpsSpeed
    officialMaxSpeedKmh = actualGpsSpeed
    startCalculatedKmh = actualGpsSpeed
    startGpsKmh = actualGpsSpeed
    integratedSpeedKmh = actualGpsSpeed
    maxIntegratedSpeedKmh = actualGpsSpeed
    maxGpsSpeedKmh = actualGpsSpeed
    maxDisplaySpeedKmh = actualGpsSpeed

    uniqueGpsFixes.clear()
    isGpsFrozenDetected = false
    gpsFrozenStartTimeNs = null
    gpsFrozenSpeedKmh = actualGpsSpeed
    gpsFrozenIntegratedStartKmh = actualGpsSpeed
    locationCallbackCount = 0
    uniqueGpsFixCount = 0
    gpsSpeedChangeCount = 0
    sensorSampleCount = 0
    maxGpsIntervalMs = 0L
    maxGpsAgeMs = 0L
    lastProcessedLocationForSampleNs = 0L

    splitTime0to60 = null
    splitTime0to100 = null
    splitCross60Ns = null
    splitCross80Ns = null
    splitCross100Ns = null
    splitTime60to100 = null
    splitTime80to120 = null
    splitTime100to200 = null
    splitTime60Feet = null
    splitTime100M = null
    splitTime201M = null
    splitTime402M = null

    suspectStartTimeNs = null
    suspectNegativeSampleCount = 0
    clutchStartTimeNs = null
    speedDropStartTimeNs = null

    gpsFixHistory.clear()
    diagnosticLogs.clear()
    diagnosticLogs.add("Início oficial da passagem: id=$testId, gatilho=${_uiState.value.startSpeedTriggerKmh} km/h, gpsInicial=$actualGpsSpeed km/h")

    totalInertialSamples = 0
    rejectedInertialSamples = 0
    syncDiffSum = 0.0
    syncDiffCount = 0
    maxSyncDiff = 0f
    validGpsUpdatesDuringRunCount = 1
    resultSaved = false
    recordedSamples.clear()
    inertialHistory.clear()

    // Cria imediatamente um registro no DynoMobileDB com status "recording"
    val profile = activeVehicleProfile
    val snapshotJson = runResultRepository.createSnapshotFromProfile(
      profile,
      _uiState.value.selectedGearRatio,
      _uiState.value.selectedFinalDrive,
      _uiState.value.selectedGear,
      _uiState.value.slopeMode,
      if (_uiState.value.slopeMode == "MANUAL") _uiState.value.manualSlopePercent else 0f
    )
    val vehName = "${profile?.manufacturer ?: ""} ${profile?.model ?: ""} ${profile?.engine ?: ""}".trim()

    viewModelScope.launch(Dispatchers.IO) {
      runResultRepository.startRecordingTest(
        testId = testId,
        vehicleId = profile?.id,
        vehicleName = vehName,
        snapshotJson = snapshotJson,
        startSpeedKmh = actualGpsSpeed
      )
    }

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
      val peakDiff = abs(maxGpsSpeedKmh - maxIntegratedSpeedKmh)
      val avgDiff = if (syncDiffCount > 0) (syncDiffSum / syncDiffCount).toFloat() else 0f

      // Descartar o primeiro e o último ponto se forem transientes de início/corte
      val rawSamples = recordedSamples.take(500)
      val trimmedSamples = if (rawSamples.size > 4) {
        rawSamples.subList(1, rawSamples.size - 1)
      } else rawSamples

      val finalSamples = trimmedSamples.toList()
      val validCount = finalSamples.count { it.isValid }
      val rejectedCount = finalSamples.count { !it.isValid }
      val rejectionRatio = if (totalInertialSamples > 0) rejectedInertialSamples.toFloat() / totalInertialSamples.toFloat() else 0f

      val officialEndSpeed = finalGpsKmh
      val effectiveMaxGps = if (officialMaxSpeedKmh > 0f) officialMaxSpeedKmh else maxGpsSpeedKmh
      val effectiveStartGps = if (officialStartSpeedKmh > 0f) officialStartSpeedKmh else startGpsKmh
      val speedGainKmh = (effectiveMaxGps - effectiveStartGps).coerceAtLeast(0f)

      val avgHz = if (elapsedSec > 0f) finalSamples.size / elapsedSec else 0f

      val elapsedGpsSec = if (uniqueGpsFixes.size > 1) {
        (uniqueGpsFixes.last().elapsedRealtimeNanos - uniqueGpsFixes.first().elapsedRealtimeNanos) / 1_000_000_000.0f
      } else 0f

      val gpsFreqHz = if (uniqueGpsFixes.size > 1 && elapsedGpsSec > 0.05f) {
        (uniqueGpsFixes.size - 1) / elapsedGpsSec
      } else if (elapsedSec > 0f) {
        validGpsUpdatesDuringRunCount / elapsedSec
      } else 0f

      var wheelPowerCv = 0f
      var enginePowerCv = 0f
      var wheelTorqueKgfm = 0f
      var engineTorqueKgfm = 0f
      var peakLongG = peakLongitudinalG
      var avgLongG = 0f
      var peakPowerRpm: Int? = null
      var peakTorqueRpm: Int? = null
      var peakPowerSpeedKmh = effectiveMaxGps
      var peakTorqueSpeedKmh = effectiveStartGps + (effectiveMaxGps - effectiveStartGps) * 0.45f
      var totalMassKg = 0f
      var drivetrainLossPercent = 12f
      var marginPercent = 10f

      val profileToUse = vehicle ?: activeVehicleProfile
      if (profileToUse != null) {
        totalMassKg = profileToUse.totalWeightKg
        val frontalArea = profileToUse.frontalAreaM2
        val cd = profileToUse.dragCoefficient
        val cr = profileToUse.rollingResistanceCoeff
        val drivetrain = profileToUse.drivetrain
        val efficiency = VehicleCalculations.getDrivetrainEfficiency(drivetrain, profileToUse.customDrivetrainLossPercent)
        drivetrainLossPercent = VehicleCalculations.getDrivetrainLossPercent(drivetrain, profileToUse.customDrivetrainLossPercent)

        val tireCalc = VehicleCalculations.calculateTireDimensions(
          widthMm = profileToUse.tireWidthMm,
          aspectRatio = profileToUse.tireAspectRatio,
          rimInches = profileToUse.wheelDiameterInches,
          tireCorrectionPercent = profileToUse.tireCorrectionPercent
        )

        val gearRatio = _uiState.value.selectedGearRatio
        val finalDrive = _uiState.value.selectedFinalDrive
        val slopePercent = if (_uiState.value.slopeMode == "MANUAL") _uiState.value.manualSlopePercent else 0f

        val rollForce = VehicleCalculations.calculateRollingResistanceForce(totalMassKg, cr)
        val slopeForce = VehicleCalculations.calculateSlopeForce(totalMassKg, slopePercent)

        // Processamento detalhado e limpo de cada amostra
        val computedSamples = finalSamples.map { sample ->
          val aMps2 = sample.finalAccelerationMps2
          val g = aMps2 / 9.80665f
          val vMps = sample.filteredSpeedMs
          val fAero = VehicleCalculations.calculateAerodynamicForce(vMps, cd, frontalArea, profileToUse.airDensityKgM3)
          val fAccel = VehicleCalculations.calculateAccelerationForce(totalMassKg, max(0f, aMps2))
          val fTractive = VehicleCalculations.calculateTotalTractiveForce(fAccel, rollForce, fAero, slopeForce)
          val wWatts = VehicleCalculations.calculateWheelPowerWatts(fTractive, vMps)
          val sampleWheelPowerCv = VehicleCalculations.convertWattsToCv(wWatts)
          val sampleEnginePowerCv = VehicleCalculations.calculateEnginePowerCv(sampleWheelPowerCv, drivetrain, profileToUse.customDrivetrainLossPercent)

          val sampleRpm = VehicleCalculations.calculateRpmFromSpeed(vMps, tireCalc.circumferenceM, gearRatio, finalDrive)?.toInt()
          val sampleTorquePair = if (sampleRpm != null && sampleRpm > 500) {
            val engineWatts = VehicleCalculations.convertCvToWatts(sampleEnginePowerCv)
            VehicleCalculations.calculateTorqueFromPowerWatts(engineWatts, sampleRpm.toFloat())
          } else null

          val sampleWheelTorquePair = if (sampleRpm != null && sampleRpm > 500) {
            VehicleCalculations.calculateTorqueFromPowerWatts(wWatts, sampleRpm.toFloat())
          } else null

          val sampleEngineTorqueKgfm = sampleTorquePair?.second ?: 0f
          val sampleWheelTorqueKgfm = sampleWheelTorquePair?.second ?: 0f

          sample.copy(
            longitudinalG = g,
            accelerationForceN = fAccel,
            aerodynamicForceN = fAero,
            rollingForceN = rollForce,
            slopeForceN = slopeForce,
            totalForceN = fTractive,
            wheelPowerWatts = wWatts,
            wheelPowerKw = wWatts / 1000f,
            wheelPowerCv = sampleWheelPowerCv,
            enginePowerCv = sampleEnginePowerCv,
            wheelTorqueKgfm = sampleWheelTorqueKgfm,
            engineTorqueKgfm = sampleEngineTorqueKgfm,
            wheelTorqueNm = sampleWheelTorquePair?.first ?: 0f,
            engineTorqueNm = sampleTorquePair?.first ?: 0f,
            engineRpm = sampleRpm
          )
        }

        val sampleRpms = computedSamples.mapNotNull { it.engineRpm }.filter { it > 500 }
        val sampleRpmSpan = if (sampleRpms.isNotEmpty()) ((sampleRpms.maxOrNull() ?: 0) - (sampleRpms.minOrNull() ?: 0)) else null
        val isRpmValid = (sampleRpms.size >= 6 && sampleRpmSpan != null && sampleRpmSpan >= 800)

        // Classificação técnica da passagem
        val hasGearShift = finalSamples.any { it.isGearShift || it.isClutchDisengaged }
        val isStable = _uiState.value.isPhoneStable && !_uiState.value.hasPhoneMovedAfterCalib

        val qualityEval = VehicleCalculations.classifyRunQuality(
          speedGainKmh = speedGainKmh,
          validGpsLocationsCount = if (uniqueGpsFixCount > 0) uniqueGpsFixCount else validGpsUpdatesDuringRunCount,
          elapsedSec = elapsedSec,
          lastGpsAccuracyMeters = lastGpsAccuracyMeters,
          avgSyncDiffKmh = avgDiff,
          rejectionRatio = rejectionRatio,
          finishReason = reason,
          isPhoneStable = isStable,
          gearShiftDetected = hasGearShift,
          finalGpsSpeedKmh = officialEndSpeed,
          startGpsSpeedKmh = effectiveStartGps,
          rpmSpan = sampleRpmSpan,
          gpsFrozen = isGpsFrozenDetected,
          maxIntegratedSpeedKmh = maxIntegratedSpeedKmh,
          maxGpsSpeedKmh = effectiveMaxGps
        )

        val runQualityStr = qualityEval.quality
        val confidenceLevelStr = qualityEval.confidenceLevel
        val invalidReasonText = qualityEval.invalidationReason
        marginPercent = qualityEval.marginPercent

        // Detecção de picos sustentados (evita pico isolado de vibração)
        val peaks = VehicleCalculations.findSustainedPeaks(computedSamples, isRpmValid = isRpmValid)
        wheelPowerCv = peaks.peakWheelPowerCv
        enginePowerCv = peaks.peakEnginePowerCv
        peakLongG = peaks.peakLongitudinalG
        peakPowerRpm = peaks.peakPowerRpm
        peakTorqueRpm = peaks.peakTorqueRpm
        peakPowerSpeedKmh = peaks.peakPowerSpeedKmh
        peakTorqueSpeedKmh = peaks.peakTorqueSpeedKmh
        wheelTorqueKgfm = peaks.wheelTorqueKgfm
        engineTorqueKgfm = peaks.engineTorqueKgfm

        val validComputed = computedSamples.filter { it.isValid && it.finalAccelerationMps2 > 0.05f }
        if (validComputed.isNotEmpty()) {
          avgLongG = validComputed.map { it.longitudinalG }.average().toFloat()
        }

        val avgAccuracy = if (gpsFixHistory.isNotEmpty()) {
          gpsFixHistory.map { it.accuracyM }.average().toFloat()
        } else lastGpsAccuracyMeters

        val finalTestId = currentTestId ?: java.util.UUID.randomUUID().toString()
        val result = RunResult(
          id = finalTestId,
          vehicleId = profileToUse.id,
          vehicleName = "${profileToUse.manufacturer} ${profileToUse.model} ${profileToUse.engine}".trim(),
          officialStartSpeedKmh = effectiveStartGps,
          officialMaxSpeedKmh = effectiveMaxGps,
          officialEndSpeedKmh = officialEndSpeed,
          officialSpeedGainKmh = speedGainKmh,
          runStartCalculatedSpeedKmh = startCalculatedKmh,
          runStartGpsSpeedKmh = effectiveStartGps,
          startSpeedKmh = effectiveStartGps,
          maximumGpsSpeedKmh = effectiveMaxGps,
          maximumCalculatedSpeedKmh = maxIntegratedSpeedKmh,
          maxIntegratedSpeedKmh = maxIntegratedSpeedKmh,
          finalGpsSpeedKmh = officialEndSpeed,
          finalCalculatedSpeedKmh = finalCalcSpeedKmh,
          finalIntegratedSpeedKmh = finalCalcSpeedKmh,
          finalSpeedKmh = officialEndSpeed,
          speedGainKmh = speedGainKmh,
          totalDistanceMeters = totalRunDistanceMeters,
          estimatedPowerCv = enginePowerCv,
          estimatedTorqueKgfm = engineTorqueKgfm,
          wheelPowerCv = wheelPowerCv,
          enginePowerCv = enginePowerCv,
          wheelPowerKw = (wheelPowerCv * 735.49875f) / 1000f,
          enginePowerKw = (enginePowerCv * 735.49875f) / 1000f,
          wheelTorqueKgfm = wheelTorqueKgfm,
          engineTorqueKgfm = engineTorqueKgfm,
          wheelTorqueNm = wheelTorqueKgfm * 9.80665f,
          engineTorqueNm = engineTorqueKgfm * 9.80665f,
          peakLongitudinalG = peakLongG,
          averageLongitudinalG = avgLongG,
          peakPowerRpm = peakPowerRpm,
          peakTorqueRpm = peakTorqueRpm,
          peakPowerSpeedKmh = peakPowerSpeedKmh,
          peakTorqueSpeedKmh = peakTorqueSpeedKmh,
          totalVehicleMassKg = totalMassKg,
          drivetrainLossPercent = drivetrainLossPercent,
          estimatedMarginPercent = marginPercent,
          gearUsed = _uiState.value.selectedGear,
          gearRatioUsed = gearRatio,
          finalDriveUsed = finalDrive,
          isAerodynamicsEstimated = true,
          cdUsed = cd,
          frontalAreaUsed = frontalArea,
          crrUsed = cr,
          airDensityUsed = profileToUse.airDensityKgM3,
          slopeModeUsed = _uiState.value.slopeMode,
          slopePercentUsed = slopePercent,
          confidenceLevel = confidenceLevelStr,
          elapsedSeconds = elapsedSec,
          gpsAccuracyMeters = lastGpsAccuracyMeters,
          averageGpsAccuracyMeters = avgAccuracy,
          totalSamples = computedSamples.size,
          rejectedSamples = rejectedCount,
          validSamplesCount = validCount,
          validGpsLocationsCount = if (uniqueGpsFixCount > 0) uniqueGpsFixCount else validGpsUpdatesDuringRunCount,
          locationCallbackCount = locationCallbackCount,
          uniqueGpsFixCount = if (uniqueGpsFixCount > 0) uniqueGpsFixCount else uniqueGpsFixes.size,
          gpsSpeedChangeCount = gpsSpeedChangeCount,
          sensorSampleCount = if (sensorSampleCount > 0) sensorSampleCount else computedSamples.size,
          maxGpsIntervalMs = maxGpsIntervalMs,
          maxGpsAgeMs = maxGpsAgeMs,
          gpsFrozen = isGpsFrozenDetected,
          isPreliminary = qualityEval.isPreliminary,
          averageSamplingRateHz = avgHz,
          averageGpsFrequencyHz = gpsFreqHz,
          quality = runQualityStr,
          finishReason = reason.code,
          averageSpeedDifferenceKmh = avgDiff,
          maximumSpeedDifferenceKmh = maxSyncDiff,
          invalidationReason = invalidReasonText,
          appVersion = "0.20.0",
          time0to60Kmh = splitTime0to60,
          time0to100Kmh = splitTime0to100,
          time60to100Kmh = splitTime60to100,
          time80to120Kmh = splitTime80to120,
          time100to200Kmh = splitTime100to200,
          time60Feet = splitTime60Feet,
          time100M = splitTime100M,
          time201M = splitTime201M,
          time402M = splitTime402M,
          samples = computedSamples,
          uniqueGpsFixes = uniqueGpsFixes.toList()
        )

        viewModelScope.launch(Dispatchers.IO) {
          val savedOk = runResultRepository.saveResultSuspending(result, status = "completed")
          resultSaved = savedOk
          withContext(Dispatchers.Main) {
            onRunCompletedCallback?.invoke(savedOk)
          }
        }
      } else {
        onRunCompletedCallback?.invoke(false)
      }
    }
  }

  private fun resetRunData() {
    zMedianBuffer.clear()
    zFiltradoRun = 0f
    peakLongitudinalG = 0f
    liveLongitudinalG = 0f
    filteredGpsAccelerationMps2 = 0f
    previousGpsSpeedMs = 0f
    previousGpsElapsedNs = 0L
    gpsSpeedMovingAverageBuffer.clear()
    consecutiveHighSpeedCount = 0
    gyroUnstableStartTimeNs = 0L
    isPhoneStable = true
    synchronized(gyroWindow) {
      gyroWindow.clear()
    }

    suspectStartTimeNs = null
    suspectNegativeSampleCount = 0
    clutchStartTimeNs = null
    speedDropStartTimeNs = null
    invalidContinuousAnomalyStartTimeNs = null
    runStartTimeNs = 0L
    runEndTimeNs = 0L
    lastSensorTimestampNs = 0L
    lastSampleRecordedNs = 0L
    lastDistanceIntegrationNs = 0L
    lastDistanceSpeedMs = 0f

    totalRunDistanceMeters = 0f
    splitTime0to60 = null
    splitTime0to100 = null
    splitCross60Ns = null
    splitCross80Ns = null
    splitCross100Ns = null
    splitTime60to100 = null
    splitTime80to120 = null
    splitTime100to200 = null
    splitTime60Feet = null
    splitTime100M = null
    splitTime201M = null
    splitTime402M = null

    armedEstimatedSpeedMs = 0f
    armedLastNanoTime = 0L
    positiveAccelDurationMs = 0L
    lastAccelCheckNs = 0L

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
