package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.example.ui.components.DynoStatusBadge
import com.example.ui.theme.DynoBackground
import com.example.ui.theme.DynoBlueLight
import com.example.ui.theme.DynoBluePrimary
import com.example.ui.theme.DynoBorder
import com.example.ui.theme.DynoBorderLight
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

/**
 * Gerenciador centralizado de bloqueio de orientação de tela.
 * Bloqueia a orientação enquanto estiver no Composable e restaura ao sair.
 */
@Composable
fun LockScreenOrientation(orientation: Int) {
  val context = LocalContext.current
  val activity = remember(context) {
    generateSequence(context) { current ->
      (current as? ContextWrapper)?.baseContext
    }.filterIsInstance<Activity>().firstOrNull()
  }

  DisposableEffect(activity, orientation) {
    val previousOrientation = activity?.requestedOrientation

    activity?.requestedOrientation = orientation

    onDispose {
      if (previousOrientation != null) {
        activity.requestedOrientation = previousOrientation
      } else {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
      }
    }
  }
}

/**
 * PAINEL HORIZONTAL PRINCIPAL DO TESTE DYNO LITE
 * Executa exclusivamente em orientação horizontal durante a preparação e a passagem.
 */
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
  // 1. BLOQUEIO CENTRALIZADO DE ORIENTAÇÃO HORIZONTAL
  LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)

  val context = LocalContext.current
  val activity = remember(context) {
    generateSequence(context) { current ->
      (current as? ContextWrapper)?.baseContext
    }.filterIsInstance<Activity>().firstOrNull()
  }

  // 2. MODO IMERSIVO E MANTER TELA ACESA (SEM MANIPULAR ORIENTAÇÃO)
  DisposableEffect(activity) {
    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    val window = activity?.window
    val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    insetsController?.hide(WindowInsetsCompat.Type.navigationBars())

    onDispose {
      activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      insetsController?.show(WindowInsetsCompat.Type.navigationBars())
    }
  }

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

  val linearAccelerationSensor = remember(sensorManager) {
    sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
  }
  val gyroscopeSensor = remember(sensorManager) {
    sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
  }

  // Sensores Brutos e Filtrados
  var linearX by remember { mutableFloatStateOf(0f) }
  var linearY by remember { mutableFloatStateOf(0f) }
  var linearZ by remember { mutableFloatStateOf(0f) }
  var gyroX by remember { mutableFloatStateOf(0f) }
  var gyroY by remember { mutableFloatStateOf(0f) }
  var gyroZ by remember { mutableFloatStateOf(0f) }

  // Offsets de Calibração e Ruído Normal
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
  var hasPhoneMovedAfterCalib by remember { mutableStateOf(false) }
  var calibrationStatusText by remember {
    mutableStateOf(if (isCalibrated) "Calibração concluída" else "Não calibrado")
  }

  // Coletor de Calibração: Coleta 150 amostras (~3s) com veículo parado no suporte
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

  // Velocidade de Gatilho de Início Automático (40, 50 ou 60 km/h)
  var startSpeedTriggerKmh by remember {
    mutableFloatStateOf(prefs.getFloat("start_speed_trigger_kmh", 40.0f))
  }

  // Máquina de Estados da Passagem Dyno
  var runState by remember { mutableStateOf(DynoRunState.PARADO) }
  var currentGpsSpeedKmh by remember { mutableFloatStateOf(0f) }
  var armedEstimatedSpeedKmh by remember { mutableFloatStateOf(0f) }
  var runElapsedSeconds by remember { mutableFloatStateOf(0f) }
  var runVelocityMs by remember { mutableFloatStateOf(0f) }
  var resultSaved by remember { mutableStateOf(false) }
  var showCancelConfirmDialog by remember { mutableStateOf(false) }
  var showRecalibrateConfirmDialog by remember { mutableStateOf(false) }

  // Tracker e Amostragem
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

  // Permissões e Provedor GPS
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

  // Verificação de Veículo Parado (< 3 km/h estável)
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

  // Início Oficial da Passagem usando a velocidade GPS real no instante do gatilho
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
    runState = DynoRunState.MEDINDO
  }

  // Finalização e Salvamento Único da Passagem
  fun finalizeRun(reason: FinishReason) {
    if (dynoTracker.state == DynoRunState.MEDINDO && !resultSaved) {
      val nowNs = System.nanoTime()
      dynoTracker.runEndTimeNs = nowNs
      dynoTracker.finalGpsKmh = currentGpsSpeedKmh
      dynoTracker.finalCalcSpeedKmh = dynoTracker.velocityMs * 3.6f
      dynoTracker.finishReason = reason
      dynoTracker.state = DynoRunState.FINALIZADO
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

  // Listener GPS
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

            // Detecção de Desaceleração pelo GPS
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

  // Listener de Sensores (Acelerômetro Linear e Giroscópio)
  DisposableEffect(sensorManager, linearAccelerationSensor, gyroscopeSensor) {
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

                // Detecção de alteração física da posição do celular no suporte (após calibrar)
                if (isCalibrated && !isCalibrating && dynoTracker.state == DynoRunState.PARADO && currentGpsSpeedKmh < 3f) {
                  val gMag = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)
                  if (gMag > 2.2f) {
                    isCalibrated = false
                    hasPhoneMovedAfterCalib = true
                    calibrationStatusText = "O celular mudou de posição. Calibre novamente."
                    prefs.edit().putBoolean("is_calibrated", false).apply()
                  }
                }

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

                  // Detecção de desaceleração pelos sensores
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

                // Coleta de Calibração com Celular no Suporte
                if (calibCollector.isCollecting) {
                  val currentCount = calibCollector.count
                  if (currentCount > 10) {
                    val partialAvgX = (calibCollector.sumX / currentCount).toFloat()
                    val partialAvgY = (calibCollector.sumY / currentCount).toFloat()
                    val partialAvgZ = (calibCollector.sumZ / currentCount).toFloat()
                    val gyroMag = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)

                    if (abs(event.values[0] - partialAvgX) > 2.2f ||
                      abs(event.values[1] - partialAvgY) > 2.2f ||
                      abs(event.values[2] - partialAvgZ) > 2.2f ||
                      gyroMag > 2.0f) {
                      calibCollector.reset()
                      isCalibrating = false
                      calibrationStatusText = "O aparelho se moveu durante a calibração"
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
                    calibrationStatusText = "CALIBRANDO $calibProgressPercent%"

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
                      hasPhoneMovedAfterCalib = false
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

  // Estabilidade do celular e prontidão GPS
  val currentGyroMag = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)
  val isPhoneStable = currentGyroMag <= 0.65f
  val gpsStatusCategory = when {
    !hasLocationPermission -> "Permissão negada"
    !isGpsProviderEnabled -> "GPS desligado"
    !hasGpsFix -> "Aguardando GPS"
    gpsAccuracyM <= 8.0f -> "GPS Bom"
    gpsAccuracyM <= 15.0f -> "GPS Regular"
    else -> "GPS Fraco"
  }
  val isGpsReady = hasLocationPermission && isGpsProviderEnabled && hasGpsFix && gpsAccuracyM <= 15.0f
  val isVehicleMoving = currentGpsSpeedKmh >= 3.0f

  // Habilitação para Iniciar
  val isReadyToArm = isCalibrated && isGpsReady && isStoppedForTwoSeconds && !isVehicleMoving && isPhoneStable && runState == DynoRunState.PARADO

  // Motivo da desabilitação para exibir acima do botão INICIAR
  val disabledReasonText: String? = when {
    runState == DynoRunState.AGUARDANDO_INICIO -> "Acelere até ${startSpeedTriggerKmh.toInt()} km/h na marcha selecionada."
    runState == DynoRunState.MEDINDO -> "Passagem em andamento — aceleração plena."
    hasPhoneMovedAfterCalib -> "O celular mudou de posição. Calibre novamente."
    !isCalibrated -> "Calibre o celular antes de iniciar."
    isVehicleMoving -> "Aguarde o veículo parar (${String.format(Locale.US, "%.1f", currentGpsSpeedKmh)} km/h)."
    !isGpsReady -> "Procurando sinal GPS..."
    !isPhoneStable -> "Celular em movimento excessivo."
    else -> null
  }

  // Estado grande para o topo do velocímetro
  val (stateTitle, stateColor) = when {
    runState == DynoRunState.MEDINDO -> Pair("MEDINDO", DynoSuccessGreen)
    runState == DynoRunState.AGUARDANDO_INICIO -> Pair("TESTE ARMADO", DynoBlueLight)
    isCalibrating -> Pair("CALIBRANDO", DynoPowerCyan)
    hasPhoneMovedAfterCalib -> Pair("TESTE INVÁLIDO", DynoErrorRed)
    isReadyToArm -> Pair("PRONTO", DynoSuccessGreen)
    runState == DynoRunState.FINALIZADO -> Pair("FINALIZADO", DynoBlueLight)
    else -> Pair("PREPARE O TESTE", DynoTextSecondary)
  }

  // Instrução única dinâmica para a área direita
  val singleDynamicInstruction = when {
    hasPhoneMovedAfterCalib -> "O celular mudou de posição. Calibre novamente."
    !isCalibrated -> "Pare o veículo e calibre o celular no suporte."
    isCalibrating -> "Mantenha o veículo parado com o celular no suporte."
    runState == DynoRunState.PARADO -> "Tudo pronto. Toque em iniciar."
    runState == DynoRunState.AGUARDANDO_INICIO -> {
      if (currentGpsSpeedKmh >= startSpeedTriggerKmh - 8f) "Prepare-se."
      else "Acelere na marcha selecionada até ${startSpeedTriggerKmh.toInt()} km/h."
    }
    runState == DynoRunState.MEDINDO -> "Mantenha a aceleração na mesma marcha."
    runState == DynoRunState.FINALIZADO -> "Passagem finalizada. Salvando resultado."
    else -> "Pare o veículo e calibre o celular no suporte."
  }

  val totalWeight = remember(vehicle) {
    VehicleCalculations.calculateTotalWeight(
      curbWeightKg = vehicle.curbWeightKg,
      driverWeightKg = vehicle.driverWeightKg,
      passengerWeightKg = vehicle.passengerWeightKg,
      cargoWeightKg = vehicle.cargoWeightKg,
      audioWeightKg = vehicle.audioWeightKg,
      gnvWeightKg = vehicle.gnvWeightKg,
      otherWeightKg = vehicle.otherWeightKg,
      removedWeightKg = vehicle.removedWeightKg,
      measuredTotalWeightKg = vehicle.measuredTotalWeightKg,
      useMeasuredWeight = vehicle.useMeasuredWeight
    )
  }

  // Interceptação do botão Voltar do sistema
  BackHandler {
    if (runState == DynoRunState.AGUARDANDO_INICIO || runState == DynoRunState.MEDINDO) {
      showCancelConfirmDialog = true
    } else {
      onNavigateHomeOrBack(onNavigateToHome, onNavigateBack)
    }
  }

  // Diálogo de confirmação para cancelamento durante espera ou medição
  if (showCancelConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showCancelConfirmDialog = false },
      title = {
        Text(
          text = "Deseja cancelar este teste?",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = DynoTextPrimary
        )
      },
      text = {
        Text(
          text = "A medição em andamento será interrompida e nenhum resultado será gravado.",
          style = MaterialTheme.typography.bodyMedium,
          color = DynoTextSecondary
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showCancelConfirmDialog = false
            dynoTracker.reset()
            runState = DynoRunState.PARADO
            onNavigateHomeOrBack(onNavigateToHome, onNavigateBack)
          },
          colors = ButtonDefaults.buttonColors(containerColor = DynoErrorRed),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("CANCELAR TESTE", fontWeight = FontWeight.Bold, color = Color.White)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showCancelConfirmDialog = false },
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("CONTINUAR TESTE", fontWeight = FontWeight.SemiBold, color = DynoBlueLight)
        }
      },
      containerColor = DynoSurfaceContainer,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.testTag("dialog_cancel_test_confirm")
    )
  }

  // Diálogo para recalibrar
  if (showRecalibrateConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showRecalibrateConfirmDialog = false },
      title = {
        Text(
          text = "Calibrar novamente?",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = DynoTextPrimary
        )
      },
      text = {
        Text(
          text = "Deseja realizar uma nova leitura de vibração com o celular preso no suporte?",
          style = MaterialTheme.typography.bodyMedium,
          color = DynoTextSecondary
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showRecalibrateConfirmDialog = false
            if (!isVehicleMoving) {
              calibCollector.reset()
              calibCollector.isCollecting = true
              isCalibrating = true
              calibProgressPercent = 0
              calibrationStatusText = "CALIBRANDO 0%"
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = DynoSuccessGreen),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("CALIBRAR", fontWeight = FontWeight.Bold, color = DynoSurface)
        }
      },
      dismissButton = {
        TextButton(onClick = { showRecalibrateConfirmDialog = false }) {
          Text("VOLTAR", color = DynoTextSecondary)
        }
      },
      containerColor = DynoSurfaceContainer,
      shape = RoundedCornerShape(16.dp)
    )
  }

  // ESTRUTURA PRINCIPAL DO PAINEL HORIZONTAL
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DynoBackground)
      .safeDrawingPadding()
      .testTag("test_preparation_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 14.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // LINHA SUPERIOR: 3 ÁREAS PRINCIPAIS
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // =========================================================================
        // 1. ÁREA ESQUERDA: RESUMO DO VEÍCULO E GATILHO
        // =========================================================================
        Card(
          modifier = Modifier
            .weight(1.05f)
            .fillMaxHeight(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = DynoSurfaceContainer),
          border = BorderStroke(1.dp, DynoBorder)
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            // Identificação do Veículo
            Column {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.DirectionsCar,
                  contentDescription = null,
                  tint = DynoBlueLight,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "${vehicle.manufacturer} ${vehicle.model}",
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  ),
                  color = DynoTextPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              val subInfo = listOfNotNull(
                vehicle.engine.ifBlank { null },
                vehicle.version.ifBlank { null }
              ).joinToString(" • ")
              if (subInfo.isNotEmpty()) {
                Text(
                  text = subInfo,
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                  color = DynoTextSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            HorizontalDivider(thickness = 0.6.dp, color = DynoDivider)

            // Especificações Compactas
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text(
                  text = "PESO TOTAL",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                  ),
                  color = DynoTextSecondary
                )
                Text(
                  text = String.format(Locale.US, "%.0f kg", totalWeight),
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                  ),
                  color = DynoTextPrimary
                )
              }

              Column {
                Text(
                  text = "PNEU",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                  ),
                  color = DynoTextSecondary
                )
                Text(
                  text = "${vehicle.tireWidthMm}/${vehicle.tireAspectRatio} R${vehicle.wheelDiameterInches}",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                  ),
                  color = DynoTextPrimary
                )
              }

              Column {
                Text(
                  text = "MARCHA",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                  ),
                  color = DynoTextSecondary
                )
                Text(
                  text = "2ª",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                  ),
                  color = DynoPowerCyan
                )
              }
            }

            HorizontalDivider(thickness = 0.6.dp, color = DynoDivider)

            // Seletor de Velocidade de Início Automático [ 40 ] [ 50 ] [ 60 ] km/h
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "INÍCIO AUTOMÁTICO",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                ),
                color = DynoTextSecondary
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                listOf(40f, 50f, 60f).forEach { speed ->
                  val isSelected = startSpeedTriggerKmh == speed
                  val isSelectorEnabled = runState == DynoRunState.PARADO

                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) DynoBluePrimary else DynoSurfaceElevated,
                    border = BorderStroke(
                      1.dp,
                      if (isSelected) DynoBlueLight else DynoBorder
                    ),
                    modifier = Modifier
                      .weight(1f)
                      .height(34.dp)
                      .testTag("btn_speed_trigger_${speed.toInt()}")
                      .clickable(enabled = isSelectorEnabled) {
                        startSpeedTriggerKmh = speed
                        dynoTracker.startTriggerSpeedKmh = speed
                        prefs.edit().putFloat("start_speed_trigger_kmh", speed).apply()
                      }
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = "${speed.toInt()}",
                        style = MaterialTheme.typography.labelMedium.copy(
                          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                          fontSize = 12.5.sp
                        ),
                        color = if (isSelected) Color.White else DynoTextSecondary
                      )
                    }
                  }
                }

                Text(
                  text = "km/h",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                  ),
                  color = DynoTextSecondary
                )
              }
            }
          }
        }

        // =========================================================================
        // 2. ÁREA CENTRAL: VELOCÍMETRO SEMICIRCULAR GRANDE E ESTADO
        // =========================================================================
        Column(
          modifier = Modifier
            .weight(1.6f)
            .fillMaxHeight(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          // Estado Grande no Topo
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = stateColor.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, stateColor.copy(alpha = 0.55f)),
            modifier = Modifier.padding(top = 2.dp)
          ) {
            Text(
              text = stateTitle,
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                fontSize = 13.sp
              ),
              color = stateColor,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
          }

          // Velocímetro Central Semicircular Grande
          HorizontalDynoSpeedometer(
            currentSpeedKmh = if (runState == DynoRunState.MEDINDO) runVelocityMs * 3.6f else currentGpsSpeedKmh,
            targetTriggerSpeedKmh = startSpeedTriggerKmh,
            isMeasuring = runState == DynoRunState.MEDINDO,
            maxSpeedKmh = if (runState == DynoRunState.MEDINDO) max(dynoTracker.maxCalcSpeedKmh, runVelocityMs * 3.6f) else dynoTracker.maxGpsKmh,
            modifier = Modifier.weight(1f, fill = false)
          )

          // Aviso / Motivo de bloqueio ou instrução do gatilho logo acima do botão central
          if (disabledReasonText != null) {
            Text(
              text = disabledReasonText,
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp
              ),
              color = if (hasPhoneMovedAfterCalib || isVehicleMoving) DynoErrorRed else DynoTextSecondary,
              textAlign = TextAlign.Center,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(bottom = 2.dp)
            )
          } else {
            Spacer(modifier = Modifier.height(14.dp))
          }
        }

        // =========================================================================
        // 3. ÁREA DIREITA: QUALIDADE GPS, CALIBRAÇÃO, MÉTRICAS E INSTRUÇÃO
        // =========================================================================
        Card(
          modifier = Modifier
            .weight(1.05f)
            .fillMaxHeight(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = DynoSurfaceContainer),
          border = BorderStroke(1.dp, DynoBorder)
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            // Status do GPS e da Calibração
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = null,
                    tint = if (isGpsReady) DynoSuccessGreen else DynoWarningYellow,
                    modifier = Modifier.size(15.dp)
                  )
                  Text(
                    text = "GPS",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.sp
                    ),
                    color = DynoTextPrimary
                  )
                }
                Text(
                  text = if (isGpsReady) "$gpsStatusCategory (±${gpsAccuracyM.toInt()}m)" else gpsStatusCategory,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                  ),
                  color = if (isGpsReady) DynoSuccessGreen else DynoWarningYellow
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = if (isCalibrated) DynoSuccessGreen else DynoWarningYellow,
                    modifier = Modifier.size(15.dp)
                  )
                  Text(
                    text = "CALIBRAÇÃO",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.sp
                    ),
                    color = DynoTextPrimary
                  )
                }
                Text(
                  text = if (isCalibrated) "Calibrado ✓" else if (isCalibrating) "$calibProgressPercent%" else "Pendente",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                  ),
                  color = if (isCalibrated) DynoSuccessGreen else if (isCalibrating) DynoPowerCyan else DynoWarningYellow
                )
              }
            }

            HorizontalDivider(thickness = 0.6.dp, color = DynoDivider)

            // Métricas em Tempo Real da Passagem
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text(
                  text = "TEMPO",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                  ),
                  color = DynoTextSecondary
                )
                Text(
                  text = String.format(Locale.US, "%.2f s", runElapsedSeconds),
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.5.sp
                  ),
                  color = DynoTextPrimary
                )
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "VEL. MÁXIMA",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                  ),
                  color = DynoTextSecondary
                )
                val displayedMaxSpeed = if (runState == DynoRunState.MEDINDO) {
                  max(dynoTracker.maxCalcSpeedKmh, runVelocityMs * 3.6f)
                } else {
                  dynoTracker.maxGpsKmh
                }
                Text(
                  text = String.format(Locale.US, "%.1f km/h", displayedMaxSpeed),
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.5.sp
                  ),
                  color = DynoPowerCyan
                )
              }
            }

            HorizontalDivider(thickness = 0.6.dp, color = DynoDivider)

            // Instrução Dinâmica Única
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = DynoSurfaceElevated,
              border = BorderStroke(1.dp, DynoBorderLight),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Outlined.Info,
                  contentDescription = null,
                  tint = DynoBlueLight,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = singleDynamicInstruction,
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium
                  ),
                  color = DynoTextPrimary,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // =========================================================================
      // 4. TRÊS BOTÕES INFERIORES [ CALIBRAR ] [ INICIAR ] [ CANCELAR ]
      // =========================================================================
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // -------------------------------------------------------------
        // 5.1 BOTÃO ESQUERDO — CALIBRAR
        // -------------------------------------------------------------
        val isCalibrateEnabled = !isCalibrating && (runState == DynoRunState.PARADO)

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = if (isCalibrated) DynoSuccessGreen else DynoSurfaceContainer,
          border = BorderStroke(
            1.5.dp,
            if (isCalibrated) DynoSuccessGreen else if (isCalibrating) DynoPowerCyan else DynoSuccessGreen
          ),
          modifier = Modifier
            .weight(1f)
            .height(64.dp)
            .testTag("btn_calibrate")
            .clickable(enabled = isCalibrateEnabled) {
              if (isCalibrated) {
                showRecalibrateConfirmDialog = true
              } else if (!isVehicleMoving) {
                calibCollector.reset()
                calibCollector.isCollecting = true
                isCalibrating = true
                calibProgressPercent = 0
                calibrationStatusText = "CALIBRANDO 0%"
              }
            }
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = if (isCalibrated) Icons.Default.Check else Icons.Default.Tune,
                contentDescription = null,
                tint = if (isCalibrated) DynoSurface else DynoSuccessGreen,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = if (isCalibrating) "CALIBRANDO $calibProgressPercent%"
                       else if (isCalibrated) "✓ CALIBRADO"
                       else "CALIBRAR",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  letterSpacing = 0.5.sp
                ),
                color = if (isCalibrated) DynoSurface else DynoSuccessGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        // -------------------------------------------------------------
        // 5.2 BOTÃO CENTRAL — INICIAR (~20% MAIOR)
        // -------------------------------------------------------------
        val isStartButtonArmed = runState == DynoRunState.AGUARDANDO_INICIO
        val isStartButtonMeasuring = runState == DynoRunState.MEDINDO
        val isStartClickable = isReadyToArm && runState == DynoRunState.PARADO

        val startBtnText = when {
          isStartButtonMeasuring -> "MEDINDO"
          isStartButtonArmed -> "ARMADO — ${startSpeedTriggerKmh.toInt()} KM/H"
          else -> "INICIAR"
        }

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = if (isStartClickable || isStartButtonArmed || isStartButtonMeasuring) Color(0xFFE53935)
                  else Color(0xFF421516),
          border = BorderStroke(
            1.dp,
            if (isStartClickable || isStartButtonArmed || isStartButtonMeasuring) Color(0xFFFF5252)
            else Color(0xFF5C1B1A)
          ),
          modifier = Modifier
            .weight(1.35f)
            .height(74.dp)
            .testTag("btn_start_test")
            .clickable(enabled = isStartClickable) {
              if (isReadyToArm) {
                dynoTracker.reset()
                dynoTracker.state = DynoRunState.AGUARDANDO_INICIO
                dynoTracker.armedEstimatedSpeedMs = 0f
                dynoTracker.armedLastNanoTime = System.nanoTime()
                armedEstimatedSpeedKmh = 0f
                runElapsedSeconds = 0f
                runVelocityMs = 0f
                resultSaved = false
                runState = DynoRunState.AGUARDANDO_INICIO
              }
            }
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = when {
                  isStartButtonMeasuring -> Icons.Default.FiberManualRecord
                  isStartButtonArmed -> Icons.Default.Speed
                  else -> Icons.Default.PlayArrow
                },
                contentDescription = null,
                tint = if (isStartClickable || isStartButtonArmed || isStartButtonMeasuring) Color.White
                       else Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(26.dp)
              )
              Text(
                text = startBtnText,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Black,
                  fontSize = 17.sp,
                  letterSpacing = 0.6.sp
                ),
                color = if (isStartClickable || isStartButtonArmed || isStartButtonMeasuring) Color.White
                        else Color.White.copy(alpha = 0.45f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        // -------------------------------------------------------------
        // 5.3 BOTÃO DIREITO — CANCELAR
        // -------------------------------------------------------------
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = DynoSurfaceContainer,
          border = BorderStroke(1.5.dp, DynoBorderLight),
          modifier = Modifier
            .weight(1f)
            .height(64.dp)
            .testTag("btn_cancel_test")
            .clickable {
              if (runState == DynoRunState.AGUARDANDO_INICIO || runState == DynoRunState.MEDINDO) {
                showCancelConfirmDialog = true
              } else {
                onNavigateHomeOrBack(onNavigateToHome, onNavigateBack)
              }
            }
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = DynoTextPrimary,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = "CANCELAR",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  letterSpacing = 0.5.sp
                ),
                color = DynoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Velocímetro semicircular customizado para o painel horizontal
 */
