package ni.edu.rally.nexosos.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ni.edu.rally.nexosos.datalocal.db.NexoDatabase
import ni.edu.rally.nexosos.datalocal.repository.NexoLocalRepository
import ni.edu.rally.nexosos.domain.engine.*
import ni.edu.rally.nexosos.domain.model.*
import ni.edu.rally.nexosos.protocol.NexoCompactFrame
import ni.edu.rally.nexosos.protocol.NexoPacketCodec
import ni.edu.rally.nexosos.protocol.PacketAuthenticationException
import ni.edu.rally.nexosos.transportip.EstadoPasarelaManagua
import ni.edu.rally.nexosos.transportip.GatewaySyncManager
import ni.edu.rally.nexosos.transportmesh.EstadoConexionRadio
import ni.edu.rally.nexosos.transportmesh.MeshTransportManager
import ni.edu.rally.nexosos.transportmesh.ResultadoPruebaCargaRadio
import java.util.UUID

data class NexoUiState(
    val rolActivo: RolActor = RolActor.COMUNIDAD_BRIGADA,
    val estadoRadio: EstadoConexionRadio = EstadoConexionRadio(),
    val estadoPasarela: EstadoPasarelaManagua = EstadoPasarelaManagua(),
    val instituciones: List<Institucion> = emptyList(),
    val solicitudes: List<SolicitudConDetalle> = emptyList(),
    val reservas: List<Reserva> = emptyList(),
    val outboxItems: List<Outbox> = emptyList(),
    val reportesViales: List<ReporteVial> = emptyList(),
    val centrosAtencion: List<CentroAtencion> = emptyList(),
    val tramosWaze: List<TramoVialWaze> = WazeRouteEngine.tramosInicialesSanRafael,
    val nodoOrigenWazeId: String = "NODO_SAN_RAFAEL",
    val nodoDestinoWazeId: String = "NODO_MASACHAPA",
    val rutaOptimaWaze: RutaOptimaWaze = WazeRouteEngine.calcularRutaOptima(
        "NODO_SAN_RAFAEL",
        "NODO_MASACHAPA",
        WazeRouteEngine.tramosInicialesSanRafael
    ),
    val progresoBrigadaWazePct: Float = 0.35f,
    val relojActualEpochMs: Long = System.currentTimeMillis(),
    val pasoDemoRallyActual: Int = 0,
    val uuidSosDemoRally: String? = null,
    val ultimoMensajeBanner: String = "Corredor San Rafael del Sur activo (Masachapa → San Rafael → El Crucero → Managua).",
    val resultadoUltimaPruebaCarga: ResultadoPruebaCargaRadio? = null,
    val bitacoraSeguridad: List<String> = listOf(
        "Corredor activo: San Rafael del Sur (Masachapa, Casco Urbano San Rafael, El Crucero y Managua).",
        "Navegación en vivo Waze habilitada sobre la red vial NIC-2, NIC-12 y Costera.",
        "Firmas de seguridad institucionales activas para los 4 niveles territoriales.",
        "Protección activa: una solicitud sin radio conectado se guarda como 'Pendiente de envío'."
    )
) {
    val nivelBandejaActiva: NivelTerritorial
        get() = rolActivo.nivelAsociado ?: NivelTerritorial.COMARCA
}

class NexoSosViewModel(application: Application) : AndroidViewModel(application) {

    private val db = NexoDatabase.getInstance(application)
    private val repository = NexoLocalRepository(db.nexoDao())
    private val meshTransport = MeshTransportManager(application)
    private val gatewaySync = GatewaySyncManager()

    private val _rolActivo = MutableStateFlow(RolActor.COMUNIDAD_BRIGADA)
    private val _relojActual = MutableStateFlow(System.currentTimeMillis())
    private val _pasoDemoRally = MutableStateFlow(0)
    private val _uuidSosDemo = MutableStateFlow<String?>(null)
    private val _ultimoBanner = MutableStateFlow("Corredor San Rafael del Sur activo: Mapa de rutas en vivo y comunicación por radio listos.")
    private val _resultadoCarga = MutableStateFlow<ResultadoPruebaCargaRadio?>(null)
    private val _tramosWaze = MutableStateFlow(WazeRouteEngine.tramosInicialesSanRafael)
    private val _nodoOrigenWaze = MutableStateFlow("NODO_MANAGUA")
    private val _nodoDestinoWaze = MutableStateFlow("NODO_MASACHAPA")
    private val _progresoBrigadaWaze = MutableStateFlow(0.25f)
    private val _bitacoraSeguridad = MutableStateFlow(
        listOf(
            "Corredor San Rafael del Sur activo: Torres en Masachapa, San Rafael del Sur y El Crucero.",
            "Credenciales institucionales verificadas para Comarca, Municipio, Departamento y Sede Central.",
            "Red de emergencia privada activa con prevención automática de mensajes en bucle.",
            "Verificación automática de integridad y autenticidad de mensajes habilitada."
        )
    )

