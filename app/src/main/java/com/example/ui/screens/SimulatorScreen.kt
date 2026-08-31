package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import com.example.data.RunResultRepository
import com.example.data.TuningBuildRepository
import com.example.data.VehicleRepository
import com.example.model.*
import com.example.ui.components.DynoPrimaryButton
import com.example.ui.components.DynoSecondaryButton
import com.example.ui.theme.*
import java.util.Locale

val SimPurplePrimary = Color(0xFF8B5CF6)
val SimPurpleLight = Color(0xFFA78BFA)
val SimPurpleContainer = Color(0xFF4C1D95).copy(alpha = 0.35f)
val SimAmberHighlight = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(
  initialRunId: String? = null,
  onNavigateToRunDetails: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val tuningRepo = remember { TuningBuildRepository(context) }
  val vehicleRepo = remember { VehicleRepository(context) }
  val runRepo = remember { RunResultRepository(context) }

  val allVehicles = remember { vehicleRepo.getVehicles() }
  val allRuns = remember { runRepo.getResults() }

  var savedBuilds by remember { mutableStateOf(tuningRepo.getSavedBuilds()) }
  var currentBuild by remember { mutableStateOf(tuningRepo.getActiveBuild()) }

  // Diálogos
  var showDynoModal by remember { mutableStateOf(false) }
  var showCompareModal by remember { mutableStateOf(false) }
  var showVehicleSetupModal by remember { mutableStateOf(false) }
  var showSaveProjectDialog by remember { mutableStateOf(false) }
  var showProjectSwitcherDialog by remember { mutableStateOf(false) }
  var showBudgetModal by remember { mutableStateOf(false) }

  var projectNameInput by remember { mutableStateOf(currentBuild.projectName) }
  var selectedCategory by remember { mutableStateOf(TuningCategory.MOTOR) }

  // Se veio de um teste real inicial, adapta
  LaunchedEffect(initialRunId) {
    if (initialRunId != null) {
      val run = allRuns.firstOrNull { it.id == initialRunId }
      if (run != null) {
        currentBuild = currentBuild.copy(
          projectName = "Projeto a partir de Passagem Real",
          vehicleName = run.vehicleName,
          baseRunId = run.id,
          factoryEnginePowerCv = run.enginePowerCv,
          factoryEngineTorqueKgfm = run.engineTorqueKgfm,
          factoryPeakPowerRpm = run.peakPowerRpm ?: 5600,
          factoryPeakTorqueRpm = run.peakTorqueRpm ?: 3600,
          baseVehicleCurbWeightKg = (run.totalVehicleMassKg - 80f).coerceAtLeast(600f)
        )
      }
    }
  }

  // Cálculo da Preparação em Tempo Real
  val tuningResult = remember(currentBuild) {
    GarageTuningEngine.calculateTuningBuild(currentBuild)
  }

  // Simulação de Aceleração Dinâmica
  val dynoTestResult = remember(tuningResult) {
    GarageTuningEngine.runDynoTrackSimulation(tuningResult)
  }

  val scrollState = rememberScrollState()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DynoBackground)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
      // -----------------------------------------------------------------------------------
      // 1. CABEÇALHO DA GARAGEM & SELETOR DE PROJETOS
      // -----------------------------------------------------------------------------------
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DynoSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SimPurplePrimary.copy(alpha = 0.4f))
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .background(SimPurplePrimary.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Filled.Build, contentDescription = null, tint = SimPurpleLight, modifier = Modifier.size(18.dp))
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    currentBuild.projectName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DynoTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  IconButton(
                    onClick = { showProjectSwitcherDialog = true },
                    modifier = Modifier.size(24.dp).testTag("switch_project_button")
                  ) {
                    Icon(Icons.Filled.Tune, contentDescription = "Trocar", tint = SimPurpleLight, modifier = Modifier.size(16.dp))
                  }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    currentBuild.vehicleName,
                    fontSize = 12.sp,
                    color = DynoTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  IconButton(
                    onClick = { showVehicleSetupModal = true },
                    modifier = Modifier.size(20.dp).testTag("edit_vehicle_specs_button")
                  ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = DynoTextMuted, modifier = Modifier.size(13.dp))
                  }
                }
              }
            }

            // Nível do Projeto (Stage Badge)
            Surface(
              color = Color(tuningResult.projectLevel.badgeColorHex).copy(alpha = 0.2f),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, Color(tuningResult.projectLevel.badgeColorHex).copy(alpha = 0.8f))
            ) {
              Text(
                tuningResult.projectLevel.title.uppercase(),
                color = Color(tuningResult.projectLevel.badgeColorHex),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = DynoDivider)
          Spacer(modifier = Modifier.height(10.dp))

          // ---------------------------------------------------------------------------------
          // 2. DASHBOARD DE MÉTRICAS EM TEMPO REAL (TOP HUD)
          // ---------------------------------------------------------------------------------
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            // Potência Estimada
            Column(modifier = Modifier.weight(1f)) {
              Text("POTÊNCIA", fontSize = 10.sp, color = DynoTextMuted, fontWeight = FontWeight.Bold)
              Text(
                "${String.format(Locale.US, "%.0f", tuningResult.estimatedEnginePowerCv)} cv",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = DynoPowerCyan
              )
              Text(
                "${String.format(Locale.US, "%.0f", tuningResult.estimatedWheelPowerCv)} cv rodas",
                fontSize = 10.sp,
                color = DynoTextSecondary
              )
            }

            // Torque Estimado
            Column(modifier = Modifier.weight(1f)) {
              Text("TORQUE", fontSize = 10.sp, color = DynoTextMuted, fontWeight = FontWeight.Bold)
              Text(
                "${String.format(Locale.US, "%.1f", tuningResult.estimatedEngineTorqueKgfm)} kgf",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = DynoTorqueOrange
              )
              Text(
                "@ ${tuningResult.peakTorqueRpm} RPM",
                fontSize = 10.sp,
                color = DynoTextSecondary
              )
            }

            // Turbo / Pressão
            Column(modifier = Modifier.weight(1f)) {
              Text("TURBO", fontSize = 10.sp, color = DynoTextMuted, fontWeight = FontWeight.Bold)
              Text(
                if (currentBuild.aspiration != AspirationType.ASPIRADO) "${String.format(Locale.US, "%.1f", tuningResult.actualBoostBar)} bar" else "Aspirado",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = if (currentBuild.aspiration != AspirationType.ASPIRADO) SimPurpleLight else DynoTextSecondary
              )
              Text(
                "${String.format(Locale.US, "%.1f", tuningResult.weightToPowerRatioKgCv)} kg/cv",
                fontSize = 10.sp,
                color = DynoTextMuted
              )
            }

            // Uso dos Bicos & Confiabilidade
            Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.End) {
              Text("USO DOS BICOS", fontSize = 10.sp, color = DynoTextMuted, fontWeight = FontWeight.Bold)
              val duty = tuningResult.injectorDutyCyclePercent
              val dutyColor = when {
                duty <= 80f -> DynoSuccessGreen
                duty <= 90f -> DynoWarningYellow
                duty <= 100f -> DynoTorqueOrange
                else -> DynoErrorRed
              }
              Text(
                "${String.format(Locale.US, "%.1f", duty)}%",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = dutyColor
              )
              Text(
                "Confiab.: ${tuningResult.reliabilityScore}%",
                fontSize = 10.sp,
                color = if (tuningResult.reliabilityScore >= 75) DynoSuccessGreen else DynoWarningYellow,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // -----------------------------------------------------------------------------------
      // 3. BARRA DE BOTÕES DE AÇÃO PRINCIPAIS (Seção 1)
      // -----------------------------------------------------------------------------------
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Testar Configuração (Abre o Dinamômetro Virtual)
        Button(
          onClick = { showDynoModal = true },
          colors = ButtonDefaults.buttonColors(
            containerColor = SimPurplePrimary,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
          modifier = Modifier.weight(1.3f).testTag("test_configuration_button")
        ) {
          Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Testar Dyno", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        // Comparar com Original
        OutlinedButton(
          onClick = { showCompareModal = true },
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, DynoBorder),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = DynoTextPrimary),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
          modifier = Modifier.weight(1f).testTag("compare_with_stock_button")
        ) {
          Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = SimPurpleLight, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Comparar", fontSize = 12.sp)
        }

        // Salvar Projeto
        OutlinedButton(
          onClick = {
            projectNameInput = currentBuild.projectName
            showSaveProjectDialog = true
          },
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, DynoBorder),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = DynoTextPrimary),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
          modifier = Modifier.weight(1f).testTag("save_project_button")
        ) {
          Icon(Icons.Filled.Save, contentDescription = null, tint = DynoPowerCyan, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Salvar", fontSize = 12.sp)
        }

        // Voltar ao Original (Reset)
        IconButton(
          onClick = {
            currentBuild = GarageTuningEngine.applyProjectTemplate(ProjectTemplateType.ORIGINAL, currentBuild)
            Toast.makeText(context, "Configuração original restaurada!", Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier
            .background(DynoSurfaceElevated, RoundedCornerShape(10.dp))
            .size(42.dp)
            .testTag("reset_to_stock_button")
        ) {
          Icon(Icons.Filled.Refresh, contentDescription = "Restaurar Original", tint = DynoTextMuted, modifier = Modifier.size(18.dp))
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // -----------------------------------------------------------------------------------
      // 4. PRESETS RÁPIDOS DE PROJETO (Seção 3)
      // -----------------------------------------------------------------------------------
      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        val presets = listOf(
          ProjectTemplateType.ORIGINAL to "Aspirado Original",
          ProjectTemplateType.ASPIRADO_PREPARADO to "Aspirado 276°",
          ProjectTemplateType.TURBO_BAIXA_PRESSAO to "Turbo 0.5 bar",
          ProjectTemplateType.TURBO_INTERMEDIARIO to "Turbo 0.8 bar",
          ProjectTemplateType.TURBO_FORJADO_ALTA to "Turbo 1.5 bar Forjado",
          ProjectTemplateType.SUPERCHARGER to "Supercharger"
        )
        items(presets) { (template, label) ->
          Surface(
            color = DynoSurfaceContainer,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, if (currentBuild.projectName.contains(label)) SimPurplePrimary else DynoBorder),
            modifier = Modifier.clickable {
              currentBuild = GarageTuningEngine.applyProjectTemplate(template, currentBuild)
              Toast.makeText(context, "Preset '$label' aplicado!", Toast.LENGTH_SHORT).show()
            }
          ) {
            Text(
              label,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (currentBuild.projectName.contains(label)) SimPurpleLight else DynoTextSecondary,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // -----------------------------------------------------------------------------------
      // 5. BANNER DE GARGALO ATUAL & DIAGNÓSTICO MECÂNICO (Seções 14, 15)
      // -----------------------------------------------------------------------------------
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = if (tuningResult.allWarnings.isNotEmpty()) DynoWarningYellow.copy(alpha = 0.12f) else DynoSurfaceContainer
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
          1.dp,
          if (tuningResult.allWarnings.isNotEmpty()) DynoWarningYellow.copy(alpha = 0.5f) else DynoBorder
        )
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              if (tuningResult.allWarnings.isNotEmpty()) Icons.Filled.Warning else Icons.Filled.Check,
              contentDescription = null,
              tint = if (tuningResult.allWarnings.isNotEmpty()) DynoWarningYellow else DynoSuccessGreen,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              tuningResult.primaryBottleneckTitle,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = if (tuningResult.allWarnings.isNotEmpty()) DynoWarningYellow else DynoTextPrimary
            )
          }

          Spacer(modifier = Modifier.height(4.dp))
          Text(
            tuningResult.primaryBottleneckDescription,
            fontSize = 11.sp,
            color = DynoTextSecondary,
            lineHeight = 15.sp
          )

          // Limite Estrutural do Motor
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "Internos do Motor: ${tuningResult.structuralRiskLevel}",
              fontSize = 11.sp,
              color = if (tuningResult.engineStressPercent > 100f) DynoErrorRed else DynoTextMuted,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              "Limite: ${String.format(Locale.US, "%.0f", tuningResult.engineStructuralLimitHp)} cv",
              fontSize = 11.sp,
              color = DynoTextMuted
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // -----------------------------------------------------------------------------------
      // 6. INDICADORES VISUAIS (9 BARRAS DE CAPACIDADE & CONFIABILIDADE) (Seção 16)
      // -----------------------------------------------------------------------------------
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DynoSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DynoBorder)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text("Diagnóstico e Limites do Conjunto", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DynoTextPrimary)
          Spacer(modifier = Modifier.height(8.dp))

          GaugeBarItem("Uso dos Bicos (Duty)", tuningResult.gauges.injectorDutyPercent, 80f, 95f, "%")
          GaugeBarItem("Uso da Bomba de Combustível", tuningResult.gauges.fuelPumpUsagePercent, 80f, 95f, "%")
          GaugeBarItem("Tensão Estrutural do Motor", tuningResult.gauges.engineStressPercent, 80f, 100f, "%")
          GaugeBarItem("Tensão na Embreagem", tuningResult.gauges.clutchStressPercent, 80f, 100f, "%")
          GaugeBarItem("Confiabilidade do Projeto", tuningResult.gauges.reliabilityPercent, 50f, 75f, "%", isInverted = true)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // -----------------------------------------------------------------------------------
      // 7. CATEGORIAS DE PREPARAÇÃO (14 ABAS & COMPONENTES DETALHADOS) (Seção 1)
      // -----------------------------------------------------------------------------------
      Text("Categorias de Preparação", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DynoTextPrimary)
      Spacer(modifier = Modifier.height(8.dp))

      ScrollableTabRow(
        selectedTabIndex = TuningCategory.entries.indexOf(selectedCategory),
        containerColor = DynoSurfaceContainer,
        contentColor = DynoTextPrimary,
        edgePadding = 6.dp,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            Modifier.tabIndicatorOffset(tabPositions[TuningCategory.entries.indexOf(selectedCategory)]),
            color = SimPurplePrimary
          )
        }
      ) {
        TuningCategory.entries.forEach { cat ->
          val isSel = cat == selectedCategory
          Tab(
            selected = isSel,
            onClick = { selectedCategory = cat },
            text = {
              Text(
                cat.title.substringBefore(" & "),
                fontSize = 12.sp,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                color = if (isSel) SimPurpleLight else DynoTextSecondary
              )
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Painel da Categoria Selecionada
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DynoSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SimPurplePrimary.copy(alpha = 0.3f))
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(selectedCategory.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SimPurpleLight)
          }
          Text(selectedCategory.subtitle, fontSize = 12.sp, color = DynoTextMuted)
          Spacer(modifier = Modifier.height(12.dp))
          HorizontalDivider(color = DynoDivider)
          Spacer(modifier = Modifier.height(12.dp))

          // Renderizador dinâmico de cada categoria
          when (selectedCategory) {
            TuningCategory.MOTOR -> MotorCategoryContent(currentBuild) { currentBuild = it }
            TuningCategory.ALIMENTACAO -> AlimentacaoCategoryContent(currentBuild, tuningResult) { currentBuild = it }
            TuningCategory.ASPIRACAO -> AspiracaoCategoryContent(currentBuild) { currentBuild = it }
            TuningCategory.COMBUSTIVEL -> CombustivelCategoryContent(currentBuild) { currentBuild = it }
            TuningCategory.INJECAO -> InjecaoCategoryContent(currentBuild) { currentBuild = it }
            TuningCategory.IGNICAO -> IgnicaoCategoryContent(currentBuild) { currentBuild = it }
            TuningCategory.ADMISSAO -> AdmissaoCategoryContent(currentBuild) { currentBuild = it }
            TuningCategory.ESCAPE -> EscapeCategoryContent(currentBuild) { currentBuild = it }
            TuningCategory.CABECOTE -> CabecoteCategoryContent(currentBuild) { currentBuild = it }
            TuningCategory.COMANDO -> ComandoCategoryContent(currentBuild) { currentBuild = it }
            TuningCategory.TRANSMISSAO -> TransmissaoCategoryContent(currentBuild) { currentBuild = it }
            TuningCategory.PNEUS_TRACAO -> PneusCategoryContent(currentBuild) { currentBuild = it }
            TuningCategory.PESO -> PesoCategoryContent(currentBuild) { currentBuild = it }
            TuningCategory.AERODINAMICA -> AeroCategoryContent(currentBuild) { currentBuild = it }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // -----------------------------------------------------------------------------------
      // 8. SISTEMA DE NOTAS E PONTUAÇÃO (0 A 100) (Seção 19)
      // -----------------------------------------------------------------------------------
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DynoSurfaceContainer),
        shape = RoundedCornerShape(12.dp)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = SimAmberHighlight, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Avaliação Geral do Projeto (0 a 100)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DynoTextPrimary)
          }
          Spacer(modifier = Modifier.height(10.dp))

          ScoreBarItem("Desempenho & Aceleração", tuningResult.performanceScore, DynoPowerCyan)
          ScoreBarItem("Resposta de Acelerador / Spool", tuningResult.spoolResponseScore, SimPurpleLight)
          ScoreBarItem("Confiabilidade & Durabilidade", tuningResult.reliabilityScore, DynoSuccessGreen)
          ScoreBarItem("Custo-Benefício", tuningResult.costBenefitScore, DynoWarningYellow)
          ScoreBarItem("Usabilidade para o Dia a Dia", tuningResult.dailyDriveScore, DynoTorqueOrange)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // -----------------------------------------------------------------------------------
      // 9. ORÇAMENTO & PRÓXIMA MELHORIA RECOMENDADA (Seção 20)
      // -----------------------------------------------------------------------------------
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
            Column {
              Text("Orçamento Estimado", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DynoTextPrimary)
              Text("Peças + Mão de Obra", fontSize = 11.sp, color = DynoTextMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                "R$ ${String.format(Locale.US, "%,.2f", tuningResult.grandTotalCostBrl)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = DynoSuccessGreen
              )
              if (tuningResult.costPerCvGainedBrl > 0.0) {
                Text(
                  "R$ ${String.format(Locale.US, "%.1f", tuningResult.costPerCvGainedBrl)} / cv ganho",
                  fontSize = 10.sp,
                  color = DynoTextSecondary
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          HorizontalDivider(color = DynoDivider)
          Spacer(modifier = Modifier.height(8.dp))

          Text("Próxima melhoria recomendada:", fontSize = 11.sp, color = DynoTextMuted, fontWeight = FontWeight.SemiBold)
          Text(tuningResult.nextRecommendedUpgrade, fontSize = 12.sp, color = SimPurpleLight, fontWeight = FontWeight.SemiBold)
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }

  // ---------------------------------------------------------------------------------------
  // DIÁLOGOS E MODAIS
  // ---------------------------------------------------------------------------------------

  if (showDynoModal) {
    VirtualDynoPullDialog(
      tuningResult = tuningResult,
      dynoTestResult = dynoTestResult,
      onDismiss = { showDynoModal = false }
    )
  }

  if (showCompareModal) {
    TuningComparisonDialog(
      currentBuild = currentBuild,
      currentResult = tuningResult,
      currentTrack = dynoTestResult,
      onDismiss = { showCompareModal = false }
    )
  }

  if (showVehicleSetupModal) {
    VehicleSetupDialog(
      currentBuild = currentBuild,
      onSave = { updated ->
        currentBuild = updated
        tuningRepo.saveBuild(updated)
      },
      onDismiss = { showVehicleSetupModal = false }
    )
  }

  if (showSaveProjectDialog) {
    AlertDialog(
      onDismissRequest = { showSaveProjectDialog = false },
      title = { Text("Salvar Projeto na Garagem") },
      text = {
        OutlinedTextField(
          value = projectNameInput,
          onValueChange = { projectNameInput = it },
          label = { Text("Nome do Projeto") },
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            val toSave = currentBuild.copy(projectName = projectNameInput.ifBlank { "Meu Projeto" })
            currentBuild = toSave
            tuningRepo.saveBuild(toSave)
            savedBuilds = tuningRepo.getSavedBuilds()
            showSaveProjectDialog = false
            Toast.makeText(context, "Projeto salvo com sucesso!", Toast.LENGTH_SHORT).show()
          }
        ) {
          Text("Salvar", color = SimPurpleLight)
        }
      },
      dismissButton = {
        TextButton(onClick = { showSaveProjectDialog = false }) { Text("Cancelar") }
      }
    )
  }

  if (showProjectSwitcherDialog) {
    AlertDialog(
      onDismissRequest = { showProjectSwitcherDialog = false },
      title = { Text("Meus Projetos Salvos") },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          savedBuilds.forEach { b ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  currentBuild = b
                  tuningRepo.setActiveBuildId(b.id)
                  showProjectSwitcherDialog = false
                }
                .padding(vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(b.projectName, fontWeight = FontWeight.Bold, color = if (b.id == currentBuild.id) SimPurpleLight else DynoTextPrimary)
                Text(b.vehicleName, fontSize = 11.sp, color = DynoTextMuted)
              }
              if (b.id == currentBuild.id) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = SimPurpleLight)
              }
            }
            HorizontalDivider(color = DynoDivider)
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            val newBuild = GarageTuningEngine.createDefaultVectraBuild().copy(
              projectName = "Novo Projeto ${savedBuilds.size + 1}"
            )
            tuningRepo.saveBuild(newBuild)
            savedBuilds = tuningRepo.getSavedBuilds()
            currentBuild = newBuild
            showProjectSwitcherDialog = false
          }
        ) {
          Text("Criar Novo Projeto", color = SimPurpleLight)
        }
      },
      dismissButton = {
        TextButton(onClick = { showProjectSwitcherDialog = false }) { Text("Fechar") }
      }
    )
  }
}

