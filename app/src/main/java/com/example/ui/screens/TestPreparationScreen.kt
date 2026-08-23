package com.example.ui.screens

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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.DynoRunState
import com.example.PrepBufferSample
import com.example.data.RunResultRepository
import com.example.data.VehicleDatabase
import com.example.model.FinishReason
import com.example.model.RunResult
import com.example.model.RunSample
import com.example.model.VehicleCalculations
import com.example.model.VehicleProfile
import com.example.ui.components.DynoBadgeStatus
import com.example.ui.components.DynoCard
import com.example.ui.components.DynoDangerButton
import com.example.ui.components.DynoPrimaryButton
import com.example.ui.components.DynoSecondaryButton
import com.example.ui.components.DynoSpeedometer
import com.example.ui.components.DynoStatusBadge
import com.example.ui.theme.DynoBlueLight
import com.example.ui.theme.DynoBluePrimary
import com.example.ui.theme.DynoDivider
import com.example.ui.theme.DynoErrorRed
import com.example.ui.theme.DynoPowerCyan
import com.example.ui.theme.DynoSuccessGreen
import com.example.ui.theme.DynoSurface
import com.example.ui.theme.DynoSurfaceContainer
import com.example.ui.theme.DynoSurfaceElevated
import com.example.ui.theme.DynoTextMuted
import com.example.ui.theme.DynoTextPrimary
import com.example.ui.theme.DynoTextSecondary
import com.example.ui.theme.DynoTorqueOrange
import com.example.ui.theme.DynoWarningYellow
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestPreparationScreen(
  vehicle: VehicleProfile,
  onNavigateToResults: () -> Unit,
  onNavigateToHome: () -> Unit,
  onSwitchVehicle: () -> Unit,
  onEditVehicle: () -> Unit,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val prefs = remember(context) {
    context.getSharedPreferences("dyno_lite_prefs", Context.MODE_PRIVATE)
  }

  val sensorManager = remember {
    context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
  }
  val locationManager = remember {
    context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
  }
  val runResultRepository = remember { RunResultRepository(context) }

  val accelerometerSensor = remember(sensorManager) {
    sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
  }
  val linearAccelerationSensor = remember(sensorManager) {
    sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
  }
  val gyroscopeSensor = remember(sensorManager) {
    sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
  }

  // Raw and Filtered Sensors
  var linearX by remember { mutableFloatStateOf(0f) }
  var linearY by remember { mutableFloatStateOf(0f) }
  var linearZ by remember { mutableFloatStateOf(0f) }
  var gyroX by remember { mutableFloatStateOf(0f) }
  var gyroY by remember { mutableFloatStateOf(0f) }
  var gyroZ by remember { mutableFloatStateOf(0f) }

  // Calibration Offsets & Vehicle Vibration Baseline
  var isCalibrated by remember {
    mutableStateOf(prefs.getBoolean("is_calibrated", false))
  }
  var offsetX by remember { mutableFloatStateOf(prefs.getFloat("offset_x", 0.0f)) }
  var offsetY by remember { mutableFloatStateOf(prefs.getFloat("offset_y", 0.0f)) }
  var offsetZ by remember { mutableFloatStateOf(prefs.getFloat("offset_z", 0.0f)) }
  var calibratedNormalVibration by remember { mutableFloatStateOf(prefs.getFloat("calibrated_vibration", 0.12f)) }
  var calibratedGyroDeviation by remember { mutableFloatStateOf(prefs.getFloat("calibrated_gyro", 0.08f)) }
  var invertSignal by remember { mutableStateOf(prefs.getBoolean("invert_longitudinal_signal", false)) }

  var isCalibrating by remember { mutableStateOf(false) }
  var calibProgressPercent by remember { mutableIntStateOf(0) }
  var calibrationStatusText by remember { mutableStateOf(if (isCalibrated) "Calibração concluída" else "Não calibrado") }

  // Calibration Collector: Collects ~150 samples (~3s) with vehicle stopped & engine running
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

  // Automatic Start Trigger Speed (40, 50 or 60 km/h)
  var startSpeedTriggerKmh by remember {
    mutableFloatStateOf(prefs.getFloat("start_speed_trigger_kmh", 40.0f))
  }

  // Dyno Run State Machine
  var runState by remember { mutableStateOf(DynoRunState.PARADO) }
  var currentGpsSpeedKmh by remember { mutableFloatStateOf(0f) }
  var armedEstimatedSpeedKmh by remember { mutableFloatStateOf(0f) }
  var runElapsedSeconds by remember { mutableFloatStateOf(0f) }
  var runVelocityMs by remember { mutableFloatStateOf(0f) }
  var resultSaved by remember { mutableStateOf(false) }
  var runFinishReason by remember { mutableStateOf<FinishReason?>(null) }
  var showCancelConfirmDialog by remember { mutableStateOf(false) }

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

      var startTriggerSpeedKmh: Float = 40.0f

      var armedEstimatedSpeedMs: Float = 0f
      var armedLastNanoTime: Long = 0L
      val prepCircularBuffer = mutableListOf<PrepBufferSample>()
      val maxPrepBufferSize = 180

      var runStartTimeNs: Long = 0L
      var runEndTimeNs: Long = 0L
      var lastSensorTimestampNs: Long = 0L
      var lastSampleRecordedNs: Long = 0L
      var decelerationStartNs: Long? = null
      var gpsSpeedDropStartNs: Long? = null

      val zMedianBuffer = mutableListOf<Float>()
      var zFiltradoRun = 0f

      var startCalculatedKmh = 40.0f
      var startGpsKmh = 0f
      var maxGpsKmh = 0f
      var finalGpsKmh = 0f

      var velocityMs = 0f
      var maxCalcSpeedKmh = 40.0f
      var maxCalcSpeedMs = 40.0f / 3.6f
      var finalCalcSpeedKmh = 0f

      var elapsedSec = 0f
      var total = 0
      var rejected = 0
      var finishReason: FinishReason? = null

      var lastProcessedGpsTimestamp: Long = -1L
      var validGpsUpdatesCount: Int = 0

      var diffSum = 0.0
      var diffCount = 0
      var maxDiff = 0f

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

        startCalculatedKmh = startTriggerSpeedKmh
        startGpsKmh = 0f
        maxGpsKmh = 0f
        finalGpsKmh = 0f

        velocityMs = 0f
        maxCalcSpeedKmh = startTriggerSpeedKmh
        maxCalcSpeedMs = startTriggerSpeedKmh / 3.6f
        finalCalcSpeedKmh = 0f

        elapsedSec = 0f
        total = 0
        rejected = 0
        finishReason = null

        lastProcessedGpsTimestamp = -1L
        validGpsUpdatesCount = 0

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
  dynoTracker.startTriggerSpeedKmh = startSpeedTriggerKmh

  // Location / GPS Setup
  var hasLocationPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
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
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
      )
    }
  }

  var isGpsProviderEnabled by remember {
    mutableStateOf(locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false)
  }
  var hasGpsFix by remember { mutableStateOf(false) }
  var gpsAccuracyM by remember { mutableFloatStateOf(0.0f) }

  // Vehicle Stopped Check (< 3 km/h for >= 2 seconds)
  var isStoppedForTwoSeconds by remember { mutableStateOf(true) }
  var stoppedStartTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

  LaunchedEffect(currentGpsSpeedKmh) {
    if (currentGpsSpeedKmh >= 3.0f) {
      isStoppedForTwoSeconds = false
      stoppedStartTimeMs = System.currentTimeMillis()
    } else {
      val now = System.currentTimeMillis()
      if (now - stoppedStartTimeMs >= 2000L) {
        isStoppedForTwoSeconds = true
      } else {
        delay(2000L - (now - stoppedStartTimeMs).coerceAtLeast(0L))
        if (currentGpsSpeedKmh < 3.0f) {
          isStoppedForTwoSeconds = true
        }
      }
    }
  }

  // Official Start of Dyno Run using real GPS speed at the trigger instant
  fun triggerOfficialRunStart(nowNs: Long, availableGpsKmh: Float) {
    val actualGpsSpeed = availableGpsKmh.coerceAtLeast(0f)

    dynoTracker.state = DynoRunState.MEDINDO
    dynoTracker.runStartTimeNs = nowNs
    dynoTracker.lastSensorTimestampNs = nowNs
    dynoTracker.lastSampleRecordedNs = nowNs

    dynoTracker.startCalculatedKmh = actualGpsSpeed
    dynoTracker.startGpsKmh = actualGpsSpeed
    dynoTracker.velocityMs = actualGpsSpeed / 3.6f
    dynoTracker.maxCalcSpeedKmh = actualGpsSpeed
    dynoTracker.maxCalcSpeedMs = actualGpsSpeed / 3.6f
    dynoTracker.maxGpsKmh = actualGpsSpeed

    dynoTracker.decelerationStartNs = null
    dynoTracker.gpsSpeedDropStartNs = null
    dynoTracker.rejected = 0
    dynoTracker.total = 0
    dynoTracker.diffSum = 0.0
    dynoTracker.diffCount = 0
    dynoTracker.maxDiff = 0f
    dynoTracker.validGpsUpdatesCount = 1
    dynoTracker.finishReason = null
    dynoTracker.recordedSamples.clear()
    dynoTracker.prepCircularBuffer.clear()

    val firstSample = RunSample(
      elapsedTimeMs = 0L,
      filteredAccelerationZ = dynoTracker.zFiltradoRun,
      correctedAccelerationZ = (linearZ - dynoTracker.offsetZ) * (if (dynoTracker.invertSignal) -1f else 1f),
      gpsSpeedKmh = actualGpsSpeed,
      calculatedSpeedKmh = actualGpsSpeed,
      speedDifferenceKmh = 0.0f,
      gpsAccuracyMeters = gpsAccuracyM,
      gyroMagnitude = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ),
      isValid = true
    )
    dynoTracker.recordedSamples.add(firstSample)
    dynoTracker.total++

    runVelocityMs = actualGpsSpeed / 3.6f
    runElapsedSeconds = 0f
    resultSaved = false
    runFinishReason = null
    runState = DynoRunState.MEDINDO
  }

  // Function to finalize run and save automatically once, then navigate to results
  fun finalizeRun(reason: FinishReason) {
    if (dynoTracker.state == DynoRunState.MEDINDO) {
      val nowNs = System.nanoTime()
      dynoTracker.runEndTimeNs = nowNs
      dynoTracker.finalGpsKmh = currentGpsSpeedKmh
      dynoTracker.finalCalcSpeedKmh = dynoTracker.velocityMs * 3.6f
      dynoTracker.finishReason = reason
      dynoTracker.state = DynoRunState.FINALIZADO

      runFinishReason = reason
      runState = DynoRunState.FINALIZADO

      val avgDiff = if (dynoTracker.diffCount > 0) (dynoTracker.diffSum / dynoTracker.diffCount).toFloat() else 0f
      val peakDiff = abs(dynoTracker.maxGpsKmh - dynoTracker.maxCalcSpeedKmh)
      val rejectionRatio = if (dynoTracker.total > 0) dynoTracker.rejected.toFloat() / dynoTracker.total.toFloat() else 0f
      var invalidReasonText: String? = null

      val runQualityStr = when {
        reason == FinishReason.TIMEOUT -> {
          invalidReasonText = "Tempo de passagem excessivo (> 25s)."
          "INVÁLIDA"
        }
        dynoTracker.elapsedSec < 1.5f -> {
          invalidReasonText = "Duração muito curta (${String.format(Locale.US, "%.2f", dynoTracker.elapsedSec)}s) para registrar curva completa."
          "INVÁLIDA"
        }
        gpsAccuracyM > 12f -> {
          invalidReasonText = "Precisão do GPS insuficiente (${String.format(Locale.US, "%.1f", gpsAccuracyM)}m > 12m)."
          "INVÁLIDA"
        }
        rejectionRatio > 0.25f -> {
          invalidReasonText = "Mais de 25% das amostras rejeitadas por vibração excessiva."
          "INVÁLIDA"
        }
        dynoTracker.validGpsUpdatesCount < 2 -> {
          invalidReasonText = "Poucas leituras de GPS válidas (< 2) durante a medição."
          "INVÁLIDA"
        }
        peakDiff > 15.0f -> {
          invalidReasonText = "Divergência entre velocidade máxima GPS (${String.format(Locale.US, "%.1f", dynoTracker.maxGpsKmh)} km/h) e calculada (${String.format(Locale.US, "%.1f", dynoTracker.maxCalcSpeedKmh)} km/h)."
          "INVÁLIDA"
        }
        avgDiff > 12.0f -> {
          invalidReasonText = "Diferença média sincronizada entre GPS e acelerômetro elevada (±${String.format(Locale.US, "%.1f", avgDiff)} km/h)."
          "INVÁLIDA"
        }
        dynoTracker.maxDiff > 20.0f -> {
          invalidReasonText = "Pico de divergência momentânea excessivo (${String.format(Locale.US, "%.1f", dynoTracker.maxDiff)} km/h)."
          "INVÁLIDA"
        }
        dynoTracker.maxGpsKmh < (dynoTracker.startTriggerSpeedKmh + 5f) -> {
          invalidReasonText = "Velocidade máxima atingida insuficiente para teste de aceleração."
          "INVÁLIDA"
        }
        avgDiff <= 6.0f && dynoTracker.maxDiff <= 12.0f && peakDiff <= 10.0f && dynoTracker.validGpsUpdatesCount >= 2 &&
          gpsAccuracyM <= 6f && dynoTracker.elapsedSec in 2f..20f && rejectionRatio <= 0.10f &&
          dynoTracker.maxGpsKmh >= (dynoTracker.startTriggerSpeedKmh + 10f) -> "BOA"
        avgDiff <= 12.0f && dynoTracker.maxDiff <= 20.0f && peakDiff <= 15.0f && dynoTracker.validGpsUpdatesCount >= 2 &&
          gpsAccuracyM <= 10f && dynoTracker.elapsedSec <= 25f && rejectionRatio <= 0.25f -> "REGULAR"
        else -> {
          invalidReasonText = "Inconsistência na detecção inercial ou divergência de velocidade."
          "INVÁLIDA"
        }
      }

      val finalSamples = dynoTracker.recordedSamples.take(500).toList()
      val validCount = finalSamples.count { it.isValid }
      val rejectedCount = finalSamples.count { !it.isValid }
      val avgHz = if (dynoTracker.elapsedSec > 0f) finalSamples.size / dynoTracker.elapsedSec else 0f

      if (!resultSaved) {
        val result = RunResult(
          vehicleId = vehicle.id,
          vehicleName = "${vehicle.manufacturer} ${vehicle.model} ${vehicle.engine}".trim(),
          runStartCalculatedSpeedKmh = dynoTracker.startCalculatedKmh,
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
          invalidationReason = invalidReasonText,
          appVersion = "0.19.1",
          samples = finalSamples
        )
        runResultRepository.saveResult(result)
        resultSaved = true
        onNavigateToResults()
      }
    }
  }

  // Location Listener
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

          val isNewGpsFix = (location.time != dynoTracker.lastProcessedGpsTimestamp)
          if (isNewGpsFix) {
            dynoTracker.lastProcessedGpsTimestamp = location.time
          }

          if (dynoTracker.state == DynoRunState.AGUARDANDO_INICIO) {
            if (location.hasSpeed()) {
              dynoTracker.armedEstimatedSpeedMs = (dynoTracker.armedEstimatedSpeedMs * 0.70f + rawSpeed * 0.30f).coerceAtLeast(0f)
              armedEstimatedSpeedKmh = dynoTracker.armedEstimatedSpeedMs * 3.6f
            }

            val targetTrigger = dynoTracker.startTriggerSpeedKmh
            val estimatedKmh = dynoTracker.armedEstimatedSpeedMs * 3.6f
            if (speedKmh >= targetTrigger && location.accuracy <= 12.0f && isCalibrated) {
              val now = System.nanoTime()
              triggerOfficialRunStart(now, speedKmh)
            } else if (estimatedKmh >= targetTrigger && speedKmh >= (targetTrigger - 5.0f) && location.accuracy <= 12.0f && isCalibrated) {
              val now = System.nanoTime()
              triggerOfficialRunStart(now, speedKmh)
            }
          } else if (dynoTracker.state == DynoRunState.MEDINDO) {
            if (speedKmh > dynoTracker.maxGpsKmh) {
              dynoTracker.maxGpsKmh = speedKmh
            }

            val isBeforeDeceleration = dynoTracker.decelerationStartNs == null && dynoTracker.gpsSpeedDropStartNs == null
            if (isNewGpsFix && location.hasSpeed() && location.hasAccuracy() && location.accuracy <= 12f && isBeforeDeceleration) {
              dynoTracker.validGpsUpdatesCount++
              val currentCalcKmh = dynoTracker.velocityMs * 3.6f
              val diff = abs(currentCalcKmh - speedKmh)
              dynoTracker.diffSum += diff
              dynoTracker.diffCount++
              if (diff > dynoTracker.maxDiff) {
                dynoTracker.maxDiff = diff
              }
            }

            // GPS Deceleration Detection
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
          if (provider == LocationManager.GPS_PROVIDER) isGpsProviderEnabled = true
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
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200L, 0f, locationListener)
      } catch (e: Exception) {}

      onDispose {
        try { locationManager.removeUpdates(locationListener) } catch (e: Exception) {}
      }
    } else {
      onDispose {}
    }
  }

  // Sensor Listener
  DisposableEffect(sensorManager, accelerometerSensor, linearAccelerationSensor, gyroscopeSensor) {
    if (sensorManager != null) {
      val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
          when (event?.sensor?.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
              if (event.values.size >= 3) {
                linearX = event.values[0]
                linearY = event.values[1]
                linearZ = event.values[2]

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

                if (dynoTracker.state == DynoRunState.AGUARDANDO_INICIO) {
                  if (dynoTracker.armedLastNanoTime != 0L) {
                    val dt = (nowNs - dynoTracker.armedLastNanoTime) / 1_000_000_000f
                    if (dt > 0f && dt <= 0.1f) {
                      dynoTracker.armedEstimatedSpeedMs = (dynoTracker.armedEstimatedSpeedMs + zRunFinal * dt).coerceAtLeast(0f)
                      armedEstimatedSpeedKmh = dynoTracker.armedEstimatedSpeedMs * 3.6f
                    }
                  }
                  dynoTracker.armedLastNanoTime = nowNs

                  val targetTrigger = dynoTracker.startTriggerSpeedKmh
                  val estKmh = dynoTracker.armedEstimatedSpeedMs * 3.6f
                  if (currentGpsSpeedKmh >= targetTrigger && gpsAccuracyM <= 12.0f && isCalibrated) {
                    triggerOfficialRunStart(nowNs, currentGpsSpeedKmh)
                  } else if (estKmh >= targetTrigger && currentGpsSpeedKmh >= (targetTrigger - 5.0f) && gpsAccuracyM <= 12.0f && isCalibrated) {
                    triggerOfficialRunStart(nowNs, currentGpsSpeedKmh)
                  }
                } else if (dynoTracker.state == DynoRunState.MEDINDO) {
                  if (dynoTracker.lastSensorTimestampNs != 0L) {
                    val dt = (nowNs - dynoTracker.lastSensorTimestampNs) / 1_000_000_000f
                    if (dt > 0f && dt <= 0.1f) {
                      dynoTracker.velocityMs = (dynoTracker.velocityMs + zRunFinal * dt).coerceAtLeast(0f)
                      val currentCalcKmh = dynoTracker.velocityMs * 3.6f
                      runVelocityMs = dynoTracker.velocityMs

                      if (currentCalcKmh > dynoTracker.maxCalcSpeedKmh) {
                        dynoTracker.maxCalcSpeedKmh = currentCalcKmh
                        dynoTracker.maxCalcSpeedMs = dynoTracker.velocityMs
                      }
                      dynoTracker.elapsedSec = (nowNs - dynoTracker.runStartTimeNs) / 1_000_000_000f
                      runElapsedSeconds = dynoTracker.elapsedSec
                    }
                  }
                  dynoTracker.lastSensorTimestampNs = nowNs

                  val corrX = abs(event.values[0] - dynoTracker.offsetX)
                  val corrY = abs(event.values[1] - dynoTracker.offsetY)
                  val gyroMag = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)
                  val maxNormalVib = max(3.5f, dynoTracker.normalVib * 2.5f)
                  val maxNormalGyro = max(2.5f, dynoTracker.gyroDev * 3.0f)

                  dynoTracker.total++
                  val isSampleValid = !(corrX > maxNormalVib || corrY > maxNormalVib || gyroMag > maxNormalGyro)
                  if (!isSampleValid) {
                    dynoTracker.rejected++
                  }

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

                  // Sensor deceleration detection
                  if (zRunFinal < -0.15f) {
                    if (dynoTracker.decelerationStartNs == null) {
                      dynoTracker.decelerationStartNs = nowNs
                    } else if (nowNs - dynoTracker.decelerationStartNs!! >= 600_000_000L) {
                      finalizeRun(FinishReason.SENSOR_DECELERATION)
                    }
                  } else {
                    dynoTracker.decelerationStartNs = null
                  }

                  if (dynoTracker.elapsedSec > 25.0f) {
                    finalizeRun(FinishReason.TIMEOUT)
                  }
                }

                // Calibration Collection Logic
                if (calibCollector.isCollecting) {
                  val currentCount = calibCollector.count
                  if (currentCount > 10) {
                    val partialAvgX = (calibCollector.sumX / currentCount).toFloat()
                    val partialAvgY = (calibCollector.sumY / currentCount).toFloat()
                    val partialAvgZ = (calibCollector.sumZ / currentCount).toFloat()
                    val gyroMag = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)

                    if (abs(event.values[0] - partialAvgX) > 2.0f ||
                      abs(event.values[1] - partialAvgY) > 2.0f ||
                      abs(event.values[2] - partialAvgZ) > 2.0f ||
                      gyroMag > 1.8f) {
                      calibCollector.reset()
                      isCalibrating = false
                      calibrationStatusText = "Calibração cancelada: aparelho se moveu"
                    }
                  }

                  if (calibCollector.isCollecting) {
                    calibCollector.sumX += event.values[0]
                    calibCollector.sumY += event.values[1]
                    calibCollector.sumZ += event.values[2]

                    val currentAvgX = (calibCollector.sumX / (currentCount + 1)).toFloat()
                    val currentAvgY = (calibCollector.sumY / (currentCount + 1)).toFloat()
                    val currentAvgZ = (calibCollector.sumZ / (currentCount + 1)).toFloat()

                    calibCollector.sumDevX += abs(event.values[0] - currentAvgX)
                    calibCollector.sumDevY += abs(event.values[1] - currentAvgY)
                    calibCollector.sumDevZ += abs(event.values[2] - currentAvgZ)

                    val gMag = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)
                    calibCollector.sumGyroMag += gMag

                    calibCollector.count++
                    val newCount = calibCollector.count
                    calibProgressPercent = (newCount * 100) / 150
                    calibrationStatusText = "Calibrando… $calibProgressPercent%"

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
                      calibrationStatusText = "Calibração concluída"
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
      onDispose {}
    }
  }

  // Internal Stability Evaluation (gyroscope + linear accel magnitude)
  val currentGyroMag = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)
  val phoneStabilityState = when {
    gyroscopeSensor == null || linearAccelerationSensor == null -> "pronto"
    currentGyroMag > 0.65f -> "aguardando"
    currentGyroMag > 0.25f -> "verificando"
    else -> "pronto"
  }

  // GPS textual status
  val gpsStatusCategory = when {
    !hasLocationPermission -> "Permissão negada"
    !isGpsProviderEnabled -> "GPS desligado"
    !hasGpsFix -> "Aguardando GPS"
    gpsAccuracyM <= 8.0f -> "GPS bom"
    gpsAccuracyM <= 15.0f -> "GPS regular"
    else -> "GPS fraco"
  }
  val isGpsReady = hasLocationPermission && isGpsProviderEnabled && hasGpsFix && gpsAccuracyM <= 15.0f

  val isVehicleMoving = currentGpsSpeedKmh >= 3.0f
  val isReadyToArm = isCalibrated && isGpsReady && isStoppedForTwoSeconds && !isVehicleMoving

  // Hardware back handler
  BackHandler {
    if (runState == DynoRunState.AGUARDANDO_INICIO || runState == DynoRunState.MEDINDO) {
      showCancelConfirmDialog = true
    } else {
      onNavigateBack()
    }
  }

  if (showCancelConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showCancelConfirmDialog = false },
      title = { Text("Cancelar teste?", fontWeight = FontWeight.Bold) },
      text = { Text("O teste em andamento será cancelado e os dados temporários serão descartados.") },
      confirmButton = {
        Button(
          onClick = {
            showCancelConfirmDialog = false
            dynoTracker.reset()
            runState = DynoRunState.PARADO
            onNavigateHomeOrBack(onNavigateToHome, onNavigateBack)
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Sim, cancelar")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCancelConfirmDialog = false }) {
          Text("Continuar no teste")
        }
      }
    )
  }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("test_preparation_screen"),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = if (runState == DynoRunState.MEDINDO) "MEDIÇÃO EM ANDAMENTO"
                     else if (runState == DynoRunState.AGUARDANDO_INICIO) "TESTE ARMADO"
                     else "PREPARAR TESTE",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                fontSize = 18.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
            if (runState == DynoRunState.PARADO) {
              Text(
                text = "${vehicle.manufacturer} ${vehicle.model} ${vehicle.engine}".trim(),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        },
        navigationIcon = {
          IconButton(
            onClick = {
              if (runState != DynoRunState.PARADO) {
                showCancelConfirmDialog = true
              } else {
                onNavigateBack()
              }
            },
            modifier = Modifier.testTag("top_bar_back_prep")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Voltar"
            )
          }
        },
        actions = {
          if (runState == DynoRunState.PARADO) {
            TextButton(
              onClick = onSwitchVehicle,
              modifier = Modifier.testTag("btn_switch_vehicle_top")
            ) {
              Text(
                text = "Trocar veículo",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 12.sp
                ),
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background
        )
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter
    ) {
      // -------------------------------------------------------------
      // STATE 1: ARMED (AGUARDANDO_INICIO)
      // -------------------------------------------------------------
      if (runState == DynoRunState.AGUARDANDO_INICIO) {
        ArmedDrivingHud(
          currentSpeedKmh = currentGpsSpeedKmh,
          targetTriggerSpeedKmh = startSpeedTriggerKmh,
          gpsStatusText = gpsStatusCategory,
          onCancel = {
            dynoTracker.reset()
            runState = DynoRunState.PARADO
          }
        )
      }
      // -------------------------------------------------------------
      // STATE 2: MEASURING (MEDINDO)
      // -------------------------------------------------------------
      else if (runState == DynoRunState.MEDINDO) {
        MeasuringHud(
          currentSpeedKmh = runVelocityMs * 3.6f,
          elapsedSeconds = runElapsedSeconds,
          targetTriggerSpeedKmh = startSpeedTriggerKmh,
          maxSpeedKmh = max(dynoTracker.maxCalcSpeedKmh, runVelocityMs * 3.6f),
          onEmergencyStop = {
            finalizeRun(FinishReason.USER_STOP)
          }
        )
      }
      // -------------------------------------------------------------
      // STATE 0: CONFIGURING & PREPARATION (PARADO)
      // -------------------------------------------------------------
      else {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .widthIn(max = 480.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // 1. RESUMO DO VEÍCULO
          VehicleSummaryCard(
            vehicle = vehicle,
            startSpeedTriggerKmh = startSpeedTriggerKmh,
            onEditVehicle = onEditVehicle
          )

          // 2. FIXAÇÃO DO CELULAR
          PhoneMountCard(
            stabilityState = phoneStabilityState
          )

          // 3. CALIBRAÇÃO SIMPLIFICADA
          CalibrationCard(
            isCalibrated = isCalibrated,
            isCalibrating = isCalibrating,
            progressPercent = calibProgressPercent,
            statusText = calibrationStatusText,
            isVehicleMoving = isVehicleMoving,
            onStartCalibration = {
              if (!isVehicleMoving) {
                calibCollector.reset()
                calibCollector.isCollecting = true
                isCalibrating = true
                calibProgressPercent = 0
                calibrationStatusText = "Calibrando… 0%"
              }
            }
          )

          // 4. VELOCIDADE DE INÍCIO AUTOMÁTICO
          StartSpeedTriggerCard(
            selectedSpeedKmh = startSpeedTriggerKmh,
            onSelectSpeed = { newSpeed ->
              startSpeedTriggerKmh = newSpeed
              dynoTracker.startTriggerSpeedKmh = newSpeed
              prefs.edit().putFloat("start_speed_trigger_kmh", newSpeed).apply()
            }
          )

          // 5. VERIFICAÇÃO AUTOMÁTICA
          ReadinessChecklistCard(
            isVehicleConfigured = true,
            isCalibrated = isCalibrated,
            isPhoneStable = phoneStabilityState == "pronto",
            isGpsReady = isGpsReady,
            gpsStatusText = gpsStatusCategory
          )

          Spacer(modifier = Modifier.height(4.dp))

          // Moving Alert Notice if vehicle is moving
          if (isVehicleMoving) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
              modifier = Modifier.fillMaxWidth().testTag("vehicle_moving_warning")
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(
                  imageVector = Icons.Outlined.Warning,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(20.dp)
                )
                Text(
                  text = "Pare o veículo completamente antes de armar (${String.format(Locale.US, "%.1f", currentGpsSpeedKmh)} km/h).",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp
                  ),
                  color = MaterialTheme.colorScheme.error
                )
              }
            }
          }

          // 6. BOTÃO PRINCIPAL: ARMAR TESTE
          Button(
            onClick = {
              if (isReadyToArm) {
                dynoTracker.reset()
                dynoTracker.state = DynoRunState.AGUARDANDO_INICIO
                dynoTracker.armedEstimatedSpeedMs = 0f
                dynoTracker.armedLastNanoTime = System.nanoTime()
                armedEstimatedSpeedKmh = 0f
                runElapsedSeconds = 0f
                runVelocityMs = 0f
                resultSaved = false
                runFinishReason = null
                runState = DynoRunState.AGUARDANDO_INICIO
              }
            },
            enabled = isReadyToArm,
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("btn_arm_test"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              contentColor = MaterialTheme.colorScheme.onPrimary
            )
          ) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = null,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isVehicleMoving) "PARE O VEÍCULO (< 3 km/h)" else "ARMAR TESTE",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                fontSize = 15.sp
              )
            )
          }

          // 7. BOTÃO SECUNDÁRIO: VOLTAR AO INÍCIO
          OutlinedButton(
            onClick = onNavigateToHome,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("btn_back_to_home_from_prep"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "VOLTAR AO INÍCIO",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                letterSpacing = 0.5.sp
              )
            )
          }

          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    }
  }
}

