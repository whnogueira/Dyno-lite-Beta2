package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.*
import com.example.ui.components.DynoPrimaryButton
import com.example.ui.components.DynoSecondaryButton
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// =========================================================================================
// 1. MODAL DE DINAMÔMETRO VIRTUAL ANIMADO & TESTE DE PISTA (Seção 17)
// =========================================================================================

@Composable
fun VirtualDynoPullDialog(
  tuningResult: TuningCalculationResult,
  dynoTestResult: DynoTestAccelerationResult,
  onDismiss: () -> Unit
) {
  val build = tuningResult.build
  val coroutineScope = rememberCoroutineScope()
  val rpmAnim = remember { Animatable(1000f) }
  var isPullRunning by remember { mutableStateOf(false) }
  var hasPullFinished by remember { mutableStateOf(false) }
  var selectedTab by remember { mutableIntStateOf(0) } // 0: Dinamômetro & Curvas, 1: Aceleração & Pista

  fun startDynoPull() {
    coroutineScope.launch {
      isPullRunning = true
      hasPullFinished = false
      rpmAnim.snapTo(1000f)
      rpmAnim.animateTo(
        targetValue = tuningResult.effectiveRedlineRpm.toFloat(),
        animationSpec = tween(durationMillis = 3200, easing = LinearEasing)
      )
      isPullRunning = false
      hasPullFinished = true
    }
  }

  LaunchedEffect(Unit) {
    startDynoPull()
  }

  val currentAnimRpm = rpmAnim.value.toInt()
  // Interpolação instantânea durante o pull
  val currentPower = remember(currentAnimRpm) {
    tuningResult.powerCurvePoints.findClosestY(currentAnimRpm)
  }
  val currentTorque = remember(currentAnimRpm) {
    tuningResult.torqueCurvePoints.findClosestY(currentAnimRpm)
  }
  val currentBoost = remember(currentAnimRpm) {
    tuningResult.boostCurvePoints.findClosestY(currentAnimRpm)
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(DynoBackground)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        // Top Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .background(SimPurplePrimary.copy(alpha = 0.2f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Filled.Speed, contentDescription = null, tint = SimPurpleLight, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Dinamômetro Virtual", fontWeight = FontWeight.Bold, color = DynoTextPrimary, fontSize = 18.sp)
              Text("Teste de Potência e Aceleração", color = DynoTextSecondary, fontSize = 12.sp)
            }
          }

          IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_dyno_dialog")) {
            Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = DynoTextMuted)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tacômetro Analógico / Digital e Manômetro
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = DynoSurfaceContainer),
          border = BorderStroke(1.dp, SimPurplePrimary.copy(alpha = 0.4f)),
          shape = RoundedCornerShape(14.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            // Mostradores Digitais Principais
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceAround,
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Rotação
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  "${currentAnimRpm}",
                  fontSize = 28.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  color = if (currentAnimRpm >= tuningResult.effectiveRedlineRpm - 300) DynoErrorRed else DynoTextPrimary
                )
                Text("RPM MOTOR", fontSize = 11.sp, color = DynoTextMuted, fontWeight = FontWeight.SemiBold)
              }

              // Potência
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  String.format(Locale.US, "%.0f", currentPower),
                  fontSize = 28.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  color = DynoPowerCyan
                )
                Text("POTÊNCIA (cv)", fontSize = 11.sp, color = DynoPowerCyan, fontWeight = FontWeight.SemiBold)
              }

              // Torque
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  String.format(Locale.US, "%.1f", currentTorque),
                  fontSize = 28.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  color = DynoTorqueOrange
                )
                Text("TORQUE (kgfm)", fontSize = 11.sp, color = DynoTorqueOrange, fontWeight = FontWeight.SemiBold)
              }

              // Turbo / Boost
              if (build.aspiration != AspirationType.ASPIRADO) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    String.format(Locale.US, "%.2f", currentBoost),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = SimPurpleLight
                  )
                  Text("TURBO (bar)", fontSize = 11.sp, color = SimPurpleLight, fontWeight = FontWeight.SemiBold)
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Barra Animada de Rotação (Tachometer Sweep)
            val rpmFraction = ((currentAnimRpm - 1000f) / (tuningResult.effectiveRedlineRpm - 1000f)).coerceIn(0f, 1f)
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(DynoSurfaceElevated, RoundedCornerShape(6.dp))
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth(rpmFraction)
                  .fillMaxHeight()
                  .background(
                    Brush.horizontalGradient(
                      listOf(
                        DynoPowerCyan,
                        SimPurplePrimary,
                        if (rpmFraction > 0.88f) DynoErrorRed else DynoTorqueOrange
                      )
                    ),
                    RoundedCornerShape(6.dp)
                  )
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botão Repetir Passagem
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End
            ) {
              OutlinedButton(
                onClick = { startDynoPull() },
                enabled = !isPullRunning,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SimPurplePrimary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("repeat_dyno_pull_button")
              ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = SimPurpleLight, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isPullRunning) "Acelerando..." else "Repetir Puxada", color = SimPurpleLight, fontSize = 13.sp)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = DynoSurface,
          contentColor = DynoTextPrimary,
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
              color = SimPurplePrimary
            )
          }
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("Curvas de Dinamômetro", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("Tempos de Arrancada & Pista", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Conteúdo da Aba
        Box(modifier = Modifier.weight(1f)) {
          if (selectedTab == 0) {
            DynoCurvesTabContent(tuningResult, currentAnimRpm)
          } else {
            DynoTrackTimesTabContent(tuningResult, dynoTestResult)
          }
        }

        // Rodapé de Ação
        Spacer(modifier = Modifier.height(8.dp))
        DynoPrimaryButton(
          text = "Concluir Teste",
          onClick = onDismiss,
          icon = Icons.Filled.Check,
          modifier = Modifier.fillMaxWidth().testTag("finish_dyno_test_button")
        )
      }
    }
  }
}

