package ni.edu.rally.nexosos.datalocal.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ni.edu.rally.nexosos.datalocal.db.*
import ni.edu.rally.nexosos.domain.engine.EscalationEngine
import ni.edu.rally.nexosos.domain.model.*
import ni.edu.rally.nexosos.protocol.NexoPacketCodec
import java.util.UUID

class NexoLocalRepository(private val dao: NexoDao) {

    fun observeInstituciones(): Flow<List<Institucion>> =
        dao.observeInstituciones().map { list -> list.map { it.toDomain() } }

    fun observeReservas(): Flow<List<Reserva>> =
        dao.observeReservas().map { list -> list.map { it.toDomain() } }

    fun observeOutbox(): Flow<List<Outbox>> =
        dao.observeOutbox().map { list -> list.map { it.toDomain() } }

    fun observeReportesViales(): Flow<List<ReporteVial>> =
        dao.observeReportesViales().map { list -> list.map { it.toDomain() } }

    fun observeCentrosAtencion(): Flow<List<CentroAtencion>> =
        dao.observeCentrosAtencion().map { list -> list.map { it.toDomain() } }

    fun observeSolicitudesConDetalle(): Flow<List<SolicitudConDetalle>> {
        val baseFlow = combine(
            dao.observeSolicitudes(),
            dao.observeNecesidades(),
            dao.observeEventosEstado()
        ) { sols, necs, evts ->
            Triple(sols, necs, evts)
        }
        val extraFlow = combine(
            dao.observeAsignaciones(),
            dao.observeVencimientos(),
            dao.observeOutbox()
        ) { asigs, vencs, outboxes ->
            Triple(asigs, vencs, outboxes)
        }

        return combine(baseFlow, extraFlow) { (sols, necs, evts), (asigs, vencs, outboxes) ->
            val necByUuid = necs.associate { it.uuidEvento to it.toDomain() }
            val evtsByUuid = evts.map { it.toDomain() }.groupBy { it.uuidEvento }
            val asigsByUuid = asigs.map { it.toDomain() }.groupBy { it.uuidEvento }
            val vencsByUuid = vencs.map { it.toDomain() }.groupBy { it.uuidEvento }
            val outboxByUuid = outboxes.map { it.toDomain() }.groupBy { it.uuidEvento }

            sols.mapNotNull { solEnt ->
                val sol = solEnt.toDomain()
                val nec = necByUuid[sol.uuidEvento] ?: return@mapNotNull null
                val vencActual = vencsByUuid[sol.uuidEvento]
                    ?.firstOrNull { it.nivel == sol.nivelActual && !it.decisionTomada }
                    ?: vencsByUuid[sol.uuidEvento]?.lastOrNull()
                val ultimoOut = outboxByUuid[sol.uuidEvento]?.firstOrNull()

                SolicitudConDetalle(
                    solicitud = sol,
                    necesidad = nec,
                    historial = evtsByUuid[sol.uuidEvento].orEmpty(),
                    asignaciones = asigsByUuid[sol.uuidEvento].orEmpty(),
                    vencimientoActual = vencActual,
                    ultimoOutbox = ultimoOut
                )
            }
        }
    }