@Composable
private fun HorizontalDynoSpeedometer(
  currentSpeedKmh: Float,
  targetTriggerSpeedKmh: Float,
  isMeasuring: Boolean,
  maxSpeedKmh: Float,
  modifier: Modifier = Modifier
) {
  val density = LocalDensity.current
  val visualSpeed = currentSpeedKmh.coerceAtLeast(0f)
  val progressFraction = (visualSpeed / 200f).coerceIn(0f, 1f)
  val progressSweep = progressFraction * 220f

  val primaryColor = if (isMeasuring) DynoSuccessGreen else DynoBluePrimary
  val triggerHighlightColor = DynoBlueLight
  val trackColor = DynoSurfaceElevated
  val normalTickColor = DynoTextMuted
  val normalTextColor = DynoTextSecondary.toArgb()
  val highlightTextColor = triggerHighlightColor.toArgb()

  val tickSteps = remember { listOf(0, 40, 80, 120, 160, 200) }

  val textPaint = remember {
    android.graphics.Paint().apply {
      isAntiAlias = true
      textAlign = android.graphics.Paint.Align.CENTER
    }
  }

  Box(
    modifier = modifier
      .size(270.dp, 165.dp)
      .semantics { contentDescription = "Velocidade GPS: ${visualSpeed.toInt()} km/h" }
      .testTag("dyno_speedometer"),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val canvasWidth = size.width
      val canvasHeight = size.height
      val centerX = canvasWidth / 2f
      val centerY = canvasHeight * 0.72f

      val arcRadius = with(density) { 84.dp.toPx() }
      val strokeWidthPx = with(density) { 11.dp.toPx() }
      val labelRadius = arcRadius + with(density) { 16.dp.toPx() }
      val tickInnerRadius = arcRadius - with(density) { 7.dp.toPx() }
      val tickOuterRadius = arcRadius - with(density) { 2.dp.toPx() }

      val arcRect = Rect(
        left = centerX - arcRadius,
        top = centerY - arcRadius,
        right = centerX + arcRadius,
        bottom = centerY + arcRadius
      )

      // 1. Trilho de Fundo
      drawArc(
        color = trackColor,
        startAngle = 160f,
        sweepAngle = 220f,
        useCenter = false,
        topLeft = arcRect.topLeft,
        size = arcRect.size,
        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
      )

      // 2. Arco Dinâmico
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

      // 3. Marcadores de Escala e Números
      val labelTextSizePx = with(density) { 9.5.sp.toPx() }
      textPaint.textSize = labelTextSizePx

      tickSteps.forEach { step ->
        val stepFraction = step / 200f
        val stepAngleDeg = 160f + stepFraction * 220f
        val stepAngleRad = Math.toRadians(stepAngleDeg.toDouble())

        val cosA = cos(stepAngleRad).toFloat()
        val sinA = sin(stepAngleRad).toFloat()

        val isTrigger = step == targetTriggerSpeedKmh.toInt()

        val tOuter = if (isTrigger) arcRadius else tickOuterRadius
        val tInner = if (isTrigger) arcRadius - with(density) { 10.dp.toPx() } else tickInnerRadius
        val tColor = if (isTrigger) triggerHighlightColor else normalTickColor
        val tStroke = with(density) { (if (isTrigger) 2.2.dp else 1.dp).toPx() }

        drawLine(
          color = tColor,
          start = Offset(centerX + tInner * cosA, centerY + tInner * sinA),
          end = Offset(centerX + tOuter * cosA, centerY + tOuter * sinA),
          strokeWidth = tStroke,
          cap = StrokeCap.Round
        )

        val lx = centerX + labelRadius * cosA
        val ly = centerY + labelRadius * sinA + (labelTextSizePx * 0.35f)

        drawIntoCanvas { canvas ->
          textPaint.color = if (isTrigger) highlightTextColor else normalTextColor
          textPaint.typeface = if (isTrigger) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
          canvas.nativeCanvas.drawText(step.toString(), lx, ly, textPaint)
        }
      }

      // 4. Marcador do Gatilho Selecionado (Ponto destacado)
      if (targetTriggerSpeedKmh > 0f) {
        val trigFraction = (targetTriggerSpeedKmh / 200f).coerceIn(0f, 1f)
        val trigAngleDeg = 160f + trigFraction * 220f
        val trigAngleRad = Math.toRadians(trigAngleDeg.toDouble())
        val tx = centerX + arcRadius * cos(trigAngleRad).toFloat()
        val ty = centerY + arcRadius * sin(trigAngleRad).toFloat()
        drawCircle(
          color = if (isMeasuring) DynoPowerCyan else DynoBlueLight,
          radius = with(density) { 3.5.dp.toPx() },
          center = Offset(tx, ty)
        )
      }

      // 5. Marcador da Velocidade Máxima Atingida
      if (maxSpeedKmh > 5f) {
        val maxFraction = (maxSpeedKmh / 200f).coerceIn(0f, 1f)
        val maxAngleDeg = 160f + maxFraction * 220f
        val maxAngleRad = Math.toRadians(maxAngleDeg.toDouble())
        val mx = centerX + arcRadius * cos(maxAngleRad).toFloat()
        val my = centerY + arcRadius * sin(maxAngleRad).toFloat()
        drawCircle(
          color = DynoTorqueOrange,
          radius = with(density) { 4.dp.toPx() },
          center = Offset(mx, my)
        )
      }
    }

    // Centro do Velocímetro: Velocidade GPS Grande e "km/h"
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .align(Alignment.Center)
        .padding(top = 18.dp)
    ) {
      val formattedSpeed = if (visualSpeed < 10f && visualSpeed > 0f && (visualSpeed % 1.0f != 0f)) {
        String.format(Locale.US, "%.1f", visualSpeed)
      } else {
        String.format(Locale.US, "%.0f", visualSpeed)
      }

      Text(
        text = formattedSpeed,
        style = MaterialTheme.typography.displayLarge.copy(
          fontWeight = FontWeight.Black,
          fontSize = 48.sp,
          fontFamily = FontFamily.Monospace,
          letterSpacing = (-1).sp
        ),
        color = DynoTextPrimary,
        modifier = Modifier.padding(0.dp)
      )

      Text(
        text = "km/h",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Medium,
          fontSize = 13.5.sp
        ),
        color = DynoTextSecondary
      )
    }
  }
}

/**
 * Função utilitária para retorno seguro à Home ou voltar
 */
private fun onNavigateHomeOrBack(onNavigateToHome: () -> Unit, onNavigateBack: () -> Unit) {
  try {
    onNavigateToHome()
  } catch (e: Exception) {
    onNavigateBack()
  }
}
