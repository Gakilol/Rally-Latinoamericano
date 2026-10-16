package ni.edu.rally.nexosos.domain.engine

import ni.edu.rally.nexosos.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class EscalationEngineTest {

    private val instComarca = Institucion(
        id = "INST-COM-01",
        nombre = "Comarca Las Nubes",
        nivel = NivelTerritorial.COMARCA,
        territorioId = "TER-COM-01",
        responsableTurno = "María López",
        ciudad = "El Crucero",
        codigoFuncion = "ROL-COMARCA"
    )

    private val instMunicipio = Institucion(
        id = "INST-MUN-01",
        nombre = "COMUPRED El Crucero",
        nivel = NivelTerritorial.MUNICIPIO,
        territorioId = "TER-MUN-01",
        responsableTurno = "Carlos Rivas",
        ciudad = "El Crucero",
        codigoFuncion = "ROL-MUNICIPIO"
    )

    private val instDepartamento = Institucion(
        id = "INST-DEP-01",
        nombre = "CODEPRED Managua",
        nivel = NivelTerritorial.CABECERA_DEPARTAMENTAL,
        territorioId = "TER-DEP-01",
        responsableTurno = "Elena Morales",
        ciudad = "Managua (Rol Depto)",
        codigoFuncion = "ROL-DEPTO"
    )

    private val instManagua = Institucion(
        id = "INST-MGA-01",
        nombre = "Sede Central Managua",
        nivel = NivelTerritorial.SEDE_CENTRAL_MANAGUA,
        territorioId = "TER-MGA-01",
        responsableTurno = "Roberto Blandón",
        ciudad = "Managua (Rol Central)",
        codigoFuncion = "ROL-CENTRAL"
    )

    private fun crearDetalleBase(
        nivel: NivelTerritorial = NivelTerritorial.MUNICIPIO,
        solicitada: Int = 10,
        aceptada: Int = 0,
        pendiente: Int = 10
    ): SolicitudConDetalle {
        val uuid = "550e8400-e29b-41d4-a716-446655440000"
        val ahora = 1_700_000_000_000L
        val sol = Solicitud(
            uuidEvento = uuid,
            revision = 1,
            institucionOrigenId = instComarca.id,
            institucionDestinoActualId = instMunicipio.id,
            nivelActual = nivel,
            creadoPorUsuarioId = "USR-01",
            fechaCreacionEpochMs = ahora,
            calidadReloj = CalidadReloj.SINCRONIZADO_GNSS_RED,
            latitud = 11.9798,
            longitud = -86.3145,
            precisionMetros = 6,
            antiguedadUbicacionSeg = 4,
            referenciaSitio = "PR-COM-ESC01",
            fuenteUbicacion = FuenteUbicacion.GNSS_TELEFONO,
            personasAfectadas = 30,
            urgencia = UrgenciaDeclarada.CRITICA_VITAL,
            estadoGeneral = EstadoSolicitud.ENVIADO_A_RADIO,
            motivoUltimoCambio = MotivoCapacidad.NINGUNO,
            notaMotivo = "",
            descripcionAmpliadaLocal = ""
        )
        val nec = Necesidad(
            id = "NEC-$uuid",
            uuidEvento = uuid,
            tipo = TipoNecesidad.AGUA_Y_ALIMENTOS,
            unidad = "kits",
            cantidadSolicitada = solicitada,
            cantidadAceptadaTotal = aceptada,
            cantidadPendiente = pendiente,
            nivelResponsableActual = nivel
        )
        val venc = Vencimiento(
            id = "VENC-1",
            uuidEvento = uuid,
            nivel = nivel,
            primerEnvioNivelEpochMs = ahora,
            venceAcuse60sEpochMs = ahora + 60_000L,
            venceDecision180sEpochMs = ahora + 180_000L,
            acuseAplicacionRecibido = false,
            decisionTomada = false,
            escaladoAutomaticoEjecutado = false
        )
        return SolicitudConDetalle(sol, nec, emptyList(), emptyList(), venc, null)
    }

    @Test
    fun `Anexo J - Acuse tecnico sin autoridad no cambia estado a recibido por autoridad ni detiene plazo`() {
        val base = crearDetalleBase(nivel = NivelTerritorial.COMARCA)
        val res = EscalationEngine.aplicarAckTecnicoRadio(
            detalle = base,
            nodoRadioId = "!a1b201",
            ahoraEpochMs = 1_700_000_010_000L
        )
        assertNotEquals(EstadoSolicitud.RECIBIDO_AUTORIDAD, res.solicitudActualizada.estadoGeneral)
        assertEquals(EstadoSolicitud.ACK_TECNICO_RADIO, res.solicitudActualizada.estadoGeneral)
        assertFalse(res.nuevoVencimiento!!.acuseAplicacionRecibido)
        assertEquals(base.vencimientoActual!!.venceDecision180sEpochMs, res.nuevoVencimiento!!.venceDecision180sEpochMs)
    }

    @Test
    fun `Anexo J - Atencion parcial de 10 unidades acepta 4 en municipio y escala 6 a departamento con mismo UUID`() {
        val base = crearDetalleBase(nivel = NivelTerritorial.MUNICIPIO, solicitada = 10, aceptada = 0, pendiente = 10)
        val reservaMunicipio = Reserva(
            id = "RES-MUN-AGUA",
            codigoRecurso = "MUN-KIT-04",
            nombreRecurso = "Kits municipales",
            tipoNecesidadCubierta = TipoNecesidad.AGUA_Y_ALIMENTOS,
            institucionPropietariaId = instMunicipio.id,
            nivel = NivelTerritorial.MUNICIPIO,
            cantidadTotalInventario = 4,
            cantidadReservadaLocal = 0,
            unidad = "kits",
            responsableInventario = "Carlos Rivas"
        )

        val res = EscalationEngine.aplicarAceptacionRecursos(
            detalle = base,
            institucionActual = instMunicipio,
            institucionSiguienteNivel = instDepartamento,
            reservaSeleccionada = reservaMunicipio,
            cantidadAceptar = 4,
            responsableSeguimiento = "Carlos Rivas",
            ahoraEpochMs = 1_700_000_030_000L
        )

        assertEquals(base.solicitud.uuidEvento, res.solicitudActualizada.uuidEvento)
        assertEquals(EstadoSolicitud.ACEPTADO_PARCIAL, res.solicitudActualizada.estadoGeneral)
        assertEquals(NivelTerritorial.CABECERA_DEPARTAMENTAL, res.solicitudActualizada.nivelActual)
        assertEquals(10, res.necesidadActualizada.cantidadSolicitada)
        assertEquals(4, res.necesidadActualizada.cantidadAceptadaTotal)
        assertEquals(6, res.necesidadActualizada.cantidadPendiente)
        assertEquals(4, res.reservaActualizada!!.cantidadReservadaLocal)
        assertEquals(0, res.reservaActualizada!!.cantidadDisponible)
    }

    @Test
    fun `Anexo C - Si Managua tampoco puede atender se mantiene PENDIENTE_APOYO_CENTRAL sin cerrar la solicitud`() {
        val baseManagua = crearDetalleBase(
            nivel = NivelTerritorial.SEDE_CENTRAL_MANAGUA,
            solicitada = 10,
            aceptada = 4,
            pendiente = 6
        )
        val res = EscalationEngine.declararCapacidadInsuficienteOEscalar(
            detalle = baseManagua,
            institucionActual = instManagua,
            institucionSiguienteNivel = null,
            motivo = MotivoCapacidad.FALTA_TRANSPORTE,
            observacionAdicional = "Rutas aéreas y terrestres saturadas",
            autorNombre = "Roberto Blandón",
            ahoraEpochMs = 1_700_000_090_000L
        )

        assertEquals(EstadoSolicitud.PENDIENTE_APOYO_CENTRAL, res.solicitudActualizada.estadoGeneral)
        assertEquals(6, res.necesidadActualizada.cantidadPendiente)
    }
}