// -----------------------------------------------------------------------------------------
// COMPONENTES DAS 14 CATEGORIAS DE PREPARAÇÃO
// -----------------------------------------------------------------------------------------

@Composable
private fun MotorCategoryContent(build: TuningBuild, onChange: (TuningBuild) -> Unit) {
  Column {
    Text("Pistões", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
    PistonType.entries.forEach { opt ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onChange(build.copy(pistons = opt)) }
          .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            if (build.pistons == opt) Icons.Filled.Check else Icons.Filled.DirectionsCar,
            contentDescription = null,
            tint = if (build.pistons == opt) SimPurpleLight else DynoTextMuted,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(opt.displayName, fontSize = 12.sp, color = if (build.pistons == opt) DynoTextPrimary else DynoTextSecondary)
        }
        Text("até ${String.format(Locale.US, "%.0f", opt.maxHpLimit)} cv", fontSize = 11.sp, color = DynoTextMuted)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = DynoDivider)
    Spacer(modifier = Modifier.height(10.dp))

    Text("Bielas", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
    RodsType.entries.forEach { opt ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onChange(build.copy(rods = opt)) }
          .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            if (build.rods == opt) Icons.Filled.Check else Icons.Filled.DirectionsCar,
            contentDescription = null,
            tint = if (build.rods == opt) SimPurpleLight else DynoTextMuted,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(opt.displayName, fontSize = 12.sp, color = if (build.rods == opt) DynoTextPrimary else DynoTextSecondary)
        }
        Text("até ${String.format(Locale.US, "%.0f", opt.maxHpLimit)} cv", fontSize = 11.sp, color = DynoTextMuted)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = DynoDivider)
    Spacer(modifier = Modifier.height(10.dp))

    Text("Junta de Cabeçote", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
    HeadGasketType.entries.forEach { opt ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onChange(build.copy(headGasket = opt)) }
          .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            if (build.headGasket == opt) Icons.Filled.Check else Icons.Filled.DirectionsCar,
            contentDescription = null,
            tint = if (build.headGasket == opt) SimPurpleLight else DynoTextMuted,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(opt.displayName, fontSize = 12.sp, color = if (build.headGasket == opt) DynoTextPrimary else DynoTextSecondary)
        }
        Text("até ${opt.maxBoostBar} bar", fontSize = 11.sp, color = DynoTextMuted)
      }
    }
  }
}