    /**
     * Inicializa el directorio territorial pre-provisionado para el corredor de
     * SAN RAFAEL DEL SUR (Departamento de Managua, Nicaragua) con máximo 3 torres (T1, T2, T3)
     * y 4 niveles institucionales sin ciclos (Anexos A, D y H).
     */
    suspend fun ensureSeedData() {
        val existentes = dao.getInstituciones()
        if (existentes.isNotEmpty()) return

        val territorios = listOf(
            Territorio("TER-COM-01", "Comarca Masachapa / Pochomil (San Rafael del Sur)", NivelTerritorial.COMARCA, "TER-MUN-01", "T1"),
            Territorio("TER-MUN-01", "Municipio San Rafael del Sur (Casco Urbano)", NivelTerritorial.MUNICIPIO, "TER-DEP-01", "T2"),
            Territorio("TER-DEP-01", "Departamento de Managua (Cabecera / Enlace El Crucero)", NivelTerritorial.CABECERA_DEPARTAMENTAL, "TER-MGA-01", "T3"),
            Territorio("TER-MGA-01", "Sede Central Nacional (Managua)", NivelTerritorial.SEDE_CENTRAL_MANAGUA, null, null)
        )
        dao.insertTerritorios(territorios.map { TerritorioEntity.fromDomain(it) })

        val instituciones = listOf(
            Institucion(
                id = "INST-COM-01",
                nombre = "Puesto Comunitario Comarca Masachapa",
                nivel = NivelTerritorial.COMARCA,
                territorioId = "TER-COM-01",
                responsableTurno = "Coord. Comarcal María López (Masachapa)",
                ciudad = "San Rafael del Sur - Comarca Masachapa",
                codigoFuncion = "ROL-COMARCA-01"
            ),
            Institucion(
                id = "INST-MUN-01",
                nombre = "COMUPRED Alcaldía de San Rafael del Sur",
                nivel = NivelTerritorial.MUNICIPIO,
                territorioId = "TER-MUN-01",
                responsableTurno = "Ing. Carlos Rivas (Turno Municipal SRS)",
                ciudad = "San Rafael del Sur",
                codigoFuncion = "ROL-MUNICIPIO-02"
            ),
            Institucion(
                id = "INST-DEP-01",
                nombre = "CODEPRED Cabecera Departamental Managua",
                nivel = NivelTerritorial.CABECERA_DEPARTAMENTAL,
                territorioId = "TER-DEP-01",
                responsableTurno = "Dra. Elena Morales (Turno Depto. Managua)",
                ciudad = "Managua (Rol Departamental)",
                codigoFuncion = "ROL-DEPTO-03"
            ),
            Institucion(
                id = "INST-MGA-01",
                nombre = "Centro de Operaciones Central Managua (SINAPRED)",
                nivel = NivelTerritorial.SEDE_CENTRAL_MANAGUA,
                territorioId = "TER-MGA-01",
                responsableTurno = "Dir. Roberto Blandón (Sede Central)",
                ciudad = "Managua (Rol Sede Central)",
                codigoFuncion = "ROL-CENTRAL-04"
            )
        )
        dao.insertInstituciones(instituciones.map { InstitucionEntity.fromDomain(it) })

        val reservas = listOf(
            Reserva(
                id = "RES-COM-AGUA",
                codigoRecurso = "MAS-KIT-01",
                nombreRecurso = "Kits comunitarios de agua y raciones (Masachapa)",
                tipoNecesidadCubierta = TipoNecesidad.AGUA_Y_ALIMENTOS,
                institucionPropietariaId = "INST-COM-01",
                nivel = NivelTerritorial.COMARCA,
                cantidadTotalInventario = 2,
                cantidadReservadaLocal = 2,
                unidad = "kits",
                responsableInventario = "María López (Comarca Masachapa)"
            ),
            Reserva(
                id = "RES-MUN-AGUA",
                codigoRecurso = "SRS-KIT-04",
                nombreRecurso = "Kits municipales de agua potable (Alcaldía San Rafael del Sur)",
                tipoNecesidadCubierta = TipoNecesidad.AGUA_Y_ALIMENTOS,
                institucionPropietariaId = "INST-MUN-01",
                nivel = NivelTerritorial.MUNICIPIO,
                cantidadTotalInventario = 4, // 4 disponibles para demostrar caso Anexo J (acepta 4 de 10 y escala 6)
                cantidadReservadaLocal = 0,
                unidad = "kits",
                responsableInventario = "Carlos Rivas (San Rafael del Sur)"
            ),
            Reserva(
                id = "RES-MUN-BRIGADA",
                codigoRecurso = "SRS-BRIG-02",
                nombreRecurso = "Brigada de rescate costero y camión municipal 4x4",
                tipoNecesidadCubierta = TipoNecesidad.RESCATE_BRIGADA,
                institucionPropietariaId = "INST-MUN-01",
                nivel = NivelTerritorial.MUNICIPIO,
                cantidadTotalInventario = 2,
                cantidadReservadaLocal = 0,
                unidad = "brigadas",
                responsableInventario = "Carlos Rivas (San Rafael del Sur)"
            ),
            Reserva(
                id = "RES-DEP-AGUA",
                codigoRecurso = "DEP-KIT-06",
                nombreRecurso = "Lote departamental de hidratación y alimentos",
                tipoNecesidadCubierta = TipoNecesidad.AGUA_Y_ALIMENTOS,
                institucionPropietariaId = "INST-DEP-01",
                nivel = NivelTerritorial.CABECERA_DEPARTAMENTAL,
                cantidadTotalInventario = 3,
                cantidadReservadaLocal = 0,
                unidad = "kits",
                responsableInventario = "Elena Morales (Departamento)"
            ),
            Reserva(
                id = "RES-DEP-MED",
                codigoRecurso = "DEP-MED-05",
                nombreRecurso = "Unidades móviles de trauma departamental",
                tipoNecesidadCubierta = TipoNecesidad.ATENCION_MEDICA_PRIMEROS_AUXILIOS,
                institucionPropietariaId = "INST-DEP-01",
                nivel = NivelTerritorial.CABECERA_DEPARTAMENTAL,
                cantidadTotalInventario = 4,
                cantidadReservadaLocal = 0,
                unidad = "unidades",
                responsableInventario = "Elena Morales (Departamento)"
            ),
            Reserva(
                id = "RES-MGA-AGUA",
                codigoRecurso = "MGA-KIT-20",
                nombreRecurso = "Reserva estratégica Sede Central Managua",
                tipoNecesidadCubierta = TipoNecesidad.AGUA_Y_ALIMENTOS,
                institucionPropietariaId = "INST-MGA-01",
                nivel = NivelTerritorial.SEDE_CENTRAL_MANAGUA,
                cantidadTotalInventario = 25,
                cantidadReservadaLocal = 0,
                unidad = "kits",
                responsableInventario = "Roberto Blandón (Sede Central)"
            ),
            Reserva(
                id = "RES-MGA-BRIG",
                codigoRecurso = "MGA-BRIG-10",
                nombreRecurso = "Brigadas pesadas de despeje y rescate central",
                tipoNecesidadCubierta = TipoNecesidad.RESCATE_BRIGADA,
                institucionPropietariaId = "INST-MGA-01",
                nivel = NivelTerritorial.SEDE_CENTRAL_MANAGUA,
                cantidadTotalInventario = 8,
                cantidadReservadaLocal = 0,
                unidad = "brigadas",
                responsableInventario = "Roberto Blandón (Sede Central)"
            )
        )
        dao.insertReservas(reservas.map { ReservaEntity.fromDomain(it) })

        val centros = listOf(
            CentroAtencion(
                id = "TORRE-T1",
                codigoSitio = "T1-MASACHAPA",
                nombre = "Torre 1 (Loma Costera Masachapa ~65 msnm - Nodo LoRa)",
                tipo = TipoPuntoMapa.TORRE_REPETIDORA_LORA,
                nivel = NivelTerritorial.COMARCA,
                latitud = 11.7865,
                longitud = -86.5140,
                capacidadDisponible = "Cobertura Comarcas Masachapa / Pochomil / Montelimar → T2",
                responsable = "Nodo Meshtastic !a1b201"
            ),
            CentroAtencion(
                id = "TORRE-T2",
                codigoSitio = "T2-SAN-RAFAEL",
                nombre = "Torre 2 (Cerro Casco Urbano San Rafael del Sur ~165 msnm)",
                tipo = TipoPuntoMapa.TORRE_REPETIDORA_LORA,
                nivel = NivelTerritorial.MUNICIPIO,
                latitud = 11.8465,
                longitud = -86.4389,
                capacidadDisponible = "Enlace Municipal T1 (Masachapa) ↔ T2 (San Rafael) ↔ T3 (El Crucero)",
                responsable = "Nodo Meshtastic !a1b202"
            ),
            CentroAtencion(
                id = "TORRE-T3",
                codigoSitio = "T3-EL-CRUCERO",
                nombre = "Torre 3 (Cumbre Las Nubes / El Crucero ~930 msnm - Pasarela a Managua)",
                tipo = TipoPuntoMapa.TORRE_REPETIDORA_LORA,
                nivel = NivelTerritorial.CABECERA_DEPARTAMENTAL,
                latitud = 11.9921,
                longitud = -86.3085,
                capacidadDisponible = "Línea de vista directa Costa Pacífico ↔ Cuenca de Managua (Máx 3 torres)",
                responsable = "Nodo Meshtastic !a1b203"
            ),
            CentroAtencion(
                id = "SITIO-REU-01",
                codigoSitio = "PR-MAS-ESC01",
                nombre = "Escuela Pública Masachapa (Punto Comunitario con Radio)",
                tipo = TipoPuntoMapa.PUNTO_REUNION_COMUNITARIO,
                nivel = NivelTerritorial.COMARCA,
                latitud = 11.7809,
                longitud = -86.5193,
                capacidadDisponible = "Zona segura elevada en Masachapa; capacidad 180 personas",
                responsable = "María López"
            ),
            CentroAtencion(
                id = "SITIO-REC-01",
                codigoSitio = "CR-SRS-SALUD",
                nombre = "Centro de Salud y Albergue Municipal San Rafael del Sur",
                tipo = TipoPuntoMapa.CENTRO_RECEPTOR_SALUD_ALBERGUE,
                nivel = NivelTerritorial.MUNICIPIO,
                latitud = 11.8465,
                longitud = -86.4389,
                capacidadDisponible = "24 camas disponibles / Albergue Polideportivo Municipal",
                responsable = "Dr. Jorge Somarriba"
            )
        )
        dao.insertCentrosAtencion(centros.map { CentroAtencionEntity.fromDomain(it) })

        val ahora = System.currentTimeMillis()
        val reportesViales = listOf(
            ReporteVial(
                id = "VIAL-01",
                tramoNombre = "NIC-2 Bajada de La Sierra (El Crucero ↔ Comarca El Salto)",
                referenciaUbicacion = "Derrumbe de talud en Km 34 antes de El Salto",
                latitud = 11.8920,
                longitud = -86.3880,
                estadoRuta = EstadoRuta.VERIFICADA_BLOQUEADA,
                reportadoPor = "COMUPRED San Rafael del Sur",
                verificadoPorAutoridad = true,
                observacion = "Paso cerrado en NIC-2 directa; Waze recalcula desvío óptimo por Carretera Vieja a León (Empalme Santa Rita → San Bartolo).",
                timestampEpochMs = ahora - 900_000L
            ),
            ReporteVial(
                id = "VIAL-02",
                tramoNombre = "NIC-12B Empalme Santa Rita ↔ San Bartolo ↔ San Rafael del Sur",
                referenciaUbicacion = "Desvío por Villa El Carmen / San Bartolo",
                latitud = 11.8840,
                longitud = -86.4450,
                estadoRuta = EstadoRuta.COMPROBADA_HABILITADA,
                reportadoPor = "COORDINACION_MUNICIPAL",
                verificadoPorAutoridad = true,
                observacion = "Ruta alterna comprobada y habilitada para camiones y ambulancias hacia San Rafael del Sur.",
                timestampEpochMs = ahora - 600_000L
            ),
            ReporteVial(
                id = "VIAL-03",
                tramoNombre = "Carretera San Rafael del Sur ↔ Comarca Masachapa / Pochomil",
                referenciaUbicacion = "Tramo costero Km 46 a Km 58 (Puente Río Masachapa)",
                latitud = 11.7809,
                longitud = -86.5193,
                estadoRuta = EstadoRuta.COMPROBADA_HABILITADA,
                reportadoPor = "Brigada Comarcal Masachapa",
                verificadoPorAutoridad = true,
                observacion = "Acceso principal habilitado; ruta alterna candidata disponible vía Montelimar.",
                timestampEpochMs = ahora - 300_000L
            )
        )
        dao.insertReportesViales(reportesViales.map { ReporteVialEntity.fromDomain(it) })
    }

