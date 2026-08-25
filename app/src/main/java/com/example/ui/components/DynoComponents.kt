package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DynoBackground
import com.example.ui.theme.DynoBlueLight
import com.example.ui.theme.DynoBluePrimary
import com.example.ui.theme.DynoBorder
import com.example.ui.theme.DynoBorderLight
import com.example.ui.theme.DynoDivider
import com.example.ui.theme.DynoErrorRed
import com.example.ui.theme.DynoPowerCyan
import com.example.ui.theme.DynoSuccessGreen
import com.example.ui.theme.DynoSurface
import com.example.ui.theme.DynoSurfaceContainer
import com.example.ui.theme.DynoSurfaceElevated
import com.example.ui.theme.DynoTextMuted
import com.example.ui.theme.DynoTextPrimary
import com.example.ui.theme.DynoTextSecondary
import com.example.ui.theme.DynoTorqueOrange
import com.example.ui.theme.DynoWarningYellow
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// =========================================================================================
// 1. BOTÕES PADRONIZADOS
// =========================================================================================

/**
 * Botão Principal Oficial (Azul #2F80ED, texto branco #FFFFFF, cantos 12dp, altura mínima 48dp)
 */
@Composable
fun DynoPrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: ImageVector? = null,
  testTag: String = "dyno_primary_button"
) {
  Button(
    onClick = onClick,
    modifier = modifier
      .height(50.dp)
      .testTag(testTag),
    enabled = enabled,
    shape = RoundedCornerShape(12.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = DynoBluePrimary,
      contentColor = Color.White,
      disabledContainerColor = DynoSurfaceElevated,
      disabledContentColor = DynoTextMuted
    ),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(20.dp),
          tint = Color.White
        )
        Spacer(modifier = Modifier.width(8.dp))
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
          fontWeight = FontWeight.Medium,
          letterSpacing = 0.5.sp,
          fontSize = 14.5.sp
        ),
        color = Color.White
      )
    }
  }
}

/**
 * Botão Secundário Oficial (Surface elevada #303540, texto claro #F2F4F8)
 */
@Composable
fun DynoSecondaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: ImageVector? = null,
  testTag: String = "dyno_secondary_button"
) {
  FilledTonalButton(
    onClick = onClick,
    modifier = modifier
      .height(48.dp)
      .testTag(testTag),
    enabled = enabled,
    shape = RoundedCornerShape(12.dp),
    colors = ButtonDefaults.filledTonalButtonColors(
      containerColor = DynoSurfaceElevated,
      contentColor = DynoTextPrimary,
      disabledContainerColor = DynoSurfaceContainer,
      disabledContentColor = DynoTextMuted
    ),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
          tint = DynoTextPrimary
        )
        Spacer(modifier = Modifier.width(6.dp))
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.Medium,
          fontSize = 13.5.sp
        ),
        color = DynoTextPrimary
      )
    }
  }
}

/**
 * Botão Destrutivo / Cancelar / Alerta (Vermelho #E35D62)
 */
@Composable
fun DynoDangerButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isOutlined: Boolean = false,
  icon: ImageVector? = null,
  testTag: String = "dyno_danger_button"
) {
  if (isOutlined) {
    OutlinedButton(
      onClick = onClick,
      modifier = modifier
        .height(48.dp)
        .testTag(testTag),
      enabled = enabled,
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.2.dp, DynoErrorRed.copy(alpha = 0.8f)),
      colors = ButtonDefaults.outlinedButtonColors(
        contentColor = DynoErrorRed
      ),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        if (icon != null) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = DynoErrorRed
          )
          Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
          text = text,
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 13.5.sp
          ),
          color = DynoErrorRed
        )
      }
    }
  } else {
    Button(
      onClick = onClick,
      modifier = modifier
        .height(48.dp)
        .testTag(testTag),
      enabled = enabled,
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = DynoErrorRed,
        contentColor = Color.White
      ),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        if (icon != null) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.White
          )
          Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
          text = text,
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 13.5.sp
          ),
          color = Color.White
        )
      }
    }
  }
}

// =========================================================================================
// 2. CARTÕES PADRONIZADOS
// =========================================================================================

/**
 * Cartão Padrão Dyno Lite (Surface #242832, borda sutil #303540, cantos 16dp)
 */