private fun onNavigateHomeOrBack(onHome: () -> Unit, onBack: () -> Unit) {
  try {
    onHome()
  } catch (e: Exception) {
    onBack()
  }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 1: RESUMO DO VEÍCULO (Cartão Compacto)
// -----------------------------------------------------------------------------------------
@Composable
private fun VehicleSummaryCard(
  vehicle: VehicleProfile,
  startSpeedTriggerKmh: Float,
  onEditVehicle: () -> Unit,
  modifier: Modifier = Modifier
) {
  val transmission = VehicleDatabase.getTransmission(vehicle.transmissionId)
  val transmissionLabel = transmission?.displayName ?: vehicle.customTransmissionName ?: "Câmbio manual"
  val tireCalculation = VehicleCalculations.calculateTireDimensions(
    vehicle.tireWidthMm,
    vehicle.tireAspectRatio,
    vehicle.wheelDiameterInches
  )
  val tireDimension = tireCalculation.formattedMeasure
  val totalWeightKg = VehicleCalculations.calculateTotalWeight(
    curbWeightKg = vehicle.curbWeightKg,
    driverWeightKg = if (vehicle.driverWeightKg > 0f) vehicle.driverWeightKg else 75f,
    passengerWeightKg = vehicle.passengerWeightKg,
    cargoWeightKg = vehicle.cargoWeightKg,
    audioWeightKg = vehicle.audioWeightKg,
    gnvWeightKg = vehicle.gnvWeightKg,
    otherWeightKg = vehicle.otherWeightKg,
    removedWeightKg = vehicle.removedWeightKg,
    measuredTotalWeightKg = vehicle.measuredTotalWeightKg,
    useMeasuredWeight = vehicle.useMeasuredWeight
  )

  Card(
    modifier = modifier.fillMaxWidth().testTag("card_vehicle_summary_prep"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.DirectionsCar,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = "VEÍCULO DO TESTE",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp,
              fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.primary
          )
        }

        TextButton(
          onClick = onEditVehicle,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.testTag("btn_correct_vehicle_data")
        ) {
          Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = null,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "CORRIGIR DADOS",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 11.5.sp
            ),
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      Text(
        text = "${vehicle.manufacturer} ${vehicle.model} ${vehicle.engine}".trim(),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )

      HorizontalDivider(
        thickness = 0.8.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
      )

      // 4 Specs Lines
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SpecRow(label = "Peso total utilizado", value = String.format(Locale.US, "%.0f kg", totalWeightKg))
        SpecRow(label = "Pneu", value = tireDimension)
        SpecRow(label = "Câmbio e marcha", value = "$transmissionLabel (3ª Marcha)")
        SpecRow(label = "Velocidade de início", value = "${startSpeedTriggerKmh.toInt()} km/h")
      }
    }
  }
}