    /**
     * Crea un nuevo SOS en una sola transacción SQLite/Room (Anexo D RF-02, Anexo F y Anexo G).
     * Prohibido mostrar ENVIADO_A_RADIO si radioConectado == false: se guarda como PENDIENTE_TRANSMISION.
     */
    suspend fun crearSosAtomico(
        tipoNecesidad: TipoNecesidad,
        cantidadSolicitada: Int,
        urgencia: UrgenciaDeclarada,
        personasAfectadas: Int,
        fuenteUbicacion: FuenteUbicacion,
        latitud: Double?,
        longitud: Double?,
        precisionMetros: Int?,
        antiguedadSeg: Int?,
        referenciaSitio: String,
        descripcionAmpliadaLocal: String,
        radioVinculadoActivo: Boolean,
        canalPreferido: CanalTransporte,
        creadoPorNombre: String = "Brigada / Punto Comunitario"
    ): SolicitudConDetalle {
        val ahora = System.currentTimeMillis()
        val uuid = UUID.randomUUID().toString()
        val estadoInicial = if (radioVinculadoActivo) {
            EstadoSolicitud.ENVIADO_A_RADIO
        } else {
            EstadoSolicitud.PENDIENTE_TRANSMISION
        }

        val solicitud = Solicitud(
            uuidEvento = uuid,
            revision = 1,
            institucionOrigenId = "INST-COM-01",
            institucionDestinoActualId = "INST-COM-01",
            nivelActual = NivelTerritorial.COMARCA,
            creadoPorUsuarioId = "USR-BRIG-01",
            fechaCreacionEpochMs = ahora,
            calidadReloj = CalidadReloj.SINCRONIZADO_GNSS_RED,
            latitud = latitud,
            longitud = longitud,
            precisionMetros = precisionMetros,
            antiguedadUbicacionSeg = antiguedadSeg,
            referenciaSitio = referenciaSitio.ifBlank { "PR-COM-ESC01 (Ref. comunitaria)" },
            fuenteUbicacion = fuenteUbicacion,
            personasAfectadas = personasAfectadas,
            urgencia = urgencia,
            estadoGeneral = estadoInicial,
            motivoUltimoCambio = MotivoCapacidad.NINGUNO,
            notaMotivo = if (radioVinculadoActivo) {
                "SOS registrado en base local y entregado a radio Meshtastic (esperando RECIBIDO_AUTORIDAD en 60s)"
            } else {
                "Teléfono sin radio vinculado: guardado en base local como PENDIENTE DE TRANSMISIÓN (no afirma llegada a autoridad)"
            },
            descripcionAmpliadaLocal = descripcionAmpliadaLocal
        )

        val necesidad = Necesidad(
            id = "NEC-$uuid",
            uuidEvento = uuid,
            tipo = tipoNecesidad,
            unidad = tipoNecesidad.unidadPorDefecto,
            cantidadSolicitada = cantidadSolicitada,
            cantidadAceptadaTotal = 0,
            cantidadPendiente = cantidadSolicitada,
            nivelResponsableActual = NivelTerritorial.COMARCA
        )

        val frame = NexoPacketCodec.fromSolicitud(
            solicitud = solicitud,
            necesidad = necesidad,
            tipoMensaje = TipoMensajeApp.SOS_ACTUALIZACION,
            nivelOrigen = NivelTerritorial.COMARCA,
            nivelDestino = NivelTerritorial.COMARCA,
            keyId = 101
        )
        val (bytes, _) = NexoPacketCodec.encode(frame)

        val outbox = Outbox(
            id = "OUT-$uuid-1",
            idMensajeLogico = "MSG-${uuid.take(8)}-R1",
            uuidEvento = uuid,
            revision = 1,
            tipoMensaje = TipoMensajeApp.SOS_ACTUALIZACION,
            nivelDestino = NivelTerritorial.COMARCA,
            payloadBinario = bytes,
            tamanoBytes = bytes.size,
            estadoOutbox = if (radioVinculadoActivo) {
                EstadoOutbox.TRANSMITIDO_ESPERANDO_ACUSE_APP
            } else {
                EstadoOutbox.PENDIENTE_SIN_RADIO
            },
            intentos = if (radioVinculadoActivo) 1 else 0,
            proximoIntentoEpochMs = ahora + 15_000L,
            canalPreferido = canalPreferido,
            creadoEpochMs = ahora
        )

        val vencimiento = Vencimiento(
            id = "VENC-$uuid-COM-1",
            uuidEvento = uuid,
            nivel = NivelTerritorial.COMARCA,
            primerEnvioNivelEpochMs = ahora,
            venceAcuse60sEpochMs = ahora + Vencimiento.PLAZO_ACUSE_SIMULACRO_MS,
            venceDecision180sEpochMs = ahora + Vencimiento.PLAZO_DECISION_SIMULACRO_MS,
            acuseAplicacionRecibido = false,
            decisionTomada = false,
            escaladoAutomaticoEjecutado = false
        )

        val evento = EventoEstado(
            id = "EVT-$uuid-1-CREACION",
            uuidEvento = uuid,
            revision = 1,
            estadoAnterior = null,
            estadoNuevo = estadoInicial,
            nivel = NivelTerritorial.COMARCA,
            institucionId = "INST-COM-01",
            autorId = "USR-BRIG-01",
            autorNombre = creadoPorNombre,
            motivo = "${solicitud.notaMotivo} [Trama PRIVATE_APP=256 de ${bytes.size} bytes <= 160B]",
            timestampEpochMs = ahora,
            calidadReloj = CalidadReloj.SINCRONIZADO_GNSS_RED
        )

        dao.guardarTransicionYOutboxAtomico(
            solicitud = SolicitudEntity.fromDomain(solicitud),
            necesidad = NecesidadEntity.fromDomain(necesidad),
            evento = EventoEstadoEntity.fromDomain(evento),
            outbox = OutboxEntity.fromDomain(outbox),
            vencimiento = VencimientoEntity.fromDomain(vencimiento)
        )

        return SolicitudConDetalle(
            solicitud = solicitud,
            necesidad = necesidad,
            historial = listOf(evento),
            asignaciones = emptyList(),
            vencimientoActual = vencimiento,
            ultimoOutbox = outbox
        )
    }

