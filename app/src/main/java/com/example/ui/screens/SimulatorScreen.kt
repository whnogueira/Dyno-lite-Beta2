package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RunResultRepository
import com.example.data.SimulationRepository
import com.example.data.VehicleRepository
import com.example.model.DrivetrainType
import com.example.model.GearShiftPoint
import com.example.model.GearSpeedEntry
import com.example.model.RunResult
import com.example.model.SavedSimulationProject
import com.example.model.ShiftSpeedType
import com.example.model.SimulationConfidence
import com.example.model.SimulationConfig
import com.example.model.SimulationEngine
import com.example.model.SimulationResult
import com.example.model.TireGripType
import com.example.model.VehicleCalculations
import com.example.model.VehicleProfile
import com.example.ui.components.DynoPrimaryButton
import com.example.ui.components.DynoSecondaryButton
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// Cor de destaque oficial do simulador (Roxo / Índigo vibrante)
val SimPurplePrimary = Color(0xFF8B5CF6)
val SimPurpleLight = Color(0xFFA78BFA)
val SimPurpleContainer = Color(0xFF4C1D95).copy(alpha = 0.35f)
val SimAmberHighlight = Color(0xFFF59E0B)

enum class SimulatorGraphType(val title: String, val unitY: String) {
  POWER_TORQUE("Potência & Torque", "cv / kgfm"),
  SPEED_TIME("Velocidade x Tempo", "km/h"),
  DISTANCE_TIME("Distância x Tempo", "m"),
  TRACTIVE_FORCE("Força Trativa por Marcha", "N"),
  RPM_SPEED("RPM x Velocidade", "RPM")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(
  initialRunId: String? = null,
  onNavigateToRunDetails: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val vehicleRepo = remember { VehicleRepository(context) }
  val runRepo = remember { RunResultRepository(context) }
  val simRepo = remember { SimulationRepository(context) }

  val allVehicles = remember { vehicleRepo.getVehicles() }
  val allRuns = remember { runRepo.getResults() }
  var savedProjects by remember { mutableStateOf(simRepo.getSavedProjects()) }

  // Diálogos
  var showSelectBaseDialog by remember { mutableStateOf(false) }
  var showSaveProjectDialog by remember { mutableStateOf(false) }
  var showProjectsListDialog by remember { mutableStateOf(false) }
  var projectNameInput by remember { mutableStateOf("") }
  var projectNotesInput by remember { mutableStateOf("") }
  var feedbackToast by remember { mutableStateOf<String?>(null) }

  // Configuração Base (A) e Simulada (B)
  var configA by remember {
    val initialRun = initialRunId?.let { runRepo.getResultById(it) } ?: allRuns.firstOrNull()
    val initialVeh = vehicleRepo.getPrimaryVehicle() ?: allVehicles.firstOrNull()
    mutableStateOf(createConfigFromRunOrVehicle(initialRun, initialVeh))
  }

  var configB by remember {
    mutableStateOf(configA.copy(label = "Simulada (B)"))
  }

  // Execução de Simulação
  val resultA = remember(configA) { SimulationEngine.runSimulation(configA) }
  val resultB = remember(configB) { SimulationEngine.runSimulation(configB) }

  var activeTab by remember { mutableIntStateOf(0) } // 0 = Comparação & Resultados, 1 = Configuração B (Modificada), 2 = Configuração A (Atual)
  var selectedGraph by remember { mutableStateOf(SimulatorGraphType.POWER_TORQUE) }

  LaunchedEffect(feedbackToast) {
    if (feedbackToast != null) {
      kotlinx.coroutines.delay(3500)
      feedbackToast = null
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp)
        .widthIn(max = 680.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {

      // =========================================================================
      // 1. BANNER DE IDENTIFICAÇÃO VISUAL DO SIMULADOR (RESULTADO SIMULADO)
      // =========================================================================
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("banner_simulated_result"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SimPurpleContainer),
        border = BorderStroke(1.2.dp, SimPurplePrimary.copy(alpha = 0.6f))
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
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
                shape = CircleShape,
                color = SimPurplePrimary,
                modifier = Modifier.size(28.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Outlined.Science,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                  )
                }
              }
              Column {
                Text(
                  text = "RESULTADO SIMULADO",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    fontSize = 15.sp
                  ),
                  color = SimPurpleLight
                )
                Text(
                  text = "Modelagem física e matemática de desempenho",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                  color = DynoTextSecondary
                )
              }
            }

