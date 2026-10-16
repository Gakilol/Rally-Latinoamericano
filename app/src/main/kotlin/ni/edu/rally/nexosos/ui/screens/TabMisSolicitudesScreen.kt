package ni.edu.rally.nexosos.ui.screens

import androidx.compose.foundation.background
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla RF-04 (Mis solicitudes: cola local Outbox y cronología verificable)
 * y RF-08 (Seguimiento y cierre: salida, llegada, atención, pendiente residual y cancelación con motivo).
 */
@Composable
fun TabMisSolicitudesScreen(
    state: NexoUiState,
    viewModel: NexoSosViewModel
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Resumen de la Cola Persistente Outbox (Anexo E, F y G)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFECEFF1)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Mensajes pendientes de envío",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Mensajes en cola: ${state.outboxItems.size}",
                    fontSize = 12.sp
                )
                state.outboxItems.take(4).forEach { out ->
                    Text(
                        text = "• ${out.tipoMensaje.etiqueta} para ${out.nivelDestino.etiqueta} · ${out.tamanoBytes} B · " + when (out.estadoOutbox) {
                            EstadoOutbox.PENDIENTE_SIN_RADIO -> "Esperando radio"
                            EstadoOutbox.ENCOLADO_PARA_RADIO -> "Listo para enviar"
                            EstadoOutbox.TRANSMITIDO_ESPERANDO_ACUSE_APP -> "Enviado, esperando respuesta"
                            EstadoOutbox.PENDIENTE_SALIDA_MANAGUA -> "Esperando enlace con Managua"
                            EstadoOutbox.ENTREGADO_CONFIRMADO -> "Entregado"
                            EstadoOutbox.RECHAZADO_TAMANO_O_FIRMA -> "Rechazado"
                        },
                        fontSize = 11.sp,
                        color = Color(0xFF263238)
                    )
                }
            }
        }

        if (state.solicitudes.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No hay solicitudes registradas aún.", fontWeight = FontWeight.Bold)
                    Text(
                        "Usa la pestaña 'Pedir Ayuda' o inicia una simulación en 'Inicio'.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        state.solicitudes.forEach { detalle ->
            val sol = detalle.solicitud
            val nec = detalle.necesidad
            var motivoSeguimiento by remember(sol.uuidEvento) {
                mutableStateOf("Brigada y suministros verificados en punto comunitario.")
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                        Text(
                            text = "Urgencia: ${sol.urgencia.etiqueta}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB71C1C)
                        )
                    }

                    BadgeEstadoAccesible(estado = sol.estadoGeneral)

                    // Cadena territorial de 4 niveles (Comarca -> Municipio -> Cabecera -> Managua)
                    BarraCadenaTerritorial(
                        nivelActual = sol.nivelActual,
                        estadoGeneral = sol.estadoGeneral
                    )

                    // Desglose de cantidades (Solicitado vs Aceptado vs Pendiente Residual)
                    Surface(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Necesidad: ${nec.tipo.etiqueta} (${sol.personasAfectadas} personas afectadas)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Pedido: ${nec.cantidadSolicitada} ${nec.unidad} · Aceptado: ${nec.cantidadAceptadaTotal} ${nec.unidad} · Falta: ${nec.cantidadPendiente} ${nec.unidad}",
                                fontSize = 12.sp,
                                color = if (nec.cantidadPendiente > 0) Color(0xFFE65100) else Color(0xFF1B5E20),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Ubicación (${sol.fuenteUbicacion.etiqueta}): ${sol.referenciaSitio}" +
                                    (if (sol.latitud != null && sol.longitud != null) " [${sol.latitud}, ${sol.longitud} ±${sol.precisionMetros ?: 0}m]" else ""),
                                fontSize = 11.sp
                            )
                            detalle.ultimoOutbox?.let { out ->
                                Text(
                                    text = "Último mensaje por radio: ${out.tamanoBytes} bytes",
                                    fontSize = 11.sp,
                                    color = Color(0xFF0D47A1)
                                )
                            }
                        }
                    }

                    // Relojes de vencimiento del simulacro (60s recepción / 180s decisión - Anexo C)
                    detalle.vencimientoActual?.let { venc ->
                        if (!venc.decisionTomada) {
                            val segRestantes60 = ((venc.venceAcuse60sEpochMs - state.relojActualEpochMs) / 1000L).coerceAtLeast(0L)
                            val segRestantes180 = ((venc.venceDecision180sEpochMs - state.relojActualEpochMs) / 1000L).coerceAtLeast(0L)
                            Surface(
                                color = Color(0xFFFFF8E1),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "Tiempos de respuesta en ${venc.nivel.etiqueta}:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = if (venc.acuseAplicacionRecibido) {
                                            "• Recibido por la autoridad · Tiempo para decidir: ${segRestantes180} s restantes"
                                        } else {
                                            "• Tiempo para confirmar recepción: ${segRestantes60} s restantes · Tiempo para decidir: ${segRestantes180} s restantes"
                                        },
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Asignaciones comprometidas por institución
                    if (detalle.asignaciones.isNotEmpty()) {
                        Text(
                            text = "Recursos comprometidos:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        detalle.asignaciones.forEach { asig ->
                            Text(
                                text = "✓ [${asig.nivel.etiqueta}] ${asig.cantidadComprometida} ${nec.unidad} de '${asig.recursoNombre}' · Responsable: ${asig.responsableNombre} (${asig.estadoOperativo.etiqueta})",
                                fontSize = 11.sp,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }

                    // Controles de prueba rápida Anexo J (ACK técnico sin autoridad y Duplicados/Tardíos)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.simularAckTecnicoRadioSinAutoridad(detalle) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Simular confirmación del radio", fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.probarReconciliacionTardiaYDuplicado(detalle) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Probar mensaje repetido y tardío", fontSize = 10.sp)
                        }
                    }

                    // RF-08 Seguimiento y Cierre (Salida, Atención en sitio, Atendido/Cierre, Cancelar con motivo)
                    HorizontalDivider()
                    Text(
                        text = "Seguimiento y cierre (se conserva el historial y el motivo):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = motivoSeguimiento,
                        onValueChange = { motivoSeguimiento = it },
                        label = { Text("Motivo de la actualización, cierre o cancelación") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.actualizarSeguimiento(detalle, EstadoSolicitud.EN_CAMINO, motivoSeguimiento)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("En camino", fontSize = 10.sp)
                        }
                        Button(
                            onClick = {
                                viewModel.actualizarSeguimiento(detalle, EstadoSolicitud.ATENDIDO, motivoSeguimiento)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Marcar atendido", fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.actualizarSeguimiento(detalle, EstadoSolicitud.CANCELADO, motivoSeguimiento)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar con motivo", fontSize = 10.sp)
                        }
                    }

                    // Cronología auditable paso a paso (RF-04: Mostrar quién confirmó cada paso)
                    HorizontalDivider()
                    Text(
                        text = "Historial: quién confirmó cada paso",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    detalle.historial.forEach { evt ->
                        val bgEvt = if (evt.esTardioReconciliado) Color(0xFFFFF3E0) else Color(0xFFFAFAFA)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgEvt, RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        ) {
                            Text(
                                text = "[${timeFormatter.format(Date(evt.timestampEpochMs))}] ${ni.edu.rally.nexosos.ui.components.obtenerEstiloEstado(evt.estadoNuevo).prefijoTexto} — Por: ${evt.autorNombre}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = evt.motivo,
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}