    /**
     * Persiste el resultado de una transición del EscalationEngine junto a su trama Outbox en una sola transacción.
     */
    suspend fun persistirResultadoTransicion(
        resultado: EscalationEngine.ResultadoTransicion,
        keyIdEmisor: Int,
        canalPreferido: CanalTransporte,
        salidaManaguaDisponible: Boolean = true
    ) {
        val sol = resultado.solicitudActualizada
        val nec = resultado.necesidadActualizada
        val ahora = System.currentTimeMillis()

        val outbox = resultado.tipoMensajeEmitir?.let { tipoMsg ->
            val frame = NexoPacketCodec.fromSolicitud(
                solicitud = sol,
                necesidad = nec,
                tipoMensaje = tipoMsg,
                nivelOrigen = resultado.nuevoEvento.nivel,
                nivelDestino = sol.nivelActual,
                keyId = keyIdEmisor,
                responsableCompacto = resultado.nuevaAsignacion?.responsableNombre ?: resultado.nuevoEvento.autorNombre
            )
            val (bytes, _) = NexoPacketCodec.encode(frame)
            val estadoOut = if (sol.nivelActual == NivelTerritorial.SEDE_CENTRAL_MANAGUA && !salidaManaguaDisponible) {
                EstadoOutbox.PENDIENTE_SALIDA_MANAGUA
            } else {
                EstadoOutbox.TRANSMITIDO_ESPERANDO_ACUSE_APP
            }
            Outbox(
                id = "OUT-${sol.uuidEvento}-${sol.revision}-$ahora",
                idMensajeLogico = "MSG-${sol.uuidEvento.take(8)}-R${sol.revision}",
                uuidEvento = sol.uuidEvento,
                revision = sol.revision,
                tipoMensaje = tipoMsg,
                nivelDestino = sol.nivelActual,
                payloadBinario = bytes,
                tamanoBytes = bytes.size,
                estadoOutbox = estadoOut,
                intentos = 1,
                proximoIntentoEpochMs = ahora + 20_000L,
                canalPreferido = canalPreferido,
                creadoEpochMs = ahora
            )
        }

        dao.guardarTransicionYOutboxAtomico(
            solicitud = SolicitudEntity.fromDomain(sol),
            necesidad = NecesidadEntity.fromDomain(nec),
            evento = EventoEstadoEntity.fromDomain(resultado.nuevoEvento),
            outbox = outbox?.let { OutboxEntity.fromDomain(it) },
            vencimiento = resultado.nuevoVencimiento?.let { VencimientoEntity.fromDomain(it) },
            asignacion = resultado.nuevaAsignacion?.let { AsignacionEntity.fromDomain(it) },
            reserva = resultado.reservaActualizada?.let { ReservaEntity.fromDomain(it) }
        )
    }

