package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.RunResultRepository
import com.example.data.VehicleRepository
import com.example.model.FinishReason
import com.example.model.RunResult
import com.example.model.RunSample
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class DynoRunState {
  PARADO,
  AGUARDANDO_30,
  MEDINDO,
  FINALIZADO
}

data class PrepBufferSample(
  val timestampNs: Long,
  val linearX: Float,
  val linearY: Float,
  val linearZ: Float,
  val zCorrigido: Float,
  val zFiltrado: Float,
  val gpsSpeedKmh: Float,
  val armedEstimatedSpeedKmh: Float,
  val gpsAccuracyMeters: Float,
  val gyroMagnitude: Float,
  val isValid: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorScreen(
  onNavigateBack: () -> Unit,
  onNavigateToResults: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onNavigateBack)

  val context = LocalContext.current
  val sensorManager = remember {
    context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
  }
  val locationManager = remember {
    context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
  }

  val vehicleRepository = remember { VehicleRepository(context) }
  val runResultRepository = remember { RunResultRepository(context) }
  val primaryVehicle = remember { vehicleRepository.getPrimaryVehicle() }

  val accelerometerSensor = remember(sensorManager) {
    sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
  }
  val linearAccelerationSensor = remember(sensorManager) {
    sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
  }
  val gyroscopeSensor = remember(sensorManager) {
    sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
  }

  val isRawAvailable = accelerometerSensor != null
  val isLinearAvailable = linearAccelerationSensor != null
  val isGyroAvailable = gyroscopeSensor != null

  var rawX by remember { mutableFloatStateOf(0f) }
  var rawY by remember { mutableFloatStateOf(0f) }
  var rawZ by remember { mutableFloatStateOf(0f) }

  var linearX by remember { mutableFloatStateOf(0f) }
  var linearY by remember { mutableFloatStateOf(0f) }
  var linearZ by remember { mutableFloatStateOf(0f) }

  var filteredLinearX by remember { mutableFloatStateOf(0f) }
  var filteredLinearY by remember { mutableFloatStateOf(0f) }
  var filteredLinearZ by remember { mutableFloatStateOf(0f) }

  var gyroX by remember { mutableFloatStateOf(0f) }
  var gyroY by remember { mutableFloatStateOf(0f) }
  var gyroZ by remember { mutableFloatStateOf(0f) }

  var samplingFrequencyHz by remember { mutableDoubleStateOf(0.0) }

  val scope = rememberCoroutineScope()

  // Longitudinal Axis Preferences & State
  val prefs = remember(context) {
    context.getSharedPreferences("dyno_lite_prefs", Context.MODE_PRIVATE)
  }
  var selectedAxis by remember {
    mutableStateOf(prefs.getString("selected_longitudinal_axis", "Z") ?: "Z")
  }
  var invertSignal by remember {
    mutableStateOf(prefs.getBoolean("invert_longitudinal_signal", false))
  }

  // Calibration Offsets & Vehicle Vibration Baseline State
  var isCalibrated by remember {
    mutableStateOf(prefs.getBoolean("is_calibrated", false))
  }
  var offsetX by remember {
    mutableFloatStateOf(prefs.getFloat("offset_x", 0.0f))
  }
  var offsetY by remember {
    mutableFloatStateOf(prefs.getFloat("offset_y", 0.0f))
  }
  var offsetZ by remember {
    mutableFloatStateOf(prefs.getFloat("offset_z", 0.0f))
  }
  var calibratedNormalVibration by remember {
    mutableFloatStateOf(prefs.getFloat("calibrated_vibration", 0.12f))
  }
  var calibratedGyroDeviation by remember {
    mutableFloatStateOf(prefs.getFloat("calibrated_gyro", 0.08f))
  }

  var isCalibrating by remember { mutableStateOf(false) }
  var calibrationStatus by remember {
    mutableStateOf(if (isCalibrated) "Calibração concluída" else "Não calibrado")
  }

  // Calibration Collector: Collects ~150 samples (~3 seconds) with vehicle stopped & engine running
  val calibCollector = remember {
    object {
      var isCollecting = false
      var count = 0
      var sumX = 0.0
      var sumY = 0.0
      var sumZ = 0.0
      var sumDevX = 0.0
      var sumDevY = 0.0
      var sumDevZ = 0.0
      var sumGyroMag = 0.0

      fun reset() {
        isCollecting = false
        count = 0
        sumX = 0.0
        sumY = 0.0
        sumZ = 0.0
        sumDevX = 0.0
        sumDevY = 0.0
        sumDevZ = 0.0
        sumGyroMag = 0.0
      }
    }
  }

  // Aceleração corrigida (sem filtro)
  val corrigidoX = linearX - offsetX
  val corrigidoY = linearY - offsetY
  val corrigidoZ = linearZ - offsetZ

  val rawCorrigido = when (selectedAxis) {
    "X" -> corrigidoX
    "Y" -> corrigidoY
    else -> corrigidoZ
  }
  val aceleracaoCorrigida = if (invertSignal) rawCorrigido * -1f else rawCorrigido

  // Aceleração filtrada corrigida
  val filtradoX = filteredLinearX - offsetX
  val filtradoY = filteredLinearY - offsetY
  val filtradoZ = filteredLinearZ - offsetZ

  val rawFiltrado = when (selectedAxis) {
    "X" -> filtradoX
    "Y" -> filtradoY
    else -> filtradoZ
  }
  val filtradoInvertido = if (invertSignal) rawFiltrado * -1f else rawFiltrado

  // Zona morta somente no valor filtrado: entre -0.05 e +0.05 m/s² mostra 0.000 m/s²
  val aceleracaoFiltrada = if (filtradoInvertido in -0.05f..0.05f) 0f else filtradoInvertido

  // Direção baseada SOMENTE na aceleração filtrada
  val direcao = when {
    aceleracaoFiltrada > 0.15f -> "frente"
    aceleracaoFiltrada < -0.15f -> "trás"
    else -> "parado"
  }

  val currentOffset = when (selectedAxis) {
    "X" -> offsetX
    "Y" -> offsetY
    else -> offsetZ
  }

  // Experimental Velocity Integration States (Manual Playground)
  var isIntegrating by remember { mutableStateOf(false) }
  var integratedVelocityMs by remember { mutableFloatStateOf(0f) }
  var integrationElapsedSeconds by remember { mutableFloatStateOf(0f) }

  val currentAceleracaoFiltrada by rememberUpdatedState(aceleracaoFiltrada)

  LaunchedEffect(isIntegrating) {
    if (isIntegrating) {
      var lastNanoTime = System.nanoTime()
      while (isActive) {
        delay(20L)
        val now = System.nanoTime()
        val dt = (now - lastNanoTime) / 1_000_000_000f
        lastNanoTime = now

        if (dt > 0f && dt <= 0.1f) {
          val novaVelocidade = (integratedVelocityMs + currentAceleracaoFiltrada * dt).coerceAtLeast(0f)
          integratedVelocityMs = novaVelocidade
          integrationElapsedSeconds += dt
        }
      }
    }
  }

  val integratedVelocityKmh = integratedVelocityMs * 3.6f

  // -------------------------------------------------------------
  // DADOS DA PASSAGEM DINAMOMÉTRICA (0.15.0)
  // -------------------------------------------------------------
  var runState by remember { mutableStateOf(DynoRunState.PARADO) }
  var currentGpsSpeedKmh by remember { mutableFloatStateOf(0f) }
  var armedEstimatedSpeedKmh by remember { mutableFloatStateOf(0f) }
  var runStartCalculatedSpeedKmh by remember { mutableFloatStateOf(30.0f) }
  var runStartGpsSpeedKmh by remember { mutableFloatStateOf(0f) }
  var runFinalGpsSpeedKmh by remember { mutableFloatStateOf(0f) }
  var runMaximumGpsSpeedKmh by remember { mutableFloatStateOf(0f) }
  var runFinalCalculatedSpeedKmh by remember { mutableFloatStateOf(0f) }
  var runMaximumCalculatedSpeedKmh by remember { mutableFloatStateOf(30.0f) }
  var runMaximumCalculatedSpeedMs by remember { mutableFloatStateOf(30.0f / 3.6f) }
  var runElapsedSeconds by remember { mutableFloatStateOf(0f) }
  var runVelocityMs by remember { mutableFloatStateOf(0f) }
  var totalSamples by remember { mutableIntStateOf(0) }
  var rejectedSamples by remember { mutableIntStateOf(0) }
  var resultSaved by remember { mutableStateOf(false) }
  var runFinishReason by remember { mutableStateOf<FinishReason?>(null) }
  var averageSpeedDifferenceKmh by remember { mutableFloatStateOf(0f) }
  var maximumSpeedDifferenceKmh by remember { mutableFloatStateOf(0f) }

  val runVelocityKmh = runVelocityMs * 3.6f

  // Tracker State & Circular Buffer & Time Series Recording
  val dynoTracker = remember {
    object {
      var state: DynoRunState = DynoRunState.PARADO
      var isCalibrated: Boolean = false
      var offsetZ: Float = 0f
      var offsetX: Float = 0f
      var offsetY: Float = 0f
      var normalVib: Float = 0.12f
      var gyroDev: Float = 0.08f
      var invertSignal: Boolean = false

      // Armed preparation estimation
      var armedEstimatedSpeedMs: Float = 0f
      var armedLastNanoTime: Long = 0L
      val prepCircularBuffer = mutableListOf<PrepBufferSample>()
      val maxPrepBufferSize = 180 // ~3s @ 60Hz

      // Measurement state
      var runStartTimeNs: Long = 0L
      var runEndTimeNs: Long = 0L
      var lastSensorTimestampNs: Long = 0L
      var lastSampleRecordedNs: Long = 0L
      var decelerationStartNs: Long? = null
      var gpsSpeedDropStartNs: Long? = null

      val zMedianBuffer = mutableListOf<Float>()
      var zFiltradoRun = 0f

      var startCalculatedKmh = 30.0f
      var startGpsKmh = 0f
      var maxGpsKmh = 0f
      var finalGpsKmh = 0f

      var velocityMs = 0f
      var maxCalcSpeedKmh = 30.0f
      var maxCalcSpeedMs = 30.0f / 3.6f
      var finalCalcSpeedKmh = 0f

      var elapsedSec = 0f
      var total = 0
      var rejected = 0
      var finishReason: FinishReason? = null

      var diffSum = 0.0
      var diffCount = 0
      var maxDiff = 0f

      // Time series samples (max 500 immutable samples)
      val recordedSamples = mutableListOf<RunSample>()

      fun reset() {
        state = DynoRunState.PARADO
        zMedianBuffer.clear()
        zFiltradoRun = 0f
        decelerationStartNs = null
        gpsSpeedDropStartNs = null
        runStartTimeNs = 0L
        runEndTimeNs = 0L
        lastSensorTimestampNs = 0L
        lastSampleRecordedNs = 0L

        armedEstimatedSpeedMs = 0f
        armedLastNanoTime = 0L
        prepCircularBuffer.clear()

        startCalculatedKmh = 30.0f
        startGpsKmh = 0f
        maxGpsKmh = 0f
        finalGpsKmh = 0f

        velocityMs = 0f
        maxCalcSpeedKmh = 30.0f
        maxCalcSpeedMs = 30.0f / 3.6f
        finalCalcSpeedKmh = 0f

        elapsedSec = 0f
        total = 0
        rejected = 0
        finishReason = null

        diffSum = 0.0
        diffCount = 0
        maxDiff = 0f

        recordedSamples.clear()
      }
    }
  }

  dynoTracker.isCalibrated = isCalibrated
  dynoTracker.offsetZ = offsetZ
  dynoTracker.offsetX = offsetX
  dynoTracker.offsetY = offsetY
  dynoTracker.normalVib = calibratedNormalVibration
  dynoTracker.gyroDev = calibratedGyroDeviation
  dynoTracker.invertSignal = invertSignal

  // GPS Manager & Listener
  var hasLocationPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
      ) == PackageManager.PERMISSION_GRANTED ||
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
      ) == PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
  }

  LaunchedEffect(Unit) {
    if (!hasLocationPermission) {
      permissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION
        )
      )
    }
  }

  var isGpsProviderEnabled by remember {
    mutableStateOf(
      locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
    )
  }
  var hasGpsFix by remember { mutableStateOf(false) }
  var gpsAccuracyM by remember { mutableFloatStateOf(0.0f) }

  // Official Start of Dyno Run at 30 km/h
  fun triggerOfficialRunStart(nowNs: Long, availableGpsKmh: Float) {
    dynoTracker.state = DynoRunState.MEDINDO
    dynoTracker.runStartTimeNs = nowNs
    dynoTracker.lastSensorTimestampNs = nowNs
    dynoTracker.lastSampleRecordedNs = nowNs

    dynoTracker.startCalculatedKmh = 30.0f
    dynoTracker.startGpsKmh = availableGpsKmh
    dynoTracker.velocityMs = 30.0f / 3.6f
    dynoTracker.maxCalcSpeedKmh = 30.0f
    dynoTracker.maxCalcSpeedMs = 30.0f / 3.6f
    dynoTracker.maxGpsKmh = max(availableGpsKmh, 30.0f)

    dynoTracker.decelerationStartNs = null
    dynoTracker.gpsSpeedDropStartNs = null
    dynoTracker.rejected = 0
    dynoTracker.total = 0
    dynoTracker.diffSum = 0.0
    dynoTracker.diffCount = 0
    dynoTracker.maxDiff = 0f
    dynoTracker.finishReason = null
    dynoTracker.recordedSamples.clear()
    dynoTracker.prepCircularBuffer.clear()

    // First sample at exactly t = 0 ms and 30.0 km/h
    val firstSample = RunSample(
      elapsedTimeMs = 0L,
      filteredAccelerationZ = dynoTracker.zFiltradoRun,
      correctedAccelerationZ = (linearZ - dynoTracker.offsetZ) * (if (dynoTracker.invertSignal) -1f else 1f),
      gpsSpeedKmh = availableGpsKmh,
      calculatedSpeedKmh = 30.0f,
      speedDifferenceKmh = abs(30.0f - availableGpsKmh),
      gpsAccuracyMeters = gpsAccuracyM,
      gyroMagnitude = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ),
      isValid = true
    )
    dynoTracker.recordedSamples.add(firstSample)
    dynoTracker.total++

    runVelocityMs = 30.0f / 3.6f
    runStartCalculatedSpeedKmh = 30.0f
    runStartGpsSpeedKmh = availableGpsKmh
    runMaximumGpsSpeedKmh = dynoTracker.maxGpsKmh
    runMaximumCalculatedSpeedKmh = 30.0f
    runMaximumCalculatedSpeedMs = 30.0f / 3.6f
    runElapsedSeconds = 0f
    rejectedSamples = 0
    totalSamples = 1
    resultSaved = false
    runFinishReason = null
    runState = DynoRunState.MEDINDO
  }

  // Function to finalize run and save automatically once
  fun finalizeRun(reason: FinishReason) {
    if (dynoTracker.state == DynoRunState.MEDINDO) {
      val nowNs = System.nanoTime()
      dynoTracker.runEndTimeNs = nowNs
      dynoTracker.finalGpsKmh = currentGpsSpeedKmh
      dynoTracker.finalCalcSpeedKmh = dynoTracker.velocityMs * 3.6f
      dynoTracker.finishReason = reason
      dynoTracker.state = DynoRunState.FINALIZADO

      runFinalGpsSpeedKmh = currentGpsSpeedKmh
      runFinalCalculatedSpeedKmh = dynoTracker.finalCalcSpeedKmh
      runFinishReason = reason
      runState = DynoRunState.FINALIZADO

      // Calculate final difference stats
      val avgDiff = if (dynoTracker.diffCount > 0) (dynoTracker.diffSum / dynoTracker.diffCount).toFloat() else 0f
      averageSpeedDifferenceKmh = avgDiff
      maximumSpeedDifferenceKmh = dynoTracker.maxDiff

      // Evaluate Quality
      val rejectionRatio = if (dynoTracker.total > 0) dynoTracker.rejected.toFloat() / dynoTracker.total.toFloat() else 0f
      val runQualityStr = when {
        reason == FinishReason.TIMEOUT || dynoTracker.elapsedSec > 25f || dynoTracker.elapsedSec < 2f ||
          gpsAccuracyM > 10f || rejectionRatio > 0.25f || dynoTracker.maxGpsKmh < 40f -> "INVÁLIDA"
        gpsAccuracyM <= 6f && dynoTracker.elapsedSec in 2f..20f && rejectionRatio <= 0.10f -> "BOA"
        gpsAccuracyM <= 10f && dynoTracker.elapsedSec <= 25f && rejectionRatio <= 0.25f -> "REGULAR"
        else -> "INVÁLIDA"
      }

      // Freeze immutable samples list (max 500 samples)
      val finalSamples = dynoTracker.recordedSamples.take(500).toList()
      val validCount = finalSamples.count { it.isValid }
      val rejectedCount = finalSamples.count { !it.isValid }
      val avgHz = if (dynoTracker.elapsedSec > 0f) finalSamples.size / dynoTracker.elapsedSec else 0f

      // Auto-save RunResult once
      if (!resultSaved) {
        val result = RunResult(
          vehicleId = primaryVehicle?.id,
          vehicleName = if (primaryVehicle != null) "${primaryVehicle.manufacturer} ${primaryVehicle.model} ${primaryVehicle.engine}".trim() else "Veículo Principal",
          runStartCalculatedSpeedKmh = 30.0f,
          runStartGpsSpeedKmh = dynoTracker.startGpsKmh,
          maximumGpsSpeedKmh = dynoTracker.maxGpsKmh,
          maximumCalculatedSpeedKmh = dynoTracker.maxCalcSpeedKmh,
          finalGpsSpeedKmh = dynoTracker.finalGpsKmh,
          finalCalculatedSpeedKmh = dynoTracker.finalCalcSpeedKmh,
          elapsedSeconds = dynoTracker.elapsedSec,
          gpsAccuracyMeters = gpsAccuracyM,
          totalSamples = finalSamples.size,
          rejectedSamples = rejectedCount,
          validSamplesCount = validCount,
          averageSamplingRateHz = avgHz,
          quality = runQualityStr,
          finishReason = reason.code,
          averageSpeedDifferenceKmh = avgDiff,
          maximumSpeedDifferenceKmh = dynoTracker.maxDiff,
          appVersion = "0.15.0",
          samples = finalSamples
        )
        runResultRepository.saveResult(result)
        resultSaved = true
      }
    }
  }

  DisposableEffect(locationManager, hasLocationPermission) {
    if (locationManager != null && hasLocationPermission) {
      isGpsProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

      val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
          hasGpsFix = true
          val rawSpeed = if (location.hasSpeed()) location.speed else 0f
          val speedKmh = (rawSpeed * 3.6f).coerceAtLeast(0f)
          currentGpsSpeedKmh = speedKmh
          gpsAccuracyM = location.accuracy

          if (dynoTracker.state == DynoRunState.AGUARDANDO_30) {
            // Suave correção da velocidade estimada pelo GPS durante preparação:
            // armedEstimatedSpeedMs = armedEstimatedSpeedMs * 0.70 + gpsSpeedMs * 0.30
            if (location.hasSpeed()) {
              dynoTracker.armedEstimatedSpeedMs = (dynoTracker.armedEstimatedSpeedMs * 0.70f + rawSpeed * 0.30f).coerceAtLeast(0f)
              armedEstimatedSpeedKmh = dynoTracker.armedEstimatedSpeedMs * 3.6f
            }

            // Início Oficial:
            // armedEstimatedSpeedMs >= 30 km/h, GPS >= 25 km/h, precisão <= 10m e calibrado
            val estimatedKmh = dynoTracker.armedEstimatedSpeedMs * 3.6f
            if (estimatedKmh >= 30.0f && speedKmh >= 25.0f && location.accuracy <= 10.0f && isCalibrated) {
              val now = System.nanoTime()
              triggerOfficialRunStart(now, speedKmh)
            }
          } else if (dynoTracker.state == DynoRunState.MEDINDO) {
            // Update Maximum GPS speed (never replace with a lower speed)
            if (speedKmh > dynoTracker.maxGpsKmh) {
              dynoTracker.maxGpsKmh = speedKmh
              runMaximumGpsSpeedKmh = speedKmh
            }

            // Accumulate GPS x Calculated speed differences
            if (location.hasSpeed() && location.hasAccuracy() && location.accuracy <= 10f) {
              val currentCalcKmh = dynoTracker.velocityMs * 3.6f
              val diff = abs(currentCalcKmh - speedKmh)
              dynoTracker.diffSum += diff
              dynoTracker.diffCount++
              if (diff > dynoTracker.maxDiff) {
                dynoTracker.maxDiff = diff
                maximumSpeedDifferenceKmh = diff
              }
              if (dynoTracker.diffCount > 0) {
                averageSpeedDifferenceKmh = (dynoTracker.diffSum / dynoTracker.diffCount).toFloat()
              }
            }

            // CONDIÇÃO 2 — QUEDA CONFIRMADA PELO GPS
            // (velocidade GPS pelo menos 2 km/h abaixo da máxima E zFiltrado <= 0 por >= 600 ms)
            if (speedKmh <= (dynoTracker.maxGpsKmh - 2.0f) && dynoTracker.zFiltradoRun <= 0.0f) {
              val nowNs = System.nanoTime()
              if (dynoTracker.gpsSpeedDropStartNs == null) {
                dynoTracker.gpsSpeedDropStartNs = nowNs
              } else if (nowNs - dynoTracker.gpsSpeedDropStartNs!! >= 600_000_000L) {
                finalizeRun(FinishReason.GPS_DECELERATION)
              }
            } else {
              dynoTracker.gpsSpeedDropStartNs = null
            }
          }
        }

        override fun onProviderEnabled(provider: String) {
          if (provider == LocationManager.GPS_PROVIDER) {
            isGpsProviderEnabled = true
          }
        }

        override fun onProviderDisabled(provider: String) {
          if (provider == LocationManager.GPS_PROVIDER) {
            isGpsProviderEnabled = false
            hasGpsFix = false
          }
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
      }

      try {
        locationManager.requestLocationUpdates(
          LocationManager.GPS_PROVIDER,
          200L,
          0f,
          locationListener
        )
      } catch (e: SecurityException) {
        hasLocationPermission = false
      } catch (e: IllegalArgumentException) {
        // GPS provider might not exist
      }

      onDispose {
        try {
          locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {
          // Safe cleanup
        }
      }
    } else {
      onDispose { }
    }
  }

  // Sensor Listener: Accelerometer, Linear Acceleration & Gyroscope
  DisposableEffect(sensorManager, accelerometerSensor, linearAccelerationSensor, gyroscopeSensor) {
    if (sensorManager != null) {
      var previousTimestampNs = 0L
      val validIntervals = mutableListOf<Double>()
      var lastUiUpdateTimestampNs = 0L

      val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
          when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
              if (event.values.size >= 3) {
                rawX = event.values[0]
                rawY = event.values[1]
                rawZ = event.values[2]
              }
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
              if (event.values.size >= 3) {
                val currentLinearX = event.values[0]
                val currentLinearY = event.values[1]
                val currentLinearZ = event.values[2]

                linearX = currentLinearX
                linearY = currentLinearY
                linearZ = currentLinearZ

                val alpha = 0.18f
                filteredLinearX = filteredLinearX + alpha * (event.values[0] - filteredLinearX)
                filteredLinearY = filteredLinearY + alpha * (event.values[1] - filteredLinearY)
                filteredLinearZ = filteredLinearZ + alpha * (event.values[2] - filteredLinearZ)

                // Passagem: cálculo do eixo Z (filtro mediano 5 amostras + passa-baixa + zona morta)
                val rawLinearZ = event.values[2]
                val rawCorrigidoZ = (rawLinearZ - dynoTracker.offsetZ) * (if (dynoTracker.invertSignal) -1f else 1f)

                dynoTracker.zMedianBuffer.add(rawCorrigidoZ)
                if (dynoTracker.zMedianBuffer.size > 5) {
                  dynoTracker.zMedianBuffer.removeAt(0)
                }
                val sortedZ = dynoTracker.zMedianBuffer.sorted()
                val medianaZ = if (sortedZ.isNotEmpty()) sortedZ[sortedZ.size / 2] else 0f
                dynoTracker.zFiltradoRun += 0.18f * (medianaZ - dynoTracker.zFiltradoRun)
                val zRunFinal = if (abs(dynoTracker.zFiltradoRun) < 0.05f) 0f else dynoTracker.zFiltradoRun

                val nowNs = System.nanoTime()

                // ESTADO: AGUARDANDO_30 (Integração preliminar para armedEstimatedSpeedMs + Buffer circular)
                if (dynoTracker.state == DynoRunState.AGUARDANDO_30) {
                  if (dynoTracker.armedLastNanoTime != 0L) {
                    val dt = (nowNs - dynoTracker.armedLastNanoTime) / 1_000_000_000f
                    if (dt > 0f && dt <= 0.1f) {
                      dynoTracker.armedEstimatedSpeedMs = (dynoTracker.armedEstimatedSpeedMs + zRunFinal * dt).coerceAtLeast(0f)
                      armedEstimatedSpeedKmh = dynoTracker.armedEstimatedSpeedMs * 3.6f
                    }
                  }
                  dynoTracker.armedLastNanoTime = nowNs

                  val corrX = abs(event.values[0] - dynoTracker.offsetX)
                  val corrY = abs(event.values[1] - dynoTracker.offsetY)
                  val gyroMag = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)
                  val maxNormalVib = max(3.5f, dynoTracker.normalVib * 2.5f)
                  val maxNormalGyro = max(2.5f, dynoTracker.gyroDev * 3.0f)
                  val isSampleValid = !(corrX > maxNormalVib || corrY > maxNormalVib || gyroMag > maxNormalGyro)

                  // Adiciona ao buffer circular dos últimos 3s
                  dynoTracker.prepCircularBuffer.add(
                    PrepBufferSample(
                      timestampNs = nowNs,
                      linearX = currentLinearX,
                      linearY = currentLinearY,
                      linearZ = currentLinearZ,
                      zCorrigido = rawCorrigidoZ,
                      zFiltrado = zRunFinal,
                      gpsSpeedKmh = currentGpsSpeedKmh,
                      armedEstimatedSpeedKmh = dynoTracker.armedEstimatedSpeedMs * 3.6f,
                      gpsAccuracyMeters = gpsAccuracyM,
                      gyroMagnitude = gyroMag,
                      isValid = isSampleValid
                    )
                  )
                  if (dynoTracker.prepCircularBuffer.size > dynoTracker.maxPrepBufferSize) {
                    dynoTracker.prepCircularBuffer.removeAt(0)
                  }

                  // Verifica início oficial pelo cruzamento dos 30 km/h
                  val estKmh = dynoTracker.armedEstimatedSpeedMs * 3.6f
                  if (estKmh >= 30.0f && currentGpsSpeedKmh >= 25.0f && gpsAccuracyM <= 10.0f && isCalibrated) {
                    triggerOfficialRunStart(nowNs, currentGpsSpeedKmh)
                  }
                }
                // ESTADO: MEDINDO
                else if (dynoTracker.state == DynoRunState.MEDINDO) {
                  if (dynoTracker.lastSensorTimestampNs != 0L) {
                    val dt = (nowNs - dynoTracker.lastSensorTimestampNs) / 1_000_000_000f
                    if (dt > 0f && dt <= 0.1f) {
                      dynoTracker.velocityMs = (dynoTracker.velocityMs + zRunFinal * dt).coerceAtLeast(0f)
                      val currentCalcKmh = dynoTracker.velocityMs * 3.6f
                      runVelocityMs = dynoTracker.velocityMs

                      // Update Maximum Calculated Speed (never replace with a lower speed)
                      if (currentCalcKmh > dynoTracker.maxCalcSpeedKmh) {
                        dynoTracker.maxCalcSpeedKmh = currentCalcKmh
                        dynoTracker.maxCalcSpeedMs = dynoTracker.velocityMs
                        runMaximumCalculatedSpeedKmh = currentCalcKmh
                        runMaximumCalculatedSpeedMs = dynoTracker.velocityMs
                      }

                      dynoTracker.elapsedSec = (nowNs - dynoTracker.runStartTimeNs) / 1_000_000_000f
                      runElapsedSeconds = dynoTracker.elapsedSec
                    }
                  }
                  dynoTracker.lastSensorTimestampNs = nowNs

                  // Sample Quality Evaluation
                  val corrX = abs(event.values[0] - dynoTracker.offsetX)
                  val corrY = abs(event.values[1] - dynoTracker.offsetY)
                  val gyroMag = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)

                  val maxNormalVib = max(3.5f, dynoTracker.normalVib * 2.5f)
                  val maxNormalGyro = max(2.5f, dynoTracker.gyroDev * 3.0f)

                  dynoTracker.total++
                  totalSamples = dynoTracker.total
                  val isSampleValid = !(corrX > maxNormalVib || corrY > maxNormalVib || gyroMag > maxNormalGyro)
                  if (!isSampleValid) {
                    dynoTracker.rejected++
                    rejectedSamples = dynoTracker.rejected
                  }

                  // Gravação da Série Temporal (RunSample) a ~20 Hz (intervalo de ~50ms, máximo 500 amostras)
                  if (nowNs - dynoTracker.lastSampleRecordedNs >= 50_000_000L && dynoTracker.recordedSamples.size < 500) {
                    dynoTracker.lastSampleRecordedNs = nowNs
                    val elapsedMs = ((nowNs - dynoTracker.runStartTimeNs) / 1_000_000L).coerceAtLeast(0L)
                    val currentCalcKmh = dynoTracker.velocityMs * 3.6f
                    val diff = abs(currentCalcKmh - currentGpsSpeedKmh)

                    val rejReason = if (!isSampleValid) {
                      when {
                        corrX > maxNormalVib -> "Vibração lateral X excessiva"
                        corrY > maxNormalVib -> "Vibração vertical Y excessiva"
                        else -> "Giroscópio elevado"
                      }
                    } else null

                    val samplePoint = RunSample(
                      elapsedTimeMs = elapsedMs,
                      filteredAccelerationZ = zRunFinal,
                      correctedAccelerationZ = rawCorrigidoZ,
                      gpsSpeedKmh = currentGpsSpeedKmh,
                      calculatedSpeedKmh = currentCalcKmh,
                      speedDifferenceKmh = diff,
                      gpsAccuracyMeters = gpsAccuracyM,
                      gyroMagnitude = gyroMag,
                      isValid = isSampleValid,
                      rejectionReason = rejReason
                    )
                    dynoTracker.recordedSamples.add(samplePoint)
                  }

                  // CONDIÇÃO 1 — DESACELERAÇÃO PELO EIXO Z
                  // (zFiltrado < -0.15 m/s² mantido por pelo menos 600 ms)
                  if (zRunFinal < -0.15f) {
                    if (dynoTracker.decelerationStartNs == null) {
                      dynoTracker.decelerationStartNs = nowNs
                    } else if (nowNs - dynoTracker.decelerationStartNs!! >= 600_000_000L) {
                      finalizeRun(FinishReason.SENSOR_DECELERATION)
                    }
                  } else {
                    dynoTracker.decelerationStartNs = null
                  }

                  // CONDIÇÃO 4 — TEMPO EXCESSIVO (> 25 segundos)
                  if (dynoTracker.elapsedSec > 25.0f) {
                    finalizeRun(FinishReason.TIMEOUT)
                  }
                }

                // CALIBRAÇÃO (Coleta de ~150 amostras em ~3s com motor funcionando)
                if (calibCollector.isCollecting) {
                  val currentCount = calibCollector.count
                  if (currentCount > 10) {
                    val partialAvgX = (calibCollector.sumX / currentCount).toFloat()
                    val partialAvgY = (calibCollector.sumY / currentCount).toFloat()
                    val partialAvgZ = (calibCollector.sumZ / currentCount).toFloat()

                    val gyroMag = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)

                    // Se houver movimento brusco do aparelho (> 2.0 m/s² ou giro > 1.8 rad/s), cancela
                    if (abs(currentLinearX - partialAvgX) > 2.0f ||
                        abs(currentLinearY - partialAvgY) > 2.0f ||
                        abs(currentLinearZ - partialAvgZ) > 2.0f ||
                        gyroMag > 1.8f) {
                      calibCollector.reset()
                      isCalibrating = false
                      calibrationStatus = "Calibração cancelada: aparelho se moveu"
                    }
                  }

                  if (calibCollector.isCollecting) {
                    calibCollector.sumX += currentLinearX
                    calibCollector.sumY += currentLinearY
                    calibCollector.sumZ += currentLinearZ

                    val currentAvgX = (calibCollector.sumX / (currentCount + 1)).toFloat()
                    val currentAvgY = (calibCollector.sumY / (currentCount + 1)).toFloat()
                    val currentAvgZ = (calibCollector.sumZ / (currentCount + 1)).toFloat()

                    calibCollector.sumDevX += abs(currentLinearX - currentAvgX)
                    calibCollector.sumDevY += abs(currentLinearY - currentAvgY)
                    calibCollector.sumDevZ += abs(currentLinearZ - currentAvgZ)

                    val gMag = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)
                    calibCollector.sumGyroMag += gMag

                    calibCollector.count++
                    val newCount = calibCollector.count
                    val progressPercent = (newCount * 100) / 150
                    calibrationStatus = "Calibrando $progressPercent% (mantenha parado)"

                    if (newCount >= 150) {
                      val avgX = (calibCollector.sumX / 150.0).toFloat()
                      val avgY = (calibCollector.sumY / 150.0).toFloat()
                      val avgZ = (calibCollector.sumZ / 150.0).toFloat()

                      val normVib = ((calibCollector.sumDevX + calibCollector.sumDevY + calibCollector.sumDevZ) / 450.0).toFloat()
                      val avgGyroDev = (calibCollector.sumGyroMag / 150.0).toFloat()

                      offsetX = avgX
                      offsetY = avgY
                      offsetZ = avgZ
                      calibratedNormalVibration = normVib
                      calibratedGyroDeviation = avgGyroDev
                      isCalibrated = true
                      calibrationStatus = "Calibração concluída"
                      isCalibrating = false
                      calibCollector.reset()

                      prefs.edit()
                        .putFloat("offset_x", avgX)
                        .putFloat("offset_y", avgY)
                        .putFloat("offset_z", avgZ)
                        .putFloat("calibrated_vibration", normVib)
                        .putFloat("calibrated_gyro", avgGyroDev)
                        .putBoolean("is_calibrated", true)
                        .apply()
                    }
                  }
                }
              }

              val currentTimestampNs = event.timestamp
              if (previousTimestampNs != 0L) {
                val deltaNs = currentTimestampNs - previousTimestampNs
                if (deltaNs > 0 && deltaNs <= 1_000_000_000L) {
                  val intervaloSegundos = deltaNs / 1_000_000_000.0
                  validIntervals.add(intervaloSegundos)
                  if (validIntervals.size > 20) {
                    validIntervals.removeAt(0)
                  }

                  val mediaDosIntervalos = validIntervals.average()
                  if (mediaDosIntervalos > 0.0) {
                    val frequenciaMedia = 1.0 / mediaDosIntervalos
                    if (currentTimestampNs - lastUiUpdateTimestampNs >= 200_000_000L) {
                      samplingFrequencyHz = frequenciaMedia
                      lastUiUpdateTimestampNs = currentTimestampNs
                    }
                  }
                }
              }
              previousTimestampNs = currentTimestampNs
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

      if (accelerometerSensor != null) {
        sensorManager.registerListener(listener, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME)
      }
      if (linearAccelerationSensor != null) {
        sensorManager.registerListener(listener, linearAccelerationSensor, SensorManager.SENSOR_DELAY_GAME)
      }
      if (gyroscopeSensor != null) {
        sensorManager.registerListener(listener, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME)
      }

      onDispose {
        sensorManager.unregisterListener(listener)
      }
    } else {
      onDispose { }
    }
  }

  val (gpsSpeedText, gpsAccuracyText) = when {
    !hasLocationPermission -> "0.0 km/h" to "permissão negada"
    !isGpsProviderEnabled -> "0.0 km/h" to "GPS desligado"
    !hasGpsFix -> "0.0 km/h" to "aguardando GPS"
    else -> String.format(Locale.US, "%.1f km/h", currentGpsSpeedKmh) to String.format(Locale.US, "%.1f m", gpsAccuracyM)
  }

  val rejectionRatio = if (totalSamples > 0) rejectedSamples.toFloat() / totalSamples.toFloat() else 0f
  val runQuality = when {
    totalSamples == 0 -> "-"
    runFinishReason == FinishReason.TIMEOUT || runElapsedSeconds > 25f || runElapsedSeconds < 2f ||
      gpsAccuracyM > 10f || rejectionRatio > 0.25f || runMaximumGpsSpeedKmh < 40f -> "INVÁLIDA"
    rejectionRatio <= 0.10f && gpsAccuracyM <= 6f && runElapsedSeconds in 2f..20f -> "BOA"
    rejectionRatio <= 0.25f && gpsAccuracyM <= 10f && runElapsedSeconds <= 25f -> "REGULAR"
    else -> "INVÁLIDA"
  }

  val isPassActive = runState == DynoRunState.AGUARDANDO_30 || runState == DynoRunState.MEDINDO
  val isLongitudinalMeasuring = isIntegrating || isPassActive

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("sensor_screen"),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      Column {
        TopAppBar(
          title = {
            Text(
              text = stringResource(R.string.title_sensors_test),
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                fontSize = 20.sp,
              ),
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.testTag("sensor_screen_title")
            )
          },
          navigationIcon = {
            IconButton(
              onClick = onNavigateBack,
              modifier = Modifier.testTag("top_bar_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.btn_back),
                tint = MaterialTheme.colorScheme.onSurface,
              )
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
          ),
        )
        HorizontalDivider(
          thickness = 1.dp,
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 20.dp)
          .widthIn(max = 480.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // 1. PASSAGEM DINAMOMÉTRICA (Top Hero Card)
        DynoRunCard(
          runState = runState,
          isCalibrated = isCalibrated,
          currentGpsSpeedKmh = currentGpsSpeedKmh,
          armedEstimatedSpeedKmh = armedEstimatedSpeedKmh,
          runStartCalculatedSpeedKmh = runStartCalculatedSpeedKmh,
          runStartGpsSpeedKmh = runStartGpsSpeedKmh,
          runMaximumGpsSpeedKmh = runMaximumGpsSpeedKmh,
          runMaximumCalculatedSpeedKmh = runMaximumCalculatedSpeedKmh,
          runFinalGpsSpeedKmh = runFinalGpsSpeedKmh,
          runFinalCalculatedSpeedKmh = runFinalCalculatedSpeedKmh,
          runVelocityKmh = runVelocityKmh,
          runVelocityMs = runVelocityMs,
          runElapsedSeconds = runElapsedSeconds,
          runQuality = runQuality,
          finishReason = runFinishReason,
          averageSpeedDifferenceKmh = averageSpeedDifferenceKmh,
          maximumSpeedDifferenceKmh = maximumSpeedDifferenceKmh,
          recordedSamplesCount = dynoTracker.recordedSamples.size,
          onPrepare = {
            if (isCalibrated) {
              dynoTracker.reset()
              dynoTracker.state = DynoRunState.AGUARDANDO_30
              dynoTracker.armedEstimatedSpeedMs = (currentGpsSpeedKmh / 3.6f).coerceAtLeast(0f)
              dynoTracker.armedLastNanoTime = System.nanoTime()
              armedEstimatedSpeedKmh = currentGpsSpeedKmh
              runElapsedSeconds = 0f
              runVelocityMs = 0f
              runStartCalculatedSpeedKmh = 30.0f
              runStartGpsSpeedKmh = 0f
              runMaximumGpsSpeedKmh = 0f
              runMaximumCalculatedSpeedKmh = 30.0f
              runFinalGpsSpeedKmh = 0f
              runFinalCalculatedSpeedKmh = 0f
              rejectedSamples = 0
              totalSamples = 0
              resultSaved = false
              runFinishReason = null
              runState = DynoRunState.AGUARDANDO_30
            }
          },
          onCancel = {
            dynoTracker.reset()
            runState = DynoRunState.PARADO
          },
          onStop = {
            // CONDIÇÃO 3: Finalização manual pelo usuário
            finalizeRun(FinishReason.USER_STOP)
          },
          onRepeat = {
            // Repetir teste: limpa temporários, preserva resultado salvo, veículo e calibração
            dynoTracker.reset()
            dynoTracker.state = DynoRunState.AGUARDANDO_30
            dynoTracker.armedEstimatedSpeedMs = (currentGpsSpeedKmh / 3.6f).coerceAtLeast(0f)
            dynoTracker.armedLastNanoTime = System.nanoTime()
            armedEstimatedSpeedKmh = currentGpsSpeedKmh
            runElapsedSeconds = 0f
            runVelocityMs = 0f
            runStartCalculatedSpeedKmh = 30.0f
            runStartGpsSpeedKmh = 0f
            runMaximumGpsSpeedKmh = 0f
            runMaximumCalculatedSpeedKmh = 30.0f
            runFinalGpsSpeedKmh = 0f
            runFinalCalculatedSpeedKmh = 0f
            rejectedSamples = 0
            totalSamples = 0
            resultSaved = false
            runFinishReason = null
            runState = DynoRunState.AGUARDANDO_30
          },
          onReset = {
            // Zerar: retorna a PARADO sem apagar histórico ou calibração
            dynoTracker.reset()
            runElapsedSeconds = 0f
            runVelocityMs = 0f
            armedEstimatedSpeedKmh = 0f
            runStartCalculatedSpeedKmh = 30.0f
            runStartGpsSpeedKmh = 0f
            runMaximumGpsSpeedKmh = 0f
            runMaximumCalculatedSpeedKmh = 30.0f
            runFinalGpsSpeedKmh = 0f
            runFinalCalculatedSpeedKmh = 0f
            rejectedSamples = 0
            totalSamples = 0
            resultSaved = false
            runFinishReason = null
            runState = DynoRunState.PARADO
          },
          onViewResults = onNavigateToResults
        )

        // 2. CALIBRAÇÃO E EIXO LONGITUDINAL (Com celular preso e motor funcionando)
        LongitudinalAxisCard(
          selectedAxis = selectedAxis,
          onAxisSelected = { axis ->
            selectedAxis = axis
            prefs.edit().putString("selected_longitudinal_axis", axis).apply()
          },
          invertSignal = invertSignal,
          onInvertSignalChanged = { inverted ->
            invertSignal = inverted
            prefs.edit().putBoolean("invert_longitudinal_signal", inverted).apply()
          },
          correctedAcceleration = aceleracaoCorrigida,
          filteredAcceleration = aceleracaoFiltrada,
          direction = direcao,
          currentOffset = currentOffset,
          calibrationStatus = calibrationStatus,
          isCalibrating = isCalibrating,
          isMeasuring = isLongitudinalMeasuring,
          normalVibration = calibratedNormalVibration,
          onCalibrateZero = {
            scope.launch {
              isCalibrating = true
              calibrationStatus = "Mantenha o veículo parado e motor funcionando..."
              calibCollector.reset()
              delay(600L)
              calibCollector.isCollecting = true
            }
          }
        )

        // 3. VELOCIDADE EXPERIMENTAL (Integração manual)
        ExperimentalVelocityCard(
          isIntegrating = isIntegrating,
          isCalibrated = isCalibrated,
          elapsedSeconds = integrationElapsedSeconds,
          velocityMs = integratedVelocityMs,
          velocityKmh = integratedVelocityKmh,
          onStart = {
            if (isCalibrated && !isIntegrating && !isPassActive) {
              integratedVelocityMs = 0f
              integrationElapsedSeconds = 0f
              isIntegrating = true
            }
          },
          onStop = {
            isIntegrating = false
          },
          onReset = {
            isIntegrating = false
            integratedVelocityMs = 0f
            integrationElapsedSeconds = 0f
          }
        )

        // 4. ACELERÔMETRO BRUTO
        SensorCard(
          title = "ACELERÔMETRO BRUTO",
          icon = Icons.Outlined.Sensors,
          isAvailable = isRawAvailable,
          unavailableMessage = stringResource(R.string.sensor_unavailable),
          items = listOf(
            "X" to String.format(Locale.US, "%.3f m/s²", rawX),
            "Y" to String.format(Locale.US, "%.3f m/s²", rawY),
            "Z" to String.format(Locale.US, "%.3f m/s²", rawZ)
          ),
          testTag = "raw_accelerometer_card"
        )

        // 5. ACELERAÇÃO LINEAR
        SensorCard(
          title = "ACELERAÇÃO LINEAR",
          icon = Icons.Outlined.Speed,
          isAvailable = isLinearAvailable,
          unavailableMessage = stringResource(R.string.linear_sensor_unavailable),
          items = listOf(
            "X" to String.format(Locale.US, "%.3f m/s²", linearX),
            "Y" to String.format(Locale.US, "%.3f m/s²", linearY),
            "Z" to String.format(Locale.US, "%.3f m/s²", linearZ)
          ),
          testTag = "linear_acceleration_card"
        )

        // 6. GIROSCÓPIO
        SensorCard(
          title = "GIROSCÓPIO",
          icon = Icons.Outlined.Explore,
          isAvailable = isGyroAvailable,
          unavailableMessage = stringResource(R.string.gyro_sensor_unavailable),
          items = listOf(
            "X" to String.format(Locale.US, "%.3f rad/s", gyroX),
            "Y" to String.format(Locale.US, "%.3f rad/s", gyroY),
            "Z" to String.format(Locale.US, "%.3f rad/s", gyroZ)
          ),
          testTag = "gyroscope_card"
        )

        // 7. GPS (Claramente identificado como GPS agora)
        SensorDataCard(
          title = "GPS AGORA (DIAGNÓSTICO)",
          icon = Icons.Outlined.LocationOn,
          items = listOf(
            "GPS agora" to gpsSpeedText,
            "Precisão" to gpsAccuracyText
          )
        )

        // 8. AMOSTRAGEM
        SensorDataCard(
          title = "AMOSTRAGEM",
          icon = Icons.Outlined.Timer,
          items = listOf(
            "Frequência" to String.format(Locale.US, "%.1f Hz", samplingFrequencyHz)
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Botão Voltar (Pill Button)
        Button(
          onClick = onNavigateBack,
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("back_button"),
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
          contentPadding = PaddingValues(horizontal = 24.dp),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.btn_back).uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Medium,
              letterSpacing = 1.sp,
            )
          )
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun SensorCard(
  title: String,
  icon: ImageVector,
  isAvailable: Boolean,
  unavailableMessage: String,
  items: List<Pair<String, String>>,
  modifier: Modifier = Modifier,
  testTag: String = ""
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            fontSize = 13.sp,
          ),
          color = MaterialTheme.colorScheme.primary,
        )
      }

      HorizontalDivider(
        thickness = 0.8.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
      )

      if (!isAvailable) {
        Text(
          text = unavailableMessage,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Medium
          ),
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(vertical = 4.dp)
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          items.forEach { (label, value) ->
            SensorValueRow(label = label, value = value)
          }
        }
      }
    }
  }
}

