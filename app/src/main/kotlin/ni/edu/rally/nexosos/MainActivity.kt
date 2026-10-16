package ni.edu.rally.nexosos

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import ni.edu.rally.nexosos.service.MeshForegroundService
import ni.edu.rally.nexosos.ui.NexoSosViewModel
import ni.edu.rally.nexosos.ui.components.BarraEstadoSuperiorRf01
import ni.edu.rally.nexosos.ui.screens.TabBandejaAutoridadScreen
import ni.edu.rally.nexosos.ui.screens.TabCrearSosScreen
import ni.edu.rally.nexosos.ui.screens.TabInicioYDemoScreen
import ni.edu.rally.nexosos.ui.screens.TabMapaYConfigScreen
import ni.edu.rally.nexosos.ui.screens.TabMisSolicitudesScreen
import ni.edu.rally.nexosos.ui.theme.NexoCelesteColors
import ni.edu.rally.nexosos.ui.theme.NexoSosCelesteTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        iniciarServicioPrimerPlanoMesh()

        setContent {
            NexoSosCelesteTheme {
                val vm: NexoSosViewModel = viewModel()
                val state by vm.uiState.collectAsState()
                var pestanaSeleccionada by remember { mutableIntStateOf(0) }

                Scaffold(
                    containerColor = NexoCelesteColors.BlancoHieloFondo,
                    topBar = {
                        BarraEstadoSuperiorRf01(
                            state = state,
                            onSelectRol = { vm.seleccionarRol(it) }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color.White,
                            contentColor = NexoCelesteColors.CelestePrimario
                        ) {
                            NavigationBarItem(
                                selected = pestanaSeleccionada == 0,
                                onClick = { pestanaSeleccionada = 0 },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                                label = { Text("Inicio") }
                            )
                            NavigationBarItem(
                                selected = pestanaSeleccionada == 1,
                                onClick = { pestanaSeleccionada = 1 },
                                icon = { Icon(Icons.Default.Sos, contentDescription = "Pedir Ayuda") },
                                label = { Text("Pedir Ayuda") }
                            )
                            NavigationBarItem(
                                selected = pestanaSeleccionada == 2,
                                onClick = { pestanaSeleccionada = 2 },
                                icon = { Icon(Icons.Default.TrackChanges, contentDescription = "Seguimiento") },
                                label = { Text("Seguimiento") }
                            )
                            NavigationBarItem(
                                selected = pestanaSeleccionada == 3,
                                onClick = { pestanaSeleccionada = 3 },
                                icon = { Icon(Icons.Default.AssignmentTurnedIn, contentDescription = "Atención") },
                                label = { Text("Atención") }
                            )
                            NavigationBarItem(
                                selected = pestanaSeleccionada == 4,
                                onClick = { pestanaSeleccionada = 4 },
                                icon = { Icon(Icons.Default.Navigation, contentDescription = "Ruta Waze") },
                                label = { Text("Ruta Waze") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (pestanaSeleccionada) {
                            0 -> TabInicioYDemoScreen(state = state, viewModel = vm)
                            1 -> TabCrearSosScreen(
                                state = state,
                                viewModel = vm,
                                onSosCreadoIrAMisSolicitudes = { pestanaSeleccionada = 2 }
                            )
                            2 -> TabMisSolicitudesScreen(state = state, viewModel = vm)
                            3 -> TabBandejaAutoridadScreen(state = state, viewModel = vm)
                            else -> TabMapaYConfigScreen(state = state, viewModel = vm)
                        }
                    }
                }
            }
        }
    }

    private fun iniciarServicioPrimerPlanoMesh() {
        val intent = Intent(this, MeshForegroundService::class.java)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }
}
