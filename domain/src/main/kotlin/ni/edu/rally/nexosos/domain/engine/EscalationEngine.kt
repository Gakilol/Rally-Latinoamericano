package ni.edu.rally.nexosos.domain.engine

import ni.edu.rally.nexosos.domain.model.*

/**
 * Motor determinista de escalamiento, capacidad y reconciliación (Anexos C, E, G y J).
 *
 * Reglas invariantes:
 * 1. Guardar un SOS sin radio vinculado deja el estado en PENDIENTE_TRANSMISION (nunca ENVIADO_A_RADIO).
 * 2. Un ACK técnico de radio LoRa NO equivale a RECIBIDO_AUTORIDAD ni reinicia ni detiene los plazos 60s/180s.
 * 3. RECIBIDO_AUTORIDAD marca acuse de aplicación (60s), pero NO reinicia el reloj de decisión (180s desde primer envío al nivel).
 * 4. La aceptación parcial reserva únicamente lo comprometido localmente y escala el pendiente residual al siguiente nivel
 *    conservando el mismo UUID de evento y aumentando la revisión.
 * 5. Si Managua tampoco puede atender, pasa a PENDIENTE_APOYO_CENTRAL (nunca se cierra por llegar al último nivel).
 * 6. Reintentos con la misma revisión son idempotentes; mensajes tardíos con revisión menor se registran en bitácora sin
 *    sobrescribir silenciosamente una decisión más reciente.
 */
object EscalationEngine {

    data class ResultadoTransicion(
        val solicitudActualizada: Solicitud,
        val necesidadActualizada: Necesidad,
        val nuevoEvento: EventoEstado,
        val nuevaAsignacion: Asignacion? = null,
        val reservaActualizada: Reserva? = null,
        val nuevoVencimiento: Vencimiento? = null,
        val tipoMensajeEmitir: TipoMensajeApp? = null,
        val esIdempotenteIgnorado: Boolean = false
    )

    /**
     * Registra un acuse técnico del radio (ACK de capa malla Meshtastic).
     * Anexo C y J: "Recibir un ACK de radio sin RECIBIDO_AUTORIDAD no cambia la interfaz a 'autoridad recibió'
     * ni detiene el plazo operativo."
     */
    fun aplicarAckTecnicoRadio(
        detalle: SolicitudConDetalle,
        nodoRadioId: String,
        ahoraEpochMs: Long
    ): ResultadoTransicion {
        val sol = detalle.solicitud
        val nuevoEstado = if (sol.estadoGeneral == EstadoSolicitud.CREADO ||
            sol.estadoGeneral == EstadoSolicitud.PENDIENTE_TRANSMISION ||
            sol.estadoGeneral == EstadoSolicitud.ENVIADO_A_RADIO
        ) {
            EstadoSolicitud.ACK_TECNICO_RADIO
        } else {
            sol.estadoGeneral
        }

        val solicitudAct = sol.copy(estadoGeneral = nuevoEstado)
        val evento = EventoEstado(
            id = "EVT-${sol.uuidEvento}-${sol.revision}-ACKRADIO-$ahoraEpochMs",
            uuidEvento = sol.uuidEvento,
            revision = sol.revision,
            estadoAnterior = sol.estadoGeneral,
            estadoNuevo = nuevoEstado,
            nivel = sol.nivelActual,
            institucionId = sol.institucionDestinoActualId,
            autorId = nodoRadioId,
            autorNombre = "Radio LoRa ($nodoRadioId)",
            motivo = "ACK técnico de malla LoRa (no confirma recepción de autoridad ni detiene plazo 60/180s)",
            timestampEpochMs = ahoraEpochMs,
            calidadReloj = sol.calidadReloj
        )
        // El vencimiento NO se modifica ni se detiene
        return ResultadoTransicion(
            solicitudActualizada = solicitudAct,
            necesidadActualizada = detalle.necesidad,
            nuevoEvento = evento,
            nuevoVencimiento = detalle.vencimientoActual,
            tipoMensajeEmitir = null
        )
    }

