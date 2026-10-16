package ni.edu.rally.nexosos.datalocal.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import ni.edu.rally.nexosos.domain.model.*

@Entity(tableName = "instituciones")
data class InstitucionEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val nivelOrden: Int,
    val territorioId: String,
    val responsableTurno: String,
    val ciudad: String,
    val codigoFuncion: String
) {
    fun toDomain() = Institucion(
        id = id,
        nombre = nombre,
        nivel = NivelTerritorial.fromOrden(nivelOrden),
        territorioId = territorioId,
        responsableTurno = responsableTurno,
        ciudad = ciudad,
        codigoFuncion = codigoFuncion
    )

    companion object {
        fun fromDomain(d: Institucion) = InstitucionEntity(
            id = d.id,
            nombre = d.nombre,
            nivelOrden = d.nivel.orden,
            territorioId = d.territorioId,
            responsableTurno = d.responsableTurno,
            ciudad = d.ciudad,
            codigoFuncion = d.codigoFuncion
        )
    }
}

@Entity(tableName = "territorios")
data class TerritorioEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val nivelOrden: Int,
    val padreId: String?,
    val torreCercanaId: String?
) {
    fun toDomain() = Territorio(
        id = id,
        nombre = nombre,
        nivel = NivelTerritorial.fromOrden(nivelOrden),
        padreId = padreId,
        torreCercanaId = torreCercanaId
    )

    companion object {
        fun fromDomain(d: Territorio) = TerritorioEntity(
            id = d.id,
            nombre = d.nombre,
            nivelOrden = d.nivel.orden,
            padreId = d.padreId,
            torreCercanaId = d.torreCercanaId
        )
    }
}

@Entity(tableName = "solicitudes")
data class SolicitudEntity(
    @PrimaryKey val uuidEvento: String,
    val revision: Int,
    val institucionOrigenId: String,
    val institucionDestinoActualId: String,
    val nivelActualOrden: Int,
    val creadoPorUsuarioId: String,
    val fechaCreacionEpochMs: Long,
    val calidadRelojCodigo: Int,
    val latitud: Double?,
    val longitud: Double?,
    val precisionMetros: Int?,
    val antiguedadUbicacionSeg: Int?,
    val referenciaSitio: String,
    val fuenteUbicacionCodigo: Int,
    val personasAfectadas: Int,
    val urgenciaCodigo: Int,
    val estadoGeneralName: String,
    val motivoUltimoCambioCodigo: Int,
    val notaMotivo: String,
    val descripcionAmpliadaLocal: String,
    val requiereConciliacionHumana: Boolean
) {
    fun toDomain() = Solicitud(
        uuidEvento = uuidEvento,
        revision = revision,
        institucionOrigenId = institucionOrigenId,
        institucionDestinoActualId = institucionDestinoActualId,
        nivelActual = NivelTerritorial.fromOrden(nivelActualOrden),
        creadoPorUsuarioId = creadoPorUsuarioId,
        fechaCreacionEpochMs = fechaCreacionEpochMs,
        calidadReloj = CalidadReloj.fromCodigo(calidadRelojCodigo),
        latitud = latitud,
        longitud = longitud,
        precisionMetros = precisionMetros,
        antiguedadUbicacionSeg = antiguedadUbicacionSeg,
        referenciaSitio = referenciaSitio,
        fuenteUbicacion = FuenteUbicacion.fromCodigo(fuenteUbicacionCodigo),
        personasAfectadas = personasAfectadas,
        urgencia = UrgenciaDeclarada.fromCodigo(urgenciaCodigo),
        estadoGeneral = runCatching { EstadoSolicitud.valueOf(estadoGeneralName) }
            .getOrDefault(EstadoSolicitud.PENDIENTE_TRANSMISION),
        motivoUltimoCambio = MotivoCapacidad.fromCodigo(motivoUltimoCambioCodigo),
        notaMotivo = notaMotivo,
        descripcionAmpliadaLocal = descripcionAmpliadaLocal,
        requiereConciliacionHumana = requiereConciliacionHumana
    )

    companion object {
        fun fromDomain(d: Solicitud) = SolicitudEntity(
            uuidEvento = d.uuidEvento,
            revision = d.revision,
            institucionOrigenId = d.institucionOrigenId,
            institucionDestinoActualId = d.institucionDestinoActualId,
            nivelActualOrden = d.nivelActual.orden,
            creadoPorUsuarioId = d.creadoPorUsuarioId,
            fechaCreacionEpochMs = d.fechaCreacionEpochMs,
            calidadRelojCodigo = d.calidadReloj.codigo,
            latitud = d.latitud,
            longitud = d.longitud,
            precisionMetros = d.precisionMetros,
            antiguedadUbicacionSeg = d.antiguedadUbicacionSeg,
            referenciaSitio = d.referenciaSitio,
            fuenteUbicacionCodigo = d.fuenteUbicacion.codigo,
            personasAfectadas = d.personasAfectadas,
            urgenciaCodigo = d.urgencia.codigo,
            estadoGeneralName = d.estadoGeneral.name,
            motivoUltimoCambioCodigo = d.motivoUltimoCambio.codigo,
            notaMotivo = d.notaMotivo,
            descripcionAmpliadaLocal = d.descripcionAmpliadaLocal,
            requiereConciliacionHumana = d.requiereConciliacionHumana
        )
    }
}

