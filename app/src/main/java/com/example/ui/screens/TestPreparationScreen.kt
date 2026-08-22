package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VehicleDatabase
import com.example.model.VehicleCalculations
import com.example.model.VehicleProfile
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestPreparationScreen(
  vehicle: VehicleProfile,
  onProceedToSensorScreen: () -> Unit,
  onEditVehicle: () -> Unit,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val prefs = remember(context) {
    context.getSharedPreferences("dyno_lite_prefs", Context.MODE_PRIVATE)
  }

  val defaultSavedDriverWeight = remember {
    prefs.getFloat("default_driver_weight", 75f)
  }
  val savedStartSpeed = remember {
    prefs.getFloat("start_speed_trigger_kmh", 40f)
  }

  // Wizard state (5 steps)
  var currentStep by remember { mutableIntStateOf(1) }
  val totalSteps = 5

  // Step 1: Occupants State
  var occupantCount by remember { mutableIntStateOf(1) }
  var driverWeightText by remember {
    mutableStateOf(
      if (vehicle.driverWeightKg > 0) vehicle.driverWeightKg.toInt().toString()
      else defaultSavedDriverWeight.toInt().toString()
    )
  }
  val passengerWeights = remember {
    mutableStateListOf(
      if (vehicle.passengerWeightKg > 0) vehicle.passengerWeightKg.toInt().toString() else "70",
      "70",
      "70",
      "70"
    )
  }

  // Step 2: Temporary Cargo State
  var tempCargoOption by remember { mutableStateOf("NONE") } // NONE, LUGGAGE, TOOLS, CUSTOM
  var tempCargoWeightText by remember { mutableStateOf("0") }

  // Step 3: Selected Gear State
  var selectedGear by remember { mutableIntStateOf(3) } // 2, 3, 4

  // Step 4: Start Speed State
  var selectedStartSpeed by remember { mutableFloatStateOf(savedStartSpeed) } // 40f, 50f, 60f

  // Computed weights
  val driverWeightVal = driverWeightText.toFloatOrNull() ?: 0f
  val passengerTotalVal = if (occupantCount > 1) {
    (0 until (occupantCount - 1)).sumOf { idx ->
      passengerWeights.getOrNull(idx)?.toFloatOrNull()?.toDouble() ?: 0.0
    }.toFloat()
  } else 0f

  val totalOccupantsWeight = driverWeightVal + passengerTotalVal

  val tempCargoVal = when (tempCargoOption) {
    "NONE" -> 0f
    "LUGGAGE" -> 30f
    "TOOLS" -> 20f
    "CUSTOM" -> tempCargoWeightText.toFloatOrNull() ?: 0f
    else -> 0f
  }

  val permanentAdditions = vehicle.audioWeightKg + vehicle.gnvWeightKg + vehicle.otherWeightKg
  val totalPassWeight = vehicle.curbWeightKg + totalOccupantsWeight + tempCargoVal + permanentAdditions - vehicle.removedWeightKg

  val confidence = VehicleCalculations.evaluateWeightConfidence(
    useMeasuredWeight = vehicle.useMeasuredWeight,
    audioPreset = vehicle.audioPreset,
    hasGnv = vehicle.gnvWeightKg > 0f,
    hasCargo = tempCargoVal > 30f
  )

  val tireCalc = VehicleCalculations.calculateTireDimensions(
    widthMm = vehicle.tireWidthMm,
    aspectRatio = vehicle.tireAspectRatio,
    rimInches = vehicle.wheelDiameterInches
  )

  val transmissionName = when {
    vehicle.transmissionId != null -> VehicleDatabase.getTransmission(vehicle.transmissionId)?.displayName ?: "Original"
    !vehicle.customTransmissionName.isNullOrBlank() -> vehicle.customTransmissionName
    else -> "Original"
  }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("test_preparation_screen"),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "PREPARAÇÃO DA PASSAGEM",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
        },
        navigationIcon = {
          IconButton(onClick = {
            if (currentStep > 1) currentStep-- else onNavigateBack()
          }) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Voltar"
            )
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
          .widthIn(max = 500.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Step Indicator Progress Bar
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "ETAPA $currentStep DE $totalSteps",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
              ),
              color = MaterialTheme.colorScheme.primary
            )
            val stepLabel = when (currentStep) {
              1 -> "Ocupantes"
              2 -> "Carga Temporária"
              3 -> "Marcha do Teste"
              4 -> "Velocidade de Início"
              5 -> "Resumo & Confirmação"
              else -> ""
            }
            Text(
              text = stepLabel,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            for (step in 1..totalSteps) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(4.dp)
                  .background(
                    color = if (step <= currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(2.dp)
                  )
              )
            }
          }
        }

        HorizontalDivider(
          thickness = 0.8.dp,
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        // Step Content (Scrollable)
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentAlignment = Alignment.TopCenter
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            when (currentStep) {
              // -------------------------------------------------------------
              // ETAPA 1: OCUPANTES DESTA PASSAGEM
              // -------------------------------------------------------------
              1 -> {
                Text(
                  text = "Ocupantes nesta passagem",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Informe a quantidade de pessoas e os pesos respectivos para garantir a máxima precisão do cálculo de inércia.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Counter: Quantidade de pessoas
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                      Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                      Column {
                        Text(text = "Quantidade de pessoas", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text(text = "$occupantCount ${if (occupantCount == 1) "ocupante" else "ocupantes"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                    }

                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      IconButton(
                        onClick = { if (occupantCount > 1) occupantCount-- },
                        enabled = occupantCount > 1,
                        modifier = Modifier.size(38.dp)
                      ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Diminuir")
                      }

                      Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Text(text = "$occupantCount", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                      }

                      IconButton(
                        onClick = { if (occupantCount < 5) occupantCount++ },
                        enabled = occupantCount < 5,
                        modifier = Modifier.size(38.dp)
                      ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Aumentar")
                      }
                    }
                  }
                }

                // Dynamic Occupant Fields
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                  // Motorista
                  OutlinedTextField(
                    value = driverWeightText,
                    onValueChange = { driverWeightText = it.filter { c -> c.isDigit() } },
                    label = { Text("Peso do motorista (kg)") },
                    supportingText = { Text("Padrão salvo: ${defaultSavedDriverWeight.toInt()} kg") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("input_prep_driver_weight")
                  )

                  // Passageiros
                  for (i in 1 until occupantCount) {
                    val passIdx = i - 1
                    val currentVal = passengerWeights.getOrElse(passIdx) { "70" }
                    OutlinedTextField(
                      value = currentVal,
                      onValueChange = { newText ->
                        if (passIdx < passengerWeights.size) {
                          passengerWeights[passIdx] = newText.filter { c -> c.isDigit() }
                        }
                      },
                      label = { Text("Passageiro $i (kg)") },
                      singleLine = true,
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      modifier = Modifier.fillMaxWidth().testTag("input_prep_passenger_$i")
                    )
                  }
                }

                // Total Occupants Card
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(14.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(text = "Soma dos ocupantes:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                      text = String.format(Locale.US, "%.0f kg", totalOccupantsWeight),
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )
                  }
                }
              }

              // -------------------------------------------------------------
              // ETAPA 2: CARGA TEMPORÁRIA
              // -------------------------------------------------------------
              2 -> {
                Text(
                  text = "Carga temporária nesta passagem",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Existe carga adicional nesta passagem? (Não serão somados novamente pesos permanentes já cadastrados no veículo).",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  // Option 1: Não
                  SelectableOptionCard(
                    title = "Não (sem carga adicional)",
                    subtitle = "Veículo apenas com ocupantes e itens permanentes",
                    isSelected = tempCargoOption == "NONE",
                    onClick = { tempCargoOption = "NONE" }
                  )

                  // Option 2: Bagagem
                  SelectableOptionCard(
                    title = "Bagagem / Malas",
                    subtitle = "Aproximadamente +30 kg no porta-malas",
                    isSelected = tempCargoOption == "LUGGAGE",
                    onClick = { tempCargoOption = "LUGGAGE" }
                  )

                  // Option 3: Ferramentas
                  SelectableOptionCard(
                    title = "Caixa de Ferramentas / Equipamentos",
                    subtitle = "Aproximadamente +20 kg",
                    isSelected = tempCargoOption == "TOOLS",
                    onClick = { tempCargoOption = "TOOLS" }
                  )

                  // Option 4: Outro peso personalizado
                  SelectableOptionCard(
                    title = "Outro peso específico",
                    subtitle = "Informar valor em kg",
                    isSelected = tempCargoOption == "CUSTOM",
                    onClick = { tempCargoOption = "CUSTOM" }
                  )

                  if (tempCargoOption == "CUSTOM") {
                    OutlinedTextField(
                      value = tempCargoWeightText,
                      onValueChange = { tempCargoWeightText = it.filter { c -> c.isDigit() } },
                      label = { Text("Peso da carga adicional (kg)") },
                      singleLine = true,
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      modifier = Modifier.fillMaxWidth().testTag("input_prep_temp_cargo")
                    )
                  }
                }
              }

              // -------------------------------------------------------------
              // ETAPA 3: SELEÇÃO DE MARCHA
              // -------------------------------------------------------------
              3 -> {
                Text(
                  text = "Marcha do teste",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Selecione a marcha que permanecerá engatada durante todo o teste de aceleração linear.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  listOf(2, 3, 4).forEach { gear ->
                    Button(
                      onClick = { selectedGear = gear },
                      modifier = Modifier.weight(1f).height(52.dp).testTag("btn_gear_$gear"),
                      shape = RoundedCornerShape(12.dp),
                      colors = if (selectedGear == gear) ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                      ) else ButtonDefaults.outlinedButtonColors(),
                      border = if (selectedGear != gear) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
                    ) {
                      Text(
                        text = "${gear}ª MARCHA",
                        style = MaterialTheme.typography.labelMedium.copy(
                          fontWeight = FontWeight.Bold,
                          fontSize = 13.sp
                        )
                      )
                    }
                  }
                }

                // Guidance Card
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                  Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Icon(imageVector = Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                      Text(text = "Recomendações técnicas:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }

                    Text(
                      text = "• A 3ª marcha é a mais recomendada na maioria dos veículos manuais brasileiros (relação próxima a 1.3:1 a 1.4:1).\n" +
                        "• Se câmbio automático: utilize obrigatoriamente o modo manual/sequencial para travar a marcha e evitar kickdown involuntário.\n" +
                        "• Não recomendado para câmbios CVT contínuos que alteram a relação durante a aceleração.",
                      style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }

              // -------------------------------------------------------------
              // ETAPA 4: VELOCIDADE DE INÍCIO
              // -------------------------------------------------------------
              4 -> {
                Text(
                  text = "Velocidade de início do teste",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "O teste começará automaticamente pelo GPS real ao atingir a velocidade escolhida.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  listOf(40f, 50f, 60f).forEach { speed ->
                    Button(
                      onClick = {
                        selectedStartSpeed = speed
                        prefs.edit().putFloat("start_speed_trigger_kmh", speed).apply()
                      },
                      modifier = Modifier.weight(1f).height(52.dp).testTag("btn_speed_${speed.toInt()}"),
                      shape = RoundedCornerShape(12.dp),
                      colors = if (selectedStartSpeed == speed) ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                      ) else ButtonDefaults.outlinedButtonColors(),
                      border = if (selectedStartSpeed != speed) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
                    ) {
                      Text(
                        text = "${speed.toInt()} KM/H",
                        style = MaterialTheme.typography.labelMedium.copy(
                          fontWeight = FontWeight.Bold,
                          fontSize = 13.sp
                        )
                      )
                    }
                  }
                }

                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                  Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Icon(imageVector = Icons.Outlined.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                      Text(text = "Como funciona o gatilho automático:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }

                    Text(
                      text = "Você começa com o carro parado e inicia o aplicativo. Acelere gradualmente na marcha escolhida. Ao cruzar exatamente ${selectedStartSpeed.toInt()} km/h, o Dyno Lite dispara a gravação inercial de alta resolução até você atingir o corte ou pisar na embreagem.",
                      style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }

              // -------------------------------------------------------------
              // ETAPA 5: PESO TOTAL & RESUMO
              // -------------------------------------------------------------
              5 -> {
                Text(
                  text = "Resumo da passagem",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )

                // Resumo do Veículo & Passagem
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(18.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                  Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                    Text(
                      text = "${vehicle.manufacturer} ${vehicle.model} (${vehicle.year})",
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    SummaryLine(label = "Peso base do veículo:", value = String.format(Locale.US, "%.0f kg", vehicle.curbWeightKg))
                    if (permanentAdditions > 0f) {
                      SummaryLine(label = "Som / GNV / Permanente:", value = String.format(Locale.US, "+%.0f kg", permanentAdditions))
                    }
                    SummaryLine(label = "Motorista:", value = String.format(Locale.US, "+%.0f kg", driverWeightVal))
                    if (passengerTotalVal > 0f) {
                      SummaryLine(label = "Passageiros (${occupantCount - 1}):", value = String.format(Locale.US, "+%.0f kg", passengerTotalVal))
                    }
                    if (tempCargoVal > 0f) {
                      SummaryLine(label = "Carga temporária:", value = String.format(Locale.US, "+%.0f kg", tempCargoVal))
                    }
                    if (vehicle.removedWeightKg > 0f) {
                      SummaryLine(label = "Itens removidos:", value = String.format(Locale.US, "-%.0f kg", vehicle.removedWeightKg))
                    }

                    HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(text = "PESO TOTAL DA PASSAGEM:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                      Text(
                        text = String.format(Locale.US, "%.0f kg", totalPassWeight),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                      )
                    }

                    SummaryLine(label = "Confiança do peso:", value = confidence.label)
                    SummaryLine(label = "Marcha selecionada:", value = "${selectedGear}ª marcha")
                    SummaryLine(label = "Velocidade de início:", value = "${selectedStartSpeed.toInt()} km/h")
                    SummaryLine(label = "Pneu:", value = tireCalc.formattedMeasure)
                    SummaryLine(label = "Câmbio:", value = transmissionName)
                  }
                }

                // Safety Alert Card
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                  Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                    Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      Text(
                        text = "AVISO DE SEGURANÇA",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        color = MaterialTheme.colorScheme.error
                      )
                      Text(
                        text = "Inicie o procedimento antes de movimentar o veículo. Realize o teste em local seguro e fechado.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurface
                      )
                    }
                  }
                }
              }
            }
          }
        }

        HorizontalDivider(
          thickness = 0.8.dp,
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        // Bottom Navigation Buttons
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (currentStep > 1) {
            OutlinedButton(
              onClick = { currentStep-- },
              modifier = Modifier.weight(1f).height(48.dp).testTag("btn_prep_back"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("VOLTAR")
            }
          } else {
            OutlinedButton(
              onClick = onNavigateBack,
              modifier = Modifier.weight(1f).height(48.dp).testTag("btn_prep_cancel"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("CANCELAR")
            }
          }

          if (currentStep < totalSteps) {
            Button(
              onClick = { currentStep++ },
              modifier = Modifier.weight(1.3f).height(48.dp).testTag("btn_prep_next"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("AVANÇAR")
            }
          } else {
            Button(
              onClick = onProceedToSensorScreen,
              modifier = Modifier.weight(1.5f).height(50.dp).testTag("btn_preparar_teste_proceed"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              )
            ) {
              Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("INICIAR COM O CARRO PARADO", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SummaryLine(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
  }
}

@Composable
private fun SelectableOptionCard(
  title: String,
  subtitle: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ),
    border = BorderStroke(
      1.dp,
      if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Surface(
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(20.dp)
      ) {
        if (isSelected) {
          Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
          }
        }
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}
