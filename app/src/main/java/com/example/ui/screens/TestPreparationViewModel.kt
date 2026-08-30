package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.RunResultRepository
import com.example.data.VehicleRepository
import com.example.data.db.DynoMobileDatabase
import com.example.model.DynoRunState
import com.example.model.PendingSession
import com.example.model.RunProcessor
import com.example.model.RunResult
import com.example.model.RunSample
import com.example.model.Vehicle
import com.example.model.finiteOrDefault
import com.example.model.finiteOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class VehicleMotionState {
    STOPPED,
    MOVING
}

data class TestPreparationUiState(
    val activeVehicle: Vehicle? = null,
    val testState: DynoRunState = DynoRunState.PARADO,
    val displaySpeedKmh: Float = 0f,
    val maxDisplaySpeedKmh: Float = 0f,
    val gpsSpeedKmh: Float = 0f,
    val avgGpsSpeedKmh: Float = 0f,
    val integratedSpeedKmh: Float = 0f,
    val longitudinalG: Float = 0f,
    val lateralG: Float = 0f,
    val verticalG: Float = 0f,
    val liveEnginePowerCv: Float = 0f,
    val liveWheelPowerCv: Float = 0f,
    val liveEngineTorqueKgm: Float = 0f,
    val liveRpm: Int = 0,
    val selectedGearIndex: Int = 2,
    val availableGearRatios: List<Float> = listOf(3.78f, 2.12f, 1.46f, 1.03f, 0.86f, 0.73f),
    val gearRatioUsed: Float = 1.46f,
    val isGearValid: Boolean = true,
    val gpsAccuracyMeters: Float = 99f,
    val gpsAgeMillis: Long = 9999L,
    val locationUpdateCount: Int = 0,
    val isGpsProviderEnabled: Boolean = false,
    val hasGpsFix: Boolean = false,
    val isGpsReady: Boolean = false,
    val isCalibrating: Boolean = false,
    val isCalibrated: Boolean = false,
    val calibrationPitchDeg: Float = 0f,
    val calibrationRollDeg: Float = 0f,
    val calibrationYawDeg: Float = 0f,
    val vehicleMotionState: VehicleMotionState = VehicleMotionState.STOPPED,
    val isStoppedForTwoSeconds: Boolean = true,
    val isPhoneStable: Boolean = true,
    val isReadyToArm: Boolean = false,
    val blockingReason: String = "Aguardando GPS",
    val hasPhoneMovedAfterCalib: Boolean = false,
    val startSpeedTriggerKmh: Float = 30.0f,
    val targetMaxSpeedKmh: Float = 140.0f,
    val runElapsedSeconds: Float = 0f,
    val lastCompletedResultId: String? = null,
    val userMessage: String? = null,
    val diagnosticError: String? = null
) {
    val selectedGearNumber: Int
        get() = selectedGearIndex + 1
}

