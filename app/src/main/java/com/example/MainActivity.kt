package com.example

import android.content.Context
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VehicleRepository
import com.example.model.TestMode
import com.example.model.VehicleProfile
import com.example.ui.components.DynoBottomNavigation
import com.example.ui.components.DynoTab
import com.example.ui.components.DynoTopBar
import com.example.ui.screens.AccuracyGuideScreen
import com.example.ui.screens.GarageScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingPresentationDialog
import com.example.ui.screens.ResultsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SimulatorScreen
import com.example.ui.screens.TestPreparationScreen
import com.example.ui.screens.VehicleWizardScreen
import com.example.ui.theme.DynoLiteTheme

enum class AppDestination {
  MAIN_TABS,
  VEHICLE_WIZARD,
  TEST_PREPARATION,
  SETTINGS,
  ACCURACY_GUIDE,
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
  val prefs = remember(context) {
    context.getSharedPreferences("dyno_lite_prefs", Context.MODE_PRIVATE)
  }

  var vehicles by remember { mutableStateOf(repository.getVehicles()) }
  var currentDestination by remember { mutableStateOf(AppDestination.MAIN_TABS) }
  var previousDestination by remember { mutableStateOf<AppDestination?>(null) }
  var selectedTab by remember { mutableStateOf(DynoTab.HOME) }
  var vehicleToEdit by remember { mutableStateOf<VehicleProfile?>(null) }
  var activeTestVehicle by remember { mutableStateOf<VehicleProfile?>(null) }
  var activeTestMode by remember { mutableStateOf(TestMode.DYNO) }
  var homeFeedbackMessage by remember { mutableStateOf<String?>(null) }
  var simulatorInitialRunId by remember { mutableStateOf<String?>(null) }

  // Onboarding presentation dialog state for version 0.16.0
  var showOnboardingDialog by remember {
    mutableStateOf(!prefs.getBoolean("has_seen_accuracy_guide_v16", false))
  }

  val primaryVehicle = repository.getPrimaryVehicle()

  // Hardware / gesture back navigation handler
  BackHandler(enabled = currentDestination != AppDestination.MAIN_TABS || selectedTab != DynoTab.HOME) {
    if (currentDestination != AppDestination.MAIN_TABS) {
      if (currentDestination == AppDestination.ACCURACY_GUIDE) {
        currentDestination = previousDestination ?: AppDestination.MAIN_TABS
      } else {
        currentDestination = AppDestination.MAIN_TABS
        selectedTab = DynoTab.HOME
      }
    } else if (selectedTab != DynoTab.HOME) {
      selectedTab = DynoTab.HOME
    }
  }

  if (showOnboardingDialog) {
    OnboardingPresentationDialog(
      onDismiss = { dontShowAgain ->
        if (dontShowAgain) {
          prefs.edit().putBoolean("has_seen_accuracy_guide_v16", true).apply()
        }
        showOnboardingDialog = false
      },
      onOpenFullGuide = {
        showOnboardingDialog = false
        previousDestination = currentDestination
        currentDestination = AppDestination.ACCURACY_GUIDE
      }
    )
  }

