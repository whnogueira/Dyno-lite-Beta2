package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VehicleDatabase
import com.example.model.AudioWeightPreset
import com.example.model.VehicleCalculations
import com.example.model.VehicleProfile
import com.example.model.WeightConfidence
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleWizardScreen(
  existingVehicle: VehicleProfile?,
  onSaveVehicle: (VehicleProfile) -> Unit,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier
) {
  var currentStep by remember { mutableIntStateOf(1) }
  val totalSteps = 6

  // Form State
  var vehicleId by remember { mutableStateOf(existingVehicle?.id ?: UUID.randomUUID().toString()) }
  var isCustom by remember { mutableStateOf(existingVehicle?.isCustom ?: false) }

  // Step 1: Identification
  var manufacturer by remember { mutableStateOf(existingVehicle?.manufacturer ?: "") }
  var model by remember { mutableStateOf(existingVehicle?.model ?: "") }
  var yearText by remember { mutableStateOf(existingVehicle?.year?.toString() ?: "2018") }
  var version by remember { mutableStateOf(existingVehicle?.version ?: "") }
  var engine by remember { mutableStateOf(existingVehicle?.engine ?: "") }
  var searchQuery by remember { mutableStateOf("") }
  var isSearchMode by remember { mutableStateOf(existingVehicle == null || !existingVehicle.isCustom) }

  // Step 2: Factory Data
  var curbWeightText by remember { mutableStateOf(existingVehicle?.curbWeightKg?.toInt()?.toString() ?: "1000") }
  var factoryPowerText by remember { mutableStateOf(existingVehicle?.factoryPowerCv?.toInt()?.toString() ?: "") }
  var factoryTorqueText by remember { mutableStateOf(existingVehicle?.factoryTorqueKgf?.toString() ?: "") }
  var displacement by remember { mutableStateOf(existingVehicle?.displacement ?: "") }
  var drivetrain by remember { mutableStateOf(existingVehicle?.drivetrain ?: "Dianteira") }

  // Step 3: Transmission
  var transmissionOption by remember {
    mutableStateOf(
      when {
        existingVehicle?.transmissionId != null -> "KNOWN"
        existingVehicle?.customTransmissionName != null -> "CUSTOM"
        else -> "ORIGINAL"
      }
    )
  }
  var selectedTransmissionId by remember { mutableStateOf(existingVehicle?.transmissionId ?: "gm_f17_ccw") }
  var customTransmissionName by remember { mutableStateOf(existingVehicle?.customTransmissionName ?: "") }
  var showTransmissionTechnicalDetails by remember { mutableStateOf(false) }

  // Step 4: Tires
  var tireWidthText by remember { mutableStateOf(existingVehicle?.tireWidthMm?.toString() ?: "185") }
  var tireAspectText by remember { mutableStateOf(existingVehicle?.tireAspectRatio?.toString() ?: "70") }
  var wheelDiameterText by remember { mutableStateOf(existingVehicle?.wheelDiameterInches?.toString() ?: "14") }

  // Step 5: Weights
  var driverWeightText by remember { mutableStateOf(existingVehicle?.driverWeightKg?.toInt()?.toString() ?: "0") }
  var passengerWeightText by remember { mutableStateOf(existingVehicle?.passengerWeightKg?.toInt()?.toString() ?: "0") }
  var cargoWeightText by remember { mutableStateOf(existingVehicle?.cargoWeightKg?.toInt()?.toString() ?: "0") }
  var audioPreset by remember { mutableStateOf(existingVehicle?.audioPreset ?: AudioWeightPreset.NONE) }
  var customAudioWeightText by remember { mutableStateOf(existingVehicle?.audioWeightKg?.toInt()?.toString() ?: "50") }
  var hasGnv by remember { mutableStateOf((existingVehicle?.gnvWeightKg ?: 0f) > 0f) }
  var gnvWeightText by remember { mutableStateOf(existingVehicle?.gnvWeightKg?.toInt()?.toString() ?: "70") }
  var hasRemovedSpare by remember { mutableStateOf((existingVehicle?.removedWeightKg ?: 0f) > 0f) }
  var removedWeightText by remember { mutableStateOf(existingVehicle?.removedWeightKg?.toInt()?.toString() ?: "15") }
  var otherWeightText by remember { mutableStateOf(existingVehicle?.otherWeightKg?.toInt()?.toString() ?: "0") }
  var useMeasuredWeight by remember { mutableStateOf(existingVehicle?.useMeasuredWeight ?: false) }
  var measuredWeightText by remember { mutableStateOf(existingVehicle?.measuredTotalWeightKg?.toInt()?.toString() ?: "") }

  // Computations
  val widthInt = tireWidthText.toIntOrNull() ?: 185
  val aspectInt = tireAspectText.toIntOrNull() ?: 70
  val rimInt = wheelDiameterText.toIntOrNull() ?: 14
  val tireCalc = remember(widthInt, aspectInt, rimInt) {
    VehicleCalculations.calculateTireDimensions(widthInt, aspectInt, rimInt)
  }

  val curbWeightVal = curbWeightText.toFloatOrNull() ?: 1000f
  val driverWeightVal = driverWeightText.toFloatOrNull() ?: 0f
  val passengerWeightVal = passengerWeightText.toFloatOrNull() ?: 0f
  val cargoWeightVal = cargoWeightText.toFloatOrNull() ?: 0f
  val audioWeightVal = when (audioPreset) {
    AudioWeightPreset.NONE -> 0f
    AudioWeightPreset.LIGHT -> 18f
    AudioWeightPreset.MEDIUM -> 43f
    AudioWeightPreset.HEAVY -> 90f
    AudioWeightPreset.PAREDAO, AudioWeightPreset.CUSTOM -> customAudioWeightText.toFloatOrNull() ?: 50f
  }
  val gnvWeightVal = if (hasGnv) gnvWeightText.toFloatOrNull() ?: 70f else 0f
  val removedWeightVal = if (hasRemovedSpare) removedWeightText.toFloatOrNull() ?: 15f else 0f
  val otherWeightVal = otherWeightText.toFloatOrNull() ?: 0f
  val measuredWeightVal = if (useMeasuredWeight) measuredWeightText.toFloatOrNull() else null

  val totalWeightKg = remember(
    curbWeightVal, driverWeightVal, passengerWeightVal, cargoWeightVal,
    audioWeightVal, gnvWeightVal, otherWeightVal, removedWeightVal,
    measuredWeightVal, useMeasuredWeight
  ) {
    VehicleCalculations.calculateTotalWeight(
      curbWeightKg = curbWeightVal,
      driverWeightKg = driverWeightVal,
      passengerWeightKg = passengerWeightVal,
      cargoWeightKg = cargoWeightVal,
      audioWeightKg = audioWeightVal,
      gnvWeightKg = gnvWeightVal,
      otherWeightKg = otherWeightVal,
      removedWeightKg = removedWeightVal,
      measuredTotalWeightKg = measuredWeightVal,
      useMeasuredWeight = useMeasuredWeight
    )
  }

  val confidence = remember(useMeasuredWeight, audioPreset, hasGnv, cargoWeightVal) {
    VehicleCalculations.evaluateWeightConfidence(
      useMeasuredWeight = useMeasuredWeight,
      audioPreset = audioPreset,
      hasGnv = hasGnv,
      hasCargo = cargoWeightVal > 30f
    )
  }

  fun buildVehicleProfile(): VehicleProfile {
    return VehicleProfile(
      id = vehicleId,
      manufacturer = manufacturer.trim(),
      model = model.trim(),
      year = yearText.toIntOrNull() ?: 2018,
      version = version.trim(),
      engine = engine.trim(),
      displacement = displacement.trim(),
      factoryPowerCv = factoryPowerText.toFloatOrNull(),
      factoryTorqueKgf = factoryTorqueText.toFloatOrNull(),
      curbWeightKg = curbWeightVal,
      drivetrain = drivetrain,
      transmissionId = if (transmissionOption == "KNOWN" || transmissionOption == "ORIGINAL") selectedTransmissionId else null,
      customTransmissionName = if (transmissionOption == "CUSTOM") customTransmissionName.trim() else null,
      tireWidthMm = widthInt,
      tireAspectRatio = aspectInt,
      wheelDiameterInches = rimInt,
      driverWeightKg = driverWeightVal,
      passengerWeightKg = passengerWeightVal,
      cargoWeightKg = cargoWeightVal,
      audioPreset = audioPreset,
      audioWeightKg = audioWeightVal,
      gnvWeightKg = gnvWeightVal,
      otherWeightKg = otherWeightVal,
      removedWeightKg = removedWeightVal,
      measuredTotalWeightKg = measuredWeightVal,
      useMeasuredWeight = useMeasuredWeight,
      isPrimary = existingVehicle?.isPrimary ?: false,
      isCustom = isCustom
    )
  }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("vehicle_wizard_screen"),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (existingVehicle == null) "CADASTRAR VEÍCULO" else "EDITAR VEÍCULO",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
        },
        navigationIcon = {
          IconButton(
            onClick = {
              if (currentStep > 1) {
                currentStep--
              } else {
                onCancel()
              }
            }
          ) {
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
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Step Progress Bar
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
              letterSpacing = 1.sp,
              fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = when (currentStep) {
              1 -> "Identificação"
              2 -> "Dados Originais"
              3 -> "Câmbio"
              4 -> "Pneus"
              5 -> "Peso Adicional"
              6 -> "Confirmação"
              else -> ""
            },
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Medium,
              fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        LinearProgressIndicator(
          progress = { currentStep.toFloat() / totalSteps.toFloat() },
          modifier = Modifier.fillMaxWidth().height(4.dp),
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
      }

      HorizontalDivider(
        thickness = 0.8.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
      )

      // Step Contents (Scrollable)
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
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .widthIn(max = 480.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          when (currentStep) {
            1 -> Step1Identification(
              isSearchMode = isSearchMode,
              onSearchModeChanged = { isSearchMode = it },
              searchQuery = searchQuery,
              onSearchQueryChanged = { searchQuery = it },
              manufacturer = manufacturer,
              onManufacturerChanged = { manufacturer = it },
              model = model,
              onModelChanged = { model = it },
              yearText = yearText,
              onYearTextChanged = { yearText = it },
              version = version,
              onVersionChanged = { version = it },
              engine = engine,
              onEngineChanged = { engine = it },
              onCatalogVehicleSelected = { cv ->
                manufacturer = cv.manufacturer
                model = cv.model
                yearText = cv.year.toString()
                version = cv.version
                engine = cv.engine
                displacement = cv.displacement
                curbWeightText = cv.curbWeightKg.toInt().toString()
                factoryPowerText = cv.factoryPowerCv?.toInt()?.toString() ?: ""
                factoryTorqueText = cv.factoryTorqueKgf?.toString() ?: ""
                drivetrain = cv.drivetrain
                tireWidthText = cv.tireWidthMm.toString()
                tireAspectText = cv.tireAspectRatio.toString()
                wheelDiameterText = cv.wheelDiameterInches.toString()
                selectedTransmissionId = cv.transmissionId ?: "gm_f17_ccw"
                transmissionOption = "ORIGINAL"
                isCustom = false
                currentStep = 2
              }
            )
            2 -> Step2OriginalData(
              curbWeightText = curbWeightText,
              onCurbWeightChanged = { curbWeightText = it },
              factoryPowerText = factoryPowerText,
              onFactoryPowerChanged = { factoryPowerText = it },
              factoryTorqueText = factoryTorqueText,
              onFactoryTorqueChanged = { factoryTorqueText = it },
              displacement = displacement,
              onDisplacementChanged = { displacement = it },
              drivetrain = drivetrain,
              onDrivetrainChanged = { drivetrain = it }
            )
            3 -> Step3Transmission(
              transmissionOption = transmissionOption,
              onTransmissionOptionChanged = { transmissionOption = it },
              selectedTransmissionId = selectedTransmissionId,
              onSelectedTransmissionIdChanged = { selectedTransmissionId = it },
              customTransmissionName = customTransmissionName,
              onCustomTransmissionNameChanged = { customTransmissionName = it },
              showTechnicalDetails = showTransmissionTechnicalDetails,
              onToggleTechnicalDetails = { showTransmissionTechnicalDetails = !showTransmissionTechnicalDetails }
            )
            4 -> Step4Tires(
              tireWidthText = tireWidthText,
              onTireWidthChanged = { tireWidthText = it },
              tireAspectText = tireAspectText,
              onTireAspectChanged = { tireAspectText = it },
              wheelDiameterText = wheelDiameterText,
              onWheelDiameterChanged = { wheelDiameterText = it },
              tireCalc = tireCalc
            )
            5 -> Step5AdditionalWeight(
              curbWeightVal = curbWeightVal,
              driverWeightText = driverWeightText,
              onDriverWeightChanged = { driverWeightText = it },
              passengerWeightText = passengerWeightText,
              onPassengerWeightChanged = { passengerWeightText = it },
              cargoWeightText = cargoWeightText,
              onCargoWeightChanged = { cargoWeightText = it },
              audioPreset = audioPreset,
              onAudioPresetChanged = { audioPreset = it },
              customAudioWeightText = customAudioWeightText,
              onCustomAudioWeightChanged = { customAudioWeightText = it },
              hasGnv = hasGnv,
              onHasGnvChanged = { hasGnv = it },
              gnvWeightText = gnvWeightText,
              onGnvWeightChanged = { gnvWeightText = it },
              hasRemovedSpare = hasRemovedSpare,
              onHasRemovedSpareChanged = { hasRemovedSpare = it },
              removedWeightText = removedWeightText,
              onRemovedWeightChanged = { removedWeightText = it },
              otherWeightText = otherWeightText,
              onOtherWeightChanged = { otherWeightText = it },
              useMeasuredWeight = useMeasuredWeight,
              onUseMeasuredWeightChanged = { useMeasuredWeight = it },
              measuredWeightText = measuredWeightText,
              onMeasuredWeightChanged = { measuredWeightText = it },
              totalWeightKg = totalWeightKg,
              confidence = confidence
            )
            6 -> Step6Confirmation(
              manufacturer = manufacturer,
              model = model,
              yearText = yearText,
              engine = engine,
              version = version,
              totalWeightKg = totalWeightKg,
              transmissionOption = transmissionOption,
              selectedTransmissionId = selectedTransmissionId,
              customTransmissionName = customTransmissionName,
              tireCalc = tireCalc,
              confidence = confidence
            )
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
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("btn_wizard_back"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("VOLTAR")
          }
        } else {
          OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("btn_wizard_cancel"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("CANCELAR")
          }
        }

        if (currentStep < totalSteps) {
          val canAdvance = when (currentStep) {
            1 -> manufacturer.isNotBlank() && model.isNotBlank()
            2 -> curbWeightVal > 200f
            3 -> true
            4 -> widthInt in 125..355 && aspectInt in 25..90 && rimInt in 10..24
            5 -> totalWeightKg > 200f
            else -> true
          }

          Button(
            onClick = {
              if (currentStep == 1 && isSearchMode && manufacturer.isBlank()) {
                isSearchMode = false
              } else {
                currentStep++
              }
            },
            enabled = canAdvance,
            modifier = Modifier
              .weight(1.3f)
              .height(48.dp)
              .testTag("btn_wizard_next"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("AVANÇAR")
          }
        } else {
          Button(
            onClick = {
              val profile = buildVehicleProfile()
              onSaveVehicle(profile)
            },
            modifier = Modifier
              .weight(1.3f)
              .height(48.dp)
              .testTag("btn_wizard_save"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            )
          ) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("SALVAR VEÍCULO")
          }
        }
      }
    }
  }
}