    val uiState: StateFlow<NexoUiState> = combine(
        combine(_rolActivo, meshTransport.estado, gatewaySync.estado) { rol, radio, pasarela ->
            Triple(rol, radio, pasarela)
        },
        combine(
            repository.observeInstituciones(),
            repository.observeSolicitudesConDetalle(),
            repository.observeReservas()
        ) { insts, sols, res ->
            Triple(insts, sols, res)
        },
        combine(
            repository.observeOutbox(),
            repository.observeReportesViales(),
            repository.observeCentrosAtencion()
        ) { out, viales, centros ->
            Triple(out, viales, centros)
        },
        combine(
            _relojActual,
            _pasoDemoRally,
            _uuidSosDemo,
            _ultimoBanner,
            _resultadoCarga
        ) { reloj, paso, uuidDemo, banner, carga ->
            listOf(reloj, paso, uuidDemo, banner, carga)
        },
        combine(
            _tramosWaze,
            _nodoOrigenWaze,
            _nodoDestinoWaze,
            _progresoBrigadaWaze,
            _bitacoraSeguridad
        ) { tramos, orig, dest, prog, bitacora ->
            listOf(tramos, orig, dest, prog, bitacora)
        }
    ) { (rol, radio, pasarela), (insts, sols, res), (out, viales, centros), meta, wazeMeta ->
        @Suppress("UNCHECKED_CAST")
        val tramos = wazeMeta[0] as List<TramoVialWaze>
        val origId = wazeMeta[1] as String
        val destId = wazeMeta[2] as String
        val prog = wazeMeta[3] as Float
        @Suppress("UNCHECKED_CAST")
        val bitacora = wazeMeta[4] as List<String>

        val rutaOptima = WazeRouteEngine.calcularRutaOptima(origId, destId, tramos)

        NexoUiState(
            rolActivo = rol,
            estadoRadio = radio,
            estadoPasarela = pasarela,
            instituciones = insts,
            solicitudes = sols,
            reservas = res,
            outboxItems = out,
            reportesViales = viales,
            centrosAtencion = centros,
            tramosWaze = tramos,
            nodoOrigenWazeId = origId,
            nodoDestinoWazeId = destId,
            rutaOptimaWaze = rutaOptima,
            progresoBrigadaWazePct = prog,
            relojActualEpochMs = meta[0] as Long,
            pasoDemoRallyActual = meta[1] as Int,
            uuidSosDemoRally = meta[2] as String?,
            ultimoMensajeBanner = meta[3] as String,
            resultadoUltimaPruebaCarga = meta[4] as ResultadoPruebaCargaRadio?,
            bitacoraSeguridad = bitacora
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NexoUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
        }
        viewModelScope.launch {
            while (true) {
                delay(1_000L)
                _relojActual.value = System.currentTimeMillis()
            }
        }
    }

    fun seleccionarRol(rol: RolActor) {
        _rolActivo.value = rol
        _ultimoBanner.value = "Rol activo cambiado a: ${rol.titulo}"
    }

    fun cambiarNodosRutaWaze(origenId: String, destinoId: String) {
        _nodoOrigenWaze.value = origenId
        _nodoDestinoWaze.value = destinoId
        _progresoBrigadaWaze.value = 0.15f
        val nuevaRuta = WazeRouteEngine.calcularRutaOptima(origenId, destinoId, _tramosWaze.value)
        _ultimoBanner.value = nuevaRuta.resumenRuta
    }

    /**
     * Permite reportar o despejar en vivo un bloqueo estilo Waze en cualquier tramo de San Rafael del Sur
     * y recalcula de inmediato la ruta más óptima hacia el lugar del desastre.
     */
    fun alternarEstadoTramoWaze(tramoId: String) {
        _tramosWaze.update { lista ->
            lista.map { tr ->
                if (tr.id != tramoId) tr
                else {
                    val siguienteEstado = when (tr.estadoRuta) {
                        EstadoRuta.COMPROBADA_HABILITADA -> EstadoRuta.VERIFICADA_BLOQUEADA
                        EstadoRuta.CANDIDATA_NO_VERIFICADA -> EstadoRuta.VERIFICADA_BLOQUEADA
                        EstadoRuta.REPORTADA_BLOQUEADA, EstadoRuta.VERIFICADA_BLOQUEADA -> EstadoRuta.COMPROBADA_HABILITADA
                    }
                    val nuevaAlerta = if (siguienteEstado == EstadoRuta.VERIFICADA_BLOQUEADA) {
                        "⛔ VÍA CERRADA (Reporte Waze SOS: Derrumbe / Inundación)"
                    } else {
                        "✅ Vía despejada y comprobada por brigada"
                    }
                    tr.copy(estadoRuta = siguienteEstado, tipoAlertaWaze = nuevaAlerta)
                }
            }
        }
        val recalculo = WazeRouteEngine.calcularRutaOptima(_nodoOrigenWaze.value, _nodoDestinoWaze.value, _tramosWaze.value)
        _ultimoBanner.value = "Mapa Waze recalculado: ${recalculo.resumenRuta}"
    }

    fun avanzarSimulacionVehiculoWaze() {
        _progresoBrigadaWaze.update { actual ->
            val sig = actual + 0.20f
            if (sig > 1.0f) 0.0f else sig
        }
        val pct = (_progresoBrigadaWaze.value * 100).toInt()
        _ultimoBanner.value = "Brigada avanzando por ruta óptima Waze hacia el desastre ($pct% del recorrido completado)."
    }

    fun cambiarModoTransporte(modo: CanalTransporte) {
        meshTransport.cambiarModoTransporte(modo)
        _ultimoBanner.value = "Transporte configurado: ${modo.etiqueta}"
    }

    fun alternarRadioVinculado(vinculado: Boolean) {
        meshTransport.alternarRadioVinculado(vinculado)
        viewModelScope.launch {
            if (vinculado) {
                val actuales = uiState.value.solicitudes
                val pendientes = actuales.count { it.solicitud.estadoGeneral == EstadoSolicitud.PENDIENTE_TRANSMISION }
                if (pendientes > 0) {
                    repository.transmitirPendientesAlConectarRadio(
                        solicitudes = actuales,
                        canal = uiState.value.estadoRadio.modoTransporte
                    )
                    meshTransport.registrarTramaEnviada(108)
                    _ultimoBanner.value = "Radio conectado: se enviaron $pendientes solicitudes pendientes automáticamente."
                } else {
                    _ultimoBanner.value = "Radio conectado y listo para transmitir en el corredor San Rafael del Sur."
                }
            } else {
                _ultimoBanner.value = "Radio desconectado: las nuevas solicitudes se guardarán como 'Pendiente de envío'."
            }
        }
    }

    fun alternarInternetDisponible(disponible: Boolean) {
        meshTransport.alternarInternetDisponible(disponible)
        _ultimoBanner.value = if (disponible) {
            "Internet local disponible (la red de radio continúa operando de forma independiente)."
        } else {
            "Modo sin internet activo: comunicación 100% por radio en San Rafael del Sur."
        }
    }

