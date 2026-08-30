package com.example.ui.screens

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.DynoRunState
import com.example.ui.components.DynoMetricCard
import com.example.ui.theme.DynoBg
import com.example.ui.theme.DynoCardBg
import com.example.ui.theme.DynoCardBorder
import com.example.ui.theme.DynoCardSurface
import com.example.ui.theme.DynoErrorRed
import com.example.ui.theme.DynoPowerCyan
import com.example.ui.theme.DynoRed
import com.example.ui.theme.DynoSuccessGreen
import com.example.ui.theme.DynoTextMuted
import com.example.ui.theme.DynoTextPrimary
import com.example.ui.theme.DynoTextSecondary
import com.example.ui.theme.DynoTorqueAmber
import com.example.ui.theme.DynoWarningYellow
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestPreparationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToResults: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext as Application
    val viewModel: TestPreparationViewModel = viewModel(
        factory = TestPreparationViewModel.Factory(context)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    DisposableEffect(Unit) {
        viewModel.startSensorsAndGps()
        onDispose {
            viewModel.stopSensorsAndGps()
        }
    }

    LaunchedEffect(uiState.lastCompletedResultId) {
        uiState.lastCompletedResultId?.let { resultId ->
            onNavigateToResults(resultId)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DynoBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MODO DINAMÔMETRO REAL",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = DynoTextPrimary
                        )
                        Text(
                            text = uiState.activeVehicle?.name ?: "Nenhum veículo selecionado",
                            style = MaterialTheme.typography.labelSmall,
                            color = DynoPowerCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = DynoTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DynoBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Velocímetro Central Circular
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .clip(CircleShape)
                    .background(DynoCardSurface)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val speed = uiState.displaySpeedKmh
                val maxSpeed = 220f
                val progress = (speed / maxSpeed).coerceIn(0f, 1f)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Arco de fundo
                    drawArc(
                        color = Color(0xFF1E2837),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Arco de velocidade
                    drawArc(
                        color = if (uiState.testState == DynoRunState.MEDINDO) DynoRed else DynoPowerCyan,
                        startAngle = 135f,
                        sweepAngle = 270f * progress,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale.US, "%.0f", speed),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = DynoTextPrimary
                    )
                    Text(
                        text = "KM/H",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = DynoTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${uiState.liveRpm} RPM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = DynoPowerCyan
                    )
                }
            }

            // Diagnóstico e Instrução Dinâmica
            if (uiState.userMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DynoCardSurface),
                    border = BorderStroke(1.dp, DynoTorqueAmber)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = uiState.userMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = DynoTorqueAmber,
                            textAlign = TextAlign.Center
                        )
                        if (uiState.diagnosticError != null) {
                            Text(
                                text = uiState.diagnosticError ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = DynoTextSecondary
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DynoCardBg),
                border = BorderStroke(1.dp, DynoCardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (uiState.testState == DynoRunState.PARADO) {
                        if (uiState.isReadyToArm) {
                            Text(
                                text = "Tudo pronto. Toque em iniciar.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = DynoSuccessGreen,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "Bloqueio atual: ${uiState.blockingReason}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = DynoWarningYellow,
                                textAlign = TextAlign.Center
                            )
                        }

                        Text(
                            text = "GPS: ${String.format(Locale.US, "%.1f", uiState.gpsSpeedKmh)} km/h (Média: ${String.format(Locale.US, "%.1f", uiState.avgGpsSpeedKmh)}) | ±${String.format(Locale.US, "%.1f", uiState.gpsAccuracyMeters)}m | ${uiState.gpsAgeMillis}ms | ${if (uiState.vehicleMotionState == VehicleMotionState.STOPPED) (if (uiState.isStoppedForTwoSeconds) "PARADO" else "PARANDO (2s)") else "MOVIMENTO"} | ${if (uiState.isPhoneStable) "ESTÁVEL" else "INSTÁVEL"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = DynoTextSecondary.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (uiState.testState == DynoRunState.AGUARDANDO_INICIO) {
                        Text(
                            text = "ARMADO: ACELERE TUDO A PARTIR DE ${uiState.startSpeedTriggerKmh.toInt()} KM/H EM 3ª MARCHA",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                            color = DynoWarningYellow,
                            textAlign = TextAlign.Center
                        )
                    } else if (uiState.testState == DynoRunState.MEDINDO) {
                        Text(
                            text = "MEDINDO PASSADA... MANTENHA PÉ EMBAIXO ATÉ CORTAR O GIRO",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                            color = DynoRed,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Telemetria ao Vivo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DynoMetricCard(
                    title = "Potência Ao Vivo",
                    value = String.format(Locale.US, "%.0f", uiState.liveEnginePowerCv),
                    unit = "cv",
                    accentColor = DynoPowerCyan,
                    modifier = Modifier.weight(1f)
                )
                DynoMetricCard(
                    title = "Torque Ao Vivo",
                    value = String.format(Locale.US, "%.1f", uiState.liveEngineTorqueKgm),
                    unit = "kgfm",
                    accentColor = DynoTorqueAmber,
                    modifier = Modifier.weight(1f)
                )
                DynoMetricCard(
                    title = "Força G Long.",
                    value = String.format(Locale.US, "%.2f", uiState.longitudinalG),
                    unit = "G",
                    accentColor = if (uiState.longitudinalG > 0f) DynoSuccessGreen else DynoTextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Checklist de Prontidão
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DynoCardBg),
                border = BorderStroke(1.dp, DynoCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "STATUS DE SENSORES E PRONTIDÃO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = DynoTextSecondary
                    )

                    // 1. Sinal GPS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sinal GPS:", style = MaterialTheme.typography.bodySmall, color = DynoTextSecondary)
                        Text(
                            text = if (uiState.locationUpdateCount == 0) "Aguardando..." else if (uiState.gpsAgeMillis <= 3500L) "Fix Válido" else "Sinal Atrasado",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (uiState.locationUpdateCount > 0 && uiState.gpsAgeMillis <= 3500L) DynoSuccessGreen else DynoWarningYellow
                        )
                    }

                    // 2. Precisão GPS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Precisão Horizontal:", style = MaterialTheme.typography.bodySmall, color = DynoTextSecondary)
                        Text(
                            text = if (uiState.locationUpdateCount == 0) "--" else "±${String.format(Locale.US, "%.1f", uiState.gpsAccuracyMeters)} m",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (uiState.gpsAccuracyMeters <= 15f) DynoSuccessGreen else if (uiState.gpsAccuracyMeters <= 25f) DynoWarningYellow else DynoErrorRed
                        )
                    }

                    // 3. Calibração do Suporte
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Calibração no Suporte:", style = MaterialTheme.typography.bodySmall, color = DynoTextSecondary)
                        Text(
                            text = if (uiState.isCalibrated) "Calibrado" else "Pendente",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (uiState.isCalibrated) DynoSuccessGreen else DynoWarningYellow
                        )
                    }

                    // 4. Estado do Veículo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Movimento do Veículo:", style = MaterialTheme.typography.bodySmall, color = DynoTextSecondary)
                        val vehText = when {
                            uiState.vehicleMotionState == VehicleMotionState.STOPPED && uiState.isStoppedForTwoSeconds -> "Parado"
                            uiState.vehicleMotionState == VehicleMotionState.STOPPED -> "Parando (2s)"
                            else -> "Em movimento"
                        }
                        Text(
                            text = vehText,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (uiState.vehicleMotionState == VehicleMotionState.STOPPED && uiState.isStoppedForTwoSeconds) DynoSuccessGreen else DynoWarningYellow
                        )
                    }
                }
            }

            // Botão de Calibração
            if (!uiState.isCalibrated || uiState.testState == DynoRunState.PARADO) {
                Button(
                    onClick = { viewModel.calibratePhoneMount() },
                    enabled = !uiState.isCalibrating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DynoCardSurface),
                    border = BorderStroke(1.dp, DynoCardBorder)
                ) {
                    if (uiState.isCalibrating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DynoPowerCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CALIBRANDO SUPORTE...", color = DynoTextPrimary)
                    } else {
                        Icon(Icons.Default.ScreenRotation, contentDescription = null, tint = DynoPowerCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isCalibrated) "RECALIBRAR POSIÇÃO DO CELULAR" else "CALIBRAR CELULAR NO SUPORTE",
                            color = DynoTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Botão Principal de Ação (INICIAR / CANCELAR)
            if (uiState.testState == DynoRunState.PARADO) {
                val startBtnText = when {
                    uiState.isReadyToArm -> "INICIAR MEDIÇÃO"
                    !uiState.isCalibrated -> "CALIBRE O SUPORTE PRIMEIRO"
                    uiState.locationUpdateCount == 0 || uiState.gpsAgeMillis > 5000L -> "AGUARDANDO SINAL GPS"
                    uiState.gpsAccuracyMeters > 25.0f -> "PRECISÃO GPS FRACA"
                    uiState.vehicleMotionState != VehicleMotionState.STOPPED || !uiState.isStoppedForTwoSeconds -> "PARE O CARRO PARA ARMAR"
                    !uiState.isPhoneStable -> "ESTABILIZE O CELULAR"
                    else -> "INICIAR MEDIÇÃO"
                }

                Button(
                    onClick = { viewModel.armTest() },
                    enabled = uiState.isReadyToArm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isReadyToArm) DynoRed else DynoCardSurface,
                        disabledContainerColor = DynoCardBg
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (uiState.isReadyToArm) Color.White else DynoTextMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = startBtnText,
                        color = if (uiState.isReadyToArm) Color.White else DynoTextMuted,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
            } else {
                Button(
                    onClick = { viewModel.cancelTest() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DynoCardSurface),
                    border = BorderStroke(1.5.dp, DynoRed)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, tint = DynoRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CANCELAR TESTE", color = DynoRed, fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