@Composable
private fun AlimentacaoCategoryContent(
  build: TuningBuild,
  result: TuningCalculationResult,
  onChange: (TuningBuild) -> Unit
) {
  Column {
    Text("Bicos Injetores (Alimentação)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DynoTextPrimary)
    Spacer(modifier = Modifier.height(6.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Vazão Nominal: ${String.format(Locale.US, "%.0f", build.injectorFlowLbHr)} lb/h (${String.format(Locale.US, "%.0f", result.correctedInjectorFlowCcMin)} cc/min)", fontSize = 12.sp, color = SimPurpleLight)
      Text("Capacidade: ${String.format(Locale.US, "%.0f", result.maxSupportedPowerByInjectorsCv)} cv", fontSize = 12.sp, color = DynoPowerCyan, fontWeight = FontWeight.Bold)
    }

    Slider(
      value = build.injectorFlowLbHr,
      onValueChange = { onChange(build.copy(injectorFlowLbHr = it)) },
      valueRange = 18f..160f,
      steps = 14,
      colors = SliderDefaults.colors(thumbColor = SimPurplePrimary, activeTrackColor = SimPurpleLight)
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Pressão de Linha (Rail)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Pressão de Linha (Rail): ${String.format(Locale.US, "%.1f", build.injectorOperatingPressureBar)} bar", fontSize = 12.sp, color = DynoTextSecondary)
      Text("Ref: ${build.injectorBasePressureBar} bar", fontSize = 11.sp, color = DynoTextMuted)
    }
    Slider(
      value = build.injectorOperatingPressureBar,
      onValueChange = { onChange(build.copy(injectorOperatingPressureBar = it)) },
      valueRange = 2.5f..5.5f,
      steps = 6,
      colors = SliderDefaults.colors(thumbColor = SimPurplePrimary, activeTrackColor = SimPurpleLight)
    )

    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = DynoDivider)
    Spacer(modifier = Modifier.height(10.dp))

    // Bomba de Combustível
    Text("Bomba de Combustível", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DynoTextPrimary)
    Spacer(modifier = Modifier.height(4.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Vazão: ${String.format(Locale.US, "%.0f", build.fuelPumpFlowLph * build.fuelPumpCount)} L/h", fontSize = 12.sp, color = SimPurpleLight)
      Text("Necessário: ${String.format(Locale.US, "%.0f", result.requiredFuelPumpFlowLph)} L/h", fontSize = 12.sp, color = DynoTextMuted)
    }

    val pumpOptions = listOf(100f to "Original (100 L/h)", 150f to "GTI/Flex (150 L/h)", 255f to "Walbro 255 L/h", 340f to "AEM 340 L/h", 450f to "Walbro 450 L/h")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      pumpOptions.forEach { (flow, name) ->
        FilterChip(
          selected = build.fuelPumpFlowLph == flow,
          onClick = { onChange(build.copy(fuelPumpFlowLph = flow)) },
          label = { Text(name.substringBefore(" "), fontSize = 11.sp) },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SimPurplePrimary, selectedLabelColor = Color.White)
        )
      }
    }
  }
}

