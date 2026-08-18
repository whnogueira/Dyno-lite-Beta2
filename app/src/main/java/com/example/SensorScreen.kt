package com.example

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorScreen(
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onNavigateBack)

  val context = LocalContext.current
  val sensorManager = remember {
    context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
  }
  val accelerometerSensor = remember(sensorManager) {
    sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
  }
  val linearAccelerationSensor = remember(sensorManager) {
    sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
  }
  val gyroscopeSensor = remember(sensorManager) {
    sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
  }

  val isRawAvailable = accelerometerSensor != null
  val isLinearAvailable = linearAccelerationSensor != null
  val isGyroAvailable = gyroscopeSensor != null

  var rawX by remember { mutableFloatStateOf(0f) }
  var rawY by remember { mutableFloatStateOf(0f) }
  var rawZ by remember { mutableFloatStateOf(0f) }

  var linearX by remember { mutableFloatStateOf(0f) }
  var linearY by remember { mutableFloatStateOf(0f) }
  var linearZ by remember { mutableFloatStateOf(0f) }

  var gyroX by remember { mutableFloatStateOf(0f) }
  var gyroY by remember { mutableFloatStateOf(0f) }
  var gyroZ by remember { mutableFloatStateOf(0f) }

  var samplingFrequencyHz by remember { mutableDoubleStateOf(0.0) }

  DisposableEffect(sensorManager, accelerometerSensor, linearAccelerationSensor, gyroscopeSensor) {
    if (sensorManager != null) {
      var previousTimestampNs = 0L
      val validIntervals = mutableListOf<Double>()
      var lastUiUpdateTimestampNs = 0L

      val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
          when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
              if (event.values.size >= 3) {
                rawX = event.values[0]
                rawY = event.values[1]
                rawZ = event.values[2]
              }
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
              if (event.values.size >= 3) {
                linearX = event.values[0]
                linearY = event.values[1]
                linearZ = event.values[2]
              }

              val currentTimestampNs = event.timestamp
              if (previousTimestampNs != 0L) {
                val deltaNs = currentTimestampNs - previousTimestampNs
                // Ignora intervalos <= 0 e > 1 segundo (1_000_000_000 ns)
                if (deltaNs > 0 && deltaNs <= 1_000_000_000L) {
                  val intervaloSegundos = deltaNs / 1_000_000_000.0
                  validIntervals.add(intervaloSegundos)
                  if (validIntervals.size > 20) {
                    validIntervals.removeAt(0)
                  }

                  val mediaDosIntervalos = validIntervals.average()
                  if (mediaDosIntervalos > 0.0) {
                    val frequenciaMedia = 1.0 / mediaDosIntervalos

                    // Atualiza o texto visual no máximo cinco vezes por segundo (intervalo de 200 ms / 200_000_000 ns)
                    if (currentTimestampNs - lastUiUpdateTimestampNs >= 200_000_000L) {
                      samplingFrequencyHz = frequenciaMedia
                      lastUiUpdateTimestampNs = currentTimestampNs
                    }
                  }
                }
              }
              previousTimestampNs = currentTimestampNs
            }
            Sensor.TYPE_GYROSCOPE -> {
              if (event.values.size >= 3) {
                gyroX = event.values[0]
                gyroY = event.values[1]
                gyroZ = event.values[2]
              }
            }
          }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
          // No-op
        }
      }

      if (accelerometerSensor != null) {
        sensorManager.registerListener(
          listener,
          accelerometerSensor,
          SensorManager.SENSOR_DELAY_GAME
        )
      }

      if (linearAccelerationSensor != null) {
        sensorManager.registerListener(
          listener,
          linearAccelerationSensor,
          SensorManager.SENSOR_DELAY_GAME
        )
      }

      if (gyroscopeSensor != null) {
        sensorManager.registerListener(
          listener,
          gyroscopeSensor,
          SensorManager.SENSOR_DELAY_GAME
        )
      }

      onDispose {
        sensorManager.unregisterListener(listener)
      }
    } else {
      onDispose { }
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("sensor_screen"),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      Column {
        TopAppBar(
          title = {
            Text(
              text = stringResource(R.string.title_sensors_test),
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                fontSize = 20.sp,
              ),
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.testTag("sensor_screen_title")
            )
          },
          navigationIcon = {
            IconButton(
              onClick = onNavigateBack,
              modifier = Modifier.testTag("top_bar_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.btn_back),
                tint = MaterialTheme.colorScheme.onSurface,
              )
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
          ),
        )
        HorizontalDivider(
          thickness = 1.dp,
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
      }
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
          .padding(horizontal = 20.dp, vertical = 20.dp)
          .widthIn(max = 480.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // 1. ACELERÔMETRO BRUTO (Real Sensor Readings)
        SensorCard(
          title = "ACELERÔMETRO BRUTO",
          icon = Icons.Outlined.Sensors,
          isAvailable = isRawAvailable,
          unavailableMessage = stringResource(R.string.sensor_unavailable),
          items = listOf(
            "X" to String.format(Locale.US, "%.3f m/s²", rawX),
            "Y" to String.format(Locale.US, "%.3f m/s²", rawY),
            "Z" to String.format(Locale.US, "%.3f m/s²", rawZ)
          ),
          testTag = "raw_accelerometer_card"
        )

        // 2. ACELERAÇÃO LINEAR (Real Sensor Readings)
        SensorCard(
          title = "ACELERAÇÃO LINEAR",
          icon = Icons.Outlined.Speed,
          isAvailable = isLinearAvailable,
          unavailableMessage = stringResource(R.string.linear_sensor_unavailable),
          items = listOf(
            "X" to String.format(Locale.US, "%.3f m/s²", linearX),
            "Y" to String.format(Locale.US, "%.3f m/s²", linearY),
            "Z" to String.format(Locale.US, "%.3f m/s²", linearZ)
          ),
          testTag = "linear_acceleration_card"
        )

        // 3. GIROSCÓPIO (Real Sensor Readings)
        SensorCard(
          title = "GIROSCÓPIO",
          icon = Icons.Outlined.Explore,
          isAvailable = isGyroAvailable,
          unavailableMessage = stringResource(R.string.gyro_sensor_unavailable),
          items = listOf(
            "X" to String.format(Locale.US, "%.3f rad/s", gyroX),
            "Y" to String.format(Locale.US, "%.3f rad/s", gyroY),
            "Z" to String.format(Locale.US, "%.3f rad/s", gyroZ)
          ),
          testTag = "gyroscope_card"
        )

        // 4. GPS (Fixo em zero / indisponível)
        SensorDataCard(
          title = "GPS",
          icon = Icons.Outlined.LocationOn,
          items = listOf(
            "Velocidade" to "0.0 km/h",
            "Precisão" to "indisponível"
          )
        )

        // 5. AMOSTRAGEM (Frequência real de amostragem)
        SensorDataCard(
          title = "AMOSTRAGEM",
          icon = Icons.Outlined.Timer,
          items = listOf(
            "Frequência" to String.format(Locale.US, "%.1f Hz", samplingFrequencyHz)
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Botão Voltar (Pill Button)
        Button(
          onClick = onNavigateBack,
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("back_button"),
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
          contentPadding = PaddingValues(horizontal = 24.dp),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.btn_back).uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Medium,
              letterSpacing = 1.sp,
            )
          )
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun SensorCard(
  title: String,
  icon: ImageVector,
  isAvailable: Boolean,
  unavailableMessage: String,
  items: List<Pair<String, String>>,
  modifier: Modifier = Modifier,
  testTag: String = ""
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Card Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            fontSize = 13.sp,
          ),
          color = MaterialTheme.colorScheme.primary,
        )
      }

      HorizontalDivider(
        thickness = 0.8.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
      )

      if (!isAvailable) {
        Text(
          text = unavailableMessage,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Medium
          ),
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(vertical = 4.dp)
        )
      } else {
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items.forEach { (label, value) ->
            SensorValueRow(label = label, value = value)
          }
        }
      }
    }
  }
}

@Composable
private fun SensorValueRow(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = if (label.length == 1) "$label:" else "$label:",
      style = MaterialTheme.typography.bodyMedium.copy(
        fontWeight = FontWeight.Medium
      ),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium.copy(
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace,
      ),
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun SensorDataCard(
  title: String,
  icon: ImageVector,
  items: List<Pair<String, String>>,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Card Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            fontSize = 13.sp,
          ),
          color = MaterialTheme.colorScheme.primary,
        )
      }

      HorizontalDivider(
        thickness = 0.8.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
      )

      // Key-Value Items
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items.forEach { (label, value) ->
          SensorValueRow(label = label, value = value)
        }
      }
    }
  }
}