@Composable
private fun SpecRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 2: FIXAÇÃO DO CELULAR
// -----------------------------------------------------------------------------------------
@Composable
private fun PhoneMountCard(
  stabilityState: String,
  modifier: Modifier = Modifier
) {
  val (statusLabel, statusColor, statusBg) = when (stabilityState) {
    "aguardando" -> Triple("Aguardando posicionamento", MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceContainer)
    "verificando" -> Triple("Verificando estabilidade", Color(0xFFF59E0B), Color(0xFFF59E0B).copy(alpha = 0.15f))
    else -> Triple("Celular pronto", Color(0xFF10B981), Color(0xFF10B981).copy(alpha = 0.15f))
  }

  Card(
    modifier = modifier.fillMaxWidth().testTag("card_phone_mount"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.PhoneAndroid,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = "POSICIONE O CELULAR",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp,
              fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.primary
          )
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = statusBg
        ) {
          Text(
            text = statusLabel,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp
            ),
            color = statusColor
          )
        }
      }

      Text(
        text = "Prenda o celular firmemente no suporte, na posição vertical, com a tela voltada para o motorista. Não movimente o aparelho durante o teste.",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 17.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 3: CALIBRAÇÃO SIMPLIFICADA
// -----------------------------------------------------------------------------------------
@Composable
private fun CalibrationCard(
  isCalibrated: Boolean,
  isCalibrating: Boolean,
  progressPercent: Int,
  statusText: String,
  isVehicleMoving: Boolean,
  onStartCalibration: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth().testTag("card_calibration_prep"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.Sensors,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = "CALIBRAÇÃO",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp,
              fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.primary
          )
        }

        if (isCalibrated && !isCalibrating) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF10B981).copy(alpha = 0.15f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(12.dp)
              )
              Text(
                text = "Calibração concluída",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp
                ),
                color = Color(0xFF10B981)
              )
            }
          }
        }
      }

      Text(
        text = "Com o veículo parado e o motor funcionando, mantenha o celular preso no suporte.",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 17.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      if (isCalibrating) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          LinearProgressIndicator(
            progress = { (progressPercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainer
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Calibrando… $progressPercent%",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Mantenha parado",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        if (!isCalibrated) {
          Button(
            onClick = onStartCalibration,
            enabled = !isVehicleMoving,
            modifier = Modifier
              .fillMaxWidth()
              .height(46.dp)
              .testTag("btn_calibrate_phone"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            )
          ) {
            Icon(
              imageVector = Icons.Outlined.Refresh,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "CALIBRAR CELULAR",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                letterSpacing = 0.5.sp
              )
            )
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            TextButton(
              onClick = onStartCalibration,
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.testTag("btn_recalibrate_phone")
            ) {
              Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Calibrar novamente",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.5.sp
                ),
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }
    }
  }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 4: VELOCIDADE DE INÍCIO AUTOMÁTICO
// -----------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartSpeedTriggerCard(
  selectedSpeedKmh: Float,
  onSelectSpeed: (Float) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth().testTag("card_start_speed_trigger"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Outlined.Speed,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Text(
          text = "INÍCIO AUTOMÁTICO",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            fontSize = 12.sp
          ),
          color = MaterialTheme.colorScheme.primary
        )
      }

      Text(
        text = "Escolha a velocidade em que a medição começará.",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        listOf(40.0f, 50.0f, 60.0f).forEach { speed ->
          val isSelected = selectedSpeedKmh == speed
          FilterChip(
            selected = isSelected,
            onClick = { onSelectSpeed(speed) },
            label = {
              Text(
                text = "${speed.toInt()} km/h",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                modifier = Modifier.padding(vertical = 4.dp)
              )
            },
            modifier = Modifier.weight(1f).testTag("chip_trigger_${speed.toInt()}"),
            shape = RoundedCornerShape(10.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
              containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
          )
        }
      }

      Text(
        text = "O teste começa automaticamente quando o GPS atingir a velocidade escolhida.",
        style = MaterialTheme.typography.bodySmall.copy(
          fontSize = 11.5.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
      )
    }
  }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 5: VERIFICAÇÃO AUTOMÁTICA
// -----------------------------------------------------------------------------------------
@Composable
private fun ReadinessChecklistCard(
  isVehicleConfigured: Boolean,
  isCalibrated: Boolean,
  isPhoneStable: Boolean,
  isGpsReady: Boolean,
  gpsStatusText: String,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth().testTag("card_readiness_checklist"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Outlined.CheckCircle,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Text(
          text = "PRONTO PARA O TESTE",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            fontSize = 12.sp
          ),
          color = MaterialTheme.colorScheme.primary
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CheckItemRow(
          title = "Veículo configurado",
          isOk = isVehicleConfigured,
          statusText = if (isVehicleConfigured) "Pronto" else "Pendente"
        )
        CheckItemRow(
          title = "Celular calibrado",
          isOk = isCalibrated,
          statusText = if (isCalibrated) "Calibrado" else "Pendente"
        )
        CheckItemRow(
          title = "Celular estável no suporte",
          isOk = isPhoneStable,
          statusText = if (isPhoneStable) "Estável" else "Verificando"
        )
        CheckItemRow(
          title = "GPS disponível",
          isOk = isGpsReady,
          statusText = gpsStatusText
        )
      }
    }
  }
}