    fun alternarMallaAisladaDeManagua(aislada: Boolean) {
        gatewaySync.configurarMallaAisladaDeManagua(aislada)
        _ultimoBanner.value = if (aislada) {
            "Managua sin enlace: las solicitudes escaladas a nivel central quedarán pendientes de entrega hasta reconectar."
        } else {
            "Enlace con Managua restaurado: mensajes pendientes sincronizados y confirmados."
        }
    }

    fun alternarSedeCentralDestruida(destruida: Boolean) {
        gatewaySync.alternarSedeCentralDestruida(destruida, activarConsolaAlterna = true)
        _ultimoBanner.value = gatewaySync.estado.value.ultimoEstadoSincronizacion
    }

    fun iniciarEscaneoBle() = meshTransport.iniciarEscaneoBleReal()
    fun detenerEscaneoBle() = meshTransport.detenerEscaneoBleReal()

    fun crearSolicitudSos(
        tipoNecesidad: TipoNecesidad,
        cantidad: Int,
        urgencia: UrgenciaDeclarada,
        personasAfectadas: Int,
        fuenteUbicacion: FuenteUbicacion,
        latitud: Double?,
        longitud: Double?,
        precisionMetros: Int?,
        antiguedadSeg: Int?,
        referenciaSitio: String,
        descripcionAmpliada: String
    ) {
        viewModelScope.launch {
            val radioActivo = uiState.value.estadoRadio.radioVinculado
            val canal = uiState.value.estadoRadio.modoTransporte
            val creado = repository.crearSosAtomico(
                tipoNecesidad = tipoNecesidad,
                cantidadSolicitada = cantidad.coerceAtLeast(1),
                urgencia = urgencia,
                personasAfectadas = personasAfectadas.coerceAtLeast(1),
                fuenteUbicacion = fuenteUbicacion,
                latitud = latitud,
                longitud = longitud,
                precisionMetros = precisionMetros,
                antiguedadSeg = antiguedadSeg,
                referenciaSitio = referenciaSitio,
                descripcionAmpliadaLocal = descripcionAmpliada,
                radioVinculadoActivo = radioActivo,
                canalPreferido = canal,
                creadoPorNombre = uiState.value.rolActivo.titulo
            )
            val bytes = creado.ultimoOutbox?.tamanoBytes ?: 104
            if (radioActivo) {
                meshTransport.registrarTramaEnviada(bytes)
                _ultimoBanner.value = "Solicitud #${creado.solicitud.uuidEvento.take(8)} enviada por radio hacia Comarca Masachapa."
            } else {
                _ultimoBanner.value = "Solicitud #${creado.solicitud.uuidEvento.take(8)} guardada como Pendiente de envío (conecta un radio para transmitirla)."
            }
        }
    }

    private data class EscenarioTorre(
        val frame: NexoCompactFrame,
        val nombreTorre: String,
        val nodoId: String,
        val rssiDbm: Int,
        val snrDb: Double,
        val destWaze: String,
        val descLocal: String
    )

