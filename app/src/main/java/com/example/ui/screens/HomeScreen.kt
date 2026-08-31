package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VehicleDatabase
import com.example.model.RunResult
import com.example.model.VehicleCalculations
import com.example.model.VehicleProfile
import com.example.ui.components.DynoBadgeStatus
import com.example.ui.components.DynoCard
import com.example.ui.components.DynoLogo
import com.example.ui.components.DynoPrimaryButton
import com.example.ui.components.DynoSecondaryButton
import com.example.ui.components.DynoStatusBadge
import com.example.ui.theme.DynoBlueLight
import com.example.ui.theme.DynoBluePrimary
import com.example.ui.theme.DynoDivider
import com.example.ui.theme.DynoErrorRed
import com.example.ui.theme.DynoPowerCyan
import com.example.ui.theme.DynoSuccessGreen
import com.example.ui.theme.DynoSurfaceElevated
import com.example.ui.theme.DynoTextPrimary
import com.example.ui.theme.DynoTextSecondary
import com.example.ui.theme.DynoTorqueOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.data.RunResultRepository
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
  primaryVehicle: VehicleProfile?,
  lastRunResult: RunResult? = null,
  feedbackMessage: String? = null,
  onDismissFeedback: () -> Unit = {},
  onNavigateToWizard: () -> Unit,
  onNavigateToGarage: () -> Unit,
  onNavigateToTestPrep: () -> Unit,
  onNavigateToSettings: () -> Unit,
  onNavigateToGuide: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val runResultRepository = remember { RunResultRepository(context) }
  val coroutineScope = rememberCoroutineScope()
  var showHowItWorksDialog by remember { mutableStateOf(false) }
  var incompleteTestToRecover by remember { mutableStateOf<RunResult?>(null) }

  // Detecta testes não finalizados no banco ao abrir a tela (Requisito 7)
  LaunchedEffect(Unit) {
    val incompleteList = runResultRepository.getIncompleteTests()
    val recoverable = incompleteList.firstOrNull { it.samples.isNotEmpty() }
    if (recoverable != null) {
      incompleteTestToRecover = recoverable
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentAlignment = Alignment.TopCenter
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(top = 8.dp, bottom = 20.dp)
        .widthIn(max = 520.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

      // Hero Header com Logo Oficial
      DynoLogo(
        symbolSize = 58.dp,
        showSubtitle = true,
        subtitleText = "Desempenho do seu carro de forma simples",
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
      )

      // Feedback Message Banner (ex: após término da passagem)
      if (feedbackMessage != null) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("banner_feedback_message"),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (feedbackMessage.contains("Não foi possível", ignoreCase = true))
              DynoErrorRed.copy(alpha = 0.15f)
            else
              DynoSuccessGreen.copy(alpha = 0.15f)
          ),
          border = BorderStroke(
            1.dp,
            if (feedbackMessage.contains("Não foi possível", ignoreCase = true))
              DynoErrorRed.copy(alpha = 0.5f)
            else
              DynoSuccessGreen.copy(alpha = 0.5f)
          )
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = if (feedbackMessage.contains("Não foi possível", ignoreCase = true))
                Icons.Default.Close
              else
                Icons.Default.Check,
              contentDescription = null,
              tint = if (feedbackMessage.contains("Não foi possível", ignoreCase = true))
                DynoErrorRed
              else
                DynoSuccessGreen,
              modifier = Modifier.size(22.dp)
            )
            Text(
              text = feedbackMessage,
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp
              ),
              color = DynoTextPrimary,
              modifier = Modifier.weight(1f)
            )
            IconButton(
              onClick = onDismissFeedback,
              modifier = Modifier.size(24.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Fechar aviso",
                tint = DynoTextSecondary,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }

      if (primaryVehicle == null) {
        // -------------------------------------------------------------
        // ESTADO SEM VEÍCULO
        // -------------------------------------------------------------
        DynoCard(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_vehicle_card")
        ) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Surface(
              modifier = Modifier.size(54.dp),
              shape = CircleShape,
              color = DynoSurfaceElevated
            ) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Outlined.DirectionsCar,
                  contentDescription = null,
                  modifier = Modifier.size(28.dp),
                  tint = DynoBlueLight
                )
              }
            }

            Text(
              text = "Nenhum veículo cadastrado",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
              ),
              color = DynoTextPrimary
            )

            Text(
              text = "Cadastre seu carro para configurar os dados de peso, pneu e câmbio necessários para a medição.",
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp
              ),
              color = DynoTextSecondary,
              textAlign = TextAlign.Center
            )

            DynoPrimaryButton(
              text = "CADASTRAR MEU CARRO",
              onClick = onNavigateToWizard,
              icon = Icons.Default.DirectionsCar,
              modifier = Modifier.fillMaxWidth(),
              testTag = "btn_register_first_car"
            )
          }
        }

        // Cartão: Como funciona
        DynoCard(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("card_how_it_works_steps")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.HelpOutline,
              contentDescription = null,
              tint = DynoBlueLight,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = "Como funciona o Dyno Lite:",
              style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp
              ),
              color = DynoTextPrimary
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(thickness = 0.8.dp, color = DynoDivider)
          Spacer(modifier = Modifier.height(10.dp))

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeStepItem(number = "1", text = "Cadastre o veículo no catálogo ou de forma personalizada.")
            HomeStepItem(number = "2", text = "Confirme o peso real com ocupantes e cargas.")
            HomeStepItem(number = "3", text = "Prepare o teste e fixe o celular na vertical voltado para a frente.")
            HomeStepItem(number = "4", text = "O teste inicia automaticamente ao atingir a velocidade selecionada.")
            HomeStepItem(number = "5", text = "Veja e compare os resultados de potência e torque.")
          }
        }

      } else {
        // -------------------------------------------------------------
        // ESTADO COM VEÍCULO
        // -------------------------------------------------------------
        val transmissionLabel = when {
          primaryVehicle.transmissionId != null ->
            VehicleDatabase.getTransmission(primaryVehicle.transmissionId)?.displayName ?: "Original"
          !primaryVehicle.customTransmissionName.isNullOrBlank() ->
            primaryVehicle.customTransmissionName
          else -> "Original"
        }

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

        val isDataComplete = primaryVehicle.tireWidthMm > 0 &&
          primaryVehicle.tireAspectRatio > 0 &&
          primaryVehicle.wheelDiameterInches > 0 &&
          totalWeight > 300f

        val (dataStatusLabel, dataBadgeStatus) = when {
          primaryVehicle.useMeasuredWeight -> Pair("Verificado", DynoBadgeStatus.SUCCESS)
          isDataComplete -> Pair("Conferido", DynoBadgeStatus.INFO)
          else -> Pair("Parcial", DynoBadgeStatus.WARNING)
        }

        // Cartão do Veículo Principal
        DynoCard(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("primary_vehicle_card")
        ) {
          // Cabeçalho do Cartão
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
                tint = DynoBluePrimary,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = "VEÍCULO PRINCIPAL",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.8.sp,
                  fontSize = 11.5.sp
                ),
                color = DynoBlueLight
              )
            }

            DynoStatusBadge(text = dataStatusLabel, status = dataBadgeStatus)
          }

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(thickness = 0.8.dp, color = DynoDivider)
          Spacer(modifier = Modifier.height(10.dp))

          // Título do Veículo
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "${primaryVehicle.manufacturer} ${primaryVehicle.model}",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
              ),
              color = DynoTextPrimary
            )
            val details = listOfNotNull(
              primaryVehicle.year.toString(),
              primaryVehicle.engine.ifBlank { null },
              primaryVehicle.version.ifBlank { null }
            ).joinToString(" • ")
            Text(
              text = details,
              style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
              color = DynoTextSecondary
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Linha de Especificações
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                text = "Peso Total",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = DynoTextSecondary
              )
              Text(
                text = String.format(Locale.US, "%.0f kg", totalWeight),
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                ),
                color = DynoTextPrimary
              )
            }

            Column {
              Text(
                text = "Pneu",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = DynoTextSecondary
              )
              Text(
                text = "${primaryVehicle.tireWidthMm}/${primaryVehicle.tireAspectRatio} R${primaryVehicle.wheelDiameterInches}",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                ),
                color = DynoTextPrimary
              )
            }

            Column {
              Text(
                text = "Câmbio",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = DynoTextSecondary
              )
              Text(
                text = transmissionLabel.take(12),
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                ),
                color = DynoTextPrimary
              )
            }

            if (primaryVehicle.factoryPowerCv != null && primaryVehicle.factoryPowerCv > 0f) {
              Column {
                Text(
                  text = "Potência Orig.",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                  color = DynoTextSecondary
                )
                Text(
                  text = String.format(Locale.US, "%.0f cv", primaryVehicle.factoryPowerCv),
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  ),
                  color = DynoPowerCyan
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(thickness = 0.8.dp, color = DynoDivider)
          Spacer(modifier = Modifier.height(8.dp))

          // Último Teste
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.Speed,
                contentDescription = null,
                tint = DynoBlueLight,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "Último teste:",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontWeight = FontWeight.Medium,
                  fontSize = 12.sp
                ),
                color = DynoTextPrimary
              )
            }

            if (lastRunResult != null) {
              val dateFormat = SimpleDateFormat("dd/MM 'às' HH:mm", Locale.getDefault())
              val formattedDate = dateFormat.format(Date(lastRunResult.timestamp))
              Text(
                text = "Máx: ${String.format(Locale.US, "%.0f km/h", lastRunResult.maximumGpsSpeedKmh)} • $formattedDate",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = DynoTextSecondary
              )
            } else {
              Text(
                text = "Nenhum teste realizado",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = DynoTextSecondary
              )
            }
          }
        }

        // Botões de Ação Principais
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Botão Principal Azul "INICIAR TESTE"
          DynoPrimaryButton(
            text = "INICIAR TESTE",
            onClick = onNavigateToTestPrep,
            icon = Icons.Default.PlayArrow,
            modifier = Modifier.fillMaxWidth(),
            testTag = "btn_discover_power"
          )

          // Botões Secundários
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            DynoSecondaryButton(
              text = "DADOS DO CARRO",
              onClick = onNavigateToGarage,
              icon = Icons.Outlined.DirectionsCar,
              modifier = Modifier.weight(1f),
              testTag = "btn_view_vehicle_data"
            )

            DynoSecondaryButton(
              text = "GUIA DO TESTE",
              onClick = onNavigateToGuide,
              icon = Icons.Outlined.MenuBook,
              modifier = Modifier.weight(1f),
              testTag = "btn_home_guide"
            )
          }
        }
      }

      // Links Secundários de Apoio
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextButton(
          onClick = onNavigateToGuide,
          modifier = Modifier.testTag("btn_accuracy_guide_home")
        ) {
          Icon(
            imageVector = Icons.Outlined.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = DynoBlueLight
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Guia do teste",
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.Medium,
              fontSize = 13.sp
            ),
            color = DynoTextPrimary
          )
        }

        TextButton(
          onClick = { showHowItWorksDialog = true },
          modifier = Modifier.testTag("btn_how_it_works")
        ) {
          Icon(
            imageVector = Icons.Outlined.HelpOutline,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = DynoBlueLight
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Como funciona",
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.Medium,
              fontSize = 13.sp
            ),
            color = DynoTextPrimary
          )
        }
      }
    }
  }

  if (showHowItWorksDialog) {
    AlertDialog(
      onDismissRequest = { showHowItWorksDialog = false },
      containerColor = DynoSurfaceElevated,
      title = {
        Text(
          text = "Como funciona o Dyno Lite",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = DynoTextPrimary
        )
      },
      text = {
        Column(
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "1. Cadastre os dados reais do seu veículo (peso, pneu e câmbio).",
            style = MaterialTheme.typography.bodyMedium,
            color = DynoTextPrimary
          )
          Text(
            text = "2. Fixe o celular no suporte veicular alinhado na vertical voltado para a frente.",
            style = MaterialTheme.typography.bodyMedium,
            color = DynoTextPrimary
          )
          Text(
            text = "3. Ao atingir a velocidade de início selecionada (40, 50 ou 60 km/h), o teste inicia automaticamente e mede a aceleração longitudinal precisa do veículo.",
            style = MaterialTheme.typography.bodyMedium,
            color = DynoTextPrimary
          )
          Text(
            text = "Aviso: Sempre realize os testes em local seguro e fechado, com o auxílio de um passageiro.",
            style = MaterialTheme.typography.bodySmall,
            color = DynoTextSecondary
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { showHowItWorksDialog = false }) {
          Text("ENTENDI", color = DynoBlueLight, fontWeight = FontWeight.Bold)
        }
      }
    )
  }

  // DIÁLOGO DE RECUPERAÇÃO DE TESTE NÃO FINALIZADO (Requisito 7)
  val testToRecover = incompleteTestToRecover
  if (testToRecover != null) {
    AlertDialog(
      onDismissRequest = { /* Não descarta automaticamente sem escolha */ },
      containerColor = DynoSurfaceElevated,
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Outlined.Visibility, contentDescription = null, tint = DynoTorqueOrange)
          Text(
            text = "Teste não finalizado",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = DynoTextPrimary
          )
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Foi encontrado um teste não finalizado (${testToRecover.samples.size} amostras gravadas no banco).",
            style = MaterialTheme.typography.bodyMedium,
            color = DynoTextPrimary
          )
          Text(
            text = "Deseja recuperar esses dados para visualizar no histórico ou descartar a gravação?",
            style = MaterialTheme.typography.bodySmall,
            color = DynoTextSecondary
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            coroutineScope.launch {
              runResultRepository.saveResultSuspending(
                testToRecover.copy(
                  quality = if (testToRecover.samples.size >= 10) "RECUPERADA" else "INVÁLIDA",
                  validSamplesCount = testToRecover.samples.size
                ),
                status = "completed"
              )
              incompleteTestToRecover = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = DynoBluePrimary),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("RECUPERAR")
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = {
            coroutineScope.launch {
              runResultRepository.deleteResultSuspending(testToRecover.id)
              incompleteTestToRecover = null
            }
          },
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("DESCARTAR")
        }
      }
    )
  }
}

@Composable
private fun HomeStepItem(
  number: String,
  text: String,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Surface(
      modifier = Modifier.size(24.dp),
      shape = CircleShape,
      color = DynoSurfaceElevated,
      border = BorderStroke(0.8.dp, DynoBluePrimary.copy(alpha = 0.5f))
    ) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
          text = number,
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.5.sp
          ),
          color = DynoBlueLight
        )
      }
    }
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall.copy(
        fontSize = 12.5.sp,
        lineHeight = 17.sp
      ),
      color = DynoTextSecondary
    )
  }
}
