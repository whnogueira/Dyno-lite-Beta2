package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RunResultRepository
import com.example.model.FinishReason
import com.example.model.RunResult
import com.example.model.RunSample
import com.example.ui.components.DynoBadgeStatus
import com.example.ui.components.DynoCard
import com.example.ui.components.DynoDangerButton
import com.example.ui.components.DynoPrimaryButton
import com.example.ui.components.DynoResultCard
import com.example.ui.components.DynoSecondaryButton
import com.example.ui.components.DynoStatusBadge
import com.example.ui.components.DynoTopBar
import com.example.ui.theme.DynoBlueLight
import com.example.ui.theme.DynoBluePrimary
import com.example.ui.theme.DynoDivider
import com.example.ui.theme.DynoErrorRed
import com.example.ui.theme.DynoPowerCyan
import com.example.ui.theme.DynoSuccessGreen
import com.example.ui.theme.DynoSurface
import com.example.ui.theme.DynoSurfaceContainer
import com.example.ui.theme.DynoSurfaceElevated
import com.example.ui.theme.DynoTextPrimary
import com.example.ui.theme.DynoTextSecondary
import com.example.ui.theme.DynoTorqueOrange
import com.example.ui.theme.DynoWarningYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
  onStartNewTest: (String?) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val runResultRepository = remember { RunResultRepository(context) }
  var results by remember { mutableStateOf(runResultRepository.getResults()) }

  // Result currently displayed on screen (defaults to most recent run if available)
  var currentDisplayedResult by remember(results) {
    mutableStateOf(results.firstOrNull())
  }

  var isMeasurementDetailsExpanded by remember { mutableStateOf(false) }
  var showHistoryDialog by remember { mutableStateOf(false) }
  var showComparisonDialog by remember { mutableStateOf(false) }
  var showClearAllConfirmDialog by remember { mutableStateOf(false) }

  // Check valid tests for the current vehicle to evaluate comparison eligibility
  val currentVehicleName = currentDisplayedResult?.vehicleName ?: ""
  val validRunsForVehicle = remember(results, currentVehicleName) {
    if (currentVehicleName.isNotEmpty()) {
      results.filter { it.vehicleName == currentVehicleName && it.quality != "INVÁLIDA" }
    } else {
      results.filter { it.quality != "INVÁLIDA" }
    }
  }
  val canCompare = validRunsForVehicle.size >= 2

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("results_screen"),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "RESULTADO DO TESTE",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              fontSize = 20.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("results_title_text")
          )
        },
        actions = {
          if (results.isNotEmpty()) {
            IconButton(
              onClick = { showHistoryDialog = true },
              modifier = Modifier.testTag("top_bar_btn_history")
            ) {
              Icon(
                imageVector = Icons.Default.History,
                contentDescription = "Ver histórico",
                tint = MaterialTheme.colorScheme.primary
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
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
          .padding(horizontal = 18.dp, vertical = 14.dp)
          .widthIn(max = 520.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        val run = currentDisplayedResult

        if (run != null) {
          val dateFormat = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
          val formattedDate = dateFormat.format(Date(run.timestamp))
          val isInvalid = run.quality == "INVÁLIDA" || run.quality == "INVALID"

          if (isInvalid) {
            // -------------------------------------------------------------
            // FLUXO DE PASSAGEM INVÁLIDA
            // 1. BANNER "PASSAGEM INVÁLIDA" NO TOPO
            // -------------------------------------------------------------
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("banner_invalid_run"),
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
            ) {
              Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
              ) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(38.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onError,
                      modifier = Modifier.size(24.dp)
                    )
                  }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = "PASSAGEM INVÁLIDA",
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Black,
                      letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onErrorContainer
                  )
                  Text(
                    text = "Esta passagem não pode ser utilizada para calcular potência e torque.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.95f)
                  )
                }
              }
            }

            // 2. VEÍCULO E DATA
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 10.dp),
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
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                  )
                  Text(
                    text = if (run.vehicleName.isNotEmpty()) run.vehicleName else "Veículo Principal",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }

                Text(
                  text = formattedDate,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // 3. MOTIVO DA INVALIDAÇÃO
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("card_invalidation_reason"),
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
              ),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
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
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                  )
                  Text(
                    text = "MOTIVO DA INVALIDAÇÃO",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.error
                  )
                }
                Text(
                  text = run.getEffectiveInvalidationReason(),
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                  ),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }

            // 4. RESUMO DAS VELOCIDADES (OFICIAL GPS)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Velocidade Máxima Oficial (GPS)
              Card(
                modifier = Modifier
                  .weight(1f)
                  .testTag("card_speed_max_gps"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text(
                    text = "VELOCIDADE MÁXIMA",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                  )
                  Text(
                    text = String.format(Locale.US, "%.1f km/h", run.maximumGpsSpeedKmh),
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }

              // Ganho de Velocidade
              Card(
                modifier = Modifier
                  .weight(1f)
                  .testTag("card_speed_gain"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text(
                    text = "GANHO VELOCIDADE",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 10.sp
                    ),
                    color = Color(0xFF38BDF8)
                  )
                  val gain = if (run.speedGainKmh > 0f) run.speedGainKmh else (run.maximumGpsSpeedKmh - run.runStartGpsSpeedKmh).coerceAtLeast(0f)
                  Text(
                    text = String.format(Locale.US, "+%.1f km/h", gain),
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }

              // Duração
              Card(
                modifier = Modifier
                  .weight(0.9f)
                  .testTag("card_speed_duration"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text(
                    text = "DURAÇÃO",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = String.format(Locale.US, "%.2f s", run.elapsedSeconds),
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }

            // 5. QUALIDADE E CONFIABILIDADE DO SINAL
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("card_quality_differences"),
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
              ),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "QUALIDADE DO SINAL E ESTABILIDADE",
                    style = MaterialTheme.typography.labelMedium.copy(
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                  )

                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer
                  ) {
                    Text(
                      text = "INVÁLIDA",
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                      ),
                      color = MaterialTheme.colorScheme.onErrorContainer
                    )
                  }
                }

                HorizontalDivider(
                  thickness = 0.6.dp,
                  color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                DetailRow(
                  label = "Precisão do GPS",
                  value = String.format(Locale.US, "±%.1f m", run.gpsAccuracyMeters)
                )
                DetailRow(
                  label = "Leituras de GPS válidas",
                  value = "${run.validGpsLocationsCount} atualizações"
                )
                DetailRow(
                  label = "Amostras inerciais",
                  value = "${run.validSamplesCount} válidas (${run.rejectedSamples} descartadas)"
                )
              }
            }

            // 6. DETALHES RECOLHÍVEIS [ VER DETALHES ]
            MeasurementDetailsCard(
              run = run,
              isExpanded = isMeasurementDetailsExpanded,
              onToggleExpand = { isMeasurementDetailsExpanded = !isMeasurementDetailsExpanded },
              orderedSamples = remember(run.id) { runResultRepository.getOrderedRunSamples(run.id) },
              modifier = Modifier.fillMaxWidth()
            )

            // 7. BOTÃO REPETIR QUANDO ESTIVER PARADO
            Button(
              onClick = { onStartNewTest(run.vehicleId) },
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_repeat_test"),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              )
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "REPETIR QUANDO ESTIVER PARADO",
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                )
              )
            }

            // 8. BOTÕES: [ COMPARAR RESULTADOS ] e [ VER HISTÓRICO ]
            ComparisonAndHistoryButtons(
              canCompare = canCompare,
              onCompareClick = { showComparisonDialog = true },
              onHistoryClick = { showHistoryDialog = true },
              modifier = Modifier.fillMaxWidth()
            )

          } else {
            // -------------------------------------------------------------
            // FLUXO DE PASSAGEM VÁLIDA (SEM POTÊNCIA OU COM POTÊNCIA)
            // -------------------------------------------------------------
            // 1. VEÍCULO E DATA
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 10.dp),
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
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                  )
                  Text(
                    text = if (run.vehicleName.isNotEmpty()) run.vehicleName else "Veículo Principal",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }

                Text(
                  text = formattedDate,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // 2. RESUMO DAS VELOCIDADES (OFICIAL GPS)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text("VELOCIDADE MÁXIMA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = MaterialTheme.colorScheme.primary)
                  Text(String.format(Locale.US, "%.1f km/h", run.maximumGpsSpeedKmh), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace))
                }
              }

              Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text("GANHO VELOCIDADE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = Color(0xFF38BDF8))
                  val gain = if (run.speedGainKmh > 0f) run.speedGainKmh else (run.maximumGpsSpeedKmh - run.runStartGpsSpeedKmh).coerceAtLeast(0f)
                  Text(String.format(Locale.US, "+%.1f km/h", gain), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace))
                }
              }

              Card(
                modifier = Modifier.weight(0.9f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text("DURAÇÃO", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(String.format(Locale.US, "%.2f s", run.elapsedSeconds), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace))
                }
              }
            }

            // 3. CARTÕES DE POTÊNCIA, TORQUE E FORÇA G (ESTIMADOS)
            if (run.estimatedPowerCv > 0f || run.peakLongitudinalG > 0f) {
              val runSamples = remember(run.id) { runResultRepository.getOrderedRunSamples(run.id) }

              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  // Potência Motor
                  Card(
                    modifier = Modifier.weight(1f).testTag("card_power_engine"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                  ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text("POTÊNCIA MOTOR", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp), color = Color(0xFF38BDF8))
                        Text("est.", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                      val pCv = if (run.enginePowerCv > 0f) run.enginePowerCv else run.estimatedPowerCv
                      Text(String.format(Locale.US, "%.1f cv", pCv), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
                      if (run.wheelPowerCv > 0f) {
                        Text(String.format(Locale.US, "Roda: %.1f cv", run.wheelPowerCv), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                    }
                  }

                  // Torque Motor
                  Card(
                    modifier = Modifier.weight(1f).testTag("card_torque_engine"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, Color(0xFFFB923C).copy(alpha = 0.5f))
                  ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text("TORQUE MOTOR", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp), color = Color(0xFFFB923C))
                        Text("est.", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                      val tKgfm = if (run.engineTorqueKgfm > 0f) run.engineTorqueKgfm else run.estimatedTorqueKgfm
                      Text(String.format(Locale.US, "%.1f kgfm", tKgfm), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
                      if (run.wheelTorqueKgfm > 0f) {
                        Text(String.format(Locale.US, "Roda: %.1f kgfm", run.wheelTorqueKgfm), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                    }
                  }

                  // Força G Pico
                  Card(
                    modifier = Modifier.weight(0.95f).testTag("card_g_force"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, DynoPowerCyan.copy(alpha = 0.5f))
                  ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      Text("FORÇA G PICO", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp), color = DynoPowerCyan)
                      val gPeak = if (run.peakLongitudinalG > 0f) run.peakLongitudinalG else 0f
                      Text(String.format(Locale.US, "%+.2f G", gPeak), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
                      if (run.averageLongitudinalG > 0f) {
                        Text(String.format(Locale.US, "Média: %+.2f G", run.averageLongitudinalG), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                    }
                  }
                }

                // Gráfico Dinamômetro com Curva de Potência e Torque
                DynoPowerTorqueGraphCard(
                  samples = runSamples,
                  hasVehicleConfig = true,
                  peakPowerCv = if (run.enginePowerCv > 0f) run.enginePowerCv else run.estimatedPowerCv,
                  peakTorqueKgfm = if (run.engineTorqueKgfm > 0f) run.engineTorqueKgfm else run.estimatedTorqueKgfm,
                  modifier = Modifier.fillMaxWidth()
                )
                DynoGraphLegend(modifier = Modifier.fillMaxWidth())

                // Splits de Aceleração (se houver tempos registrados)
                if (hasAnySplits(run)) {
                  AccelerationSplitsCard(run = run, modifier = Modifier.fillMaxWidth())
                }

                // Aviso e Margem Técnica
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                  border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Info,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp),
                      tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                      text = "Estimativa Dyno Lite (margem ±${run.estimatedMarginPercent.toInt()}% com base em peso e arrasto). Não substitui dinamômetro certificado.",
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            } else {
              // Passagem sem perfil veicular completo
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("card_valid_power_pending"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Icon(
                    imageVector = Icons.Outlined.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                  )
                  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                      text = "Passagem concluída com sucesso.",
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = "Dados oficiais sincronizados por GPS e acelerômetro.",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }

            // 4. QUALIDADE E CONFIABILIDADE DO SINAL
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("QUALIDADE DO SINAL E ESTABILIDADE", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp))
                  Surface(
                    shape = CircleShape,
                    color = if (run.quality == "BOA") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                  ) {
                    Text(
                      text = run.quality,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                      color = if (run.quality == "BOA") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                    )
                  }
                }
                HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                DetailRow("Precisão do GPS", String.format(Locale.US, "±%.1f m", run.gpsAccuracyMeters))
                DetailRow("Leituras de GPS válidas", "${run.validGpsLocationsCount} atualizações")
                DetailRow("Amostras inerciais", "${run.validSamplesCount} válidas (${run.rejectedSamples} descartadas)")
              }
            }

            // 5. DETALHES DA MEDIÇÃO RECOLHÍVEIS
            MeasurementDetailsCard(
              run = run,
              isExpanded = isMeasurementDetailsExpanded,
              onToggleExpand = { isMeasurementDetailsExpanded = !isMeasurementDetailsExpanded },
              orderedSamples = remember(run.id) { runResultRepository.getOrderedRunSamples(run.id) },
              modifier = Modifier.fillMaxWidth()
            )

            // 6. BOTÕES COMPARAR E HISTÓRICO
            ComparisonAndHistoryButtons(
              canCompare = canCompare,
              onCompareClick = { showComparisonDialog = true },
              onHistoryClick = { showHistoryDialog = true },
              modifier = Modifier.fillMaxWidth()
            )

            // 7. BOTÃO REPETIR TESTE
            Button(
              onClick = { onStartNewTest(run.vehicleId) },
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_repeat_test"),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              )
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "REPETIR TESTE",
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                )
              )
            }
          }
        } else {
          // Empty State Prompt Card when no run is recorded
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
              )
              Text(
                text = "Nenhum teste concluído ainda. Realize uma passagem de aceleração para visualizar os detalhes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
              )
            }
          }

          // Botões de navegação
          ComparisonAndHistoryButtons(
            canCompare = canCompare,
            onCompareClick = { showComparisonDialog = true },
            onHistoryClick = { showHistoryDialog = true },
            modifier = Modifier.fillMaxWidth()
          )
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }

  // DIÁLOGO DE HISTÓRICO DE RESULTADOS (Preserva o teste exibido na tela ao fechar)
  if (showHistoryDialog) {
    HistoryResultsDialog(
      results = results,
      currentSelectedId = currentDisplayedResult?.id,
      onSelectResult = { selected ->
        currentDisplayedResult = selected
        showHistoryDialog = false
      },
      onDeleteResult = { idToDelete ->
        runResultRepository.deleteResult(idToDelete)
        results = runResultRepository.getResults()
        if (currentDisplayedResult?.id == idToDelete) {
          currentDisplayedResult = results.firstOrNull()
        }
      },
      onClearAll = {
        showClearAllConfirmDialog = true
      },
      onDismiss = { showHistoryDialog = false }
    )
  }

  // DIÁLOGO DE COMPARAÇÃO DE RESULTADOS VÁLIDOS
  if (showComparisonDialog && canCompare) {
    ComparisonResultsDialog(
      validRuns = validRunsForVehicle,
      onDismiss = { showComparisonDialog = false }
    )
  }

  // CONFIRMAÇÃO PARA LIMPAR TODO O HISTÓRICO
  if (showClearAllConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showClearAllConfirmDialog = false },
      title = { Text("Limpar Histórico") },
      text = { Text("Deseja realmente apagar todos os registros salvos? Esta ação não pode ser desfeita.") },
      confirmButton = {
        Button(
          onClick = {
            runResultRepository.clearAllResults()
            results = emptyList()
            currentDisplayedResult = null
            showClearAllConfirmDialog = false
            showHistoryDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("LIMPAR TUDO")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showClearAllConfirmDialog = false }) {
          Text("CANCELAR")
        }
      }
    )
  }
}