    /**
     * Cuando se vincula el radio tras haber creado solicitudes en modo "Teléfono sin radio",
     * transmite la cola pendiente sin recrear la emergencia ni cambiar su UUID (Anexo G y J).
     */
    suspend fun transmitirPendientesAlConectarRadio(
        solicitudes: List<SolicitudConDetalle>,
        canal: CanalTransporte
    ) {
        val ahora = System.currentTimeMillis()
        solicitudes
            .filter { it.solicitud.estadoGeneral == EstadoSolicitud.PENDIENTE_TRANSMISION }
            .forEach { det ->
                val solAct = det.solicitud.copy(
                    estadoGeneral = EstadoSolicitud.ENVIADO_A_RADIO,
                    notaMotivo = "Radio vinculado: transmitido desde cola Outbox conservando UUID ${det.solicitud.uuidEvento.take(8)}"
                )
                val outAct = det.ultimoOutbox?.let { prevOut ->
                    prevOut.copy(
                        estadoOutbox = EstadoOutbox.TRANSMITIDO_ESPERANDO_ACUSE_APP,
                        intentos = prevOut.intentos + 1,
                        canalPreferido = canal
                    )
                }
                val evt = EventoEstado(
                    id = "EVT-${solAct.uuidEvento}-R${solAct.revision}-TX-$ahora",
                    uuidEvento = solAct.uuidEvento,
                    revision = solAct.revision,
                    estadoAnterior = EstadoSolicitud.PENDIENTE_TRANSMISION,
                    estadoNuevo = EstadoSolicitud.ENVIADO_A_RADIO,
                    nivel = solAct.nivelActual,
                    institucionId = solAct.institucionDestinoActualId,
                    autorId = "OUTBOX-RADIO",
                    autorNombre = "Cola Outbox -> ${canal.etiqueta}",
                    motivo = "Enviado automáticamente al vincular radio sin recrear el SOS.",
                    timestampEpochMs = ahora,
                    calidadReloj = solAct.calidadReloj
                )
                val vencAct = det.vencimientoActual?.copy(
                    primerEnvioNivelEpochMs = ahora,
                    venceAcuse60sEpochMs = ahora + Vencimiento.PLAZO_ACUSE_SIMULACRO_MS,
                    venceDecision180sEpochMs = ahora + Vencimiento.PLAZO_DECISION_SIMULACRO_MS
                )
                dao.guardarTransicionYOutboxAtomico(
                    solicitud = SolicitudEntity.fromDomain(solAct),
                    necesidad = NecesidadEntity.fromDomain(det.necesidad),
                    evento = EventoEstadoEntity.fromDomain(evt),
                    outbox = outAct?.let { OutboxEntity.fromDomain(it) },
                    vencimiento = vencAct?.let { VencimientoEntity.fromDomain(it) }
                )
            }
    }