            // Grau de Confiança
            ConfidenceBadge(confidence = resultB.confidence)
          }

          HorizontalDivider(thickness = 0.6.dp, color = SimPurplePrimary.copy(alpha = 0.3f))

          // Barra de Ações Rápidas do Topo (Mudar Base, Salvar Projeto, Meus Projetos)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            DynoSecondaryButton(
              text = "MUDAR BASE",
              onClick = { showSelectBaseDialog = true },
              icon = Icons.Default.Tune,
              modifier = Modifier.weight(1f),
              testTag = "btn_sim_change_base"
            )

            DynoSecondaryButton(
              text = "PROJETOS (${savedProjects.size})",
              onClick = { showProjectsListDialog = true },
              icon = Icons.Default.BookmarkBorder,
              modifier = Modifier.weight(1f),
              testTag = "btn_sim_open_projects"
            )

            DynoPrimaryButton(
              text = "SALVAR",
              onClick = {
                projectNameInput = "${configB.vehicleName} - ${if (configB.isTurboSimulated) "Turbo ${configB.turboBoostBar}bar" else "Modificada"}"
                showSaveProjectDialog = true
              },
              icon = Icons.Default.Save,
              modifier = Modifier.weight(1f),
              testTag = "btn_sim_save_project"
            )
          }
        }
      }

      // Feedback Toast
      if (feedbackToast != null) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = DynoSuccessGreen.copy(alpha = 0.2f),
          border = BorderStroke(1.dp, DynoSuccessGreen),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = DynoSuccessGreen, modifier = Modifier.size(18.dp))
            Text(feedbackToast ?: "", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = DynoTextPrimary)
          }
        }
      }

      // =========================================================================
      // 2. SEÇÃO DE MODIFICAÇÕES RÁPIDAS (1 TOQUE)
      // =========================================================================
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DynoSurfaceElevated),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = SimAmberHighlight, modifier = Modifier.size(18.dp))
              Text(
                text = "MODIFICAÇÕES RÁPIDAS (APLICAR NA CONFIGURAÇÃO B)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                color = DynoTextPrimary
              )
            }

            TextButton(
              onClick = {
                configB = configA.copy(label = "Simulada (B)")
                feedbackToast = "Configuração B resetada para os valores de A."
              },
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = SimPurpleLight)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Copiar A para B", style = MaterialTheme.typography.labelSmall, color = SimPurpleLight)
            }
          }

          LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            item {
              QuickModChip(label = "-50 kg", onClick = {
                configB = configB.copy(vehicleCurbWeightKg = (configB.vehicleCurbWeightKg - 50f).coerceAtLeast(300f))
                feedbackToast = "Peso reduzido em 50 kg."
              })
            }
            item {
              QuickModChip(label = "-100 kg", onClick = {
                configB = configB.copy(vehicleCurbWeightKg = (configB.vehicleCurbWeightKg - 100f).coerceAtLeast(300f))
                feedbackToast = "Peso reduzido em 100 kg."
              })
            }
            item {
              QuickModChip(label = "+10% Potência", onClick = {
                val newP = configB.enginePowerCv * 1.10f
                val newT = configB.engineTorqueKgfm * 1.10f
                configB = configB.copy(enginePowerCv = newP, engineTorqueKgfm = newT)
                feedbackToast = "Potência e torque aumentados em +10%."
              })
            }
            item {
              QuickModChip(label = "+20% Potência", onClick = {
                val newP = configB.enginePowerCv * 1.20f
                val newT = configB.engineTorqueKgfm * 1.20f
                configB = configB.copy(enginePowerCv = newP, engineTorqueKgfm = newT)
                feedbackToast = "Potência e torque aumentados em +20%."
              })
            }
            item {
              QuickModChip(label = "Turbo +0.8 bar", onClick = {
                configB = configB.copy(isTurboSimulated = true, turboBoostBar = 0.8f)
                feedbackToast = "Turbo simulado de 0.8 bar ativado."
              })
            }
            item {
              QuickModChip(label = "Etanol (+7% cv)", onClick = {
                configB = configB.copy(
                  enginePowerCv = configB.enginePowerCv * 1.07f,
                  engineTorqueKgfm = configB.engineTorqueKgfm * 1.07f
                )
                feedbackToast = "Ganho de combustível Etanol (+7%) aplicado."
              })
            }
            item {
              QuickModChip(label = "Dif. Curto (+10%)", onClick = {
                configB = configB.copy(finalDriveRatio = configB.finalDriveRatio * 1.10f)
                feedbackToast = "Diferencial encurtado em +10%."
              })
            }
            item {
              QuickModChip(label = "Dif. Longo (-10%)", onClick = {
                configB = configB.copy(finalDriveRatio = configB.finalDriveRatio * 0.90f)
                feedbackToast = "Diferencial alongado em -10%."
              })
            }
            item {
              QuickModChip(label = "Pneu 195/50 R15", onClick = {
                configB = configB.copy(tireWidthMm = 195, tireAspectRatio = 50, rimDiameterInches = 15)
                feedbackToast = "Dimensão de pneus ajustada para 195/50 R15."
              })
            }
            item {
              QuickModChip(label = "Cd -10% (Aero)", onClick = {
                configB = configB.copy(cd = (configB.cd * 0.90f).coerceAtLeast(0.20f))
                feedbackToast = "Coeficiente aerodinâmico (Cd) melhorado em 10%."
              })
            }
            item {
              QuickModChip(label = "+500 RPM Corte", onClick = {
                configB = configB.copy(maxRpm = configB.maxRpm + 500)
                feedbackToast = "Limite de giro aumentado para ${configB.maxRpm} RPM."
              })
            }
          }
        }
      }

      // =========================================================================
      // 3. ABAS: [COMPARAÇÃO & RESULTADOS] | [CONFIG B (MODIFICADA)] | [CONFIG A (ATUAL)]
      // =========================================================================
      TabRow(
        selectedTabIndex = activeTab,
        containerColor = DynoSurface,
        contentColor = SimPurpleLight,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            Modifier.tabIndicatorOffset(tabPositions[activeTab]),
            color = SimPurplePrimary,
            height = 3.dp
          )
        },
        divider = { HorizontalDivider(thickness = 0.8.dp, color = DynoDivider) }
      ) {
        Tab(
          selected = activeTab == 0,
          onClick = { activeTab = 0 },
          text = {
            Text(
              "COMPARAÇÃO & GRÁFICOS",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
              color = if (activeTab == 0) SimPurpleLight else DynoTextSecondary
            )
          }
        )
        Tab(
          selected = activeTab == 1,
          onClick = { activeTab = 1 },
          text = {
            Text(
              "CONFIG B (SIMULADA)",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
              color = if (activeTab == 1) SimPurpleLight else DynoTextSecondary
            )
          }
        )
        Tab(
          selected = activeTab == 2,
          onClick = { activeTab = 2 },
          text = {
            Text(
              "CONFIG A (ATUAL)",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
              color = if (activeTab == 2) SimPurpleLight else DynoTextSecondary
            )
          }
        )
      }

      // =========================================================================
      // CONTEÚDO DA ABA SELECIONADA
      // =========================================================================
      when (activeTab) {
        0 -> {
          // -------------------------------------------------------------
          // ABA 0: RESULTADOS COMPARATIVOS, TABELA A vs B E GRÁFICOS
          // -------------------------------------------------------------

          // Tabela Comparativa de Resultados (Seção 34)
          ComparisonSummaryCard(resultA = resultA, resultB = resultB)

          // Alerta se houver perda de aderência estimada (Seção 30)
          if (resultB.hasTractionLossWarning) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = DynoWarningYellow.copy(alpha = 0.15f),
              border = BorderStroke(1.dp, DynoWarningYellow.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = DynoWarningYellow, modifier = Modifier.size(22.dp))
                Column {
                  Text(
                    text = "Possível perda de tração detectada",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = DynoWarningYellow
                  )
                  Text(
                    text = "Em marchas baixas, a força gerada pelo motor supera a aderência estimada dos pneus. O resultado na pista dependerá da modulação do acelerador e temperatura do asfalto.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                    color = DynoTextPrimary
                  )
                }
              }
            }
          }

          // Gráficos do Simulador (Seção 35)
          SimulatorGraphsCard(
            resultA = resultA,
            resultB = resultB,
            selectedGraph = selectedGraph,
            onSelectGraph = { selectedGraph = it }
          )

          // Tabela de Velocidade por Marcha (Seção 28)
          GearSpeedTableCard(config = configB, gearSpeeds = resultB.gearSpeeds)

          // Ponto Ideal de Troca de Marcha (Seção 33)
          OptimalShiftPointsCard(shiftPoints = resultB.optimalShiftPoints)

          // Aviso de Isenção Técnico (Seção 39 & 40)
          DisclaimerTechnicalCard()
        }

        1 -> {
          // -------------------------------------------------------------
          // ABA 1: EDIÇÃO DA CONFIGURAÇÃO B (SIMULADA)
          // -------------------------------------------------------------
          ConfigEditorForm(
            config = configB,
            referenceConfig = configA,
            title = "CONFIGURAÇÃO B — SIMULADA",
            onConfigChange = { configB = it },
            onCopyToOther = { configA = configB.copy(label = "Atual (A)") }
          )
        }

        2 -> {
          // -------------------------------------------------------------
          // ABA 2: EDIÇÃO DA CONFIGURAÇÃO A (ATUAL / BASE)
          // -------------------------------------------------------------
          ConfigEditorForm(
            config = configA,
            referenceConfig = configA,
            title = "CONFIGURAÇÃO A — ATUAL",
            onConfigChange = { configA = it },
            onCopyToOther = { configB = configA.copy(label = "Simulada (B)") }
          )
        }
      }

      Spacer(modifier = Modifier.height(30.dp))
    }
  }

  // =========================================================================
  // DIÁLOGO: SELEÇÃO DA BASE DA SIMULAÇÃO (PASSAGEM REAL OU VEÍCULO)
  // =========================================================================
  if (showSelectBaseDialog) {
    AlertDialog(
      onDismissRequest = { showSelectBaseDialog = false },
      containerColor = DynoSurfaceElevated,
      title = {
        Text("Escolher Base da Simulação", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = DynoTextPrimary)
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "Selecione uma medição real do histórico para obter a curva de potência exata ou escolha um veículo da garagem:",
            style = MaterialTheme.typography.bodySmall,
            color = DynoTextSecondary
          )

          Text(
            text = "PASSAGENS REAIS SALVAS (${allRuns.size})",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = SimPurpleLight
          )

          if (allRuns.isEmpty()) {
            Text("Nenhuma passagem real registrada no histórico.", style = MaterialTheme.typography.bodySmall, color = DynoTextMuted)
          } else {
            allRuns.take(8).forEach { run ->
              val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
              val dateStr = df.format(Date(run.timestamp))
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = DynoSurface,
                border = BorderStroke(1.dp, if (configA.baseRunId == run.id) SimPurplePrimary else DynoDivider),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    configA = createConfigFromRun(run)
                    configB = configA.copy(label = "Simulada (B)")
                    showSelectBaseDialog = false
                    feedbackToast = "Simulação atualizada com base na passagem de $dateStr."
                  }
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text(run.vehicleName.ifBlank { "Veículo" }, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = DynoTextPrimary)
                    Text("$dateStr • ${String.format(Locale.US, "%.0f cv", run.estimatedPowerCv)} • ${String.format(Locale.US, "%.1f kgfm", run.estimatedTorqueKgfm)}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp), color = DynoPowerCyan)
                  }
                  Icon(Icons.Default.Check, contentDescription = null, tint = SimPurplePrimary)
                }
              }
            }
          }

          HorizontalDivider(thickness = 0.8.dp, color = DynoDivider)

          Text(
            text = "VEÍCULOS DA GARAGEM (${allVehicles.size})",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = DynoBlueLight
          )

          allVehicles.forEach { veh ->
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = DynoSurface,
              border = BorderStroke(1.dp, DynoDivider),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  configA = createConfigFromVehicle(veh)
                  configB = configA.copy(label = "Simulada (B)")
                  showSelectBaseDialog = false
                  feedbackToast = "Simulação atualizada com dados do veículo ${veh.model}."
                }
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column {
                  Text("${veh.manufacturer} ${veh.model}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = DynoTextPrimary)
                  Text("${veh.year} • ${veh.curbWeightKg.toInt()} kg • ${veh.factoryPowerCv?.toInt() ?: 120} cv", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp), color = DynoTextSecondary)
                }
                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = DynoBlueLight)
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showSelectBaseDialog = false }) {
          Text("FECHAR", color = DynoTextSecondary)
        }
      }
    )
  }

  // =========================================================================
  // DIÁLOGO: SALVAR PROJETO
  // =========================================================================
  if (showSaveProjectDialog) {
    AlertDialog(
      onDismissRequest = { showSaveProjectDialog = false },
      containerColor = DynoSurfaceElevated,
      title = { Text("Salvar Projeto de Simulação", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = DynoTextPrimary) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = projectNameInput,
            onValueChange = { projectNameInput = it },
            label = { Text("Nome do Projeto") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = projectNotesInput,
            onValueChange = { projectNotesInput = it },
            label = { Text("Observações (opcional)") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (projectNameInput.isNotBlank()) {
              val newProj = SavedSimulationProject(
                name = projectNameInput,
                vehicleName = configB.vehicleName,
                baseRunId = configA.baseRunId,
                notes = projectNotesInput,
                configA = configA,
                configB = configB,
                confidence = resultB.confidence
              )
              simRepo.saveProject(newProj)
              savedProjects = simRepo.getSavedProjects()
              showSaveProjectDialog = false
              feedbackToast = "Projeto \"$projectNameInput\" salvo com sucesso!"
            }
          }
        ) {
          Text("SALVAR", color = SimPurpleLight, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showSaveProjectDialog = false }) {
          Text("CANCELAR", color = DynoTextSecondary)
        }
      }
    )
  }

  // =========================================================================
  // DIÁLOGO: LISTA DE PROJETOS SALVOS
  // =========================================================================
  if (showProjectsListDialog) {
    AlertDialog(
      onDismissRequest = { showProjectsListDialog = false },
      containerColor = DynoSurfaceElevated,
      title = { Text("Projetos Salvos (${savedProjects.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = DynoTextPrimary) },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          if (savedProjects.isEmpty()) {
            Text("Nenhum projeto de simulação salvo ainda.", style = MaterialTheme.typography.bodySmall, color = DynoTextMuted)
          } else {
            savedProjects.forEach { proj ->
              val df = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
              val dStr = df.format(Date(proj.createdAt))
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = DynoSurface,
                border = BorderStroke(1.dp, DynoDivider),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(proj.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = DynoTextPrimary)
                    ConfidenceBadge(confidence = proj.confidence)
                  }
                  Text("${proj.vehicleName} • $dStr", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = DynoTextSecondary)
                  if (proj.notes.isNotBlank()) {
                    Text(proj.notes, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp), color = DynoTextMuted)
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    TextButton(
                      onClick = {
                        val dup = simRepo.duplicateProject(proj.id)
                        savedProjects = simRepo.getSavedProjects()
                        feedbackToast = "Projeto duplicado."
                      }
                    ) {
                      Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = DynoTextSecondary)
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Duplicar", style = MaterialTheme.typography.labelSmall, color = DynoTextSecondary)
                    }

                    TextButton(
                      onClick = {
                        configA = proj.configA
                        configB = proj.configB
                        showProjectsListDialog = false
                        feedbackToast = "Projeto \"${proj.name}\" carregado."
                      }
                    ) {
                      Text("Carregar", style = MaterialTheme.typography.labelSmall, color = SimPurpleLight, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                      onClick = {
                        simRepo.deleteProject(proj.id)
                        savedProjects = simRepo.getSavedProjects()
                      },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = DynoErrorRed, modifier = Modifier.size(16.dp))
                    }
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showProjectsListDialog = false }) {
          Text("FECHAR", color = DynoTextSecondary)
        }
      }
    )
  }
}