    /**
     * Registra el acuse de aplicación RECIBIDO_AUTORIDAD tras persistir en la estación/app receptora.
     * Detiene el plazo de 60s de recepción, pero NO reinicia el reloj de decisión de 180s.
     */
    fun aplicarRecibidoAutoridad(
        detalle: SolicitudConDetalle,
        institucionReceptora: Institucion,
        operadorReceptor: String,
        ahoraEpochMs: Long
    ): ResultadoTransicion {
        val sol = detalle.solicitud
        val nuevaRev = sol.revision + 1
        val solicitudAct = sol.copy(
            revision = nuevaRev,
            estadoGeneral = EstadoSolicitud.RECIBIDO_AUTORIDAD,
            notaMotivo = "Persistido por ${institucionReceptora.nombre} (${institucionReceptora.nivel.etiqueta})"
        )
        val vencimientoAct = detalle.vencimientoActual?.copy(
            acuseAplicacionRecibido = true
            // venceDecision180sEpochMs permanece intacto (no se reinicia el reloj de decisión)
        ) ?: Vencimiento(
            id = "VENC-${sol.uuidEvento}-${sol.nivelActual.codigoCorto}",
            uuidEvento = sol.uuidEvento,
            nivel = sol.nivelActual,
            primerEnvioNivelEpochMs = ahoraEpochMs,
            venceAcuse60sEpochMs = ahoraEpochMs + Vencimiento.PLAZO_ACUSE_SIMULACRO_MS,
            venceDecision180sEpochMs = ahoraEpochMs + Vencimiento.PLAZO_DECISION_SIMULACRO_MS,
            acuseAplicacionRecibido = true,
            decisionTomada = false,
            escaladoAutomaticoEjecutado = false
        )

        val evento = EventoEstado(
            id = "EVT-${sol.uuidEvento}-$nuevaRev-RECIBIDO",
            uuidEvento = sol.uuidEvento,
            revision = nuevaRev,
            estadoAnterior = sol.estadoGeneral,
            estadoNuevo = EstadoSolicitud.RECIBIDO_AUTORIDAD,
            nivel = sol.nivelActual,
            institucionId = institucionReceptora.id,
            autorId = institucionReceptora.id,
            autorNombre = "$operadorReceptor (${institucionReceptora.nivel.etiqueta})",
            motivo = "RECIBIDO_AUTORIDAD confirmado tras persistencia local. Reloj de decisión (180s) continúa.",
            timestampEpochMs = ahoraEpochMs,
            calidadReloj = sol.calidadReloj
        )

        return ResultadoTransicion(
            solicitudActualizada = solicitudAct,
            necesidadActualizada = detalle.necesidad,
            nuevoEvento = evento,
            nuevoVencimiento = vencimientoAct,
            tipoMensajeEmitir = TipoMensajeApp.RECIBIDO_AUTORIDAD
        )
    }