@Entity(tableName = "necesidades")
data class NecesidadEntity(
    @PrimaryKey val id: String,
    val uuidEvento: String,
    val tipoCodigo: Int,
    val unidad: String,
    val cantidadSolicitada: Int,
    val cantidadAceptadaTotal: Int,
    val cantidadPendiente: Int,
    val nivelResponsableActualOrden: Int
) {
    fun toDomain() = Necesidad(
        id = id,
        uuidEvento = uuidEvento,
        tipo = TipoNecesidad.fromCodigo(tipoCodigo),
        unidad = unidad,
        cantidadSolicitada = cantidadSolicitada,
        cantidadAceptadaTotal = cantidadAceptadaTotal,
        cantidadPendiente = cantidadPendiente,
        nivelResponsableActual = NivelTerritorial.fromOrden(nivelResponsableActualOrden)
    )

    companion object {
        fun fromDomain(d: Necesidad) = NecesidadEntity(
            id = d.id,
            uuidEvento = d.uuidEvento,
            tipoCodigo = d.tipo.codigo,
            unidad = d.unidad,
            cantidadSolicitada = d.cantidadSolicitada,
            cantidadAceptadaTotal = d.cantidadAceptadaTotal,
            cantidadPendiente = d.cantidadPendiente,
            nivelResponsableActualOrden = d.nivelResponsableActual.orden
        )
    }
}

@Entity(tableName = "eventos_estado")
data class EventoEstadoEntity(
    @PrimaryKey val id: String,
    val uuidEvento: String,
    val revision: Int,
    val estadoAnteriorName: String?,
    val estadoNuevoName: String,
    val nivelOrden: Int,
    val institucionId: String,
    val autorId: String,
    val autorNombre: String,
    val motivo: String,
    val timestampEpochMs: Long,
    val calidadRelojCodigo: Int,
    val esTardioReconciliado: Boolean
) {
    fun toDomain() = EventoEstado(
        id = id,
        uuidEvento = uuidEvento,
        revision = revision,
        estadoAnterior = estadoAnteriorName?.let {
            runCatching { EstadoSolicitud.valueOf(it) }.getOrNull()
        },
        estadoNuevo = runCatching { EstadoSolicitud.valueOf(estadoNuevoName) }
            .getOrDefault(EstadoSolicitud.CREADO),
        nivel = NivelTerritorial.fromOrden(nivelOrden),
        institucionId = institucionId,
        autorId = autorId,
        autorNombre = autorNombre,
        motivo = motivo,
        timestampEpochMs = timestampEpochMs,
        calidadReloj = CalidadReloj.fromCodigo(calidadRelojCodigo),
        esTardioReconciliado = esTardioReconciliado
    )

    companion object {
        fun fromDomain(d: EventoEstado) = EventoEstadoEntity(
            id = d.id,
            uuidEvento = d.uuidEvento,
            revision = d.revision,
            estadoAnteriorName = d.estadoAnterior?.name,
            estadoNuevoName = d.estadoNuevo.name,
            nivelOrden = d.nivel.orden,
            institucionId = d.institucionId,
            autorId = d.autorId,
            autorNombre = d.autorNombre,
            motivo = d.motivo,
            timestampEpochMs = d.timestampEpochMs,
            calidadRelojCodigo = d.calidadReloj.codigo,
            esTardioReconciliado = d.esTardioReconciliado
        )
    }
}

