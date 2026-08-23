package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DynoBlueLight
import com.example.ui.theme.DynoPowerCyan
import com.example.ui.theme.DynoSurface
import com.example.ui.theme.DynoSurfaceElevated
import com.example.ui.theme.DynoTextMuted
import com.example.ui.theme.DynoTextPrimary
import com.example.ui.theme.DynoTextSecondary
import com.example.ui.theme.DynoTorqueOrange
import kotlin.math.cos
import kotlin.math.sin

/**
 * Símbolo vetorial isolado do DYNO LITE:
 * - Velocímetro / dinamômetro semicircular
 * - Arco em azul-ciano (#39C6F4)
 * - Ponteiro laranja (#FF8A3D) apontando para cima e para a direita
 * - Pequena curva ascendente representando potência
 * - Letras "DL" discretas no centro
 */
@Composable
fun DynoLogoSymbol(
  modifier: Modifier = Modifier,
  size: Dp = 64.dp,
  isMonochrome: Boolean = false,
  monochromeColor: Color = DynoTextPrimary
) {
  val density = LocalDensity.current

  val trackColor = if (isMonochrome) monochromeColor.copy(alpha = 0.25f) else DynoSurfaceElevated
  val arcColor = if (isMonochrome) monochromeColor else DynoPowerCyan
  val waveColor = if (isMonochrome) monochromeColor.copy(alpha = 0.6f) else DynoBlueLight
  val needleColor = if (isMonochrome) monochromeColor else DynoTorqueOrange
  val pivotBgColor = if (isMonochrome) monochromeColor.copy(alpha = 0.4f) else DynoSurface
  val textCol = if (isMonochrome) monochromeColor.copy(alpha = 0.8f).toArgb() else DynoTextSecondary.toArgb()

  val dlPaint = remember(textCol) {
    android.graphics.Paint().apply {
      isAntiAlias = true
      textAlign = android.graphics.Paint.Align.CENTER
      typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
  }

  Box(
    modifier = modifier
      .size(size)
      .semantics { contentDescription = "Logo Dyno Lite" }
      .testTag("dyno_logo_symbol"),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.size(size)) {
      val w = this.size.width
      val h = this.size.height
      val centerX = w / 2f
      val centerY = h * 0.54f

      val radius = w * 0.36f
      val strokeWidth = w * 0.085f

      val arcRect = Rect(
        left = centerX - radius,
        top = centerY - radius,
        right = centerX + radius,
        bottom = centerY + radius
      )

      // 1. Background Arc (Track)
      drawArc(
        color = trackColor,
        startAngle = 150f,
        sweepAngle = 240f,
        useCenter = false,
        topLeft = arcRect.topLeft,
        size = arcRect.size,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
      )

      // 2. Dynamic Cyan Arc (Power)
      drawArc(
        color = arcColor,
        startAngle = 150f,
        sweepAngle = 160f,
        useCenter = false,
        topLeft = arcRect.topLeft,
        size = arcRect.size,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
      )

      // 3. Ascending Power Wave (Subtle curve)
      val wavePath = Path().apply {
        moveTo(centerX - radius * 0.75f, centerY + radius * 0.25f)
        cubicTo(
          centerX - radius * 0.2f, centerY + radius * 0.2f,
          centerX + radius * 0.2f, centerY - radius * 0.2f,
          centerX + radius * 0.7f, centerY - radius * 0.55f
        )
      }
      drawPath(
        path = wavePath,
        color = waveColor,
        style = Stroke(width = strokeWidth * 0.35f, cap = StrokeCap.Round)
      )

      // 4. Center Pivot Circle
      drawCircle(
        color = pivotBgColor,
        radius = radius * 0.32f,
        center = Offset(centerX, centerY)
      )

      // 5. Orange Needle Pointing Up-Right (~315 degrees / -45 deg)
      val needleAngleRad = Math.toRadians(-40.0)
      val needleLength = radius * 0.95f
      val tipX = centerX + needleLength * cos(needleAngleRad).toFloat()
      val tipY = centerY + needleLength * sin(needleAngleRad).toFloat()

      drawLine(
        color = needleColor,
        start = Offset(centerX, centerY),
        end = Offset(tipX, tipY),
        strokeWidth = strokeWidth * 0.45f,
        cap = StrokeCap.Round
      )

      // Center Hub
      drawCircle(
        color = needleColor,
        radius = radius * 0.12f,
        center = Offset(centerX, centerY)
      )

      // 6. Discrete "DL" text at center-bottom
      val dlSizePx = with(density) { (size.value * 0.16f).sp.toPx() }
      dlPaint.textSize = dlSizePx
      dlPaint.color = textCol

      drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText("DL", centerX, centerY + radius * 0.82f, dlPaint)
      }
    }
  }
}

/**
 * Logo Completo do DYNO LITE:
 * Símbolo vetorial + Texto "DYNO LITE" + Subtítulo oficial
 */
@Composable
fun DynoLogo(
  modifier: Modifier = Modifier,
  symbolSize: Dp = 60.dp,
  showSubtitle: Boolean = true,
  subtitleText: String = "Desempenho do seu carro de forma simples",
  isCompact: Boolean = false
) {
  if (isCompact) {
    Row(
      modifier = modifier.testTag("dyno_logo_compact"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      DynoLogoSymbol(size = symbolSize)
      Column {
        Text(
          text = "DYNO LITE",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            fontSize = 18.sp
          ),
          color = DynoTextPrimary
        )
        if (showSubtitle) {
          Text(
            text = "Desempenho simples",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Normal,
              fontSize = 11.sp
            ),
            color = DynoTextSecondary
          )
        }
      }
    }
  } else {
    Column(
      modifier = modifier.testTag("dyno_logo_full"),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      DynoLogoSymbol(size = symbolSize)

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = "DYNO LITE",
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.2.sp,
          fontSize = 22.sp
        ),
        color = DynoTextPrimary
      )

      if (showSubtitle) {
        Text(
          text = subtitleText,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 13.5.sp,
            lineHeight = 18.sp
          ),
          color = DynoTextSecondary,
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

/**
 * Versão monocromática para ícones de barra de ferramentas, rodapé ou exportações
 */
@Composable
fun DynoLogoMonochrome(
  modifier: Modifier = Modifier,
  size: Dp = 32.dp,
  tint: Color = DynoTextPrimary
) {
  DynoLogoSymbol(
    modifier = modifier,
    size = size,
    isMonochrome = true,
    monochromeColor = tint
  )
}