    /**
     * Aceptación completa o parcial de recursos en el nivel actual (Anexo C, D y J).
     * - Si acepta la totalidad pendiente -> ACEPTADO y detiene escalamiento automático.
     * - Si acepta una parte (ej. 4 de 10) -> reserva las 4 unidades en inventario local, registra Asignacion
     *   y escala inmediatamente el remanente (6) al siguiente nivel territorial conservando el mismo UUID.
     */
    fun aplicarAceptacionRecursos(
        detalle: SolicitudConDetalle,
        institucionActual: Institucion,
        institucionSiguienteNivel: Institucion?,
        reservaSeleccionada: Reserva,
        cantidadAceptar: Int,
        responsableSeguimiento: String,
        ahoraEpochMs: Long
    ): ResultadoTransicion {
        require(cantidadAceptar > 0) { "La cantidad a aceptar debe ser mayor que cero." }
        require(cantidadAceptar <= reservaSeleccionada.cantidadDisponible) {
            "Capacidad local insuficiente en ${reservaSeleccionada.nombreRecurso}: disponible=${reservaSeleccionada.cantidadDisponible}, solicitado reservar=$cantidadAceptar (se impide doble reserva local)."
        }

        val sol = detalle.solicitud
        val nec = detalle.necesidad
        val efectivoAceptado = cantidadAceptar.coerceAtMost(nec.cantidadPendiente)
        val nuevoAceptadoTotal = nec.cantidadAceptadaTotal + efectivoAceptado
        val nuevoPendiente = (nec.cantidadSolicitada - nuevoAceptadoTotal).coerceAtLeast(0)
        val esParcial = nuevoPendiente > 0
        val nuevaRev = sol.revision + 1

        val reservaAct = reservaSeleccionada.copy(
            cantidadReservadaLocal = reservaSeleccionada.cantidadReservadaLocal + efectivoAceptado
        )

        val asignacion = Asignacion(
            id = "ASIG-${sol.uuidEvento}-${institucionActual.nivel.codigoCorto}-$nuevaRev",
            uuidEvento = sol.uuidEvento,
            necesidadId = nec.id,
            institucionId = institucionActual.id,
            nivel = institucionActual.nivel,
            recursoId = reservaSeleccionada.id,
            recursoNombre = reservaSeleccionada.nombreRecurso,
            cantidadComprometida = efectivoAceptado,
            responsableNombre = responsableSeguimiento,
            plazoSeguimientoEpochMs = ahoraEpochMs + 3_600_000L,
            estadoOperativo = EstadoAsignacion.RESERVADO,
            revision = nuevaRev
        )

        val siguienteNivel = sol.nivelActual.siguienteNivel()
        val nivelDestino = if (esParcial && siguienteNivel != null) siguienteNivel else sol.nivelActual
        val institucionDestinoId = if (esParcial && institucionSiguienteNivel != null) {
            institucionSiguienteNivel.id
        } else {
            institucionActual.id
        }

        val nuevoEstado = when {
            !esParcial -> EstadoSolicitud.ACEPTADO
            siguienteNivel != null -> EstadoSolicitud.ACEPTADO_PARCIAL
            else -> EstadoSolicitud.PENDIENTE_APOYO_CENTRAL
        }

        val motivoTexto = if (!esParcial) {
            "Aceptación completa en ${institucionActual.nivel.etiqueta}: $efectivoAceptado ${nec.unidad} (${reservaSeleccionada.nombreRecurso}) por $responsableSeguimiento."
        } else if (siguienteNivel != null) {
            "Aceptación parcial en ${institucionActual.nivel.etiqueta}: compromete $efectivoAceptado ${nec.unidad} ($responsableSeguimiento) y escala $nuevoPendiente ${nec.unidad} pendientes a ${siguienteNivel.etiqueta}."
        } else {
            "Aceptación parcial en Managua ($efectivoAceptado ${nec.unidad}); quedan $nuevoPendiente ${nec.unidad} en PENDIENTE_APOYO_CENTRAL."
        }

        val solicitudAct = sol.copy(
            revision = nuevaRev,
            nivelActual = nivelDestino,
            institucionDestinoActualId = institucionDestinoId,
            estadoGeneral = nuevoEstado,
            motivoUltimoCambio = if (esParcial) MotivoCapacidad.ESCALAMIENTO_PARCIAL_RESIDUAL else MotivoCapacidad.NINGUNO,
            notaMotivo = motivoTexto
        )

        val necesidadAct = nec.copy(
            cantidadAceptadaTotal = nuevoAceptadoTotal,
            cantidadPendiente = nuevoPendiente,
            nivelResponsableActual = nivelDestino
        )

        val nuevoVencimiento = if (esParcial && siguienteNivel != null) {
            Vencimiento(
                id = "VENC-${sol.uuidEvento}-${siguienteNivel.codigoCorto}-$nuevaRev",
                uuidEvento = sol.uuidEvento,
                nivel = siguienteNivel,
                primerEnvioNivelEpochMs = ahoraEpochMs,
                venceAcuse60sEpochMs = ahoraEpochMs + Vencimiento.PLAZO_ACUSE_SIMULACRO_MS,
                venceDecision180sEpochMs = ahoraEpochMs + Vencimiento.PLAZO_DECISION_SIMULACRO_MS,
                acuseAplicacionRecibido = false,
                decisionTomada = false,
                escaladoAutomaticoEjecutado = false
            )
        } else {
            detalle.vencimientoActual?.copy(
                acuseAplicacionRecibido = true,
                decisionTomada = true
            )
        }

        val evento = EventoEstado(
            id = "EVT-${sol.uuidEvento}-$nuevaRev-ACEPTACION",
            uuidEvento = sol.uuidEvento,
            revision = nuevaRev,
            estadoAnterior = sol.estadoGeneral,
            estadoNuevo = nuevoEstado,
            nivel = institucionActual.nivel,
            institucionId = institucionActual.id,
            autorId = institucionActual.id,
            autorNombre = "$responsableSeguimiento (${institucionActual.nivel.etiqueta})",
            motivo = motivoTexto,
            timestampEpochMs = ahoraEpochMs,
            calidadReloj = sol.calidadReloj
        )

        return ResultadoTransicion(
            solicitudActualizada = solicitudAct,
            necesidadActualizada = necesidadAct,
            nuevoEvento = evento,
            nuevaAsignacion = asignacion,
            reservaActualizada = reservaAct,
            nuevoVencimiento = nuevoVencimiento,
            tipoMensajeEmitir = TipoMensajeApp.ACEPTADO_PARCIAL
        )
    }

