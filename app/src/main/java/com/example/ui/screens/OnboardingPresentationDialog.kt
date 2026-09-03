package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
          contentDescription = "Logo DYNO LITE",
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

        // Safety Warning Box
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.Security,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(22.dp)
            )
            Text(
              text = "Prepare o teste antes de movimentar o veículo. Nunca mexa no celular dirigindo.",
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                lineHeight = 16.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        // Disclaimer Box
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = "Antes do primeiro teste:",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.primary
              )
            }
            Text(
              text = "Os resultados são estimativas e podem variar com peso, inclinação da pista, calibração e precisão do GPS.",
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                lineHeight = 16.sp
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Checklist of essentials
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          QuickPointRow(
            number = "1",
            text = "Fixe o celular firmemente no suporte vertical (tela para você, traseira para a frente)."
          )
          QuickPointRow(
            number = "2",
            text = "Calibre o zero no suporte com o carro parado e motor ligado."
          )
          QuickPointRow(
            number = "3",
            text = "Inicie o procedimento com o carro parado."
          )
          QuickPointRow(
            number = "4",
            text = "O teste começa automaticamente na velocidade selecionada (40, 50 ou 60 km/h) pelo GPS real."
          )
          QuickPointRow(
            number = "5",
            text = "Ao desacelerar ou pisar na embreagem, o teste finaliza e salva a passagem."
          )
        }

        HorizontalDivider(
          thickness = 0.8.dp,
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )

        // Don't show again checkbox
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { dontShowAgain = !dontShowAgain }
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Checkbox(
            checked = dontShowAgain,
            onCheckedChange = { dontShowAgain = it },
            colors = CheckboxDefaults.colors(
              checkedColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("checkbox_dont_show_again")
          )
          Text(
            text = "Não mostrar novamente na inicialização",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
          )
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