@Composable
fun DynoCard(
  modifier: Modifier = Modifier,
  shapeRadius: Dp = 16.dp,
  borderColor: Color = DynoBorder,
  content: @Composable ColumnScope.() -> Unit
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(shapeRadius),
    colors = CardDefaults.cardColors(
      containerColor = DynoSurface
    ),
    border = BorderStroke(1.dp, borderColor)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      content = content
    )
  }
}

/**
 * Cartão Elevado Dyno Lite (Surface #303540)
 */
@Composable
fun DynoElevatedCard(
  modifier: Modifier = Modifier,
  shapeRadius: Dp = 16.dp,
  content: @Composable ColumnScope.() -> Unit
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(shapeRadius),
    colors = CardDefaults.cardColors(
      containerColor = DynoSurfaceElevated
    ),
    border = BorderStroke(1.dp, DynoBorderLight)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      content = content
    )
  }
}

// =========================================================================================
// 3. BADGES DE STATUS
// =========================================================================================

enum class DynoBadgeStatus {
  SUCCESS,  // Verde #42C77A
  WARNING,  // Amarelo #F2C94C
  ERROR,    // Vermelho #E35D62
  INFO,     // Azul #2F80ED
  NEUTRAL   // Secundário #B7BCC7
}

@Composable
fun DynoStatusBadge(
  text: String,
  status: DynoBadgeStatus = DynoBadgeStatus.INFO,
  modifier: Modifier = Modifier
) {
  val (bgColor, textColor, borderColor) = when (status) {
    DynoBadgeStatus.SUCCESS -> Triple(
      DynoSuccessGreen.copy(alpha = 0.15f),
      DynoSuccessGreen,
      DynoSuccessGreen.copy(alpha = 0.4f)
    )
    DynoBadgeStatus.WARNING -> Triple(
      DynoWarningYellow.copy(alpha = 0.15f),
      DynoWarningYellow,
      DynoWarningYellow.copy(alpha = 0.4f)
    )
    DynoBadgeStatus.ERROR -> Triple(
      DynoErrorRed.copy(alpha = 0.15f),
      DynoErrorRed,
      DynoErrorRed.copy(alpha = 0.4f)
    )
    DynoBadgeStatus.INFO -> Triple(
      DynoBluePrimary.copy(alpha = 0.15f),
      DynoBlueLight,
      DynoBluePrimary.copy(alpha = 0.4f)
    )
    DynoBadgeStatus.NEUTRAL -> Triple(
      DynoSurfaceElevated,
      DynoTextSecondary,
      DynoBorder
    )
  }

  Surface(
    modifier = modifier.testTag("dyno_status_badge"),
    shape = CircleShape,
    color = bgColor,
    border = BorderStroke(0.8.dp, borderColor)
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
      ),
      color = textColor,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
    )
  }
}

// =========================================================================================
// 4. CARTÃO DE RESULTADO COM POTÊNCIA (CIANO) E TORQUE (LARANJA)
// =========================================================================================

@Composable
fun DynoResultCard(
  powerValue: Float,
  powerUnit: String = "cv",
  torqueValue: Float,
  torqueUnit: String = "kgfm",
  qualityText: String? = null,
  qualityStatus: DynoBadgeStatus = DynoBadgeStatus.SUCCESS,
  speedMaxKmh: Float? = null,
  modifier: Modifier = Modifier
) {
  DynoCard(modifier = modifier.testTag("dyno_result_card")) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "DESEMPENHO MEDIDO",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.8.sp,
          fontSize = 11.5.sp
        ),
        color = DynoTextSecondary
      )

      if (qualityText != null) {
        DynoStatusBadge(text = qualityText, status = qualityStatus)
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Bloco Potência (Exclusivo Ciano #39C6F4)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
      ) {
        Text(
          text = "POTÊNCIA",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp
          ),
          color = DynoTextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
          Text(
            text = String.format(Locale.US, "%.0f", powerValue),
            style = MaterialTheme.typography.displayMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 38.sp
            ),
            color = DynoPowerCyan
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = powerUnit,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            ),
            color = DynoPowerCyan.copy(alpha = 0.85f),
            modifier = Modifier.padding(bottom = 6.dp)
          )
        }
      }

      // Separador vertical sutil
      Surface(
        modifier = Modifier
          .height(44.dp)
          .width(1.dp),
        color = DynoBorder
      ) {}

      // Bloco Torque (Exclusivo Laranja #FF8A3D)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
      ) {
        Text(
          text = "TORQUE",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp
          ),
          color = DynoTextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
          Text(
            text = String.format(Locale.US, "%.1f", torqueValue),
            style = MaterialTheme.typography.displayMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 38.sp
            ),
            color = DynoTorqueOrange
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = torqueUnit,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            ),
            color = DynoTorqueOrange.copy(alpha = 0.85f),
            modifier = Modifier.padding(bottom = 6.dp)
          )
        }
      }
    }

    if (speedMaxKmh != null && speedMaxKmh > 0f) {
      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = DynoDivider, thickness = 0.8.dp)
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Velocidade máxima atingida",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
          color = DynoTextSecondary
        )
        Text(
          text = String.format(Locale.US, "%.1f km/h", speedMaxKmh),
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp
          ),
          color = DynoTextPrimary
        )
      }
    }
  }
}