    /**
     * Declaración explícita de capacidad insuficiente o escalamiento por vencimiento 60s/180s (Anexo C).
     * Conserva el mismo UUID de evento. Si Managua tampoco puede atender, pasa a PENDIENTE_APOYO_CENTRAL.
     */
    fun declararCapacidadInsuficienteOEscalar(
        detalle: SolicitudConDetalle,
        institucionActual: Institucion,
        institucionSiguienteNivel: Institucion?,
        motivo: MotivoCapacidad,
        observacionAdicional: String,
        autorNombre: String,
        ahoraEpochMs: Long
    ): ResultadoTransicion {
        val sol = detalle.solicitud
        val nec = detalle.necesidad
        val nuevaRev = sol.revision + 1
        val siguienteNivel = sol.nivelActual.siguienteNivel()

        val esUltimoNivelManagua = siguienteNivel == null
        val nivelDestino = siguienteNivel ?: NivelTerritorial.SEDE_CENTRAL_MANAGUA
        val institucionDestinoId = institucionSiguienteNivel?.id ?: sol.institucionDestinoActualId

        val nuevoEstado = if (esUltimoNivelManagua) {
            EstadoSolicitud.PENDIENTE_APOYO_CENTRAL
        } else {
            EstadoSolicitud.ESCALADO
        }

        val descripcionMotivo = buildString {
            append(motivo.etiqueta)
            if (observacionAdicional.isNotBlank()) {
                append(": ").append(observacionAdicional.trim())
            }
            if (esUltimoNivelManagua) {
                append(" -> Sede Central Managua sin capacidad suficiente; se mantiene PENDIENTE DE APOYO CENTRAL y se activa procedimiento institucional alternativo (no se cierra la solicitud).")
            } else {
                append(" -> Escalado de ${sol.nivelActual.etiqueta} a ${nivelDestino.etiqueta} (${nec.cantidadPendiente} ${nec.unidad} pendientes).")
            }
        }

        val solicitudAct = sol.copy(
            revision = nuevaRev,
            nivelActual = nivelDestino,
            institucionDestinoActualId = institucionDestinoId,
            estadoGeneral = nuevoEstado,
            motivoUltimoCambio = motivo,
            notaMotivo = descripcionMotivo
        )

        val necesidadAct = nec.copy(
            nivelResponsableActual = nivelDestino
        )

        val nuevoVencimiento = if (!esUltimoNivelManagua) {
            Vencimiento(
                id = "VENC-${sol.uuidEvento}-${nivelDestino.codigoCorto}-$nuevaRev",
                uuidEvento = sol.uuidEvento,
                nivel = nivelDestino,
                primerEnvioNivelEpochMs = ahoraEpochMs,
                venceAcuse60sEpochMs = ahoraEpochMs + Vencimiento.PLAZO_ACUSE_SIMULACRO_MS,
                venceDecision180sEpochMs = ahoraEpochMs + Vencimiento.PLAZO_DECISION_SIMULACRO_MS,
                acuseAplicacionRecibido = false,
                decisionTomada = false,
                escaladoAutomaticoEjecutado = false
            )
        } else {
            detalle.vencimientoActual?.copy(
                decisionTomada = true,
                escaladoAutomaticoEjecutado = true
            )
        }

        val evento = EventoEstado(
            id = "EVT-${sol.uuidEvento}-$nuevaRev-ESCALADO",
            uuidEvento = sol.uuidEvento,
            revision = nuevaRev,
            estadoAnterior = sol.estadoGeneral,
            estadoNuevo = nuevoEstado,
            nivel = sol.nivelActual,
            institucionId = institucionActual.id,
            autorId = institucionActual.id,
            autorNombre = "$autorNombre (${institucionActual.nivel.etiqueta})",
            motivo = descripcionMotivo,
            timestampEpochMs = ahoraEpochMs,
            calidadReloj = sol.calidadReloj
        )

        return ResultadoTransicion(
            solicitudActualizada = solicitudAct,
            necesidadActualizada = necesidadAct,
            nuevoEvento = evento,
            nuevoVencimiento = nuevoVencimiento,
            tipoMensajeEmitir = TipoMensajeApp.SIN_CAPACIDAD_ESCALADO
        )
    }