// =============================================================================
// COMPONENTE: BADGE DE CONFIANÇA
// =============================================================================
@Composable
fun ConfidenceBadge(confidence: SimulationConfidence, modifier: Modifier = Modifier) {
  val (color, text) = when (confidence) {
    SimulationConfidence.HIGH -> Pair(DynoSuccessGreen, "ALTA CONFIANÇA")
    SimulationConfidence.MEDIUM -> Pair(SimAmberHighlight, "MÉDIA CONFIANÇA")
    SimulationConfidence.LOW -> Pair(DynoBlueLight, "BAIXA CONFIANÇA")
  }

  Surface(
    shape = CircleShape,
    color = color.copy(alpha = 0.15f),
    border = BorderStroke(0.8.dp, color.copy(alpha = 0.5f)),
    modifier = modifier
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
      color = color,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
    )
  }
}

// =============================================================================
// COMPONENTE: QUICK MOD CHIP
// =============================================================================
@Composable
private fun QuickModChip(label: String, onClick: () -> Unit) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = DynoSurfaceContainer,
    border = BorderStroke(1.dp, SimPurplePrimary.copy(alpha = 0.4f)),
    modifier = Modifier.clickable { onClick() }
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp), color = DynoTextPrimary)
    }
  }
}

// =============================================================================
// COMPONENTE: CARD DA TABELA COMPARATIVA DE RESULTADOS (Seção 34)
// =============================================================================
@Composable
private fun ComparisonSummaryCard(
  resultA: SimulationResult,
  resultB: SimulationResult,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("card_comparison_summary"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = DynoSurface),
    border = BorderStroke(1.dp, DynoDivider)
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
          Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SimPurpleLight, modifier = Modifier.size(18.dp))
          Text(
            text = "COMPARAÇÃO DE DESEMPENHO",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            color = DynoTextPrimary
          )
        }
      }

      HorizontalDivider(thickness = 0.6.dp, color = DynoDivider)

      // Cabeçalho da Tabela
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(DynoSurfaceElevated, RoundedCornerShape(6.dp))
          .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text("Métrica", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = DynoTextSecondary, modifier = Modifier.weight(1.5f))
        Text("Atual (A)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = DynoTextSecondary, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
        Text("Simulada (B)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = SimPurpleLight, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
        Text("Diferença", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = DynoTextSecondary, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
      }

      // Linhas da Tabela
      // 1. Potência no Motor
      ComparisonTableRow(
        label = "Potência no motor",
        valA = String.format(Locale.US, "%.0f cv", resultA.config.enginePowerCv),
        valB = String.format(Locale.US, "%.0f cv", resultB.config.enginePowerCv),
        diff = formatPercentDiff(resultB.config.enginePowerCv, resultA.config.enginePowerCv, isHigherBetter = true),
        isPositive = resultB.config.enginePowerCv >= resultA.config.enginePowerCv
      )

      // 2. Potência nas Rodas
      ComparisonTableRow(
        label = "Potência nas rodas",
        valA = String.format(Locale.US, "%.0f cv", resultA.config.estimatedWheelPowerCv),
        valB = String.format(Locale.US, "%.0f cv", resultB.config.estimatedWheelPowerCv),
        diff = formatPercentDiff(resultB.config.estimatedWheelPowerCv, resultA.config.estimatedWheelPowerCv, isHigherBetter = true),
        isPositive = resultB.config.estimatedWheelPowerCv >= resultA.config.estimatedWheelPowerCv
      )

      // 3. Torque no Motor
      ComparisonTableRow(
        label = "Torque no motor",
        valA = String.format(Locale.US, "%.1f kgfm", resultA.config.engineTorqueKgfm),
        valB = String.format(Locale.US, "%.1f kgfm", resultB.config.engineTorqueKgfm),
        diff = formatPercentDiff(resultB.config.engineTorqueKgfm, resultA.config.engineTorqueKgfm, isHigherBetter = true),
        isPositive = resultB.config.engineTorqueKgfm >= resultA.config.engineTorqueKgfm
      )

      // 4. Peso Total
      ComparisonTableRow(
        label = "Peso Total",
        valA = String.format(Locale.US, "%.0f kg", resultA.config.totalWeightKg),
        valB = String.format(Locale.US, "%.0f kg", resultB.config.totalWeightKg),
        diff = formatDiff(resultB.config.totalWeightKg - resultA.config.totalWeightKg, "kg", isLowerBetter = true),
        isPositive = resultB.config.totalWeightKg <= resultA.config.totalWeightKg
      )

      // 5. Relação Peso/Potência
      ComparisonTableRow(
        label = "Peso / Potência",
        valA = String.format(Locale.US, "%.1f kg/cv", resultA.config.weightToPowerRatioKgCv),
        valB = String.format(Locale.US, "%.1f kg/cv", resultB.config.weightToPowerRatioKgCv),
        diff = formatPercentDiff(resultB.config.weightToPowerRatioKgCv, resultA.config.weightToPowerRatioKgCv, isHigherBetter = false),
        isPositive = resultB.config.weightToPowerRatioKgCv <= resultA.config.weightToPowerRatioKgCv,
        isHighlight = true
      )

      // 6. 0 a 60 km/h
      ComparisonTableRow(
        label = "0–60 km/h",
        valA = formatSec(resultA.time0to60Kmh),
        valB = formatSec(resultB.time0to60Kmh),
        diff = formatTimeDiff(resultB.time0to60Kmh, resultA.time0to60Kmh),
        isPositive = (resultB.time0to60Kmh ?: 99f) <= (resultA.time0to60Kmh ?: 99f)
      )

      // 7. 0 a 100 km/h
      ComparisonTableRow(
        label = "0–100 km/h",
        valA = formatSec(resultA.time0to100Kmh),
        valB = formatSec(resultB.time0to100Kmh),
        diff = formatTimeDiff(resultB.time0to100Kmh, resultA.time0to100Kmh),
        isPositive = (resultB.time0to100Kmh ?: 99f) <= (resultA.time0to100Kmh ?: 99f),
        isHighlight = true
      )

      // 8. 60 a 100 km/h
      ComparisonTableRow(
        label = "60–100 km/h",
        valA = formatSec(resultA.time60to100Kmh),
        valB = formatSec(resultB.time60to100Kmh),
        diff = formatTimeDiff(resultB.time60to100Kmh, resultA.time60to100Kmh),
        isPositive = (resultB.time60to100Kmh ?: 99f) <= (resultA.time60to100Kmh ?: 99f)
      )

      // 9. 80 a 120 km/h
      ComparisonTableRow(
        label = "80–120 km/h",
        valA = formatSec(resultA.time80to120Kmh),
        valB = formatSec(resultB.time80to120Kmh),
        diff = formatTimeDiff(resultB.time80to120Kmh, resultA.time80to120Kmh),
        isPositive = (resultB.time80to120Kmh ?: 99f) <= (resultA.time80to120Kmh ?: 99f)
      )

      // 10. 100 a 200 km/h
      ComparisonTableRow(
        label = "100–200 km/h",
        valA = formatSec(resultA.time100to200Kmh),
        valB = formatSec(resultB.time100to200Kmh),
        diff = formatTimeDiff(resultB.time100to200Kmh, resultA.time100to200Kmh),
        isPositive = (resultB.time100to200Kmh ?: 99f) <= (resultA.time100to200Kmh ?: 99f)
      )

      // 11. 100 metros
      ComparisonTableRow(
        label = "100 metros",
        valA = formatSec(resultA.time100m),
        valB = formatSec(resultB.time100m),
        diff = formatTimeDiff(resultB.time100m, resultA.time100m),
        isPositive = (resultB.time100m ?: 99f) <= (resultA.time100m ?: 99f)
      )

      // 12. 201 metros (1/8 milha)
      ComparisonTableRow(
        label = "201 m (1/8 mi)",
        valA = formatSec(resultA.time201m),
        valB = formatSec(resultB.time201m),
        diff = formatTimeDiff(resultB.time201m, resultA.time201m),
        isPositive = (resultB.time201m ?: 99f) <= (resultA.time201m ?: 99f)
      )

      // 13. 402 metros (1/4 milha)
      ComparisonTableRow(
        label = "402 m (1/4 mi)",
        valA = formatSec(resultA.time402m),
        valB = formatSec(resultB.time402m),
        diff = formatTimeDiff(resultB.time402m, resultA.time402m),
        isPositive = (resultB.time402m ?: 99f) <= (resultA.time402m ?: 99f),
        isHighlight = true
      )

      // 14. Velocidade Máxima
      ComparisonTableRow(
        label = "Velocidade máxima",
        valA = String.format(Locale.US, "%.0f km/h", resultA.topSpeedKmh),
        valB = String.format(Locale.US, "%.0f km/h", resultB.topSpeedKmh),
        diff = formatDiff(resultB.topSpeedKmh - resultA.topSpeedKmh, "km/h", isLowerBetter = false),
        isPositive = resultB.topSpeedKmh >= resultA.topSpeedKmh
      )

      // 15. Pico de Força G
      ComparisonTableRow(
        label = "Pico de força G",
        valA = String.format(Locale.US, "%.2f G", resultA.peakLongitudinalG),
        valB = String.format(Locale.US, "%.2f G", resultB.peakLongitudinalG),
        diff = formatDiff(resultB.peakLongitudinalG - resultA.peakLongitudinalG, "G", isLowerBetter = false),
        isPositive = resultB.peakLongitudinalG >= resultA.peakLongitudinalG
      )
    }
  }
}