@Composable
private fun Step1Identification(
  isSearchMode: Boolean,
  onSearchModeChanged: (Boolean) -> Unit,
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  manufacturer: String,
  onManufacturerChanged: (String) -> Unit,
  model: String,
  onModelChanged: (String) -> Unit,
  yearText: String,
  onYearTextChanged: (String) -> Unit,
  version: String,
  onVersionChanged: (String) -> Unit,
  engine: String,
  onEngineChanged: (String) -> Unit,
  onCatalogVehicleSelected: (VehicleProfile) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(
      text = "Identificação do Veículo",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )

    // Mode Selector Toggle
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = { onSearchModeChanged(true) },
        modifier = Modifier.weight(1f).height(44.dp),
        shape = RoundedCornerShape(10.dp),
        colors = if (isSearchMode) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
        border = if (!isSearchMode) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
      ) {
        Text("BUSCAR NO BANCO", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
      }

      Button(
        onClick = { onSearchModeChanged(false) },
        modifier = Modifier.weight(1f).height(44.dp),
        shape = RoundedCornerShape(10.dp),
        colors = if (!isSearchMode) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
        border = if (isSearchMode) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
      ) {
        Text("PERSONALIZADO", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
      }
    }

    if (isSearchMode) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        label = { Text("Buscar modelo (ex: Gol, Corsa, Onix, Ka...)") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().testTag("input_search_catalog"),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
      )

      val searchResults = remember(searchQuery) {
        VehicleDatabase.searchVehicles(searchQuery)
      }

      if (searchResults.isEmpty()) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "Veículo ainda não disponível no banco. Continue com o cadastro personalizado.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
              onClick = { onSearchModeChanged(false) },
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("Preencher manualmente")
            }
          }
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Modelos encontrados:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          searchResults.forEach { cv ->
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onCatalogVehicleSelected(cv) },
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Icon(
                  imageVector = Icons.Outlined.DirectionsCar,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${cv.manufacturer} ${cv.model} ${cv.engine}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                  )
                  Text(
                    text = "${cv.year} • ${cv.version} • ${cv.factoryPowerCv?.toInt() ?: 0} cv • ${cv.curbWeightKg.toInt()} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }
      }
    } else {
      // Manual Fields
      OutlinedTextField(
        value = manufacturer,
        onValueChange = onManufacturerChanged,
        label = { Text("Marca * (ex: Chevrolet, Volkswagen)") },
        modifier = Modifier.fillMaxWidth().testTag("input_manufacturer"),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
      )

      OutlinedTextField(
        value = model,
        onValueChange = onModelChanged,
        label = { Text("Modelo * (ex: Corsa, Gol, Civic)") },
        modifier = Modifier.fillMaxWidth().testTag("input_model"),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = yearText,
          onValueChange = onYearTextChanged,
          label = { Text("Ano") },
          modifier = Modifier.weight(1f).testTag("input_year"),
          shape = RoundedCornerShape(12.dp),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true
        )

        OutlinedTextField(
          value = engine,
          onValueChange = onEngineChanged,
          label = { Text("Motor (ex: 1.0 8V)") },
          modifier = Modifier.weight(1.5f).testTag("input_engine"),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )
      }

      OutlinedTextField(
        value = version,
        onValueChange = onVersionChanged,
        label = { Text("Versão / Acabamento (opcional)") },
        modifier = Modifier.fillMaxWidth().testTag("input_version"),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
      )
    }
  }
}