    /**
     * Actualiza el seguimiento operativo (RF-08: EN_CAMINO, EN_ATENCION, ATENDIDO o CANCELADO con motivo).
     */
    suspend fun actualizarSeguimientoYCierre(
        detalle: SolicitudConDetalle,
        nuevoEstado: EstadoSolicitud,
        motivoObligatorio: String,
        autorNombre: String,
        nivelAutor: NivelTerritorial,
        institucionId: String
    ) {
        val ahora = System.currentTimeMillis()
        val sol = detalle.solicitud
        val nuevaRev = sol.revision + 1
        val solAct = sol.copy(
            revision = nuevaRev,
            estadoGeneral = nuevoEstado,
            motivoUltimoCambio = if (nuevoEstado == EstadoSolicitud.CANCELADO) {
                MotivoCapacidad.CORRECCION_O_CANCELACION_OPERADOR
            } else {
                sol.motivoUltimoCambio
            },
            notaMotivo = motivoObligatorio
        )
        val evt = EventoEstado(
            id = "EVT-${sol.uuidEvento}-$nuevaRev-SEG-$ahora",
            uuidEvento = sol.uuidEvento,
            revision = nuevaRev,
            estadoAnterior = sol.estadoGeneral,
            estadoNuevo = nuevoEstado,
            nivel = nivelAutor,
            institucionId = institucionId,
            autorId = institucionId,
            autorNombre = "$autorNombre (${nivelAutor.etiqueta})",
            motivo = motivoObligatorio,
            timestampEpochMs = ahora,
            calidadReloj = sol.calidadReloj
        )
        val frame = NexoPacketCodec.fromSolicitud(
            solicitud = solAct,
            necesidad = detalle.necesidad,
            tipoMensaje = TipoMensajeApp.ESTADO_OPERATIVO,
            nivelOrigen = nivelAutor,
            nivelDestino = NivelTerritorial.COMARCA,
            keyId = 201,
            responsableCompacto = autorNombre
        )
        val (bytes, _) = NexoPacketCodec.encode(frame)
        val outbox = Outbox(
            id = "OUT-${sol.uuidEvento}-$nuevaRev-SEG",
            idMensajeLogico = "MSG-${sol.uuidEvento.take(8)}-R$nuevaRev",
            uuidEvento = sol.uuidEvento,
            revision = nuevaRev,
            tipoMensaje = TipoMensajeApp.ESTADO_OPERATIVO,
            nivelDestino = NivelTerritorial.COMARCA,
            payloadBinario = bytes,
            tamanoBytes = bytes.size,
            estadoOutbox = EstadoOutbox.ENTREGADO_CONFIRMADO,
            intentos = 1,
            proximoIntentoEpochMs = ahora,
            canalPreferido = CanalTransporte.MALLA_SIMULADA_RALLY,
            creadoEpochMs = ahora
        )
        dao.guardarTransicionYOutboxAtomico(
            solicitud = SolicitudEntity.fromDomain(solAct),
            necesidad = NecesidadEntity.fromDomain(detalle.necesidad),
            evento = EventoEstadoEntity.fromDomain(evt),
            outbox = OutboxEntity.fromDomain(outbox),
            vencimiento = detalle.vencimientoActual?.copy(decisionTomada = true)?.let { VencimientoEntity.fromDomain(it) }
        )
    }

