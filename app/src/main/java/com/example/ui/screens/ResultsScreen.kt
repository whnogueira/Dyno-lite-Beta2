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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.model.CurveDisplayType
import com.example.model.DynoCorrectionConfig
import com.example.model.DynoRecalculationEngine
import com.example.model.DynoRecalculationResult
import com.example.model.FinishReason
import com.example.model.RunResult
import com.example.model.RunSample
import com.example.model.TestResultRevision
import com.example.model.VehicleCalculations
import org.json.JSONObject
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

import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
  onStartNewTest: (String?) -> Unit = {},
  onOpenSimulator: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val runResultRepository = remember { RunResultRepository(context) }
  val coroutineScope = rememberCoroutineScope()

  // Observa fluxo em tempo real do banco de dados Room (DynoMobileDB)
  val flowResults by runResultRepository.getResultsFlow().collectAsStateWithLifecycle(initialValue = runResultRepository.getResults())
  var results by remember(flowResults) { mutableStateOf(flowResults) }

  // Result currently displayed on screen (defaults to most recent run if available)
  var currentDisplayedResult by remember(results) {
    mutableStateOf(results.firstOrNull())
  }

  var isMeasurementDetailsExpanded by remember { mutableStateOf(false) }
  var showHistoryDialog by remember { mutableStateOf(false) }
  var showComparisonDialog by remember { mutableStateOf(false) }
  var showClearAllConfirmDialog by remember { mutableStateOf(false) }
  var showCorrectRunDialog by remember { mutableStateOf(false) }
  var showOriginalConfigDialog by remember { mutableStateOf(false) }
  var selfTestResultMessage by remember { mutableStateOf<String?>(null) }
  var isRunningSelfTest by remember { mutableStateOf(false) }

  // Check valid tests for the current vehicle to evaluate comparison eligibility (BOA/REGULAR with gain >= 15 km/h)
  val currentVehicleName = currentDisplayedResult?.vehicleName ?: ""
  val isAccelCurrent = currentDisplayedResult?.testMode == "ACCELERATION"
  val validRunsForVehicle = remember(results, currentVehicleName, isAccelCurrent, currentDisplayedResult?.accelRangeLabel) {
    if (isAccelCurrent) {
      val targetLabel = currentDisplayedResult?.accelRangeLabel ?: ""
      results.filter {
        it.testMode == "ACCELERATION" &&
        it.vehicleName == currentVehicleName &&
        (it.quality == "BOA" || it.quality == "REGULAR") &&
        it.accelRangeLabel == targetLabel
      }
    } else {
      if (currentVehicleName.isNotEmpty()) {
        results.filter {
          it.testMode != "ACCELERATION" &&
          it.vehicleName == currentVehicleName &&
          (it.quality == "BOA" || it.quality == "REGULAR") &&
          it.speedGainKmh >= 15.0f
        }
      } else {
        results.filter {
          it.testMode != "ACCELERATION" &&
          (it.quality == "BOA" || it.quality == "REGULAR") &&
          it.speedGainKmh >= 15.0f
        }
      }
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

          if (run.testMode == "ACCELERATION") {
            AccelerationRunResultContent(
              run = run,
              formattedDate = formattedDate,
              isInvalid = isInvalid,
              canCompare = canCompare,
              runResultRepository = runResultRepository,
              onStartNewTest = { onStartNewTest(run.vehicleId) },
              onCompareClick = { showComparisonDialog = true },
              onHistoryClick = { showHistoryDialog = true }
            )
          } else if (isInvalid) {
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

            // BOTÃO SALVAR TESTE / TESTE SALVO
            SaveTestButtonRow(
              run = run,
              runResultRepository = runResultRepository,
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
            // Banner de DADOS INSUFICIENTES se a passagem teve ganho de velocidade insuficiente
            if (run.quality == "DADOS INSUFICIENTES") {
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("banner_insufficient_data"),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFEF3C7),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Surface(
                    shape = CircleShape,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.size(36.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                      )
                    }
                  }
                  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                      text = "DADOS INSUFICIENTES",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                      color = Color(0xFF92400E)
                    )
                    Text(
                      text = run.invalidationReason ?: "Estimativa preliminar — faixa de aceleração insuficiente.",
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                      color = Color(0xFFB45309)
                    )
                  }
                }
              }
            }

            if (run.isRecalculated) {
              RecalculatedRunBadgeCard(
                run = run,
                onViewOriginalConfig = { showOriginalConfigDialog = true },
                modifier = Modifier.fillMaxWidth()
              )
            }

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
              val isInsufficient = run.quality == "DADOS INSUFICIENTES"

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
                        Text(if (isInsufficient) "preliminar" else "est.", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = if (isInsufficient) FontWeight.Bold else FontWeight.Normal), color = if (isInsufficient) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                      val pCv = if (run.enginePowerCv > 0f) run.enginePowerCv else run.estimatedPowerCv
                      Text(String.format(Locale.US, "%.1f cv", pCv), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
                      if (isInsufficient) {
                        Text("Faixa insuficiente", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, color = Color(0xFFF59E0B)))
                      } else if (run.wheelPowerCv > 0f) {
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
                        Text(if (isInsufficient) "preliminar" else "est.", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                      val tKgfm = if (run.engineTorqueKgfm > 0f) run.engineTorqueKgfm else run.estimatedTorqueKgfm
                      val hasValidTorque = !isInsufficient && tKgfm > 0f && (run.peakTorqueRpm ?: 0) > 500
                      if (hasValidTorque) {
                        Text(String.format(Locale.US, "%.1f kgfm", tKgfm), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
                        if (run.wheelTorqueKgfm > 0f) {
                          Text(String.format(Locale.US, "Roda: %.1f kgfm", run.wheelTorqueKgfm), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                      } else {
                        Text("Indisponível", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("RPM não confiável", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                  run = run,
                  samples = runSamples,
                  hasVehicleConfig = true,
                  peakPowerCv = if (run.enginePowerCv > 0f) run.enginePowerCv else run.estimatedPowerCv,
                  peakTorqueKgfm = if (run.engineTorqueKgfm > 0f) run.engineTorqueKgfm else run.estimatedTorqueKgfm,
                  modifier = Modifier.fillMaxWidth()
                )
                DynoGraphLegend(
                  showTorque = !isInsufficient && ((run.engineTorqueKgfm > 0f || run.estimatedTorqueKgfm > 0f) && (run.peakTorqueRpm ?: 0) > 500),
                  modifier = Modifier.fillMaxWidth()
                )

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
                      tint = if (isInsufficient) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary
                    )
                    val marginText = when (run.quality) {
                      "BOA" -> "Estimativa Dyno Lite (margem ±10% com base em peso e arrasto). Não substitui dinamômetro certificado."
                      "REGULAR" -> "Estimativa Dyno Lite (margem ±15% com base em peso e arrasto). Não substitui dinamômetro certificado."
                      "DADOS INSUFICIENTES" -> "Estimativa preliminar (acima de ±20%). Faixa de aceleração insuficiente para homologação ou comparação."
                      else -> "Passagem não homologada para medição de potência."
                    }
                    Text(
                      text = marginText,
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
                  val qualityBadgeBg = when (run.quality) {
                    "BOA" -> MaterialTheme.colorScheme.primaryContainer
                    "REGULAR" -> MaterialTheme.colorScheme.tertiaryContainer
                    "DADOS INSUFICIENTES" -> Color(0xFFFEF3C7)
                    else -> MaterialTheme.colorScheme.errorContainer
                  }
                  val qualityBadgeText = when (run.quality) {
                    "BOA" -> MaterialTheme.colorScheme.onPrimaryContainer
                    "REGULAR" -> MaterialTheme.colorScheme.onTertiaryContainer
                    "DADOS INSUFICIENTES" -> Color(0xFF92400E)
                    else -> MaterialTheme.colorScheme.onErrorContainer
                  }
                  Surface(
                    shape = CircleShape,
                    color = qualityBadgeBg
                  ) {
                    Text(
                      text = run.quality,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                      color = qualityBadgeText
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

            // 7. BOTÕES SIMULADOR E CORREÇÃO DE DADOS
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Botão Simular a partir desta passagem
              if (onOpenSimulator != null) {
                Button(
                  onClick = { onOpenSimulator(run.id) },
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("btn_simulate_from_run"),
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6),
                    contentColor = Color.White
                  )
                ) {
                  Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "SIMULADOR",
                    style = MaterialTheme.typography.labelMedium.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp
                    )
                  )
                }
              }

              // Botão Corrigir dados da passagem
              OutlinedButton(
                onClick = { showCorrectRunDialog = true },
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("btn_correct_run_data"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DynoBlueLight.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                  contentColor = DynoBlueLight
                )
              ) {
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "CORRIGIR DADOS",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                  )
                )
              }
            }

            // BOTÃO SALVAR TESTE / TESTE SALVO
            SaveTestButtonRow(
              run = run,
              runResultRepository = runResultRepository,
              modifier = Modifier.fillMaxWidth()
            )

            // 8. BOTÃO REPETIR TESTE
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
      runResultRepository = runResultRepository,
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
      onRunSelfTest = {
        coroutineScope.launch {
          isRunningSelfTest = true
          val (ok, message) = runResultRepository.runStorageSelfTest()
          isRunningSelfTest = false
          selfTestResultMessage = message
        }
      },
      onClearAll = {
        showClearAllConfirmDialog = true
      },
      onDismiss = { showHistoryDialog = false }
    )
  }

  // DIÁLOGO DE RESULTADO DO TESTE DE ARMAZENAMENTO
  if (selfTestResultMessage != null) {
    AlertDialog(
      onDismissRequest = { selfTestResultMessage = null },
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          val isSuccess = selfTestResultMessage?.startsWith("SUCESSO") == true
          Icon(
            imageVector = if (isSuccess) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (isSuccess) DynoSuccessGreen else DynoErrorRed
          )
          Text(
            text = "Teste de Armazenamento",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        }
      },
      text = {
        Text(
          text = selfTestResultMessage ?: "",
          style = MaterialTheme.typography.bodyMedium
        )
      },
      confirmButton = {
        Button(
          onClick = { selfTestResultMessage = null },
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("FECHAR")
        }
      }
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

  var saveErrorMessage by remember { mutableStateOf<String?>(null) }

  // DIÁLOGO DE CORREÇÃO DE DADOS DA PASSAGEM (Seção 37)
  if (showCorrectRunDialog && currentDisplayedResult != null) {
    val run = currentDisplayedResult!!
    val samples = remember(run.id) { runResultRepository.getOrderedRunSamples(run.id) }
    CorrectRunDataDialog(
      run = run,
      samples = samples,
      onDismiss = { showCorrectRunDialog = false },
      onSaveThisRun = { correctedRun, note ->
        coroutineScope.launch {
          val res = runResultRepository.saveCorrectionSuspending(run.id, correctedRun, note)
          if (res.isSuccess) {
            val updated = res.getOrThrow()
            results = runResultRepository.getResults()
            currentDisplayedResult = updated
            showCorrectRunDialog = false
          } else {
            saveErrorMessage = "Não foi possível salvar a correção."
          }
        }
      },
      onSaveNewVersion = { correctedRun, note ->
        coroutineScope.launch {
          val res = runResultRepository.saveAsNewVersionSuspending(run, correctedRun, note)
          if (res.isSuccess) {
            val newRun = res.getOrThrow()
            results = runResultRepository.getResults()
            currentDisplayedResult = newRun
            showCorrectRunDialog = false
          } else {
            saveErrorMessage = "Não foi possível salvar a correção."
          }
        }
      }
    )
  }

  // DIÁLOGO DE ERRO AO SALVAR CORREÇÃO
  if (saveErrorMessage != null) {
    AlertDialog(
      onDismissRequest = { saveErrorMessage = null },
      title = {
        Text("Erro ao salvar", fontWeight = FontWeight.Bold)
      },
      text = {
        Text(saveErrorMessage ?: "Não foi possível salvar a correção.")
      },
      confirmButton = {
        Button(
          onClick = { saveErrorMessage = null },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Text("OK")
        }
      }
    )
  }

  // DIÁLOGO PARA VISUALIZAR CONFIGURAÇÃO ORIGINAL DA PASSAGEM
  if (showOriginalConfigDialog && currentDisplayedResult != null) {
    OriginalConfigDialog(
      run = currentDisplayedResult!!,
      onDismiss = { showOriginalConfigDialog = false }
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
 * - Eixo horizontal: RPM (ou km/h se RPM indisponível) com valores numéricos
 * - Eixo vertical esquerdo: cv com valores numéricos
 * - Eixo vertical direito: kgfm com valores numéricos
 * - Marcha, RPM inicial/final e Velocidade Inicial
 * - Faixa insuficiente para gerar curva confiável quando dados inadequados
 */
@Composable
private fun DynoPowerTorqueGraphCard(
  run: RunResult,
  samples: List<RunSample> = emptyList(),
  hasVehicleConfig: Boolean = true,
  peakPowerCv: Float = 0f,
  peakTorqueKgfm: Float = 0f,
  modifier: Modifier = Modifier
) {
  val curveType = remember(run, samples) {
    VehicleCalculations.evaluateCurveEligibility(
      samples = samples,
      gearUsed = run.gearUsed,
      gearRatio = run.gearRatioUsed,
      finalDrive = run.finalDriveUsed,
      tireCircumferenceM = 1.9,
      speedGainKmh = run.speedGainKmh
    )
  }

  val useRpm = curveType == CurveDisplayType.RPM
  val processedPoints = remember(samples, curveType) {
    if (curveType == CurveDisplayType.INSUFFICIENT) {
      emptyList()
    } else {
      processSamplesForGraph(samples, useRpm)
    }
  }

  val hasValidPlotData = curveType != CurveDisplayType.INSUFFICIENT && processedPoints.size >= 3

  val startSpeedDisplay = String.format(Locale.US, "%.1f km/h", if (run.runStartGpsSpeedKmh > 0f) run.runStartGpsSpeedKmh else run.startSpeedKmh)
  val gearDisplay = if (run.gearUsed.isNotBlank()) run.gearUsed else "Indisponível"

  val minRpm = if (useRpm && processedPoints.isNotEmpty()) processedPoints.minOf { it.x }.toInt() else null
  val maxRpm = if (useRpm && processedPoints.isNotEmpty()) processedPoints.maxOf { it.x }.toInt() else null
  val rpmRangeDisplay = if (minRpm != null && maxRpm != null && maxRpm > minRpm) "$minRpm - $maxRpm RPM" else "Indisponível"

  Card(
    modifier = modifier
      .height(310.dp)
      .testTag("dyno_graph_card"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = Color(0xFF0F141C)
    ),
    border = BorderStroke(1.dp, Color(0xFF263042))
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
      // Header com Marcha, RPM e Velocidade Inicial
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Marcha: $gearDisplay",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = Color(0xFF94A3B8)
          )
          Text(
            text = "RPM: $rpmRangeDisplay",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = Color(0xFF94A3B8)
          )
        }
        Text(
          text = "V. Inicial: $startSpeedDisplay",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
          color = Color(0xFF94A3B8)
        )
      }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .weight(1f)
      ) {
        // Background Grid & Technical Axes Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
          val gridColor = Color(0xFF1E293B)
          val axisColor = Color(0xFF475569)

          val paddingLeft = 38.dp.toPx()
          val paddingRight = 38.dp.toPx()
          val paddingTop = 16.dp.toPx()
          val paddingBottom = 26.dp.toPx()

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

          // Draw vertical grid lines
          val verticalSteps = 4
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

          // Se houver dados reais, traçamos as curvas
          if (hasValidPlotData) {
            val maxPower = maxOf(peakPowerCv, processedPoints.maxOfOrNull { it.powerCv } ?: 100f) * 1.15f
            val maxTorque = maxOf(peakTorqueKgfm, processedPoints.maxOfOrNull { it.torqueKgfm } ?: 20f) * 1.15f

            val minX = processedPoints.minOfOrNull { it.x } ?: if (useRpm) 1500f else 20f
            val maxX = processedPoints.maxOfOrNull { it.x } ?: if (useRpm) 6500f else 100f
            val rangeX = (maxX - minX).coerceAtLeast(1f)

            // Curva de Potência (Azul / Ciano)
            val powerPath = androidx.compose.ui.graphics.Path()
            var isFirstPower = true

            processedPoints.forEach { pt ->
              val normX = ((pt.x - minX) / rangeX).coerceIn(0f, 1f)
              val normY = (pt.powerCv / maxPower.coerceAtLeast(1f)).coerceIn(0f, 1f)

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

            // Curva de Torque (Laranja) - somente com RPM válido
            if (useRpm) {
              val torquePath = androidx.compose.ui.graphics.Path()
              var isFirstTorque = true

              processedPoints.forEach { pt ->
                if (pt.torqueKgfm > 0f) {
                  val normX = ((pt.x - minX) / rangeX).coerceIn(0f, 1f)
                  val normY = (pt.torqueKgfm / maxTorque.coerceAtLeast(1f)).coerceIn(0f, 1f)

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
        }

        // Left Axis Label & Value: cv
        Column(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = 2.dp, top = 2.dp)
        ) {
          Text(
            text = "cv",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = Color(0xFF38BDF8)
          )
          if (hasValidPlotData) {
            val maxPower = maxOf(peakPowerCv, processedPoints.maxOfOrNull { it.powerCv } ?: 100f) * 1.15f
            Text(
              text = "${maxPower.toInt()}",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
              color = Color(0xFF64748B)
            )
          }
        }

        // Right Axis Label & Value: kgfm
        if (useRpm) {
          Column(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(end = 2.dp, top = 2.dp),
            horizontalAlignment = Alignment.End
          ) {
            Text(
              text = "kgfm",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
              color = Color(0xFFFB923C)
            )
            if (hasValidPlotData) {
              val maxTorque = maxOf(peakTorqueKgfm, processedPoints.maxOfOrNull { it.torqueKgfm } ?: 20f) * 1.15f
              Text(
                text = String.format(Locale.US, "%.0f", maxTorque),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                color = Color(0xFF64748B)
              )
            }
          }
        }

        // Bottom Axis Label & Range Values: RPM ou km/h
        Row(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 38.dp, vertical = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (hasValidPlotData) {
            val minX = processedPoints.minOf { it.x }.toInt()
            val maxX = processedPoints.maxOf { it.x }.toInt()
            Text(
              text = "$minX",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontFamily = FontFamily.Monospace),
              color = Color(0xFF64748B)
            )
            Text(
              text = if (useRpm) "RPM" else "km/h",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp),
              color = Color(0xFF94A3B8)
            )
            Text(
              text = "$maxX",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontFamily = FontFamily.Monospace),
              color = Color(0xFF64748B)
            )
          } else {
            Spacer(modifier = Modifier.weight(1f))
            Text(
              text = if (useRpm) "RPM" else "km/h",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp),
              color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.weight(1f))
          }
        }

        // Se a faixa for insuficiente ou dados não plotáveis
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
                tint = if (curveType == CurveDisplayType.INSUFFICIENT) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                modifier = Modifier
                  .padding(10.dp)
                  .size(24.dp)
              )
            }

            Text(
              text = if (curveType == CurveDisplayType.INSUFFICIENT) "Faixa insuficiente para gerar curva confiável." else "Curva calculada com base na telemetria GPS e inercial.",
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium
              ),
              color = if (curveType == CurveDisplayType.INSUFFICIENT) Color(0xFFFCD34D) else Color(0xFFCBD5E1),
              textAlign = TextAlign.Center
            )
          }
        }
      }
    }
  }
}

