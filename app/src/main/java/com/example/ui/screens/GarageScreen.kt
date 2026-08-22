package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VehicleDatabase
import com.example.model.VehicleCalculations
import com.example.model.VehicleProfile
import java.util.Locale

@Composable
fun GarageScreen(
  vehicles: List<VehicleProfile>,
  onAddVehicle: () -> Unit,
  onEditVehicle: (VehicleProfile) -> Unit,
  onDuplicateVehicle: (String) -> Unit = {},
  onSetPrimaryVehicle: (String) -> Unit,
  onDeleteVehicle: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var vehicleToDelete by remember { mutableStateOf<VehicleProfile?>(null) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp),
    contentAlignment = Alignment.TopCenter
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 500.dp)
        .padding(vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Header Section
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "MINHA GARAGEM",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              fontSize = 20.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("garage_title")
          )
          Text(
            text = "${vehicles.size} ${if (vehicles.size == 1) "veículo cadastrado" else "veículos cadastrados"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Button(
          onClick = onAddVehicle,
          modifier = Modifier
            .height(42.dp)
            .testTag("btn_add_vehicle_top"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          contentPadding = PaddingValues(horizontal = 14.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "ADICIONAR",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          )
        }
      }

      if (vehicles.isEmpty()) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
          ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.DirectionsCar,
              contentDescription = null,
              modifier = Modifier.size(52.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Text(
              text = "Sua garagem está vazia",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )

            Text(
              text = "Adicione seu primeiro veículo para começar a preparar as medições de potência.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )

            Button(
              onClick = onAddVehicle,
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_garage_empty_add"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("CADASTRAR VEÍCULO")
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(14.dp),
          contentPadding = PaddingValues(bottom = 24.dp)
        ) {
          items(vehicles, key = { it.id }) { vehicle ->
            VehicleGarageCard(
              vehicle = vehicle,
              onSelectPrimary = { onSetPrimaryVehicle(vehicle.id) },
              onEdit = { onEditVehicle(vehicle) },
              onDuplicate = { onDuplicateVehicle(vehicle.id) },
              onDelete = { vehicleToDelete = vehicle }
            )
          }
        }
      }
    }
  }

  vehicleToDelete?.let { vehicle ->
    AlertDialog(
      onDismissRequest = { vehicleToDelete = null },
      title = {
        Text(
          text = "Excluir veículo?",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Tem certeza que deseja remover ${vehicle.manufacturer} ${vehicle.model} da sua garagem?",
            style = MaterialTheme.typography.bodyMedium
          )
          Text(
            text = "Aviso: Os resultados permanecerão no histórico, mas o perfil do veículo será removido.",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.error
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteVehicle(vehicle.id)
            vehicleToDelete = null
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
          )
        ) {
          Text("EXCLUIR")
        }
      },
      dismissButton = {
        TextButton(onClick = { vehicleToDelete = null }) {
          Text("CANCELAR")
        }
      }
    )
  }
}

@Composable
private fun VehicleGarageCard(
  vehicle: VehicleProfile,
  onSelectPrimary: () -> Unit,
  onEdit: () -> Unit,
  onDuplicate: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val totalWeight = VehicleCalculations.calculateTotalWeight(
    curbWeightKg = vehicle.curbWeightKg,
    driverWeightKg = vehicle.driverWeightKg,
    passengerWeightKg = vehicle.passengerWeightKg,
    cargoWeightKg = vehicle.cargoWeightKg,
    audioWeightKg = vehicle.audioWeightKg,
    gnvWeightKg = vehicle.gnvWeightKg,
    otherWeightKg = vehicle.otherWeightKg,
    removedWeightKg = vehicle.removedWeightKg,
    measuredTotalWeightKg = vehicle.measuredTotalWeightKg,
    useMeasuredWeight = vehicle.useMeasuredWeight
  )

  val transmissionLabel = when {
    vehicle.transmissionId != null ->
      VehicleDatabase.getTransmission(vehicle.transmissionId)?.displayName ?: "Original"
    !vehicle.customTransmissionName.isNullOrBlank() ->
      vehicle.customTransmissionName
    else -> "Original"
  }

  val isDataComplete = vehicle.tireWidthMm > 0 &&
    vehicle.tireAspectRatio > 0 &&
    vehicle.wheelDiameterInches > 0 &&
    totalWeight > 300f

  val dataStatusLabel = if (vehicle.useMeasuredWeight) {
    "Dados verificados"
  } else if (isDataComplete) {
    "Dados conferidos"
  } else {
    "Dados parciais"
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("vehicle_card_${vehicle.id}"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (vehicle.isPrimary)
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
      else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ),
    border = BorderStroke(
      1.dp,
      if (vehicle.isPrimary)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
      else
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${vehicle.manufacturer} ${vehicle.model}",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
          val sub = listOfNotNull(
            vehicle.year.toString(),
            vehicle.engine.ifBlank { null },
            vehicle.version.ifBlank { null }
          ).joinToString(" • ")
          Text(
            text = sub,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
          ) {
            Text(
              text = dataStatusLabel,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 10.5.sp
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }

          if (vehicle.isPrimary) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primaryContainer
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Star,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = "Principal",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                  ),
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }
            }
          }
        }
      }

      HorizontalDivider(
        thickness = 0.8.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
      )

      // Specs summary
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text(
            text = "Peso Total",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
          Text(
            text = String.format(Locale.US, "%.0f kg", totalWeight),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Column {
          Text(
            text = "Pneu",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
          Text(
            text = "${vehicle.tireWidthMm}/${vehicle.tireAspectRatio} R${vehicle.wheelDiameterInches}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Column {
          Text(
            text = "Câmbio",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
          Text(
            text = transmissionLabel.take(12),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        if (vehicle.factoryPowerCv != null && vehicle.factoryPowerCv > 0f) {
          Column {
            Text(
              text = "Potência Orig.",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
              text = String.format(Locale.US, "%.0f cv", vehicle.factoryPowerCv),
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }

      HorizontalDivider(
        thickness = 0.8.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
      )

      // Action buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (!vehicle.isPrimary) {
          TextButton(
            onClick = onSelectPrimary,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.testTag("btn_set_primary_${vehicle.id}")
          ) {
            Icon(
              imageVector = Icons.Outlined.StarOutline,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Tornar principal",
              style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp)
            )
          }
        } else {
          Spacer(modifier = Modifier.width(1.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(
            onClick = onDuplicate,
            modifier = Modifier.size(36.dp).testTag("btn_duplicate_${vehicle.id}")
          ) {
            Icon(
              imageVector = Icons.Outlined.ContentCopy,
              contentDescription = "Duplicar",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }

          IconButton(
            onClick = onEdit,
            modifier = Modifier.size(36.dp).testTag("btn_edit_${vehicle.id}")
          ) {
            Icon(
              imageVector = Icons.Outlined.Edit,
              contentDescription = "Editar",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }

          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp).testTag("btn_delete_${vehicle.id}")
          ) {
            Icon(
              imageVector = Icons.Outlined.DeleteOutline,
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