    /**
     * Evalúa si el vencimiento de 60 s (acuse de aplicación) o 180 s (decisión operativa) se cumplió.
     * Anexo C: "Sin recepción de la aplicación: Tras el plazo configurado, mostrar 'sin confirmación' y solicitar
     * el siguiente nivel con ese motivo. No concluir que el nivel inferior carece de recursos."
     */
    fun evaluarVencimientoPlazos(
        detalle: SolicitudConDetalle,
        institucionActual: Institucion,
        institucionSiguienteNivel: Institucion?,
        ahoraEpochMs: Long
    ): ResultadoTransicion? {
        val venc = detalle.vencimientoActual ?: return null
        if (venc.decisionTomada || venc.escaladoAutomaticoEjecutado) return null

        if (!venc.acuseAplicacionRecibido && ahoraEpochMs >= venc.venceAcuse60sEpochMs) {
            return declararCapacidadInsuficienteOEscalar(
                detalle = detalle.copy(
                    solicitud = detalle.solicitud.copy(estadoGeneral = EstadoSolicitud.SIN_CONFIRMACION)
                ),
                institucionActual = institucionActual,
                institucionSiguienteNivel = institucionSiguienteNivel,
                motivo = MotivoCapacidad.VENCIMIENTO_SIN_RECEPCION_60S,
                observacionAdicional = "Plazo de 60 s vencido sin RECIBIDO_AUTORIDAD (no implica falta de recursos en ${institucionActual.nivel.etiqueta})",
                autorNombre = "Motor de Plazos (60s)",
                ahoraEpochMs = ahoraEpochMs
            )
        }

        if (ahoraEpochMs >= venc.venceDecision180sEpochMs) {
            return declararCapacidadInsuficienteOEscalar(
                detalle = detalle,
                institucionActual = institucionActual,
                institucionSiguienteNivel = institucionSiguienteNivel,
                motivo = MotivoCapacidad.VENCIMIENTO_SIN_DECISION_180S,
                observacionAdicional = "Plazo de 180 s vencido desde el primer envío al nivel sin decisión operativa",
                autorNombre = "Motor de Plazos (180s)",
                ahoraEpochMs = ahoraEpochMs
            )
        }

        return null
    }