/**
 * 5. Legenda das Curvas (Imediatamente abaixo do gráfico e fora da área das curvas)
 */
@Composable
private fun DynoGraphLegend(
  showTorque: Boolean = true,
  modifier: Modifier = Modifier
) {
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

    if (showTorque) {
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

          if (run.gearUsed.isNotBlank()) {
            DetailRow("Marcha utilizada", "${run.gearUsed} (relação: ${String.format(Locale.US, "%.2f", run.gearRatioUsed)})")
          }
          if ((run.peakPowerRpm ?: 0) > 0) {
            DetailRow("RPM no pico de potência", "${run.peakPowerRpm} RPM")
          }
          if ((run.peakTorqueRpm ?: 0) > 0) {
            DetailRow("RPM no pico de torque", "${run.peakTorqueRpm} RPM")
          }
          if (run.peakLongitudinalG > 0f) {
            DetailRow("Força G de pico (longitudinal)", String.format(Locale.US, "%+.2f G", run.peakLongitudinalG))
          }
          if (run.averageLongitudinalG > 0f) {
            DetailRow("Força G média (longitudinal)", String.format(Locale.US, "%+.2f G", run.averageLongitudinalG))
          }
          if (run.totalVehicleMassKg > 0f) {
            val breakdown = buildString {
              append(String.format(Locale.US, "%.0f kg", run.totalVehicleMassKg))
              val parts = mutableListOf<String>()
              if (run.curbWeightKg > 0f) parts.add("vazio: ${run.curbWeightKg.toInt()}kg")
              if (run.driverWeightKg > 0f) parts.add("mot: ${run.driverWeightKg.toInt()}kg")
              if (run.passengerWeightKg > 0f) {
                val pStr = if (run.passengerCount > 0) "${run.passengerCount} pass (${run.passengerWeightKg.toInt()}kg)" else "pass: ${run.passengerWeightKg.toInt()}kg"
                parts.add(pStr)
              }
              if (run.additionalWeightKg > 0f) parts.add("carga: ${run.additionalWeightKg.toInt()}kg")
              if (run.fuelAdjustmentKg > 0f) parts.add("comb: ${run.fuelAdjustmentKg.toInt()}kg")
              if (parts.isNotEmpty()) {
                append(" (${parts.joinToString(" + ")})")
              }
            }
            DetailRow("Peso total do teste", breakdown)
          }
          if (run.drivetrainLossPercent > 0f) {
            DetailRow("Perda de transmissão", String.format(Locale.US, "%.1f%%", run.drivetrainLossPercent))
          }
          if (run.estimatedMarginPercent > 0f) {
            DetailRow("Margem estimada", String.format(Locale.US, "±%.1f%%", run.estimatedMarginPercent))
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
  runResultRepository: RunResultRepository,
  currentSelectedId: String?,
  onSelectResult: (RunResult) -> Unit,
  onDeleteResult: (String) -> Unit,
  onRunSelfTest: () -> Unit,
  onClearAll: () -> Unit,
  onDismiss: () -> Unit
) {
  var incompleteTests by remember { mutableStateOf<List<RunResult>>(emptyList()) }

  androidx.compose.runtime.LaunchedEffect(Unit) {
    incompleteTests = runResultRepository.getIncompleteTests()
  }

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
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 440.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Botão de Auto-Diagnóstico de Armazenamento (Requisito 12)
        OutlinedButton(
          onClick = onRunSelfTest,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("btn_test_storage_selftest"),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, DynoBlueLight.copy(alpha = 0.6f))
        ) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = DynoBlueLight)
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "TESTAR ARMAZENAMENTO (DIAGNÓSTICO)",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = DynoBlueLight
          )
        }

        // Filtro por tipo de teste (Requisito 10)
        var historyFilter by remember { mutableStateOf("TODOS") }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(2.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          listOf("TODOS", "DINAMÔMETRO", "ACELERAÇÃO").forEach { filterName ->
            val isFilterSelected = historyFilter == filterName
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (isFilterSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
              modifier = Modifier
                .weight(1f)
                .clickable { historyFilter = filterName }
            ) {
              Box(
                modifier = Modifier.padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = filterName,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isFilterSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 9.5.sp
                  ),
                  color = if (isFilterSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        val filteredResults = remember(results, historyFilter) {
          when (historyFilter) {
            "DINAMÔMETRO" -> results.filter { it.testMode != "ACCELERATION" }
            "ACELERAÇÃO" -> results.filter { it.testMode == "ACCELERATION" }
            else -> results
          }
        }

        // Seção: Testes Não Concluídos (se houver)
        if (incompleteTests.isNotEmpty()) {
          Text(
            text = "Testes não concluídos (${incompleteTests.size}):",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = DynoTorqueOrange)
          )
          incompleteTests.forEach { incomplete ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(containerColor = DynoTorqueOrange.copy(alpha = 0.12f)),
              border = BorderStroke(1.dp, DynoTorqueOrange.copy(alpha = 0.35f))
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = incomplete.vehicleName.ifBlank { "Passagem Interrompida" },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = DynoTextPrimary
                  )
                  Text(
                    text = "Amostras gravadas: ${incomplete.samples.size} (não finalizado)",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = DynoTextSecondary
                  )
                }
                IconButton(
                  onClick = {
                    onDeleteResult(incomplete.id)
                    incompleteTests = incompleteTests.filter { it.id != incomplete.id }
                  },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
          HorizontalDivider(thickness = 0.8.dp, color = DynoDivider)
        }

        // Seção: Testes Concluídos
        if (filteredResults.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (results.isEmpty()) "Nenhum teste gravado no histórico." else "Nenhum teste correspondente ao filtro.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          Text(
            text = "Testes concluídos (${filteredResults.size}):",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = DynoBlueLight)
          )
          filteredResults.forEach { run ->
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
                    if (run.isRecalculated) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = DynoTorqueOrange.copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, DynoTorqueOrange)
                      ) {
                        Text(
                          text = if (run.revisionNumber > 1) "RECÁLCULO v${run.revisionNumber}" else "RECÁLCULO",
                          style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                          color = DynoTorqueOrange,
                          modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                      }
                    }
                  }

                  Text(
                    text = if (run.vehicleName.isNotEmpty()) run.vehicleName else "Veículo Principal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )

                  if (run.testMode == "ACCELERATION") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                      Text(
                        text = run.accelRangeLabel,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = DynoPowerCyan)
                      )
                      Text(
                        text = String.format(Locale.US, "%.2fs", run.elapsedSeconds),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                      )
                      Text(
                        text = String.format(Locale.US, "Máx: %.2fG", run.peakLongitudinalG),
                        style = MaterialTheme.typography.bodySmall
                      )
                    }
                  } else {
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
          val isAccelCompare = run1.testMode == "ACCELERATION"
          Text(
            text = if (isAccelCompare) {
              "Comparando os dois testes de aceleração de ${run1.vehicleName.ifEmpty { "Veículo Principal" }} (${run1.accelRangeLabel}):"
            } else {
              "Comparando os dois testes válidos mais recentes de ${run1.vehicleName.ifEmpty { "Veículo Principal" }}:"
            },
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

          if (isAccelCompare) {
            ComparisonRow("Faixa", run1.accelRangeLabel, run2.accelRangeLabel)
            ComparisonRow("Tempo", String.format(Locale.US, "%.2f s", run1.elapsedSeconds), String.format(Locale.US, "%.2f s", run2.elapsedSeconds))
            ComparisonRow("Margem de erro", String.format(Locale.US, "±%.2f s", run1.estimatedMarginSeconds), String.format(Locale.US, "±%.2f s", run2.estimatedMarginSeconds))
            ComparisonRow("Aceleração máx", String.format(Locale.US, "%.2f G", run1.peakLongitudinalG), String.format(Locale.US, "%.2f G", run2.peakLongitudinalG))
            ComparisonRow("Distância", String.format(Locale.US, "%.1f m", run1.totalDistanceMeters), String.format(Locale.US, "%.1f m", run2.totalDistanceMeters))
            ComparisonRow("Precisão GPS", String.format(Locale.US, "±%.1f m", run1.gpsAccuracyMeters), String.format(Locale.US, "±%.1f m", run2.gpsAccuracyMeters))
            ComparisonRow("Trocas de marcha", "${run1.gearShiftCount}", "${run2.gearShiftCount}")
            ComparisonRow("Qualidade", run1.quality, run2.quality)
          } else {
            ComparisonRow("Máx GPS", String.format(Locale.US, "%.1f km/h", run1.maximumGpsSpeedKmh), String.format(Locale.US, "%.1f km/h", run2.maximumGpsSpeedKmh))
            ComparisonRow("Máx Calc", String.format(Locale.US, "%.1f km/h", run1.maximumCalculatedSpeedKmh), String.format(Locale.US, "%.1f km/h", run2.maximumCalculatedSpeedKmh))
            ComparisonRow("Duração", String.format(Locale.US, "%.2f s", run1.elapsedSeconds), String.format(Locale.US, "%.2f s", run2.elapsedSeconds))
            ComparisonRow("Dif. média", String.format(Locale.US, "±%.1f km/h", run1.averageSpeedDifferenceKmh), String.format(Locale.US, "±%.1f km/h", run2.averageSpeedDifferenceKmh))
            ComparisonRow("Qualidade", run1.quality, run2.quality)
            ComparisonRow("Amostras", "${run1.totalSamples}", "${run2.totalSamples}")
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

/**
 * Badge visual de passagem com dados recalculados/corrigidos (Requisito 6)
 */
@Composable
private fun RecalculatedRunBadgeCard(
  run: RunResult,
  onViewOriginalConfig: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("card_recalculated_badge"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = DynoTorqueOrange.copy(alpha = 0.12f)),
    border = BorderStroke(1.dp, DynoTorqueOrange.copy(alpha = 0.45f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
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
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = DynoTorqueOrange,
            modifier = Modifier.padding(vertical = 2.dp)
          ) {
            Text(
              text = "RECALCULADO APÓS CORREÇÃO",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
              ),
              color = Color.Black,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
          if (run.revisionNumber > 1) {
            Text(
              text = "Revisão ${run.revisionNumber}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = DynoTorqueOrange
            )
          }
        }

        TextButton(
          onClick = onViewOriginalConfig,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.testTag("btn_view_original_config")
        ) {
          Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp), tint = DynoBlueLight)
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "VER CONFIGURAÇÃO ORIGINAL",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = DynoBlueLight
          )
        }
      }

      if (!run.recalculationNote.isNullOrBlank()) {
        Text(
          text = "Observação: ${run.recalculationNote}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      } else if (!run.recalculationReason.isNullOrBlank()) {
        Text(
          text = run.recalculationReason!!,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun ComparisonRow(
  label: String,
  original: String,
  newVal: String,
  diff: String? = null,
  isHighlight: Boolean = false
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal),
      color = if (isHighlight) DynoPowerCyan else MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = original,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Icon(
        imageVector = Icons.Default.CompareArrows,
        contentDescription = null,
        modifier = Modifier.size(12.dp),
        tint = MaterialTheme.colorScheme.outline
      )
      Text(
        text = newVal,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
        color = if (isHighlight) DynoPowerCyan else MaterialTheme.colorScheme.onSurface
      )
      if (diff != null) {
        Text(
          text = diff,
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
          ),
          color = if (diff.startsWith("+")) DynoSuccessGreen else if (diff.startsWith("-")) DynoErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

/**
 * Diálogo para visualizar a configuração original e comparar com os dados atuais (Requisito 6)
 */
@Composable
fun OriginalConfigDialog(
  run: RunResult,
  onDismiss: () -> Unit
) {
  val originalConfig = remember(run.previousConfigurationJson) {
    if (!run.previousConfigurationJson.isNullOrBlank()) {
      try {
        JSONObject(run.previousConfigurationJson)
      } catch (e: Exception) {
        null
      }
    } else null
  }
  val originalCalc = remember(run.previousCalculatedResultJson) {
    if (!run.previousCalculatedResultJson.isNullOrBlank()) {
      try {
        JSONObject(run.previousCalculatedResultJson)
      } catch (e: Exception) {
        null
      }
    } else null
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Default.History, contentDescription = null, tint = DynoBlueLight)
        Text(
          text = "Configuração Original da Passagem",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Valores registrados no momento em que a puxada foi feita versus o resultado recalculado atual.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Comparativo de Resultados
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "COMPARATIVO DE RESULTADOS",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = DynoPowerCyan)
            )
            HorizontalDivider(thickness = 0.5.dp, color = DynoDivider)

            val origEnginePower = originalCalc?.optDouble("enginePowerCv", 0.0)?.toFloat() ?: 0f
            val origWheelPower = originalCalc?.optDouble("wheelPowerCv", 0.0)?.toFloat() ?: 0f
            val origTorque = originalCalc?.optDouble("engineTorqueKgfm", 0.0)?.toFloat() ?: 0f
            val origMass = originalCalc?.optDouble("totalMassKg", 0.0)?.toFloat() ?: originalConfig?.optDouble("totalMassKg", 0.0)?.toFloat() ?: 0f

            ComparisonRow("Massa Total", "${origMass.toInt()} kg", "${run.totalVehicleMassKg.toInt()} kg", isHighlight = false)
            ComparisonRow("Potência Motor", String.format(Locale.US, "%.1f cv", origEnginePower), String.format(Locale.US, "%.1f cv", run.enginePowerCv), isHighlight = true)
            ComparisonRow("Potência Rodas", String.format(Locale.US, "%.1f cv", origWheelPower), String.format(Locale.US, "%.1f cv", run.wheelPowerCv))
            ComparisonRow("Torque Motor", String.format(Locale.US, "%.1f kgfm", origTorque), String.format(Locale.US, "%.1f kgfm", run.engineTorqueKgfm))
          }
        }

        // Parâmetros Originais de Entrada
        if (originalConfig != null) {
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "PARÂMETROS REGISTRADOS NA PASSAGEM",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = DynoTextSecondary)
              )
              HorizontalDivider(thickness = 0.5.dp, color = DynoDivider)

              DetailRow("Peso vazio veículo", "${originalConfig.optDouble("curbWeightKg", 0.0).toInt()} kg")
              DetailRow("Peso motorista", "${originalConfig.optDouble("driverWeightKg", 0.0).toInt()} kg")
              val passCount = originalConfig.optInt("passengerCount", 0)
              val passWeight = originalConfig.optDouble("passengerWeightKg", 0.0).toInt()
              DetailRow("Passageiros", "$passCount (${passWeight} kg)")
              DetailRow("Carga adicional", "${originalConfig.optDouble("additionalWeightKg", 0.0).toInt()} kg")
              DetailRow("Som automotivo", "${originalConfig.optDouble("soundSystemWeightKg", 0.0).toInt()} kg")
              DetailRow("Kit GNV", "${originalConfig.optDouble("cngWeightKg", 0.0).toInt()} kg")
              val tW = originalConfig.optInt("tireWidthMm", 195)
              val tA = originalConfig.optInt("tireAspectRatio", 55)
              val rI = originalConfig.optInt("rimInches", 15)
              DetailRow("Pneus", "$tW/${tA}R$rI")
              DetailRow("Marcha puxada", originalConfig.optString("gearUsed", "2ª"))
              DetailRow("Relação marcha", String.format(Locale.US, "%.2f", originalConfig.optDouble("gearRatio", 0.0)))
              DetailRow("Diferencial", String.format(Locale.US, "%.2f", originalConfig.optDouble("finalDrive", 0.0)))
              DetailRow("Perda transmissão", String.format(Locale.US, "%.1f%%", originalConfig.optDouble("drivetrainLossPercent", 0.0)))
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
 * Diálogo de Correção de Dados da Passagem (Seção 37)
 * Permite ao usuário editar parâmetros de veículo/carga e recalcular potência/torque reais
 * utilizando os dados de velocidade e tempo brutos gravados na passagem original.
 */
@Composable
fun CorrectRunDataDialog(
  run: RunResult,
  samples: List<RunSample>,
  onDismiss: () -> Unit,
  onSaveThisRun: (RunResult, String?) -> Unit,
  onSaveNewVersion: (RunResult, String?) -> Unit
) {
  val initialConfig = remember(run) { DynoRecalculationEngine.extractConfigFromRun(run) }

  // 1. Peso
  var curbWeightText by remember { mutableStateOf(String.format(Locale.US, "%.0f", initialConfig.curbWeightKg)) }
  var fuelLitersText by remember { mutableStateOf(if (initialConfig.fuelWeightKg > 0f) String.format(Locale.US, "%.0f", initialConfig.fuelWeightKg / 0.74f) else "25") }
  var selectedFuelType by remember { mutableStateOf("Gasolina") }

  // 2. Ocupantes
  var driverWeightText by remember { mutableStateOf(String.format(Locale.US, "%.0f", initialConfig.driverWeightKg)) }
  var passengerWeightMode by remember { mutableStateOf(initialConfig.passengerWeightMode) }
  var passengerCountText by remember { mutableStateOf(initialConfig.passengerCount.toString()) }
  var passengerIndividualWeightText by remember { mutableStateOf(if (initialConfig.passengerIndividualWeightKg > 0f) String.format(Locale.US, "%.0f", initialConfig.passengerIndividualWeightKg) else "70") }
  var passengerTotalWeightText by remember { mutableStateOf(if (initialConfig.passengerTotalWeightKg > 0f) String.format(Locale.US, "%.0f", initialConfig.passengerTotalWeightKg) else "") }
  var cargoWeightText by remember { mutableStateOf(String.format(Locale.US, "%.0f", initialConfig.cargoWeightKg)) }
  var soundSystemWeightText by remember { mutableStateOf(String.format(Locale.US, "%.0f", initialConfig.soundSystemWeightKg)) }
  var cngWeightText by remember { mutableStateOf(String.format(Locale.US, "%.0f", initialConfig.cngWeightKg)) }

  // 3. Transmissão
  var gearUsedText by remember { mutableStateOf(initialConfig.gearUsed) }
  var gearRatioText by remember { mutableStateOf(String.format(Locale.US, "%.2f", initialConfig.gearRatio)) }
  var finalDriveText by remember { mutableStateOf(String.format(Locale.US, "%.2f", initialConfig.finalDriveRatio)) }
  var lossPercentText by remember { mutableStateOf(String.format(Locale.US, "%.1f", initialConfig.drivetrainLossPercent)) }

  // 4. Pneus
  var tireWidthText by remember { mutableStateOf(initialConfig.tireWidthMm.toString()) }
  var tireAspectText by remember { mutableStateOf(initialConfig.tireAspectRatio.toString()) }
  var rimInchesText by remember { mutableStateOf(initialConfig.rimInches.toString()) }

  // 5. Parâmetros Avançados
  var crrText by remember { mutableStateOf(String.format(Locale.US, "%.3f", if (initialConfig.crr > 0f) initialConfig.crr else 0.015f)) }
  var cdText by remember { mutableStateOf(String.format(Locale.US, "%.2f", initialConfig.cd)) }
  var frontalAreaText by remember { mutableStateOf(String.format(Locale.US, "%.2f", initialConfig.frontalAreaM2)) }

  // Motivo/Observação curta
  var noteText by remember { mutableStateOf("") }

  // Construção reativa da configuração corrigida
  val parsedConfig = remember(
    curbWeightText, driverWeightText, passengerWeightMode, passengerCountText,
    passengerIndividualWeightText, passengerTotalWeightText,
    cargoWeightText, soundSystemWeightText, cngWeightText, fuelLitersText, selectedFuelType,
    gearUsedText, gearRatioText, finalDriveText, lossPercentText,
    tireWidthText, tireAspectText, rimInchesText, crrText, cdText, frontalAreaText
  ) {
    val curb = curbWeightText.replace(',', '.').toFloatOrNull() ?: 0f
    val driver = driverWeightText.replace(',', '.').toFloatOrNull() ?: 0f
    val passCount = passengerCountText.toIntOrNull() ?: 0
    val passIndiv = passengerIndividualWeightText.replace(',', '.').toFloatOrNull() ?: 70f
    val passDirectTotal = passengerTotalWeightText.replace(',', '.').toFloatOrNull() ?: 0f

    val passTotal = when (passengerWeightMode) {
      DynoCorrectionConfig.PassengerWeightMode.INDIVIDUAL -> (passCount * passIndiv).coerceAtLeast(0f)
      DynoCorrectionConfig.PassengerWeightMode.TOTAL -> passDirectTotal.coerceAtLeast(0f)
    }

    val cargo = cargoWeightText.replace(',', '.').toFloatOrNull() ?: 0f
    val sound = soundSystemWeightText.replace(',', '.').toFloatOrNull() ?: 0f
    val cng = cngWeightText.replace(',', '.').toFloatOrNull() ?: 0f
    val fuelLiters = fuelLitersText.replace(',', '.').toFloatOrNull() ?: 0f
    val fuelDensityKgL = when (selectedFuelType.lowercase()) {
      "etanol" -> 0.79f
      "diesel" -> 0.84f
      else -> 0.74f // gasolina
    }
    val fuelWeight = fuelLiters * fuelDensityKgL

    val tWidth = tireWidthText.toIntOrNull() ?: 195
    val tAspect = tireAspectText.toIntOrNull() ?: 55
    val rim = rimInchesText.toIntOrNull() ?: 15
    val gRatio = gearRatioText.replace(',', '.').toFloatOrNull() ?: 2.14f
    val fd = finalDriveText.replace(',', '.').toFloatOrNull() ?: 4.19f
    val loss = lossPercentText.replace(',', '.').toFloatOrNull() ?: 12f
    val crrVal = crrText.replace(',', '.').toFloatOrNull() ?: 0.015f
    val cdVal = cdText.replace(',', '.').toFloatOrNull() ?: 0.34f
    val area = frontalAreaText.replace(',', '.').toFloatOrNull() ?: 2.10f

    DynoCorrectionConfig(
      curbWeightKg = curb,
      driverWeightKg = driver,
      passengerCount = passCount,
      passengerWeightMode = passengerWeightMode,
      passengerIndividualWeightKg = passIndiv,
      passengerTotalWeightKg = passTotal,
      cargoWeightKg = cargo,
      soundSystemWeightKg = sound,
      cngWeightKg = cng,
      fuelWeightKg = fuelWeight,
      gearUsed = gearUsedText.ifBlank { "2ª" },
      gearRatio = gRatio,
      finalDriveRatio = fd,
      tireWidthMm = tWidth,
      tireAspectRatio = tAspect,
      rimInches = rim,
      drivetrainLossPercent = loss,
      crr = crrVal,
      cd = cdVal,
      frontalAreaM2 = area
    )
  }

  // Validação dos dados em tempo real
  val validationError = remember(parsedConfig) {
    val total = parsedConfig.totalMassKg
    when {
      parsedConfig.curbWeightKg <= 0f -> "Peso do veículo vazio deve ser maior que zero."
      parsedConfig.driverWeightKg < 0f -> "Peso do motorista não pode ser negativo."
      parsedConfig.cargoWeightKg < 0f -> "Carga adicional não pode ser negativa."
      parsedConfig.soundSystemWeightKg < 0f -> "Peso do som não pode ser negativo."
      parsedConfig.cngWeightKg < 0f -> "Peso do kit GNV não pode ser negativo."
      parsedConfig.fuelWeightKg < 0f -> "Peso do combustível não pode ser negativo."
      total < 300f -> "Massa total (${total.toInt()} kg) é inferior ao mínimo permitido (300 kg)."
      total > 5000f -> "Massa total (${total.toInt()} kg) excede o limite máximo permitido (5000 kg)."
      parsedConfig.gearUsed.isBlank() -> "Marcha utilizada deve ser informada."
      parsedConfig.gearRatio <= 0f || parsedConfig.gearRatio !in 0.2f..8.0f -> "Relação de marcha deve ser maior que zero (0.2 a 8.0)."
      parsedConfig.finalDriveRatio <= 0f || parsedConfig.finalDriveRatio !in 0.5f..10.0f -> "Diferencial deve ser maior que zero (0.5 a 10.0)."
      parsedConfig.tireWidthMm !in 125..385 -> "Largura do pneu inválida (125 a 385 mm)."
      parsedConfig.tireAspectRatio !in 20..90 -> "Perfil do pneu inválido (20 a 90%)."
      parsedConfig.rimInches !in 10..26 -> "Aro inválido (10 a 26 polegadas)."
      parsedConfig.drivetrainLossPercent !in 0f..40f -> "Perda da transmissão deve estar entre 0% e 40%."
      parsedConfig.crr !in 0.005f..0.050f -> "Crr deve estar entre 0.005 e 0.050."
      parsedConfig.cd !in 0.15f..1.0f -> "Coeficiente Cd deve ser entre 0.15 e 1.0."
      parsedConfig.frontalAreaM2 !in 1.0f..5.0f -> "Área frontal deve ser entre 1.0 e 5.0 m²."
      else -> null
    }
  }

  // Recálculo dinâmico da passagem
  val recalculationResult = remember(parsedConfig, validationError, samples, run) {
    if (validationError != null) null
    else {
      try {
        DynoRecalculationEngine.recalculate(samples, parsedConfig, run)
      } catch (e: Exception) {
        null
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Default.Edit, contentDescription = null, tint = DynoBluePrimary)
        Text(
          text = "CORRIGIR DADOS DA PASSAGEM",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Aviso Obrigatório (Requisito 12)
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = DynoBluePrimary.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, DynoBluePrimary.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = DynoBluePrimary, modifier = Modifier.size(20.dp))
            Text(
              text = "Os dados medidos pelo GPS e pelos sensores não serão alterados. Somente potência, torque e RPM serão recalculados.",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        // Banner de erro de validação
        if (validationError != null) {
          Surface(
            color = DynoErrorRed.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.Close, contentDescription = null, tint = DynoErrorRed, modifier = Modifier.size(18.dp))
              Text(
                text = validationError,
                color = DynoErrorRed,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
              )
            }
          }
        }

        // -------------------------------------------------------------
        // SEÇÃO 1: PESO
        // -------------------------------------------------------------
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "1. PESO",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = DynoPowerCyan)
            )
            HorizontalDivider(thickness = 0.5.dp, color = DynoDivider)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = curbWeightText,
                onValueChange = { curbWeightText = it },
                label = { Text("Veículo vazio (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_curb_weight")
              )
              OutlinedTextField(
                value = fuelLitersText,
                onValueChange = { fuelLitersText = it },
                label = { Text("Combustível (L)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_fuel_liters")
              )
            }

            // Seleção de Tipo de Combustível
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Tipo:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              listOf("Gasolina", "Etanol", "Diesel").forEach { fType ->
                val isSel = selectedFuelType.equals(fType, ignoreCase = true)
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                  modifier = Modifier.clickable { selectedFuelType = fType }
                ) {
                  Text(
                    text = fType,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal),
                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }
            }
          }
        }

        // -------------------------------------------------------------
        // SEÇÃO 2: OCUPANTES E CARGA
        // -------------------------------------------------------------
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "2. OCUPANTES",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = DynoPowerCyan)
            )
            HorizontalDivider(thickness = 0.5.dp, color = DynoDivider)

            OutlinedTextField(
              value = driverWeightText,
              onValueChange = { driverWeightText = it },
              label = { Text("Motorista (kg)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("input_driver_weight")
            )

            // Modo de Passageiros: Individual vs Total
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Cálculo passageiros:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (passengerWeightMode == DynoCorrectionConfig.PassengerWeightMode.INDIVIDUAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { passengerWeightMode = DynoCorrectionConfig.PassengerWeightMode.INDIVIDUAL }
              ) {
                Text(
                  text = "Individual",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = if (passengerWeightMode == DynoCorrectionConfig.PassengerWeightMode.INDIVIDUAL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (passengerWeightMode == DynoCorrectionConfig.PassengerWeightMode.TOTAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { passengerWeightMode = DynoCorrectionConfig.PassengerWeightMode.TOTAL }
              ) {
                Text(
                  text = "Total direto",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = if (passengerWeightMode == DynoCorrectionConfig.PassengerWeightMode.TOTAL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            if (passengerWeightMode == DynoCorrectionConfig.PassengerWeightMode.INDIVIDUAL) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                  value = passengerCountText,
                  onValueChange = { passengerCountText = it },
                  label = { Text("Qtd Passageiros") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("input_passenger_count")
                )
                OutlinedTextField(
                  value = passengerIndividualWeightText,
                  onValueChange = { passengerIndividualWeightText = it },
                  label = { Text("Peso Médio (kg/cada)") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("input_passenger_indiv_weight")
                )
              }
            } else {
              OutlinedTextField(
                value = passengerTotalWeightText,
                onValueChange = { passengerTotalWeightText = it },
                label = { Text("Peso Total dos Passageiros (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_passenger_total_weight")
              )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = cargoWeightText,
                onValueChange = { cargoWeightText = it },
                label = { Text("Carga extra (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_cargo_weight")
              )
              OutlinedTextField(
                value = soundSystemWeightText,
                onValueChange = { soundSystemWeightText = it },
                label = { Text("Som (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_sound_weight")
              )
              OutlinedTextField(
                value = cngWeightText,
                onValueChange = { cngWeightText = it },
                label = { Text("GNV (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_cng_weight")
              )
            }

            // Resumo de Massa em tempo real (Requisito 2)
            val curb = parsedConfig.curbWeightKg
            val driver = parsedConfig.driverWeightKg
            val pass = parsedConfig.effectivePassengerWeightKg
            val cargo = parsedConfig.cargoWeightKg + parsedConfig.soundSystemWeightKg + parsedConfig.cngWeightKg
            val total = parsedConfig.totalMassKg

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = DynoTorqueOrange.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, DynoTorqueOrange.copy(alpha = 0.35f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                  text = "RESUMO DE MASSA EM TEMPO REAL",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = DynoTorqueOrange)
                )
                Text("Veículo vazio: ${curb.toInt()} kg", style = MaterialTheme.typography.bodySmall)
                Text("Motorista: ${driver.toInt()} kg", style = MaterialTheme.typography.bodySmall)
                Text("Passageiros: ${pass.toInt()} kg", style = MaterialTheme.typography.bodySmall)
                Text("Carga adicional: ${cargo.toInt()} kg", style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(thickness = 0.5.dp, color = DynoTorqueOrange.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 2.dp))
                Text(
                  text = "Massa total corrigida: ${total.toInt()} kg",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = DynoTorqueOrange)
                )
              }
            }
          }
        }

        // -------------------------------------------------------------
        // SEÇÃO 3: TRANSMISSÃO
        // -------------------------------------------------------------
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "3. TRANSMISSÃO",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = DynoPowerCyan)
            )
            HorizontalDivider(thickness = 0.5.dp, color = DynoDivider)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = gearUsedText,
                onValueChange = { gearUsedText = it },
                label = { Text("Marcha puxada") },
                singleLine = true,
                modifier = Modifier.weight(0.8f).testTag("input_gear_used")
              )
              OutlinedTextField(
                value = gearRatioText,
                onValueChange = { gearRatioText = it },
                label = { Text("Relação marcha") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_gear_ratio")
              )
              OutlinedTextField(
                value = finalDriveText,
                onValueChange = { finalDriveText = it },
                label = { Text("Diferencial") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_final_drive")
              )
            }

            OutlinedTextField(
              value = lossPercentText,
              onValueChange = { lossPercentText = it },
              label = { Text("Perda de Transmissão (%)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("input_loss_percent")
            )
          }
        }

        // -------------------------------------------------------------
        // SEÇÃO 4: PNEUS
        // -------------------------------------------------------------
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "4. PNEUS",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = DynoPowerCyan)
            )
            HorizontalDivider(thickness = 0.5.dp, color = DynoDivider)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedTextField(
                value = tireWidthText,
                onValueChange = { tireWidthText = it },
                label = { Text("Largura (mm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_tire_width")
              )
              OutlinedTextField(
                value = tireAspectText,
                onValueChange = { tireAspectText = it },
                label = { Text("Perfil (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_tire_aspect")
              )
              OutlinedTextField(
                value = rimInchesText,
                onValueChange = { rimInchesText = it },
                label = { Text("Aro (pol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_rim_inches")
              )
            }
          }
        }

        // -------------------------------------------------------------
        // SEÇÃO 5: PARÂMETROS AVANÇADOS
        // -------------------------------------------------------------
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "5. PARÂMETROS AVANÇADOS",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = DynoPowerCyan)
            )
            HorizontalDivider(thickness = 0.5.dp, color = DynoDivider)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = crrText,
                onValueChange = { crrText = it },
                label = { Text("Rolamento (Crr)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_crr")
              )
              OutlinedTextField(
                value = cdText,
                onValueChange = { cdText = it },
                label = { Text("Arrasto (Cd)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_cd")
              )
              OutlinedTextField(
                value = frontalAreaText,
                onValueChange = { frontalAreaText = it },
                label = { Text("Área (m²)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("input_frontal_area")
              )
            }
          }
        }

        // MOTIVO OU JUSTIFICATIVA DA CORREÇÃO
        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it },
          label = { Text("Observação da correção (opcional)") },
          placeholder = { Text("ex: Passageiro de 70 kg não informado no teste.") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_correction_note")
        )

        // -------------------------------------------------------------
        // SEÇÃO 6: COMPARAÇÃO ANTES / DEPOIS (Requisito 6)
        // -------------------------------------------------------------
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = DynoSurfaceContainer),
          border = BorderStroke(1.dp, DynoPowerCyan.copy(alpha = 0.5f)),
          modifier = Modifier.testTag("card_preview_recalculation")
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "6. COMPARAÇÃO ANTES / DEPOIS",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Black,
                  letterSpacing = 0.5.sp,
                  color = DynoPowerCyan
                )
              )
              Text(
                text = "PRÉVIA TEMPO REAL",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = DynoSuccessGreen
                )
              )
            }

            HorizontalDivider(thickness = 0.5.dp, color = DynoDivider)

            if (recalculationResult != null) {
              val newEnginePower = recalculationResult.peakEnginePowerCv
              val newWheelPower = recalculationResult.peakWheelPowerCv
              val newEngineTorque = recalculationResult.peakEngineTorqueKgfm
              val newWheelTorque = recalculationResult.peakWheelTorqueKgfm
              val newRpm = recalculationResult.peakPowerRpm

              val origEnginePower = run.enginePowerCv
              val origWheelPower = run.wheelPowerCv
              val origEngineTorque = run.engineTorqueKgfm
              val origWheelTorque = run.wheelTorqueKgfm
              val origRpm = run.peakPowerRpm

              val pwrDiff = newEnginePower - origEnginePower
              val pwrDiffPct = if (origEnginePower > 0f) (pwrDiff / origEnginePower) * 100f else 0f
              val pwrSign = if (pwrDiff > 0f) "+" else ""

              val wheelPwrDiff = newWheelPower - origWheelPower
              val wheelPwrDiffPct = if (origWheelPower > 0f) (wheelPwrDiff / origWheelPower) * 100f else 0f
              val wheelPwrSign = if (wheelPwrDiff > 0f) "+" else ""

              val trqDiff = newEngineTorque - origEngineTorque
              val trqSign = if (trqDiff > 0f) "+" else ""

              val wheelTrqDiff = newWheelTorque - origWheelTorque
              val wheelTrqSign = if (wheelTrqDiff > 0f) "+" else ""

              val massDiff = parsedConfig.totalVehicleMassKg - run.totalVehicleMassKg
              val massSign = if (massDiff > 0f) "+" else ""

              ComparisonRow(
                label = "Potência Motor",
                original = String.format(Locale.US, "%.1f cv", origEnginePower),
                newVal = String.format(Locale.US, "%.1f cv", newEnginePower),
                diff = "$pwrSign${String.format(Locale.US, "%.1f", pwrDiff)} cv ($pwrSign${String.format(Locale.US, "%.1f", pwrDiffPct)}%)",
                isHighlight = true
              )

              ComparisonRow(
                label = "Potência Rodas",
                original = String.format(Locale.US, "%.1f cv", origWheelPower),
                newVal = String.format(Locale.US, "%.1f cv", newWheelPower),
                diff = "$wheelPwrSign${String.format(Locale.US, "%.1f", wheelPwrDiff)} cv ($wheelPwrSign${String.format(Locale.US, "%.1f", wheelPwrDiffPct)}%)"
              )

              ComparisonRow(
                label = "Torque Motor",
                original = String.format(Locale.US, "%.1f kgfm", origEngineTorque),
                newVal = String.format(Locale.US, "%.1f kgfm", newEngineTorque),
                diff = "$trqSign${String.format(Locale.US, "%.1f", trqDiff)} kgfm"
              )

              ComparisonRow(
                label = "Torque Rodas",
                original = String.format(Locale.US, "%.1f kgfm", origWheelTorque),
                newVal = String.format(Locale.US, "%.1f kgfm", newWheelTorque),
                diff = "$wheelTrqSign${String.format(Locale.US, "%.1f", wheelTrqDiff)} kgfm"
              )

              ComparisonRow(
                label = "RPM de Pico",
                original = "$origRpm rpm",
                newVal = "$newRpm rpm"
              )

              ComparisonRow(
                label = "Massa Total",
                original = "${run.totalVehicleMassKg.toInt()} kg",
                newVal = "${parsedConfig.totalVehicleMassKg.toInt()} kg",
                diff = "$massSign${massDiff.toInt()} kg"
              )
            } else {
              Text(
                text = "Não foi possível recalcular a passagem. Verifique os dados informados.",
                color = DynoErrorRed,
                style = MaterialTheme.typography.bodySmall
              )
            }
          }
        }
      }
    },
    confirmButton = {
      val canSave = validationError == null && recalculationResult != null
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Opção 1: Salvar Correção
        Button(
          onClick = {
            if (canSave && recalculationResult != null) {
              onSaveThisRun(recalculationResult.recalculatedRun, noteText.ifBlank { null })
            }
          },
          enabled = canSave,
          modifier = Modifier.fillMaxWidth().testTag("btn_save_this_run"),
          colors = ButtonDefaults.buttonColors(containerColor = DynoBluePrimary),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("SALVAR CORREÇÃO", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }

        // Opção 2: Salvar como nova versão
        OutlinedButton(
          onClick = {
            if (canSave && recalculationResult != null) {
              onSaveNewVersion(recalculationResult.recalculatedRun, noteText.ifBlank { null })
            }
          },
          enabled = canSave,
          modifier = Modifier.fillMaxWidth().testTag("btn_save_new_version"),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, DynoPowerCyan)
        ) {
          Text("SALVAR COMO NOVA VERSÃO", fontWeight = FontWeight.Bold, color = DynoPowerCyan, letterSpacing = 0.5.sp)
        }
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("btn_cancel_correction")
      ) {
        Text("CANCELAR", fontWeight = FontWeight.Bold)
      }
    }
  )
}

