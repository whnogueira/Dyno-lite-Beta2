package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RunResultRepository
import com.example.model.RunResult
import com.example.model.finiteOrDefault
import com.example.ui.components.DynoMetricCard
import com.example.ui.theme.DynoBg
import com.example.ui.theme.DynoCardBg
import com.example.ui.theme.DynoCardBorder
import com.example.ui.theme.DynoCardSurface
import com.example.ui.theme.DynoErrorRed
import com.example.ui.theme.DynoPowerCyan
import com.example.ui.theme.DynoRed
import com.example.ui.theme.DynoTextPrimary
import com.example.ui.theme.DynoTextSecondary
import com.example.ui.theme.DynoTorqueAmber
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    runResultRepository: RunResultRepository,
    resultId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var result by remember { mutableStateOf<RunResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(resultId) {
        if (resultId != null) {
            result = runResultRepository.getResultById(resultId)
        }
        isLoading = false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DynoBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "RESULTADO DA PASSADA",
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
                actions = {
                    result?.let { r ->
                        IconButton(onClick = {
                            coroutineScope.launch {
                                runResultRepository.deleteResult(r.id)
                                onNavigateBack()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = DynoErrorRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DynoBg)
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DynoPowerCyan)
            }
        } else if (result == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Resultado não encontrado", color = DynoTextSecondary)
            }
        } else {
            val r = result!!
            val scrollState = rememberScrollState()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Se o resultado foi recuperado de forma parcial com aviso
                if (r.qualityStatus != "VALID" || r.technicalFailureReason != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DynoCardSurface),
                        border = BorderStroke(1.dp, DynoTorqueAmber)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = DynoTorqueAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = r.technicalFailureReason ?: "Resultado processado com telemetria preservada.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DynoTorqueAmber
                            )
                        }
                    }
                }

                // Cabeçalho da Passada
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DynoCardBg),
                    border = BorderStroke(1.dp, DynoCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = r.vehicleName.ifBlank { "Veículo Dyno" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = DynoTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dateFormat.format(Date(r.testDateTimestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = DynoTextSecondary
                        )
                    }
                }

                // Métricas Principais de Potência e Torque
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DynoMetricCard(
                        title = "Potência Máxima",
                        value = String.format(Locale.US, "%.0f", r.peakEnginePowerCv?.finiteOrDefault(0f) ?: 0f),
                        unit = "cv",
                        accentColor = DynoPowerCyan,
                        modifier = Modifier.weight(1f)
                    )
                    DynoMetricCard(
                        title = "Torque Máximo",
                        value = String.format(Locale.US, "%.1f", r.peakEngineTorqueKgm?.finiteOrDefault(0f) ?: 0f),
                        unit = "kgfm",
                        accentColor = DynoTorqueAmber,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DynoMetricCard(
                        title = "Potência na Roda",
                        value = String.format(Locale.US, "%.0f", r.peakWheelPowerCv?.finiteOrDefault(0f) ?: 0f),
                        unit = "whp",
                        accentColor = DynoRed,
                        modifier = Modifier.weight(1f)
                    )
                    DynoMetricCard(
                        title = "Aceleração Máx",
                        value = String.format(Locale.US, "%.2f", r.peakLongitudinalG?.finiteOrDefault(0f) ?: 0f),
                        unit = "G",
                        accentColor = DynoTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Gráfico das Curvas da Passada
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DynoCardBg),
                    border = BorderStroke(1.dp, DynoCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "GRÁFICO DA PASSADA (DINAMÔMETRO REAL)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = DynoTextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(DynoCardSurface, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            val samples = r.samples
                            if (samples.isNotEmpty()) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val maxPower = (samples.maxOfOrNull { it.enginePowerCv.finiteOrDefault(0f) } ?: 100f).coerceAtLeast(50f)
                                    val maxTorque = (samples.maxOfOrNull { it.engineTorqueKgm.finiteOrDefault(0f) } ?: 20f).coerceAtLeast(10f)

                                    val powerPath = Path()
                                    val torquePath = Path()

                                    samples.forEachIndexed { index, pt ->
                                        val x = (index.toFloat() / (samples.size - 1).coerceAtLeast(1)) * size.width
                                        val pwr = pt.enginePowerCv.finiteOrDefault(0f)
                                        val trq = pt.engineTorqueKgm.finiteOrDefault(0f)
                                        val yPower = size.height - ((pwr / maxPower) * size.height * 0.9f)
                                        val yTorque = size.height - ((trq / maxTorque) * size.height * 0.9f)

                                        if (index == 0) {
                                            powerPath.moveTo(x, yPower)
                                            torquePath.moveTo(x, yTorque)
                                        } else {
                                            powerPath.lineTo(x, yPower)
                                            torquePath.lineTo(x, yTorque)
                                        }
                                    }

                                    drawPath(path = powerPath, color = DynoPowerCyan, style = Stroke(width = 3.dp.toPx()))
                                    drawPath(path = torquePath, color = DynoTorqueAmber, style = Stroke(width = 3.dp.toPx()))
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DynoPowerCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("VOLTAR AO INÍCIO", color = DynoBg, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
