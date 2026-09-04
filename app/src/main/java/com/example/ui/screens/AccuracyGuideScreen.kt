package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccuracyGuideScreen(
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  Scaffold(
    modifier = modifier.fillMaxSize().testTag("accuracy_guide_screen"),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "GUIA DE PRECISÃO",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Voltar"
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
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
          .padding(horizontal = 20.dp, vertical = 16.dp)
          .widthIn(max = 480.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Logo & Hero Header
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth()
        ) {
          // Logo Oficial DYNO LITE institucional
          Image(
            painter = painterResource(id = R.drawable.dyno_horizontal_logo),
            contentDescription = "DYNO LITE",
            modifier = Modifier
              .fillMaxWidth(0.85f)
              .height(56.dp),
            contentScale = ContentScale.Fit
          )

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "Descubra o desempenho estimado do seu carro",
            style = MaterialTheme.typography.bodyMedium.copy(
              fontSize = 14.sp,
              lineHeight = 19.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
          )
        }

        // Safety Disclaimer Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
          ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
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
                text = "SEGURANÇA EM PRIMEIRO LUGAR",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.error
              )
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

        // Accuracy Disclaimer Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.Info,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "Antes do primeiro teste:",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "Os resultados são estimativas e podem variar conforme o peso informado, a inclinação da pista, a calibração dos sensores e a precisão do GPS.",
                style = MaterialTheme.typography.bodySmall.copy(
                  lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Text(
          text = "PASSOS PARA UMA LEITURA REAL E PRECISA",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
          ),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.align(Alignment.Start)
        )

        // Step 1: Fixação
        GuideStepCard(
          stepNumber = "1",
          title = "Fixação firme do celular",
          icon = Icons.Outlined.PhoneAndroid,
          description = "Fixe o celular firmemente em um suporte, na posição indicada pela calibração."
        )

        // Step 2: Calibração
        GuideStepCard(
          stepNumber = "2",
          title = "Calibre os sensores",
          icon = Icons.Outlined.Straighten,
          description = "Com o veículo parado e o motor funcionando, calibre os sensores."
        )

        // Step 3: Configuração do teste
        GuideStepCard(
          stepNumber = "3",
          title = "Configuração do teste",
          icon = Icons.Outlined.Tune,
          description = "Selecione a marcha, a velocidade de início e confirme o peso total do teste."
        )

        // Step 4: Preparar teste
        GuideStepCard(
          stepNumber = "4",
          title = "Preparar teste com o veículo parado",
          icon = Icons.Outlined.Speed,
          description = "Toque em “Preparar teste” ainda com o veículo parado. A medição começará automaticamente quando a velocidade selecionada for cruzada pelo GPS."
        )

        // Step 5: Aceleração contínua e encerramento
        GuideStepCard(
          stepNumber = "5",
          title = "Aceleração contínua e encerramento",
          icon = Icons.Filled.Speed,
          description = "Acelere continuamente na mesma marcha. Ao aliviar o acelerador, o GPS confirmará a desaceleração e encerrará a passagem automaticamente."
        )

        // Step 6: Sem troca de marcha
        GuideStepCard(
          stepNumber = "6",
          title = "Sem troca de marcha durante a medição",
          icon = Icons.Outlined.CheckCircle,
          description = "Não troque de marcha durante uma medição de potência. Se houver troca de marcha ou acionamento da embreagem, a passagem poderá ser marcada como incompleta."
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
          onClick = onNavigateBack,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("btn_close_guide"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          )
        ) {
          Text(
            text = "ENTENDI, VOLTAR",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              fontSize = 15.sp
            )
          )
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun GuideStepCard(
  stepNumber: String,
  title: String,
  icon: ImageVector,
  description: String,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Surface(
          modifier = Modifier.size(32.dp),
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer
        ) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
              text = stepNumber,
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              ),
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }

        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          ),
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f)
        )

        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
      }

      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall.copy(
          lineHeight = 19.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