    /**
     * Recibe una trama binaria SOS entrante desde una de las 3 torres repetidoras del corredor de San Rafael del Sur:
     * - Torre 1 (Masachapa · !a1b201): SOS desde el Centro Comunitario de Pochomil
     * - Torre 2 (San Rafael del Sur · !a1b202): SOS desde Comarca El Salto Km 34
     * - Torre 3 (El Crucero · !a1b203): SOS escalado desde Cruce Costero Montelimar
     */
    fun recibirSosEntranteDesdeTorre(indiceTorre: Int) {
        viewModelScope.launch {
            val ahoraSec = System.currentTimeMillis() / 1000L
            val uuidNuevo = UUID.randomUUID().toString()

            val esc = when (indiceTorre) {
                1 -> {
                    val f = NexoCompactFrame(
                        tipoMensaje = TipoMensajeApp.SOS_ACTUALIZACION,
                        uuidEvento = uuidNuevo,
                        idMensajeCorto = (ahoraSec % 900 + 100).toInt(),
                        revision = 1,
                        institucionOrigenCodigo = "COM-01",
                        institucionDestinoCodigo = "COM-01",
                        nivelOrigen = NivelTerritorial.COMARCA,
                        nivelDestino = NivelTerritorial.COMARCA,
                        timestampEpochSec = ahoraSec,
                        calidadReloj = CalidadReloj.SINCRONIZADO_GNSS_RED,
                        fuenteUbicacion = FuenteUbicacion.GNSS_TELEFONO,
                        latitud = 11.7724,
                        longitud = -86.5044,
                        precisionMetros = 5,
                        antiguedadUbicacionSeg = 3,
                        referenciaSitioCompacta = "Centro Com. Pochomil",
                        tipoNecesidad = TipoNecesidad.AGUA_Y_ALIMENTOS,
                        cantidadSolicitada = 8,
                        cantidadAceptadaTotal = 0,
                        cantidadPendiente = 8,
                        personasAfectadas = 35,
                        urgencia = UrgenciaDeclarada.CRITICA_VITAL,
                        estadoGeneral = EstadoSolicitud.ENVIADO_A_RADIO,
                        motivo = MotivoCapacidad.NINGUNO,
                        vencimientoRestanteSeg = 60,
                        responsableORecursoCompacto = "Brigada Pochomil",
                        keyId = 101
                    )
                    EscenarioTorre(
                        f,
                        "Torre 1 · Masachapa",
                        "!a1b201",
                        -65,
                        10.2,
                        "NODO_POCHOMIL",
                        "Mensaje SOS recibido por Torre 1 (Masachapa): 35 personas en Centro Comunitario de Pochomil solicitan 8 kits de agua y alimentos."
                    )
                }
                2 -> {
                    val f = NexoCompactFrame(
                        tipoMensaje = TipoMensajeApp.SOS_ACTUALIZACION,
                        uuidEvento = uuidNuevo,
                        idMensajeCorto = (ahoraSec % 900 + 100).toInt(),
                        revision = 1,
                        institucionOrigenCodigo = "COM-01",
                        institucionDestinoCodigo = "MUN-01",
                        nivelOrigen = NivelTerritorial.COMARCA,
                        nivelDestino = NivelTerritorial.MUNICIPIO,
                        timestampEpochSec = ahoraSec,
                        calidadReloj = CalidadReloj.SINCRONIZADO_GNSS_RED,
                        fuenteUbicacion = FuenteUbicacion.GNSS_TELEFONO,
                        latitud = 11.8920,
                        longitud = -86.3880,
                        precisionMetros = 7,
                        antiguedadUbicacionSeg = 6,
                        referenciaSitioCompacta = "Comarca El Salto Km34",
                        tipoNecesidad = TipoNecesidad.RESCATE_BRIGADA,
                        cantidadSolicitada = 2,
                        cantidadAceptadaTotal = 0,
                        cantidadPendiente = 2,
                        personasAfectadas = 18,
                        urgencia = UrgenciaDeclarada.CRITICA_VITAL,
                        estadoGeneral = EstadoSolicitud.ENVIADO_A_RADIO,
                        motivo = MotivoCapacidad.NINGUNO,
                        vencimientoRestanteSeg = 60,
                        responsableORecursoCompacto = "Brigada El Salto",
                        keyId = 101
                    )
                    EscenarioTorre(
                        f,
                        "Torre 2 · San Rafael del Sur",
                        "!a1b202",
                        -71,
                        8.4,
                        "NODO_EL_SALTO",
                        "Mensaje SOS recibido por Torre 2 (Casco Urbano San Rafael): 18 personas en Comarca El Salto Km 34 requieren 2 brigadas de rescate."
                    )
                }
                else -> {
                    val f = NexoCompactFrame(
                        tipoMensaje = TipoMensajeApp.SOS_ACTUALIZACION,
                        uuidEvento = uuidNuevo,
                        idMensajeCorto = (ahoraSec % 900 + 100).toInt(),
                        revision = 1,
                        institucionOrigenCodigo = "MUN-01",
                        institucionDestinoCodigo = "DEP-01",
                        nivelOrigen = NivelTerritorial.MUNICIPIO,
                        nivelDestino = NivelTerritorial.CABECERA_DEPARTAMENTAL,
                        timestampEpochSec = ahoraSec,
                        calidadReloj = CalidadReloj.SINCRONIZADO_GNSS_RED,
                        fuenteUbicacion = FuenteUbicacion.GNSS_TELEFONO,
                        latitud = 11.8125,
                        longitud = -86.5110,
                        precisionMetros = 6,
                        antiguedadUbicacionSeg = 4,
                        referenciaSitioCompacta = "Cruce Montelimar",
                        tipoNecesidad = TipoNecesidad.ATENCION_MEDICA_PRIMEROS_AUXILIOS,
                        cantidadSolicitada = 5,
                        cantidadAceptadaTotal = 0,
                        cantidadPendiente = 5,
                        personasAfectadas = 24,
                        urgencia = UrgenciaDeclarada.ALTA,
                        estadoGeneral = EstadoSolicitud.ENVIADO_A_RADIO,
                        motivo = MotivoCapacidad.NINGUNO,
                        vencimientoRestanteSeg = 60,
                        responsableORecursoCompacto = "Puesto Montelimar",
                        keyId = 201
                    )
                    EscenarioTorre(
                        f,
                        "Torre 3 · El Crucero",
                        "!a1b203",
                        -76,
                        6.8,
                        "NODO_MONTELIMAR",
                        "Mensaje SOS recibido por Torre 3 (Cumbre El Crucero): Puesto Montelimar solicita 5 botiquines de atención médica para 24 personas."
                    )
                }
            }

            val frame = esc.frame
            val nombreTorreStr = esc.nombreTorre
            val nodoStr = esc.nodoId
            val rssiInt = esc.rssiDbm
            val snrDbl = esc.snrDb
            val destWaze = esc.destWaze
            val descStr = esc.descLocal

            val (bytesBinarios, _) = NexoPacketCodec.encode(frame)
            val recibido = repository.recibirTramaSosDesdeTorre(
                payloadBinario = bytesBinarios,
                torreNombre = nombreTorreStr,
                nodoLoRaId = nodoStr,
                rssiDbm = rssiInt,
                snrDb = snrDbl,
                descripcionLocalRecibida = descStr,
                canalRecepcion = uiState.value.estadoRadio.modoTransporte
            )

            val resumenCorto = "SOS #${recibido.solicitud.uuidEvento.take(6)} · ${recibido.necesidad.cantidadSolicitada} ${recibido.necesidad.unidad} en ${recibido.solicitud.referenciaSitio}"
            meshTransport.registrarMensajeRecibidoDesdeTorre(
                torreNombre = nombreTorreStr,
                nodoLoRaId = nodoStr,
                rssiDbm = rssiInt,
                snrDb = snrDbl,
                resumenMensaje = resumenCorto,
                tamanoBytes = bytesBinarios.size
            )

            _rolActivo.value = when (frame.nivelDestino) {
                NivelTerritorial.COMARCA -> RolActor.RESPONSABLE_COMARCA
                NivelTerritorial.MUNICIPIO -> RolActor.COORDINACION_MUNICIPAL
                NivelTerritorial.CABECERA_DEPARTAMENTAL -> RolActor.COORDINACION_DEPARTAMENTAL
                NivelTerritorial.SEDE_CENTRAL_MANAGUA -> RolActor.SEDE_CENTRAL_MANAGUA
            }
            _nodoDestinoWaze.value = destWaze
            _progresoBrigadaWaze.value = 0.15f
            _ultimoBanner.value = "📡 Nuevo SOS recibido desde $nombreTorreStr ($rssiInt dBm): ${recibido.necesidad.cantidadSolicitada} ${recibido.necesidad.unidad} en ${recibido.solicitud.referenciaSitio}."
        }
    }

