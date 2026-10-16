package ni.edu.rally.nexosos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ni.edu.rally.nexosos.domain.model.*
import ni.edu.rally.nexosos.ui.NexoSosViewModel
import ni.edu.rally.nexosos.ui.NexoUiState
import ni.edu.rally.nexosos.ui.components.BadgeEstadoAccesible
import ni.edu.rally.nexosos.ui.components.BarraCadenaTerritorial

/**
 * Pantalla RF-05 (Bandeja de autoridad) y Anexo C (Reglas de escalamiento y capacidad).
 *
 * Implementa la regla clave del Anexo B:
 * "El SOS puede pasar físicamente por un radio municipal antes de que lo revise la comarca.
 * Eso no significa que ya se escaló. La aplicación identifica el nivel responsable actual
 * y solo activa su bandeja de decisión."
 */
@Composable
fun TabBandejaAutoridadScreen(
    state: NexoUiState,
    viewModel: NexoSosViewModel
) {
    var nivelSeleccionado by remember(state.rolActivo) {
        mutableStateOf(state.nivelBandejaActiva)
    }

    val solicitudesEnMiBandeja = state.solicitudes.filter { it.solicitud.nivelActual == nivelSeleccionado }
    val copiasTecnicasOtrosNiveles = state.solicitudes.filter { it.solicitud.nivelActual != nivelSeleccionado }
    val reservasDelNivel = state.reservas.filter { it.nivel == nivelSeleccionado }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Selector de nivel de autoridad para inspección y prueba
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Solicitudes por atender",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = Color(0xFF0D47A1)
                )
                Text(
                    text = "Cada solicitud la decide solo el nivel responsable en ese momento, aunque su mensaje haya pasado antes por el radio de otro municipio.",
                    fontSize = 11.sp,
                    color = Color(0xFF1565C0)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NivelTerritorial.entries.forEach { nivel ->
                        FilterChip(
                            selected = nivelSeleccionado == nivel,
                            onClick = { nivelSeleccionado = nivel },
                            label = { Text("${nivel.orden}. ${nivel.codigoCorto}", fontSize = 11.sp) }
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "📡 Recibir SOS entrante desde torre repetidora:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF0D47A1)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.recibirSosEntranteDesdeTorre(1)
                            nivelSeleccionado = NivelTerritorial.COMARCA
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Torre Masachapa", fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.recibirSosEntranteDesdeTorre(2)
                            nivelSeleccionado = NivelTerritorial.MUNICIPIO
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Torre San Rafael", fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.recibirSosEntranteDesdeTorre(3)
                            nivelSeleccionado = NivelTerritorial.CABECERA_DEPARTAMENTAL
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Torre El Crucero", fontSize = 10.sp)
                    }
                }
            }
        }

        // Inventario propio disponible en este nivel (RF-07)
        Card(shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Inventario disponible en ${nivelSeleccionado.etiqueta}:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                reservasDelNivel.forEach { res ->
                    Text(
                        text = "• ${res.nombreRecurso}: disponible ${res.cantidadDisponible} de ${res.cantidadTotalInventario} ${res.unidad} (Responsable: ${res.responsableInventario})",
                        fontSize = 12.sp,
                        color = if (res.cantidadDisponible > 0) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                    )
                }
            }
        }

        // Lista de Solicitudes Activas en la Bandeja de este Nivel
        Text(
            text = "Solicitudes por decidir en ${nivelSeleccionado.etiqueta} (${solicitudesEnMiBandeja.size}):",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        if (solicitudesEnMiBandeja.isEmpty()) {
            Surface(
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No hay solicitudes por atender en ${nivelSeleccionado.etiqueta}.",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }
        }

        solicitudesEnMiBandeja.forEach { detalle ->
            val sol = detalle.solicitud
            val nec = detalle.necesidad

            val reservaSugerida = reservasDelNivel.firstOrNull { it.tipoNecesidadCubierta == nec.tipo }
                ?: reservasDelNivel.firstOrNull()
            var cantidadAceptarTexto by remember(sol.uuidEvento, sol.revision, reservaSugerida?.cantidadDisponible) {
                val sugerido = reservaSugerida?.cantidadDisponible
                    ?.coerceAtMost(nec.cantidadPendiente)
                    ?.coerceAtLeast(1) ?: nec.cantidadPendiente
                mutableStateOf(sugerido.toString())
            }
            var motivoIncapacidad by remember(sol.uuidEvento) {
                mutableStateOf(MotivoCapacidad.FALTA_SUMINISTROS)
            }
            var observacionEscalado by remember(sol.uuidEvento) {
                mutableStateOf("Capacidad local insuficiente para cubrir las ${nec.cantidadPendiente} ${nec.unidad} pendientes.")
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Solicitud ${sol.uuidEvento.take(8)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        BadgeEstadoAccesible(estado = sol.estadoGeneral)
                    }

                    BarraCadenaTerritorial(
                        nivelActual = sol.nivelActual,
                        estadoGeneral = sol.estadoGeneral
                    )

                    Text(
                        text = "Necesidad: ${nec.tipo.etiqueta} · Falta por atender: ${nec.cantidadPendiente} de ${nec.cantidadSolicitada} ${nec.unidad}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFFB71C1C)
                    )
                    Text(
                        text = "Ubicación: ${sol.fuenteUbicacion.etiqueta} (${sol.referenciaSitio}) · Personas afectadas: ${sol.personasAfectadas}",
                        fontSize = 12.sp
                    )
                    if (sol.notaMotivo.isNotBlank()) {
                        Text(
                            text = "Contexto previo: ${sol.notaMotivo}",
                            fontSize = 11.sp,
                            color = Color(0xFF0D47A1)
                        )
                    }

                    HorizontalDivider()

                    // Acción 1: Emitir RECIBIDO_AUTORIDAD
                    Button(
                        onClick = { viewModel.confirmarRecibidoAutoridad(detalle) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00838F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MarkEmailRead, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("1. Confirmar que la solicitud fue recibida")
                    }

                    // Acción 2: Aceptación Completa o Parcial (ej. aceptar 4 de 10 y escalar 6)
                    if (reservaSugerida != null) {
                        Text(
                            text = "2. Aceptar todo o una parte con el inventario local (${reservaSugerida.nombreRecurso} · disponible: ${reservaSugerida.cantidadDisponible}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = cantidadAceptarTexto,
                                onValueChange = { cantidadAceptarTexto = it.filter { c -> c.isDigit() } },
                                label = { Text("Cantidad a comprometer (${nec.unidad})") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    val cant = cantidadAceptarTexto.toIntOrNull() ?: 1
                                    viewModel.aceptarRecursosEnNivel(detalle, reservaSugerida, cant)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                modifier = Modifier.weight(1f)
                            ) {
                                val cant = cantidadAceptarTexto.toIntOrNull() ?: 0
                                val textoBtn = if (cant in 1 until nec.cantidadPendiente) {
                                    "Aceptar $cant y derivar ${nec.cantidadPendiente - cant}"
                                } else {
                                    "Aceptar todo ($cant)"
                                }
                                Text(textoBtn, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider()

                    // Acción 3: Declarar Capacidad Insuficiente y Escalar inmediatamente (Anexo C)
                    Text(
                        text = "3. No hay capacidad: derivar lo pendiente (${nec.cantidadPendiente} ${nec.unidad}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    val motivosOperador = listOf(
                        MotivoCapacidad.FALTA_BRIGADA,
                        MotivoCapacidad.FALTA_TRANSPORTE,
                        MotivoCapacidad.FALTA_SUMINISTROS,
                        MotivoCapacidad.FALTA_CENTRO_RECEPTOR,
                        MotivoCapacidad.NOTIFICACION_VITAL_PARALELA
                    )
                    motivosOperador.forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = motivoIncapacidad == m,
                                onClick = { motivoIncapacidad = m }
                            )
                            Text(m.etiqueta, fontSize = 12.sp)
                        }
                    }
                    OutlinedTextField(
                        value = observacionEscalado,
                        onValueChange = { observacionEscalado = it },
                        label = { Text("Nota para el siguiente nivel") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            viewModel.declararCapacidadInsuficiente(detalle, motivoIncapacidad, observacionEscalado)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val sig = sol.nivelActual.siguienteNivel()
                        val txt = if (sig != null) {
                            "No puedo atender: derivar a ${sig.etiqueta}"
                        } else {
                            "Managua sin capacidad: marcar como pendiente de apoyo central"
                        }
                        Text(txt, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Acción 4: Probar vencimientos automáticos de 60s y 180s (Anexo C y J)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.forzarVencimientoPlazoSimulacro(detalle, vencer60s = true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Simular: sin confirmación en 60 s", fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.forzarVencimientoPlazoSimulacro(detalle, vencer60s = false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Simular: sin decisión en 180 s", fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Sección de Copias Técnicas en Tránsito (Anexo B: No activan bandeja de decisión si están en otro nivel)
        if (copiasTecnicasOtrosNiveles.isNotEmpty()) {
            HorizontalDivider()
            Text(
                text = "Solicitudes en otros niveles (solo consulta desde ${nivelSeleccionado.etiqueta}):",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.DarkGray
            )
            copiasTecnicasOtrosNiveles.forEach { det ->
                Surface(
                    color = Color(0xFFEEEEEE),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Solicitud ${det.solicitud.uuidEvento.take(8)} · Responsable actual: ${det.solicitud.nivelActual.etiqueta}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Estado: ${ni.edu.rally.nexosos.ui.components.obtenerEstiloEstado(det.solicitud.estadoGeneral).prefijoTexto} · Falta: ${det.necesidad.cantidadPendiente}/${det.necesidad.cantidadSolicitada} ${det.necesidad.unidad}",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