  when (currentDestination) {
    AppDestination.MAIN_TABS -> {
      Scaffold(
        modifier = Modifier.fillMaxSize().testTag("main_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
          DynoTopBar(
            title = stringResource(R.string.app_name),
            showBrandLogo = true,
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
            }
          )
        },
        bottomBar = {
          DynoBottomNavigation(
            selectedTab = selectedTab,
            onTabSelected = { clickedTab ->
              // Limpa feedback se o usuário navega voluntariamente
              if (clickedTab != DynoTab.HOME) {
                homeFeedbackMessage = null
              }
              selectedTab = clickedTab
            }
          )
        }
      ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
          when (selectedTab) {
            DynoTab.HOME -> HomeScreen(
              primaryVehicle = primaryVehicle,
              feedbackMessage = homeFeedbackMessage,
              onDismissFeedback = { homeFeedbackMessage = null },
              onNavigateToWizard = {
                vehicleToEdit = null
                currentDestination = AppDestination.VEHICLE_WIZARD
              },
              onNavigateToGarage = { selectedTab = DynoTab.GARAGE },
              onNavigateToMode = { mode ->
                activeTestMode = mode
                homeFeedbackMessage = null
                if (primaryVehicle != null) {
                  activeTestVehicle = primaryVehicle
                  currentDestination = AppDestination.TEST_PREPARATION
                } else {
                  vehicleToEdit = null
                  currentDestination = AppDestination.VEHICLE_WIZARD
                }
              },
              onNavigateToTestPrep = {
                activeTestMode = TestMode.DYNO
                homeFeedbackMessage = null
                if (primaryVehicle != null) {
                  activeTestVehicle = primaryVehicle
                  currentDestination = AppDestination.TEST_PREPARATION
                } else {
                  vehicleToEdit = null
                  currentDestination = AppDestination.VEHICLE_WIZARD
                }
              },
              onNavigateToSettings = { currentDestination = AppDestination.SETTINGS },
              onNavigateToGuide = {
                previousDestination = AppDestination.MAIN_TABS
                currentDestination = AppDestination.ACCURACY_GUIDE
              }
            )
            DynoTab.GARAGE -> GarageScreen(
              vehicles = vehicles,
              onAddVehicle = {
                vehicleToEdit = null
                currentDestination = AppDestination.VEHICLE_WIZARD
              },
              onEditVehicle = { veh ->
                vehicleToEdit = veh
                currentDestination = AppDestination.VEHICLE_WIZARD
              },
              onDuplicateVehicle = { id ->
                repository.duplicateVehicle(id)
                vehicles = repository.getVehicles()
              },
              onSetPrimaryVehicle = { id ->
                repository.setPrimaryVehicle(id)
                vehicles = repository.getVehicles()
              },
              onDeleteVehicle = { id ->
                repository.deleteVehicle(id)
                vehicles = repository.getVehicles()
              },
              onTestVehicle = { veh ->
                repository.setPrimaryVehicle(veh.id)
                vehicles = repository.getVehicles()
                activeTestVehicle = veh
                homeFeedbackMessage = null
                currentDestination = AppDestination.TEST_PREPARATION
              },
              onNavigateToHome = {
                selectedTab = DynoTab.HOME
              }
            )
            DynoTab.RESULTS -> ResultsScreen(
              onStartNewTest = { vehicleId ->
                val targetVeh = vehicleId?.let { id -> vehicles.find { it.id == id } }
                  ?: primaryVehicle
                  ?: vehicles.firstOrNull()
                if (targetVeh != null) {
                  activeTestVehicle = targetVeh
                  homeFeedbackMessage = null
                  currentDestination = AppDestination.TEST_PREPARATION
                } else {
                  vehicleToEdit = null
                  currentDestination = AppDestination.VEHICLE_WIZARD
                }
              },
              onOpenSimulator = { runId ->
                simulatorInitialRunId = runId
                selectedTab = DynoTab.SIMULATOR
              }
            )
            DynoTab.SIMULATOR -> SimulatorScreen(
              initialRunId = simulatorInitialRunId,
              onNavigateToRunDetails = {
                selectedTab = DynoTab.RESULTS
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
          repository.setPrimaryVehicle(savedVeh.id)
          vehicles = repository.getVehicles()
          activeTestVehicle = savedVeh
          homeFeedbackMessage = null
          currentDestination = AppDestination.TEST_PREPARATION
        },
        onCancel = {
          if (activeTestVehicle != null || previousDestination == AppDestination.TEST_PREPARATION) {
            currentDestination = AppDestination.TEST_PREPARATION
          } else {
            currentDestination = AppDestination.MAIN_TABS
            selectedTab = DynoTab.HOME
          }
        }
      )
    }

    AppDestination.TEST_PREPARATION -> {
      val veh = activeTestVehicle ?: primaryVehicle ?: vehicles.firstOrNull()
      if (veh != null) {
        TestPreparationScreen(
          vehicle = veh,
          initialTestMode = activeTestMode,
          onNavigateToHome = { saved ->
            activeTestVehicle = null
            currentDestination = AppDestination.MAIN_TABS
            if (saved) {
              selectedTab = DynoTab.RESULTS
              homeFeedbackMessage = "Passagem concluída e salva com sucesso!"
            } else {
              selectedTab = DynoTab.HOME
              homeFeedbackMessage = "Não foi possível salvar a passagem."
            }
          },
          onSwitchVehicle = {
            activeTestVehicle = null
            currentDestination = AppDestination.MAIN_TABS
            selectedTab = DynoTab.GARAGE
          },
          onEditVehicle = {
            vehicleToEdit = veh
            previousDestination = AppDestination.TEST_PREPARATION
            currentDestination = AppDestination.VEHICLE_WIZARD
          },
          onNavigateBack = {
            activeTestVehicle = null
            homeFeedbackMessage = null
            currentDestination = AppDestination.MAIN_TABS
            selectedTab = DynoTab.HOME
          }
        )
      } else {
        currentDestination = AppDestination.MAIN_TABS
        selectedTab = DynoTab.HOME
      }
    }

    AppDestination.SETTINGS -> {
      SettingsScreen(
        onNavigateToSensorDiagnostic = {
          currentDestination = AppDestination.SENSORS
        },
        onNavigateToHowItWorks = {
          previousDestination = AppDestination.SETTINGS
          currentDestination = AppDestination.ACCURACY_GUIDE
        },
        onNavigateToGuide = {
          previousDestination = AppDestination.SETTINGS
          currentDestination = AppDestination.ACCURACY_GUIDE
        },
        onNavigateBack = {
          currentDestination = AppDestination.MAIN_TABS
          selectedTab = DynoTab.HOME
        }
      )
    }

    AppDestination.ACCURACY_GUIDE -> {
      AccuracyGuideScreen(
        onNavigateBack = {
          currentDestination = previousDestination ?: AppDestination.MAIN_TABS
        }
      )
    }

    AppDestination.SENSORS -> {
      SensorScreen(
        onNavigateBack = {
          currentDestination = AppDestination.MAIN_TABS
          selectedTab = DynoTab.HOME
        },
        onNavigateToResults = {
          currentDestination = AppDestination.MAIN_TABS
          selectedTab = DynoTab.RESULTS
        }
      )
    }
  }
}
