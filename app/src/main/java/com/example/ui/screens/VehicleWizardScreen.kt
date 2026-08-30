package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VehicleRepository
import com.example.model.AspirationType
import com.example.model.FuelType
import com.example.model.TireSpec
import com.example.model.Vehicle
import com.example.ui.theme.DynoBg
import com.example.ui.theme.DynoCardBg
import com.example.ui.theme.DynoCardBorder
import com.example.ui.theme.DynoCardSurface
import com.example.ui.theme.DynoPowerCyan
import com.example.ui.theme.DynoTextPrimary
import com.example.ui.theme.DynoTextSecondary
import com.example.ui.theme.DynoTorqueAmber
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleWizardScreen(
    vehicleRepository: VehicleRepository,
    vehicleId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var yearText by remember { mutableStateOf("2020") }
    var curbWeightText by remember { mutableStateOf("1350") }
    var driverWeightText by remember { mutableStateOf("80") }
    var displacementText by remember { mutableStateOf("2000") }
    var aspiration by remember { mutableStateOf(AspirationType.TURBOCHARGED) }
    var fuelType by remember { mutableStateOf(FuelType.GASOLINE) }
    var tireWidthText by remember { mutableStateOf("225") }
    var tireProfileText by remember { mutableStateOf("45") }
    var tireRimText by remember { mutableStateOf("17") }
    var finalDriveText by remember { mutableStateOf("3.94") }

    LaunchedEffect(vehicleId) {
        if (vehicleId != null) {
            val v = vehicleRepository.getVehicleById(vehicleId)
            if (v != null) {
                name = v.name
                brand = v.brand
                model = v.model
                yearText = v.year.toString()
                curbWeightText = v.curbWeightKg.toInt().toString()
                driverWeightText = v.driverWeightKg.toInt().toString()
                displacementText = v.engineDisplacementCc.toString()
                aspiration = v.aspiration
                fuelType = v.fuelType
                tireWidthText = v.tireSpec.widthMm.toString()
                tireProfileText = v.tireSpec.profilePercent.toString()
                tireRimText = v.tireSpec.rimInches.toString()
                finalDriveText = v.finalDriveRatio.toString()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DynoBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (vehicleId == null) "NOVO VEÍCULO" else "EDITAR VEÍCULO",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DynoTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = DynoTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DynoBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DynoCardBg),
                border = BorderStroke(1.dp, DynoCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Identificação", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = DynoPowerCyan)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Apelido do Carro (Ex: Golf Stage 2)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DynoPowerCyan,
                            unfocusedBorderColor = DynoCardBorder,
                            focusedContainerColor = DynoCardSurface,
                            unfocusedContainerColor = DynoCardSurface
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text("Marca (Ex: VW)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DynoPowerCyan,
                                unfocusedBorderColor = DynoCardBorder,
                                focusedContainerColor = DynoCardSurface,
                                unfocusedContainerColor = DynoCardSurface
                            )
                        )
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("Modelo (Ex: Golf GTI)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DynoPowerCyan,
                                unfocusedBorderColor = DynoCardBorder,
                                focusedContainerColor = DynoCardSurface,
                                unfocusedContainerColor = DynoCardSurface
                            )
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DynoCardBg),
                border = BorderStroke(1.dp, DynoCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Massa e Aerodinâmica", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = DynoTorqueAmber)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = curbWeightText,
                            onValueChange = { curbWeightText = it },
                            label = { Text("Peso em ordem (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DynoPowerCyan,
                                unfocusedBorderColor = DynoCardBorder,
                                focusedContainerColor = DynoCardSurface,
                                unfocusedContainerColor = DynoCardSurface
                            )
                        )
                        OutlinedTextField(
                            value = driverWeightText,
                            onValueChange = { driverWeightText = it },
                            label = { Text("Piloto + Ocupantes (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DynoPowerCyan,
                                unfocusedBorderColor = DynoCardBorder,
                                focusedContainerColor = DynoCardSurface,
                                unfocusedContainerColor = DynoCardSurface
                            )
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DynoCardBg),
                border = BorderStroke(1.dp, DynoCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Motor e Pneus", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = DynoPowerCyan)

                    OutlinedTextField(
                        value = displacementText,
                        onValueChange = { displacementText = it },
                        label = { Text("Cilindrada (cc)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DynoPowerCyan,
                            unfocusedBorderColor = DynoCardBorder,
                            focusedContainerColor = DynoCardSurface,
                            unfocusedContainerColor = DynoCardSurface
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tireWidthText,
                            onValueChange = { tireWidthText = it },
                            label = { Text("Largura (mm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DynoPowerCyan,
                                unfocusedBorderColor = DynoCardBorder,
                                focusedContainerColor = DynoCardSurface,
                                unfocusedContainerColor = DynoCardSurface
                            )
                        )
                        OutlinedTextField(
                            value = tireProfileText,
                            onValueChange = { tireProfileText = it },
                            label = { Text("Perfil (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DynoPowerCyan,
                                unfocusedBorderColor = DynoCardBorder,
                                focusedContainerColor = DynoCardSurface,
                                unfocusedContainerColor = DynoCardSurface
                            )
                        )
                        OutlinedTextField(
                            value = tireRimText,
                            onValueChange = { tireRimText = it },
                            label = { Text("Aro (pol)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DynoPowerCyan,
                                unfocusedBorderColor = DynoCardBorder,
                                focusedContainerColor = DynoCardSurface,
                                unfocusedContainerColor = DynoCardSurface
                            )
                        )
                    }
                }
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        val safeWeight = curbWeightText.toFloatOrNull() ?: 1350f
                        val safeDriver = driverWeightText.toFloatOrNull() ?: 80f
                        val safeDisplacement = displacementText.toIntOrNull() ?: 2000
                        val safeWidth = tireWidthText.toIntOrNull() ?: 225
                        val safeProfile = tireProfileText.toIntOrNull() ?: 45
                        val safeRim = tireRimText.toIntOrNull() ?: 17
                        val safeFinalDrive = finalDriveText.toFloatOrNull() ?: 3.94f

                        val v = Vehicle(
                            id = vehicleId ?: UUID.randomUUID().toString(),
                            name = name.ifBlank { "${brand.ifBlank { "Carro" }} ${model.ifBlank { "Turbo" }}" },
                            brand = brand.ifBlank { "Custom" },
                            model = model.ifBlank { "Modelo" },
                            year = yearText.toIntOrNull() ?: 2020,
                            curbWeightKg = safeWeight,
                            driverWeightKg = safeDriver,
                            engineDisplacementCc = safeDisplacement,
                            aspiration = aspiration,
                            fuelType = fuelType,
                            tireSpec = TireSpec(safeWidth, safeProfile, safeRim),
                            finalDriveRatio = safeFinalDrive,
                            isPrimary = vehicleId == null
                        )
                        vehicleRepository.insertOrUpdateVehicle(v)
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DynoPowerCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = DynoBg)
                Spacer(modifier = Modifier.padding(4.dp))
                Text("SALVAR VEÍCULO", color = DynoBg, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
