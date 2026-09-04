package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R

@Composable
fun OnboardingPresentationDialog(
  onDismiss: (dontShowAgain: Boolean) -> Unit,
  onOpenFullGuide: () -> Unit
) {
  var dontShowAgain by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = { onDismiss(dontShowAgain) },
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .padding(vertical = 24.dp)
        .testTag("onboarding_guide_dialog"),
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.background,
      tonalElevation = 8.dp,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Logo and Header
        // Logo Oficial DYNO LITE em versão ampliada na tela de abertura
        Image(
          painter = painterResource(id = R.drawable.dyno_horizontal_logo),
          contentDescription = "DYNO LITE",
          modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(56.dp)
            .testTag("onboarding_title"),
          contentScale = ContentScale.Fit
        )

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = "Descubra o desempenho estimado do seu carro",
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.Medium,
              fontSize = 14.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
          )
        }

        // Safety Warning Box - Aviso de segurança em destaque
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.Security,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "Prepare o teste antes de movimentar o veículo.\nNunca mexa no celular dirigindo.",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.5.sp,
                  lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Não é permitido iniciar configurações que exijam interação com o veículo em movimento.",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 11.5.sp,
                  lineHeight = 15.sp,
                  fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.error
              )
            }
          }
        }

        // Disclaimer Box - Texto Introdutório
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = "Antes do primeiro teste:",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.primary
              )
            }
            Text(
              text = "Os resultados são estimativas e podem variar conforme o peso informado, a inclinação da pista, a calibração dos sensores e a precisão do GPS.",
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.5.sp,
                lineHeight = 17.sp
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Checklist de instruções corrigidas (1 a 6)
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          QuickPointRow(
            number = "1",
            text = "Fixe o celular firmemente em um suporte, na posição indicada pela calibração."
          )
          QuickPointRow(
            number = "2",
            text = "Com o veículo parado e o motor funcionando, calibre os sensores."
          )
          QuickPointRow(
            number = "3",
            text = "Selecione a marcha, a velocidade de início e confirme o peso total do teste."
          )
          QuickPointRow(
            number = "4",
            text = "Toque em “Preparar teste” ainda com o veículo parado. A medição começará automaticamente quando a velocidade selecionada for cruzada pelo GPS."
          )
          QuickPointRow(
            number = "5",
            text = "Acelere continuamente na mesma marcha. Ao aliviar o acelerador, o GPS confirmará a desaceleração e encerrará a passagem automaticamente."
          )
          QuickPointRow(
            number = "6",
            text = "Não troque de marcha durante uma medição de potência. Se houver troca de marcha ou acionamento da embreagem, a passagem poderá ser marcada como incompleta."
          )
        }

        HorizontalDivider(
          thickness = 0.8.dp,
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )

        // Don't show again checkbox com área de toque mínima de 48dp, borda visível e ciano
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { dontShowAgain = !dontShowAgain }
            .testTag("row_dont_show_again"),
          shape = RoundedCornerShape(12.dp),
          color = if (dontShowAgain) Color(0xFF00E5FF).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          border = BorderStroke(1.dp, if (dontShowAgain) Color(0xFF00E5FF) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 48.dp)
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Checkbox(
              checked = dontShowAgain,
              onCheckedChange = { dontShowAgain = it },
              colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF00E5FF),
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkmarkColor = Color(0xFF0B141A)
              ),
              modifier = Modifier
                .size(24.dp)
                .testTag("checkbox_dont_show_again")
            )
            Text(
              text = "Não mostrar novamente na inicialização",
              style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                fontWeight = if (dontShowAgain) FontWeight.SemiBold else FontWeight.Normal
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        // Action Buttons
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = { onDismiss(dontShowAgain) },
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("btn_dismiss_onboarding"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            )
          ) {
            Text(
              text = "ENTENDI, VAMOS COMEÇAR",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
            )
          }

          Button(
            onClick = onOpenFullGuide,
            modifier = Modifier
              .fillMaxWidth()
              .height(44.dp)
              .testTag("btn_open_full_guide_from_dialog"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
              contentColor = MaterialTheme.colorScheme.onSurface
            )
          ) {
            Text(
              text = "VER GUIA COMPLETO DE PRECISÃO",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
              )
            )
          }
        }
      }
    }
  }
}

@Composable
private fun QuickPointRow(number: String, text: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Surface(
      modifier = Modifier.size(22.dp),
      shape = CircleShape,
      color = MaterialTheme.colorScheme.primaryContainer
    ) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
          text = number,
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          ),
          color = MaterialTheme.colorScheme.onPrimaryContainer
        )
      }
    }
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall.copy(
        fontSize = 12.5.sp,
        lineHeight = 17.sp
      ),
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}