@Composable
private fun AspiracaoCategoryContent(build: TuningBuild, onChange: (TuningBuild) -> Unit) {
  Column {
    Text("Tipo de Aspiração", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
    AspirationType.entries.forEach { asp ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            val isGoingTurbo = asp != AspirationType.ASPIRADO && build.aspiration == AspirationType.ASPIRADO
            var updated = build.copy(aspiration = asp)
            // Auto-adiciona componentes básicos ao mudar para turbo se estavam em original (Seção 3)
            if (isGoingTurbo) {
              updated = updated.copy(
                intercooler = IntercoolerType.PEQUENO_FRONTAL,
                injectorFlowLbHr = max(42f, updated.injectorFlowLbHr),
                fuelPumpFlowLph = max(255f, updated.fuelPumpFlowLph),
                ecu = if (updated.ecu == EcuType.ORIGINAL) EcuType.PROGRAMAVEL_BASICA else updated.ecu
              )
            }
            onChange(updated)
          }
          .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            if (build.aspiration == asp) Icons.Filled.Check else Icons.Filled.DirectionsCar,
            contentDescription = null,
            tint = if (build.aspiration == asp) SimPurpleLight else DynoTextMuted,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(asp.displayName, fontSize = 12.sp, color = if (build.aspiration == asp) DynoTextPrimary else DynoTextSecondary)
        }
      }
    }

    if (build.aspiration != AspirationType.ASPIRADO) {
      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = DynoDivider)
      Spacer(modifier = Modifier.height(10.dp))

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Pressão Máxima de Turbo: ${String.format(Locale.US, "%.2f", build.turboBoostBar)} bar", fontSize = 13.sp, color = SimPurpleLight, fontWeight = FontWeight.Bold)
      }
      Slider(
        value = build.turboBoostBar,
        onValueChange = { onChange(build.copy(turboBoostBar = it)) },
        valueRange = 0.2f..2.5f,
        steps = 22,
        colors = SliderDefaults.colors(thumbColor = SimPurplePrimary, activeTrackColor = SimPurpleLight)
      )

      Spacer(modifier = Modifier.height(10.dp))
      Text("Intercooler", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
      IntercoolerType.entries.forEach { ic ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(build.copy(intercooler = ic)) }
            .padding(vertical = 5.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              if (build.intercooler == ic) Icons.Filled.Check else Icons.Filled.DirectionsCar,
              contentDescription = null,
              tint = if (build.intercooler == ic) SimPurpleLight else DynoTextMuted,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(ic.displayName, fontSize = 12.sp, color = if (build.intercooler == ic) DynoTextPrimary else DynoTextSecondary)
          }
          Text("queda -${ic.tempDropC.toInt()}°C", fontSize = 11.sp, color = DynoTextMuted)
        }
      }
    }
  }
}

