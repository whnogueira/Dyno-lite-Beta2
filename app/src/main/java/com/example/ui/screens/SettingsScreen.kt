package com.example.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.RunResultRepository
import com.example.ui.theme.DynoErrorRed
import com.example.ui.theme.DynoSuccessGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  onNavigateToSensorDiagnostic: () -> Unit,
  onNavigateToHowItWorks: () -> Unit,
  onNavigateToGuide: () -> Unit = {},
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val runResultRepository = remember { RunResultRepository(context) }
  val coroutineScope = rememberCoroutineScope()
  var selfTestMessage by remember { mutableStateOf<String?>(null) }
  var isTestingStorage by remember { mutableStateOf(false) }
  val prefs = remember(context) {
    context.getSharedPreferences("dyno_lite_prefs", Context.MODE_PRIVATE)
  }

  var defaultDriverWeight by remember {
    mutableFloatStateOf(prefs.getFloat("default_driver_weight", 75f))
  }
  var unitPower by remember {
    mutableStateOf(prefs.getString("unit_power", "cv") ?: "cv")
  }
  var unitTorque by remember {
    mutableStateOf(prefs.getString("unit_torque", "kgfm") ?: "kgfm")
  }
  var unitSpeed by remember {
    mutableStateOf(prefs.getString("unit_speed", "km/h") ?: "km/h")
  }

  var showWeightDialog by remember { mutableStateOf(false) }
  var showUnitsDialog by remember { mutableStateOf(false) }
  var showAboutDialog by remember { mutableStateOf(false) }
  var showPrivacyDialog by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("settings_screen"),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "CONFIGURAÇÕES",
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Section: Personalização & Preferências
        Text(
          text = "PREFERÊNCIAS DO USUÁRIO",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
          ),
          color = MaterialTheme.colorScheme.primary
        )

        // 1. Meu peso padrão
        SettingsItemCard(
          title = "Meu peso padrão",
          subtitle = String.format(Locale.US, "%.0f kg (utilizado como padrão nas passagens)", defaultDriverWeight),
          icon = Icons.Outlined.Person,
          onClick = { showWeightDialog = true },
          testTag = "card_setting_default_weight"
        )

        // 2. Unidades
        SettingsItemCard(
          title = "Unidades de medida",
          subtitle = "Potência ($unitPower), Torque ($unitTorque), Velocidade ($unitSpeed)",
          icon = Icons.Outlined.Tune,
          onClick = { showUnitsDialog = true },
          testTag = "card_setting_units"
        )

        // Section: Ferramentas & Diagnóstico
        Text(
          text = "FERRAMENTAS & DIAGNÓSTICO",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
          ),
          color = MaterialTheme.colorScheme.primary
        )

        // 3. Diagnóstico avançado
        SettingsItemCard(
          title = "Diagnóstico de sensores",
          subtitle = "Acelerômetro bruto, eixos X/Y/Z, calibração manual, giroscópio, GPS e passagem experimental.",
          icon = Icons.Outlined.Sensors,
          onClick = onNavigateToSensorDiagnostic,
          testTag = "card_sensor_diagnostic"
        )

        // 3.1 Testar Armazenamento
        SettingsItemCard(
          title = "Diagnóstico do banco de dados",
          subtitle = "Executar teste de ciclo completo de gravação, persistência e leitura no Room (DynoMobileDB).",
          icon = Icons.Outlined.Storage,
          onClick = {
            coroutineScope.launch {
              isTestingStorage = true
              val (ok, msg) = runResultRepository.runStorageSelfTest()
              isTestingStorage = false
              selfTestMessage = msg
            }
          },
          testTag = "card_storage_diagnostic"
        )

        // Section: Informações & Ajuda
        Text(
          text = "INFORMAÇÕES & AJUDA",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
          ),
          color = MaterialTheme.colorScheme.primary
        )

        // 4. Guia inicial / Como melhorar a precisão
        SettingsItemCard(
          title = "Como melhorar a precisão",
          subtitle = "Fixação do suporte, calibração parado e aceleração em marcha única.",
          icon = Icons.Outlined.Speed,
          onClick = onNavigateToGuide,
          testTag = "card_accuracy_guide"
        )

        // 5. Como fazer o teste / Como funciona
        SettingsItemCard(
          title = "Como fazer o teste",
          subtitle = "Passo a passo completo do teste inercial do início ao fim.",
          icon = Icons.Outlined.HelpOutline,
          onClick = onNavigateToHowItWorks,
          testTag = "card_how_it_works"
        )

        // 6. Sobre o aplicativo
        SettingsItemCard(
          title = "Sobre o aplicativo",
          subtitle = "Dyno Lite v0.19.0 — Dinamômetro Inercial Móvel para Android",
          icon = Icons.Outlined.Info,
          onClick = { showAboutDialog = true },
          testTag = "card_about_app"
        )

        // 7. Privacidade
        SettingsItemCard(
          title = "Privacidade e dados",
          subtitle = "Armazenamento 100% local no dispositivo, sem rastreamento ou telemetria.",
          icon = Icons.Outlined.Security,
          onClick = { showPrivacyDialog = true },
          testTag = "card_privacy"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Version Badge
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.testTag("version_badge")
          ) {
            Text(
              text = "VERSÃO 0.19.0",
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Text(
            text = "Processamento inercial com fusão sensorial e GPS",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
        }
      }
    }
  }

  // Dialog: Meu peso padrão
  if (showWeightDialog) {
    var weightInput by remember { mutableStateOf(defaultDriverWeight.toInt().toString()) }
    AlertDialog(
      onDismissRequest = { showWeightDialog = false },
      title = {
        Text(
          text = "Meu peso padrão",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Informe o seu peso corporal em kg. Este valor será sugerido automaticamente como peso do motorista na preparação de cada teste.",
            style = MaterialTheme.typography.bodyMedium
          )
          OutlinedTextField(
            value = weightInput,
            onValueChange = { weightInput = it.filter { char -> char.isDigit() } },
            label = { Text("Peso do motorista (kg)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("input_default_weight")
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val parsed = weightInput.toFloatOrNull() ?: 75f
            val valid = parsed.coerceIn(30f, 250f)
            defaultDriverWeight = valid
            prefs.edit().putFloat("default_driver_weight", valid).apply()
            showWeightDialog = false
          }
        ) {
          Text("SALVAR")
        }
      },
      dismissButton = {
        TextButton(onClick = { showWeightDialog = false }) {
          Text("CANCELAR")
        }
      }
    )
  }

  // Dialog: Unidades
  if (showUnitsDialog) {
    var tempPower by remember { mutableStateOf(unitPower) }
    var tempTorque by remember { mutableStateOf(unitTorque) }
    var tempSpeed by remember { mutableStateOf(unitSpeed) }

    AlertDialog(
      onDismissRequest = { showUnitsDialog = false },
      title = {
        Text(
          text = "Unidades de medida",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Text(
            text = "Potência:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("cv", "hp", "kW").forEach { u ->
              Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = tempPower == u, onClick = { tempPower = u })
                Text(u, style = MaterialTheme.typography.bodyMedium)
              }
            }
          }

          Text(
            text = "Torque:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("kgfm", "Nm").forEach { u ->
              Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = tempTorque == u, onClick = { tempTorque = u })
                Text(u, style = MaterialTheme.typography.bodyMedium)
              }
            }
          }

          Text(
            text = "Velocidade:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("km/h", "mph").forEach { u ->
              Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = tempSpeed == u, onClick = { tempSpeed = u })
                Text(u, style = MaterialTheme.typography.bodyMedium)
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            unitPower = tempPower
            unitTorque = tempTorque
            unitSpeed = tempSpeed
            prefs.edit()
              .putString("unit_power", tempPower)
              .putString("unit_torque", tempTorque)
              .putString("unit_speed", tempSpeed)
              .apply()
            showUnitsDialog = false
          }
        ) {
          Text("SALVAR")
        }
      },
      dismissButton = {
        TextButton(onClick = { showUnitsDialog = false }) {
          Text("CANCELAR")
        }
      }
    )
  }

  // Dialog: Sobre o aplicativo
  if (showAboutDialog) {
    AlertDialog(
      onDismissRequest = { showAboutDialog = false },
      title = {
        Text(
          text = "Sobre o Dyno Lite",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Dyno Lite v0.19.0",
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          )
          Text(
            text = "O Dyno Lite é um aplicativo de dinamômetro inercial veicular para Android. Ele utiliza a fusão de sensores inerciais de alta precisão (acelerômetro no eixo longitudinal Z e giroscópio) com o GPS para estimar com rigor a curva de aceleração e desempenho do seu veículo.",
            style = MaterialTheme.typography.bodyMedium
          )
          Text(
            text = "Catálogo brasileiro integrado com Volkswagen, GM/Chevrolet Família 1 e 2, transmissões conhecidas e suporte a perfis personalizados.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      },
      confirmButton = {
        Button(onClick = { showAboutDialog = false }) {
          Text("FECHAR")
        }
      }
    )
  }

  // Dialog: Privacidade
  if (showPrivacyDialog) {
    AlertDialog(
      onDismissRequest = { showPrivacyDialog = false },
      title = {
        Text(
          text = "Privacidade e Segurança",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "100% Offline e Privado",
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          )
          Text(
            text = "Todos os dados dos seus veículos, calibragens, leituras de sensores inerciais, trajetos GPS e resultados de passagens são armazenados e processados exclusivamente na memória local do seu aparelho celular.",
            style = MaterialTheme.typography.bodyMedium
          )
          Text(
            text = "Nenhum dado é transmitido para servidores de terceiros ou coletado para fins publicitários.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      },
      confirmButton = {
        Button(onClick = { showPrivacyDialog = false }) {
          Text("ENTENDI")
        }
      }
    )
  }

  // Dialog: Resultado do Teste de Armazenamento
  if (selfTestMessage != null) {
    val isSuccess = selfTestMessage?.startsWith("SUCESSO") == true
    AlertDialog(
      onDismissRequest = { selfTestMessage = null },
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = if (isSuccess) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (isSuccess) DynoSuccessGreen else DynoErrorRed
          )
          Text(
            text = "Diagnóstico de Armazenamento",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        }
      },
      text = {
        Text(
          text = selfTestMessage ?: "",
          style = MaterialTheme.typography.bodyMedium
        )
      },
      confirmButton = {
        Button(onClick = { selfTestMessage = null }) {
          Text("FECHAR")
        }
      }
    )
  }
}

@Composable
private fun SettingsItemCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag(testTag),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
  ) {
    Row(
      modifier = Modifier.padding(18.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Surface(
        modifier = Modifier.size(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer
      ) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
          )
        }
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}
