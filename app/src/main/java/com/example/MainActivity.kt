package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.RunResultRepository
import com.example.data.VehicleRepository
import com.example.data.db.DynoMobileDatabase
import com.example.ui.screens.GarageScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SimulatorScreen
import com.example.ui.screens.TestPreparationScreen
import com.example.ui.screens.VehicleWizardScreen
import com.example.ui.theme.DynoBg
import com.example.ui.theme.DynoTheme

const val ROUTE_HOME = "home"
const val ROUTE_TEST_PREPARATION = "test_preparation"
const val SIMULATION_ROUTE = "simulation"
const val ROUTE_GARAGE = "garage"
const val ROUTE_VEHICLE_WIZARD = "vehicle_wizard"
const val ROUTE_RESULTS = "results"
const val ROUTE_SETTINGS = "settings"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = DynoMobileDatabase.getDatabase(this)
        val vehicleRepository = VehicleRepository(database.vehicleDao())
        val runResultRepository = RunResultRepository(database.runResultDao(), database.pendingSessionDao())

        setContent {
            DynoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DynoBg
                ) {
                    LocationPermissionHandler()
                    AppNavigation(
                        vehicleRepository = vehicleRepository,
                        runResultRepository = runResultRepository
                    )
                }
            }
        }
    }
}

@Composable
fun LocationPermissionHandler() {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.HIGH_SAMPLING_RATE_SENSORS)
        }
        launcher.launch(permissions.toTypedArray())
    }
}

@Composable
fun AppNavigation(
    vehicleRepository: VehicleRepository,
    runResultRepository: RunResultRepository
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME
    ) {
        composable(ROUTE_HOME) {
            HomeScreen(
                vehicleRepository = vehicleRepository,
                runResultRepository = runResultRepository,
                onNavigateToTest = {
                    navController.navigate(ROUTE_TEST_PREPARATION) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSimulation = {
                    navController.navigate(SIMULATION_ROUTE) {
                        launchSingleTop = true
                    }
                },
                onNavigateToGarage = {
                    navController.navigate(ROUTE_GARAGE) {
                        launchSingleTop = true
                    }
                },
                onNavigateToHistory = {
                    navController.navigate(ROUTE_GARAGE) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(ROUTE_SETTINGS) {
                        launchSingleTop = true
                    }
                },
                onNavigateToResults = { resultId ->
                    navController.navigate("$ROUTE_RESULTS/$resultId") {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(ROUTE_TEST_PREPARATION) {
            TestPreparationScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToResults = { resultId ->
                    navController.navigate("$ROUTE_RESULTS/$resultId") {
                        popUpTo(ROUTE_HOME)
                    }
                }
            )
        }

        composable(SIMULATION_ROUTE) {
            SimulatorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_GARAGE) {
            GarageScreen(
                vehicleRepository = vehicleRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddVehicle = {
                    navController.navigate(ROUTE_VEHICLE_WIZARD)
                },
                onNavigateToEditVehicle = { vehicleId ->
                    navController.navigate("$ROUTE_VEHICLE_WIZARD?vehicleId=$vehicleId")
                }
            )
        }

        composable(
            route = "$ROUTE_VEHICLE_WIZARD?vehicleId={vehicleId}",
            arguments = listOf(
                navArgument("vehicleId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId")
            VehicleWizardScreen(
                vehicleRepository = vehicleRepository,
                vehicleId = vehicleId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "$ROUTE_RESULTS/{resultId}",
            arguments = listOf(
                navArgument("resultId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val resultId = backStackEntry.arguments?.getString("resultId")
            ResultsScreen(
                runResultRepository = runResultRepository,
                resultId = resultId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