@Composable
private fun CheckItemRow(
  title: String,
  isOk: Boolean,
  statusText: String
) {
  val iconColor = if (isOk) Color(0xFF10B981) else Color(0xFFF59E0B)
  val iconBg = if (isOk) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Surface(
        shape = CircleShape,
        color = iconBg,
        modifier = Modifier.size(20.dp)
      ) {
        Icon(
          imageVector = if (isOk) Icons.Filled.Check else Icons.Filled.Warning,
          contentDescription = null,
          tint = iconColor,
          modifier = Modifier.padding(3.dp)
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    Text(
      text = statusText,
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.5.sp
      ),
      color = iconColor
    )
  }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 6: VELOCÍMETRO DIGITAL SEMICIRCULAR
// -----------------------------------------------------------------------------------------
@Composable
private fun SemicircularSpeedometer(
  currentSpeedKmh: Float,
  targetTriggerSpeedKmh: Float,
  isMeasuring: Boolean = false,
  modifier: Modifier = Modifier
) {
  val density = LocalDensity.current
  val visualSpeed = currentSpeedKmh.coerceAtLeast(0f)
  val progressFraction = (visualSpeed / 200f).coerceIn(0f, 1f)
  val progressSweep = progressFraction * 220f

  val primaryColor = if (isMeasuring) Color(0xFF10B981) else Color(0xFF38BDF8)
  val triggerHighlightColor = Color(0xFF38BDF8)
  val trackColor = Color(0xFF222834)
  val normalTickColor = Color(0xFF4B5563)
  val normalTextColor = Color(0xFF9CA3AF).toArgb()
  val highlightTextColor = triggerHighlightColor.toArgb()

  val tickSteps = remember { listOf(0, 20, 40, 60, 80, 100, 120, 140, 160, 180, 200) }

  val textPaint = remember {
    android.graphics.Paint().apply {
      isAntiAlias = true
      textAlign = android.graphics.Paint.Align.CENTER
    }
  }

  val accessibleDescription = "Velocidade GPS: ${String.format(Locale.US, "%.0f", visualSpeed)} km/h"

  Box(
    modifier = modifier
      .size(300.dp, 250.dp)
      .semantics { contentDescription = accessibleDescription }
      .testTag("semicircular_speedometer"),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val canvasWidth = size.width
      val canvasHeight = size.height
      val centerX = canvasWidth / 2f
      val centerY = canvasHeight * 0.52f

      val arcRadius = with(density) { 98.dp.toPx() }
      val strokeWidthPx = with(density) { 14.dp.toPx() }
      val labelRadius = arcRadius + with(density) { 20.dp.toPx() }
      val tickInnerRadius = arcRadius - with(density) { 9.dp.toPx() }
      val tickOuterRadius = arcRadius - with(density) { 3.dp.toPx() }

      val arcRect = Rect(
        left = centerX - arcRadius,
        top = centerY - arcRadius,
        right = centerX + arcRadius,
        bottom = centerY + arcRadius
      )

      // 1. Background Arc (220 degrees starting at 160 degrees)
      drawArc(
        color = trackColor,
        startAngle = 160f,
        sweepAngle = 220f,
        useCenter = false,
        topLeft = arcRect.topLeft,
        size = arcRect.size,
        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
      )

      // 2. Dynamic Progress Arc
      if (progressSweep > 0.5f) {
        drawArc(
          color = primaryColor,
          startAngle = 160f,
          sweepAngle = progressSweep,
          useCenter = false,
          topLeft = arcRect.topLeft,
          size = arcRect.size,
          style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )
      }

      // 3. Ticks and Labels
      val labelTextSizePx = with(density) { 10.5.sp.toPx() }
      textPaint.textSize = labelTextSizePx

      tickSteps.forEach { step ->
        val stepFraction = step / 200f
        val stepAngleDeg = 160f + stepFraction * 220f
        val stepAngleRad = Math.toRadians(stepAngleDeg.toDouble())

        val cosA = cos(stepAngleRad).toFloat()
        val sinA = sin(stepAngleRad).toFloat()

        val isTrigger = step == targetTriggerSpeedKmh.toInt()

        // Tick line
        val tOuter = if (isTrigger) arcRadius - with(density) { 1.dp.toPx() } else tickOuterRadius
        val tInner = if (isTrigger) arcRadius - with(density) { 13.dp.toPx() } else tickInnerRadius
        val tColor = if (isTrigger) triggerHighlightColor else normalTickColor
        val tStroke = with(density) { (if (isTrigger) 2.5.dp else 1.2.dp).toPx() }

        drawLine(
          color = tColor,
          start = Offset(centerX + tInner * cosA, centerY + tInner * sinA),
          end = Offset(centerX + tOuter * cosA, centerY + tOuter * sinA),
          strokeWidth = tStroke,
          cap = StrokeCap.Round
        )

        // Numbers along the arc perimeter
        val lx = centerX + labelRadius * cosA
        val ly = centerY + labelRadius * sinA + (labelTextSizePx * 0.35f)

        drawIntoCanvas { canvas ->
          textPaint.color = if (isTrigger) highlightTextColor else normalTextColor
          textPaint.typeface = if (isTrigger) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
          canvas.nativeCanvas.drawText(step.toString(), lx, ly, textPaint)
        }
      }

      // 4. Dot indicator at initial trigger speed during measurement
      if (isMeasuring && targetTriggerSpeedKmh > 0f) {
        val trigFraction = (targetTriggerSpeedKmh / 200f).coerceIn(0f, 1f)
        val trigAngleDeg = 160f + trigFraction * 220f
        val trigAngleRad = Math.toRadians(trigAngleDeg.toDouble())
        val tx = centerX + arcRadius * cos(trigAngleRad).toFloat()
        val ty = centerY + arcRadius * sin(trigAngleRad).toFloat()
        drawCircle(
          color = Color.White,
          radius = with(density) { 4.dp.toPx() },
          center = Offset(tx, ty)
        )
      }
    }

    // Center Display Content
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .padding(top = 18.dp)
        .align(Alignment.Center)
    ) {
      Text(
        text = "VELOCIDADE GPS",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.1.sp,
          fontSize = 11.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      val formattedSpeed = if (visualSpeed < 10f && visualSpeed > 0f && (visualSpeed % 1.0f != 0f)) {
        String.format(Locale.US, "%.1f", visualSpeed)
      } else {
        String.format(Locale.US, "%.0f", visualSpeed)
      }

      Text(
        text = formattedSpeed,
        style = MaterialTheme.typography.displayLarge.copy(
          fontWeight = FontWeight.Black,
          fontSize = 54.sp,
          letterSpacing = (-1).sp
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 0.dp)
      )

      Text(
        text = "km/h",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      if (!isMeasuring) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Início automático: ${targetTriggerSpeedKmh.toInt()} km/h",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
          ),
          color = Color(0xFF38BDF8)
        )
      }
    }
  }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 7: TELA ENQUANTO AGUARDA (DRIVING HUD)