@Composable
private fun Step2OriginalData(
  curbWeightText: String,
  onCurbWeightChanged: (String) -> Unit,
  factoryPowerText: String,
  onFactoryPowerChanged: (String) -> Unit,
  factoryTorqueText: String,
  onFactoryTorqueChanged: (String) -> Unit,
  displacement: String,
  onDisplacementChanged: (String) -> Unit,
  drivetrain: String,
  onDrivetrainChanged: (String) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(
      text = "Dados Originais de Fábrica",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )

    Text(
      text = "Essas informações são usadas como referência técnica para estimar a potência e torque.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    OutlinedTextField(
      value = curbWeightText,
      onValueChange = onCurbWeightChanged,
      label = { Text("Peso original em ordem de marcha (kg) *") },
      modifier = Modifier.fillMaxWidth().testTag("input_curb_weight"),
      shape = RoundedCornerShape(12.dp),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      singleLine = true
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      OutlinedTextField(
        value = factoryPowerText,
        onValueChange = onFactoryPowerChanged,
        label = { Text("Potência (cv) [opc]") },
        modifier = Modifier.weight(1f).testTag("input_factory_power"),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
      )

      OutlinedTextField(
        value = factoryTorqueText,
        onValueChange = onFactoryTorqueChanged,
        label = { Text("Torque (kgfm) [opc]") },
        modifier = Modifier.weight(1f).testTag("input_factory_torque"),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
      )
    }

    OutlinedTextField(
      value = displacement,
      onValueChange = onDisplacementChanged,
      label = { Text("Cilindrada (ex: 1.0, 1.4, 1.6, 2.0) [opc]") },
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      singleLine = true
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        text = "Tração:",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf("Dianteira", "Traseira", "Integral").forEach { dt ->
          val isSelected = drivetrain.equals(dt, ignoreCase = true)
          Surface(
            modifier = Modifier
              .weight(1f)
              .clickable { onDrivetrainChanged(dt) },
            shape = RoundedCornerShape(10.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          ) {
            Box(
              modifier = Modifier.padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = dt,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun Step3Transmission(
  transmissionOption: String,
  onTransmissionOptionChanged: (String) -> Unit,
  selectedTransmissionId: String,
  onSelectedTransmissionIdChanged: (String) -> Unit,
  customTransmissionName: String,
  onCustomTransmissionNameChanged: (String) -> Unit,
  showTechnicalDetails: Boolean,
  onToggleTechnicalDetails: () -> Unit
) {
  val currentTransmission = VehicleDatabase.getTransmission(selectedTransmissionId)

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(
      text = "Câmbio e Relações",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )

    Text(
      text = "O câmbio permite converter a velocidade das rodas na rotação (RPM) do motor.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // Option 1: Original / Catalog
    Card(
      modifier = Modifier.fillMaxWidth().clickable { onTransmissionOptionChanged("KNOWN") },
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(
        containerColor = if (transmissionOption == "KNOWN" || transmissionOption == "ORIGINAL")
          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
      ),
      border = BorderStroke(
        1.dp,
        if (transmissionOption == "KNOWN" || transmissionOption == "ORIGINAL")
          MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
      )
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          RadioButton(
            selected = transmissionOption == "KNOWN" || transmissionOption == "ORIGINAL",
            onClick = { onTransmissionOptionChanged("KNOWN") }
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Selecionar câmbio conhecido",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
        }

        if (transmissionOption == "KNOWN" || transmissionOption == "ORIGINAL") {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VehicleDatabase.transmissions.forEach { trans ->
              val isSelected = trans.id == selectedTransmissionId
              Surface(
                modifier = Modifier.fillMaxWidth().clickable { onSelectedTransmissionIdChanged(trans.id) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text(
                      text = trans.displayName,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                      text = "Fabricante: ${trans.manufacturer} • Família: ${trans.family} • Código: ${trans.code}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                  }
                }
              }
            }

            if (currentTransmission != null) {
              OutlinedButton(
                onClick = onToggleTechnicalDetails,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
              ) {
                Text(if (showTechnicalDetails) "Ocultar dados técnicos" else "Ver dados técnicos")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                  if (showTechnicalDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                  contentDescription = null
                )
              }

              AnimatedVisibility(visible = showTechnicalDetails) {
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(10.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                  Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Dados Técnicos do Câmbio:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    currentTransmission.gearRatios.forEachIndexed { idx, ratio ->
                      Text("${idx + 1}ª marcha: $ratio : 1", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Diferencial final: ${currentTransmission.finalDrive} : 1", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                  }
                }
              }
            }
          }
        }
      }
    }

    // Option 2: Custom
    Card(
      modifier = Modifier.fillMaxWidth().clickable { onTransmissionOptionChanged("CUSTOM") },
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(
        containerColor = if (transmissionOption == "CUSTOM")
          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
      ),
      border = BorderStroke(
        1.dp,
        if (transmissionOption == "CUSTOM")
          MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
      )
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          RadioButton(
            selected = transmissionOption == "CUSTOM",
            onClick = { onTransmissionOptionChanged("CUSTOM") }
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Câmbio personalizado",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
        }
        if (transmissionOption == "CUSTOM") {
          OutlinedTextField(
            value = customTransmissionName,
            onValueChange = onCustomTransmissionNameChanged,
            label = { Text("Nome do câmbio (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          )
        }
      }
    }

    // Option 3: Don't know
    Card(
      modifier = Modifier.fillMaxWidth().clickable { onTransmissionOptionChanged("UNKNOWN") },
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(
        containerColor = if (transmissionOption == "UNKNOWN")
          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
      ),
      border = BorderStroke(
        1.dp,
        if (transmissionOption == "UNKNOWN")
          MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
      )
    ) {
      Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
          selected = transmissionOption == "UNKNOWN",
          onClick = { onTransmissionOptionChanged("UNKNOWN") }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "Não sei qual é o câmbio",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            text = "Você poderá realizar o teste normalmente. O cálculo de RPM ficará pendente.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
private fun Step4Tires(
  tireWidthText: String,
  onTireWidthChanged: (String) -> Unit,
  tireAspectText: String,
  onTireAspectChanged: (String) -> Unit,
  wheelDiameterText: String,
  onWheelDiameterChanged: (String) -> Unit,
  tireCalc: com.example.model.TireCalculation
) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(
      text = "Medida dos Pneus",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )

    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
          text = "Encontre essa medida escrita na lateral do pneu (exemplo: 185/70 R14).",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      OutlinedTextField(
        value = tireWidthText,
        onValueChange = onTireWidthChanged,
        label = { Text("Largura (mm)") },
        modifier = Modifier.weight(1f).testTag("input_tire_width"),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
      )

      OutlinedTextField(
        value = tireAspectText,
        onValueChange = onTireAspectChanged,
        label = { Text("Perfil (%)") },
        modifier = Modifier.weight(1f).testTag("input_tire_aspect"),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
      )

      OutlinedTextField(
        value = wheelDiameterText,
        onValueChange = onWheelDiameterChanged,
        label = { Text("Aro (pol)") },
        modifier = Modifier.weight(1f).testTag("input_wheel_diameter"),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
      )
    }

    // Calculated Tire Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Dimensões Calculadas:",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Medida formatada:", style = MaterialTheme.typography.bodyMedium)
          Text(tireCalc.formattedMeasure, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Diâmetro total do pneu:", style = MaterialTheme.typography.bodyMedium)
          Text(String.format(Locale.US, "%.1f mm (%.2f cm)", tireCalc.totalDiameterMm, tireCalc.totalDiameterMm / 10.0), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Circunferência de rolamento:", style = MaterialTheme.typography.bodyMedium)
          Text(String.format(Locale.US, "%.3f metros", tireCalc.circumferenceM), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
        }
      }
    }
  }
}

@Composable
private fun Step5AdditionalWeight(
  curbWeightVal: Float,
  driverWeightText: String,
  onDriverWeightChanged: (String) -> Unit,
  passengerWeightText: String,
  onPassengerWeightChanged: (String) -> Unit,
  cargoWeightText: String,
  onCargoWeightChanged: (String) -> Unit,
  audioPreset: AudioWeightPreset,
  onAudioPresetChanged: (AudioWeightPreset) -> Unit,
  customAudioWeightText: String,
  onCustomAudioWeightChanged: (String) -> Unit,
  hasGnv: Boolean,
  onHasGnvChanged: (Boolean) -> Unit,
  gnvWeightText: String,
  onGnvWeightChanged: (String) -> Unit,
  hasRemovedSpare: Boolean,
  onHasRemovedSpareChanged: (Boolean) -> Unit,
  removedWeightText: String,
  onRemovedWeightChanged: (String) -> Unit,
  otherWeightText: String,
  onOtherWeightChanged: (String) -> Unit,
  useMeasuredWeight: Boolean,
  onUseMeasuredWeightChanged: (Boolean) -> Unit,
  measuredWeightText: String,
  onMeasuredWeightChanged: (String) -> Unit,
  totalWeightKg: Float,
  confidence: WeightConfidence
) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(
      text = "Peso dos Ocupantes e Adicionais",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )

    // Option: Weighing Scale override
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(
        containerColor = if (useMeasuredWeight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
      ),
      border = BorderStroke(1.dp, if (useMeasuredWeight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().clickable { onUseMeasuredWeightChanged(!useMeasuredWeight) }
        ) {
          Checkbox(
            checked = useMeasuredWeight,
            onCheckedChange = { onUseMeasuredWeightChanged(it) }
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Peso total verificado em balança",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
        }

        if (useMeasuredWeight) {
          OutlinedTextField(
            value = measuredWeightText,
            onValueChange = onMeasuredWeightChanged,
            label = { Text("Peso total medido na balança (kg)") },
            modifier = Modifier.fillMaxWidth().testTag("input_measured_weight"),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
          )
        }
      }
    }

    // Occupants
    Text(
      text = "PESO DOS OCUPANTES",
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
      color = MaterialTheme.colorScheme.primary
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      OutlinedTextField(
        value = driverWeightText,
        onValueChange = onDriverWeightChanged,
        label = { Text("Motorista (kg)") },
        modifier = Modifier.weight(1f).testTag("input_driver_weight"),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
      )

      OutlinedTextField(
        value = passengerWeightText,
        onValueChange = onPassengerWeightChanged,
        label = { Text("Passageiros (kg)") },
        modifier = Modifier.weight(1f).testTag("input_passenger_weight"),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
      )

      OutlinedTextField(
        value = cargoWeightText,
        onValueChange = onCargoWeightChanged,
        label = { Text("Carga (kg)") },
        modifier = Modifier.weight(1f).testTag("input_cargo_weight"),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
      )
    }

    // Audio Question
    Text(
      text = "Possui som adicional?",
      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
    )

    AudioWeightPreset.values().forEach { preset ->
      val isSelected = audioPreset == preset
      Surface(
        modifier = Modifier.fillMaxWidth().clickable { onAudioPresetChanged(preset) },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          RadioButton(selected = isSelected, onClick = { onAudioPresetChanged(preset) })
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(preset.label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            if (preset.estimatedWeightKg > 0f) {
              Text(
                "Estimativa: ${preset.estimatedWeightKg.toInt()} kg (faixa: ${preset.minWeightKg.toInt()} a ${preset.maxWeightKg.toInt()} kg)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }

    if (audioPreset == AudioWeightPreset.CUSTOM || audioPreset == AudioWeightPreset.PAREDAO) {
      if (audioPreset == AudioWeightPreset.PAREDAO) {
        Text(
          text = "Projetos de som/paredão variam muito de peso devido à quantidade de baterias e madeira. Informe o peso estimado do projeto.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error
        )
      }
      OutlinedTextField(
        value = customAudioWeightText,
        onValueChange = onCustomAudioWeightChanged,
        label = { Text("Peso estimado do som (kg)") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
      )
    }

    // Additional options: GNV, Removed spare wheel, other
    Text(
      text = "OUTROS EQUIPAMENTOS",
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
      color = MaterialTheme.colorScheme.primary
    )

    // Kit GNV
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Checkbox(checked = hasGnv, onCheckedChange = onHasGnvChanged)
      Text("Kit GNV instalado", style = MaterialTheme.typography.bodyMedium)
      if (hasGnv) {
        Spacer(modifier = Modifier.width(12.dp))
        OutlinedTextField(
          value = gnvWeightText,
          onValueChange = onGnvWeightChanged,
          label = { Text("kg") },
          modifier = Modifier.width(90.dp),
          shape = RoundedCornerShape(8.dp),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true
        )
      }
    }

    // Estepe removido
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Checkbox(checked = hasRemovedSpare, onCheckedChange = onHasRemovedSpareChanged)
      Text("Estepe / banco removido (-kg)", style = MaterialTheme.typography.bodyMedium)
      if (hasRemovedSpare) {
        Spacer(modifier = Modifier.width(12.dp))
        OutlinedTextField(
          value = removedWeightText,
          onValueChange = onRemovedWeightChanged,
          label = { Text("kg") },
          modifier = Modifier.width(90.dp),
          shape = RoundedCornerShape(8.dp),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true
        )
      }
    }

    // Live Total Weight Summary Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Detalhamento de Peso:", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Peso original do carro:", style = MaterialTheme.typography.bodySmall)
          Text("${curbWeightVal.toInt()} kg", style = MaterialTheme.typography.bodySmall)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Motorista:", style = MaterialTheme.typography.bodySmall)
          Text("${driverWeightText.toIntOrNull() ?: 0} kg", style = MaterialTheme.typography.bodySmall)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Passageiros:", style = MaterialTheme.typography.bodySmall)
          Text("${passengerWeightText.toIntOrNull() ?: 0} kg", style = MaterialTheme.typography.bodySmall)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Som adicional:", style = MaterialTheme.typography.bodySmall)
          Text("${when(audioPreset) { AudioWeightPreset.NONE -> 0; AudioWeightPreset.LIGHT -> 18; AudioWeightPreset.MEDIUM -> 43; AudioWeightPreset.HEAVY -> 90; else -> customAudioWeightText.toIntOrNull() ?: 0 }} kg", style = MaterialTheme.typography.bodySmall)
        }
        if (hasGnv) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("GNV:", style = MaterialTheme.typography.bodySmall)
            Text("${gnvWeightText.toIntOrNull() ?: 0} kg", style = MaterialTheme.typography.bodySmall)
          }
        }
        if (hasRemovedSpare) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Itens removidos:", style = MaterialTheme.typography.bodySmall)
            Text("-${removedWeightText.toIntOrNull() ?: 0} kg", style = MaterialTheme.typography.bodySmall)
          }
        }
        HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("PESO TOTAL ESTIMADO:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
          Text(String.format(Locale.US, "%.0f kg", totalWeightKg), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = when (confidence) {
            WeightConfidence.HIGH -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            WeightConfidence.GOOD -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            WeightConfidence.ESTIMATED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
          }
        ) {
          Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
              text = "${confidence.label}: ${confidence.description}",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun Step6Confirmation(
  manufacturer: String,
  model: String,
  yearText: String,
  engine: String,
  version: String,
  totalWeightKg: Float,
  transmissionOption: String,
  selectedTransmissionId: String,
  customTransmissionName: String,
  tireCalc: com.example.model.TireCalculation,
  confidence: WeightConfidence
) {
  val transmissionDisplayName = when (transmissionOption) {
    "KNOWN", "ORIGINAL" -> VehicleDatabase.getTransmission(selectedTransmissionId)?.displayName ?: "Original"
    "CUSTOM" -> customTransmissionName.ifBlank { "Personalizado" }
    else -> "Não especificado"
  }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(
      text = "VEÍCULO CONFIGURADO",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.primary
    )

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "$manufacturer $model",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp)
          )
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
          ) {
            Text(
              text = confidence.label,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
            )
          }
        }

        val subline = listOfNotNull(yearText, engine.ifBlank { null }, version.ifBlank { null }).joinToString(" • ")
        Text(text = subline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Peso total para o teste:", style = MaterialTheme.typography.bodyMedium)
          Text(String.format(Locale.US, "%.0f kg", totalWeightKg), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Câmbio:", style = MaterialTheme.typography.bodyMedium)
          Text(transmissionDisplayName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Marcha recomendada no teste:", style = MaterialTheme.typography.bodyMedium)
          Text("3ª marcha (direta/intermediária)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Pneu:", style = MaterialTheme.typography.bodyMedium)
          Text(tireCalc.formattedMeasure, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Circunferência de rolamento:", style = MaterialTheme.typography.bodyMedium)
          Text(String.format(Locale.US, "%.3f m", tireCalc.circumferenceM), style = MaterialTheme.typography.bodyMedium)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Confiança do peso:", style = MaterialTheme.typography.bodyMedium)
          Text(confidence.label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        }
      }
    }
  }
}