@Composable
private fun ComparisonTableRow(
  label: String,
  valA: String,
  valB: String,
  diff: String,
  isPositive: Boolean,
  isHighlight: Boolean = false
) {
  val diffColor = if (diff == "-" || diff == "0.0%" || diff == "+0.00s") DynoTextSecondary else if (isPositive) DynoSuccessGreen else DynoErrorRed
  val bgColor = if (isHighlight) SimPurplePrimary.copy(alpha = 0.10f) else Color.Transparent

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(bgColor, RoundedCornerShape(4.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium, fontSize = 12.sp),
      color = if (isHighlight) SimPurpleLight else DynoTextPrimary,
      modifier = Modifier.weight(1.5f)
    )
    Text(
      text = valA,
      style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
      color = DynoTextSecondary,
      modifier = Modifier.weight(1.1f),
      textAlign = TextAlign.End
    )
    Text(
      text = valB,
      style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp),
      color = if (isHighlight) SimPurpleLight else DynoTextPrimary,
      modifier = Modifier.weight(1.1f),
      textAlign = TextAlign.End
    )
    Text(
      text = diff,
      style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
      color = diffColor,
      modifier = Modifier.weight(1.1f),
      textAlign = TextAlign.End
    )
  }
}

// =============================================================================
// COMPONENTE: CARD DOS 5 GRÁFICOS DO SIMULADOR (Seção 35)
// =============================================================================
@Composable
private fun SimulatorGraphsCard(
  resultA: SimulationResult,
  resultB: SimulationResult,
  selectedGraph: SimulatorGraphType,
  onSelectGraph: (SimulatorGraphType) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("card_simulator_graphs"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = DynoSurface),
    border = BorderStroke(1.dp, DynoDivider)
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
          Icon(Icons.Outlined.ShowChart, contentDescription = null, tint = SimPurpleLight, modifier = Modifier.size(18.dp))
          Text(
            text = "GRÁFICOS COMPARATIVOS SOBREPOSTOS",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            color = DynoTextPrimary
          )
        }
      }

      // Seletor de Gráficos (Scroll Horizontal)
      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        items(SimulatorGraphType.entries) { gType ->
          FilterChip(
            selected = selectedGraph == gType,
            onClick = { onSelectGraph(gType) },
            label = { Text(gType.title, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SimPurplePrimary,
              selectedLabelColor = Color.White,
              containerColor = DynoSurfaceElevated,
              labelColor = DynoTextSecondary
            ),
            border = FilterChipDefaults.filterChipBorder(
              borderColor = if (selectedGraph == gType) SimPurplePrimary else DynoDivider,
              enabled = true,
              selected = selectedGraph == gType
            )
          )
        }
      }

      // Área de Desenho do Gráfico Selecionado (Linhas contínuas A vs Linhas tracejadas B)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(260.dp)
          .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
          .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
      ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
          val w = size.width
          val h = size.height
          val padL = 34.dp.toPx()
          val padR = 16.dp.toPx()
          val padT = 16.dp.toPx()
          val padB = 24.dp.toPx()

          val graphW = w - padL - padR
          val graphH = h - padT - padB

          // Grade de fundo
          for (i in 0..4) {
            val yPos = padT + (i.toFloat() / 4f) * graphH
            drawLine(
              color = Color(0xFF1E293B),
              start = Offset(padL, yPos),
              end = Offset(padL + graphW, yPos),
              strokeWidth = 1f,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            )
          }

          // Renderização de cada tipo de gráfico
          when (selectedGraph) {
            SimulatorGraphType.POWER_TORQUE -> {
              // Curvas de Potência (Ciano) e Torque (Laranja) por RPM
              val ptsA = resultA.powerTorqueRpmCurve
              val ptsB = resultB.powerTorqueRpmCurve

              val maxP = maxOf(resultA.config.enginePowerCv, resultB.config.enginePowerCv, 100f) * 1.15f
              val minRpm = 1000f
              val maxRpm = max(resultA.config.maxRpm, resultB.config.maxRpm).toFloat()

              // Potência A (Linha Contínua Ciano)
              drawSimulationCurve(ptsA.map { Pair(it.first.toFloat(), it.second) }, minRpm, maxRpm, maxP, padL, padB, graphW, graphH, DynoPowerCyan, isDashed = false)
              // Potência B (Linha Tracejada Ciano)
              drawSimulationCurve(ptsB.map { Pair(it.first.toFloat(), it.second) }, minRpm, maxRpm, maxP, padL, padB, graphW, graphH, DynoPowerCyan, isDashed = true)

              // Torque A (Linha Contínua Laranja)
              val maxT = maxOf(resultA.config.engineTorqueKgfm, resultB.config.engineTorqueKgfm, 20f) * 1.15f
              drawSimulationCurve(ptsA.map { Pair(it.first.toFloat(), it.third) }, minRpm, maxRpm, maxT, padL, padB, graphW, graphH, DynoTorqueOrange, isDashed = false)
              // Torque B (Linha Tracejada Laranja)
              drawSimulationCurve(ptsB.map { Pair(it.first.toFloat(), it.third) }, minRpm, maxRpm, maxT, padL, padB, graphW, graphH, DynoTorqueOrange, isDashed = true)
            }

            SimulatorGraphType.SPEED_TIME -> {
              // Velocidade x Tempo
              val maxSpeed = max(resultA.topSpeedKmh, resultB.topSpeedKmh).coerceAtLeast(100f) * 1.05f
              val maxTime = max(resultA.points.lastOrNull()?.timeSec ?: 15f, resultB.points.lastOrNull()?.timeSec ?: 15f)

              drawSimulationCurve(resultA.points.map { Pair(it.timeSec, it.speedKmh) }, 0f, maxTime, maxSpeed, padL, padB, graphW, graphH, DynoBlueLight, isDashed = false)
              drawSimulationCurve(resultB.points.map { Pair(it.timeSec, it.speedKmh) }, 0f, maxTime, maxSpeed, padL, padB, graphW, graphH, SimPurpleLight, isDashed = true)
            }

            SimulatorGraphType.DISTANCE_TIME -> {
              // Distância x Tempo
              val maxDist = max(resultA.points.lastOrNull()?.distanceMeters ?: 402f, resultB.points.lastOrNull()?.distanceMeters ?: 402f).coerceAtLeast(402f)
              val maxTime = max(resultA.points.lastOrNull()?.timeSec ?: 15f, resultB.points.lastOrNull()?.timeSec ?: 15f)

              drawSimulationCurve(resultA.points.map { Pair(it.timeSec, it.distanceMeters) }, 0f, maxTime, maxDist, padL, padB, graphW, graphH, DynoBlueLight, isDashed = false)
              drawSimulationCurve(resultB.points.map { Pair(it.timeSec, it.distanceMeters) }, 0f, maxTime, maxDist, padL, padB, graphW, graphH, SimPurpleLight, isDashed = true)
            }

            SimulatorGraphType.TRACTIVE_FORCE -> {
              // Força Trativa x Velocidade
              val maxForce = maxOf(
                resultA.points.maxOfOrNull { it.wheelTractiveForceN } ?: 3000f,
                resultB.points.maxOfOrNull { it.wheelTractiveForceN } ?: 3000f
              ) * 1.1f
              val maxSpeed = max(resultA.topSpeedKmh, resultB.topSpeedKmh).coerceAtLeast(100f)

              drawSimulationCurve(resultA.points.map { Pair(it.speedKmh, it.wheelTractiveForceN) }, 0f, maxSpeed, maxForce, padL, padB, graphW, graphH, DynoTorqueOrange, isDashed = false)
              drawSimulationCurve(resultB.points.map { Pair(it.speedKmh, it.wheelTractiveForceN) }, 0f, maxSpeed, maxForce, padL, padB, graphW, graphH, SimPurpleLight, isDashed = true)
            }

            SimulatorGraphType.RPM_SPEED -> {
              // RPM x Velocidade
              val maxRpm = max(resultA.config.maxRpm, resultB.config.maxRpm).toFloat() * 1.05f
              val maxSpeed = max(resultA.topSpeedKmh, resultB.topSpeedKmh).coerceAtLeast(100f)

              drawSimulationCurve(resultA.points.map { Pair(it.speedKmh, it.engineRpm.toFloat()) }, 0f, maxSpeed, maxRpm, padL, padB, graphW, graphH, DynoBlueLight, isDashed = false)
              drawSimulationCurve(resultB.points.map { Pair(it.speedKmh, it.engineRpm.toFloat()) }, 0f, maxSpeed, maxRpm, padL, padB, graphW, graphH, SimPurpleLight, isDashed = true)
            }
          }
        }

        // Legenda do Gráfico
        Row(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(8.dp)
            .background(Color(0xFF020617).copy(alpha = 0.85f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.width(16.dp).height(2.dp).background(DynoBlueLight))
            Text("Atual (A) — Contínua", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = DynoTextSecondary)
          }
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.width(16.dp).height(2.dp).background(SimPurpleLight))
            Text("Simulada (B) - - Tracejada", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = SimPurpleLight)
          }
        }
      }
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSimulationCurve(
  points: List<Pair<Float, Float>>,
  minX: Float,
  maxX: Float,
  maxY: Float,
  padL: Float,
  padB: Float,
  graphW: Float,
  graphH: Float,
  color: Color,
  isDashed: Boolean
) {
  if (points.size < 2) return
  val rangeX = (maxX - minX).coerceAtLeast(1f)
  val path = Path()
  var isFirst = true

  points.forEach { (x, y) ->
    val normX = ((x - minX) / rangeX).coerceIn(0f, 1f)
    val normY = (y / maxY.coerceAtLeast(1f)).coerceIn(0f, 1f)

    val px = padL + normX * graphW
    val py = size.height - padB - normY * graphH

    if (isFirst) {
      path.moveTo(px, py)
      isFirst = false
    } else {
      path.lineTo(px, py)
    }
  }

  drawPath(
    path = path,
    color = color,
    style = Stroke(
      width = 2.5f,
      cap = StrokeCap.Round,
      pathEffect = if (isDashed) PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f) else null
    )
  )
}

