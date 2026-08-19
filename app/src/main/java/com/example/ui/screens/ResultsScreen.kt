package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
  onStartNewTest: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val runResultRepository = remember { RunResultRepository(context) }
  var results by remember { mutableStateOf(runResultRepository.getResults()) }
  var selectedResultForDetail by remember { mutableStateOf<RunResult?>(null) }
  var showClearAllConfirmDialog by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("results_screen"),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "RESULTADOS",
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
              onClick = { showClearAllConfirmDialog = true },
              modifier = Modifier.testTag("btn_clear_history")
            ) {
              Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "Limpar histórico",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
      if (results.isEmpty()) {
        // EMPTY STATE
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .widthIn(max = 480.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
          Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
          ) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.Assessment,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "Nenhum teste concluído ainda.",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
              ),
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center
            )

            Text(
              text = "Após realizar uma passagem de aceleração de 30 km/h até o corte ou desaceleração, os resultados serão gravados automaticamente aqui.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )
          }

          Button(
            onClick = onStartNewTest,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.testTag("btn_results_new_test")
          ) {
            Icon(
              imageVector = Icons.Default.Speed,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("REALIZAR PASSAGEM")
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Preview Framing Information
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Text(
                text = "Como funciona o registro de passagens",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.primary
              )

              HorizontalDivider(
                thickness = 0.8.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
              )

              Text(
                text = "• Início automático ao ultrapassar 30 km/h no GPS.\n• Coleta da aceleração longitudinal Z e comparação contínua com GPS.\n• Detecção automática de fim por desaceleração (Z < -0.15 m/s² ou queda no GPS).\n• Armazenamento permanente da velocidade máxima e qualidade do teste.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
              )
            }
          }
        }
      } else {
        // RESULTS LIST
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .widthIn(max = 480.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          // Top Summary Banner
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "${results.size} ${if (results.size == 1) "passagem registrada" else "passagens registradas"}",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )

            Button(
              onClick = onStartNewTest,
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
              modifier = Modifier.testTag("btn_results_new_pass")
            ) {
              Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("NOVO TESTE", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
          }

          // List of Result Cards
          results.forEach { run ->
            RunResultCard(
              run = run,
              onClick = { selectedResultForDetail = run }
            )
          }

          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    }
  }

  // DETAILS DIALOG
  if (selectedResultForDetail != null) {
    val run = selectedResultForDetail!!
    val dateFormat = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm:ss", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(run.timestamp))
    val finishReasonEnum = FinishReason.fromCode(run.finishReason)

    AlertDialog(
      onDismissRequest = { selectedResultForDetail = null },
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Speed,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
          Text(
            text = "Detalhes da Passagem",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
          )
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = formattedDate,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          if (run.vehicleName.isNotEmpty()) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(Icons.Outlined.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Text(run.vehicleName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
              }
            }
          }

          HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

          DetailRow("Velocidade inicial GPS", String.format(Locale.US, "%.1f km/h", run.runStartGpsSpeedKmh))
          DetailRow("Velocidade máxima GPS", String.format(Locale.US, "%.1f km/h", run.maximumGpsSpeedKmh))
          DetailRow("Velocidade máxima calculada", String.format(Locale.US, "%.1f km/h", run.maximumCalculatedSpeedKmh))
          DetailRow("Velocidade final GPS", String.format(Locale.US, "%.1f km/h", run.finalGpsSpeedKmh))
          DetailRow("Velocidade final calculada", String.format(Locale.US, "%.1f km/h", run.finalCalculatedSpeedKmh))
          DetailRow("Diferença média", String.format(Locale.US, "±%.1f km/h", run.averageSpeedDifferenceKmh))
          DetailRow("Maior diferença", String.format(Locale.US, "%.1f km/h", run.maximumSpeedDifferenceKmh))
          DetailRow("Duração do teste", String.format(Locale.US, "%.2f s", run.elapsedSeconds))
          DetailRow("Qualidade", run.quality)
          DetailRow("Motivo do término", finishReasonEnum.displayName)
          DetailRow("Precisão GPS", String.format(Locale.US, "%.1f m", run.gpsAccuracyMeters))

          val rejectionPercent = if (run.totalSamples > 0) (run.rejectedSamples * 100) / run.totalSamples else 0
          DetailRow("Amostras", "${run.totalSamples} (${run.rejectedSamples} rejeitadas / $rejectionPercent%)")
        }
      },
      confirmButton = {
        Button(
          onClick = { selectedResultForDetail = null },
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("FECHAR")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            runResultRepository.deleteResult(run.id)
            results = runResultRepository.getResults()
            selectedResultForDetail = null
          },
          colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("EXCLUIR")
        }
      }
    )
  }

  // CLEAR ALL CONFIRMATION DIALOG
  if (showClearAllConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showClearAllConfirmDialog = false },
      title = { Text("Limpar Histórico") },
      text = { Text("Deseja realmente apagar todos os registros de passagens? Esta ação não pode ser desfeita.") },
      confirmButton = {
        Button(
          onClick = {
            runResultRepository.clearAllResults()
            results = emptyList()
            showClearAllConfirmDialog = false
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

@Composable
private fun RunResultCard(
  run: RunResult,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
  val formattedDate = dateFormat.format(Date(run.timestamp))
  val finishReasonEnum = FinishReason.fromCode(run.finishReason)

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("run_result_card_${run.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Header: Date & Quality Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.CalendarToday,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
          )
          Text(
            text = formattedDate,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        // Quality Chip
        Surface(
          shape = CircleShape,
          color = when (run.quality) {
            "BOA" -> MaterialTheme.colorScheme.primaryContainer
            "REGULAR" -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.errorContainer
          }
        ) {
          Text(
            text = run.quality,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = when (run.quality) {
              "BOA" -> MaterialTheme.colorScheme.onPrimaryContainer
              "REGULAR" -> MaterialTheme.colorScheme.onTertiaryContainer
              else -> MaterialTheme.colorScheme.onErrorContainer
            }
          )
        }
      }

      HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

      // Speed and Duration Metrics
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text(
            text = "Máx GPS",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = String.format(Locale.US, "%.1f km/h", run.maximumGpsSpeedKmh),
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.primary
          )
        }

        Column {
          Text(
            text = "Máx Calc",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = String.format(Locale.US, "%.1f km/h", run.maximumCalculatedSpeedKmh),
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Column {
          Text(
            text = "Duração",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = String.format(Locale.US, "%.2f s", run.elapsedSeconds),
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      // Footer: Finish reason and vehicle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = finishReasonEnum.displayName,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
          text = "Toque para detalhes",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
          color = MaterialTheme.colorScheme.primary
        )
      }
    }
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
