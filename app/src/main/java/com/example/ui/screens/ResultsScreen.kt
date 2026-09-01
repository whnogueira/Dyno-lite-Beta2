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
import com.example.model.FinishReason
import com.example.model.RunResult
import com.example.model.RunSample
import com.example.model.VehicleCalculations
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
  var selfTestResultMessage by remember { mutableStateOf<String?>(null) }
  var isRunningSelfTest by remember { mutableStateOf(false) }

  // Check valid tests for the current vehicle to evaluate comparison eligibility (BOA/REGULAR with gain >= 15 km/h)
  val currentVehicleName = currentDisplayedResult?.vehicleName ?: ""
  val validRunsForVehicle = remember(results, currentVehicleName) {
    if (currentVehicleName.isNotEmpty()) {
      results.filter {
        it.vehicleName == currentVehicleName &&
        (it.quality == "BOA" || it.quality == "REGULAR") &&
        it.speedGainKmh >= 15.0f
      }
    } else {
      results.filter {
        (it.quality == "BOA" || it.quality == "REGULAR") &&
        it.speedGainKmh >= 15.0f
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

  // DIÁLOGO DE CORREÇÃO DE DADOS DA PASSAGEM (Seção 37)
  if (showCorrectRunDialog && currentDisplayedResult != null) {
    CorrectRunDataDialog(
      run = currentDisplayedResult!!,
      onDismiss = { showCorrectRunDialog = false },
      onSaveCorrected = { correctedRun ->
        runResultRepository.saveResult(correctedRun)
        results = runResultRepository.getResults()
        currentDisplayedResult = correctedRun
        showCorrectRunDialog = false
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
        if (results.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Nenhum teste gravado no histórico.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          Text(
            text = "Testes concluídos (${results.size}):",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = DynoBlueLight)
          )
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

/**
 * Diálogo de Correção de Dados da Passagem (Seção 37)
 * Permite ao usuário editar parâmetros de veículo/teste e recalcular potência/torque reais
 * utilizando os dados de velocidade e tempo brutos gravados na passagem original.
 */
@Composable
fun CorrectRunDataDialog(
  run: RunResult,
  onDismiss: () -> Unit,
  onSaveCorrected: (RunResult) -> Unit
) {
  var weightText by remember { mutableStateOf(if (run.totalVehicleMassKg > 0f) String.format(Locale.US, "%.0f", run.totalVehicleMassKg) else "1250") }
  var gearRatioText by remember { mutableStateOf(String.format(Locale.US, "%.2f", run.gearRatioUsed)) }
  var finalDriveText by remember { mutableStateOf(String.format(Locale.US, "%.2f", run.finalDriveUsed)) }
  var tireWidthText by remember { mutableStateOf("195") }
  var tireAspectText by remember { mutableStateOf("55") }
  var rimInchesText by remember { mutableStateOf("15") }
  var lossPercentText by remember { mutableStateOf(String.format(Locale.US, "%.1f", run.drivetrainLossPercent)) }
  var cdText by remember { mutableStateOf(String.format(Locale.US, "%.2f", run.cdUsed)) }
  var frontalAreaText by remember { mutableStateOf(String.format(Locale.US, "%.2f", run.frontalAreaUsed)) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Default.Edit, contentDescription = null, tint = DynoBluePrimary)
        Text(
          text = "Corrigir dados da passagem",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
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
          text = "Ajuste os parâmetros físicos. A potência e torque reais serão recalculados preservando as leituras de velocidade GPS da passagem original.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (errorMessage != null) {
          Surface(
            color = DynoErrorRed.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = errorMessage!!,
              color = DynoErrorRed,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(8.dp)
            )
          }
        }

        // 1. Peso Total
        OutlinedTextField(
          value = weightText,
          onValueChange = { weightText = it },
          label = { Text("Massa Total (Carro + Motorista) [kg]") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        // 2. Relações de Câmbio
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = gearRatioText,
            onValueChange = { gearRatioText = it },
            label = { Text("Relação da Marcha") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = finalDriveText,
            onValueChange = { finalDriveText = it },
            label = { Text("Diferencial") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        // 3. Dimensões do Pneu
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          OutlinedTextField(
            value = tireWidthText,
            onValueChange = { tireWidthText = it },
            label = { Text("Largura (mm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = tireAspectText,
            onValueChange = { tireAspectText = it },
            label = { Text("Perfil (%)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = rimInchesText,
            onValueChange = { rimInchesText = it },
            label = { Text("Aro (pol)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        // 4. Perda e Aerodinâmica
        OutlinedTextField(
          value = lossPercentText,
          onValueChange = { lossPercentText = it },
          label = { Text("Perda de Transmissão (%)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = cdText,
            onValueChange = { cdText = it },
            label = { Text("Arrasto (Cd)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = frontalAreaText,
            onValueChange = { frontalAreaText = it },
            label = { Text("Área Frontal (m²)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val weight = weightText.toFloatOrNull()
          val gear = gearRatioText.toFloatOrNull()
          val fd = finalDriveText.toFloatOrNull()
          val tWidth = tireWidthText.toIntOrNull()
          val tAspect = tireAspectText.toIntOrNull()
          val rim = rimInchesText.toIntOrNull()
          val loss = lossPercentText.toFloatOrNull()
          val cd = cdText.toFloatOrNull()
          val area = frontalAreaText.toFloatOrNull()

          if (weight == null || weight < 300f || weight > 5000f) {
            errorMessage = "Informe um peso válido entre 300 e 5000 kg"
            return@Button
          }
          if (gear == null || gear <= 0.2f || gear > 8f) {
            errorMessage = "Relação de marcha inválida"
            return@Button
          }
          if (fd == null || fd <= 0.5f || fd > 10f) {
            errorMessage = "Relação de diferencial inválida"
            return@Button
          }
          if (tWidth == null || tWidth < 125 || tWidth > 385) {
            errorMessage = "Largura de pneu inválida"
            return@Button
          }
          if (tAspect == null || tAspect < 20 || tAspect > 90) {
            errorMessage = "Perfil de pneu inválido"
            return@Button
          }
          if (rim == null || rim < 10 || rim > 26) {
            errorMessage = "Aro inválido"
            return@Button
          }
          if (loss == null || loss < 0f || loss > 40f) {
            errorMessage = "Perda de transmissão deve ser entre 0% e 40%"
            return@Button
          }
          if (cd == null || cd < 0.15f || cd > 1.0f) {
            errorMessage = "Coeficiente Cd inválido"
            return@Button
          }
          if (area == null || area < 1.0f || area > 5.0f) {
            errorMessage = "Área frontal inválida"
            return@Button
          }

          val recalculated = VehicleCalculations.recalculateRunResult(
            run = run,
            correctedTotalMassKg = weight,
            correctedGearRatio = gear,
            correctedFinalDrive = fd,
            correctedTireWidthMm = tWidth,
            correctedTireAspectRatio = tAspect,
            correctedRimInches = rim,
            correctedLossPercent = loss,
            correctedCd = cd,
            correctedFrontalAreaM2 = area,
            correctedCrr = run.crrUsed
          )
          onSaveCorrected(recalculated)
        },
        colors = ButtonDefaults.buttonColors(containerColor = DynoBluePrimary),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("RECALCULAR E SALVAR")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
        Text("CANCELAR")
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

