package ni.edu.rally.nexosos.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ni.edu.rally.nexosos.domain.engine.WazeRouteEngine
import ni.edu.rally.nexosos.domain.model.*
import ni.edu.rally.nexosos.ui.NexoSosViewModel
import ni.edu.rally.nexosos.ui.NexoUiState
import ni.edu.rally.nexosos.ui.theme.NexoCelesteColors

/**
 * Pantalla RF-06 (Mapa estilo Waze de San Rafael del Sur con cálculo de ruta óptima),
 * RF-07 (Inventario y reservas) y RF-09 (Configuración protegida) con paleta Celeste y Blanco.
 */
@Composable
fun TabMapaYConfigScreen(
    state: NexoUiState,
    viewModel: NexoSosViewModel
) {
    var subTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = subTab,
            containerColor = NexoCelesteColors.BlancoPuro,
            contentColor = NexoCelesteColors.CelestePrimario
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text("Mapa y ruta", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text("Inventario", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = subTab == 2,
                onClick = { subTab = 2 },
                text = { Text("Ajustes", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        when (subTab) {
            0 -> SubVistaMapaWazeSanRafaelRf06(state, viewModel)
            1 -> SubVistaInventarioRf07(state, viewModel)
            else -> SubVistaConfiguracionRf09(state, viewModel)
        }
    }
}

@Composable
private fun SubVistaMapaWazeSanRafaelRf06(
    state: NexoUiState,
    viewModel: NexoSosViewModel
) {
    val ruta = state.rutaOptimaWaze
    val nodos = WazeRouteEngine.nodosSanRafaelDelSur
    val mapaNodos = remember(nodos) { nodos.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Banner Superior de Navegación Estilo Waze (Celeste y Blanco)
        Card(
            colors = CardDefaults.cardColors(containerColor = NexoCelesteColors.CelestePrimario),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Ruta de la brigada · San Rafael del Sur",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${ruta.origen.nombre.substringBefore(" (")} → ${ruta.destino.nombre.substringBefore(" (")}",
                                color = Color(0xFFE1F5FE),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (ruta.hayRutaDisponible) "${ruta.tiempoEstimadoMin} min" else "Sin ruta",
                                color = if (ruta.hayRutaDisponible) NexoCelesteColors.CelesteOscuroTexto else Color(0xFFC62828),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${ruta.distanciaTotalKm} km",
                                color = NexoCelesteColors.CelestePrimario,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Surface(
                    color = Color.White.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = ruta.resumenRuta,
                            color = NexoCelesteColors.CelesteOscuroTexto,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        if (ruta.tramosBloqueadosEvitados.isNotEmpty()) {
                            Text(
                                text = "⚠️ Desvío activo evitando: " +
                                    ruta.tramosBloqueadosEvitados.joinToString(", ") { "${it.nombreTramo} (${it.tipoAlertaWaze})" },
                                color = Color(0xFFC62828),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Selector rápido de punto de despacho (Origen) y lugar del desastre (Destino)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NexoCelesteColors.CelesteBorde, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Brigada: punto de salida → lugar de la emergencia:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NexoCelesteColors.CelesteOscuroTexto
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.cambiarNodosRutaWaze("NODO_SAN_RAFAEL", "NODO_MASACHAPA") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Alcaldía → Masachapa", fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.cambiarNodosRutaWaze("NODO_MANAGUA", "NODO_MASACHAPA") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Managua → Masachapa", fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.cambiarNodosRutaWaze("NODO_EL_CRUCERO", "NODO_POCHOMIL") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("El Crucero → Pochomil", fontSize = 10.sp)
                    }
                }

                Button(
                    onClick = { viewModel.avanzarSimulacionVehiculoWaze() },
                    colors = ButtonDefaults.buttonColors(containerColor = NexoCelesteColors.CelesteBrillante),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simular avance de la brigada (${(state.progresoBrigadaWazePct * 100).toInt()}%)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Lienzo Interactivo del Mapa Estilo Waze (San Rafael del Sur - El Crucero - Managua)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, NexoCelesteColors.CelestePrimario, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Mapa en vivo · San Rafael del Sur",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = NexoCelesteColors.CelesteOscuroTexto
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                        .background(NexoCelesteColors.CelesteClaroFondo, RoundedCornerShape(12.dp))
                        .border(1.dp, NexoCelesteColors.CelesteBorde, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    val tramosEnRutaIds = ruta.tramosEnRuta.map { it.id }.toSet()

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Franja costera del Océano Pacífico al suroeste (Masachapa / Pochomil / Montelimar)
                        drawRect(
                            color = Color(0xFFB3E5FC).copy(alpha = 0.55f),
                            topLeft = Offset(0f, h * 0.78f),
                            size = androidx.compose.ui.geometry.Size(w * 0.58f, h * 0.22f)
                        )

                        // 1. Dibujar todos los tramos de la red vial de San Rafael del Sur
                        state.tramosWaze.forEach { tramo ->
                            val nOrig = mapaNodos[tramo.origenId] ?: return@forEach
                            val nDest = mapaNodos[tramo.destinoId] ?: return@forEach
                            val start = Offset(w * nOrig.xRelativoMapa, h * nOrig.yRelativoMapa)
                            val end = Offset(w * nDest.xRelativoMapa, h * nDest.yRelativoMapa)
                            val esParteDeRutaOptima = tramo.id in tramosEnRutaIds

                            // Sombra exterior si pertenece a la ruta óptima Waze
                            if (esParteDeRutaOptima) {
                                drawLine(
                                    color = Color(0xFF01579B).copy(alpha = 0.35f),
                                    start = start,
                                    end = end,
                                    strokeWidth = 18f,
                                    cap = StrokeCap.Round
                                )
                            }

                            val colorLinea = when {
                                tramo.estaBloqueado -> Color(0xFFD32F2F)
                                esParteDeRutaOptima -> Color(0xFF00B0FF) // Celeste neón estilo Waze para ruta activa
                                tramo.estadoRuta == EstadoRuta.CANDIDATA_NO_VERIFICADA -> Color(0xFFF57C00)
                                else -> Color(0xFF90A4AE)
                            }

                            val grosor = if (esParteDeRutaOptima) 11f else 6f
                            val efecto = when {
                                tramo.estaBloqueado -> PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                tramo.estadoRuta == EstadoRuta.CANDIDATA_NO_VERIFICADA && !esParteDeRutaOptima ->
                                    PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
                                else -> null
                            }

                            drawLine(
                                color = colorLinea,
                                start = start,
                                end = end,
                                strokeWidth = grosor,
                                cap = StrokeCap.Round,
                                pathEffect = efecto
                            )

                            // Marcador de bloqueo estilo Waze en el punto medio del tramo cerrado
                            if (tramo.estaBloqueado) {
                                val mid = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
                                drawCircle(color = Color.White, radius = 13f, center = mid)
                                drawCircle(color = Color(0xFFD32F2F), radius = 10f, center = mid)
                            }
                        }

                        // 2. Dibujar los nodos geográficos y las 3 Torres (T1, T2, T3)
                        nodos.forEach { nodo ->
                            val center = Offset(w * nodo.xRelativoMapa, h * nodo.yRelativoMapa)
                            val esOrigen = nodo.id == state.nodoOrigenWazeId
                            val esDestino = nodo.id == state.nodoDestinoWazeId

                            // Halo de cobertura de radio si tiene torre T1, T2 o T3
                            if (nodo.esTorreLoRa != null) {
                                drawCircle(
                                    color = Color(0xFF0288D1).copy(alpha = 0.14f),
                                    radius = 38f,
                                    center = center
                                )
                            }

                            drawCircle(color = Color.White, radius = 14f, center = center)
                            val colorNodo = when {
                                esDestino -> Color(0xFFD50000) // Incidente SOS destino
                                esOrigen -> Color(0xFF00C853)  // Punto de salida brigada
                                nodo.esTorreLoRa != null -> NexoCelesteColors.CelestePrimario
                                else -> Color(0xFF455A64)
                            }
                            drawCircle(color = colorNodo, radius = 10f, center = center)
                        }

                        // 3. Dibujar la posición en vivo del vehículo de la brigada sobre la ruta óptima Waze
                        if (ruta.nodosEnRuta.size >= 2) {
                            val totalSegmentos = ruta.nodosEnRuta.size - 1
                            val posEscalada = (state.progresoBrigadaWazePct.coerceIn(0f, 1f)) * totalSegmentos
                            val idxSeg = posEscalada.toInt().coerceAtMost(totalSegmentos - 1)
                            val frac = posEscalada - idxSeg
                            val a = ruta.nodosEnRuta[idxSeg]
                            val b = ruta.nodosEnRuta[idxSeg + 1]
                            val xVeh = w * (a.xRelativoMapa + (b.xRelativoMapa - a.xRelativoMapa) * frac)
                            val yVeh = h * (a.yRelativoMapa + (b.yRelativoMapa - a.yRelativoMapa) * frac)

                            drawCircle(color = Color.White, radius = 16f, center = Offset(xVeh, yVeh))
                            drawCircle(color = Color(0xFF0091EA), radius = 12f, center = Offset(xVeh, yVeh))
                        }
                    }

                    // Etiquetas superpuestas clave en el mapa
                    Column(
                        modifier = Modifier.align(Alignment.TopStart),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "━ Línea celeste: ruta recomendada",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NexoCelesteColors.CelesteOscuroTexto
                        )
                        Text(
                            text = "● Torres de radio: Masachapa (costa) · San Rafael del Sur · El Crucero",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexoCelesteColors.CelestePrimario
                        )
                        Text(
                            text = "⛔ Rojo punteado: tramo bloqueado (la ruta se recalcula sola)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828)
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Text(
                            text = "Océano Pacífico: Masachapa / Pochomil / Montelimar",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexoCelesteColors.CelesteOscuroTexto,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Instrucciones paso a paso Giro a Giro estilo Waze
                Text(
                    text = "Indicaciones paso a paso:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NexoCelesteColors.CelesteOscuroTexto
                )
                ruta.instruccionesGiroAGiro.forEach { paso ->
                    Surface(
                        color = NexoCelesteColors.CelesteClaroFondo,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = paso,
                            fontSize = 11.sp,
                            color = NexoCelesteColors.CelesteOscuroTexto,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        // Panel interactivo de Alertas Viales Waze (Tocar para bloquear o despejar tramos y ver recálculo en vivo)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NexoCelesteColors.CelesteBorde, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Alertas viales en tiempo real:",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = NexoCelesteColors.CelesteOscuroTexto
                )
                Text(
                    text = "Toca una carretera para marcar un derrumbe o una inundación, o para despejarla, y ver cómo se recalcula la mejor ruta:",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )

                state.tramosWaze.forEach { tramo ->
                    val bloqueado = tramo.estaBloqueado
                    Surface(
                        color = if (bloqueado) Color(0xFFFFEBEE) else NexoCelesteColors.CelesteClaroFondo,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "[${tramo.codigoCarretera}] ${tramo.nombreTramo}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (bloqueado) Color(0xFFB71C1C) else NexoCelesteColors.CelesteOscuroTexto
                                )
                                Text(
                                    text = "${tramo.distanciaKm} km · ~${tramo.tiempoBaseMin} min · ${tramo.tipoAlertaWaze}",
                                    fontSize = 11.sp,
                                    color = if (bloqueado) Color(0xFFC62828) else Color(0xFF0277BD)
                                )
                            }
                            Button(
                                onClick = { viewModel.alternarEstadoTramoWaze(tramo.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (bloqueado) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                )
                            ) {
                                Text(
                                    text = if (bloqueado) "Despejar vía" else "Reportar Bloqueo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubVistaInventarioRf07(
    state: NexoUiState,
    viewModel: NexoSosViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NexoCelesteColors.CelesteClaroFondo),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Recursos y asignaciones",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NexoCelesteColors.CelesteOscuroTexto
                )
                Text(
                    text = "Solo quien administra un recurso puede confirmarlo, y no se puede reservar dos veces. Al recuperar la conexión, las asignaciones en conflicto se muestran para su revisión.",
                    fontSize = 11.sp,
                    color = NexoCelesteColors.CelesteOscuroTexto
                )
            }
        }

        state.reservas.forEach { res ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NexoCelesteColors.CelesteBorde, RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "[${res.nivel.etiqueta}] ${res.nombreRecurso}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = NexoCelesteColors.CelesteOscuroTexto
                        )
                        Text(
                            text = res.codigoRecurso,
                            fontSize = 11.sp,
                            color = NexoCelesteColors.CelestePrimario,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Inventario total: ${res.cantidadTotalInventario} ${res.unidad} · Reservado: ${res.cantidadReservadaLocal} · Disponible: ${res.cantidadDisponible} ${res.unidad}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (res.cantidadDisponible > 0) NexoCelesteColors.CelestePrimario else Color(0xFFB71C1C)
                    )
                    Text(
                        text = "Responsable: ${res.responsableInventario}" +
                            (if (res.enConflictoParticion) " · Conflicto por revisar" else ""),
                        fontSize = 11.sp,
                        color = if (res.enConflictoParticion) Color(0xFFC62828) else Color.DarkGray
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.reabastecerOResolverConflictoReserva(res, 0, false) }
                        ) {
                            Text("Liberar / reabastecer (${res.cantidadTotalInventario} disp.)", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.reabastecerOResolverConflictoReserva(res, res.cantidadReservadaLocal, !res.enConflictoParticion) }
                        ) {
                            Text(if (res.enConflictoParticion) "Resolver conflicto" else "Simular conflicto", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubVistaConfiguracionRf09(
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
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NexoCelesteColors.CelesteBorde, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Ajustes (San Rafael del Sur → Managua)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NexoCelesteColors.CelesteOscuroTexto
                )
                Text(
                    text = "Versión de la aplicación: 1.0.0 · Última actualización: 16/10/2026",
                    fontSize = 11.sp,
                    color = NexoCelesteColors.CelestePrimario
                )
                Text(
                    text = "Plazos: confirmar recepción en 60 s · decisión en 180 s (la confirmación del radio no reinicia el plazo).",
                    fontSize = 12.sp
                )
                Text(
                    text = "Conservación de datos: 90 días, con respaldo cifrado fuera de Managua.",
                    fontSize = 12.sp
                )

                HorizontalDivider()
                Text(
                    text = "Directorio: Comarca Masachapa → Municipio San Rafael del Sur → Cabecera → Sede Central:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NexoCelesteColors.CelesteOscuroTexto
                )
                state.instituciones.forEach { inst ->
                    Surface(
                        color = NexoCelesteColors.CelesteClaroFondo,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "${inst.nivel.orden}. ${inst.nivel.etiqueta} — ${inst.nombre}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NexoCelesteColors.CelesteOscuroTexto
                            )
                            Text(
                                text = "Ciudad: ${inst.ciudad} · Responsable: ${inst.responsableTurno}",
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NexoCelesteColors.CelesteBorde, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Registro de seguridad:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = NexoCelesteColors.CelesteOscuroTexto
                )
                Button(
                    onClick = { viewModel.probarSeguridadHmacYRechazoModificado() },
                    colors = ButtonDefaults.buttonColors(containerColor = NexoCelesteColors.CelestePrimario),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Verificar que los mensajes alterados se rechazan")
                }
                state.bitacoraSeguridad.forEach { item ->
                    Text(text = "• $item", fontSize = 11.sp)
                }
            }
        }
    }
}
