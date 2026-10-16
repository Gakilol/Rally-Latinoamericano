package ni.edu.rally.nexosos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ni.edu.rally.nexosos.domain.model.*
import ni.edu.rally.nexosos.ui.NexoUiState
import ni.edu.rally.nexosos.ui.theme.NexoCelesteColors

data class EstiloEstadoAccesible(
    val icono: ImageVector,
    val colorFondo: Color,
    val colorTexto: Color,
    val etiquetaLimpia: String
) {
    val prefijoTexto: String get() = etiquetaLimpia
}

fun obtenerEstiloEstado(estado: EstadoSolicitud): EstiloEstadoAccesible = when (estado) {
    EstadoSolicitud.CREADO -> EstiloEstadoAccesible(
        Icons.Default.EditNote, Color(0xFFE1F5FE), Color(0xFF01579B), "Registrado en el teléfono"
    )
    EstadoSolicitud.PENDIENTE_TRANSMISION -> EstiloEstadoAccesible(
        Icons.Default.PortableWifiOff, Color(0xFFFFF8E1), Color(0xFFE65100), "En espera de señal de radio"
    )
    EstadoSolicitud.ENVIADO_A_RADIO -> EstiloEstadoAccesible(
        Icons.Default.SettingsInputAntenna, Color(0xFFE1F5FE), Color(0xFF0277BD), "Enviado por radio"
    )
    EstadoSolicitud.ACK_TECNICO_RADIO -> EstiloEstadoAccesible(
        Icons.Default.Router, Color(0xFFE0F7FA), Color(0xFF006064), "Repetidora confirmó · Esperando autoridad"
    )
    EstadoSolicitud.SIN_CONFIRMACION -> EstiloEstadoAccesible(
        Icons.Default.TimerOff, Color(0xFFFFEBEE), Color(0xFFB71C1C), "Sin respuesta local · Escalando"
    )
    EstadoSolicitud.RECIBIDO_AUTORIDAD -> EstiloEstadoAccesible(
        Icons.Default.MarkEmailRead, Color(0xFFB3E5FC), Color(0xFF01579B), "Recibido por la autoridad"
    )
    EstadoSolicitud.ACEPTADO_PARCIAL -> EstiloEstadoAccesible(
        Icons.Default.PieChart, Color(0xFFE1F5FE), Color(0xFF0288D1), "Atendido en parte · Resto en apoyo"
    )
    EstadoSolicitud.ESCALADO -> EstiloEstadoAccesible(
        Icons.Default.TrendingUp, Color(0xFFFFF3E0), Color(0xFFBF360C), "Transferido al siguiente nivel"
    )
    EstadoSolicitud.ACEPTADO -> EstiloEstadoAccesible(
        Icons.Default.Verified, Color(0xFFE8F5E9), Color(0xFF1B5E20), "Ayuda asignada completa"
    )
    EstadoSolicitud.EN_CAMINO -> EstiloEstadoAccesible(
        Icons.Default.Navigation, Color(0xFFE1F5FE), Color(0xFF01579B), "Brigada en camino al lugar"
    )
    EstadoSolicitud.EN_ATENCION -> EstiloEstadoAccesible(
        Icons.Default.MedicalServices, Color(0xFFE8F5E9), Color(0xFF2E7D32), "Atendiendo en el sitio"
    )
    EstadoSolicitud.ATENDIDO -> EstiloEstadoAccesible(
        Icons.Default.TaskAlt, Color(0xFFC8E6C9), Color(0xFF1B5E20), "Emergencia atendida"
    )
    EstadoSolicitud.PENDIENTE_APOYO_CENTRAL -> EstiloEstadoAccesible(
        Icons.Default.WarningAmber, Color(0xFFFFCDD2), Color(0xFFB71C1C), "En gestión de apoyo nacional"
    )
    EstadoSolicitud.CANCELADO -> EstiloEstadoAccesible(
        Icons.Default.Cancel, Color(0xFFEEEEEE), Color(0xFF424242), "Solicitud cancelada"
    )
}