@Composable
private fun CombustivelCategoryContent(build: TuningBuild, onChange: (TuningBuild) -> Unit) {
  Column {
    Text("Tipo de Combustível", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
    FuelTypeOption.entries.forEach { f ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onChange(build.copy(fuelType = f)) }
          .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            if (build.fuelType == f) Icons.Filled.Check else Icons.Filled.LocalGasStation,
            contentDescription = null,
            tint = if (build.fuelType == f) SimPurpleLight else DynoTextMuted,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(f.displayName, fontSize = 12.sp, color = if (build.fuelType == f) DynoTextPrimary else DynoTextSecondary)
        }
        Text("Oct: ${f.octaneRon.toInt()} RON", fontSize = 11.sp, color = DynoTextMuted)
      }
    }
  }
}

@Composable
private fun InjecaoCategoryContent(build: TuningBuild, onChange: (TuningBuild) -> Unit) {
  Column {
    Text("Gerenciamento de Injeção (ECU)", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
    EcuType.entries.forEach { ecu ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onChange(build.copy(ecu = ecu)) }
          .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            if (build.ecu == ecu) Icons.Filled.Check else Icons.Filled.DirectionsCar,
            contentDescription = null,
            tint = if (build.ecu == ecu) SimPurpleLight else DynoTextMuted,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(ecu.displayName, fontSize = 12.sp, color = if (build.ecu == ecu) DynoTextPrimary else DynoTextSecondary)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = DynoDivider)
    Spacer(modifier = Modifier.height(10.dp))

    Text("Tipo de Acerto / Mapa", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
    TuneMapType.entries.forEach { tm ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onChange(build.copy(tuneMap = tm)) }
          .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            if (build.tuneMap == tm) Icons.Filled.Check else Icons.Filled.DirectionsCar,
            contentDescription = null,
            tint = if (build.tuneMap == tm) SimPurpleLight else DynoTextMuted,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(tm.displayName, fontSize = 12.sp, color = if (build.tuneMap == tm) DynoTextPrimary else DynoTextSecondary)
        }
      }
    }
  }
}