// -----------------------------------------------------------------------------------------
@Composable
private fun ArmedDrivingHud(
  currentSpeedKmh: Float,
  targetTriggerSpeedKmh: Float,
  gpsStatusText: String,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier
) {
  val remainingKmh = (targetTriggerSpeedKmh - currentSpeedKmh).coerceAtLeast(0f)
  val isCloseToTrigger = currentSpeedKmh >= (targetTriggerSpeedKmh - 3.0f) && currentSpeedKmh < targetTriggerSpeedKmh

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(20.dp)
      .widthIn(max = 480.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    // Top Status
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      DynoStatusBadge(
        text = "TESTE ARMADO",
        status = DynoBadgeStatus.INFO
      )

      Text(
        text = "Acelere na marcha selecionada",
        style = MaterialTheme.typography.bodyLarge.copy(
          fontWeight = FontWeight.Medium,
          textAlign = TextAlign.Center
        ),
        color = DynoTextPrimary
      )
    }

    // Center Speed HUD with DynoSpeedometer
    DynoCard(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 10.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        DynoSpeedometer(
          currentSpeedKmh = currentSpeedKmh,
          targetTriggerSpeedKmh = targetTriggerSpeedKmh,
          isMeasuring = false
        )

        // Remaining speed indicator or "PREPARE-SE"
        if (isCloseToTrigger) {
          DynoStatusBadge(
            text = "PREPARE-SE PARA ACELERAR",
            status = DynoBadgeStatus.WARNING
          )
        } else {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = DynoSurfaceElevated
          ) {
            Text(
              text = if (remainingKmh > 0f) "FALTAM ${remainingKmh.toInt()} km/h PARA INICIAR" else "INICIANDO MEDIÇÃO...",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                fontSize = 12.sp
              ),
              color = DynoBlueLight,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
          }
        }
      }
    }

    // Bottom Action & GPS indicator
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Surface(
        shape = CircleShape,
        color = DynoSurfaceContainer
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = DynoSuccessGreen,
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = gpsStatusText,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.SemiBold,
              fontSize = 11.5.sp
            ),
            color = DynoTextPrimary
          )
        }
      }

      DynoDangerButton(
        text = "CANCELAR TESTE",
        onClick = onCancel,
        icon = Icons.Default.Close,
        isOutlined = true,
        modifier = Modifier.fillMaxWidth(),
        testTag = "btn_cancel_armed_test"
      )
    }
  }
}