// =============================================================================
// COMPONENTE: TABELA DE VELOCIDADE POR MARCHA (Seção 28)
// =============================================================================
@Composable
private fun GearSpeedTableCard(
  config: SimulationConfig,
  gearSpeeds: List<GearSpeedEntry>,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = DynoSurface),
    border = BorderStroke(1.dp, DynoDivider)
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
          Icon(Icons.Default.Speed, contentDescription = null, tint = SimPurpleLight, modifier = Modifier.size(18.dp))
          Text(
            text = "VELOCIDADE POR MARCHA (SIMULAÇÃO B)",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            color = DynoTextPrimary
          )
        }
      }

      HorizontalDivider(thickness = 0.6.dp, color = DynoDivider)

      // Cabeçalho da Tabela com Scroll Horizontal
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            modifier = Modifier
              .background(DynoSurfaceElevated, RoundedCornerShape(6.dp))
              .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Text("Marcha", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = DynoTextSecondary, modifier = Modifier.width(60.dp))
            Text("Relação", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = DynoTextSecondary, modifier = Modifier.width(55.dp), textAlign = TextAlign.End)
            Text("2.000 rpm", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = DynoTextSecondary, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
            Text("3.000 rpm", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = DynoTextSecondary, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
            Text("4.000 rpm", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = DynoTextSecondary, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
            Text("5.000 rpm", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = DynoTextSecondary, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
            Text("No Corte", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = SimPurpleLight, modifier = Modifier.width(70.dp), textAlign = TextAlign.End)
            Text("RPM Pós-Troca", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = DynoTorqueOrange, modifier = Modifier.width(85.dp), textAlign = TextAlign.End)
          }

          gearSpeeds.forEach { entry ->
            Row(
              modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(entry.gearName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = DynoTextPrimary, modifier = Modifier.width(60.dp))
              Text(String.format(Locale.US, "%.2f", entry.ratio), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = DynoTextSecondary, modifier = Modifier.width(55.dp), textAlign = TextAlign.End)
              Text(String.format(Locale.US, "%.0f km/h", entry.speedAt2000RpmKmh), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = DynoTextSecondary, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
              Text(String.format(Locale.US, "%.0f km/h", entry.speedAt3000RpmKmh), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = DynoTextSecondary, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
              Text(String.format(Locale.US, "%.0f km/h", entry.speedAt4000RpmKmh), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = DynoTextSecondary, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
              Text(String.format(Locale.US, "%.0f km/h", entry.speedAt5000RpmKmh), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = DynoTextSecondary, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
              Text(String.format(Locale.US, "%.0f km/h", entry.speedAtCutoffKmh), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = SimPurpleLight, modifier = Modifier.width(70.dp), textAlign = TextAlign.End)
              val rpmAfter = entry.rpmAfterShift?.let { "${it} RPM" } ?: "Final"
              Text(rpmAfter, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = DynoTorqueOrange, modifier = Modifier.width(85.dp), textAlign = TextAlign.End)
            }
          }
        }
      }
    }
  }
}

// =============================================================================
// COMPONENTE: CARD DO PONTO IDEAL DE TROCA DE MARCHA (Seção 33)
// =============================================================================
@Composable
private fun OptimalShiftPointsCard(
  shiftPoints: List<GearShiftPoint>,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = DynoSurface),
    border = BorderStroke(1.dp, DynoDivider)
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
          Icon(Icons.Default.AutoMode, contentDescription = null, tint = SimPurpleLight, modifier = Modifier.size(18.dp))
          Text(
            text = "PONTOS IDEAIS DE TROCA DE MARCHA",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            color = DynoTextPrimary
          )
        }
      }

      HorizontalDivider(thickness = 0.6.dp, color = DynoDivider)

      shiftPoints.forEach { pt ->
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = DynoSurfaceElevated,
          border = BorderStroke(1.dp, DynoDivider),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "${pt.fromGear}ª ➔ ${pt.toGear}ª Marcha",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = SimPurpleLight
              )
              Text(
                text = "Troca a ${String.format(Locale.US, "%.0f km/h", pt.shiftSpeedKmh)} • Queda para ${pt.rpmAfterShift} RPM",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = DynoTextSecondary
              )
              Text(
                text = pt.explanation,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                color = DynoTextMuted
              )
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = SimPurplePrimary.copy(alpha = 0.15f),
              border = BorderStroke(1.dp, SimPurplePrimary.copy(alpha = 0.5f))
            ) {
              Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text("${pt.recommendedShiftRpm}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace), color = SimPurpleLight)
                Text("RPM", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SimPurpleLight)
              }
            }
          }
        }
      }
    }
  }
}

