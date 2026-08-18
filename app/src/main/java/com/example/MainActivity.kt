package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DynoLiteTheme

enum class AppScreen {
  HOME,
  SENSORS
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      DynoLiteTheme {
        var currentScreen by remember { mutableStateOf(AppScreen.HOME) }

        when (currentScreen) {
          AppScreen.HOME -> DynoLiteHomeScreen(
            onNavigateToSensors = { currentScreen = AppScreen.SENSORS }
          )
          AppScreen.SENSORS -> SensorScreen(
            onNavigateBack = { currentScreen = AppScreen.HOME }
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynoLiteHomeScreen(
  onNavigateToSensors: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableIntStateOf(0) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Medium,
              letterSpacing = (-0.5).sp,
              fontSize = 22.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("app_title_text")
          )
        },
        navigationIcon = {
          IconButton(
            onClick = { /* Menu */ },
            modifier = Modifier.testTag("menu_button")
          ) {
            Icon(
              imageVector = Icons.Default.Menu,
              contentDescription = "Menu",
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }
        },
        actions = {
          // Circular Avatar Badge "DL"
          Box(
            modifier = Modifier
              .padding(end = 16.dp)
              .size(40.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "DL",
              style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
              ),
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
    bottomBar = {
      Column {
        HorizontalDivider(
          thickness = 1.dp,
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
        NavigationBar(
          containerColor = MaterialTheme.colorScheme.background,
          tonalElevation = 0.dp,
          modifier = Modifier.height(72.dp),
        ) {
          NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            icon = {
              Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                modifier = Modifier.size(24.dp),
              )
            },
            label = {
              Text(
                text = "HOME",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                  fontSize = 11.sp,
                  letterSpacing = 0.5.sp,
                )
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
              selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            ),
          )
          NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            icon = {
              Icon(
                imageVector = Icons.Outlined.ReceiptLong,
                contentDescription = "Logs",
                modifier = Modifier.size(24.dp),
              )
            },
            label = {
              Text(
                text = "LOGS",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                  fontSize = 11.sp,
                  letterSpacing = 0.5.sp,
                )
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
              selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            ),
          )
          NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            icon = {
              Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Info",
                modifier = Modifier.size(24.dp),
              )
            },
            label = {
              Text(
                text = "INFO",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                  fontSize = 11.sp,
                  letterSpacing = 0.5.sp,
                )
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
              selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            ),
          )
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 16.dp)
          .widthIn(max = 480.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // Hero section
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth()
        ) {
          Spacer(modifier = Modifier.height(16.dp))

          // Icon Container - Rounded 28dp card
          Surface(
            modifier = Modifier.size(96.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
          ) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Outlined.Sensors,
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }

          Spacer(modifier = Modifier.height(24.dp))

          // Prototype Subtitle / Title
          Text(
            text = stringResource(R.string.prototype_subtitle),
            style = MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.Normal,
              fontSize = 30.sp,
              lineHeight = 36.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("app_subtitle_text")
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Version Badge Pill
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.testTag("version_badge")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "VERSÃO ${stringResource(R.string.version_label)}",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Medium,
                  fontSize = 12.sp,
                  letterSpacing = 0.8.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action Buttons stack (Sleek pill buttons)
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          // 1. INICIAR TESTE (Primary Rich Blue pill button)
          Button(
            onClick = { /* Primeira etapa: sem ação */ },
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
              .testTag("start_test_button"),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            contentPadding = PaddingValues(horizontal = 24.dp),
          ) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.btn_start_test).uppercase(),
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
              )
            )
          }

          // 2. TESTAR SENSORES (Tonal light blue pill button)
          FilledTonalButton(
            onClick = onNavigateToSensors,
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
              .testTag("test_sensors_button"),
            shape = CircleShape,
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            contentPadding = PaddingValues(horizontal = 24.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Sensors,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.btn_test_sensors).uppercase(),
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
              )
            )
          }

          // 3. CONFIGURAÇÃO (Outlined pill button)
          OutlinedButton(
            onClick = { /* Primeira etapa: sem ação */ },
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
              .testTag("settings_button"),
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            contentPadding = PaddingValues(horizontal = 24.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Settings,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.btn_settings).uppercase(),
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
              )
            )
          }
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun DynoLiteHomeScreenPreview() {
  DynoLiteTheme {
    DynoLiteHomeScreen()
  }
}