@Composable
private fun IgnicaoCategoryContent(build: TuningBuild, onChange: (TuningBuild) -> Unit) {
  Column {
    Text("Bobina de Ignição", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
    IgnitionCoilType.entries.forEach { opt ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onChange(build.copy(ignitionCoil = opt)) }
          .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(opt.displayName, fontSize = 12.sp, color = if (build.ignitionCoil == opt) SimPurpleLight else DynoTextSecondary)
        Text("até ${opt.maxBoostBar} bar", fontSize = 11.sp, color = DynoTextMuted)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(color = DynoDivider)
    Spacer(modifier = Modifier.height(8.dp))

    Text("Velas de Ignição", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
    SparkPlugType.entries.forEach { opt ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onChange(build.copy(sparkPlugs = opt)) }
          .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(opt.displayName, fontSize = 12.sp, color = if (build.sparkPlugs == opt) SimPurpleLight else DynoTextSecondary)
        Text(opt.heatRange, fontSize = 11.sp, color = DynoTextMuted)
      }
    }
  }
}

@Composable
private fun AdmissaoCategoryContent(build: TuningBuild, onChange: (TuningBuild) -> Unit) {
  Column {
    Text("Sistema de Admissão & Filtro", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
    IntakeType.entries.forEach { opt ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onChange(build.copy(intake = opt)) }
          .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(opt.displayName, fontSize = 12.sp, color = if (build.intake == opt) SimPurpleLight else DynoTextSecondary)
        Text("+${opt.powerGainHp.toInt()} cv", fontSize = 11.sp, color = DynoSuccessGreen)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(color = DynoDivider)
    Spacer(modifier = Modifier.height(8.dp))

    Text("Corpo de Borboleta (TBI)", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
    ThrottleBodyType.entries.forEach { opt ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onChange(build.copy(throttleBody = opt)) }
          .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(opt.displayName, fontSize = 12.sp, color = if (build.throttleBody == opt) SimPurpleLight else DynoTextSecondary)
        Text("vazão ${opt.maxHpFlow.toInt()} cv", fontSize = 11.sp, color = DynoTextMuted)
      }
    }
  }
}