/**
 * Componente de Ação e Feedback do Salvamento do Teste (Requisito 5)
 */
@Composable
private fun SaveTestButtonRow(
  run: RunResult,
  runResultRepository: RunResultRepository,
  modifier: Modifier = Modifier
) {
  val coroutineScope = rememberCoroutineScope()
  var isSaving by remember { mutableStateOf(false) }
  var saveStatus by remember { mutableStateOf<String?>(null) } // "success", "error", null
  var isAlreadySavedInDb by remember(run.id) {
    mutableStateOf(runResultRepository.getResultById(run.id) != null)
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    // Feedback Banner
    if (saveStatus == "success" || isAlreadySavedInDb) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("banner_save_success"),
        shape = RoundedCornerShape(10.dp),
        color = DynoSuccessGreen.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, DynoSuccessGreen.copy(alpha = 0.4f))
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.Check, contentDescription = null, tint = DynoSuccessGreen, modifier = Modifier.size(18.dp))
          Text(
            text = "Teste salvo com sucesso no banco de dados",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = DynoSuccessGreen
          )
        }
      }
    } else if (saveStatus == "error") {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("banner_save_error"),
        shape = RoundedCornerShape(10.dp),
        color = DynoErrorRed.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, DynoErrorRed.copy(alpha = 0.4f))
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Close, contentDescription = null, tint = DynoErrorRed, modifier = Modifier.size(18.dp))
            Text(
              text = "Não foi possível salvar o teste",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              color = DynoErrorRed
            )
          }
          TextButton(
            onClick = {
              coroutineScope.launch {
                isSaving = true
                val ok = runResultRepository.saveResultSuspending(run, "completed")
                isSaving = false
                if (ok) {
                  isAlreadySavedInDb = true
                  saveStatus = "success"
                } else {
                  saveStatus = "error"
                }
              }
            }
          ) {
            Text("Tentar novamente", color = DynoBlueLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      }
    }

    // Botão SALVAR TESTE / TESTE SALVO
    Button(
      onClick = {
        if (!isAlreadySavedInDb && !isSaving) {
          coroutineScope.launch {
            isSaving = true
            val ok = runResultRepository.saveResultSuspending(run, "completed")
            isSaving = false
            if (ok) {
              isAlreadySavedInDb = true
              saveStatus = "success"
            } else {
              saveStatus = "error"
            }
          }
        }
      },
      enabled = !isAlreadySavedInDb && !isSaving,
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("btn_save_test"),
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = if (isAlreadySavedInDb) DynoSuccessGreen else MaterialTheme.colorScheme.primary,
        disabledContainerColor = DynoSuccessGreen.copy(alpha = 0.85f),
        disabledContentColor = Color.White,
        contentColor = Color.White
      )
    ) {
      if (isSaving) {
        Text("SALVANDO NO BANCO...", fontWeight = FontWeight.Bold)
      } else if (isAlreadySavedInDb) {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text("TESTE SALVO", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
      } else {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("SALVAR TESTE", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
      }
    }
  }
}