// =============================================================================
// COMPONENTE: FORMULÁRIO DE EDIÇÃO DE CONFIGURAÇÃO (Seção 25 & 26)
// =============================================================================
@Composable
private fun ConfigEditorForm(
  config: SimulationConfig,
  referenceConfig: SimulationConfig,
  title: String,
  onConfigChange: (SimulationConfig) -> Unit,
  onCopyToOther: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Cabeçalho do Editor
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = SimPurpleLight
      )

      DynoSecondaryButton(
        text = "COPIAR VALORES",
        onClick = onCopyToOther,
        icon = Icons.Default.ContentCopy,
        testTag = "btn_copy_config"
      )
    }

    // 1. SEÇÃO MOTOR & POTÊNCIA (Seção 26)
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = DynoSurface),
      border = BorderStroke(1.dp, DynoDivider)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text("MOTOR & POTÊNCIA", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = DynoPowerCyan)
        HorizontalDivider(thickness = 0.6.dp, color = DynoDivider)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          SimNumberField(
            label = "Potência Motor (cv)",
            value = config.enginePowerCv,
            isModified = config.enginePowerCv != referenceConfig.enginePowerCv,
            onValueChange = { onConfigChange(config.copy(enginePowerCv = it)) },
            modifier = Modifier.weight(1f)
          )

          SimNumberField(
            label = "Torque Motor (kgfm)",
            value = config.engineTorqueKgfm,
            isModified = config.engineTorqueKgfm != referenceConfig.engineTorqueKgfm,
            onValueChange = { onConfigChange(config.copy(engineTorqueKgfm = it)) },
            modifier = Modifier.weight(1f)
          )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          SimIntField(
            label = "RPM Potência",
            value = config.peakPowerRpm,
            isModified = config.peakPowerRpm != referenceConfig.peakPowerRpm,
            onValueChange = { onConfigChange(config.copy(peakPowerRpm = it)) },
            modifier = Modifier.weight(1f)
          )

          SimIntField(
            label = "RPM Torque",
            value = config.peakTorqueRpm,
            isModified = config.peakTorqueRpm != referenceConfig.peakTorqueRpm,
            onValueChange = { onConfigChange(config.copy(peakTorqueRpm = it)) },
            modifier = Modifier.weight(1f)
          )

          SimIntField(
            label = "Corte RPM",
            value = config.maxRpm,
            isModified = config.maxRpm != referenceConfig.maxRpm,
            onValueChange = { onConfigChange(config.copy(maxRpm = it)) },
            modifier = Modifier.weight(1f)
          )
        }

        // Subseção Turbo Simulado (Seção 26)
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = DynoSurfaceElevated,
          border = BorderStroke(1.dp, if (config.isTurboSimulated) SimPurplePrimary else DynoDivider),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Pressão de Turbo Simulada", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = DynoTextPrimary)
                Text("Multiplicador empírico de pressão sobre a potência aspirada", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = DynoTextSecondary)
              }
              Switch(
                checked = config.isTurboSimulated,
                onCheckedChange = { onConfigChange(config.copy(isTurboSimulated = it, turboBoostBar = if (it && config.turboBoostBar <= 0f) 0.8f else config.turboBoostBar)) },
                colors = SwitchDefaults.colors(checkedThumbColor = SimPurpleLight, checkedTrackColor = SimPurplePrimary.copy(alpha = 0.5f))
              )
            }

            if (config.isTurboSimulated) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("Pressão: ${String.format(Locale.US, "%.1f bar", config.turboBoostBar)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = SimPurpleLight)
                Text("Eficiência: ${(config.turboEfficiency * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = DynoTextSecondary)
              }

              Slider(
                value = config.turboBoostBar,
                onValueChange = { onConfigChange(config.copy(turboBoostBar = it)) },
                valueRange = 0.1f..2.5f,
                steps = 23,
                colors = SliderDefaults.colors(thumbColor = SimPurpleLight, activeTrackColor = SimPurplePrimary)
              )

              Text(
                text = "Estimativa teórica. O resultado real depende do turbo, combustível, ponto de ignição, mistura, temperatura, cabeçote e eficiência do motor.",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
                color = DynoWarningYellow
              )
            }
          }
        }
      }
    }

    // 2. SEÇÃO PESO E MASSA
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = DynoSurface),
      border = BorderStroke(1.dp, DynoDivider)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text("PESO E CARGA", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = SimAmberHighlight)
        HorizontalDivider(thickness = 0.6.dp, color = DynoDivider)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          SimNumberField(
            label = "Peso Veículo (kg)",
            value = config.vehicleCurbWeightKg,
            isModified = config.vehicleCurbWeightKg != referenceConfig.vehicleCurbWeightKg,
            onValueChange = { onConfigChange(config.copy(vehicleCurbWeightKg = it)) },
            modifier = Modifier.weight(1f)
          )

          SimNumberField(
            label = "Motorista (kg)",
            value = config.driverWeightKg,
            isModified = config.driverWeightKg != referenceConfig.driverWeightKg,
            onValueChange = { onConfigChange(config.copy(driverWeightKg = it)) },
            modifier = Modifier.weight(1f)
          )

          SimNumberField(
            label = "Adicional (kg)",
            value = config.additionalWeightKg,
            isModified = config.additionalWeightKg != referenceConfig.additionalWeightKg,
            onValueChange = { onConfigChange(config.copy(additionalWeightKg = it)) },
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // 3. SEÇÃO CÂMBIO & TRANSMISSÃO
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = DynoSurface),
      border = BorderStroke(1.dp, DynoDivider)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text("CÂMBIO & TRANSMISSÃO", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = DynoBlueLight)
        HorizontalDivider(thickness = 0.6.dp, color = DynoDivider)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          SimNumberField(
            label = "Diferencial",
            value = config.finalDriveRatio,
            isModified = config.finalDriveRatio != referenceConfig.finalDriveRatio,
            onValueChange = { onConfigChange(config.copy(finalDriveRatio = it)) },
            modifier = Modifier.weight(1f)
          )

          SimNumberField(
            label = "Perda Transm. (%)",
            value = config.drivetrainLossPercent,
            isModified = config.drivetrainLossPercent != referenceConfig.drivetrainLossPercent,
            onValueChange = { onConfigChange(config.copy(drivetrainLossPercent = it)) },
            modifier = Modifier.weight(1f)
          )

          SimNumberField(
            label = "Troca de Marcha (s)",
            value = config.shiftTimeSeconds,
            isModified = config.shiftTimeSeconds != referenceConfig.shiftTimeSeconds,
            onValueChange = { onConfigChange(config.copy(shiftTimeSeconds = it)) },
            modifier = Modifier.weight(1f)
          )
        }

        // Relações de 1ª a 5ª/6ª
        Text("Relações de Marcha:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = DynoTextSecondary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          config.gearRatios.forEachIndexed { idx, r ->
            val refR = referenceConfig.gearRatios.getOrNull(idx) ?: r
            SimNumberField(
              label = "${idx + 1}ª",
              value = r,
              isModified = r != refR,
              onValueChange = { newR ->
                val updated = config.gearRatios.toMutableList()
                updated[idx] = newR
                onConfigChange(config.copy(gearRatios = updated))
              },
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    // 4. SEÇÃO PNEUS & AERODINÂMICA
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = DynoSurface),
      border = BorderStroke(1.dp, DynoDivider)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text("PNEUS, AERODINÂMICA & PISTA", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = DynoSuccessGreen)
        HorizontalDivider(thickness = 0.6.dp, color = DynoDivider)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          SimIntField(
            label = "Largura (mm)",
            value = config.tireWidthMm,
            isModified = config.tireWidthMm != referenceConfig.tireWidthMm,
            onValueChange = { onConfigChange(config.copy(tireWidthMm = it)) },
            modifier = Modifier.weight(1f)
          )

          SimIntField(
            label = "Perfil (%)",
            value = config.tireAspectRatio,
            isModified = config.tireAspectRatio != referenceConfig.tireAspectRatio,
            onValueChange = { onConfigChange(config.copy(tireAspectRatio = it)) },
            modifier = Modifier.weight(1f)
          )

          SimIntField(
            label = "Aro (pol)",
            value = config.rimDiameterInches,
            isModified = config.rimDiameterInches != referenceConfig.rimDiameterInches,
            onValueChange = { onConfigChange(config.copy(rimDiameterInches = it)) },
            modifier = Modifier.weight(1f)
          )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          SimNumberField(
            label = "Cd (Arrasto)",
            value = config.cd,
            isModified = config.cd != referenceConfig.cd,
            onValueChange = { onConfigChange(config.copy(cd = it)) },
            modifier = Modifier.weight(1f)
          )

          SimNumberField(
            label = "Aderência μ",
            value = config.tireGripMu,
            isModified = config.tireGripMu != referenceConfig.tireGripMu,
            onValueChange = { onConfigChange(config.copy(tireGripMu = it)) },
            modifier = Modifier.weight(1f)
          )

          SimNumberField(
            label = "Vento Contra (km/h)",
            value = config.headwindSpeedKmh,
            isModified = config.headwindSpeedKmh != referenceConfig.headwindSpeedKmh,
            onValueChange = { onConfigChange(config.copy(headwindSpeedKmh = it)) },
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
  }
}