@Entity(tableName = "asignaciones")
data class AsignacionEntity(
    @PrimaryKey val id: String,
    val uuidEvento: String,
    val necesidadId: String,
    val institucionId: String,
    val nivelOrden: Int,
    val recursoId: String,
    val recursoNombre: String,
    val cantidadComprometida: Int,
    val responsableNombre: String,
    val plazoSeguimientoEpochMs: Long,
    val estadoOperativoName: String,
    val revision: Int
) {
    fun toDomain() = Asignacion(
        id = id,
        uuidEvento = uuidEvento,
        necesidadId = necesidadId,
        institucionId = institucionId,
        nivel = NivelTerritorial.fromOrden(nivelOrden),
        recursoId = recursoId,
        recursoNombre = recursoNombre,
        cantidadComprometida = cantidadComprometida,
        responsableNombre = responsableNombre,
        plazoSeguimientoEpochMs = plazoSeguimientoEpochMs,
        estadoOperativo = runCatching { EstadoAsignacion.valueOf(estadoOperativoName) }
            .getOrDefault(EstadoAsignacion.RESERVADO),
        revision = revision
    )

    companion object {
        fun fromDomain(d: Asignacion) = AsignacionEntity(
            id = d.id,
            uuidEvento = d.uuidEvento,
            necesidadId = d.necesidadId,
            institucionId = d.institucionId,
            nivelOrden = d.nivel.orden,
            recursoId = d.recursoId,
            recursoNombre = d.recursoNombre,
            cantidadComprometida = d.cantidadComprometida,
            responsableNombre = d.responsableNombre,
            plazoSeguimientoEpochMs = d.plazoSeguimientoEpochMs,
            estadoOperativoName = d.estadoOperativo.name,
            revision = d.revision
        )
    }
}

@Entity(tableName = "reservas")
data class ReservaEntity(
    @PrimaryKey val id: String,
    val codigoRecurso: String,
    val nombreRecurso: String,
    val tipoNecesidadCodigo: Int,
    val institucionPropietariaId: String,
    val nivelOrden: Int,
    val cantidadTotalInventario: Int,
    val cantidadReservadaLocal: Int,
    val unidad: String,
    val responsableInventario: String,
    val enConflictoParticion: Boolean
) {
    fun toDomain() = Reserva(
        id = id,
        codigoRecurso = codigoRecurso,
        nombreRecurso = nombreRecurso,
        tipoNecesidadCubierta = TipoNecesidad.fromCodigo(tipoNecesidadCodigo),
        institucionPropietariaId = institucionPropietariaId,
        nivel = NivelTerritorial.fromOrden(nivelOrden),
        cantidadTotalInventario = cantidadTotalInventario,
        cantidadReservadaLocal = cantidadReservadaLocal,
        unidad = unidad,
        responsableInventario = responsableInventario,
        enConflictoParticion = enConflictoParticion
    )

    companion object {
        fun fromDomain(d: Reserva) = ReservaEntity(
            id = d.id,
            codigoRecurso = d.codigoRecurso,
            nombreRecurso = d.nombreRecurso,
            tipoNecesidadCodigo = d.tipoNecesidadCubierta.codigo,
            institucionPropietariaId = d.institucionPropietariaId,
            nivelOrden = d.nivel.orden,
            cantidadTotalInventario = d.cantidadTotalInventario,
            cantidadReservadaLocal = d.cantidadReservadaLocal,
            unidad = d.unidad,
            responsableInventario = d.responsableInventario,
            enConflictoParticion = d.enConflictoParticion
        )
    }
}

@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey val id: String,
    val idMensajeLogico: String,
    val uuidEvento: String,
    val revision: Int,
    val tipoMensajeCodigo: Int,
    val nivelDestinoOrden: Int,
    val payloadBinario: ByteArray,
    val tamanoBytes: Int,
    val estadoOutboxName: String,
    val intentos: Int,
    val proximoIntentoEpochMs: Long,
    val canalPreferidoName: String,
    val creadoEpochMs: Long
) {
    fun toDomain() = Outbox(
        id = id,
        idMensajeLogico = idMensajeLogico,
        uuidEvento = uuidEvento,
        revision = revision,
        tipoMensaje = TipoMensajeApp.fromCodigo(tipoMensajeCodigo),
        nivelDestino = NivelTerritorial.fromOrden(nivelDestinoOrden),
        payloadBinario = payloadBinario,
        tamanoBytes = tamanoBytes,
        estadoOutbox = runCatching { EstadoOutbox.valueOf(estadoOutboxName) }
            .getOrDefault(EstadoOutbox.PENDIENTE_SIN_RADIO),
        intentos = intentos,
        proximoIntentoEpochMs = proximoIntentoEpochMs,
        canalPreferido = runCatching { CanalTransporte.valueOf(canalPreferidoName) }
            .getOrDefault(CanalTransporte.MALLA_SIMULADA_RALLY),
        creadoEpochMs = creadoEpochMs
    )

    companion object {
        fun fromDomain(d: Outbox) = OutboxEntity(
            id = d.id,
            idMensajeLogico = d.idMensajeLogico,
            uuidEvento = d.uuidEvento,
            revision = d.revision,
            tipoMensajeCodigo = d.tipoMensaje.codigo,
            nivelDestinoOrden = d.nivelDestino.orden,
            payloadBinario = d.payloadBinario,
            tamanoBytes = d.tamanoBytes,
            estadoOutboxName = d.estadoOutbox.name,
            intentos = d.intentos,
            proximoIntentoEpochMs = d.proximoIntentoEpochMs,
            canalPreferidoName = d.canalPreferido.name,
            creadoEpochMs = d.creadoEpochMs
        )
    }
}

