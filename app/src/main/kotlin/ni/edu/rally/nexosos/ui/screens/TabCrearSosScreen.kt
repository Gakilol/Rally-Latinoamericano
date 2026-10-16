package ni.edu.rally.nexosos.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ni.edu.rally.nexosos.domain.model.*
import ni.edu.rally.nexosos.ui.NexoSosViewModel
import ni.edu.rally.nexosos.ui.NexoUiState
import ni.edu.rally.nexosos.ui.theme.NexoCelesteColors

/**
 * Pantalla RF-02 (Crear SOS) y RF-03 (Ubicación verificable en San Rafael del Sur)
 * con paleta Celeste y Blanco.
 */
@Composable
fun TabCrearSosScreen(
    state: NexoUiState,
    viewModel: NexoSosViewModel,
    onSosCreadoIrAMisSolicitudes: () -> Unit
) {
    var tipoNecesidad by remember { mutableStateOf(TipoNecesidad.AGUA_Y_ALIMENTOS) }
    var cantidadTexto by remember { mutableStateOf("10") }
    var urgencia by remember { mutableStateOf(UrgenciaDeclarada.CRITICA_VITAL) }
    var personasTexto by remember { mutableStateOf("42") }
    var fuenteUbicacion by remember { mutableStateOf(FuenteUbicacion.GNSS_TELEFONO) }
    var latitudTexto by remember { mutableStateOf("11.7809") }
    var longitudTexto by remember { mutableStateOf("-86.5193") }
    var precisionMetros by remember { mutableStateOf("6") }
    var antiguedadSeg by remember { mutableStateOf("4") }
    var referenciaSitio by remember { mutableStateOf("PR-MAS-ESC01 (Escuela Comarca Masachapa)") }
    var descripcionAmpliada by remember {
        mutableStateOf("Familias evacuadas en Escuela de Masachapa (San Rafael del Sur) tras inundación costera. Fotos y notas detalladas se conservan en base local fuera de LoRa.")
    }
    var mostrarDialogoConfirmacion by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = if (state.estadoRadio.radioVinculado) NexoCelesteColors.CelesteClaroFondo else Color(0xFFFFF3E0),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NexoCelesteColors.CelesteBorde, RoundedCornerShape(10.dp))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (state.estadoRadio.radioVinculado) Icons.Default.BluetoothConnected else Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = if (state.estadoRadio.radioVinculado) NexoCelesteColors.CelestePrimario else Color(0xFFE65100)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.estadoRadio.radioVinculado) {
                        "Radio conectado: al enviar tu solicitud se guarda en el teléfono y se transmite de inmediato por el radio hacia la torre de Masachapa."
                    } else {
                        "Atención: el teléfono no tiene un radio conectado. Tu solicitud se guardará como 'Pendiente de envío' y no se considerará recibida por la autoridad hasta conectar un radio o acudir al Punto Comunitario de Masachapa."
                    },
                    fontSize = 12.sp,
                    color = if (state.estadoRadio.radioVinculado) NexoCelesteColors.CelesteOscuroTexto else Color(0xFFE65100),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NexoCelesteColors.CelesteBorde, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "¿Qué necesitas?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NexoCelesteColors.CelesteOscuroTexto
                )

                Text("1. Tipo de ayuda:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                TipoNecesidad.entries.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = tipoNecesidad == item,
                            onClick = { tipoNecesidad = item }
                        )
                        Text("${item.etiqueta} (${item.unidadPorDefecto})", fontSize = 12.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = cantidadTexto,
                        onValueChange = { cantidadTexto = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Cantidad (${tipoNecesidad.unidadPorDefecto})") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = personasTexto,
                        onValueChange = { personasTexto = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Personas afectadas") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text("2. ¿Qué tan urgente es?", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UrgenciaDeclarada.entries.forEach { u ->
                        FilterChip(
                            selected = urgencia == u,
                            onClick = { urgencia = u },
                            label = { Text(u.etiqueta, fontSize = 11.sp) }
                        )
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
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "¿Dónde estás? (Masachapa / Pochomil / El Salto)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NexoCelesteColors.CelesteOscuroTexto
                )
                Text(
                    text = "Importante: la ubicación de una torre de radio nunca se usa como si fuera la tuya.",
                    fontSize = 11.sp,
                    color = Color(0xFFB71C1C),
                    fontWeight = FontWeight.SemiBold
                )

                FuenteUbicacion.entries.forEach { f ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = fuenteUbicacion == f,
                            onClick = { fuenteUbicacion = f }
                        )
                        Text(f.etiqueta, fontSize = 12.sp)
                    }
                }

                // Botones rápidos de sitios reales de San Rafael del Sur
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            latitudTexto = "11.7809"
                            longitudTexto = "-86.5193"
                            referenciaSitio = "PR-MAS-ESC01 (Escuela Masachapa)"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Masachapa", fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            latitudTexto = "11.7724"
                            longitudTexto = "-86.5044"
                            referenciaSitio = "PR-POCH-02 (Centro Pochomil)"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pochomil", fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            latitudTexto = "11.8920"
                            longitudTexto = "-86.3880"
                            referenciaSitio = "COM-SALTO-KM34 (El Salto)"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("El Salto", fontSize = 10.sp)
                    }
                }

                if (fuenteUbicacion != FuenteUbicacion.REFERENCIA_SITIO_COMUNITARIO) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = latitudTexto,
                            onValueChange = { latitudTexto = it },
                            label = { Text("Latitud") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = longitudTexto,
                            onValueChange = { longitudTexto = it },
                            label = { Text("Longitud") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = precisionMetros,
                            onValueChange = { precisionMetros = it.filter { c -> c.isDigit() } },
                            label = { Text("Precisión (m)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = antiguedadSeg,
                            onValueChange = { antiguedadSeg = it.filter { c -> c.isDigit() } },
                            label = { Text("Antigüedad de la ubicación (s)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                OutlinedTextField(
                    value = referenciaSitio,
                    onValueChange = { referenciaSitio = it },
                    label = { Text("Referencia del sitio (corta, máx. 24 caracteres)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = descripcionAmpliada,
                    onValueChange = { descripcionAmpliada = it },
                    label = { Text("Descripción ampliada (se guarda en el teléfono)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        }

        Button(
            onClick = { mostrarDialogoConfirmacion = true },
            colors = ButtonDefaults.buttonColors(containerColor = NexoCelesteColors.CelestePrimario),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Icon(Icons.Default.Sos, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "PEDIR AYUDA",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }

    if (mostrarDialogoConfirmacion) {
        val cant = cantidadTexto.toIntOrNull() ?: 10
        val pers = personasTexto.toIntOrNull() ?: 1
        AlertDialog(
            onDismissRequest = { mostrarDialogoConfirmacion = false },
            title = { Text("Confirmar solicitud de ayuda") },
            text = {
                Text(
                    "• Necesidad: $cant ${tipoNecesidad.unidadPorDefecto} (${tipoNecesidad.etiqueta})\n" +
                        "• Personas afectadas: $pers · Urgencia: ${urgencia.etiqueta}\n" +
                        "• Ubicación: ${fuenteUbicacion.etiqueta} ($referenciaSitio)\n" +
                        "• Se envía primero a: Comarca Masachapa\n" +
                        "• Estado inicial: ${if (state.estadoRadio.radioVinculado) "Enviado" else "Pendiente de envío (sin radio)"}"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoConfirmacion = false
                        val lat = if (fuenteUbicacion == FuenteUbicacion.REFERENCIA_SITIO_COMUNITARIO) null else latitudTexto.toDoubleOrNull()
                        val lon = if (fuenteUbicacion == FuenteUbicacion.REFERENCIA_SITIO_COMUNITARIO) null else longitudTexto.toDoubleOrNull()
                        viewModel.crearSolicitudSos(
                            tipoNecesidad = tipoNecesidad,
                            cantidad = cant,
                            urgencia = urgencia,
                            personasAfectadas = pers,
                            fuenteUbicacion = fuenteUbicacion,
                            latitud = lat,
                            longitud = lon,
                            precisionMetros = precisionMetros.toIntOrNull(),
                            antiguedadSeg = antiguedadSeg.toIntOrNull(),
                            referenciaSitio = referenciaSitio,
                            descripcionAmpliada = descripcionAmpliada
                        )
                        onSosCreadoIrAMisSolicitudes()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexoCelesteColors.CelestePrimario)
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoConfirmacion = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
