package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.RunResultRepository
import com.example.data.VehicleRepository
import com.example.model.PendingSession
import com.example.model.Vehicle
import com.example.ui.components.DynoBrandLogo
import com.example.ui.theme.DynoBg
import com.example.ui.theme.DynoCardBg
import com.example.ui.theme.DynoCardBorder
import com.example.ui.theme.DynoCardSurface
import com.example.ui.theme.DynoPowerCyan
import com.example.ui.theme.DynoRed
import com.example.ui.theme.DynoSuccessGreen
import com.example.ui.theme.DynoTextMuted
import com.example.ui.theme.DynoTextPrimary
import com.example.ui.theme.DynoTextSecondary
import com.example.ui.theme.DynoTorqueAmber
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vehicleRepository: VehicleRepository,
    runResultRepository: RunResultRepository? = null,
    onNavigateToTest: () -> Unit,
    onNavigateToSimulation: () -> Unit,
    onNavigateToGarage: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToResults: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vehicles by vehicleRepository.allVehicles.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeVehicle = vehicles.firstOrNull { it.isPrimary } ?: vehicles.firstOrNull()

    val pendingSessions = if (runResultRepository != null) {
        val sessions by runResultRepository.pendingSessions.collectAsStateWithLifecycle(initialValue = emptyList())
        sessions
    } else emptyList()

    var isRecovering by remember { mutableStateOf(false) }
    var selectedDiagnosticSession by remember { mutableStateOf<PendingSession?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DynoBg,
        topBar = {
            TopAppBar(
                title = { DynoBrandLogo() },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações",
                            tint = DynoTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DynoBg)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Banner de Recuperação de Teste Não Finalizado
            if (pendingSessions.isNotEmpty()) {
                val latestPending = pendingSessions.first()
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DynoCardSurface),
                        border = BorderStroke(1.5.dp, DynoTorqueAmber)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = DynoTorqueAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Teste não finalizado (${latestPending.sampleCount} amostras gravadas no banco)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = DynoTorqueAmber
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Não foi possível finalizar a passagem. Os dados foram preservados para recuperação sem perda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DynoTextSecondary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (runResultRepository != null && !isRecovering) {
                                            isRecovering = true
                                            scope.launch {
                                                val recoveryResult = runResultRepository.recoverPendingSession(
                                                    sessionId = latestPending.sessionId,
                                                    vehicle = activeVehicle
                                                )
                                                isRecovering = false
                                                recoveryResult.onSuccess { res ->
                                                    Toast.makeText(context, "Sessão recuperada com sucesso!", Toast.LENGTH_SHORT).show()
                                                    onNavigateToResults(res.id)
                                                }.onFailure { err ->
                                                    Toast.makeText(context, "Falha na recuperação: ${err.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1.2f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DynoPowerCyan,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (isRecovering) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color.Black,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "RECUPERAR",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { selectedDiagnosticSession = latestPending },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, DynoCardBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DynoTextSecondary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "DETALHES",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (runResultRepository != null) {
                                            scope.launch {
                                                runResultRepository.deletePendingSession(latestPending.sessionId)
                                                Toast.makeText(context, "Sessão pendente descartada", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Descartar",
                                        tint = DynoRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Card do Veículo Selecionado
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToGarage() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DynoCardBg),
                    border = BorderStroke(1.dp, DynoCardBorder)
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
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = DynoPowerCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "VEÍCULO ATIVO",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = DynoTextSecondary
                                )
                            }
                            Text(
                                text = "Trocar",
                                style = MaterialTheme.typography.labelSmall,
                                color = DynoPowerCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (activeVehicle != null) {
                            Text(
                                text = activeVehicle.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                color = DynoTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${activeVehicle.brand} • ${activeVehicle.model} • ${activeVehicle.totalMassKg.toInt()} kg",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DynoTextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TagBadge(text = activeVehicle.aspiration.displayName, color = DynoPowerCyan)
                                TagBadge(text = activeVehicle.fuelType.displayName.split(" ")[0], color = DynoTorqueAmber)
                                TagBadge(text = "${activeVehicle.engineDisplacementCc} cc", color = DynoSuccessGreen)
                            }
                        } else {
                            Text(
                                text = "Nenhum veículo cadastrado",
                                style = MaterialTheme.typography.titleMedium,
                                color = DynoTextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Toque para adicionar seu carro na garagem",
                                style = MaterialTheme.typography.bodySmall,
                                color = DynoPowerCyan
                            )
                        }
                    }
                }
            }

            // Ação Principal: MEDIÇÃO REAL EM PISTA
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTest() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DynoCardSurface),
                    border = BorderStroke(1.5.dp, DynoRed)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(DynoRed)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "MODO DINAMÔMETRO REAL",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = DynoRed
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "INICIAR MEDIÇÃO",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp
                                ),
                                color = DynoTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Medição física de potência e torque via GPS e acelerômetro",
                                style = MaterialTheme.typography.bodySmall,
                                color = DynoTextSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(DynoRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Grid de Recursos Secundários (Simulação, Garagem, Histórico)
            item {
                Text(
                    text = "FERRAMENTAS & RECURSOS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = DynoTextSecondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuTileCard(
                        title = "SIMULAÇÃO",
                        subtitle = "Cálculo virtual de potência e turbo",
                        icon = Icons.Default.Calculate,
                        iconTint = DynoPowerCyan,
                        onClick = onNavigateToSimulation,
                        modifier = Modifier.weight(1f)
                    )

                    MenuTileCard(
                        title = "GARAGEM",
                        subtitle = "Gerenciar veículos e fichas técnicas",
                        icon = Icons.Default.DirectionsCar,
                        iconTint = DynoTorqueAmber,
                        onClick = onNavigateToGarage,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuTileCard(
                        title = "HISTÓRICO",
                        subtitle = "Passadas gravadas e telemetria",
                        icon = Icons.Default.History,
                        iconTint = DynoSuccessGreen,
                        onClick = onNavigateToHistory,
                        modifier = Modifier.weight(1f)
                    )

                    MenuTileCard(
                        title = "AJUSTES",
                        subtitle = "Calibração e padrões de cálculo",
                        icon = Icons.Default.Settings,
                        iconTint = DynoTextSecondary,
                        onClick = onNavigateToSettings,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Modal de Detalhes de Diagnóstico Avançado
    selectedDiagnosticSession?.let { session ->
        AlertDialog(
            onDismissRequest = { selectedDiagnosticSession = null },
            title = {
                Text(
                    text = "Diagnóstico da Sessão",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DynoTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagnosticRow(label = "Session ID:", value = session.sessionId)
                    DiagnosticRow(label = "Veículo:", value = session.vehicleName)
                    DiagnosticRow(label = "Amostras Gravadas:", value = "${session.sampleCount}")
                    DiagnosticRow(label = "Etapa com Falha:", value = session.errorStage ?: "Não especificada")
                    DiagnosticRow(label = "Exceção:", value = session.errorExceptionType ?: "Nenhuma")
                    DiagnosticRow(label = "Detalhe do Erro:", value = session.errorMessage ?: "Sem mensagem adicional")
                    if (session.invalidField != null) {
                        DiagnosticRow(label = "Campo Inválido:", value = session.invalidField)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sess = selectedDiagnosticSession
                        selectedDiagnosticSession = null
                        if (sess != null && runResultRepository != null) {
                            isRecovering = true
                            scope.launch {
                                val recoveryResult = runResultRepository.recoverPendingSession(
                                    sessionId = sess.sessionId,
                                    vehicle = activeVehicle
                                )
                                isRecovering = false
                                recoveryResult.onSuccess { res ->
                                    Toast.makeText(context, "Sessão recuperada com sucesso!", Toast.LENGTH_SHORT).show()
                                    onNavigateToResults(res.id)
                                }.onFailure { err ->
                                    Toast.makeText(context, "Falha na recuperação: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DynoPowerCyan, contentColor = Color.Black)
                ) {
                    Text("Tentar Recuperar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDiagnosticSession = null }) {
                    Text("Fechar", color = DynoTextSecondary)
                }
            },
            containerColor = DynoCardBg
        )
    }
}

@Composable
fun DiagnosticRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = DynoTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = DynoTextPrimary
        )
    }
}

@Composable
fun TagBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
    }
}

@Composable
fun MenuTileCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DynoCardBg),
        border = BorderStroke(1.dp, DynoCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = DynoTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = DynoTextSecondary,
                    maxLines = 2
                )
            }
        }
    }
}