@Composable
private fun DynoCurvesTabContent(result: TuningCalculationResult, currentAnimRpm: Int) {
  val scrollState = rememberScrollState()
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
  ) {
    // Gráfico de Curva de Potência & Torque
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = DynoSurface),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, DynoBorder)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Curva de Potência e Torque", fontWeight = FontWeight.Bold, color = DynoTextPrimary, fontSize = 14.sp)
          Row {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(modifier = Modifier.size(8.dp).background(DynoPowerCyan, CircleShape))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Potência (cv)", fontSize = 11.sp, color = DynoPowerCyan)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(modifier = Modifier.size(8.dp).background(DynoTorqueOrange, CircleShape))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Torque (kgfm)", fontSize = 11.sp, color = DynoTorqueOrange)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Canvas do Gráfico
        Canvas(
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
        ) {
          val w = size.width
          val h = size.height
          val paddingLeft = 35f
          val paddingBottom = 25f
          val chartW = w - paddingLeft - 10f
          val chartH = h - paddingBottom - 10f

          val maxP = (result.estimatedEnginePowerCv * 1.15f).coerceAtLeast(100f)
          val maxT = (result.estimatedEngineTorqueKgfm * 1.15f).coerceAtLeast(15f)
          val minRpm = 1000f
          val maxRpm = result.effectiveRedlineRpm.toFloat()

          // Grade horizontal
          for (i in 0..4) {
            val y = 10f + chartH * (i / 4f)
            drawLine(
              color = DynoDivider,
              start = Offset(paddingLeft, y),
              end = Offset(paddingLeft + chartW, y),
              strokeWidth = 1f
            )
          }

          // Linha de Potência (Ciano)
          val powerPath = Path()
          result.powerCurvePoints.filter { it.first <= currentAnimRpm }.forEachIndexed { idx, pt ->
            val x = paddingLeft + ((pt.first - minRpm) / (maxRpm - minRpm)) * chartW
            val y = (10f + chartH) - (pt.second / maxP) * chartH
            if (idx == 0) powerPath.moveTo(x, y) else powerPath.lineTo(x, y)
          }
          drawPath(powerPath, color = DynoPowerCyan, style = Stroke(width = 3.5f, cap = StrokeCap.Round))

          // Linha de Torque (Laranja)
          val torquePath = Path()
          result.torqueCurvePoints.filter { it.first <= currentAnimRpm }.forEachIndexed { idx, pt ->
            val x = paddingLeft + ((pt.first - minRpm) / (maxRpm - minRpm)) * chartW
            val y = (10f + chartH) - (pt.second / maxT) * chartH
            if (idx == 0) torquePath.moveTo(x, y) else torquePath.lineTo(x, y)
          }
          drawPath(torquePath, color = DynoTorqueOrange, style = Stroke(width = 3.5f, cap = StrokeCap.Round))

          // Linha vertical do RPM atual
          val currentX = paddingLeft + ((currentAnimRpm - minRpm) / (maxRpm - minRpm)) * chartW
          drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(currentX, 10f),
            end = Offset(currentX, 10f + chartH),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Curva de Pressão e Uso dos Bicos
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = DynoSurface),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, DynoBorder)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Pressão de Turbo & Uso dos Bicos", fontWeight = FontWeight.Bold, color = DynoTextPrimary, fontSize = 14.sp)
          Row {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(modifier = Modifier.size(8.dp).background(SimPurpleLight, CircleShape))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Pressão (bar)", fontSize = 11.sp, color = SimPurpleLight)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(modifier = Modifier.size(8.dp).background(DynoWarningYellow, CircleShape))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Bicos (% duty)", fontSize = 11.sp, color = DynoWarningYellow)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Canvas(
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
        ) {
          val w = size.width
          val h = size.height
          val paddingLeft = 35f
          val paddingBottom = 20f
          val chartW = w - paddingLeft - 10f
          val chartH = h - paddingBottom - 10f

          val maxBoost = (result.actualBoostBar * 1.25f).coerceAtLeast(1.0f)
          val maxDuty = 120f
          val minRpm = 1000f
          val maxRpm = result.effectiveRedlineRpm.toFloat()

          // Grade
          for (i in 0..3) {
            val y = 10f + chartH * (i / 3f)
            drawLine(
              color = DynoDivider,
              start = Offset(paddingLeft, y),
              end = Offset(paddingLeft + chartW, y),
              strokeWidth = 1f
            )
          }

          // Linha de Boost
          if (result.build.aspiration != AspirationType.ASPIRADO) {
            val boostPath = Path()
            result.boostCurvePoints.filter { it.first <= currentAnimRpm }.forEachIndexed { idx, pt ->
              val x = paddingLeft + ((pt.first - minRpm) / (maxRpm - minRpm)) * chartW
              val y = (10f + chartH) - (pt.second / maxBoost) * chartH
              if (idx == 0) boostPath.moveTo(x, y) else boostPath.lineTo(x, y)
            }
            drawPath(boostPath, color = SimPurpleLight, style = Stroke(width = 3f))
          }

          // Linha de Bicos Duty %
          val dutyPath = Path()
          result.injectorDutyCurvePoints.filter { it.first <= currentAnimRpm }.forEachIndexed { idx, pt ->
            val x = paddingLeft + ((pt.first - minRpm) / (maxRpm - minRpm)) * chartW
            val y = (10f + chartH) - (pt.second / maxDuty) * chartH
            if (idx == 0) dutyPath.moveTo(x, y) else dutyPath.lineTo(x, y)
          }
          drawPath(dutyPath, color = DynoWarningYellow, style = Stroke(width = 3f))
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Resumo dos Picos
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = DynoSurfaceContainer),
        shape = RoundedCornerShape(10.dp)
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Text("PICO POTÊNCIA", fontSize = 11.sp, color = DynoTextMuted, fontWeight = FontWeight.Bold)
          Text("${String.format(Locale.US, "%.0f", result.estimatedEnginePowerCv)} cv", fontSize = 18.sp, fontWeight = FontWeight.Black, color = DynoPowerCyan)
          Text("às ${result.peakPowerRpm} RPM", fontSize = 11.sp, color = DynoTextSecondary)
        }
      }

      Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = DynoSurfaceContainer),
        shape = RoundedCornerShape(10.dp)
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Text("PICO TORQUE", fontSize = 11.sp, color = DynoTextMuted, fontWeight = FontWeight.Bold)
          Text("${String.format(Locale.US, "%.1f", result.estimatedEngineTorqueKgfm)} kgfm", fontSize = 18.sp, fontWeight = FontWeight.Black, color = DynoTorqueOrange)
          Text("às ${result.peakTorqueRpm} RPM", fontSize = 11.sp, color = DynoTextSecondary)
        }
      }
    }
  }
}

