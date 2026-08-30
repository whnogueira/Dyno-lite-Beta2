package com.example.ui.screens

import android.app.Application
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SimulationRepository
import com.example.data.VehicleRepository
import com.example.data.db.DynoMobileDatabase
import com.example.model.AspirationType
import com.example.model.FuelType
import com.example.model.SimulationEngine
import com.example.model.SimulationUiState
import com.example.ui.components.DynoMetricCard
import com.example.ui.theme.DynoBg
import com.example.ui.theme.DynoCardBg
import com.example.ui.theme.DynoCardBorder
import com.example.ui.theme.DynoCardSurface
import com.example.ui.theme.DynoErrorRed
import com.example.ui.theme.DynoPowerCyan
import com.example.ui.theme.DynoRed
import com.example.ui.theme.DynoSuccessGreen
import com.example.ui.theme.DynoTextMuted
import com.example.ui.theme.DynoTextPrimary
import com.example.ui.theme.DynoTextSecondary
import com.example.ui.theme.DynoTorqueAmber
import com.example.ui.theme.DynoWarningYellow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class SimulationViewModel(
    application: Application,
    private val simulationRepository: SimulationRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        SimulationUiState(
            isLoading = true,
            engineDisplacementCc = 2000,
            aspiration = AspirationType.NATURALLY_ASPIRATED,
            boostBar = 0.0f,
            injectorFlowLbH = 28.0f,
            fuelType = FuelType.ETHANOL,
            estimatedPowerCv = null,
            errorMessage = null
        )
    )
    val uiState: StateFlow<SimulationUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val state = simulationRepository.getInitialSimulationState()
                _uiState.value = state
            } catch (e: Exception) {
                // Fallback seguro com valores padrão, nunca propaga crash
                val fallbackState = SimulationEngine.simulate(
                    displacementCc = 2000,
                    cylinderCount = 4,
                    aspiration = AspirationType.NATURALLY_ASPIRATED,
                    boostBar = 0f,
                    injectorFlowLbH = 28f,
                    fuelType = FuelType.ETHANOL
                )
                _uiState.value = fallbackState.copy(
                    vehicleName = "Simulação Manual Padrão",
                    errorMessage = null
                )
            }
        }
    }

    fun updateDisplacement(displacementCc: Int) {
        val current = _uiState.value
        recalculate(
            displacementCc = displacementCc,
            cylinderCount = current.cylinderCount,
            aspiration = current.aspiration,
            boostBar = current.boostBar,
            injectorFlowLbH = current.injectorFlowLbH,
            fuelType = current.fuelType
        )
    }

    fun updateAspiration(aspiration: AspirationType) {
        val current = _uiState.value
        val defaultBoost = if (aspiration == AspirationType.TURBOCHARGED) 1.0f else 0.0f
        recalculate(
            displacementCc = current.engineDisplacementCc,
            cylinderCount = current.cylinderCount,
            aspiration = aspiration,
            boostBar = defaultBoost,
            injectorFlowLbH = current.injectorFlowLbH,
            fuelType = current.fuelType
        )
    }

    fun updateBoost(boostBar: Float) {
        val current = _uiState.value
        recalculate(
            displacementCc = current.engineDisplacementCc,
            cylinderCount = current.cylinderCount,
            aspiration = current.aspiration,
            boostBar = boostBar,
            injectorFlowLbH = current.injectorFlowLbH,
            fuelType = current.fuelType
        )
    }

    fun updateInjectors(injectorFlowLbH: Float) {
        val current = _uiState.value
        recalculate(
            displacementCc = current.engineDisplacementCc,
            cylinderCount = current.cylinderCount,
            aspiration = current.aspiration,
            boostBar = current.boostBar,
            injectorFlowLbH = injectorFlowLbH,
            fuelType = current.fuelType
        )
    }

    fun updateFuelType(fuelType: FuelType) {
        val current = _uiState.value
        recalculate(
            displacementCc = current.engineDisplacementCc,
            cylinderCount = current.cylinderCount,
            aspiration = current.aspiration,
            boostBar = current.boostBar,
            injectorFlowLbH = current.injectorFlowLbH,
            fuelType = fuelType
        )
    }

    private fun recalculate(
        displacementCc: Int,
        cylinderCount: Int,
        aspiration: AspirationType,
        boostBar: Float,
        injectorFlowLbH: Float,
        fuelType: FuelType
    ) {
        // Validações antes do cálculo seguro
        if (displacementCc <= 0 || injectorFlowLbH <= 0f || !boostBar.isFinite()) {
            _uiState.update {
                it.copy(errorMessage = "Valores de simulação inválidos.")
            }
            return
        }

        val result = SimulationEngine.simulate(
            displacementCc = displacementCc,
            cylinderCount = cylinderCount,
            aspiration = aspiration,
            boostBar = boostBar,
            injectorFlowLbH = injectorFlowLbH,
            fuelType = fuelType,
            revLimitRpm = _uiState.value.targetRpm
        )

        _uiState.update { current ->
            result.copy(
                selectedVehicleId = current.selectedVehicleId,
                vehicleName = current.vehicleName,
                errorMessage = null
            )
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = DynoMobileDatabase.getDatabase(application)
            val vehicleRepo = VehicleRepository(db.vehicleDao())
            val simulationRepo = SimulationRepository(vehicleRepo)
            return SimulationViewModel(application, simulationRepo) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext as Application
    val viewModel: SimulationViewModel = viewModel(
        factory = SimulationViewModel.Factory(context)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    var displacementText by remember(uiState.engineDisplacementCc) {
        mutableStateOf(uiState.engineDisplacementCc.toString())
    }
    var injectorText by remember(uiState.injectorFlowLbH) {
        mutableStateOf(uiState.injectorFlowLbH.toInt().toString())
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DynoBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SIMULADOR DINAMOMÉTRICO",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = DynoTextPrimary
                        )
                        Text(
                            text = uiState.vehicleName,
                            style = MaterialTheme.typography.labelSmall,
                            color = DynoPowerCyan
                        )
                    }
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
                    IconButton(onClick = { viewModel.loadInitialData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recarregar",
                            tint = DynoTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DynoBg)
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DynoPowerCyan)
            }
        } else if (uiState.errorMessage != null && uiState.estimatedPowerCv == null) {
            // Fallback visual com ações seguras
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Erro",
                    tint = DynoWarningYellow,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Não foi possível carregar os dados da simulação.",
                    style = MaterialTheme.typography.titleMedium,
                    color = DynoTextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.loadInitialData() },
                    colors = ButtonDefaults.buttonColors(containerColor = DynoPowerCyan)
                ) {
                    Text("TENTAR NOVAMENTE", color = DynoBg, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(containerColor = DynoCardSurface)
                ) {
                    Text("VOLTAR AO INÍCIO", color = DynoTextPrimary)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Métricas Principais Estimadas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DynoMetricCard(
                        title = "Potência Motor",
                        value = uiState.estimatedPowerCv?.let { String.format(Locale.US, "%.0f", it) } ?: "--",
                        unit = "cv",
                        accentColor = DynoPowerCyan,
                        modifier = Modifier.weight(1f)
                    )
                    DynoMetricCard(
                        title = "Torque Motor",
                        value = uiState.estimatedTorqueKgm?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
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
                        title = "Na Roda (Est.)",
                        value = uiState.estimatedWheelPowerCv?.let { String.format(Locale.US, "%.0f", it) } ?: "--",
                        unit = "whp",
                        accentColor = DynoRed,
                        modifier = Modifier.weight(1f)
                    )
                    DynoMetricCard(
                        title = "Duty Bicos",
                        value = uiState.injectorDutyCyclePercent?.let { String.format(Locale.US, "%.0f", it) } ?: "--",
                        unit = "%",
                        accentColor = if ((uiState.injectorDutyCyclePercent ?: 0f) > 85f) DynoErrorRed else DynoSuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Gráfico da Curva Estimada
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DynoCardBg),
                    border = BorderStroke(1.dp, DynoCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CURVA DE POTÊNCIA & TORQUE VIRTUAL",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = DynoTextPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "● Potência (cv)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DynoPowerCyan
                                )
                                Text(
                                    text = "● Torque (kgfm)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DynoTorqueAmber
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(DynoCardSurface, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            val points = uiState.curvePoints
                            if (points.isNotEmpty()) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val maxPower = (points.maxOfOrNull { it.powerCv } ?: 100f).coerceAtLeast(50f)
                                    val maxTorque = (points.maxOfOrNull { it.torqueKgm } ?: 20f).coerceAtLeast(10f)

                                    val powerPath = Path()
                                    val torquePath = Path()

                                    points.forEachIndexed { index, pt ->
                                        val x = (index.toFloat() / (points.size - 1).coerceAtLeast(1)) * size.width
                                        val yPower = size.height - ((pt.powerCv / maxPower) * size.height * 0.9f)
                                        val yTorque = size.height - ((pt.torqueKgm / maxTorque) * size.height * 0.9f)

                                        if (index == 0) {
                                            powerPath.moveTo(x, yPower)
                                            torquePath.moveTo(x, yTorque)
                                        } else {
                                            powerPath.lineTo(x, yPower)
                                            torquePath.lineTo(x, yTorque)
                                        }
                                    }

                                    // Linhas de grade
                                    drawLine(
                                        color = Color(0xFF2B3547),
                                        start = Offset(0f, size.height * 0.5f),
                                        end = Offset(size.width, size.height * 0.5f),
                                        strokeWidth = 1f
                                    )

                                    drawPath(
                                        path = powerPath,
                                        color = DynoPowerCyan,
                                        style = Stroke(width = 3.dp.toPx())
                                    )
                                    drawPath(
                                        path = torquePath,
                                        color = DynoTorqueAmber,
                                        style = Stroke(width = 3.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }

                // Configurações do Motor e Aspiração
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DynoCardBg),
                    border = BorderStroke(1.dp, DynoCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "PARÂMETROS DA CONFIGURAÇÃO",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = DynoTextPrimary
                        )

                        // Aspiração
                        Text(
                            text = "Tipo de Aspiração:",
                            style = MaterialTheme.typography.bodySmall,
                            color = DynoTextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AspirationType.values().forEach { asp ->
                                FilterChip(
                                    selected = uiState.aspiration == asp,
                                    onClick = { viewModel.updateAspiration(asp) },
                                    label = { Text(asp.displayName, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DynoPowerCyan,
                                        selectedLabelColor = DynoBg,
                                        containerColor = DynoCardSurface,
                                        labelColor = DynoTextPrimary
                                    )
                                )
                            }
                        }

                        // Pressão do Turbo (se sobrealimentado)
                        if (uiState.aspiration != AspirationType.NATURALLY_ASPIRATED) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Pressão de Turbo:", style = MaterialTheme.typography.bodySmall, color = DynoTextSecondary)
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", uiState.boostBar)} bar",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = DynoPowerCyan
                                    )
                                }
                                Slider(
                                    value = uiState.boostBar,
                                    onValueChange = { viewModel.updateBoost(it) },
                                    valueRange = 0.0f..3.0f,
                                    steps = 29,
                                    colors = SliderDefaults.colors(
                                        thumbColor = DynoPowerCyan,
                                        activeTrackColor = DynoPowerCyan,
                                        inactiveTrackColor = DynoCardSurface
                                    )
                                )
                            }
                        }

                        // Cilindrada e Bicos
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = displacementText,
                                onValueChange = { newText ->
                                    displacementText = newText
                                    val safeVal = newText.toIntOrNull()
                                    if (safeVal != null && safeVal in 500..10000) {
                                        viewModel.updateDisplacement(safeVal)
                                    }
                                },
                                label = { Text("Cilindrada (cc)", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DynoPowerCyan,
                                    unfocusedBorderColor = DynoCardBorder,
                                    focusedContainerColor = DynoCardSurface,
                                    unfocusedContainerColor = DynoCardSurface
                                )
                            )

                            OutlinedTextField(
                                value = injectorText,
                                onValueChange = { newText ->
                                    injectorText = newText
                                    val safeVal = newText.toFloatOrNull()
                                    if (safeVal != null && safeVal in 10f..250f) {
                                        viewModel.updateInjectors(safeVal)
                                    }
                                },
                                label = { Text("Bicos (lb/h)", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DynoPowerCyan,
                                    unfocusedBorderColor = DynoCardBorder,
                                    focusedContainerColor = DynoCardSurface,
                                    unfocusedContainerColor = DynoCardSurface
                                )
                            )
                        }

                        // Combustível
                        Text(
                            text = "Combustível Utilizado:",
                            style = MaterialTheme.typography.bodySmall,
                            color = DynoTextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(FuelType.ETHANOL, FuelType.GASOLINE, FuelType.PREMIUM_GASOLINE).forEach { fuel ->
                                FilterChip(
                                    selected = uiState.fuelType == fuel,
                                    onClick = { viewModel.updateFuelType(fuel) },
                                    label = { Text(fuel.displayName.split(" ")[0], fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DynoTorqueAmber,
                                        selectedLabelColor = DynoBg,
                                        containerColor = DynoCardSurface,
                                        labelColor = DynoTextPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                // Estimativas de Performance
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DynoCardBg),
                    border = BorderStroke(1.dp, DynoCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("0 a 100 km/h (Est.)", style = MaterialTheme.typography.bodySmall, color = DynoTextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.estimatedZeroToHundredSec?.let { "${String.format(Locale.US, "%.1f", it)} s" } ?: "--",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = DynoTextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("1/4 de Milha (402m)", style = MaterialTheme.typography.bodySmall, color = DynoTextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.estimatedQuarterMileSec?.let { "${String.format(Locale.US, "%.1f", it)} s" } ?: "--",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = DynoTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
