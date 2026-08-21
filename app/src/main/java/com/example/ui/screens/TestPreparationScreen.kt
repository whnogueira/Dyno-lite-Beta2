package com.example.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestPreparationScreen(
  vehicle: VehicleProfile,
  onProceedToSensorScreen: () -> Unit,
  onEditVehicle: () -> Unit,
  onNavigateBack: () -> Unit,
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

  val tireCalc = VehicleCalculations.calculateTireDimensions(
    widthMm = vehicle.tireWidthMm,
    aspectRatio = vehicle.tireAspectRatio,
    rimInches = vehicle.wheelDiameterInches
  )

  val confidence = VehicleCalculations.evaluateWeightConfidence(
    useMeasuredWeight = vehicle.useMeasuredWeight,
    audioPreset = vehicle.audioPreset,
    hasGnv = vehicle.gnvWeightKg > 0f,
    hasCargo = vehicle.cargoWeightKg > 30f
  )

  val transmissionName = when {
    vehicle.transmissionId != null -> VehicleDatabase.getTransmission(vehicle.transmissionId)?.displayName ?: "Original"
    vehicle.customTransmissionName != null -> vehicle.customTransmissionName
    else -> "Original"
  }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("test_preparation_screen"),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "PREPARAÇÃO DO TESTE",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
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
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 16.dp)
          .widthIn(max = 480.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
      ) {
        // Vehicle Summary Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer
              ) {
                Box(
                  modifier = Modifier.fillMaxSize(),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                }
              }

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${vehicle.manufacturer} ${vehicle.model}",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                  )
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
            }

            HorizontalDivider(
              thickness = 0.8.dp,
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Peso total no teste:",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = String.format(Locale.US, "%.0f kg", totalWeight),
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Confiança do peso:",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = confidence.label,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.primary
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Pneu configurado:",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = tireCalc.formattedMeasure,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Circunferência:",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = String.format(Locale.US, "%.3f m", tireCalc.circumferenceM),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Câmbio:",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = transmissionName,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Marcha escolhida:",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "3ª marcha",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                  )
                )
              }
            }
          }
        }

        // Safety Alert Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
          ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.Warning,
              contentDescription = "Aviso de Segurança",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "AVISO DE SEGURANÇA",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.error
              )
              Text(
                text = "Prepare o teste antes de movimentar o veículo. Utilize um passageiro ou local fechado e seguro.",
                style = MaterialTheme.typography.bodySmall.copy(
                  lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }

        // Instructions Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
          ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "Como será realizada a medição:",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "1. Fixe o celular firmemente no suporte na vertical.\n" +
                "2. Calibre o zero com o carro parado e motor funcionando.\n" +
                "3. Inicie o procedimento com o carro totalmente parado.\n" +
                "4. Acelere em 3ª marcha; o teste começará automaticamente na velocidade selecionada (40, 50 ou 60 km/h) pelo GPS real.\n" +
                "5. Ao atingir o limite ou pisar na embreagem, o teste encerra e salva os dados.",
              style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Button(
            onClick = onProceedToSensorScreen,
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
              .testTag("btn_preparar_teste_proceed"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            )
          ) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = null,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "INICIAR COM O CARRO PARADO",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                fontSize = 14.sp
              )
            )
          }

          FilledTonalButton(
            onClick = onEditVehicle,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("btn_corrigir_dados"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
              contentColor = MaterialTheme.colorScheme.onSurface
            )
          ) {
            Icon(
              imageVector = Icons.Outlined.Edit,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "CORRIGIR DADOS",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
              )
            )
          }
        }
      }
    }
  }
}