    /**
     * Reconciliación de paquetes repetidos o respuestas tardías (Anexo C, E, G y J).
     * - Si revisionEntrante == revisionLocal: Duplicado idempotente (se ignora efecto de estado y reserva).
     * - Si revisionEntrante < revisionLocal: Mensaje tardío (ej. acuse o decisión tras un escalamiento por 60/180s);
     *   se conserva en bitácora con esTardioReconciliado = true y se marca requiereConciliacionHumana si hay reserva,
     *   sin sobrescribir silenciosamente la decisión más reciente.
     */
    fun reconciliarMensajeTardioODuplicado(
        detalleLocal: SolicitudConDetalle,
        revisionEntrante: Int,
        estadoEntrante: EstadoSolicitud,
        nivelEmisor: NivelTerritorial,
        autorEmisor: String,
        motivoEntrante: String,
        ahoraEpochMs: Long
    ): ResultadoTransicion {
        val sol = detalleLocal.solicitud
        if (revisionEntrante == sol.revision && estadoEntrante == sol.estadoGeneral) {
            // Reintento duplicado idempotente: sin efecto adicional
            val eventoIdempotente = EventoEstado(
                id = "EVT-${sol.uuidEvento}-DUP-$revisionEntrante-$ahoraEpochMs",
                uuidEvento = sol.uuidEvento,
                revision = sol.revision,
                estadoAnterior = sol.estadoGeneral,
                estadoNuevo = sol.estadoGeneral,
                nivel = nivelEmisor,
                institucionId = sol.institucionDestinoActualId,
                autorId = autorEmisor,
                autorNombre = autorEmisor,
                motivo = "Paquete duplicado (rev $revisionEntrante) reconciliado por idempotencia sin duplicar solicitud ni reserva.",
                timestampEpochMs = ahoraEpochMs,
                calidadReloj = sol.calidadReloj,
                esTardioReconciliado = true
            )
            return ResultadoTransicion(
                solicitudActualizada = sol,
                necesidadActualizada = detalleLocal.necesidad,
                nuevoEvento = eventoIdempotente,
                esIdempotenteIgnorado = true
            )
        }

        // Mensaje tardío (revisión anterior o concurrente tras partición)
        val requiereConciliacion = estadoEntrante == EstadoSolicitud.ACEPTADO ||
            estadoEntrante == EstadoSolicitud.ACEPTADO_PARCIAL
        val solicitudReconciliada = sol.copy(
            requiereConciliacionHumana = sol.requiereConciliacionHumana || requiereConciliacion
        )
        val eventoTardio = EventoEstado(
            id = "EVT-${sol.uuidEvento}-TARDIO-$revisionEntrante-$ahoraEpochMs",
            uuidEvento = sol.uuidEvento,
            revision = sol.revision,
            estadoAnterior = sol.estadoGeneral,
            estadoNuevo = sol.estadoGeneral,
            nivel = nivelEmisor,
            institucionId = sol.institucionDestinoActualId,
            autorId = autorEmisor,
            autorNombre = "$autorEmisor (${nivelEmisor.etiqueta})",
            motivo = "Respuesta tardía recibida (rev $revisionEntrante: ${estadoEntrante.etiqueta} - $motivoEntrante). Conservada en historial sin sobrescribir la revisión vigente (${sol.revision}).",
            timestampEpochMs = ahoraEpochMs,
            calidadReloj = sol.calidadReloj,
            esTardioReconciliado = true
        )
        return ResultadoTransicion(
            solicitudActualizada = solicitudReconciliada,
            necesidadActualizada = detalleLocal.necesidad,
            nuevoEvento = eventoTardio,
            esIdempotenteIgnorado = true
        )
    }
}