    fun simularAckTecnicoRadioSinAutoridad(detalle: SolicitudConDetalle) {
        viewModelScope.launch {
            val res = EscalationEngine.aplicarAckTecnicoRadio(
                detalle = detalle,
                nodoRadioId = "Torre Masachapa (!a1b201)",
                ahoraEpochMs = System.currentTimeMillis()
            )
            repository.persistirResultadoTransicion(
                resultado = res,
                keyIdEmisor = 101,
                canalPreferido = uiState.value.estadoRadio.modoTransporte
            )
            _ultimoBanner.value = "El radio repetidor recibió la señal: esperando que la autoridad confirme la recepción."
        }
    }

    fun confirmarRecibidoAutoridad(detalle: SolicitudConDetalle) {
        viewModelScope.launch {
            val instActual = obtenerInstitucionParaNivel(detalle.solicitud.nivelActual)
            val res = EscalationEngine.aplicarRecibidoAutoridad(
                detalle = detalle,
                institucionReceptora = instActual,
                operadorReceptor = instActual.responsableTurno,
                ahoraEpochMs = System.currentTimeMillis()
            )
            repository.persistirResultadoTransicion(
                resultado = res,
                keyIdEmisor = keyIdParaNivel(instActual.nivel),
                canalPreferido = uiState.value.estadoRadio.modoTransporte
            )
            meshTransport.registrarTramaRecibida(96)
            _ultimoBanner.value = "Recepción confirmada por ${instActual.nombre}. En evaluación de recursos disponibles."
        }
    }

    fun aceptarRecursosEnNivel(
        detalle: SolicitudConDetalle,
        reserva: Reserva,
        cantidadAceptar: Int
    ) {
        viewModelScope.launch {
            val instActual = obtenerInstitucionParaNivel(detalle.solicitud.nivelActual)
            val sigNivel = detalle.solicitud.nivelActual.siguienteNivel()
            val instSig = sigNivel?.let { obtenerInstitucionParaNivel(it) }

            runCatching {
                EscalationEngine.aplicarAceptacionRecursos(
                    detalle = detalle,
                    institucionActual = instActual,
                    institucionSiguienteNivel = instSig,
                    reservaSeleccionada = reserva,
                    cantidadAceptar = cantidadAceptar,
                    responsableSeguimiento = instActual.responsableTurno,
                    ahoraEpochMs = System.currentTimeMillis()
                )
            }.onSuccess { res ->
                repository.persistirResultadoTransicion(
                    resultado = res,
                    keyIdEmisor = keyIdParaNivel(instActual.nivel),
                    canalPreferido = uiState.value.estadoRadio.modoTransporte,
                    salidaManaguaDisponible = uiState.value.estadoPasarela.salidaHaciaManaguaDisponible
                )
                meshTransport.registrarTramaEnviada(112)
                _ultimoBanner.value = res.solicitudActualizada.notaMotivo
            }.onFailure { err ->
                _ultimoBanner.value = "No se pudo asignar el recurso: ${err.message}"
            }
        }
    }

    fun declararCapacidadInsuficiente(
        detalle: SolicitudConDetalle,
        motivo: MotivoCapacidad,
        observacion: String
    ) {
        viewModelScope.launch {
            val instActual = obtenerInstitucionParaNivel(detalle.solicitud.nivelActual)
            val sigNivel = detalle.solicitud.nivelActual.siguienteNivel()
            val instSig = sigNivel?.let { obtenerInstitucionParaNivel(it) }

            val res = EscalationEngine.declararCapacidadInsuficienteOEscalar(
                detalle = detalle,
                institucionActual = instActual,
                institucionSiguienteNivel = instSig,
                motivo = motivo,
                observacionAdicional = observacion,
                autorNombre = instActual.responsableTurno,
                ahoraEpochMs = System.currentTimeMillis()
            )
            repository.persistirResultadoTransicion(
                resultado = res,
                keyIdEmisor = keyIdParaNivel(instActual.nivel),
                canalPreferido = uiState.value.estadoRadio.modoTransporte,
                salidaManaguaDisponible = uiState.value.estadoPasarela.salidaHaciaManaguaDisponible
            )
            meshTransport.registrarTramaEnviada(106)
            _ultimoBanner.value = res.solicitudActualizada.notaMotivo
        }
    }

    fun forzarVencimientoPlazoSimulacro(detalle: SolicitudConDetalle, vencer60s: Boolean) {
        viewModelScope.launch {
            val instActual = obtenerInstitucionParaNivel(detalle.solicitud.nivelActual)
            val sigNivel = detalle.solicitud.nivelActual.siguienteNivel()
            val instSig = sigNivel?.let { obtenerInstitucionParaNivel(it) }
            val futuroSimulado = System.currentTimeMillis() + if (vencer60s) 65_000L else 190_000L

            val detallePreparado = if (vencer60s) {
                detalle.copy(
                    vencimientoActual = detalle.vencimientoActual?.copy(acuseAplicacionRecibido = false)
                )
            } else {
                detalle.copy(
                    vencimientoActual = detalle.vencimientoActual?.copy(acuseAplicacionRecibido = true)
                )
            }

            val res = EscalationEngine.evaluarVencimientoPlazos(
                detalle = detallePreparado,
                institucionActual = instActual,
                institucionSiguienteNivel = instSig,
                ahoraEpochMs = futuroSimulado
            )
            if (res != null) {
                repository.persistirResultadoTransicion(
                    resultado = res,
                    keyIdEmisor = keyIdParaNivel(instActual.nivel),
                    canalPreferido = uiState.value.estadoRadio.modoTransporte
                )
                meshTransport.registrarTramaEnviada(104, esReintento = true)
                _ultimoBanner.value = res.solicitudActualizada.notaMotivo
            }
        }
    }