@Entity(tableName = "vencimientos")
data class VencimientoEntity(
    @PrimaryKey val id: String,
    val uuidEvento: String,
    val nivelOrden: Int,
    val primerEnvioNivelEpochMs: Long,
    val venceAcuse60sEpochMs: Long,
    val venceDecision180sEpochMs: Long,
    val acuseAplicacionRecibido: Boolean,
    val decisionTomada: Boolean,
    val escaladoAutomaticoEjecutado: Boolean
) {
    fun toDomain() = Vencimiento(
        id = id,
        uuidEvento = uuidEvento,
        nivel = NivelTerritorial.fromOrden(nivelOrden),
        primerEnvioNivelEpochMs = primerEnvioNivelEpochMs,
        venceAcuse60sEpochMs = venceAcuse60sEpochMs,
        venceDecision180sEpochMs = venceDecision180sEpochMs,
        acuseAplicacionRecibido = acuseAplicacionRecibido,
        decisionTomada = decisionTomada,
        escaladoAutomaticoEjecutado = escaladoAutomaticoEjecutado
    )

    companion object {
        fun fromDomain(d: Vencimiento) = VencimientoEntity(
            id = d.id,
            uuidEvento = d.uuidEvento,
            nivelOrden = d.nivel.orden,
            primerEnvioNivelEpochMs = d.primerEnvioNivelEpochMs,
            venceAcuse60sEpochMs = d.venceAcuse60sEpochMs,
            venceDecision180sEpochMs = d.venceDecision180sEpochMs,
            acuseAplicacionRecibido = d.acuseAplicacionRecibido,
            decisionTomada = d.decisionTomada,
            escaladoAutomaticoEjecutado = d.escaladoAutomaticoEjecutado
        )
    }
}

@Entity(tableName = "reportes_viales")
data class ReporteVialEntity(
    @PrimaryKey val id: String,
    val tramoNombre: String,
    val referenciaUbicacion: String,
    val latitud: Double,
    val longitud: Double,
    val estadoRutaName: String,
    val reportadoPor: String,
    val verificadoPorAutoridad: Boolean,
    val observacion: String,
    val timestampEpochMs: Long
) {
    fun toDomain() = ReporteVial(
        id = id,
        tramoNombre = tramoNombre,
        referenciaUbicacion = referenciaUbicacion,
        latitud = latitud,
        longitud = longitud,
        estadoRuta = runCatching { EstadoRuta.valueOf(estadoRutaName) }
            .getOrDefault(EstadoRuta.CANDIDATA_NO_VERIFICADA),
        reportadoPor = reportadoPor,
        verificadoPorAutoridad = verificadoPorAutoridad,
        observacion = observacion,
        timestampEpochMs = timestampEpochMs
    )

    companion object {
        fun fromDomain(d: ReporteVial) = ReporteVialEntity(
            id = d.id,
            tramoNombre = d.tramoNombre,
            referenciaUbicacion = d.referenciaUbicacion,
            latitud = d.latitud,
            longitud = d.longitud,
            estadoRutaName = d.estadoRuta.name,
            reportadoPor = d.reportadoPor,
            verificadoPorAutoridad = d.verificadoPorAutoridad,
            observacion = d.observacion,
            timestampEpochMs = d.timestampEpochMs
        )
    }
}

@Entity(tableName = "centros_atencion")
data class CentroAtencionEntity(
    @PrimaryKey val id: String,
    val codigoSitio: String,
    val nombre: String,
    val tipoName: String,
    val nivelOrden: Int,
    val latitud: Double,
    val longitud: Double,
    val capacidadDisponible: String,
    val responsable: String,
    val activo: Boolean
) {
    fun toDomain() = CentroAtencion(
        id = id,
        codigoSitio = codigoSitio,
        nombre = nombre,
        tipo = runCatching { TipoPuntoMapa.valueOf(tipoName) }
            .getOrDefault(TipoPuntoMapa.PUNTO_REUNION_COMUNITARIO),
        nivel = NivelTerritorial.fromOrden(nivelOrden),
        latitud = latitud,
        longitud = longitud,
        capacidadDisponible = capacidadDisponible,
        responsable = responsable,
        activo = activo
    )

    companion object {
        fun fromDomain(d: CentroAtencion) = CentroAtencionEntity(
            id = d.id,
            codigoSitio = d.codigoSitio,
            nombre = d.nombre,
            tipoName = d.tipo.name,
            nivelOrden = d.nivel.orden,
            latitud = d.latitud,
            longitud = d.longitud,
            capacidadDisponible = d.capacidadDisponible,
            responsable = d.responsable,
            activo = d.activo
        )
    }
}
