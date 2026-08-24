package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Sensors
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.DynoRunState
import com.example.model.FinishReason
import com.example.model.VehicleCalculations
import com.example.model.VehicleProfile
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Gerenciador centralizado de bloqueio de orientação de tela.
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
 * Implementa arquitetura de 3 velocidades (GPS, Integrada, Exibida) e sincronização GPS por interpolação de timestamp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestPreparationScreen(
  vehicle: VehicleProfile,
  onNavigateToHome: (saved: Boolean) -> Unit,
  onSwitchVehicle: () -> Unit,
  onEditVehicle: () -> Unit,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: TestPreparationViewModel = viewModel()
) {
  // 1. BLOQUEIO CENTRALIZADO DE ORIENTAÇÃO HORIZONTAL
  LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)

  val context = LocalContext.current
  val activity = remember(context) {
    generateSequence(context) { current ->
      (current as? ContextWrapper)?.baseContext
    }.filterIsInstance<Activity>().firstOrNull()
  }

  // 2. MODO IMERSIVO E MANTER TELA ACESA
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

  // Permissões de localização e início dos sensores
  var hasLocationPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
    hasLocationPermission = fineGranted || coarseGranted
    if (hasLocationPermission) {
      viewModel.startLocationUpdates()
    }
  }

  LaunchedEffect(Unit) {
    if (!hasLocationPermission) {
      permissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION
        )
      )
    } else {
      viewModel.startLocationUpdates()
    }
    viewModel.startSensorUpdates()
    viewModel.setOnRunCompletedCallback { saved ->
      onNavigateToHome(saved)
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      viewModel.stopLocationUpdates()
      viewModel.stopSensorUpdates()
    }
  }

  // Observa o estado unificado do ViewModel via StateFlow
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  var showCancelConfirmDialog by remember { mutableStateOf(false) }
  var showRecalibrateConfirmDialog by remember { mutableStateOf(false) }
  var showAdvancedDiagnosticDialog by remember { mutableStateOf(false) }

  val isVehicleMoving = uiState.gpsSpeedKmh >= 3.0f
  val isReadyToArm = uiState.isCalibrated && uiState.isGpsReady && uiState.isStoppedForTwoSeconds && !isVehicleMoving && uiState.isPhoneStable && uiState.testState == DynoRunState.PARADO

  // 9. STATUS DO GPS POR IDADE
  val gpsStatusLabel = when {
    !hasLocationPermission -> "Sem permissão"
    !uiState.isGpsReady -> "Aguardando GPS"
    uiState.gpsAgeMillis <= 600L -> "GPS Bom"
    uiState.gpsAgeMillis <= 1500L -> "GPS Atualizando"
    else -> "GPS com atraso"
  }

  val gpsStatusColor = when {
    !hasLocationPermission -> DynoErrorRed
    !uiState.isGpsReady -> DynoWarningYellow
    uiState.gpsAgeMillis <= 600L -> DynoSuccessGreen
    uiState.gpsAgeMillis <= 1500L -> DynoPowerCyan
    else -> DynoWarningYellow
  }

  // Título e cor de estado no topo do velocímetro
  val (stateTitle, stateColor) = when {
    uiState.testState == DynoRunState.MEDINDO_PROTEGIDO || uiState.testState == DynoRunState.MEDINDO -> Pair("MEDINDO", DynoSuccessGreen)
    uiState.testState == DynoRunState.SUSPEITA_DESACELERACAO -> Pair("MEDINDO (FINALIZANDO)", DynoTorqueOrange)
    uiState.testState == DynoRunState.AGUARDANDO_INICIO -> Pair("TESTE ARMADO", DynoBlueLight)
    uiState.isCalibrating -> Pair("CALIBRANDO", DynoPowerCyan)
    isReadyToArm -> Pair("PRONTO PARA INICIAR", DynoSuccessGreen)
    uiState.testState == DynoRunState.FINALIZADO -> Pair("FINALIZADO", DynoBlueLight)
    !uiState.isCalibrated -> Pair("PREPARE O TESTE", DynoTextSecondary)
    else -> Pair("PREPARE O TESTE", DynoTextSecondary)
  }

  // Instrução única dinâmica
  val singleDynamicInstruction = when {
    uiState.hasPhoneMovedAfterCalib -> "O celular mudou de posição. Calibre novamente."
    !uiState.isCalibrated -> "Calibre o celular para continuar."
    uiState.isCalibrating -> "Mantenha o veículo parado com o celular no suporte."
    uiState.testState == DynoRunState.PARADO && isReadyToArm -> "Tudo pronto. Toque em iniciar."
    uiState.testState == DynoRunState.PARADO && isVehicleMoving -> "Aguarde o veículo parar completamente."
    uiState.testState == DynoRunState.PARADO && !uiState.isGpsReady -> "Aguardando sinal GPS..."
    uiState.testState == DynoRunState.AGUARDANDO_INICIO -> {
      if (uiState.gpsSpeedKmh >= uiState.startSpeedTriggerKmh - 8f) "Prepare-se."
      else "Acelere na marcha selecionada até ${uiState.startSpeedTriggerKmh.toInt()} km/h."
    }
    uiState.testState == DynoRunState.MEDINDO_PROTEGIDO || uiState.testState == DynoRunState.MEDINDO -> "Mantenha a aceleração na mesma marcha."
    uiState.testState == DynoRunState.SUSPEITA_DESACELERACAO -> "Desaceleração detectada — aguardando confirmação."
    uiState.testState == DynoRunState.FINALIZADO -> "Passagem finalizada. Salvando resultado."
    else -> "Calibre o celular para continuar."
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
    if (uiState.testState == DynoRunState.AGUARDANDO_INICIO ||
      uiState.testState == DynoRunState.MEDINDO_PROTEGIDO ||
      uiState.testState == DynoRunState.MEDINDO) {
      showCancelConfirmDialog = true
    } else {
      onNavigateHomeOrBack(onNavigateToHome, onNavigateBack)
    }
  }

  // Diálogo de confirmação para cancelamento
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
            viewModel.cancelTest()
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
              viewModel.startCalibration()
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

  // 12. DIÁLOGO DE DIAGNÓSTICO AVANÇADO
  if (showAdvancedDiagnosticDialog) {
    AdvancedDiagnosticDialog(
      uiState = uiState,
      onDismiss = { showAdvancedDiagnosticDialog = false }
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
        .padding(horizontal = 14.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // LINHA SUPERIOR: 3 ÁREAS PRINCIPAIS (25% ESQUERDA, 50% CENTRO, 25% DIREITA)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // =========================================================================
        // 1. ÁREA ESQUERDA: RESUMO COMPACTO DO VEÍCULO E GATILHO (25%)
        // =========================================================================
        Card(
          modifier = Modifier
            .weight(0.25f)
            .fillMaxHeight(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = DynoSurfaceContainer),
          border = BorderStroke(1.dp, DynoBorder)
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            // Identificação do Veículo com botão Trocar Veículo
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "${vehicle.manufacturer} ${vehicle.model}".trim(),
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                  ),
                  color = DynoTextPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f)
                )

                Text(
                  text = "TROCAR",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = DynoBlueLight
                  ),
                  modifier = Modifier
                    .clickable { onSwitchVehicle() }
                    .padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
                    .testTag("btn_switch_vehicle_compact")
                )
              }

              val subInfo = listOfNotNull(
                vehicle.engine.ifBlank { null },
                vehicle.version.ifBlank { null }
              ).joinToString(" • ")
              if (subInfo.isNotEmpty()) {
                Text(
                  text = subInfo,
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.5.sp,
                    color = DynoTextSecondary
                  ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            HorizontalDivider(thickness = 0.5.dp, color = DynoDivider)

            // Especificações Compactas
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "PESO",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                  ),
                  color = DynoTextSecondary
                )
                Text(
                  text = String.format(Locale.US, "%.0f kg", totalWeight),
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                  ),
                  color = DynoTextPrimary
                )
              }

              Column {
                Text(
                  text = "PNEU",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                  ),
                  color = DynoTextSecondary
                )
                Text(
                  text = "${vehicle.tireWidthMm}/${vehicle.tireAspectRatio} R${vehicle.wheelDiameterInches}",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                  ),
                  color = DynoTextPrimary
                )
              }

              Column {
                Text(
                  text = "MARCHA",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                  ),
                  color = DynoTextSecondary
                )
                Text(
                  text = "2ª",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                  ),
                  color = DynoPowerCyan
                )
              }
            }

            HorizontalDivider(thickness = 0.5.dp, color = DynoDivider)

            // Seletor de Velocidade de Início Automático [ 40 ] [ 50 ] [ 60 ]
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Text(
                text = "INÍCIO AUTOMÁTICO",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.4.sp
                ),
                color = DynoTextSecondary
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                listOf(40f, 50f, 60f).forEach { speed ->
                  val isSelected = uiState.startSpeedTriggerKmh == speed
                  val isSelectorEnabled = uiState.testState == DynoRunState.PARADO

                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) DynoBluePrimary else DynoSurfaceElevated,
                    border = BorderStroke(
                      1.dp,
                      if (isSelected) DynoBlueLight else DynoBorder
                    ),
                    modifier = Modifier
                      .weight(1f)
                      .height(30.dp)
                      .testTag("btn_speed_trigger_${speed.toInt()}")
                      .clickable(enabled = isSelectorEnabled) {
                        viewModel.setStartSpeedTrigger(speed)
                      }
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = "${speed.toInt()}",
                        style = MaterialTheme.typography.labelMedium.copy(
                          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                          fontSize = 12.sp
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
                    fontSize = 10.sp
                  ),
                  color = DynoTextSecondary
                )
              }
            }
          }
        }

        // =========================================================================
        // 2. ÁREA CENTRAL: VELOCÍMETRO AMPLIADO (+30%) E STATUS LIMPO (50%)
        // =========================================================================
        Column(
          modifier = Modifier
            .weight(0.50f)
            .fillMaxHeight(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          // Status Único no Topo
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = stateColor.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, stateColor.copy(alpha = 0.55f)),
            modifier = Modifier.padding(top = 1.dp)
          ) {
            Text(
              text = stateTitle,
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                fontSize = 13.sp
              ),
              color = stateColor,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp)
            )
          }

          // Velocímetro Central Semicircular mostrando a velocidade suave exibida (displaySpeedKmh)
          HorizontalDynoSpeedometer(
            currentSpeedKmh = uiState.displaySpeedKmh,
            currentG = uiState.longitudinalG,
            targetTriggerSpeedKmh = uiState.startSpeedTriggerKmh,
            isMeasuring = uiState.testState == DynoRunState.MEDINDO_PROTEGIDO || uiState.testState == DynoRunState.MEDINDO,
            maxSpeedKmh = uiState.maxGpsSpeedKmh,
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
          )

          // Instrução Única Abaixo do Velocímetro
          Text(
            text = singleDynamicInstruction,
            style = MaterialTheme.typography.bodySmall.copy(
              fontWeight = FontWeight.SemiBold,
              fontSize = 12.sp
            ),
            color = if (uiState.hasPhoneMovedAfterCalib) DynoErrorRed else DynoTextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 2.dp)
          )
        }

        // =========================================================================
        // 3. ÁREA DIREITA: CARTÃO COMPACTO "CONDIÇÕES DO TESTE" (25%)
        // =========================================================================
        Card(
          modifier = Modifier
            .weight(0.25f)
            .fillMaxHeight(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = DynoSurfaceContainer),
          border = BorderStroke(1.dp, DynoBorder)
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "CONDIÇÕES DO TESTE",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp,
                  fontSize = 9.5.sp
                ),
                color = DynoTextSecondary
              )

              // Botão de Diagnóstico Avançado
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = DynoSurfaceElevated,
                border = BorderStroke(0.5.dp, DynoBorderLight),
                modifier = Modifier
                  .clickable { showAdvancedDiagnosticDialog = true }
                  .testTag("btn_open_diagnostics")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = DynoBlueLight,
                    modifier = Modifier.size(11.dp)
                  )
                  Text(
                    text = "DIAG",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 8.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = DynoBlueLight
                    )
                  )
                }
              }
            }

            HorizontalDivider(thickness = 0.5.dp, color = DynoDivider)

            // Linhas de Diagnóstico: GPS, Calibração, Veículo, Celular
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              // 1. GPS (com idade em tempo real)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "GPS:",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                  color = DynoTextSecondary
                )
                Text(
                  text = gpsStatusLabel,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                  color = gpsStatusColor
                )
              }

              // 2. Calibração
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Calibração:",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                  color = DynoTextSecondary
                )
                Text(
                  text = if (uiState.isCalibrated) "Concluída" else if (uiState.isCalibrating) "${uiState.calibProgressPercent}%" else "Pendente",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                  color = if (uiState.isCalibrated) DynoSuccessGreen else if (uiState.isCalibrating) DynoPowerCyan else DynoWarningYellow
                )
              }

              // 3. Veículo
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Veículo:",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                  color = DynoTextSecondary
                )
                Text(
                  text = if (!isVehicleMoving) "Parado" else "Em movimento",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                  color = if (!isVehicleMoving) DynoSuccessGreen else DynoWarningYellow
                )
              }

              // 4. Celular
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Celular:",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                  color = DynoTextSecondary
                )
                Text(
                  text = if (uiState.hasPhoneMovedAfterCalib) "Recalibre" else if (uiState.isCalibrated) "Pronto" else "Aguardando",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                  color = if (uiState.hasPhoneMovedAfterCalib) DynoErrorRed else if (uiState.isCalibrated) DynoSuccessGreen else DynoWarningYellow
                )
              }
            }

            // Métricas da passagem (aparecem quando armado ou medindo)
            if (uiState.testState == DynoRunState.AGUARDANDO_INICIO ||
              uiState.testState == DynoRunState.MEDINDO_PROTEGIDO ||
              uiState.testState == DynoRunState.MEDINDO ||
              uiState.testState == DynoRunState.SUSPEITA_DESACELERACAO) {
              HorizontalDivider(thickness = 0.5.dp, color = DynoDivider)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = String.format(Locale.US, "%.1fs", uiState.runElapsedSeconds),
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = DynoTextPrimary
                  )
                )

                val displayedMax = uiState.maxGpsSpeedKmh
                Text(
                  text = String.format(Locale.US, "Máx: %.0f km/h", displayedMax),
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = DynoPowerCyan
                  )
                )
              }
            } else {
              Spacer(modifier = Modifier.height(2.dp))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // =========================================================================
      // 4. TRÊS BOTÕES INFERIORES [ CALIBRAR ] [ INICIAR ] [ CANCELAR ] (74dp)
      // Proporções: Calibrar: 30%, Iniciar: 40%, Cancelar: 30%
      // =========================================================================
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(74.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // -------------------------------------------------------------
        // 5.1 BOTÃO ESQUERDO — CALIBRAR (30%)
        // -------------------------------------------------------------
        val isCalibrateEnabled = !uiState.isCalibrating && (uiState.testState == DynoRunState.PARADO)

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (uiState.isCalibrated) DynoSuccessGreen else DynoSurfaceContainer,
          border = BorderStroke(
            1.5.dp,
            if (uiState.isCalibrated) DynoSuccessGreen else if (uiState.isCalibrating) DynoPowerCyan else DynoSuccessGreen
          ),
          modifier = Modifier
            .weight(0.30f)
            .fillMaxHeight()
            .testTag("btn_calibrate")
            .clickable(enabled = isCalibrateEnabled) {
              if (uiState.isCalibrated) {
                showRecalibrateConfirmDialog = true
              } else if (!isVehicleMoving) {
                viewModel.startCalibration()
              }
            }
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = if (uiState.isCalibrated) Icons.Default.Check else Icons.Default.Tune,
                contentDescription = null,
                tint = if (uiState.isCalibrated) DynoSurface else DynoSuccessGreen,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = if (uiState.isCalibrating) "CALIBRANDO ${uiState.calibProgressPercent}%"
                       else if (uiState.isCalibrated) "CALIBRADO"
                       else "CALIBRAR",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  letterSpacing = 0.5.sp
                ),
                color = if (uiState.isCalibrated) DynoSurface else DynoSuccessGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        // -------------------------------------------------------------
        // 5.2 BOTÃO CENTRAL — INICIAR (40%)
        // -------------------------------------------------------------
        val isStartButtonArmed = uiState.testState == DynoRunState.AGUARDANDO_INICIO
        val isStartButtonMeasuring = uiState.testState == DynoRunState.MEDINDO_PROTEGIDO ||
          uiState.testState == DynoRunState.MEDINDO ||
          uiState.testState == DynoRunState.SUSPEITA_DESACELERACAO
        val isStartClickable = isReadyToArm && uiState.testState == DynoRunState.PARADO

        val startBtnText = when {
          isStartButtonMeasuring -> "MEDINDO"
          isStartButtonArmed -> "ARMADO — ${uiState.startSpeedTriggerKmh.toInt()} KM/H"
          !uiState.isCalibrated -> "CALIBRE PRIMEIRO"
          isVehicleMoving -> "PARE O CARRO"
          !uiState.isGpsReady -> "AGUARDANDO GPS"
          else -> "INICIAR"
        }

        val startBtnColor = when {
          isStartClickable || isStartButtonArmed || isStartButtonMeasuring -> Color(0xFFE53935)
          else -> Color(0xFF331617)
        }
        val startBtnBorder = when {
          isStartClickable || isStartButtonArmed || isStartButtonMeasuring -> Color(0xFFFF5252)
          else -> Color(0xFF4A1E20)
        }

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = startBtnColor,
          border = BorderStroke(1.dp, startBtnBorder),
          modifier = Modifier
            .weight(0.40f)
            .fillMaxHeight()
            .testTag("btn_start_test")
            .clickable(enabled = isStartClickable) {
              if (isReadyToArm) {
                viewModel.armTest()
              }
            }
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = when {
                  isStartButtonMeasuring -> Icons.Default.FiberManualRecord
                  isStartButtonArmed -> Icons.Default.Speed
                  isStartClickable -> Icons.Default.PlayArrow
                  else -> Icons.Default.Lock
                },
                contentDescription = null,
                tint = if (isStartClickable || isStartButtonArmed || isStartButtonMeasuring) Color.White
                       else Color.White.copy(alpha = 0.40f),
                modifier = Modifier.size(24.dp)
              )
              Text(
                text = startBtnText,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Black,
                  fontSize = if (startBtnText.length > 12) 13.5.sp else 16.sp,
                  letterSpacing = 0.5.sp
                ),
                color = if (isStartClickable || isStartButtonArmed || isStartButtonMeasuring) Color.White
                        else Color.White.copy(alpha = 0.40f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        // -------------------------------------------------------------
        // 5.3 BOTÃO DIREITO — CANCELAR (30%)
        // -------------------------------------------------------------
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = DynoSurfaceContainer,
          border = BorderStroke(1.5.dp, DynoBorderLight),
          modifier = Modifier
            .weight(0.30f)
            .fillMaxHeight()
            .testTag("btn_cancel_test")
            .clickable {
              if (uiState.testState == DynoRunState.AGUARDANDO_INICIO ||
                uiState.testState == DynoRunState.MEDINDO_PROTEGIDO ||
                uiState.testState == DynoRunState.MEDINDO) {
                showCancelConfirmDialog = true
              } else {
                onNavigateHomeOrBack(onNavigateToHome, onNavigateBack)
              }
            }
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
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
 * 12. DIÁLOGO DE DIAGNÓSTICO AVANÇADO
 * Exibe métricas técnicas de tempo real:
 * - Velocidade GPS real (gpsSpeedKmh)
 * - Velocidade Integrada (integratedSpeedKmh)
 * - Velocidade Exibida (displaySpeedKmh)
 * - Idade da última Location (gpsAgeMillis)
 * - Frequência real GPS (gpsFrequencyHz)
 * - Contagem de Locations novas (locationUpdateCount)
 * - Timestamp GPS (gpsTimestamp)
 * - Contagem de pares sincronizados (syncPairsCount)
 */
@Composable
private fun AdvancedDiagnosticDialog(
  uiState: DynoUiState,
  onDismiss: () -> Unit
) {
  val dateFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
  val formattedGpsTime = if (uiState.gpsTimestamp > 0L) dateFormat.format(Date(uiState.gpsTimestamp)) else "--:--:--"

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Default.Analytics, contentDescription = null, tint = DynoBlueLight)
        Text("Diagnóstico Técnico em Tempo Real", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        DiagnosticItemRow("Velocidade GPS Real", String.format(Locale.US, "%.1f km/h", uiState.gpsSpeedKmh), DynoBlueLight)
        DiagnosticItemRow("Velocidade Integrada", String.format(Locale.US, "%.1f km/h", uiState.integratedSpeedKmh), DynoPowerCyan)
        DiagnosticItemRow("Velocidade Exibida (Painel)", String.format(Locale.US, "%.1f km/h", uiState.displaySpeedKmh), DynoTextPrimary)
        DiagnosticItemRow("Força G Longitudinal", String.format(Locale.US, "%+.2f G", uiState.longitudinalG), DynoPowerCyan)
        DiagnosticItemRow("Pico G na Passagem", String.format(Locale.US, "%.2f G", uiState.peakLongitudinalG), DynoPowerCyan)
        HorizontalDivider(thickness = 0.5.dp, color = DynoDivider)

        DiagnosticItemRow("Idade da última Location", "${uiState.gpsAgeMillis} ms", if (uiState.gpsAgeMillis <= 600L) DynoSuccessGreen else DynoWarningYellow)
        DiagnosticItemRow("Frequência Real GPS", String.format(Locale.US, "%.1f Hz", uiState.gpsFrequencyHz), DynoTextPrimary)
        DiagnosticItemRow("Precisão GPS", String.format(Locale.US, "±%.1f m", uiState.gpsAccuracyMeters), if (uiState.gpsAccuracyMeters <= 8f) DynoSuccessGreen else DynoWarningYellow)
        DiagnosticItemRow("Contagem de Locations", "${uiState.locationUpdateCount}", DynoTextPrimary)
        DiagnosticItemRow("Timestamp GPS", formattedGpsTime, DynoTextSecondary)
        HorizontalDivider(thickness = 0.5.dp, color = DynoDivider)

        DiagnosticItemRow("Pares Sincronizados", "${uiState.syncPairsCount}", if (uiState.syncPairsCount >= 4) DynoSuccessGreen else DynoTextSecondary)
        DiagnosticItemRow("Diferença Média Sincr.", String.format(Locale.US, "±%.1f km/h", uiState.averageSyncDiffKmh), if (uiState.averageSyncDiffKmh <= 6f) DynoSuccessGreen else DynoWarningYellow)
        DiagnosticItemRow("Maior Diferença Sincr.", String.format(Locale.US, "%.1f km/h", uiState.maxSyncDiffKmh), DynoTextSecondary)
        DiagnosticItemRow("Máx GPS na Passagem", String.format(Locale.US, "%.1f km/h", uiState.maxGpsSpeedKmh), DynoBlueLight)
        DiagnosticItemRow("Máx Calculada na Passagem", String.format(Locale.US, "%.1f km/h", uiState.maxIntegratedSpeedKmh), DynoPowerCyan)
      }
    },
    confirmButton = {
      Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
        Text("FECHAR")
      }
    },
    containerColor = DynoSurfaceContainer,
    shape = RoundedCornerShape(16.dp)
  )
}