@Composable
private fun SimNumberField(
  label: String,
  value: Float,
  isModified: Boolean,
  onValueChange: (Float) -> Unit,
  modifier: Modifier = Modifier
) {
  var text by remember(value) { mutableStateOf(String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')) }

  OutlinedTextField(
    value = text,
    onValueChange = {
      text = it
      it.toFloatOrNull()?.let { v -> onValueChange(v) }
    },
    label = { Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = if (isModified) SimAmberHighlight else SimPurplePrimary,
      unfocusedBorderColor = if (isModified) SimAmberHighlight.copy(alpha = 0.8f) else DynoDivider,
      focusedLabelColor = if (isModified) SimAmberHighlight else SimPurpleLight
    ),
    modifier = modifier
  )
}

@Composable
private fun SimIntField(
  label: String,
  value: Int,
  isModified: Boolean,
  onValueChange: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  var text by remember(value) { mutableStateOf(value.toString()) }

  OutlinedTextField(
    value = text,
    onValueChange = {
      text = it
      it.toIntOrNull()?.let { v -> onValueChange(v) }
    },
    label = { Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = if (isModified) SimAmberHighlight else SimPurplePrimary,
      unfocusedBorderColor = if (isModified) SimAmberHighlight.copy(alpha = 0.8f) else DynoDivider,
      focusedLabelColor = if (isModified) SimAmberHighlight else SimPurpleLight
    ),
    modifier = modifier
  )
}

// =============================================================================
// COMPONENTE: AVISO DE ISENÇÃO TÉCNICO (Seção 39 & 40)
// =============================================================================
@Composable
private fun DisclaimerTechnicalCard(modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = DynoSurfaceElevated,
    border = BorderStroke(1.dp, DynoDivider),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Icon(Icons.Default.Info, contentDescription = null, tint = DynoTextMuted, modifier = Modifier.size(20.dp))
      Text(
        text = "Este resultado é uma estimativa matemática baseada nas leis da dinâmica veicular. Condições da pista, temperatura do asfalto, vento real, tempo exato de embreagem e curvas térmicas do motor podem alterar os números em pista real.",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
        color = DynoTextSecondary
      )
    }
  }
}