class TestPreparationViewModel(
    application: Application,
    private val vehicleRepository: VehicleRepository,
    private val runResultRepository: RunResultRepository
) : AndroidViewModel(application), SensorEventListener, LocationListener {

    private val TAG = "DynoMobile"
    private val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _uiState = MutableStateFlow(TestPreparationUiState())
    val uiState: StateFlow<TestPreparationUiState> = _uiState.asStateFlow()

    private var rawAccelX = 0f
    private var rawAccelY = 0f
    private var rawAccelZ = 0f
    private var rawGyroX = 0f
    private var rawGyroY = 0f
    private var rawGyroZ = 0f

    private var calibX = 0f
    private var calibY = 0f
    private var calibZ = 9.81f

    private var lastGpsUpdateWallMs = 0L
    private var lastGpsLocation: Location? = null
    private var locationCount = 0

    private val speedMovingAverageBuffer = mutableListOf<Float>()
    private var motionState = VehicleMotionState.STOPPED
    private var consecutiveHighSpeedCount = 0
    private var stoppedStartMs = SystemClock.elapsedRealtime()

    private data class GyroSample(val timestampNs: Long, val mag: Float)
    private val gyroWindow = mutableListOf<GyroSample>()
    private var gyroUnstableStartNs = 0L
    @Volatile private var isPhoneStable = true

    private var displayLoopJob: Job? = null
    private var runStartTimeNs = 0L
    private var currentSessionId: String? = null
    private val samples = mutableListOf<RunSample>()

    init {
        loadActiveVehicle()
        startDisplayLoop()
    }

    private fun loadActiveVehicle() {
        viewModelScope.launch {
            vehicleRepository.allVehicles.collect { list ->
                val primary = list.firstOrNull { it.isPrimary } ?: list.firstOrNull()
                val ratios = primary?.gearRatios?.takeIf { it.isNotEmpty() } ?: listOf(3.78f, 2.12f, 1.46f, 1.03f, 0.86f, 0.73f)

                val preferredIndex = primary?.testGearIndex ?: 2
                val validIndex = if (preferredIndex in ratios.indices) {
                    preferredIndex
                } else if (ratios.size >= 3) {
                    2
                } else {
                    ratios.lastIndex.coerceAtLeast(0)
                }

                val ratio = ratios.getOrNull(validIndex) ?: 1.0f
                val isRatioValid = ratio.isFinite() && ratio > 0f

                _uiState.update { current ->
                    current.copy(
                        activeVehicle = primary,
                        availableGearRatios = ratios,
                        selectedGearIndex = validIndex,
                        gearRatioUsed = ratio,
                        isGearValid = isRatioValid
                    )
                }
            }
        }
    }

    fun selectTestGear(index: Int) {
        if (_uiState.value.testState != DynoRunState.PARADO) return

        val veh = _uiState.value.activeVehicle
        val ratios = veh?.gearRatios ?: _uiState.value.availableGearRatios
        if (index !in ratios.indices) return

        val ratio = ratios.getOrNull(index) ?: 1.0f
        val isRatioValid = ratio.isFinite() && ratio > 0f

        _uiState.update {
            it.copy(
                selectedGearIndex = index,
                gearRatioUsed = ratio,
                isGearValid = isRatioValid
            )
        }

        if (veh != null && veh.testGearIndex != index) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    vehicleRepository.insertOrUpdateVehicle(veh.copy(testGearIndex = index))
                } catch (e: Exception) {
                    Log.w(TAG, "Falha ao salvar preferência de marcha: ${e.message}")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startSensorsAndGps() {
        try {
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
            }
            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
            }

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0L,
                    0f,
                    this
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao registrar sensores/GPS: ${e.message}", e)
        }
    }

    fun stopSensorsAndGps() {
        try {
            sensorManager.unregisterListener(this)
            locationManager.removeUpdates(this)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao desregistrar sensores/GPS: ${e.message}", e)
        }
    }

    fun calibratePhoneMount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCalibrating = true) }
            delay(1200)
            calibX = rawAccelX
            calibY = rawAccelY
            calibZ = rawAccelZ
            _uiState.update {
                it.copy(
                    isCalibrating = false,
                    isCalibrated = true,
                    hasPhoneMovedAfterCalib = false
                )
            }
        }
    }

    fun armTest() {
        val state = _uiState.value
        if (!state.isGearValid) {
            _uiState.update {
                it.copy(userMessage = "Relação da marcha selecionada inválida. Verifique o câmbio do veículo.")
            }
            return
        }
        if (state.isReadyToArm) {
            currentSessionId = UUID.randomUUID().toString()
            synchronized(samples) {
                samples.clear()
            }
            _uiState.update {
                it.copy(
                    testState = DynoRunState.AGUARDANDO_INICIO,
                    userMessage = null,
                    diagnosticError = null
                )
            }
        }
    }

    fun cancelTest() {
        _uiState.update {
            it.copy(
                testState = DynoRunState.PARADO,
                displaySpeedKmh = 0f,
                liveEnginePowerCv = 0f,
                liveWheelPowerCv = 0f,
                liveEngineTorqueKgm = 0f
            )
        }
        synchronized(samples) {
            samples.clear()
        }
        runStartTimeNs = 0L
    }

    private fun startDisplayLoop() {
        displayLoopJob?.cancel()
        displayLoopJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val nowMs = SystemClock.elapsedRealtime()
                val gpsAge = if (lastGpsUpdateWallMs > 0L) (nowMs - lastGpsUpdateWallMs).coerceAtLeast(0L) else 9999L
                val currentGpsSpeed = _uiState.value.gpsSpeedKmh
                val accuracy = _uiState.value.gpsAccuracyMeters

                val avgSpeed = if (speedMovingAverageBuffer.isNotEmpty()) {
                    speedMovingAverageBuffer.average().toFloat()
                } else currentGpsSpeed

                val isStopped2s = (motionState == VehicleMotionState.STOPPED) && (nowMs - stoppedStartMs >= 2000L)
                val isGpsReady = locationCount > 0 && gpsAge <= 5000L && accuracy <= 25.0f

                val isCalibrated = _uiState.value.isCalibrated
                val isGearValid = _uiState.value.isGearValid
                val currentState = _uiState.value.testState

                val blockingReason = when {
                    !isCalibrated -> "Aguardando calibração"
                    !isGearValid -> "Relação de marcha inválida"
                    locationCount == 0 || gpsAge > 5000L -> "Aguardando sinal GPS"
                    accuracy > 25.0f -> "Precisão GPS insuficiente"
                    motionState != VehicleMotionState.STOPPED || !isStopped2s -> "Aguardando veículo parar"
                    !isPhoneStable -> "Celular se movimentando"
                    else -> "Pronto para iniciar"
                }

                val isReadyToArm = isCalibrated &&
                    isGearValid &&
                    isGpsReady &&
                    motionState == VehicleMotionState.STOPPED &&
                    isStopped2s &&
                    isPhoneStable &&
                    currentState == DynoRunState.PARADO

                // Filtragem do velocímetro
                val targetDisplaySpeed = if (isStopped2s && currentGpsSpeed < 2.0f) 0f else currentGpsSpeed
                val smoothSpeed = _uiState.value.displaySpeedKmh + 0.35f * (targetDisplaySpeed - _uiState.value.displaySpeedKmh)
                val finalSpeed = if (isStopped2s && smoothSpeed < 0.5f) 0f else smoothSpeed

                // Cálculo de G longitudinal calibrado
                val longG = ((rawAccelY - calibY) / 9.81f).coerceIn(-2.0f, 2.0f)

                // Potência e torque ao vivo
                val veh = _uiState.value.activeVehicle ?: Vehicle()
                val speedMps = (finalSpeed / 3.6f).coerceAtLeast(0f)
                val massKg = veh.totalMassKg
                val accelMps2 = (longG * 9.81f).coerceAtLeast(0f)

                // Força acelerativa: F = m*a + Faero + Froll
                val fAero = 0.5f * 1.2f * veh.frontalAreaM2 * veh.dragCoefficientCd * speedMps.pow(2)
                val fRoll = massKg * 9.81f * 0.015f
                val fInertia = massKg * accelMps2
                val fTotal = (fInertia + fAero + fRoll).coerceAtLeast(0f)

                val wheelPowerWatts = fTotal * speedMps
                val wheelPowerCv = (wheelPowerWatts / 735.5f).finiteOrDefault(0f).coerceAtLeast(0f)
                val enginePowerCv = (wheelPowerCv / max(1.0f - (veh.drivetrainLossPercent / 100.0f), 0.5f)).finiteOrDefault(0f).coerceAtLeast(0f)

                val currentSelectedGearIndex = _uiState.value.selectedGearIndex
                val liveRpm = veh.calculateRpmFromSpeedKmh(finalSpeed, gearIndex = currentSelectedGearIndex)
                val engineTorqueKgm = if (liveRpm > 0) ((enginePowerCv * 716.2f) / liveRpm).finiteOrDefault(0f).coerceAtLeast(0f) else 0f

                _uiState.update { current ->
                    current.copy(
                        displaySpeedKmh = finalSpeed,
                        avgGpsSpeedKmh = avgSpeed,
                        longitudinalG = longG,
                        liveWheelPowerCv = wheelPowerCv,
                        liveEnginePowerCv = enginePowerCv,
                        liveEngineTorqueKgm = engineTorqueKgm,
                        liveRpm = liveRpm,
                        gpsAgeMillis = gpsAge,
                        isGpsReady = isGpsReady,
                        vehicleMotionState = motionState,
                        isStoppedForTwoSeconds = isStopped2s,
                        isPhoneStable = isPhoneStable,
                        blockingReason = blockingReason,
                        isReadyToArm = isReadyToArm,
                        runElapsedSeconds = if (runStartTimeNs > 0L) ((System.nanoTime() - runStartTimeNs) / 1_000_000_000f).finiteOrDefault(0f) else 0f
                    )
                }

                // Gravação de amostra se medindo
                if (currentState == DynoRunState.MEDINDO || currentState == DynoRunState.MEDINDO_PROTEGIDO) {
                    val sample = RunSample(
                        timestampNs = System.nanoTime(),
                        elapsedSeconds = if (runStartTimeNs > 0L) ((System.nanoTime() - runStartTimeNs) / 1_000_000_000f).finiteOrDefault(0f) else 0f,
                        speedKmh = finalSpeed,
                        accelerationMps2 = accelMps2,
                        longitudinalG = longG,
                        estimatedRpm = liveRpm,
                        wheelPowerCv = wheelPowerCv,
                        enginePowerCv = enginePowerCv,
                        wheelTorqueKgm = (engineTorqueKgm * 0.85f).finiteOrDefault(0f),
                        engineTorqueKgm = engineTorqueKgm
                    )
                    synchronized(samples) {
                        samples.add(sample)
                    }

                    // Critério de fim de passada (desaceleração ou velocidade alvo atingida)
                    val sampleCount = synchronized(samples) { samples.size }
                    if (finalSpeed >= _uiState.value.targetMaxSpeedKmh || (sampleCount > 30 && longG < -0.05f)) {
                        finishRun()
                    }
                }

                delay(50)
            }
        }
    }

    private fun finishRun() {
        val recordedSamples = synchronized(samples) { samples.toList() }
        if (recordedSamples.isEmpty()) {
            cancelTest()
            return
        }

        // 1. Parar coleta
        stopSensorsAndGps()
        val sessionId = currentSessionId ?: UUID.randomUUID().toString()
        val veh = _uiState.value.activeVehicle ?: Vehicle()
        val selectedGearIdx = _uiState.value.selectedGearIndex
        val effectiveVehicleForRun = veh.copy(testGearIndex = selectedGearIdx)
        val elapsedSecs = _uiState.value.runElapsedSeconds

        viewModelScope.launch {
            var stage = "Gravação de Amostras"
            try {
                val startMs = System.currentTimeMillis() - (elapsedSecs * 1000).toLong()
                val endMs = System.currentTimeMillis()

                // 2. Gravar sessão pendente no banco antes de qualquer cálculo
                val pendingSession = PendingSession(
                    sessionId = sessionId,
                    vehicleId = effectiveVehicleForRun.id,
                    vehicleName = effectiveVehicleForRun.name,
                    startTimeMs = startMs,
                    endTimeMs = endMs,
                    sampleCount = recordedSamples.size,
                    status = "PENDING",
                    samples = recordedSamples
                )
                runResultRepository.savePendingSession(pendingSession)

                // 3. Registrar Logcat detalhado para diagnóstico
                val startSpeed = recordedSamples.firstOrNull()?.speedKmh?.finiteOrDefault(0f) ?: 0f
                val maxSpeed = recordedSamples.maxOfOrNull { it.speedKmh.finiteOrDefault(0f) } ?: 0f
                Log.i(TAG, "=== FINALIZAÇÃO ATÔMICA DA SESSÃO ===")
                Log.i(TAG, "sessionId: $sessionId | marcha: ${selectedGearIdx + 1}ª (índice $selectedGearIdx)")
                Log.i(TAG, "quantidade de amostras: ${recordedSamples.size}")
                Log.i(TAG, "timestamp inicial: $startMs | final: $endMs")
                Log.i(TAG, "velocidade inicial GPS: $startSpeed km/h | máxima: $maxSpeed km/h")

                // 4. Validar e Calcular resultado com a marcha selecionada
                stage = "CALC_RESULT"
                val calculatedResult = RunProcessor.processRun(
                    sessionId = sessionId,
                    vehicle = effectiveVehicleForRun,
                    rawSamples = recordedSamples,
                    durationOverride = elapsedSecs
                )
                Log.i(TAG, "potência calculada: ${calculatedResult.peakEnginePowerCv} cv | torque calculado: ${calculatedResult.peakEngineTorqueKgm} kgfm | marcha salva: ${calculatedResult.gearUsed} (${calculatedResult.gearRatioUsed})")

                // 5. Salvar resultado atômico
                stage = "SAVE_ATOMIC"
                val saveResult = runResultRepository.saveRunResultAtomic(
                    run = calculatedResult,
                    pendingSessionId = sessionId
                )

                when (saveResult) {
                    is com.example.model.SaveRunResult.Success -> {
                        _uiState.update {
                            it.copy(
                                testState = DynoRunState.CONCLUIDO,
                                lastCompletedResultId = saveResult.resultId,
                                userMessage = null,
                                diagnosticError = null
                            )
                        }
                    }
                    is com.example.model.SaveRunResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                testState = DynoRunState.PARADO,
                                userMessage = "Não foi possível finalizar a passagem. As amostras foram preservadas.",
                                diagnosticError = "testId=$sessionId | etapa=${saveResult.stage} | exceção=${saveResult.exceptionType} | mensagem=${saveResult.technicalMessage} | amostras=${recordedSamples.size} | app=v1.0.3 (3)"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "FALHA NA ETAPA '$stage' DA SESSÃO $sessionId: ${e.message}", e)
                try {
                    val existing = runResultRepository.getPendingSessionById(sessionId)
                    if (existing != null) {
                        val failedSession = existing.copy(
                            status = "PENDING",
                            errorMessage = e.message ?: "Erro desconhecido",
                            errorStage = stage,
                            errorExceptionType = e.javaClass.name,
                            lastAttemptTimestamp = System.currentTimeMillis()
                        )
                        runResultRepository.savePendingSession(failedSession)
                    }
                } catch (saveErr: Exception) {
                    Log.e(TAG, "Erro ao salvar status de erro na sessão: ${saveErr.message}", saveErr)
                }

                _uiState.update {
                    it.copy(
                        testState = DynoRunState.PARADO,
                        userMessage = "Não foi possível finalizar a passagem. As amostras foram preservadas.",
                        diagnosticError = "testId=$sessionId | etapa=$stage | exceção=${e.javaClass.simpleName} | mensagem=${e.message} | amostras=${recordedSamples.size} | app=v1.0.3 (3)"
                    )
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                rawAccelX = event.values[0]
                rawAccelY = event.values[1]
                rawAccelZ = event.values[2]
            }
            Sensor.TYPE_GYROSCOPE -> {
                rawGyroX = event.values[0]
                rawGyroY = event.values[1]
                rawGyroZ = event.values[2]

                val nowNs = System.nanoTime()
                val rawMag = sqrt(rawGyroX * rawGyroX + rawGyroY * rawGyroY + rawGyroZ * rawGyroZ)
                synchronized(gyroWindow) {
                    gyroWindow.add(GyroSample(nowNs, rawMag))
                    val cutoff = nowNs - 1_000_000_000L
                    while (gyroWindow.isNotEmpty() && gyroWindow.first().timestampNs < cutoff) {
                        gyroWindow.removeAt(0)
                    }
                    val avgMag = if (gyroWindow.isNotEmpty()) gyroWindow.map { it.mag }.average().toFloat() else rawMag
                    if (avgMag > 1.2f) {
                        if (gyroUnstableStartNs == 0L) {
                            gyroUnstableStartNs = nowNs
                        } else if (nowNs - gyroUnstableStartNs > 1_000_000_000L) {
                            isPhoneStable = false
                        }
                    } else {
                        gyroUnstableStartNs = 0L
                        isPhoneStable = true
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onLocationChanged(location: Location) {
        lastGpsLocation = location
        lastGpsUpdateWallMs = SystemClock.elapsedRealtime()
        locationCount++

        val speedKmh = if (location.hasSpeed()) (location.speed * 3.6f).finiteOrDefault(0f) else 0f
        val accuracy = if (location.hasAccuracy()) location.accuracy.finiteOrDefault(99f) else 99f

        speedMovingAverageBuffer.add(speedKmh)
        if (speedMovingAverageBuffer.size > 5) speedMovingAverageBuffer.removeAt(0)

        val avgSpeed = speedMovingAverageBuffer.average().toFloat().finiteOrDefault(0f)

        if (avgSpeed <= 3.0f) {
            motionState = VehicleMotionState.STOPPED
        } else if (avgSpeed > 5.0f) {
            motionState = VehicleMotionState.MOVING
        }

        if (speedKmh > 5.0f) {
            consecutiveHighSpeedCount++
        } else {
            consecutiveHighSpeedCount = 0
        }

        if (consecutiveHighSpeedCount >= 2 || motionState == VehicleMotionState.MOVING) {
            stoppedStartMs = SystemClock.elapsedRealtime()
        }

        _uiState.update {
            it.copy(
                gpsSpeedKmh = speedKmh,
                gpsAccuracyMeters = accuracy,
                locationUpdateCount = locationCount
            )
        }

        // Trigger de início da medição se armado
        if (_uiState.value.testState == DynoRunState.AGUARDANDO_INICIO) {
            if (speedKmh >= _uiState.value.startSpeedTriggerKmh && accuracy <= 25.0f) {
                runStartTimeNs = System.nanoTime()
                synchronized(samples) {
                    samples.clear()
                }
                _uiState.update { it.copy(testState = DynoRunState.MEDINDO) }
            }
        }
    }

    override fun onProviderEnabled(provider: String) {
        _uiState.update { it.copy(isGpsProviderEnabled = true) }
    }

    override fun onProviderDisabled(provider: String) {
        _uiState.update { it.copy(isGpsProviderEnabled = false) }
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onCleared() {
        super.onCleared()
        stopSensorsAndGps()
        displayLoopJob?.cancel()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = DynoMobileDatabase.getDatabase(application)
            return TestPreparationViewModel(
                application,
                VehicleRepository(db.vehicleDao()),
                RunResultRepository(db.runResultDao(), db.pendingSessionDao())
            ) as T
        }
    }
}