private data class GraphPoint(
  val x: Float,
  val powerCv: Float,
  val torqueKgfm: Float
)

private fun processSamplesForGraph(samples: List<RunSample>, useRpm: Boolean): List<GraphPoint> {
  val validSamples = samples.filter { sample ->
    sample.isValid &&
    sample.filteredSpeedKmh > 0f &&
    (sample.enginePowerCv > 0f || sample.wheelPowerCv > 0f) &&
    (!useRpm || (sample.engineRpm ?: 0) > 500) &&
    !sample.filteredSpeedKmh.isNaN() &&
    !sample.filteredSpeedKmh.isInfinite()
  }
  if (validSamples.size < 3) return emptyList()

  // 1. Ordenar por RPM ou velocidade
  val sortedSamples = validSamples.sortedBy { sample ->
    if (useRpm) (sample.engineRpm ?: 0).toFloat() else sample.filteredSpeedKmh
  }

  // 2. Agrupar em faixas (bins)
  val minX = sortedSamples.first().let { if (useRpm) (it.engineRpm ?: 0).toFloat() else it.filteredSpeedKmh }
  val maxX = sortedSamples.last().let { if (useRpm) (it.engineRpm ?: 0).toFloat() else it.filteredSpeedKmh }
  if (maxX <= minX) return emptyList()

  val numBins = (sortedSamples.size / 3).coerceIn(12, 30)
  val binWidth = (maxX - minX) / numBins

  val binnedPoints = mutableListOf<GraphPoint>()

  for (i in 0 until numBins) {
    val binStart = minX + i * binWidth
    val binEnd = if (i == numBins - 1) maxX + 0.001f else binStart + binWidth
    val inBin = sortedSamples.filter { sample ->
      val xVal = if (useRpm) (sample.engineRpm ?: 0).toFloat() else sample.filteredSpeedKmh
      xVal >= binStart && xVal < binEnd
    }
    if (inBin.isNotEmpty()) {
      // 3. Usar mediana por faixa para evitar picos isolados
      val medianX = inBin.map { if (useRpm) (it.engineRpm ?: 0).toFloat() else it.filteredSpeedKmh }.sorted()[inBin.size / 2]
      val medianPower = inBin.map { if (it.enginePowerCv > 0f) it.enginePowerCv else it.wheelPowerCv }.sorted()[inBin.size / 2]
      val medianTorque = if (useRpm) {
        inBin.map { if (it.engineTorqueKgfm > 0f) it.engineTorqueKgfm else it.wheelTorqueKgfm }.sorted()[inBin.size / 2]
      } else {
        0f
      }
      binnedPoints.add(GraphPoint(medianX, medianPower, medianTorque))
    }
  }

  if (binnedPoints.size < 2) return binnedPoints

  // 4. Aplicar suavização moderada (filtro ponderado 3 pontos)
  val smoothed = mutableListOf<GraphPoint>()
  for (i in binnedPoints.indices) {
    val prev = binnedPoints[(i - 1).coerceAtLeast(0)]
    val curr = binnedPoints[i]
    val next = binnedPoints[(i + 1).coerceAtMost(binnedPoints.size - 1)]

    val sPower = prev.powerCv * 0.25f + curr.powerCv * 0.50f + next.powerCv * 0.25f
    val sTorque = if (useRpm) (prev.torqueKgfm * 0.25f + curr.torqueKgfm * 0.50f + next.torqueKgfm * 0.25f) else 0f

    smoothed.add(GraphPoint(curr.x, sPower, sTorque))
  }

  return smoothed
}