@Composable
private fun SensorValueRow(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = "$label:",
      style = MaterialTheme.typography.bodyMedium.copy(
        fontWeight = FontWeight.Medium
      ),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium.copy(
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace,
      ),
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun SensorDataCard(
  title: String,
  icon: ImageVector,
  items: List<Pair<String, String>>,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            fontSize = 13.sp,
          ),
          color = MaterialTheme.colorScheme.primary,
        )
      }

      HorizontalDivider(
        thickness = 0.8.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
      )

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (label, value) ->
          SensorValueRow(label = label, value = value)
        }
      }
    }
  }
}

@Composable
private fun LongitudinalAxisCard(
  selectedAxis: String,
  onAxisSelected: (String) -> Unit,
  invertSignal: Boolean,
  onInvertSignalChanged: (Boolean) -> Unit,
  correctedAcceleration: Float,
  filteredAcceleration: Float,
  direction: String,
  currentOffset: Float,
  calibrationStatus: String,
  isCalibrating: Boolean,
  isMeasuring: Boolean,
  normalVibration: Float,
  onCalibrateZero: () -> Unit,
  modifier: Modifier = Modifier,
  testTag: String = "longitudinal_axis_card"
) {
  val isInteractionDisabled = isCalibrating || isMeasuring
  Card(
    modifier = modifier
      .fillMaxWidth()
      .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.Straighten,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Text(
          text = "CALIBRAÇÃO E EIXO LONGITUDINAL",
          style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            fontSize = 13.sp,
          ),
          color = MaterialTheme.colorScheme.primary,
        )
      }

      HorizontalDivider(
        thickness = 0.8.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
      )

      // Instructions Notice for In-Car Mounted Calibration
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = "Deixe o celular preso no suporte, mantenha o veículo parado e o motor funcionando.",
            style = MaterialTheme.typography.bodySmall.copy(
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      // Axis Selection Buttons: X, Y, Z
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf("X", "Y", "Z").forEach { axis ->
          val isSelected = selectedAxis == axis
          Button(
            onClick = { onAxisSelected(axis) },
            enabled = !isInteractionDisabled,
            modifier = Modifier
              .weight(1f)
              .height(44.dp)
              .testTag("axis_button_${axis.lowercase()}"),
            shape = RoundedCornerShape(12.dp),
            colors = if (isSelected) {
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
              )
            } else {
              ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
            contentPadding = PaddingValues(0.dp),
          ) {
            Text(
              text = axis,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 15.sp,
              )
            )
          }
        }
      }

      // Invert Signal Switch
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Inverter sinal",
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Medium
          ),
          color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
          checked = invertSignal,
          onCheckedChange = onInvertSignalChanged,
          enabled = !isInteractionDisabled,
          modifier = Modifier.testTag("invert_signal_switch"),
          colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
          )
        )
      }

      HorizontalDivider(
        thickness = 0.8.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
      )

      // Calibrate Zero Section
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onCalibrateZero,
          enabled = !isInteractionDisabled,
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("calibrate_zero_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
            disabledContentColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f)
          )
        ) {
          Text(
            text = "CALIBRAR ZERO NO SUPORTE",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              fontSize = 14.sp
            )
          )
        }

        // Status text
        val statusColor = when {
          calibrationStatus.startsWith("Calibração cancelada") -> MaterialTheme.colorScheme.error
          calibrationStatus == "Calibração concluída" -> MaterialTheme.colorScheme.primary
          isCalibrating -> MaterialTheme.colorScheme.tertiary
          else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        Text(
          text = calibrationStatus,
          style = MaterialTheme.typography.bodySmall.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
          ),
          color = statusColor,
          modifier = Modifier.testTag("calibration_status_text")
        )
      }

      HorizontalDivider(
        thickness = 0.8.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
      )

      // Key-Value Items
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        SensorValueRow(label = "Eixo selecionado", value = selectedAxis)
        SensorValueRow(
          label = "Offset aplicado",
          value = String.format(Locale.US, "%.3f m/s²", currentOffset)
        )
        SensorValueRow(
          label = "Vibração basal normal",
          value = String.format(Locale.US, "±%.3f m/s²", normalVibration)
        )
        SensorValueRow(
          label = "Aceleração corrigida",
          value = String.format(Locale.US, "%.3f m/s²", correctedAcceleration)
        )
        SensorValueRow(
          label = "Aceleração filtrada",
          value = String.format(Locale.US, "%.3f m/s²", filteredAcceleration)
        )
        SensorValueRow(label = "Direção", value = direction)
      }
    }
  }
}