// =========================================================================================
// 5. VELOCÍMETRO DIGITAL SEMICIRCULAR PADRONIZADO
// =========================================================================================

@Composable
fun DynoSpeedometer(
  currentSpeedKmh: Float,
  targetTriggerSpeedKmh: Float,
  isMeasuring: Boolean = false,
  modifier: Modifier = Modifier
) {
  val density = LocalDensity.current
  val visualSpeed = currentSpeedKmh.coerceAtLeast(0f)
  val progressFraction = (visualSpeed / 200f).coerceIn(0f, 1f)
  val progressSweep = progressFraction * 220f

  val primaryColor = if (isMeasuring) DynoSuccessGreen else DynoBluePrimary
  val triggerHighlightColor = DynoBlueLight
  val trackColor = DynoSurfaceElevated
  val normalTickColor = DynoTextMuted
  val normalTextColor = DynoTextSecondary.toArgb()
  val highlightTextColor = triggerHighlightColor.toArgb()

  val tickSteps = remember { listOf(0, 20, 40, 60, 80, 100, 120, 140, 160, 180, 200) }

  val textPaint = remember {
    android.graphics.Paint().apply {
      isAntiAlias = true
      textAlign = android.graphics.Paint.Align.CENTER
    }
  }

  val accessibleDescription = "Velocidade GPS: ${String.format(Locale.US, "%.0f", visualSpeed)} km/h"

  Box(
    modifier = modifier
      .size(290.dp, 240.dp)
      .semantics { contentDescription = accessibleDescription }
      .testTag("dyno_speedometer"),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val canvasWidth = size.width
      val canvasHeight = size.height
      val centerX = canvasWidth / 2f
      val centerY = canvasHeight * 0.52f

      val arcRadius = with(density) { 96.dp.toPx() }
      val strokeWidthPx = with(density) { 13.dp.toPx() }
      val labelRadius = arcRadius + with(density) { 19.dp.toPx() }
      val tickInnerRadius = arcRadius - with(density) { 8.dp.toPx() }
      val tickOuterRadius = arcRadius - with(density) { 2.dp.toPx() }

      val arcRect = Rect(
        left = centerX - arcRadius,
        top = centerY - arcRadius,
        right = centerX + arcRadius,
        bottom = centerY + arcRadius
      )

      // 1. Background Arc
      drawArc(
        color = trackColor,
        startAngle = 160f,
        sweepAngle = 220f,
        useCenter = false,
        topLeft = arcRect.topLeft,
        size = arcRect.size,
        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
      )

      // 2. Dynamic Progress Arc
      if (progressSweep > 0.5f) {
        drawArc(
          color = primaryColor,
          startAngle = 160f,
          sweepAngle = progressSweep,
          useCenter = false,
          topLeft = arcRect.topLeft,
          size = arcRect.size,
          style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )
      }

      // 3. Ticks and Numbers
      val labelTextSizePx = with(density) { 10.sp.toPx() }
      textPaint.textSize = labelTextSizePx

      tickSteps.forEach { step ->
        val stepFraction = step / 200f
        val stepAngleDeg = 160f + stepFraction * 220f
        val stepAngleRad = Math.toRadians(stepAngleDeg.toDouble())

        val cosA = cos(stepAngleRad).toFloat()
        val sinA = sin(stepAngleRad).toFloat()

        val isTrigger = step == targetTriggerSpeedKmh.toInt()

        val tOuter = if (isTrigger) arcRadius else tickOuterRadius
        val tInner = if (isTrigger) arcRadius - with(density) { 12.dp.toPx() } else tickInnerRadius
        val tColor = if (isTrigger) triggerHighlightColor else normalTickColor
        val tStroke = with(density) { (if (isTrigger) 2.5.dp else 1.2.dp).toPx() }

        drawLine(
          color = tColor,
          start = Offset(centerX + tInner * cosA, centerY + tInner * sinA),
          end = Offset(centerX + tOuter * cosA, centerY + tOuter * sinA),
          strokeWidth = tStroke,
          cap = StrokeCap.Round
        )

        val lx = centerX + labelRadius * cosA
        val ly = centerY + labelRadius * sinA + (labelTextSizePx * 0.35f)

        drawIntoCanvas { canvas ->
          textPaint.color = if (isTrigger) highlightTextColor else normalTextColor
          textPaint.typeface = if (isTrigger) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
          canvas.nativeCanvas.drawText(step.toString(), lx, ly, textPaint)
        }
      }

      // 4. Dot indicator at target trigger speed during measurement
      if (isMeasuring && targetTriggerSpeedKmh > 0f) {
        val trigFraction = (targetTriggerSpeedKmh / 200f).coerceIn(0f, 1f)
        val trigAngleDeg = 160f + trigFraction * 220f
        val trigAngleRad = Math.toRadians(trigAngleDeg.toDouble())
        val tx = centerX + arcRadius * cos(trigAngleRad).toFloat()
        val ty = centerY + arcRadius * sin(trigAngleRad).toFloat()
        drawCircle(
          color = Color.White,
          radius = with(density) { 4.dp.toPx() },
          center = Offset(tx, ty)
        )
      }
    }

    // Centro do Velocímetro
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .padding(top = 16.dp)
        .align(Alignment.Center)
    ) {
      Text(
        text = "VELOCIDADE GPS",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp,
          fontSize = 10.5.sp
        ),
        color = DynoTextSecondary
      )

      val formattedSpeed = if (visualSpeed < 10f && visualSpeed > 0f && (visualSpeed % 1.0f != 0f)) {
        String.format(Locale.US, "%.1f", visualSpeed)
      } else {
        String.format(Locale.US, "%.0f", visualSpeed)
      }

      Text(
        text = formattedSpeed,
        style = MaterialTheme.typography.displayLarge.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 52.sp,
          letterSpacing = (-1).sp
        ),
        color = DynoTextPrimary,
        modifier = Modifier.padding(vertical = 0.dp)
      )

      Text(
        text = "km/h",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Medium,
          fontSize = 14.5.sp
        ),
        color = DynoTextSecondary
      )

      if (!isMeasuring) {
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = "Início automático: ${targetTriggerSpeedKmh.toInt()} km/h",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp
          ),
          color = DynoBlueLight
        )
      }
    }
  }
}