@Composable
private fun EscapeCategoryContent(build: TuningBuild, onChange: (TuningBuild) -> Unit) {
  Column {
    Text("Coletor de Escape", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
    ExhaustHeaderType.entries.forEach { opt ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onChange(build.copy(exhaustHeader = opt)) }
          .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(opt.displayName, fontSize = 12.sp, color = if (build.exhaustHeader == opt) SimPurpleLight else DynoTextSecondary)
        Text("+${opt.highRpmPowerGain.toInt()} cv alta", fontSize = 11.sp, color = DynoSuccessGreen)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(color = DynoDivider)
    Spacer(modifier = Modifier.height(8.dp))

    Text("Diâmetro do Escapamento", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
    ExhaustSystemType.entries.forEach { opt ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onChange(build.copy(exhaustSystem = opt)) }
          .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(opt.displayName, fontSize = 12.sp, color = if (build.exhaustSystem == opt) SimPurpleLight else DynoTextSecondary)
        Text("limite ${opt.maxHpFlow.toInt()} cv", fontSize = 11.sp, color = DynoTextMuted)
      }
    }
  }
}

@Composable
private fun CabecoteCategoryContent(build: TuningBuild, onChange: (TuningBuild) -> Unit) {
  Column {
    Text("Trabalho de Cabeçote", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
    CylinderHeadType.entries.forEach { opt ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onChange(build.copy(cylinderHead = opt)) }
          .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(opt.displayName, fontSize = 12.sp, color = if (build.cylinderHead == opt) SimPurpleLight else DynoTextSecondary)
        Text("+${opt.flowGainPercent.toInt()}% fluxo", fontSize = 11.sp, color = DynoSuccessGreen)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Aumento de Taxa de Compressão: +${String.format(Locale.US, "%.1f", build.extraCompressionRatio)}:1", fontSize = 12.sp, color = SimPurpleLight)
    }
    Slider(
      value = build.extraCompressionRatio,
      onValueChange = { onChange(build.copy(extraCompressionRatio = it)) },
      valueRange = 0.0f..3.0f,
      steps = 6,
      colors = SliderDefaults.colors(thumbColor = SimPurplePrimary, activeTrackColor = SimPurpleLight)
    )
  }
}