@Composable
private fun ExperimentalVelocityCard(
  isIntegrating: Boolean,
  isCalibrated: Boolean,
  elapsedSeconds: Float,
  velocityMs: Float,
  velocityKmh: Float,
  onStart: () -> Unit,
  onStop: () -> Unit,
  onReset: () -> Unit,
  modifier: Modifier = Modifier,
  testTag: String = "experimental_velocity_card"
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.Speed,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Text(
          text = "VELOCIDADE EXPERIMENTAL (MANUAL)",
          style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            fontSize = 13.sp,
          ),
          color = MaterialTheme.colorScheme.primary,
        )
      }

      HorizontalDivider(
        thickness = 0.8.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onStart,
          enabled = isCalibrated && !isIntegrating,
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("btn_start_integration"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
          ),
          contentPadding = PaddingValues(0.dp)
        ) {
          Text(
            text = "INICIAR",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          )
        }

        Button(
          onClick = onStop,
          enabled = isIntegrating,
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("btn_stop_integration"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
          ),
          contentPadding = PaddingValues(0.dp)
        ) {
          Text(
            text = "PARAR",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          )
        }

        Button(
          onClick = onReset,
          enabled = !isIntegrating,
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("btn_reset_integration"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
          ),
          contentPadding = PaddingValues(0.dp)
        ) {
          Text(
            text = "ZERAR",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          )
        }
      }

      HorizontalDivider(
        thickness = 0.8.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
      )

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SensorValueRow(
          label = "Estado",
          value = if (isIntegrating) "medindo" else "parado"
        )
        SensorValueRow(
          label = "Tempo",
          value = String.format(Locale.US, "%.2f s", elapsedSeconds)
        )
        SensorValueRow(
          label = "Velocidade integrada",
          value = String.format(Locale.US, "%.2f m/s", velocityMs)
        )
        SensorValueRow(
          label = "Velocidade integrada",
          value = String.format(Locale.US, "%.1f km/h", velocityKmh)
        )
      }
    }
  }
}