// =========================================================================================
// 6. TOP BAR UNIFICADA (DYNO TOP BAR)
// =========================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynoTopBar(
  title: String,
  onNavigateBack: (() -> Unit)? = null,
  actions: @Composable RowScope.() -> Unit = {},
  showBrandLogo: Boolean = false,
  modifier: Modifier = Modifier
) {
  TopAppBar(
    modifier = modifier.testTag("dyno_top_bar"),
    title = {
      if (showBrandLogo) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          DynoLogoSymbol(size = 28.dp)
          Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp
            ),
            color = DynoTextPrimary
          )
        }
      } else {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          ),
          color = DynoTextPrimary
        )
      }
    },
    navigationIcon = {
      if (onNavigateBack != null) {
        IconButton(
          onClick = onNavigateBack,
          modifier = Modifier.testTag("dyno_top_bar_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Voltar",
            tint = DynoTextPrimary
          )
        }
      }
    },
    actions = actions,
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = DynoBackground,
      titleContentColor = DynoTextPrimary,
      navigationIconContentColor = DynoTextPrimary,
      actionIconContentColor = DynoTextSecondary
    )
  )
}

enum class DynoTab {
  HOME,
  GARAGE,
  RESULTS,
  SIMULATOR
}

// =========================================================================================
// 7. BOTTOM NAVIGATION PADRONIZADA (DYNO BOTTOM NAVIGATION)
// =========================================================================================

