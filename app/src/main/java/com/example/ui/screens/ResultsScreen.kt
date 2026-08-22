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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
  onStartNewTest: () -> Unit,
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

            // 4. RESUMO DAS VELOCIDADES
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Velocidade Máxima GPS
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
                    text = "MÁXIMA GPS",
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

              // Velocidade Máxima Calculada
              Card(
                modifier = Modifier
                  .weight(1f)
                  .testTag("card_speed_max_calc"),
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
                    text = "MÁX. CALCULADA",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 10.sp
                    ),
                    color = Color(0xFF38BDF8)
                  )
                  Text(
                    text = String.format(Locale.US, "%.1f km/h", run.maximumCalculatedSpeedKmh),
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

            // 5. QUALIDADE E DIFERENÇAS (Diferença no pico, Média e Máxima)
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
                    text = "QUALIDADE E DIFERENÇAS",
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
                  label = "Diferença no pico",
                  value = String.format(Locale.US, "%.1f km/h", run.peakSpeedDifferenceKmh)
                )
                DetailRow(
                  label = "Diferença média sincronizada",
                  value = String.format(Locale.US, "±%.1f km/h", run.averageSpeedDifferenceKmh)
                )
                DetailRow(
                  label = "Maior diferença sincronizada",
                  value = String.format(Locale.US, "%.1f km/h", run.maximumSpeedDifferenceKmh)
                )

                Text(
                  text = "Diferenças sincronizadas calculadas estritamente durante a aceleração plena antes da confirmação de desaceleração.",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                  ),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
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
              onClick = onStartNewTest,
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

            // 2. RESUMO DAS VELOCIDADES
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
                  Text("MÁXIMA GPS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = MaterialTheme.colorScheme.primary)
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
                  Text("MÁX. CALCULADA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = Color(0xFF38BDF8))
                  Text(String.format(Locale.US, "%.1f km/h", run.maximumCalculatedSpeedKmh), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace))
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

            // 3. CARTÃO COMPACTO: "Passagem salva. Cálculo de potência ainda não disponível."
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
                    text = "Passagem salva. Cálculo de potência ainda não disponível.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Curva disponível após implementar o cálculo de potência e torque.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }

            // 4. QUALIDADE E DIFERENÇAS
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
                  Text("QUALIDADE E DIFERENÇAS", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp))
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
                DetailRow("Diferença no pico", String.format(Locale.US, "%.1f km/h", run.peakSpeedDifferenceKmh))
                DetailRow("Diferença média sincronizada", String.format(Locale.US, "±%.1f km/h", run.averageSpeedDifferenceKmh))
                DetailRow("Maior diferença sincronizada", String.format(Locale.US, "%.1f km/h", run.maximumSpeedDifferenceKmh))
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
              onClick = onStartNewTest,
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

/**
 * 4. Card Grande do Gráfico de Potência e Torque
 * - Altura entre 260 e 320 dp (280 dp)
 * - Cantos arredondados
 * - Tema escuro
 * - Eixo horizontal: RPM
 * - Eixo vertical esquerdo: cv
 * - Eixo vertical direito: kgfm
 * - Exibe moldura com eixos e a mensagem quando ainda não configurado, sem inventar dados falsos.
 */
@Composable
private fun DynoPowerTorqueGraphCard(
  hasVehicleConfig: Boolean,
  modifier: Modifier = Modifier
) {
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

        val paddingLeft = 32.dp.toPx()
        val paddingRight = 32.dp.toPx()
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

      // Bottom Axis Label: RPM
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

      // Center Notification Overlay (When power and torque calculation is pending configuration)
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
          text = "Curva disponível após implementar o cálculo de potência e torque.",
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
          DetailRow("Velocidade inicial", String.format(Locale.US, "%.1f km/h", run.runStartCalculatedSpeedKmh))
          DetailRow("Máxima GPS", String.format(Locale.US, "%.1f km/h", run.maximumGpsSpeedKmh))
          DetailRow("Máxima calculada", String.format(Locale.US, "%.1f km/h", run.maximumCalculatedSpeedKmh))
          DetailRow("Diferença média", String.format(Locale.US, "±%.1f km/h", run.averageSpeedDifferenceKmh))
          DetailRow("Maior diferença", String.format(Locale.US, "%.1f km/h", run.maximumSpeedDifferenceKmh))
          DetailRow("Duração", String.format(Locale.US, "%.2f s", run.elapsedSeconds))

          val totalCount = if (run.totalSamples > 0) run.totalSamples else orderedSamples.size
          DetailRow("Quantidade de amostras", "$totalCount amostras")
          DetailRow("Qualidade", run.quality)
          DetailRow("Motivo da finalização", finishReasonEnum.displayName)

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
                      Text("Acel. Z", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                      Text("GPS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                      Text("Calc", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
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
      text = String.format(Locale.US, "%+.2f", sample.filteredAccelerationZ),
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f),
      textAlign = TextAlign.End
    )
    Text(
      text = String.format(Locale.US, "%.1f", sample.gpsSpeedKmh),
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.weight(1f),
      textAlign = TextAlign.End
    )
    Text(
      text = String.format(Locale.US, "%.1f", sample.calculatedSpeedKmh),
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f),
      textAlign = TextAlign.End
    )
  }
}