// =============================================================================
// FUNÇÕES AUXILIARES DE CONVERSÃO E FORMATAÇÃO
// =============================================================================
private fun createConfigFromRunOrVehicle(run: RunResult?, veh: VehicleProfile?): SimulationConfig {
  return when {
    run != null -> createConfigFromRun(run)
    veh != null -> createConfigFromVehicle(veh)
    else -> SimulationConfig()
  }
}

private fun createConfigFromRun(run: RunResult): SimulationConfig {
  val pts = run.samples
    .filter { it.engineRpm != null && it.enginePowerCv > 0f }
    .map { Pair(it.engineRpm!!, it.enginePowerCv) }

  return SimulationConfig(
    label = "Atual (A)",
    vehicleName = run.vehicleName.ifBlank { "Veículo do Histórico" },
    vehicleCurbWeightKg = (run.totalVehicleMassKg - 80f).coerceAtLeast(400f),
    driverWeightKg = 80f,
    additionalWeightKg = 0f,
    enginePowerCv = if (run.enginePowerCv > 0f) run.enginePowerCv else run.estimatedPowerCv.coerceAtLeast(100f),
    engineTorqueKgfm = if (run.engineTorqueKgfm > 0f) run.engineTorqueKgfm else run.estimatedTorqueKgfm.coerceAtLeast(15f),
    peakPowerRpm = run.peakPowerRpm ?: 5800,
    peakTorqueRpm = run.peakTorqueRpm ?: 3800,
    maxRpm = (run.peakPowerRpm ?: 5800) + 700,
    drivetrainLossPercent = run.drivetrainLossPercent,
    cd = run.cdUsed,
    frontalAreaM2 = run.frontalAreaUsed,
    crr = run.crrUsed,
    customPowerCurvePoints = pts,
    isUsingRealRunCurve = pts.isNotEmpty(),
    baseRunId = run.id
  )
}

private fun createConfigFromVehicle(veh: VehicleProfile): SimulationConfig {
  val defaultRatios = listOf(3.73f, 2.05f, 1.36f, 1.03f, 0.82f)

  return SimulationConfig(
    label = "Atual (A)",
    vehicleName = "${veh.manufacturer} ${veh.model}",
    vehicleCurbWeightKg = veh.curbWeightKg.coerceAtLeast(400f),
    driverWeightKg = veh.driverWeightKg,
    additionalWeightKg = veh.passengerWeightKg + veh.cargoWeightKg + veh.audioWeightKg + veh.gnvWeightKg + veh.otherWeightKg,
    enginePowerCv = veh.factoryPowerCv ?: 120f,
    engineTorqueKgfm = veh.factoryTorqueKgf ?: 16.5f,
    peakPowerRpm = 5600,
    peakTorqueRpm = 3600,
    maxRpm = 6400,
    gearRatios = defaultRatios,
    finalDriveRatio = veh.finalDriveRatio ?: 4.10f,
    drivetrainLossPercent = veh.customDrivetrainLossPercent ?: 12f,
    drivetrainType = DrivetrainType.fromString(veh.drivetrain),
    tireWidthMm = if (veh.tireWidthMm > 0) veh.tireWidthMm else 195,
    tireAspectRatio = if (veh.tireAspectRatio > 0) veh.tireAspectRatio else 55,
    rimDiameterInches = if (veh.wheelDiameterInches > 0) veh.wheelDiameterInches else 15,
    cd = if (veh.dragCoefficient > 0f) veh.dragCoefficient else 0.33f,
    frontalAreaM2 = if (veh.frontalAreaM2 > 0f) veh.frontalAreaM2 else 2.10f,
    isUsingRealRunCurve = false,
    baseRunId = null
  )
}

private fun formatSec(v: Float?): String {
  return v?.let { String.format(Locale.US, "%.2fs", it) } ?: "-"
}

private fun formatTimeDiff(b: Float?, a: Float?): String {
  if (b == null || a == null) return "-"
  val diff = b - a
  val sign = if (diff > 0f) "+" else ""
  return String.format(Locale.US, "%s%.2fs", sign, diff)
}

private fun formatDiff(diff: Float, unit: String, isLowerBetter: Boolean): String {
  if (abs(diff) < 0.001f) return "-"
  val sign = if (diff > 0f) "+" else ""
  return String.format(Locale.US, "%s%.1f %s", sign, diff, unit)
}

private fun formatPercentDiff(b: Float, a: Float, isHigherBetter: Boolean): String {
  if (a <= 0f || b <= 0f) return "-"
  val diffPercent = ((b - a) / a) * 100f
  val sign = if (diffPercent > 0f) "+" else ""
  return String.format(Locale.US, "%s%.1f%%", sign, diffPercent)
}
