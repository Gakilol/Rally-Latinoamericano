package ni.edu.rally.nexosos.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ni.edu.rally.nexosos.domain.model.CanalTransporte
import ni.edu.rally.nexosos.ui.NexoSosViewModel
import ni.edu.rally.nexosos.ui.NexoUiState
import ni.edu.rally.nexosos.ui.theme.NexoCelesteColors

/**
 * Pantalla RF-01 (Inicio y conexión) + Demostración del Rally en San Rafael del Sur (Anexo I)
 * + Pruebas de Aceptación del Piloto (Anexo J) con identidad visual Celeste y Blanco.
 */
@Composable
fun TabInicioYDemoScreen(
    state: NexoUiState,
    viewModel: NexoSosViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tarjeta 1: Recorrido Guiado de Demostración del Rally en San Rafael del Sur (Anexo I)
        Card(
            colors = CardDefaults.cardColors(containerColor = NexoCelesteColors.CelesteClaroFondo),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, NexoCelesteColors.CelestePrimario, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        tint = NexoCelesteColors.CelestePrimario
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simulación de atención en vivo · San Rafael del Sur",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = NexoCelesteColors.CelesteOscuroTexto
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Así funciona, paso a paso: 1) Se pide ayuda por 10 kits en Comarca Masachapa → 2) Masachapa avisa que no puede atender → 3) San Rafael del Sur acepta 4 de 10 y deriva 6 → 4) La cabecera departamental (El Crucero) deriva 6 a Managua → 5) Managua acepta los 6, los envía por la mejor ruta y cierra.",
                    fontSize = 12.sp,
                    color = NexoCelesteColors.CelesteOscuroTexto
                )
                Spacer(modifier = Modifier.height(10.dp))

                val etiquetaPaso = when (state.pasoDemoRallyActual) {
                    0 -> "Paso 1/5: Pedir ayuda (10 kits en Comarca Masachapa)"
                    1 -> "Paso 2/5: Masachapa no puede atender -> San Rafael del Sur"
                    2 -> "Paso 3/5: San Rafael acepta 4 de 10 y deriva 6 -> Cabecera"
                    3 -> "Paso 4/5: La cabecera deriva los 6 restantes -> Managua"
                    else -> "Paso 5/5: Managua acepta los 6 y los envía por la mejor ruta"
                }

                Button(
                    onClick = { viewModel.ejecutarSiguientePasoDemoRally() },
                    colors = ButtonDefaults.buttonColors(containerColor = NexoCelesteColors.CelestePrimario),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = etiquetaPaso, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Tarjeta 1B: Recepción en Vivo de Mensajes SOS desde las Torres (T1 Masachapa, T2 San Rafael, T3 El Crucero)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, NexoCelesteColors.CelestePrimario, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = null,
                        tint = NexoCelesteColors.CelestePrimario
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recepción de alertas SOS desde las Torres",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = NexoCelesteColors.CelesteOscuroTexto
                    )
                }
                Text(
                    text = "Recibe en tiempo real las solicitudes de ayuda que entran por radio desde las 3 torres del corredor de San Rafael del Sur:",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { viewModel.recibirSosEntranteDesdeTorre(1) },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoCelesteColors.CelestePrimario),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📡 Torre 1\nMasachapa", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.recibirSosEntranteDesdeTorre(2) },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoCelesteColors.CelestePrimario),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📡 Torre 2\nSan Rafael", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.recibirSosEntranteDesdeTorre(3) },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoCelesteColors.CelestePrimario),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📡 Torre 3\nEl Crucero", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                state.estadoRadio.ultimosMensajesTorre.take(3).forEach { msg ->
                    Surface(
                        color = NexoCelesteColors.CelesteClaroFondo,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "📥 ${msg.torreNombre} (${msg.nodoLoRaId}) · Señal ${msg.rssiDbm} dBm / SNR ${msg.snrDb} dB",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = NexoCelesteColors.CelesteOscuroTexto
                            )
                            Text(
                                text = msg.resumenMensaje,
                                fontSize = 11.sp,
                                color = Color(0xFF01579B)
                            )
                        }
                    }
                }
            }
        }

        // Tarjeta 2: RF-01 Inicio y conexión
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NexoCelesteColors.CelesteBorde, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Conexión y radio",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NexoCelesteColors.CelesteOscuroTexto
                )
                val dev = state.estadoRadio.dispositivoActual
                Text(
                    text = "Radio: ${dev.alias} (${dev.nodoMeshId}) · Bluetooth: ${dev.macBle}",
                    fontSize = 12.sp
                )
                Text(
                    text = "Versión: ${dev.firmwareVersion} · Batería: ${dev.bateriaPorcentaje}%",
                    fontSize = 12.sp
                )
                Text(
                    text = "Canal de radio: ${dev.canalConfigurado}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexoCelesteColors.CelestePrimario
                )
                Text(
                    text = "Cobertura: Masachapa → San Rafael del Sur → El Crucero",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val esSimulado = state.estadoRadio.modoTransporte == CanalTransporte.MALLA_SIMULADA_RALLY
                    FilterChip(
                        selected = esSimulado,
                        onClick = { viewModel.cambiarModoTransporte(CanalTransporte.MALLA_SIMULADA_RALLY) },
                        label = { Text("Modo de práctica (simulado)") }
                    )
                    FilterChip(
                        selected = !esSimulado,
                        onClick = { viewModel.cambiarModoTransporte(CanalTransporte.BLE_MESHTASTIC_REAL) },
                        label = { Text("Radio real por Bluetooth") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Radio conectado", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            "Si se apaga, la solicitud queda como 'Pendiente de envío' y no se muestra como enviada. Al reconectar, se envía sola.",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                    Switch(
                        checked = state.estadoRadio.radioVinculado,
                        onCheckedChange = { viewModel.alternarRadioVinculado(it) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Internet disponible", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            "Sin Wi-Fi ni datos móviles, tu solicitud sigue viajando por el radio.",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                    Switch(
                        checked = state.estadoRadio.internetDisponible,
                        onCheckedChange = { viewModel.alternarInternetDisponible(it) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Managua sin enlace", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            "Masachapa y San Rafael siguen trabajando; lo que debe llegar a Managua queda 'Pendiente de entrega' hasta recuperar el enlace.",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                    Switch(
                        checked = !state.estadoPasarela.salidaHaciaManaguaDisponible,
                        onCheckedChange = { viewModel.alternarMallaAisladaDeManagua(it) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Simular Sede Central fuera de servicio", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            "Activa una consola alterna de operación fuera de Managua, con autorización.",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                    Switch(
                        checked = !state.estadoPasarela.sedeCentralOperativa,
                        onCheckedChange = { viewModel.alternarSedeCentralDestruida(it) }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.iniciarEscaneoBle() }) {
                        Icon(Icons.Default.BluetoothSearching, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buscar radios cercanos")
                    }
                    if (state.estadoRadio.escaneandoBle) {
                        OutlinedButton(onClick = { viewModel.detenerEscaneoBle() }) {
                            Text("Detener escaneo")
                        }
                    }
                }
                if (state.estadoRadio.nodosBleDetectados.isNotEmpty()) {
                    state.estadoRadio.nodosBleDetectados.forEach { nodo ->
                        Text(text = "• $nodo", fontSize = 11.sp, color = NexoCelesteColors.CelestePrimario)
                    }
                }
            }
        }

        // Tarjeta 3: Pruebas de Aceptación del Piloto (Anexo J)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NexoCelesteColors.CelesteBorde, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pruebas del sistema",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NexoCelesteColors.CelesteOscuroTexto
                )
                Text(
                    text = "Tráfico actual: enviados=${state.estadoRadio.paquetesTransmitidos} · recibidos=${state.estadoRadio.paquetesRecibidos} · total=${state.estadoRadio.bytesTotalesLoRa} B · reintentos=${state.estadoRadio.reintentosAcumulados} · canal ocupado=${state.estadoRadio.ocupacionCanalEstimadaPct}%",
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.ejecutarPruebaCarga20Sos() },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoCelesteColors.CelestePrimario),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Probar 20 solicitudes en 10 min", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { viewModel.probarSeguridadHmacYRechazoModificado() },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoCelesteColors.CelesteBrillante),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Probar mensaje alterado", fontSize = 11.sp)
                    }
                }

                state.resultadoUltimaPruebaCarga?.let { res ->
                    Surface(
                        color = NexoCelesteColors.CelesteClaroFondo,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Resultado: 20 solicitudes en 10 min (meta: al menos 19 recibidas en 2 minutos):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NexoCelesteColors.CelesteOscuroTexto
                            )
                            Text(
                                text = "• Recibidas en 2 min: ${res.sosEntregadosEn120s}/${res.sosEnviados} (perdidas: ${res.perdidasVisibles}, sin duplicados)",
                                fontSize = 11.sp
                            )
                            Text(
                                text = "• Tamaño medio: ${res.bytesPromedioPorSos} B por solicitud · Total: ${res.bytesTotales} B",
                                fontSize = 11.sp
                            )
                            Text(
                                text = "• Tiempo medio de entrega por radio: ${res.tiempoMedioEntregaSeg} s · Latencia satelital: ${state.estadoPasarela.latenciaSatelitalMedidaSeg} s",
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