    suspend fun registrarReporteVial(
        tramoNombre: String,
        referenciaUbicacion: String,
        estadoRuta: EstadoRuta,
        reportadoPor: String,
        verificadoPorAutoridad: Boolean,
        observacion: String
    ) {
        val ahora = System.currentTimeMillis()
        val reporte = ReporteVial(
            id = "VIAL-${UUID.randomUUID().toString().take(8)}",
            tramoNombre = tramoNombre,
            referenciaUbicacion = referenciaUbicacion,
            latitud = 11.9860,
            longitud = -86.3100,
            estadoRuta = estadoRuta,
            reportadoPor = reportadoPor,
            verificadoPorAutoridad = verificadoPorAutoridad,
            observacion = observacion,
            timestampEpochMs = ahora
        )
        dao.upsertReporteVial(ReporteVialEntity.fromDomain(reporte))
    }

    suspend fun ajustarInventarioReserva(reserva: Reserva, nuevaCantidadReservada: Int, enConflicto: Boolean = false) {
        dao.upsertReserva(
            ReservaEntity.fromDomain(
                reserva.copy(
                    cantidadReservadaLocal = nuevaCantidadReservada.coerceIn(0, reserva.cantidadTotalInventario),
                    enConflictoParticion = enConflicto
                )
            )
        )
    }