fun hasAnySplits(run: RunResult): Boolean {
  return (run.time0to60Kmh ?: 0f) > 0f ||
    (run.time0to100Kmh ?: 0f) > 0f ||
    (run.time60to100Kmh ?: 0f) > 0f ||
    (run.time80to120Kmh ?: 0f) > 0f ||
    (run.time100to200Kmh ?: 0f) > 0f ||
    (run.time60Feet ?: 0f) > 0f ||
    (run.time100M ?: 0f) > 0f ||
    (run.time201M ?: 0f) > 0f ||
    (run.time402M ?: 0f) > 0f
}

/**
 * Cartão com os Splits de Aceleração e Distância
 */
@Composable
private fun AccelerationSplitsCard(
  run: RunResult,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.testTag("card_acceleration_splits"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
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
            imageVector = Icons.Default.Speed,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = "PARCIAIS DE ACELERAÇÃO (SPLITS)",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      HorizontalDivider(
        thickness = 0.6.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
      )

      // Splits de Velocidade
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if ((run.time0to60Kmh ?: 0f) > 0f) {
          SplitMetricItem(label = "0-60 km/h", value = String.format(Locale.US, "%.2fs", run.time0to60Kmh), modifier = Modifier.weight(1f))
        }
        if ((run.time0to100Kmh ?: 0f) > 0f) {
          SplitMetricItem(label = "0-100 km/h", value = String.format(Locale.US, "%.2fs", run.time0to100Kmh), modifier = Modifier.weight(1f), isHighlight = true)
        }
        if ((run.time60to100Kmh ?: 0f) > 0f) {
          SplitMetricItem(label = "60-100 km/h", value = String.format(Locale.US, "%.2fs", run.time60to100Kmh), modifier = Modifier.weight(1f))
        }
        if ((run.time80to120Kmh ?: 0f) > 0f) {
          SplitMetricItem(label = "80-120 km/h", value = String.format(Locale.US, "%.2fs", run.time80to120Kmh), modifier = Modifier.weight(1f))
        }
        if ((run.time100to200Kmh ?: 0f) > 0f) {
          SplitMetricItem(label = "100-200 km/h", value = String.format(Locale.US, "%.2fs", run.time100to200Kmh), modifier = Modifier.weight(1f))
        }
      }

      // Splits de Distância (se existirem)
      if ((run.time60Feet ?: 0f) > 0f || (run.time100M ?: 0f) > 0f || (run.time201M ?: 0f) > 0f || (run.time402M ?: 0f) > 0f) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if ((run.time60Feet ?: 0f) > 0f) {
            SplitMetricItem(label = "60 pés (18m)", value = String.format(Locale.US, "%.2fs", run.time60Feet), modifier = Modifier.weight(1f))
          }
          if ((run.time100M ?: 0f) > 0f) {
            SplitMetricItem(label = "100 metros", value = String.format(Locale.US, "%.2fs", run.time100M), modifier = Modifier.weight(1f))
          }
          if ((run.time201M ?: 0f) > 0f) {
            SplitMetricItem(label = "1/8 mi (201m)", value = String.format(Locale.US, "%.2fs", run.time201M), modifier = Modifier.weight(1f))
          }
          if ((run.time402M ?: 0f) > 0f) {
            SplitMetricItem(label = "1/4 mi (402m)", value = String.format(Locale.US, "%.2fs", run.time402M), modifier = Modifier.weight(1f), isHighlight = true)
          }
        }
      }
    }
  }
}

