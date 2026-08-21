package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.VehicleCalculations
import com.example.model.VehicleProfile
import java.util.Locale

@Composable
fun HomeScreen(
  primaryVehicle: VehicleProfile?,
  onNavigateToWizard: () -> Unit,
  onNavigateToGarage: () -> Unit,
  onNavigateToTestPrep: () -> Unit,
  onNavigateToSettings: () -> Unit,
  onNavigateToGuide: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var showHowItWorksDialog by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp),
    contentAlignment = Alignment.TopCenter
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(vertical = 20.dp)
        .widthIn(max = 500.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

      // Hero Header
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        // App Icon Container with official Dyno Lite logo
        Surface(
          modifier = Modifier.size(84.dp),
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          tonalElevation = 2.dp,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        ) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            Image(
              painter = painterResource(id = R.drawable.ic_dyno_logo),
              contentDescription = stringResource(R.string.app_name),
              modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(18.dp)),
              contentScale = ContentScale.Fit
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "DYNO LITE",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontSize = 26.sp
          ),
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.testTag("home_title")
        )

        Text(
          text = "Descubra o desempenho estimado do seu carro",
          style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 15.sp,
            lineHeight = 20.sp
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 4.dp).testTag("home_subtitle")
        )
      }

      if (primaryVehicle == null) {
        // Empty State Card
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_vehicle_card"),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
          ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.DirectionsCar,
              contentDescription = null,
              modifier = Modifier.size(48.dp),
              tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )

            Text(
              text = "Nenhum veículo cadastrado",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )

            Text(
              text = "Cadastre seu carro para configurar os dados de peso, pneu e câmbio necessários para a medição.",
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )

            Button(
              onClick = onNavigateToWizard,
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_register_first_car"),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              )
            ) {
              Text(
                text = "CADASTRAR MEU CARRO",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.8.sp,
                  fontSize = 14.sp
                )
              )
            }
          }
        }
      } else {
        // Vehicle Summary Card
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("primary_vehicle_card"),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                Icon(
                  imageVector = Icons.Default.DirectionsCar,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(22.dp)
                )
                Text(
                  text = "MEU CARRO",
                  style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    fontSize = 13.sp
                  ),
                  color = MaterialTheme.colorScheme.primary
                )
              }

              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
              ) {
                Text(
                  text = "Principal",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                  ),
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
              }
            }

            HorizontalDivider(
              thickness = 0.8.dp,
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Vehicle Title
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "${primaryVehicle.manufacturer} ${primaryVehicle.model}",
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "${primaryVehicle.year} • ${primaryVehicle.engine} ${primaryVehicle.version}".trim(),
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            // Specs Row
            val totalWeight = VehicleCalculations.calculateTotalWeight(
              curbWeightKg = primaryVehicle.curbWeightKg,
              driverWeightKg = primaryVehicle.driverWeightKg,
              passengerWeightKg = primaryVehicle.passengerWeightKg,
              cargoWeightKg = primaryVehicle.cargoWeightKg,
              audioWeightKg = primaryVehicle.audioWeightKg,
              gnvWeightKg = primaryVehicle.gnvWeightKg,
              otherWeightKg = primaryVehicle.otherWeightKg,
              removedWeightKg = primaryVehicle.removedWeightKg,
              measuredTotalWeightKg = primaryVehicle.measuredTotalWeightKg,
              useMeasuredWeight = primaryVehicle.useMeasuredWeight
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text(
                  text = "Peso Total Est.",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                  text = String.format(Locale.US, "%.0f kg", totalWeight),
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  ),
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
                  text = "${primaryVehicle.tireWidthMm}/${primaryVehicle.tireAspectRatio} R${primaryVehicle.wheelDiameterInches}",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  ),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              if (primaryVehicle.factoryPowerCv != null) {
                Column {
                  Text(
                    text = "Potência Orig.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                  )
                  Text(
                    text = String.format(Locale.US, "%.0f cv", primaryVehicle.factoryPowerCv),
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }

            HorizontalDivider(
              thickness = 0.8.dp,
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Last test status
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.Speed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "Último teste: Nenhum teste realizado",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 12.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
              )
            }
          }
        }

        // Action Buttons for Configured Vehicle
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Button(
            onClick = onNavigateToTestPrep,
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("btn_discover_power"),
            shape = RoundedCornerShape(14.dp),
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
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "DESCOBRIR A POTÊNCIA",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                fontSize = 14.sp
              )
            )
          }

          FilledTonalButton(
            onClick = onNavigateToGarage,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("btn_view_vehicle_data"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
          ) {
            Text(
              text = "VER DADOS DO VEÍCULO",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
              )
            )
          }
        }
      }

      // Secondary Helpful Links
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextButton(
          onClick = onNavigateToGuide,
          modifier = Modifier.testTag("btn_accuracy_guide_home")
        ) {
          Icon(
            imageVector = Icons.Outlined.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Guia do teste",
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.Medium,
              fontSize = 12.5.sp
            )
          )
        }

        TextButton(
          onClick = { showHowItWorksDialog = true },
          modifier = Modifier.testTag("btn_how_it_works")
        ) {
          Icon(
            imageVector = Icons.Outlined.HelpOutline,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Como funciona",
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.Medium,
              fontSize = 12.5.sp
            )
          )
        }

        TextButton(
          onClick = onNavigateToSettings,
          modifier = Modifier.testTag("btn_quick_settings")
        ) {
          Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Configurações",
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.Medium,
              fontSize = 12.5.sp
            )
          )
        }
      }
    }
  }

  if (showHowItWorksDialog) {
    AlertDialog(
      onDismissRequest = { showHowItWorksDialog = false },
      title = {
        Text(
          text = "Como funciona o Dyno Lite",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      },
      text = {
        Column(
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "1. Cadastre os dados reais do seu veículo (peso, pneu e câmbio).",
            style = MaterialTheme.typography.bodyMedium
          )
          Text(
            text = "2. Fixe o celular no suporte veicular alinhado na vertical voltado para a frente.",
            style = MaterialTheme.typography.bodyMedium
          )
          Text(
            text = "3. Ao atingir 30 km/h, o teste inicia automaticamente e mede a aceleração longitudinal precisa do veículo.",
            style = MaterialTheme.typography.bodyMedium
          )
          Text(
            text = "Aviso: Sempre realize os testes em local seguro e fechado, com o auxílio de um passageiro.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { showHowItWorksDialog = false }) {
          Text("ENTENDI")
        }
      }
    )
  }
}