    /**
     * Procesa un paquete binario SOS recibido desde una Torre repetidora (T1 Masachapa, T2 San Rafael del Sur o T3 El Crucero):
     * 1) Verifica autenticidad e integridad criptográfica (HMAC-SHA256 de 16 bytes) y decodifica la trama.
     * 2) Si es un SOS nuevo proveniente de una comunidad o brigada, lo persiste en SQLite/Room, activa el reloj
     *    de recepción (60s) y decisión (180s) en la bandeja del nivel destino, y registra qué torre lo entregó.
     */
    suspend fun recibirTramaSosDesdeTorre(
        payloadBinario: ByteArray,
        torreNombre: String,
        nodoLoRaId: String,
        rssiDbm: Int,
        snrDb: Double,
        descripcionLocalRecibida: String,
        canalRecepcion: CanalTransporte
    ): SolicitudConDetalle {
        val frame = NexoPacketCodec.decodeAndVerify(payloadBinario)
        val ahora = System.currentTimeMillis()
        val instDestinoId = when (frame.nivelDestino) {
            NivelTerritorial.COMARCA -> "INST-COM-01"
            NivelTerritorial.MUNICIPIO -> "INST-MUN-01"
            NivelTerritorial.CABECERA_DEPARTAMENTAL -> "INST-DEP-01"
            NivelTerritorial.SEDE_CENTRAL_MANAGUA -> "INST-MGA-01"
        }

        val notaRecepcion = "Recibido desde $torreNombre ($nodoLoRaId · Señal $rssiDbm dBm / SNR $snrDb dB). Pendiente confirmar recepción."

        val solicitud = Solicitud(
            uuidEvento = frame.uuidEvento,
            revision = frame.revision,
            institucionOrigenId = "INST-COM-01",
            institucionDestinoActualId = instDestinoId,
            nivelActual = frame.nivelDestino,
            creadoPorUsuarioId = "TORRE-$nodoLoRaId",
            fechaCreacionEpochMs = frame.timestampEpochSec * 1000L,
            calidadReloj = frame.calidadReloj,
            latitud = frame.latitud,
            longitud = frame.longitud,
            precisionMetros = frame.precisionMetros,
            antiguedadUbicacionSeg = frame.antiguedadUbicacionSeg,
            referenciaSitio = frame.referenciaSitioCompacta,
            fuenteUbicacion = frame.fuenteUbicacion,
            personasAfectadas = frame.personasAfectadas,
            urgencia = frame.urgencia,
            estadoGeneral = frame.estadoGeneral,
            motivoUltimoCambio = frame.motivo,
            notaMotivo = notaRecepcion,
            descripcionAmpliadaLocal = descripcionLocalRecibida
        )

        val necesidad = Necesidad(
            id = "NEC-${frame.uuidEvento}",
            uuidEvento = frame.uuidEvento,
            tipo = frame.tipoNecesidad,
            unidad = frame.tipoNecesidad.unidadPorDefecto,
            cantidadSolicitada = frame.cantidadSolicitada,
            cantidadAceptadaTotal = frame.cantidadAceptadaTotal,
            cantidadPendiente = frame.cantidadPendiente,
            nivelResponsableActual = frame.nivelDestino
        )

        val vencimiento = Vencimiento(
            id = "VENC-${frame.uuidEvento}-${frame.nivelDestino.codigoCorto}-${frame.revision}",
            uuidEvento = frame.uuidEvento,
            nivel = frame.nivelDestino,
            primerEnvioNivelEpochMs = ahora,
            venceAcuse60sEpochMs = ahora + Vencimiento.PLAZO_ACUSE_SIMULACRO_MS,
            venceDecision180sEpochMs = ahora + Vencimiento.PLAZO_DECISION_SIMULACRO_MS,
            acuseAplicacionRecibido = false,
            decisionTomada = false,
            escaladoAutomaticoEjecutado = false
        )

        val evento = EventoEstado(
            id = "EVT-${frame.uuidEvento}-R${frame.revision}-RX-$ahora",
            uuidEvento = frame.uuidEvento,
            revision = frame.revision,
            estadoAnterior = null,
            estadoNuevo = frame.estadoGeneral,
            nivel = frame.nivelDestino,
            institucionId = instDestinoId,
            autorId = nodoLoRaId,
            autorNombre = "$torreNombre (${frame.responsableORecursoCompacto})",
            motivo = notaRecepcion,
            timestampEpochMs = ahora,
            calidadReloj = frame.calidadReloj
        )

        val outboxRx = Outbox(
            id = "IN-${frame.uuidEvento}-${frame.revision}-$ahora",
            idMensajeLogico = "RX-${frame.uuidEvento.take(8)}-R${frame.revision}",
            uuidEvento = frame.uuidEvento,
            revision = frame.revision,
            tipoMensaje = frame.tipoMensaje,
            nivelDestino = frame.nivelDestino,
            payloadBinario = payloadBinario,
            tamanoBytes = payloadBinario.size,
            estadoOutbox = EstadoOutbox.ENTREGADO_CONFIRMADO,
            intentos = 1,
            proximoIntentoEpochMs = ahora,
            canalPreferido = canalRecepcion,
            creadoEpochMs = ahora
        )

        dao.guardarTransicionYOutboxAtomico(
            solicitud = SolicitudEntity.fromDomain(solicitud),
            necesidad = NecesidadEntity.fromDomain(necesidad),
            evento = EventoEstadoEntity.fromDomain(evento),
            outbox = OutboxEntity.fromDomain(outboxRx),
            vencimiento = VencimientoEntity.fromDomain(vencimiento)
        )

        return SolicitudConDetalle(
            solicitud = solicitud,
            necesidad = necesidad,
            historial = listOf(evento),
            asignaciones = emptyList(),
            vencimientoActual = vencimiento,
            ultimoOutbox = outboxRx
        )
    }
}
