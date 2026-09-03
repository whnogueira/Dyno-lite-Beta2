package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DynoTextPrimary
import com.example.ui.theme.DynoTextSecondary

/**
 * Símbolo vetorial oficial do DYNO LITE:
 * - Emblema estilizado "D" com velocímetro/tacômetro integrado
 * - Ponteiro laranja apontando para a zona vermelha (~45 graus / 2 horas)
 * - Fundo azul-marinho
 * - Traços aerodinâmicos de velocidade à esquerda
 * - 100% livre de textos/letras "DL" legadas
 */
@Composable
fun DynoLogoSymbol(
  modifier: Modifier = Modifier,
  size: Dp = 64.dp,
  isMonochrome: Boolean = false,
  monochromeColor: Color = DynoTextPrimary
) {
  Box(
    modifier = modifier
      .size(size)
      .semantics { contentDescription = "Símbolo Dyno Lite" }
      .testTag("dyno_logo_symbol"),
    contentAlignment = Alignment.Center
  ) {
    Image(
      painter = painterResource(id = R.drawable.ic_dyno_symbol),
      contentDescription = "Símbolo Dyno Lite",
      modifier = Modifier.size(size),
      contentScale = ContentScale.Fit,
      colorFilter = if (isMonochrome) ColorFilter.tint(monochromeColor) else null
    )
  }
}

/**
 * Logo Horizontal Oficial DYNO LITE:
 * - Emblema D-velocímetro à esquerda
 * - Tipografia "DYNO LITE" esportiva em itálico à direita
 * - Preserva fundo transparente
 * - Não deforma e mantém proporção original via ContentScale.Fit
 * - Aspect ratio padrão 1200:340 (~3.53:1)
 */
@Composable
fun DynoHorizontalLogo(
  modifier: Modifier = Modifier,
  height: Dp = 36.dp,
  contentScale: ContentScale = ContentScale.Fit
) {
  Image(
    painter = painterResource(id = R.drawable.dyno_horizontal_logo),
    contentDescription = "DYNO LITE Logo",
    modifier = modifier
      .height(height)
      .aspectRatio(1200f / 340f, matchHeightConstraintsFirst = true)
      .testTag("dyno_horizontal_logo"),
    contentScale = contentScale
  )
}

/**
 * Logo Completo do DYNO LITE para telas e cabeçalhos:
 * - Usa a imagem horizontal "DYNO LITE" com ContentScale.Fit e proporção preservada
 * - Suporta subtítulo descritivo institucional quando solicitado
 */
@Composable
fun DynoLogo(
  modifier: Modifier = Modifier,
  symbolSize: Dp = 58.dp,
  showSubtitle: Boolean = true,
  subtitleText: String = "Desempenho do seu carro de forma simples",
  isCompact: Boolean = false
) {
  if (isCompact) {
    DynoHorizontalLogo(
      height = (symbolSize * 0.55f).coerceAtLeast(26.dp),
      modifier = modifier.testTag("dyno_logo_compact")
    )
  } else {
    Column(
      modifier = modifier
        .widthIn(max = 380.dp)
        .testTag("dyno_logo_full"),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      DynoHorizontalLogo(
        height = symbolSize.coerceAtLeast(44.dp),
        modifier = Modifier.fillMaxWidth(0.85f)
      )

      if (showSubtitle) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitleText,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 17.sp
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