@Composable
private fun ComandoCategoryContent(build: TuningBuild, onChange: (TuningBuild) -> Unit) {
  Column {
    Text("Perfil do Comando de Válvulas", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
    CamshaftProfile.entries.forEach { opt ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onChange(build.copy(camshaft = opt)) }
          .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(opt.displayName, fontSize = 12.sp, color = if (build.camshaft == opt) SimPurpleLight else DynoTextSecondary)
          Text("Graduação: ${opt.durationDegrees}°", fontSize = 10.sp, color = DynoTextMuted)
        }
        Text("+${opt.highRpmPowerGain.toInt()} cv", fontSize = 11.sp, color = DynoSuccessGreen)
      }
    }
  }
}

@Composable
private fun TransmissaoCategoryContent(build: TuningBuild, onChange: (TuningBuild) -> Unit) {
  Column {
    Text("Embreagem", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
    ClutchType.entries.forEach { opt ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onChange(build.copy(clutch = opt)) }
          .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(opt.displayName, fontSize = 12.sp, color = if (build.clutch == opt) SimPurpleLight else DynoTextSecondary)
        Text("suporta ${opt.maxTorqueKgfm.toInt()} kgfm", fontSize = 11.sp, color = DynoTextMuted)
      }
    }
  }
}

@Composable
private fun PneusCategoryContent(build: TuningBuild, onChange: (TuningBuild) -> Unit) {
  Column {
    Text("Composto do Pneu & Aderência", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
    TireCompound.entries.forEach { opt ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onChange(build.copy(tireCompound = opt)) }
          .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(opt.displayName, fontSize = 12.sp, color = if (build.tireCompound == opt) SimPurpleLight else DynoTextSecondary)
        Text("μ ${opt.frictionMu}", fontSize = 11.sp, color = SimPurpleLight)
      }
    }
  }
}

@Composable
private fun PesoCategoryContent(build: TuningBuild, onChange: (TuningBuild) -> Unit) {
  Column {
    Text("Estágio de Alívio de Peso", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
    WeightReductionStage.entries.forEach { opt ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onChange(build.copy(weightReduction = opt)) }
          .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(opt.displayName, fontSize = 12.sp, color = if (build.weightReduction == opt) SimPurpleLight else DynoTextSecondary)
        Text("-${opt.weightRemovedKg.toInt()} kg", fontSize = 11.sp, color = DynoSuccessGreen)
      }
    }
  }
}

@Composable
private fun AeroCategoryContent(build: TuningBuild, onChange: (TuningBuild) -> Unit) {
  Column {
    Text("Pacote Aerodinâmico", fontSize = 13.sp, color = DynoTextSecondary, fontWeight = FontWeight.SemiBold)
    AeroPackage.entries.forEach { opt ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onChange(build.copy(aero = opt)) }
          .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(opt.displayName, fontSize = 12.sp, color = if (build.aero == opt) SimPurpleLight else DynoTextSecondary)
      }
    }
  }
}

@Composable
private fun GaugeBarItem(
  label: String,
  currentValue: Float,
  warningThreshold: Float,
  dangerThreshold: Float,
  unit: String,
  isInverted: Boolean = false
) {
  val barColor = if (!isInverted) {
    when {
      currentValue >= dangerThreshold -> DynoErrorRed
      currentValue >= warningThreshold -> DynoWarningYellow
      else -> DynoSuccessGreen
    }
  } else {
    when {
      currentValue < warningThreshold -> DynoErrorRed
      currentValue < dangerThreshold -> DynoWarningYellow
      else -> DynoSuccessGreen
    }
  }

  Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(label, fontSize = 11.sp, color = DynoTextSecondary)
      Text("${String.format(Locale.US, "%.1f", currentValue)}$unit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = barColor)
    }
    Spacer(modifier = Modifier.height(2.dp))
    LinearProgressIndicator(
      progress = { (currentValue / 120f).coerceIn(0f, 1f) },
      modifier = Modifier.fillMaxWidth().height(5.dp),
      color = barColor,
      trackColor = DynoSurfaceElevated
    )
  }
}

@Composable
private fun ScoreBarItem(label: String, score: Int, color: Color) {
  Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(label, fontSize = 11.sp, color = DynoTextSecondary)
      Text("$score / 100", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
    Spacer(modifier = Modifier.height(2.dp))
    LinearProgressIndicator(
      progress = { (score / 100f).coerceIn(0f, 1f) },
      modifier = Modifier.fillMaxWidth().height(6.dp),
      color = color,
      trackColor = DynoSurfaceElevated
    )
  }
}