@Composable
fun DynoBottomNavigation(
  selectedTab: DynoTab,
  onTabSelected: (DynoTab) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
    HorizontalDivider(thickness = 0.8.dp, color = DynoDivider)
    NavigationBar(
      containerColor = DynoBackground,
      tonalElevation = 0.dp,
      modifier = Modifier.height(68.dp)
    ) {
      // 1. INÍCIO
      NavigationBarItem(
        selected = selectedTab == DynoTab.HOME,
        onClick = { onTabSelected(DynoTab.HOME) },
        icon = {
          Icon(
            imageVector = if (selectedTab == DynoTab.HOME) Icons.Default.Home else Icons.Outlined.Home,
            contentDescription = "Início",
            modifier = Modifier.size(24.dp)
          )
        },
        label = {
          Text(
            text = "INÍCIO",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = if (selectedTab == DynoTab.HOME) FontWeight.Bold else FontWeight.Medium,
              fontSize = 10.5.sp
            )
          )
        },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = DynoBlueLight,
          selectedTextColor = DynoBlueLight,
          indicatorColor = DynoBluePrimary.copy(alpha = 0.25f),
          unselectedIconColor = DynoTextMuted,
          unselectedTextColor = DynoTextMuted
        ),
        modifier = Modifier.testTag("nav_tab_home")
      )

      // 2. GARAGEM
      NavigationBarItem(
        selected = selectedTab == DynoTab.GARAGE,
        onClick = { onTabSelected(DynoTab.GARAGE) },
        icon = {
          Icon(
            imageVector = if (selectedTab == DynoTab.GARAGE) Icons.Default.DirectionsCar else Icons.Outlined.DirectionsCar,
            contentDescription = "Garagem",
            modifier = Modifier.size(24.dp)
          )
        },
        label = {
          Text(
            text = "GARAGEM",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = if (selectedTab == DynoTab.GARAGE) FontWeight.Bold else FontWeight.Medium,
              fontSize = 10.5.sp
            )
          )
        },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = DynoBlueLight,
          selectedTextColor = DynoBlueLight,
          indicatorColor = DynoBluePrimary.copy(alpha = 0.25f),
          unselectedIconColor = DynoTextMuted,
          unselectedTextColor = DynoTextMuted
        ),
        modifier = Modifier.testTag("nav_tab_garage")
      )

      // 3. HISTÓRICO / RESULTADOS
      NavigationBarItem(
        selected = selectedTab == DynoTab.RESULTS,
        onClick = { onTabSelected(DynoTab.RESULTS) },
        icon = {
          Icon(
            imageVector = if (selectedTab == DynoTab.RESULTS) Icons.Default.Assessment else Icons.Outlined.Assessment,
            contentDescription = "Histórico",
            modifier = Modifier.size(24.dp)
          )
        },
        label = {
          Text(
            text = "HISTÓRICO",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = if (selectedTab == DynoTab.RESULTS) FontWeight.Bold else FontWeight.Medium,
              fontSize = 10.5.sp
            )
          )
        },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = DynoBlueLight,
          selectedTextColor = DynoBlueLight,
          indicatorColor = DynoBluePrimary.copy(alpha = 0.25f),
          unselectedIconColor = DynoTextMuted,
          unselectedTextColor = DynoTextMuted
        ),
        modifier = Modifier.testTag("nav_tab_results")
      )

      // 4. SIMULADOR (Modo Simulação com destaque roxo/índigo)
      NavigationBarItem(
        selected = selectedTab == DynoTab.SIMULATOR,
        onClick = { onTabSelected(DynoTab.SIMULATOR) },
        icon = {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Simulador",
            modifier = Modifier.size(24.dp)
          )
        },
        label = {
          Text(
            text = "SIMULADOR",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = if (selectedTab == DynoTab.SIMULATOR) FontWeight.Bold else FontWeight.Medium,
              fontSize = 10.5.sp
            )
          )
        },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = Color(0xFFA78BFA),
          selectedTextColor = Color(0xFFA78BFA),
          indicatorColor = Color(0xFF8B5CF6).copy(alpha = 0.25f),
          unselectedIconColor = DynoTextMuted,
          unselectedTextColor = DynoTextMuted
        ),
        modifier = Modifier.testTag("nav_tab_simulator")
      )
    }
  }
}
