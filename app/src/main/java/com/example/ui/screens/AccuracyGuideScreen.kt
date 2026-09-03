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
            contentDescription = "Logo DYNO LITE",
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
                text = "Prepare o teste antes de movimentar o veículo. Nunca opere o celular enquanto estiver dirigindo. Faça os testes sempre em locais planos, seguros e fechados ao trânsito público.",
                style = MaterialTheme.typography.bodySmall.copy(
                  lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
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
                text = "SOBRE OS RESULTADOS",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "Os resultados obtidos são estimativas baseadas na aceleração inercial e no GPS. Fatores como peso incorreto, inclinação da via, calibração instável e precisão do GPS podem influenciar a leitura.",
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
          description = "Coloque o aparelho em um suporte firme, na posição vertical (retrato), com a tela voltada para o motorista e a traseira apontada para a frente do carro. O suporte não deve vibrar ou soltar."
        )

        // Step 2: Calibração
        GuideStepCard(
          stepNumber = "2",
          title = "Calibre o zero com o carro parado",
          icon = Icons.Outlined.Straighten,
          description = "Com o celular preso no suporte, pare o carro completamente em piso plano e mantenha o motor funcionando em marcha lenta. Toque em 'Calibrar zero no suporte' para neutralizar a vibração e a inclinação."
        )

        // Step 3: Dados do veículo
        GuideStepCard(
          stepNumber = "3",
          title = "Configuração do peso e pneu",
          icon = Icons.Outlined.Tune,
          description = "Na Garagem, confira o peso do carro somando motorista, passageiros, combustível e bagagens. Verifique também as medidas corretas do pneu (ex: 185/65 R15) para máxima precisão."
        )

        // Step 4: Iniciar com o carro parado
        GuideStepCard(
          stepNumber = "4",
          title = "Iniciar com o carro parado",
          icon = Icons.Outlined.Speed,
          description = "Toque em 'INICIAR COM O CARRO PARADO'. O sistema entrará em modo preparado aguardando a aceleração. Não mexa mais no celular."
        )

        // Step 5: Disparo automático e Aceleração total
        GuideStepCard(
          stepNumber = "5",
          title = "Disparo automático na velocidade selecionada",
          icon = Icons.Filled.Speed,
          description = "Engate a marcha do teste (recomendamos 3ª marcha), comece a acelerar suavemente abaixo da velocidade de disparo (40, 50 ou 60 km/h) e, ao cruzar o gatilho em aceleração total (WOT), o Dyno Lite inicia a integração automaticamente com a velocidade real do GPS."
        )

        // Step 6: Finalização
        GuideStepCard(
          stepNumber = "6",
          title = "Finalização automática na embreagem",
          icon = Icons.Outlined.CheckCircle,
          description = "Ao atingir o limite de rotação do motor, pise na embreagem ou tire o pé do acelerador. O Dyno Lite detecta a desaceleração longitudinal e finaliza a medição instantaneamente, gravando os dados."
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