@Composable
fun BadgeEstadoAccesible(estado: EstadoSolicitud, modifier: Modifier = Modifier) {
    val estilo = obtenerEstiloEstado(estado)
    Row(
        modifier = modifier
            .background(estilo.colorFondo, RoundedCornerShape(8.dp))
            .border(1.dp, estilo.colorTexto.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = estilo.icono,
            contentDescription = estilo.etiquetaLimpia,
            tint = estilo.colorTexto,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = estilo.etiquetaLimpia,
            color = estilo.colorTexto,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun BarraEstadoSuperiorRf01(
    state: NexoUiState,
    onSelectRol: (RolActor) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NexoCelesteColors.CelestePrimario,
                        NexoCelesteColors.CelesteBrillante
                    )
                )
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Nexo SOS · San Rafael del Sur",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Red de Respuesta y Navegación Comunitaria",
                    color = Color(0xFFE1F5FE),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = null,
                        tint = NexoCelesteColors.CelestePrimario,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Red LoRa Activa",
                        color = NexoCelesteColors.CelesteOscuroTexto,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ChipIndicadorConexion(
                icono = if (state.estadoRadio.radioVinculado) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                texto = if (state.estadoRadio.radioVinculado) {
                    "Radio conectado (${state.estadoRadio.dispositivoActual.bateriaPorcentaje}%)"
                } else {
                    "Sin radio conectado"
                },
                activo = state.estadoRadio.radioVinculado,
                esAdvertenciaSiInactivo = true
            )
            ChipIndicadorConexion(
                icono = if (state.estadoRadio.autoridadAlcanzable) Icons.Default.Sensors else Icons.Default.SignalCellularOff,
                texto = if (state.estadoRadio.autoridadAlcanzable) {
                    "Cobertura en corredor"
                } else {
                    "Sin cobertura de radio"
                },
                activo = state.estadoRadio.autoridadAlcanzable,
                esAdvertenciaSiInactivo = true
            )
            ChipIndicadorConexion(
                icono = if (state.estadoPasarela.salidaHaciaManaguaDisponible) Icons.Default.CloudDone else Icons.Default.CloudOff,
                texto = if (state.estadoPasarela.salidaHaciaManaguaDisponible) {
                    "Enlace a Managua activo"
                } else {
                    "Enlace a Managua en espera"
                },
                activo = state.estadoPasarela.salidaHaciaManaguaDisponible,
                esAdvertenciaSiInactivo = true
            )
            ChipIndicadorConexion(
                icono = if (state.estadoRadio.internetDisponible) Icons.Default.Wifi else Icons.Default.WifiOff,
                texto = if (state.estadoRadio.internetDisponible) "Con internet" else "Modo sin internet",
                activo = state.estadoRadio.internetDisponible,
                esAdvertenciaSiInactivo = false
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Vista:",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            RolActor.entries.forEach { rol ->
                val seleccionado = state.rolActivo == rol
                Surface(
                    color = if (seleccionado) Color.White else Color(0xFF01579B).copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.clickable { onSelectRol(rol) }
                ) {
                    Text(
                        text = rol.titulo,
                        color = if (seleccionado) NexoCelesteColors.CelesteOscuroTexto else Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (seleccionado) FontWeight.ExtraBold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChipIndicadorConexion(
    icono: ImageVector,
    texto: String,
    activo: Boolean,
    esAdvertenciaSiInactivo: Boolean
) {
    val bg = when {
        activo -> Color.White
        esAdvertenciaSiInactivo -> Color(0xFFFFEBEE)
        else -> Color(0xFFE1F5FE)
    }
    val fg = when {
        activo -> Color(0xFF01579B)
        esAdvertenciaSiInactivo -> Color(0xFFB71C1C)
        else -> Color(0xFF0277BD)
    }
    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icono,
            contentDescription = texto,
            tint = fg,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = texto,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun BarraCadenaTerritorial(nivelActual: NivelTerritorial, estadoGeneral: EstadoSolicitud) {
    val nombresCortos = mapOf(
        NivelTerritorial.COMARCA to "Masachapa",
        NivelTerritorial.MUNICIPIO to "San Rafael",
        NivelTerritorial.CABECERA_DEPARTAMENTAL to "Departamento",
        NivelTerritorial.SEDE_CENTRAL_MANAGUA to "Managua"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NivelTerritorial.entries.forEachIndexed { idx, nivel ->
            val esActual = nivel == nivelActual
            val yaPaso = nivel.orden < nivelActual.orden || estadoGeneral == EstadoSolicitud.ATENDIDO
            val bg = when {
                esActual -> NexoCelesteColors.CelestePrimario
                yaPaso -> NexoCelesteColors.CelesteClaroFondo
                else -> Color(0xFFF5F9FC)
            }
            val fg = when {
                esActual -> Color.White
                yaPaso -> NexoCelesteColors.CelesteOscuroTexto
                else -> Color(0xFF78909C)
            }

            Surface(
                color = bg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(
                        width = 1.dp,
                        color = if (esActual) NexoCelesteColors.CelestePrimario else NexoCelesteColors.CelesteBorde,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = nombresCortos[nivel] ?: nivel.etiqueta,
                        color = fg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            esActual -> "Atendiendo"
                            yaPaso -> "Completado"
                            else -> "En espera"
                        },
                        color = fg.copy(alpha = 0.9f),
                        fontSize = 9.sp
                    )
                }
            }
            if (idx < NivelTerritorial.entries.size - 1) {
                Text(
                    text = "→",
                    fontWeight = FontWeight.Bold,
                    color = NexoCelesteColors.CelestePrimario,
                    modifier = Modifier.padding(horizontal = 3.dp)
                )
            }
        }
    }
}