@Composable
private fun DynoRunCard(
  runState: DynoRunState,
  isCalibrated: Boolean,
  currentGpsSpeedKmh: Float,
  armedEstimatedSpeedKmh: Float,
  runStartCalculatedSpeedKmh: Float,
  runStartGpsSpeedKmh: Float,
  runMaximumGpsSpeedKmh: Float,
  runMaximumCalculatedSpeedKmh: Float,
  runFinalGpsSpeedKmh: Float,
  runFinalCalculatedSpeedKmh: Float,
  runVelocityKmh: Float,
  runVelocityMs: Float,
  runElapsedSeconds: Float,
  runQuality: String,
  finishReason: FinishReason?,
  averageSpeedDifferenceKmh: Float,
  maximumSpeedDifferenceKmh: Float,
  recordedSamplesCount: Int,
  onPrepare: () -> Unit,
  onCancel: () -> Unit,
  onStop: () -> Unit,
  onRepeat: () -> Unit,
  onReset: () -> Unit,
  onViewResults: () -> Unit,
  modifier: Modifier = Modifier,
  testTag: String = "dyno_run_card"
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (runState == DynoRunState.FINALIZADO)
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
      else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ),
    border = BorderStroke(
      if (runState == DynoRunState.FINALIZADO) 1.5.dp else 1.dp,
      if (runState == DynoRunState.FINALIZADO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = when (runState) {
            DynoRunState.FINALIZADO -> Icons.Default.CheckCircle
            DynoRunState.MEDINDO -> Icons.Default.Speed
            else -> Icons.Outlined.Speed
          },
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(22.dp)
        )
        Text(
          text = when (runState) {
            DynoRunState.FINALIZADO -> "PASSAGEM FINALIZADA"
            DynoRunState.MEDINDO -> "TESTE EM ANDAMENTO"
            DynoRunState.AGUARDANDO_30 -> "TESTE PREPARADO"
            DynoRunState.PARADO -> "PASSAGEM DINAMOMÉTRICA"
          },
          style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            fontSize = 14.sp,
          ),
          color = MaterialTheme.colorScheme.primary,
        )
      }

      HorizontalDivider(
        thickness = 0.8.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
      )

      // Active Measurement States vs Finalized Summary
      if (runState == DynoRunState.FINALIZADO) {
        // SUMMARY OF FROZEN RESULTS (0.15.0)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          SensorValueRow(
            label = "Início calculado",
            value = String.format(Locale.US, "%.1f km/h", runStartCalculatedSpeedKmh)
          )
          SensorValueRow(
            label = "Primeira leitura GPS",
            value = String.format(Locale.US, "%.1f km/h (dif: %.1f km/h)", runStartGpsSpeedKmh, abs(runStartGpsSpeedKmh - runStartCalculatedSpeedKmh))
          )
          SensorValueRow(
            label = "Velocidade máxima GPS",
            value = String.format(Locale.US, "%.1f km/h", runMaximumGpsSpeedKmh)
          )
          SensorValueRow(
            label = "Velocidade máxima calculada",
            value = String.format(Locale.US, "%.1f km/h", runMaximumCalculatedSpeedKmh)
          )
          SensorValueRow(
            label = "Diferença média",
            value = String.format(Locale.US, "±%.1f km/h", averageSpeedDifferenceKmh)
          )
          SensorValueRow(
            label = "Maior diferença",
            value = String.format(Locale.US, "%.1f km/h", maximumSpeedDifferenceKmh)
          )
          SensorValueRow(
            label = "Tempo da passagem",
            value = String.format(Locale.US, "%.2f s", runElapsedSeconds)
          )
          SensorValueRow(
            label = "Pontos gravados",
            value = "$recordedSamplesCount amostras (~20 Hz)"
          )
          SensorValueRow(
            label = "Qualidade da leitura",
            value = runQuality
          )
          if (finishReason != null) {
            SensorValueRow(
              label = "Motivo da finalização",
              value = finishReason.displayName
            )
          }
        }

        HorizontalDivider(
          thickness = 0.8.dp,
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )

        // Action Buttons for FINALIZADO state: [ VER RESULTADOS ], [ REPETIR TESTE ], [ ZERAR ]
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = onViewResults,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("btn_view_results"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            )
          ) {
            Icon(Icons.Outlined.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "VER HISTÓRICO DE RESULTADOS",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = onRepeat,
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("btn_repeat_run"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
              )
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("REPETIR TESTE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }

            OutlinedButton(
              onClick = onReset,
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("btn_reset_run"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("ZERAR", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
          }
        }
      } else {
        // Controls and Clean Mode display based on state
        when (runState) {
          DynoRunState.PARADO -> {
            Button(
              onClick = onPrepare,
              enabled = isCalibrated,
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_prepare_run"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
              )
            ) {
              Text(
                text = "PREPARAR TESTE",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp,
                  fontSize = 14.sp
                )
              )
            }

            HorizontalDivider(
              thickness = 0.8.dp,
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              SensorValueRow(label = "Estado", value = "PARADO")
              SensorValueRow(label = "Velocidade GPS atual", value = String.format(Locale.US, "%.1f km/h", currentGpsSpeedKmh))
            }
          }
          DynoRunState.AGUARDANDO_30 -> {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Icon(
                      Icons.Default.Speed,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp)
                    )
                    Text(
                      text = "Aguardando 30 km/h",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  }

                  Text(
                    text = "O teste começará automaticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )

                  HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 4.dp)
                  )

                  SensorValueRow(
                    label = "Velocidade GPS",
                    value = String.format(Locale.US, "%.1f km/h", currentGpsSpeedKmh)
                  )
                  SensorValueRow(
                    label = "Velocidade estimada",
                    value = String.format(Locale.US, "%.1f km/h", armedEstimatedSpeedKmh)
                  )
                }
              }

              Button(
                onClick = onCancel,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("btn_cancel_run"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.error,
                  contentColor = MaterialTheme.colorScheme.onError
                )
              ) {
                Text(
                  text = "CANCELAR",
                  style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    fontSize = 14.sp
                  )
                )
              }
            }
          }
          DynoRunState.MEDINDO -> {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Button(
                onClick = onStop,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("btn_stop_run"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.error,
                  contentColor = MaterialTheme.colorScheme.onError
                )
              ) {
                Text(
                  text = "PARAR",
                  style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    fontSize = 14.sp
                  )
                )
              }

              HorizontalDivider(
                thickness = 0.8.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
              )

              // Clean display during active run (No X, Y or gyro here)
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SensorValueRow(
                  label = "Velocidade calculada",
                  value = String.format(Locale.US, "%.1f km/h", runVelocityKmh)
                )
                SensorValueRow(
                  label = "Velocidade GPS",
                  value = String.format(Locale.US, "%.1f km/h", currentGpsSpeedKmh)
                )
                SensorValueRow(
                  label = "Tempo",
                  value = String.format(Locale.US, "%.2f s", runElapsedSeconds)
                )
                SensorValueRow(
                  label = "Amostras gravadas",
                  value = "$recordedSamplesCount / 500"
                )
              }
            }
          }
          DynoRunState.FINALIZADO -> {
            // Handled above in if (runState == DynoRunState.FINALIZADO)
          }
        }
      }

      HorizontalDivider(
        thickness = 0.8.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
      )

      // Safety Notice
      Text(
        text = "Mantenha o celular fixo no suporte durante todo o teste. O teste finaliza automaticamente na desaceleração.",
        style = MaterialTheme.typography.bodySmall.copy(
          fontSize = 12.sp,
          lineHeight = 16.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
      )
    }
  }
}