@Composable
private fun DynoTrackTimesTabContent(
  tuning: TuningCalculationResult,
  track: DynoTestAccelerationResult
) {
  val scrollState = rememberScrollState()
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
  ) {
    // Grade de Arrancada
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = DynoSurface),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, DynoBorder)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text("Tempos de Arrancada (Drag Strip)", fontWeight = FontWeight.Bold, color = DynoTextPrimary, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          TrackTimeBadge("0-60 km/h", "${String.format(Locale.US, "%.2f", track.time0to60Kmh)}s")
          TrackTimeBadge("0-100 km/h", "${String.format(Locale.US, "%.2f", track.time0to100Kmh)}s", isHighlight = true)
          TrackTimeBadge("60-100 km/h", "${String.format(Locale.US, "%.2f", track.time60to100Kmh)}s")
          TrackTimeBadge("80-120 km/h", "${String.format(Locale.US, "%.2f", track.time80to120Kmh)}s")
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = DynoDivider)
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          TrackTimeBadge("60 pés (18m)", "${String.format(Locale.US, "%.2f", track.time60ft)}s")
          TrackTimeBadge("201m (1/8 mi)", "${String.format(Locale.US, "%.2f", track.time201m)}s\n@ ${String.format(Locale.US, "%.0f", track.speedAt201mKmh)} km/h")
          TrackTimeBadge("402m (1/4 mi)", "${String.format(Locale.US, "%.2f", track.time402m)}s\n@ ${String.format(Locale.US, "%.0f", track.speedAt402mKmh)} km/h", isHighlight = true)
          TrackTimeBadge("Vel. Máxima", "${String.format(Locale.US, "%.0f", track.estimatedTopSpeedKmh)} km/h")
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Gráfico Velocidade x Tempo
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = DynoSurface),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, DynoBorder)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text("Curva de Velocidade x Tempo (0 a 402m)", fontWeight = FontWeight.Bold, color = DynoTextPrimary, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
        ) {
          val w = size.width
          val h = size.height
          val paddingLeft = 35f
          val paddingBottom = 20f
          val chartW = w - paddingLeft - 10f
          val chartH = h - paddingBottom - 10f

          val maxT = (track.time402m * 1.1f).coerceAtLeast(10f)
          val maxV = (track.speedAt402mKmh * 1.15f).coerceAtLeast(120f)

          // Grade
          for (i in 0..3) {
            val y = 10f + chartH * (i / 3f)
            drawLine(
              color = DynoDivider,
              start = Offset(paddingLeft, y),
              end = Offset(paddingLeft + chartW, y),
              strokeWidth = 1f
            )
          }

          val path = Path()
          track.speedTimePoints.forEachIndexed { idx, pt ->
            val x = paddingLeft + (pt.first / maxT) * chartW
            val y = (10f + chartH) - (pt.second / maxV) * chartH
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
          }
          drawPath(path, color = DynoSuccessGreen, style = Stroke(width = 3.5f, cap = StrokeCap.Round))
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Consumo e Pontos Ideais de Troca
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = DynoSurfaceContainer),
      shape = RoundedCornerShape(12.dp)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text("Consumo em Plena Carga (WOT)", fontSize = 12.sp, color = DynoTextMuted, fontWeight = FontWeight.SemiBold)
            Text("${String.format(Locale.US, "%.1f", track.fuelConsumptionWotLph)} L/h", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DynoTextPrimary)
          }

          Column(horizontalAlignment = Alignment.End) {
            Text("Aceleração Máxima (G)", fontSize = 12.sp, color = DynoTextMuted, fontWeight = FontWeight.SemiBold)
            Text("${String.format(Locale.US, "%.2f", track.peakLongitudinalG)} G", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SimPurpleLight)
          }
        }
      }
    }
  }
}

