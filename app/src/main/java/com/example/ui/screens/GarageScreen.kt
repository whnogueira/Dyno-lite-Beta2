package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.VehicleRepository
import com.example.model.Vehicle
import com.example.ui.theme.DynoBg
import com.example.ui.theme.DynoCardBg
import com.example.ui.theme.DynoCardBorder
import com.example.ui.theme.DynoCardSurface
import com.example.ui.theme.DynoErrorRed
import com.example.ui.theme.DynoPowerCyan
import com.example.ui.theme.DynoSuccessGreen
import com.example.ui.theme.DynoTextMuted
import com.example.ui.theme.DynoTextPrimary
import com.example.ui.theme.DynoTextSecondary
import com.example.ui.theme.DynoTorqueAmber
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(
    vehicleRepository: VehicleRepository,
    onNavigateBack: () -> Unit,
    onNavigateToAddVehicle: () -> Unit,
    onNavigateToEditVehicle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val vehicles by vehicleRepository.allVehicles.collectAsStateWithLifecycle(initialValue = emptyList())
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DynoBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "GARAGEM DE VEÍCULOS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddVehicle,
                containerColor = DynoPowerCyan,
                contentColor = DynoBg
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Veículo")
            }
        }
    ) { innerPadding ->
        if (vehicles.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = DynoTextMuted,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Nenhum veículo na garagem",
                    style = MaterialTheme.typography.titleMedium,
                    color = DynoTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cadastre o primeiro carro para medir potência e torque com precisão.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DynoTextSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNavigateToAddVehicle,
                    colors = ButtonDefaults.buttonColors(containerColor = DynoPowerCyan)
                ) {
                    Text("CADASTRAR VEÍCULO", color = DynoBg, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vehicles, key = { it.id }) { vehicle ->
                    VehicleCard(
                        vehicle = vehicle,
                        onSelectPrimary = {
                            coroutineScope.launch {
                                vehicleRepository.setPrimaryVehicle(vehicle.id)
                            }
                        },
                        onEdit = { onNavigateToEditVehicle(vehicle.id) },
                        onDelete = {
                            coroutineScope.launch {
                                vehicleRepository.deleteVehicle(vehicle.id)
                            }
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun VehicleCard(
    vehicle: Vehicle,
    onSelectPrimary: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectPrimary() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (vehicle.isPrimary) DynoCardSurface else DynoCardBg
        ),
        border = BorderStroke(
            1.dp,
            if (vehicle.isPrimary) DynoPowerCyan else DynoCardBorder
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        imageVector = if (vehicle.isPrimary) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = if (vehicle.isPrimary) "Veículo Ativo" else "Selecionar",
                        tint = if (vehicle.isPrimary) DynoPowerCyan else DynoTextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (vehicle.isPrimary) "VEÍCULO PRINCIPAL" else "SELECIONAR COMO PRINCIPAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (vehicle.isPrimary) DynoPowerCyan else DynoTextSecondary
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = DynoTextSecondary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = DynoErrorRed.copy(alpha = 0.8f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = vehicle.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = DynoTextPrimary
            )
            Text(
                text = "${vehicle.brand} ${vehicle.model} (${vehicle.year})",
                style = MaterialTheme.typography.bodyMedium,
                color = DynoTextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TagBadge(text = "${vehicle.totalMassKg.toInt()} kg", color = DynoPowerCyan)
                TagBadge(text = vehicle.aspiration.displayName, color = DynoTorqueAmber)
                TagBadge(text = "${vehicle.engineDisplacementCc} cc", color = DynoSuccessGreen)
                TagBadge(text = "${vehicle.tireSpec.widthMm}/${vehicle.tireSpec.profilePercent} R${vehicle.tireSpec.rimInches}", color = DynoTextSecondary)
            }
        }
    }
}