/**
 * Visualização Completa dos Resultados do Teste de Aceleração (Requisito 8)
 */
@Composable
fun AccelerationRunResultContent(
  run: RunResult,
  formattedDate: String,
  isInvalid: Boolean,
  canCompare: Boolean,
  runResultRepository: RunResultRepository,
  onStartNewTest: () -> Unit,
  onCompareClick: () -> Unit,
  onHistoryClick: () -> Unit
) {
  // 1. HEADER DO MODO E STATUS
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    color = if (isInvalid) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f) else DynoSurfaceContainer,
    border = BorderStroke(
      1.dp,
      if (isInvalid) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else DynoDivider
    )
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Surface(
          shape = CircleShape,
          color = if (isInvalid) MaterialTheme.colorScheme.error else DynoPowerCyan.copy(alpha = 0.2f),
          modifier = Modifier.size(36.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (isInvalid) Icons.Default.Close else Icons.Default.Speed,
              contentDescription = null,
              tint = if (isInvalid) MaterialTheme.colorScheme.onError else DynoPowerCyan,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        Column {
          Text(
            text = "TESTE DE ACELERAÇÃO",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            ),
            color = if (isInvalid) MaterialTheme.colorScheme.onErrorContainer else DynoPowerCyan
          )
          Text(
            text = run.accelRangeLabel.ifEmpty { "0–100 km/h" },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = if (isInvalid) MaterialTheme.colorScheme.onErrorContainer else DynoTextPrimary
          )
        }
      }

      // Quality Badge
      val (qualityColor, qualityBg) = when (run.quality) {
        "BOA" -> Pair(DynoSuccessGreen, DynoSuccessGreen.copy(alpha = 0.2f))
        "REGULAR" -> Pair(DynoWarningYellow, DynoWarningYellow.copy(alpha = 0.2f))
        else -> Pair(DynoErrorRed, DynoErrorRed.copy(alpha = 0.2f))
      }

      Surface(
        shape = RoundedCornerShape(8.dp),
        color = qualityBg,
        border = BorderStroke(1.dp, qualityColor.copy(alpha = 0.5f))
      ) {
        Text(
          text = run.quality,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
          color = qualityColor
        )
      }
    }
  }

  // 2. MOTIVO DA INVALIDAÇÃO (se aplicável)
  if (isInvalid) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(
          imageVector = Icons.Outlined.Info,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(20.dp)
        )
        Column {
          Text(
            text = "MOTIVO DA INVALIDAÇÃO",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.error
          )
          Text(
            text = run.getEffectiveInvalidationReason(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }

  // 3. CARD DE DESTAQUE: TEMPO PRINCIPAL
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = DynoSurfaceContainer),
    border = BorderStroke(1.dp, DynoDivider)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Text(
        text = "TEMPO NA FAIXA ${run.accelRangeLabel.uppercase()}",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.8.sp
        ),
        color = DynoTextSecondary
      )

      Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center
      ) {
        Text(
          text = String.format(Locale.US, "%.2f", run.elapsedSeconds),
          style = MaterialTheme.typography.displayLarge.copy(
            fontWeight = FontWeight.Black,
            fontSize = 54.sp,
            fontFamily = FontFamily.Monospace
          ),
          color = if (isInvalid) DynoTextSecondary else DynoPowerCyan
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "s",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp
          ),
          color = DynoTextSecondary,
          modifier = Modifier.padding(bottom = 8.dp)
        )
      }

      Surface(
        shape = RoundedCornerShape(6.dp),
        color = DynoSurfaceElevated
      ) {
        Text(
          text = "Margem estimada de erro: ±${String.format(Locale.US, "%.2f", run.estimatedMarginSeconds)} s",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
          color = DynoTextSecondary,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
      }
    }
  }

  // 4. PARCIAIS ATINGIDAS (se houver)
  val splits = run.accelerationSplits
  if (splits.isNotEmpty()) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = DynoSurfaceContainer),
      border = BorderStroke(1.dp, DynoDivider)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = "PARCIAIS ATINGIDAS",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          ),
          color = DynoPowerCyan
        )

        splits.forEachIndexed { index, split ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                if (index % 2 == 0) DynoSurfaceElevated.copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(6.dp)
              )
              .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = split.label,
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
              color = DynoTextPrimary
            )
            Text(
              text = String.format(Locale.US, "%.2f s", split.timeSeconds),
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              ),
              color = DynoPowerCyan
            )
          }
        }
      }
    }
  }

  // 5. CARD DE DADOS TÉCNICOS DA MEDIÇÃO
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = DynoSurfaceContainer),
    border = BorderStroke(1.dp, DynoDivider)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = "DADOS DA MEDIÇÃO",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp
        ),
        color = DynoTextSecondary
      )

      DetailRow(
        label = "Veículo utilizado",
        value = run.vehicleName.ifBlank { "Veículo Principal" }
      )
      DetailRow(
        label = "Data e Hora",
        value = formattedDate
      )
      DetailRow(
        label = "Precisão do GPS",
        value = String.format(Locale.US, "±%.1f m", run.gpsAccuracyMeters)
      )
      DetailRow(
        label = "Distância percorrida",
        value = String.format(Locale.US, "%.1f m", run.totalDistanceMeters)
      )
      DetailRow(
        label = "Aceleração máxima",
        value = String.format(Locale.US, "%.2f G", run.peakLongitudinalG)
      )
      DetailRow(
        label = "Trocas de marcha estimadas",
        value = "${run.gearShiftCount}"
      )
      DetailRow(
        label = "Taxa média de amostragem GPS",
        value = String.format(Locale.US, "%.1f Hz", run.averageGpsFrequencyHz)
      )
    }
  }

  // 6. BOTÃO DE SALVAR TESTE
  SaveTestButtonRow(
    run = run,
    runResultRepository = runResultRepository,
    modifier = Modifier.fillMaxWidth()
  )

  // 7. BOTÕES DE NAVEGAÇÃO E AÇÕES
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Button(
      onClick = onStartNewTest,
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
      colors = ButtonDefaults.buttonColors(containerColor = DynoBluePrimary),
      shape = RoundedCornerShape(10.dp)
    ) {
      Icon(Icons.Default.Refresh, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text("NOVO TESTE", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedButton(
        onClick = onCompareClick,
        enabled = canCompare,
        modifier = Modifier.weight(1f).height(44.dp),
        shape = RoundedCornerShape(10.dp)
      ) {
        Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("COMPARAR", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
      }

      OutlinedButton(
        onClick = onHistoryClick,
        modifier = Modifier.weight(1f).height(44.dp),
        shape = RoundedCornerShape(10.dp)
      ) {
        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("HISTÓRICO", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
      }
    }
  }
}