@Composable
private fun TrackTimeBadge(title: String, value: String, isHighlight: Boolean = false) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(title, fontSize = 10.sp, color = DynoTextMuted, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      value,
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold,
      color = if (isHighlight) SimPurpleLight else DynoTextPrimary,
      textAlign = TextAlign.Center
    )
  }
}

private fun List<Pair<Int, Float>>.findClosestY(targetRpm: Int): Float {
  if (isEmpty()) return 0f
  val match = minByOrNull { kotlin.math.abs(it.first - targetRpm) }
  return match?.second ?: 0f
}

// =========================================================================================
// 2. MODAL DE COMPARAÇÃO LADO A LADO (ORIGINAL VS PREPARADO) (Seção 18)
// =========================================================================================

@Composable
fun TuningComparisonDialog(
  currentBuild: TuningBuild,
  currentResult: TuningCalculationResult,
  currentTrack: DynoTestAccelerationResult,
  onDismiss: () -> Unit
) {
  // Cria o resultado original de fábrica para o veículo base
  val originalBuild = remember(currentBuild) {
    GarageTuningEngine.applyProjectTemplate(ProjectTemplateType.ORIGINAL, currentBuild)
  }
  val originalResult = remember(originalBuild) {
    GarageTuningEngine.calculateTuningBuild(originalBuild)
  }
  val originalTrack = remember(originalResult) {
    GarageTuningEngine.runDynoTrackSimulation(originalResult)
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(DynoBackground)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        // Top Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("Comparativo de Preparação", fontWeight = FontWeight.Bold, color = DynoTextPrimary, fontSize = 18.sp)
            Text("ORIGINAL vs PREPARADO", color = SimPurpleLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = DynoTextMuted)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val scrollState = rememberScrollState()
        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(scrollState)
        ) {
          // Tabela Comparativa de Métricas
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DynoSurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DynoBorder)
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              // Cabeçalho da Tabela
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PARÂMETRO", fontSize = 11.sp, color = DynoTextMuted, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))
                Text("ORIGINAL", fontSize = 11.sp, color = DynoTextSecondary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text("PREPARADO", fontSize = 11.sp, color = SimPurpleLight, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text("DIFERENÇA", fontSize = 11.sp, color = DynoSuccessGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
              }

              Spacer(modifier = Modifier.height(8.dp))
              HorizontalDivider(color = DynoDivider)
              Spacer(modifier = Modifier.height(8.dp))

              // Linhas
              ComparisonRow(
                label = "Potência Motor",
                valA = "${String.format(Locale.US, "%.0f", originalResult.estimatedEnginePowerCv)} cv",
                valB = "${String.format(Locale.US, "%.0f", currentResult.estimatedEnginePowerCv)} cv",
                diff = "+${String.format(Locale.US, "%.0f", currentResult.estimatedEnginePowerCv - originalResult.estimatedEnginePowerCv)} cv",
                isPositiveGain = currentResult.estimatedEnginePowerCv >= originalResult.estimatedEnginePowerCv
              )

              ComparisonRow(
                label = "Torque Motor",
                valA = "${String.format(Locale.US, "%.1f", originalResult.estimatedEngineTorqueKgfm)} kgfm",
                valB = "${String.format(Locale.US, "%.1f", currentResult.estimatedEngineTorqueKgfm)} kgfm",
                diff = "+${String.format(Locale.US, "%.1f", currentResult.estimatedEngineTorqueKgfm - originalResult.estimatedEngineTorqueKgfm)} kgfm",
                isPositiveGain = currentResult.estimatedEngineTorqueKgfm >= originalResult.estimatedEngineTorqueKgfm
              )

              ComparisonRow(
                label = "Pressão Turbo",
                valA = "0.0 bar",
                valB = "${String.format(Locale.US, "%.1f", currentResult.actualBoostBar)} bar",
                diff = "+${String.format(Locale.US, "%.1f", currentResult.actualBoostBar)} bar",
                isPositiveGain = true
              )

              ComparisonRow(
                label = "0-100 km/h",
                valA = "${String.format(Locale.US, "%.2f", originalTrack.time0to100Kmh)}s",
                valB = "${String.format(Locale.US, "%.2f", currentTrack.time0to100Kmh)}s",
                diff = "${String.format(Locale.US, "%.2f", currentTrack.time0to100Kmh - originalTrack.time0to100Kmh)}s",
                isPositiveGain = currentTrack.time0to100Kmh <= originalTrack.time0to100Kmh
              )

              ComparisonRow(
                label = "402m (1/4 mi)",
                valA = "${String.format(Locale.US, "%.2f", originalTrack.time402m)}s",
                valB = "${String.format(Locale.US, "%.2f", currentTrack.time402m)}s",
                diff = "${String.format(Locale.US, "%.2f", currentTrack.time402m - originalTrack.time402m)}s",
                isPositiveGain = currentTrack.time402m <= originalTrack.time402m
              )

              ComparisonRow(
                label = "Vel. Final 402m",
                valA = "${String.format(Locale.US, "%.0f", originalTrack.speedAt402mKmh)} km/h",
                valB = "${String.format(Locale.US, "%.0f", currentTrack.speedAt402mKmh)} km/h",
                diff = "+${String.format(Locale.US, "%.0f", currentTrack.speedAt402mKmh - originalTrack.speedAt402mKmh)} km/h",
                isPositiveGain = currentTrack.speedAt402mKmh >= originalTrack.speedAt402mKmh
              )

              ComparisonRow(
                label = "Peso Total",
                valA = "${String.format(Locale.US, "%.0f", originalResult.totalVehicleMassKg)} kg",
                valB = "${String.format(Locale.US, "%.0f", currentResult.totalVehicleMassKg)} kg",
                diff = "${String.format(Locale.US, "%.0f", currentResult.totalVehicleMassKg - originalResult.totalVehicleMassKg)} kg",
                isPositiveGain = currentResult.totalVehicleMassKg <= originalResult.totalVehicleMassKg
              )

              ComparisonRow(
                label = "Relação Peso/Pot.",
                valA = "${String.format(Locale.US, "%.2f", originalResult.weightToPowerRatioKgCv)} kg/cv",
                valB = "${String.format(Locale.US, "%.2f", currentResult.weightToPowerRatioKgCv)} kg/cv",
                diff = "${String.format(Locale.US, "%.2f", currentResult.weightToPowerRatioKgCv - originalResult.weightToPowerRatioKgCv)}",
                isPositiveGain = currentResult.weightToPowerRatioKgCv <= originalResult.weightToPowerRatioKgCv
              )

              ComparisonRow(
                label = "Confiabilidade",
                valA = "${originalResult.reliabilityScore}%",
                valB = "${currentResult.reliabilityScore}%",
                diff = "${currentResult.reliabilityScore - originalResult.reliabilityScore}%",
                isPositiveGain = currentResult.reliabilityScore >= 70
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Gráfico de Sobreposição das Curvas de Potência
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DynoSurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DynoBorder)
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text("Sobreposição de Curvas de Potência (cv x RPM)", fontWeight = FontWeight.Bold, color = DynoTextPrimary, fontSize = 14.sp)
              Spacer(modifier = Modifier.height(8.dp))

              Canvas(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(150.dp)
              ) {
                val w = size.width
                val h = size.height
                val paddingLeft = 35f
                val paddingBottom = 20f
                val chartW = w - paddingLeft - 10f
                val chartH = h - paddingBottom - 10f

                val maxP = max(originalResult.estimatedEnginePowerCv, currentResult.estimatedEnginePowerCv) * 1.15f
                val minRpm = 1000f
                val maxRpm = max(originalResult.effectiveRedlineRpm, currentResult.effectiveRedlineRpm).toFloat()

                // Curva Original (Cinza tracejada)
                val origPath = Path()
                originalResult.powerCurvePoints.forEachIndexed { idx, pt ->
                  val x = paddingLeft + ((pt.first - minRpm) / (maxRpm - minRpm)) * chartW
                  val y = (10f + chartH) - (pt.second / maxP) * chartH
                  if (idx == 0) origPath.moveTo(x, y) else origPath.lineTo(x, y)
                }
                drawPath(
                  origPath,
                  color = DynoTextMuted,
                  style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                )

                // Curva Preparada (Roxo / Ciano)
                val prepPath = Path()
                currentResult.powerCurvePoints.forEachIndexed { idx, pt ->
                  val x = paddingLeft + ((pt.first - minRpm) / (maxRpm - minRpm)) * chartW
                  val y = (10f + chartH) - (pt.second / maxP) * chartH
                  if (idx == 0) prepPath.moveTo(x, y) else prepPath.lineTo(x, y)
                }
                drawPath(prepPath, color = SimPurpleLight, style = Stroke(width = 3.5f))
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        DynoPrimaryButton(
          text = "Fechar Comparativo",
          onClick = onDismiss,
          icon = Icons.Filled.Check,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

@Composable
private fun ComparisonRow(
  label: String,
  valA: String,
  valB: String,
  diff: String,
  isPositiveGain: Boolean
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 5.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, fontSize = 12.sp, color = DynoTextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.3f))
    Text(valA, fontSize = 12.sp, color = DynoTextMuted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
    Text(valB, fontSize = 12.sp, color = SimPurpleLight, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
    Text(
      diff,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      color = if (isPositiveGain) DynoSuccessGreen else DynoErrorRed,
      textAlign = TextAlign.End,
      modifier = Modifier.weight(1.1f)
    )
  }
}

// =========================================================================================
// 3. MODAL DE CONFIGURAÇÃO ORIGINAL DO MOTOR DO VEÍCULO (Seção 2 & 21)
// =========================================================================================

@Composable
fun VehicleSetupDialog(
  currentBuild: TuningBuild,
  onSave: (TuningBuild) -> Unit,
  onDismiss: () -> Unit
) {
  var name by remember { mutableStateOf(currentBuild.vehicleName) }
  var displacement by remember { mutableStateOf(currentBuild.displacementCc.toString()) }
  var cylinders by remember { mutableStateOf(currentBuild.cylindersCount.toString()) }
  var power by remember { mutableStateOf(currentBuild.factoryEnginePowerCv.toString()) }
  var torque by remember { mutableStateOf(currentBuild.factoryEngineTorqueKgfm.toString()) }
  var peakPowerRpm by remember { mutableStateOf(currentBuild.factoryPeakPowerRpm.toString()) }
  var peakTorqueRpm by remember { mutableStateOf(currentBuild.factoryPeakTorqueRpm.toString()) }
  var redlineRpm by remember { mutableStateOf(currentBuild.factoryRedlineRpm.toString()) }
  var weight by remember { mutableStateOf(currentBuild.baseVehicleCurbWeightKg.toString()) }
  var drivetrain by remember { mutableStateOf(currentBuild.baseDrivetrain) }
  var compression by remember { mutableStateOf(currentBuild.baseCompressionRatio.toString()) }
  var tireWidth by remember { mutableStateOf(currentBuild.tireWidthMm.toString()) }
  var tireAspect by remember { mutableStateOf(currentBuild.tireAspectRatio.toString()) }
  var rimInches by remember { mutableStateOf(currentBuild.rimInches.toString()) }
  var finalDrive by remember { mutableStateOf(currentBuild.finalDriveRatio.toString()) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(DynoBackground)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        // Top Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = SimPurpleLight)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Configuração Original do Motor", fontWeight = FontWeight.Bold, color = DynoTextPrimary, fontSize = 18.sp)
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = DynoTextMuted)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val scrollState = rememberScrollState()
        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(scrollState)
        ) {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nome do Veículo") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SimPurplePrimary,
              unfocusedBorderColor = DynoBorder,
              focusedLabelColor = SimPurpleLight
            )
          )

          Spacer(modifier = Modifier.height(10.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = displacement,
              onValueChange = { displacement = it },
              label = { Text("Cilindrada (cc)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SimPurplePrimary)
            )
            OutlinedTextField(
              value = cylinders,
              onValueChange = { cylinders = it },
              label = { Text("Cilindros") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SimPurplePrimary)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = power,
              onValueChange = { power = it },
              label = { Text("Potência Original (cv)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              modifier = Modifier.weight(1f),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SimPurplePrimary)
            )
            OutlinedTextField(
              value = torque,
              onValueChange = { torque = it },
              label = { Text("Torque Original (kgfm)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              modifier = Modifier.weight(1f),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SimPurplePrimary)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = peakPowerRpm,
              onValueChange = { peakPowerRpm = it },
              label = { Text("RPM Pot. Máx") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SimPurplePrimary)
            )
            OutlinedTextField(
              value = peakTorqueRpm,
              onValueChange = { peakTorqueRpm = it },
              label = { Text("RPM Torque Máx") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SimPurplePrimary)
            )
            OutlinedTextField(
              value = redlineRpm,
              onValueChange = { redlineRpm = it },
              label = { Text("Corte RPM") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SimPurplePrimary)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = weight,
              onValueChange = { weight = it },
              label = { Text("Peso Original (kg)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              modifier = Modifier.weight(1f),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SimPurplePrimary)
            )
            OutlinedTextField(
              value = compression,
              onValueChange = { compression = it },
              label = { Text("Taxa de Compressão") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              modifier = Modifier.weight(1f),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SimPurplePrimary)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Tração
          Text("Tração", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DrivetrainType.entries.forEach { dt ->
              FilterChip(
                selected = drivetrain == dt,
                onClick = { drivetrain = dt },
                label = { Text(dt.displayName.substringBefore(" ")) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = SimPurplePrimary,
                  selectedLabelColor = Color.White
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Pneus
          Text("Dimensões dos Pneus de Fábrica", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = tireWidth,
              onValueChange = { tireWidth = it },
              label = { Text("Largura (mm)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SimPurplePrimary)
            )
            OutlinedTextField(
              value = tireAspect,
              onValueChange = { tireAspect = it },
              label = { Text("Perfil (%)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SimPurplePrimary)
            )
            OutlinedTextField(
              value = rimInches,
              onValueChange = { rimInches = it },
              label = { Text("Aro (\")") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SimPurplePrimary)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = finalDrive,
            onValueChange = { finalDrive = it },
            label = { Text("Relação do Diferencial") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SimPurplePrimary)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        DynoPrimaryButton(
          text = "Salvar Dados do Veículo",
          onClick = {
            val updated = currentBuild.copy(
              vehicleName = name.ifBlank { "Veículo" },
              displacementCc = displacement.toIntOrNull() ?: currentBuild.displacementCc,
              cylindersCount = cylinders.toIntOrNull() ?: currentBuild.cylindersCount,
              factoryEnginePowerCv = power.toFloatOrNull() ?: currentBuild.factoryEnginePowerCv,
              factoryEngineTorqueKgfm = torque.toFloatOrNull() ?: currentBuild.factoryEngineTorqueKgfm,
              factoryPeakPowerRpm = peakPowerRpm.toIntOrNull() ?: currentBuild.factoryPeakPowerRpm,
              factoryPeakTorqueRpm = peakTorqueRpm.toIntOrNull() ?: currentBuild.factoryPeakTorqueRpm,
              factoryRedlineRpm = redlineRpm.toIntOrNull() ?: currentBuild.factoryRedlineRpm,
              baseVehicleCurbWeightKg = weight.toFloatOrNull() ?: currentBuild.baseVehicleCurbWeightKg,
              baseCompressionRatio = compression.toFloatOrNull() ?: currentBuild.baseCompressionRatio,
              baseDrivetrain = drivetrain,
              tireWidthMm = tireWidth.toIntOrNull() ?: currentBuild.tireWidthMm,
              tireAspectRatio = tireAspect.toIntOrNull() ?: currentBuild.tireAspectRatio,
              rimInches = rimInches.toIntOrNull() ?: currentBuild.rimInches,
              finalDriveRatio = finalDrive.toFloatOrNull() ?: currentBuild.finalDriveRatio
            )
            onSave(updated)
            onDismiss()
          },
          icon = Icons.Filled.Check,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}