    fun actualizarSeguimiento(
        detalle: SolicitudConDetalle,
        nuevoEstado: EstadoSolicitud,
        motivo: String
    ) {
        if (motivo.isBlank()) {
            _ultimoBanner.value = "Por favor escribe el motivo antes de actualizar o cancelar la solicitud."
            return
        }
        viewModelScope.launch {
            val instActual = obtenerInstitucionParaNivel(detalle.solicitud.nivelActual)
            repository.actualizarSeguimientoYCierre(
                detalle = detalle,
                nuevoEstado = nuevoEstado,
                motivoObligatorio = motivo,
                autorNombre = instActual.responsableTurno,
                nivelAutor = instActual.nivel,
                institucionId = instActual.id
            )
            meshTransport.registrarTramaEnviada(98)
            _ultimoBanner.value = "Estado actualizado a '${nuevoEstado.etiqueta}' y notificado al punto de origen."
        }
    }

    fun probarReconciliacionTardiaYDuplicado(detalle: SolicitudConDetalle) {
        viewModelScope.launch {
            val ahora = System.currentTimeMillis()
            val dupRes = EscalationEngine.reconciliarMensajeTardioODuplicado(
                detalleLocal = detalle,
                revisionEntrante = detalle.solicitud.revision,
                estadoEntrante = detalle.solicitud.estadoGeneral,
                nivelEmisor = detalle.solicitud.nivelActual,
                autorEmisor = "Reintento automático de radio",
                motivoEntrante = "Reenvío automático en la red de radio",
                ahoraEpochMs = ahora
            )
            repository.persistirResultadoTransicion(
                resultado = dupRes,
                keyIdEmisor = 101,
                canalPreferido = uiState.value.estadoRadio.modoTransporte
            )

            val revTardia = (detalle.solicitud.revision - 1).coerceAtLeast(1)
            val tardioRes = EscalationEngine.reconciliarMensajeTardioODuplicado(
                detalleLocal = detalle,
                revisionEntrante = revTardia,
                estadoEntrante = EstadoSolicitud.ACEPTADO_PARCIAL,
                nivelEmisor = NivelTerritorial.COMARCA,
                autorEmisor = "Puesto Comarca Masachapa (Respuesta tardía)",
                motivoEntrante = "Respuesta recibida después de que el caso ya había pasado al siguiente nivel",
                ahoraEpochMs = ahora + 50
            )
            repository.persistirResultadoTransicion(
                resultado = tardioRes,
                keyIdEmisor = 102,
                canalPreferido = uiState.value.estadoRadio.modoTransporte
            )
            _ultimoBanner.value = "Mensaje repetido descartado y respuesta tardía anotada en el historial sin alterar la asignación actual."
        }
    }

    fun probarSeguridadHmacYRechazoModificado() {
        val framePrueba = NexoCompactFrame(
            tipoMensaje = TipoMensajeApp.ACEPTADO_PARCIAL,
            uuidEvento = UUID.randomUUID().toString(),
            idMensajeCorto = 777,
            revision = 2,
            institucionOrigenCodigo = "SRS-01",
            institucionDestinoCodigo = "DEP-01",
            nivelOrigen = NivelTerritorial.MUNICIPIO,
            nivelDestino = NivelTerritorial.CABECERA_DEPARTAMENTAL,
            timestampEpochSec = System.currentTimeMillis() / 1000L,
            calidadReloj = CalidadReloj.SINCRONIZADO_GNSS_RED,
            fuenteUbicacion = FuenteUbicacion.GNSS_TELEFONO,
            latitud = 11.7809,
            longitud = -86.5193,
            precisionMetros = 6,
            antiguedadUbicacionSeg = 5,
            referenciaSitioCompacta = "PR-MAS-ESC01",
            tipoNecesidad = TipoNecesidad.AGUA_Y_ALIMENTOS,
            cantidadSolicitada = 10,
            cantidadAceptadaTotal = 4,
            cantidadPendiente = 6,
            personasAfectadas = 30,
            urgencia = UrgenciaDeclarada.CRITICA_VITAL,
            estadoGeneral = EstadoSolicitud.ACEPTADO_PARCIAL,
            motivo = MotivoCapacidad.ESCALAMIENTO_PARCIAL_RESIDUAL,
            vencimientoRestanteSeg = 180,
            responsableORecursoCompacto = "Carlos Rivas",
            keyId = 201
        )
        val (bytesValidos, _) = NexoPacketCodec.encode(framePrueba)
        val copiaAdulterada = bytesValidos.clone()
        copiaAdulterada[24] = (copiaAdulterada[24].toInt() xor 0x5A).toByte()

        val resultadoSeguridad = try {
            NexoPacketCodec.decodeAndVerify(copiaAdulterada)
            "Alerta: Se aceptó un mensaje alterado"
        } catch (e: PacketAuthenticationException) {
            "Seguridad verificada: el sistema detectó y bloqueó un mensaje alterado automáticamente."
        }

        _bitacoraSeguridad.update { listOf(resultadoSeguridad) + it }
        _ultimoBanner.value = resultadoSeguridad
    }

    fun ejecutarPruebaCarga20Sos() {
        val res = meshTransport.ejecutarPruebaCarga20Sos(tamanoMuestraBytes = 108)
        _resultadoCarga.value = res
        _ultimoBanner.value = "Prueba completada: ${res.sosEntregadosEn120s} de ${res.sosEnviados} solicitudes entregadas en menos de 2 minutos."
    }

    fun agregarReporteVial(
        tramo: String,
        referencia: String,
        estadoRuta: EstadoRuta,
        observacion: String
    ) {
        viewModelScope.launch {
            val esAutoridad = uiState.value.rolActivo != RolActor.COMUNIDAD_BRIGADA
            repository.registrarReporteVial(
                tramoNombre = tramo,
                referenciaUbicacion = referencia,
                estadoRuta = estadoRuta,
                reportadoPor = uiState.value.rolActivo.titulo,
                verificadoPorAutoridad = esAutoridad,
                observacion = observacion
            )
            _ultimoBanner.value = "Reporte vial registrado en San Rafael del Sur: $tramo (${estadoRuta.etiqueta})"
        }
    }

