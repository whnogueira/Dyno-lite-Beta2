package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VehicleRepository
import com.example.model.VehicleProfile
import com.example.ui.screens.GarageScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HowItWorksScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TestPreparationScreen
import com.example.ui.screens.VehicleWizardScreen
import com.example.ui.theme.DynoLiteTheme

enum class AppDestination {
  MAIN_TABS,
  VEHICLE_WIZARD,
  TEST_PREPARATION,
  SETTINGS,
  HOW_IT_WORKS,
  SENSORS
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      DynoLiteTheme {
        DynoLiteApp()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynoLiteApp() {
  val context = LocalContext.current
  val repository = remember { VehicleRepository(context) }

  var vehicles by remember { mutableStateOf(repository.getVehicles()) }
  var currentDestination by remember { mutableStateOf(AppDestination.MAIN_TABS) }
  var selectedTabIndex by remember { mutableIntStateOf(0) }
  var vehicleToEdit by remember { mutableStateOf<VehicleProfile?>(null) }

  val primaryVehicle = repository.getPrimaryVehicle()

  // Hardware / gesture back navigation handler
  BackHandler(enabled = currentDestination != AppDestination.MAIN_TABS || selectedTabIndex != 0) {
    if (currentDestination != AppDestination.MAIN_TABS) {
      if (currentDestination == AppDestination.HOW_IT_WORKS) {
        currentDestination = AppDestination.SETTINGS
      } else {
        currentDestination = AppDestination.MAIN_TABS
      }
    } else if (selectedTabIndex != 0) {
      selectedTabIndex = 0
    }
  }

  when (currentDestination) {
    AppDestination.MAIN_TABS -> {
      Scaffold(
        modifier = Modifier.fillMaxSize().testTag("main_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
          TopAppBar(
            title = {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp,
                  fontSize = 20.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("top_bar_app_title")
              )
            },
            actions = {
              IconButton(
                onClick = { currentDestination = AppDestination.SETTINGS },
                modifier = Modifier.testTag("top_bar_settings_button")
              ) {
                Icon(
                  imageVector = Icons.Outlined.Settings,
                  contentDescription = "Configurações",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
              thickness = 0.8.dp,
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.background,
              tonalElevation = 0.dp,
              modifier = Modifier.height(72.dp),
            ) {
              // 1. INÍCIO
              NavigationBarItem(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                icon = {
                  Icon(
                    imageVector = if (selectedTabIndex == 0) Icons.Default.Home else Icons.Outlined.Home,
                    contentDescription = "Início",
                    modifier = Modifier.size(24.dp),
                  )
                },
                label = {
                  Text(
                    text = "INÍCIO",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium,
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
                modifier = Modifier.testTag("nav_tab_home")
              )

              // 2. GARAGEM
              NavigationBarItem(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                icon = {
                  Icon(
                    imageVector = if (selectedTabIndex == 1) Icons.Default.DirectionsCar else Icons.Outlined.DirectionsCar,
                    contentDescription = "Garagem",
                    modifier = Modifier.size(24.dp),
                  )
                },
                label = {
                  Text(
                    text = "GARAGEM",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium,
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
                modifier = Modifier.testTag("nav_tab_garage")
              )

              // 3. RESULTADOS
              NavigationBarItem(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                icon = {
                  Icon(
                    imageVector = if (selectedTabIndex == 2) Icons.Default.Assessment else Icons.Outlined.Assessment,
                    contentDescription = "Resultados",
                    modifier = Modifier.size(24.dp),
                  )
                },
                label = {
                  Text(
                    text = "RESULTADOS",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Medium,
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
                modifier = Modifier.testTag("nav_tab_results")
              )
            }
          }
        }
      ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
          when (selectedTabIndex) {
            0 -> HomeScreen(
              primaryVehicle = primaryVehicle,
              onNavigateToWizard = {
                vehicleToEdit = null
                currentDestination = AppDestination.VEHICLE_WIZARD
              },
              onNavigateToGarage = { selectedTabIndex = 1 },
              onNavigateToTestPrep = {
                if (primaryVehicle != null) {
                  currentDestination = AppDestination.TEST_PREPARATION
                } else {
                  vehicleToEdit = null
                  currentDestination = AppDestination.VEHICLE_WIZARD
                }
              },
              onNavigateToSettings = { currentDestination = AppDestination.SETTINGS }
            )
            1 -> GarageScreen(
              vehicles = vehicles,
              onAddVehicle = {
                vehicleToEdit = null
                currentDestination = AppDestination.VEHICLE_WIZARD
              },
              onEditVehicle = { veh ->
                vehicleToEdit = veh
                currentDestination = AppDestination.VEHICLE_WIZARD
              },
              onSetPrimaryVehicle = { id ->
                repository.setPrimaryVehicle(id)
                vehicles = repository.getVehicles()
              },
              onDeleteVehicle = { id ->
                repository.deleteVehicle(id)
                vehicles = repository.getVehicles()
              }
            )
            2 -> ResultsScreen(
              onStartNewTest = {
                if (primaryVehicle != null) {
                  currentDestination = AppDestination.TEST_PREPARATION
                } else {
                  vehicleToEdit = null
                  currentDestination = AppDestination.VEHICLE_WIZARD
                }
              }
            )
          }
        }
      }
    }

    AppDestination.VEHICLE_WIZARD -> {
      VehicleWizardScreen(
        existingVehicle = vehicleToEdit,
        onSaveVehicle = { savedVeh ->
          repository.saveVehicle(savedVeh)
          vehicles = repository.getVehicles()
          currentDestination = AppDestination.MAIN_TABS
          selectedTabIndex = 1
        },
        onCancel = {
          currentDestination = AppDestination.MAIN_TABS
        }
      )
    }

    AppDestination.TEST_PREPARATION -> {
      val veh = primaryVehicle ?: vehicles.firstOrNull()
      if (veh != null) {
        TestPreparationScreen(
          vehicle = veh,
          onProceedToSensorScreen = {
            currentDestination = AppDestination.SENSORS
          },
          onEditVehicle = {
            vehicleToEdit = veh
            currentDestination = AppDestination.VEHICLE_WIZARD
          },
          onNavigateBack = {
            currentDestination = AppDestination.MAIN_TABS
          }
        )
      } else {
        currentDestination = AppDestination.MAIN_TABS
      }
    }

    AppDestination.SETTINGS -> {
      SettingsScreen(
        onNavigateToSensorDiagnostic = {
          currentDestination = AppDestination.SENSORS
        },
        onNavigateToHowItWorks = {
          currentDestination = AppDestination.HOW_IT_WORKS
        },
        onNavigateBack = {
          currentDestination = AppDestination.MAIN_TABS
        }
      )
    }

    AppDestination.HOW_IT_WORKS -> {
      HowItWorksScreen(
        onNavigateBack = {
          currentDestination = AppDestination.SETTINGS
        }
      )
    }

    AppDestination.SENSORS -> {
      SensorScreen(
        onNavigateBack = {
          currentDestination = AppDestination.MAIN_TABS
        },
        onNavigateToResults = {
          currentDestination = AppDestination.MAIN_TABS
          selectedTabIndex = 2
        }
      )
    }
  }
}