// -----------------------------------------------------------------------------------------
// COMPONENT 8: TELA DURANTE A MEDIÇÃO (MEDINDO)
// -----------------------------------------------------------------------------------------
@Composable
private fun MeasuringHud(
  currentSpeedKmh: Float,
  elapsedSeconds: Float,
  targetTriggerSpeedKmh: Float,
  maxSpeedKmh: Float,
  onEmergencyStop: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(20.dp)
      .widthIn(max = 480.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    // Top Status
    DynoStatusBadge(
      text = "MEDIÇÃO EM ANDAMENTO",
      status = DynoBadgeStatus.SUCCESS
    )

    // Center Speed & Stats HUD with DynoSpeedometer
    DynoCard(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 10.dp),
      borderColor = DynoSuccessGreen.copy(alpha = 0.5f)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        DynoSpeedometer(
          currentSpeedKmh = currentSpeedKmh,
          targetTriggerSpeedKmh = targetTriggerSpeedKmh,
          isMeasuring = true
        )

        // Stats panel: Tempo, Início, Velocidade máxima
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = DynoSurfaceElevated,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "Tempo",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = DynoTextSecondary
              )
              Text(
                text = String.format(Locale.US, "%.2f s", elapsedSeconds),
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                ),
                color = DynoTextPrimary
              )
            }

            Surface(
              modifier = Modifier
                .height(24.dp)
                .width(1.dp),
              color = DynoDivider
            ) {}

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "Início",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = DynoTextSecondary
              )
              Text(
                text = "${targetTriggerSpeedKmh.toInt()} km/h",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                ),
                color = DynoTextPrimary
              )
            }

            Surface(
              modifier = Modifier
                .height(24.dp)
                .width(1.dp),
              color = DynoDivider
            ) {}

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "Vel. máxima",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = DynoTextSecondary
              )
              Text(
                text = String.format(Locale.US, "%.1f km/h", max(maxSpeedKmh, currentSpeedKmh)),
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                ),
                color = DynoSuccessGreen
              )
            }
          }
        }

        Text(
          text = "Mantenha o pé cravado na mesma marcha.",
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            fontSize = 13.sp
          ),
          color = DynoSuccessGreen
        )
      }
    }

    // Bottom Action: Emergency stop
    DynoDangerButton(
      text = "ENCERRAR TESTE",
      onClick = onEmergencyStop,
      icon = Icons.Default.Stop,
      modifier = Modifier.fillMaxWidth(),
      testTag = "btn_stop_measuring_emergency"
    )
  }
}