    fun reabastecerOResolverConflictoReserva(reserva: Reserva, cantidadReservadaNueva: Int, marcarConflicto: Boolean) {
        viewModelScope.launch {
            repository.ajustarInventarioReserva(reserva, cantidadReservadaNueva, marcarConflicto)
            _ultimoBanner.value = "Inventario actualizado para ${reserva.codigoRecurso}."
        }
    }

    /**
     * Recorrido de Demostración del Rally (Anexo I) contextualizado en el corredor de SAN RAFAEL DEL SUR:
     * 1) Crear SOS por 10 kits en Comarca Masachapa (San Rafael del Sur)
     * 2) Recibir en Comarca Masachapa y declarar incapacidad -> escala a Alcaldía/COMUPRED San Rafael del Sur
     * 3) Recibir en Municipio San Rafael del Sur y aceptar parcialmente 4 de 10 -> escala 6 a Cabecera Departamental
     * 4) Cabecera Departamental escala las 6 restantes a Sede Central Managua
     * 5) Sede Central Managua acepta las 6 unidades, activa ruta óptima Waze evitando el derrumbe de El Salto y confirma ATENDIDO.
     */
    fun ejecutarSiguientePasoDemoRally() {
        viewModelScope.launch {
            val pasoActual = _pasoDemoRally.value
            val solicitudes = uiState.value.solicitudes
            val reservas = uiState.value.reservas

            when (pasoActual) {
                0 -> {
                    _rolActivo.value = RolActor.COMUNIDAD_BRIGADA
                    val creado = repository.crearSosAtomico(
                        tipoNecesidad = TipoNecesidad.AGUA_Y_ALIMENTOS,
                        cantidadSolicitada = 10,
                        urgencia = UrgenciaDeclarada.CRITICA_VITAL,
                        personasAfectadas = 42,
                        fuenteUbicacion = FuenteUbicacion.GNSS_TELEFONO,
                        latitud = 11.7809,
                        longitud = -86.5193,
                        precisionMetros = 6,
                        antiguedadSeg = 4,
                        referenciaSitio = "PR-MAS-ESC01 (Escuela Comarca Masachapa)",
                        descripcionAmpliadaLocal = "42 familias evacuadas en la Escuela de Masachapa por inundación costera; solicitan 10 kits de agua potable y alimentos.",
                        radioVinculadoActivo = uiState.value.estadoRadio.radioVinculado,
                        canalPreferido = uiState.value.estadoRadio.modoTransporte,
                        creadoPorNombre = "Brigada Costera Masachapa"
                    )
                    meshTransport.registrarTramaEnviada(creado.ultimoOutbox?.tamanoBytes ?: 108)
                    _uuidSosDemo.value = creado.solicitud.uuidEvento
                    _pasoDemoRally.value = 1
                    _ultimoBanner.value = "Paso 1/5: Solicitud #${creado.solicitud.uuidEvento.take(8)} creada en Comarca Masachapa por 10 kits."
                }

                1 -> {
                    val target = buscarSosDemo(solicitudes) ?: return@launch
                    _rolActivo.value = RolActor.RESPONSABLE_COMARCA
                    val instCom = obtenerInstitucionParaNivel(NivelTerritorial.COMARCA)
                    val instMun = obtenerInstitucionParaNivel(NivelTerritorial.MUNICIPIO)

                    val rec = EscalationEngine.aplicarRecibidoAutoridad(
                        detalle = target,
                        institucionReceptora = instCom,
                        operadorReceptor = instCom.responsableTurno,
                        ahoraEpochMs = System.currentTimeMillis()
                    )
                    repository.persistirResultadoTransicion(rec, 102, uiState.value.estadoRadio.modoTransporte)

                    val detRecibido = target.copy(
                        solicitud = rec.solicitudActualizada,
                        necesidad = rec.necesidadActualizada,
                        vencimientoActual = rec.nuevoVencimiento
                    )
                    val esc = EscalationEngine.declararCapacidadInsuficienteOEscalar(
                        detalle = detRecibido,
                        institucionActual = instCom,
                        institucionSiguienteNivel = instMun,
                        motivo = MotivoCapacidad.FALTA_SUMINISTROS,
                        observacionAdicional = "Puesto Masachapa sin suministros disponibles; se solicita apoyo a la Alcaldía de San Rafael del Sur.",
                        autorNombre = instCom.responsableTurno,
                        ahoraEpochMs = System.currentTimeMillis() + 100
                    )
                    repository.persistirResultadoTransicion(esc, 102, uiState.value.estadoRadio.modoTransporte)
                    meshTransport.registrarTramaEnviada(106)
                    _pasoDemoRally.value = 2
                    _ultimoBanner.value = "Paso 2/5: Comarca Masachapa sin suministros. La solicitud pasa a San Rafael del Sur."
                }

                2 -> {
                    val target = buscarSosDemo(solicitudes) ?: return@launch
                    _rolActivo.value = RolActor.COORDINACION_MUNICIPAL
                    val instMun = obtenerInstitucionParaNivel(NivelTerritorial.MUNICIPIO)
                    val instDep = obtenerInstitucionParaNivel(NivelTerritorial.CABECERA_DEPARTAMENTAL)
                    val reservaMun = reservas.firstOrNull {
                        it.nivel == NivelTerritorial.MUNICIPIO && it.tipoNecesidadCubierta == TipoNecesidad.AGUA_Y_ALIMENTOS
                    } ?: reservas.first { it.nivel == NivelTerritorial.MUNICIPIO }

                    val reservaLista = if (reservaMun.cantidadDisponible < 4) {
                        reservaMun.copy(cantidadReservadaLocal = 0)
                    } else {
                        reservaMun
                    }

                    val parc = EscalationEngine.aplicarAceptacionRecursos(
                        detalle = target,
                        institucionActual = instMun,
                        institucionSiguienteNivel = instDep,
                        reservaSeleccionada = reservaLista,
                        cantidadAceptar = 4,
                        responsableSeguimiento = instMun.responsableTurno,
                        ahoraEpochMs = System.currentTimeMillis()
                    )
                    repository.persistirResultadoTransicion(parc, 201, uiState.value.estadoRadio.modoTransporte)
                    meshTransport.registrarTramaEnviada(112)
                    _nodoOrigenWaze.value = "NODO_SAN_RAFAEL"
                    _nodoDestinoWaze.value = "NODO_MASACHAPA"
                    _progresoBrigadaWaze.value = 0.50f
                    _pasoDemoRally.value = 3
                    _ultimoBanner.value = "Paso 3/5: San Rafael del Sur aporta 4 de 10 kits y solicita los 6 restantes a la Cabecera Departamental."
                }

                3 -> {
                    val target = buscarSosDemo(solicitudes) ?: return@launch
                    _rolActivo.value = RolActor.COORDINACION_DEPARTAMENTAL
                    val instDep = obtenerInstitucionParaNivel(NivelTerritorial.CABECERA_DEPARTAMENTAL)
                    val instMga = obtenerInstitucionParaNivel(NivelTerritorial.SEDE_CENTRAL_MANAGUA)

                    val escDep = EscalationEngine.declararCapacidadInsuficienteOEscalar(
                        detalle = target,
                        institucionActual = instDep,
                        institucionSiguienteNivel = instMga,
                        motivo = MotivoCapacidad.FALTA_TRANSPORTE,
                        observacionAdicional = "Derrumbe en El Salto; se solicita despacho desde Managua por ruta alterna (Santa Rita → San Bartolo).",
                        autorNombre = instDep.responsableTurno,
                        ahoraEpochMs = System.currentTimeMillis()
                    )
                    repository.persistirResultadoTransicion(
                        resultado = escDep,
                        keyIdEmisor = 301,
                        canalPreferido = CanalTransporte.PASARELA_IP_TERRESTRE,
                        salidaManaguaDisponible = uiState.value.estadoPasarela.salidaHaciaManaguaDisponible
                    )
                    meshTransport.registrarTramaEnviada(110)
                    _nodoOrigenWaze.value = "NODO_MANAGUA"
                    _nodoDestinoWaze.value = "NODO_MASACHAPA"
                    _progresoBrigadaWaze.value = 0.75f
                    _pasoDemoRally.value = 4
                    _ultimoBanner.value = "Paso 4/5: Cabecera Departamental traslada el pedido de los 6 kits restantes a la Sede Central en Managua."
                }

                else -> {
                    val target = buscarSosDemo(solicitudes) ?: return@launch
                    _rolActivo.value = RolActor.SEDE_CENTRAL_MANAGUA
                    val instMga = obtenerInstitucionParaNivel(NivelTerritorial.SEDE_CENTRAL_MANAGUA)
                    val reservaMga = reservas.firstOrNull {
                        it.nivel == NivelTerritorial.SEDE_CENTRAL_MANAGUA && it.tipoNecesidadCubierta == TipoNecesidad.AGUA_Y_ALIMENTOS
                    } ?: reservas.last()

                    val pendienteActual = target.necesidad.cantidadPendiente.coerceAtLeast(1)
                    val aceptacionFinal = EscalationEngine.aplicarAceptacionRecursos(
                        detalle = target,
                        institucionActual = instMga,
                        institucionSiguienteNivel = null,
                        reservaSeleccionada = reservaMga,
                        cantidadAceptar = pendienteActual,
                        responsableSeguimiento = instMga.responsableTurno,
                        ahoraEpochMs = System.currentTimeMillis()
                    )
                    repository.persistirResultadoTransicion(aceptacionFinal, 401, CanalTransporte.PASARELA_IP_TERRESTRE)

                    val detAceptado = target.copy(
                        solicitud = aceptacionFinal.solicitudActualizada,
                        necesidad = aceptacionFinal.necesidadActualizada
                    )
                    repository.actualizarSeguimientoYCierre(
                        detalle = detAceptado,
                        nuevoEstado = EstadoSolicitud.ATENDIDO,
                        motivoObligatorio = "Entrega completada en Masachapa: 4 kits municipales + 6 kits centrales recibidos por ruta óptima Waze (10/10 cubiertos).",
                        autorNombre = instMga.responsableTurno,
                        nivelAutor = NivelTerritorial.SEDE_CENTRAL_MANAGUA,
                        institucionId = instMga.id
                    )
                    meshTransport.registrarTramaRecibida(102)
                    _progresoBrigadaWaze.value = 1.0f
                    _pasoDemoRally.value = 0
                    _ultimoBanner.value = "Paso 5/5 completado: Managua despachó los 6 kits restantes (10/10) y la brigada llegó a Masachapa por la mejor ruta."
                }
            }
        }
    }

    private fun buscarSosDemo(solicitudes: List<SolicitudConDetalle>): SolicitudConDetalle? {
        val uuid = _uuidSosDemo.value
        return solicitudes.firstOrNull { it.solicitud.uuidEvento == uuid } ?: solicitudes.firstOrNull()
    }

    private fun obtenerInstitucionParaNivel(nivel: NivelTerritorial): Institucion {
        return uiState.value.instituciones.firstOrNull { it.nivel == nivel }
            ?: Institucion(
                id = "INST-${nivel.codigoCorto}",
                nombre = "Coordinación ${nivel.etiqueta}",
                nivel = nivel,
                territorioId = "TER-${nivel.codigoCorto}",
                responsableTurno = "Responsable ${nivel.etiqueta}",
                ciudad = nivel.etiqueta,
                codigoFuncion = "ROL-${nivel.codigoCorto}"
            )
    }

    private fun keyIdParaNivel(nivel: NivelTerritorial): Int = when (nivel) {
        NivelTerritorial.COMARCA -> 102
        NivelTerritorial.MUNICIPIO -> 201
        NivelTerritorial.CABECERA_DEPARTAMENTAL -> 301
        NivelTerritorial.SEDE_CENTRAL_MANAGUA -> 401
    }
}