@Composable
private fun SplitMetricItem(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  isHighlight: Boolean = false
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(10.dp),
    color = if (isHighlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    border = BorderStroke(1.dp, if (isHighlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
        color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium.copy(
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          fontSize = 12.sp
        ),
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

/**
 * 4. Card Grande do Gráfico de Potência e Torque
 * - Altura entre 260 e 320 dp (290 dp)
 * - Cantos arredondados
 * - Tema escuro
 * - Eixo horizontal: RPM (ou km/h se RPM indisponível)
 * - Eixo vertical esquerdo: cv
 * - Eixo vertical direito: kgfm
 * - Desenha as curvas reais a partir dos pontos calculados em telemetria
 */
@Composable
private fun DynoPowerTorqueGraphCard(
  samples: List<RunSample> = emptyList(),
  hasVehicleConfig: Boolean = true,
  peakPowerCv: Float = 0f,
  peakTorqueKgfm: Float = 0f,
  modifier: Modifier = Modifier
) {
  // Filtra amostras com potência válida para o gráfico
  val powerPoints = remember(samples) {
    samples.filter { (it.enginePowerCv > 0f || it.wheelPowerCv > 0f) && it.filteredSpeedKmh > 0f }
  }

  val hasValidPlotData = powerPoints.size >= 3

  Card(
    modifier = modifier
      .height(290.dp)
      .testTag("dyno_graph_card"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = Color(0xFF0F141C)
    ),
    border = BorderStroke(1.dp, Color(0xFF263042))
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
      // Background Grid & Technical Axes Canvas
      Canvas(modifier = Modifier.fillMaxSize()) {
        val gridColor = Color(0xFF1E293B)
        val axisColor = Color(0xFF475569)

        val paddingLeft = 34.dp.toPx()
        val paddingRight = 34.dp.toPx()
        val paddingTop = 24.dp.toPx()
        val paddingBottom = 28.dp.toPx()

        val graphWidth = size.width - paddingLeft - paddingRight
        val graphHeight = size.height - paddingTop - paddingBottom

        // Draw horizontal grid lines
        val horizontalSteps = 4
        for (i in 0..horizontalSteps) {
          val y = paddingTop + (graphHeight / horizontalSteps) * i
          drawLine(
            color = if (i == horizontalSteps) axisColor else gridColor,
            start = Offset(paddingLeft, y),
            end = Offset(size.width - paddingRight, y),
            strokeWidth = if (i == horizontalSteps) 1.5f else 1f,
            pathEffect = if (i == horizontalSteps) null else PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
          )
        }

        // Draw vertical grid lines (RPM steps)
        val verticalSteps = 5
        for (i in 0..verticalSteps) {
          val x = paddingLeft + (graphWidth / verticalSteps) * i
          drawLine(
            color = if (i == 0 || i == verticalSteps) axisColor else gridColor,
            start = Offset(x, paddingTop),
            end = Offset(x, size.height - paddingBottom),
            strokeWidth = if (i == 0 || i == verticalSteps) 1.5f else 1f,
            pathEffect = if (i == 0 || i == verticalSteps) null else PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
          )
        }

        // Se houver dados reais de potência e torque, traçamos as curvas
        if (hasValidPlotData) {
          val hasRpm = powerPoints.any { (it.engineRpm ?: 0) > 500 }
          val maxPower = maxOf(peakPowerCv, powerPoints.maxOfOrNull { it.enginePowerCv } ?: 100f) * 1.15f
          val maxTorque = maxOf(peakTorqueKgfm, powerPoints.maxOfOrNull { it.engineTorqueKgfm } ?: 20f) * 1.15f

          val minX = if (hasRpm) {
            powerPoints.minOfOrNull { (it.engineRpm ?: 2000).toFloat() } ?: 1500f
          } else {
            powerPoints.minOfOrNull { it.filteredSpeedKmh } ?: 20f
          }

          val maxX = if (hasRpm) {
            powerPoints.maxOfOrNull { (it.engineRpm ?: 6000).toFloat() } ?: 6500f
          } else {
            powerPoints.maxOfOrNull { it.filteredSpeedKmh } ?: 120f
          }

          val rangeX = (maxX - minX).coerceAtLeast(1f)

          // Curva de Potência (Azul / Ciano)
          val powerPath = androidx.compose.ui.graphics.Path()
          var isFirstPower = true

          powerPoints.forEach { sample ->
            val pVal = if (sample.enginePowerCv > 0f) sample.enginePowerCv else sample.wheelPowerCv
            val xVal = if (hasRpm) (sample.engineRpm ?: 2000).toFloat() else sample.filteredSpeedKmh
            val normX = ((xVal - minX) / rangeX).coerceIn(0f, 1f)
            val normY = (pVal / maxPower.coerceAtLeast(1f)).coerceIn(0f, 1f)

            val px = paddingLeft + normX * graphWidth
            val py = size.height - paddingBottom - normY * graphHeight

            if (isFirstPower) {
              powerPath.moveTo(px, py)
              isFirstPower = false
            } else {
              powerPath.lineTo(px, py)
            }
          }

          drawPath(
            path = powerPath,
            color = Color(0xFF38BDF8),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
          )

          // Curva de Torque (Laranja)
          val torquePath = androidx.compose.ui.graphics.Path()
          var isFirstTorque = true

          powerPoints.forEach { sample ->
            val tVal = if (sample.engineTorqueKgfm > 0f) sample.engineTorqueKgfm else sample.wheelTorqueKgfm
            if (tVal > 0f) {
              val xVal = if (hasRpm) (sample.engineRpm ?: 2000).toFloat() else sample.filteredSpeedKmh
              val normX = ((xVal - minX) / rangeX).coerceIn(0f, 1f)
              val normY = (tVal / maxTorque.coerceAtLeast(1f)).coerceIn(0f, 1f)

              val px = paddingLeft + normX * graphWidth
              val py = size.height - paddingBottom - normY * graphHeight

              if (isFirstTorque) {
                torquePath.moveTo(px, py)
                isFirstTorque = false
              } else {
                torquePath.lineTo(px, py)
              }
            }
          }

          drawPath(
            path = torquePath,
            color = Color(0xFFFB923C),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
          )
        }
      }

      // Left Axis Label: cv
      Text(
        text = "cv",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp
        ),
        color = Color(0xFF38BDF8),
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(start = 2.dp, top = 2.dp)
      )

      // Right Axis Label: kgfm
      Text(
        text = "kgfm",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp
        ),
        color = Color(0xFFFB923C),
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(end = 2.dp, top = 2.dp)
      )

      // Bottom Axis Label: RPM ou km/h
      Text(
        text = "RPM",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          letterSpacing = 0.5.sp
        ),
        color = Color(0xFF94A3B8),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 2.dp)
      )

      // Se não houver pontos suficientes, exibe a moldura informativa
      if (!hasValidPlotData) {
        Column(
          modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = Color(0xFF1E293B)
          ) {
            Icon(
              imageVector = Icons.Outlined.ShowChart,
              contentDescription = null,
              tint = Color(0xFF38BDF8),
              modifier = Modifier
                .padding(10.dp)
                .size(24.dp)
            )
          }

          Text(
            text = "Curva calculada com base na telemetria GPS e inercial.",
            style = MaterialTheme.typography.bodySmall.copy(
              fontSize = 12.sp,
              lineHeight = 18.sp,
              fontWeight = FontWeight.Medium
            ),
            color = Color(0xFFCBD5E1),
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}

/**
 * 5. Legenda das Curvas (Imediatamente abaixo do gráfico e fora da área das curvas)
 */
@Composable
private fun DynoGraphLegend(modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.padding(horizontal = 8.dp, vertical = 2.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Linha azul — Potência
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Box(
        modifier = Modifier
          .width(20.dp)
          .height(4.dp)
          .background(Color(0xFF38BDF8), RoundedCornerShape(2.dp))
      )
      Text(
        text = "Potência (cv)",
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.SemiBold,
          fontSize = 12.sp
        ),
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    Spacer(modifier = Modifier.width(24.dp))

    // Linha laranja — Torque
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Box(
        modifier = Modifier
          .width(20.dp)
          .height(4.dp)
          .background(Color(0xFFFB923C), RoundedCornerShape(2.dp))
      )
      Text(
        text = "Torque (kgfm)",
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.SemiBold,
          fontSize = 12.sp
        ),
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

/**
 * 6. Cartão Recolhível "DETALHES DA MEDIÇÃO" (Fechado por padrão)
 */
@Composable
private fun MeasurementDetailsCard(
  run: RunResult,
  isExpanded: Boolean,
  onToggleExpand: () -> Unit,
  orderedSamples: List<RunSample>,
  modifier: Modifier = Modifier
) {
  val finishReasonEnum = FinishReason.fromCode(run.finishReason)
  var isSamplesTableExpanded by remember { mutableStateOf(false) }

  Card(
    modifier = modifier.testTag("card_measurement_details"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      // Header Clickable to Expand/Collapse
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onToggleExpand)
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.Assessment,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = "DETALHES DA MEDIÇÃO",
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
          ) {
            Text(
              text = if (isExpanded) "OCULTAR DETALHES" else "VER DETALHES",
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }

          Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Recolher detalhes" else "Expandir detalhes",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Collapsible Content
      AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          HorizontalDivider(
            thickness = 0.8.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
          )

          // Detalhes solicitados:
          DetailRow("Velocidade inicial (GPS)", String.format(Locale.US, "%.1f km/h", run.runStartGpsSpeedKmh))
          DetailRow("Velocidade máxima (GPS)", String.format(Locale.US, "%.1f km/h", run.maximumGpsSpeedKmh))
          val gain = if (run.speedGainKmh > 0f) run.speedGainKmh else (run.maximumGpsSpeedKmh - run.runStartGpsSpeedKmh).coerceAtLeast(0f)
          DetailRow("Ganho de velocidade", String.format(Locale.US, "+%.1f km/h", gain))
          DetailRow("Duração da janela válida", String.format(Locale.US, "%.2f s", run.elapsedSeconds))
          DetailRow("Precisão média GPS", String.format(Locale.US, "±%.1f m", run.gpsAccuracyMeters))
          DetailRow("Fixes de GPS registrados", "${run.validGpsLocationsCount} atualizações")

          if (run.peakLongitudinalG > 0f) {
            DetailRow("Força G de pico (longitudinal)", String.format(Locale.US, "%+.2f G", run.peakLongitudinalG))
          }
          if (run.averageLongitudinalG > 0f) {
            DetailRow("Força G média (longitudinal)", String.format(Locale.US, "%+.2f G", run.averageLongitudinalG))
          }
          if (run.totalVehicleMassKg > 0f) {
            DetailRow("Peso total do veículo", String.format(Locale.US, "%.0f kg", run.totalVehicleMassKg))
          }
          if (run.drivetrainLossPercent > 0f) {
            DetailRow("Perda de transmissão", String.format(Locale.US, "%.1f%%", run.drivetrainLossPercent))
          }

          val totalCount = if (run.totalSamples > 0) run.totalSamples else orderedSamples.size
          DetailRow("Amostras inerciais", "$totalCount (${run.rejectedSamples} descartadas)")
          DetailRow("Classificação da passagem", run.quality)
          DetailRow("Motivo do encerramento", finishReasonEnum.displayName)

          // Subseção de Amostras Gravadas da Passagem (opcional para análise técnica)
          if (orderedSamples.isNotEmpty()) {
            HorizontalDivider(
              thickness = 0.6.dp,
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
              modifier = Modifier.padding(vertical = 4.dp)
            )

            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
              )
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isSamplesTableExpanded = !isSamplesTableExpanded }
                    .padding(vertical = 2.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(Icons.Outlined.TableChart, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Pontos gravados (${orderedSamples.size})", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                  }
                  Icon(
                    imageVector = if (isSamplesTableExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                  )
                }

                if (isSamplesTableExpanded) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Text("Tempo", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                      Text("Força G", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                      Text("GPS km/h", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                      Text("Pot. cv", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }

                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState())
                    ) {
                      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        orderedSamples.forEach { sample ->
                          SampleTableRow(sample = sample)
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

/**
 * 7. Botões [ COMPARAR RESULTADOS ] e [ VER HISTÓRICO ]
 * - Lado a lado quando houver espaço, ou um abaixo do outro em telas estreitas.
 * - COMPARAR RESULTADOS habilitado somente com >= 2 testes válidos do mesmo veículo.
 */
@Composable
private fun ComparisonAndHistoryButtons(
  canCompare: Boolean,
  onCompareClick: () -> Unit,
  onHistoryClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val isNarrow = maxWidth < 380.dp

      if (isNarrow) {
        // Layout Vertical em telas estreitas
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Botão Comparar
          OutlinedButton(
            onClick = onCompareClick,
            enabled = canCompare,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("btn_compare_results"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("COMPARAR RESULTADOS", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
          }

          // Botão Histórico
          Button(
            onClick = onHistoryClick,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("btn_view_history"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
          ) {
            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("VER HISTÓRICO", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
          }
        }
      } else {
        // Layout Lado a Lado quando houver espaço
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onCompareClick,
            enabled = canCompare,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("btn_compare_results"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("COMPARAR", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
          }

          Button(
            onClick = onHistoryClick,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("btn_view_history"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
          ) {
            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("HISTÓRICO", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
          }
        }
      }
    }

    // Texto explicativo quando a comparação não estiver habilitada
    if (!canCompare) {
      Text(
        text = "Faça pelo menos duas passagens válidas para comparar.",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 2.dp)
      )
    }
  }
}

/**
 * Diálogo de Histórico com todas as passagens gravadas
 */
@Composable
private fun HistoryResultsDialog(
  results: List<RunResult>,
  currentSelectedId: String?,
  onSelectResult: (RunResult) -> Unit,
  onDeleteResult: (String) -> Unit,
  onClearAll: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text("Histórico de Testes", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        }

        if (results.isNotEmpty()) {
          IconButton(onClick = onClearAll) {
            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Limpar tudo", tint = MaterialTheme.colorScheme.error)
          }
        }
      }
    },
    text = {
      if (results.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Nenhum teste gravado no histórico.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      } else {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          results.forEach { run ->
            val isSelected = run.id == currentSelectedId
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(Date(run.timestamp))

            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectResult(run) },
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
              ),
              border = BorderStroke(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
              )
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Text(
                      text = formattedDate,
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (isSelected) {
                      Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Text(
                          text = "EXIBINDO",
                          style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                          color = MaterialTheme.colorScheme.onPrimary,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }
                  }

                  Text(
                    text = if (run.vehicleName.isNotEmpty()) run.vehicleName else "Veículo Principal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )

                  Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                      text = String.format(Locale.US, "GPS: %.1f km/h", run.maximumGpsSpeedKmh),
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    )
                    Text(
                      text = String.format(Locale.US, "Dur: %.2fs", run.elapsedSeconds),
                      style = MaterialTheme.typography.bodySmall
                    )
                  }
                }

                IconButton(
                  onClick = { onDeleteResult(run.id) },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
        Text("FECHAR")
      }
    }
  )
}

/**
 * Diálogo de Comparação entre os testes válidos do mesmo veículo
 */
@Composable
private fun ComparisonResultsDialog(
  validRuns: List<RunResult>,
  onDismiss: () -> Unit
) {
  val run1 = validRuns.getOrNull(0)
  val run2 = validRuns.getOrNull(1)

  val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Default.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("Comparativo de Passagens", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
      }
    },
    text = {
      if (run1 != null && run2 != null) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "Comparando os dois testes válidos mais recentes de ${run1.vehicleName.ifEmpty { "Veículo Principal" }}:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          // Cabeçalho das duas passagens
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
              .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Métrica", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.2f))
            Text(dateFormat.format(Date(run1.timestamp)), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            Text(dateFormat.format(Date(run2.timestamp)), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
          }

          ComparisonRow("Máx GPS", String.format(Locale.US, "%.1f km/h", run1.maximumGpsSpeedKmh), String.format(Locale.US, "%.1f km/h", run2.maximumGpsSpeedKmh))
          ComparisonRow("Máx Calc", String.format(Locale.US, "%.1f km/h", run1.maximumCalculatedSpeedKmh), String.format(Locale.US, "%.1f km/h", run2.maximumCalculatedSpeedKmh))
          ComparisonRow("Duração", String.format(Locale.US, "%.2f s", run1.elapsedSeconds), String.format(Locale.US, "%.2f s", run2.elapsedSeconds))
          ComparisonRow("Dif. média", String.format(Locale.US, "±%.1f km/h", run1.averageSpeedDifferenceKmh), String.format(Locale.US, "±%.1f km/h", run2.averageSpeedDifferenceKmh))
          ComparisonRow("Qualidade", run1.quality, run2.quality)
          ComparisonRow("Amostras", "${run1.totalSamples}", "${run2.totalSamples}")
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
        Text("FECHAR")
      }
    }
  )
}

@Composable
private fun ComparisonRow(label: String, val1: String, val2: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.2f))
    Text(val1, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    Text(val2, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
  }
}

@Composable
private fun DetailRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall.copy(
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace
      ),
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
private fun SampleTableRow(sample: RunSample) {
  val timeSec = sample.elapsedTimeMs / 1000.0
  val gValue = if (sample.longitudinalG != 0f) sample.longitudinalG else (sample.filteredAccelerationZ / 9.80665f)
  val powerDisplay = if (sample.enginePowerCv > 0f) {
    String.format(Locale.US, "%.0f", sample.enginePowerCv)
  } else {
    String.format(Locale.US, "%.1f", sample.calculatedSpeedKmh)
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 4.dp, vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = String.format(Locale.US, "%.2fs", timeSec),
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f)
    )
    Text(
      text = String.format(Locale.US, "%+.2f", gValue),
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
      color = DynoPowerCyan,
      modifier = Modifier.weight(1f),
      textAlign = TextAlign.End
    )
    Text(
      text = String.format(Locale.US, "%.1f", sample.gpsSpeedKmh),
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.weight(1.1f),
      textAlign = TextAlign.End
    )
    Text(
      text = powerDisplay,
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f),
      textAlign = TextAlign.End
    )
  }
}