@Composable
private fun DiagnosticItemRow(label: String, value: String, valueColor: Color) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = DynoTextSecondary)
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
      color = valueColor
    )
  }
}

/**
 * Velocímetro semicircular customizado para o painel horizontal
 */
@Composable
private fun HorizontalDynoSpeedometer(
  currentSpeedKmh: Float,
  currentG: Float = 0f,
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
      .fillMaxWidth()
      .height(180.dp)
      .semantics { contentDescription = "Velocidade GPS: ${visualSpeed.toInt()} km/h" }
      .testTag("dyno_speedometer"),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val canvasWidth = size.width
      val canvasHeight = size.height
      val centerX = canvasWidth / 2f
      val centerY = canvasHeight * 0.76f

      val arcRadius = with(density) { 98.dp.toPx() }
      val strokeWidthPx = with(density) { 13.dp.toPx() }
      val labelRadius = arcRadius + with(density) { 18.dp.toPx() }
      val tickInnerRadius = arcRadius - with(density) { 9.dp.toPx() }
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
      val labelTextSizePx = with(density) { 11.sp.toPx() }
      textPaint.textSize = labelTextSizePx

      tickSteps.forEach { step ->
        val stepFraction = step / 200f
        val stepAngleDeg = 160f + stepFraction * 220f
        val stepAngleRad = Math.toRadians(stepAngleDeg.toDouble())

        val cosA = cos(stepAngleRad).toFloat()
        val sinA = sin(stepAngleRad).toFloat()

        val isTrigger = step == targetTriggerSpeedKmh.toInt()

        val tOuter = if (isTrigger) arcRadius else tickOuterRadius
        val tInner = if (isTrigger) arcRadius - with(density) { 12.dp.toPx() } else tickInnerRadius
        val tColor = if (isTrigger) triggerHighlightColor else normalTickColor
        val tStroke = with(density) { (if (isTrigger) 2.5.dp else 1.2.dp).toPx() }

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
          radius = with(density) { 4.5.dp.toPx() },
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
          radius = with(density) { 5.dp.toPx() },
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
          fontSize = 58.sp,
          fontFamily = FontFamily.Monospace,
          letterSpacing = (-1.5).sp
        ),
        color = DynoTextPrimary,
        modifier = Modifier.padding(0.dp)
      )

      Text(
        text = "km/h",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        ),
        color = DynoTextSecondary
      )

      if (isMeasuring || abs(currentG) >= 0.05f) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = DynoSurfaceElevated,
          border = BorderStroke(0.5.dp, DynoPowerCyan.copy(alpha = 0.5f)),
          modifier = Modifier.padding(top = 2.dp)
        ) {
          Text(
            text = String.format(Locale.US, "%+.2f G", currentG),
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp,
              color = DynoPowerCyan
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
          )
        }
      }
    }
  }
}

/**
 * Função utilitária para retorno seguro à Home ou voltar
 */
private fun onNavigateHomeOrBack(onNavigateToHome: (Boolean) -> Unit, onNavigateBack: () -> Unit) {
  try {
    onNavigateToHome(false)
  } catch (e: Exception) {
    onNavigateBack()
  }
}
