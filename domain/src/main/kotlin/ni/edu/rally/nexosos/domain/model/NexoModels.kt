package ni.edu.rally.nexosos.domain.model

/**
 * Cadena territorial oficial de 4 niveles (Anexo A y C).
 * Independiente de las hasta 3 torres físicas de radio (T1, T2, T3).
 */
enum class NivelTerritorial(val orden: Int, val etiqueta: String, val codigoCorto: String) {
    COMARCA(1, "Comarca", "COM"),
    MUNICIPIO(2, "Municipio", "MUN"),
    CABECERA_DEPARTAMENTAL(3, "Cabecera Departamental", "DEP"),
    SEDE_CENTRAL_MANAGUA(4, "Sede Central Managua", "MGA");

    fun siguienteNivel(): NivelTerritorial? = entries.firstOrNull { it.orden == this.orden + 1 }

    companion object {
        fun fromOrden(orden: Int): NivelTerritorial =
            entries.firstOrNull { it.orden == orden } ?: COMARCA
    }
}

/**
 * Actores definidos en el Anexo A.
 */
enum class RolActor(val titulo: String, val nivelAsociado: NivelTerritorial?) {
    COMUNIDAD_BRIGADA("Comunidad / Brigada", NivelTerritorial.COMARCA),
    RESPONSABLE_COMARCA("Responsable de Comarca", NivelTerritorial.COMARCA),
    COORDINACION_MUNICIPAL("Coordinación Municipal", NivelTerritorial.MUNICIPIO),
    COORDINACION_DEPARTAMENTAL("Coordinación Departamental", NivelTerritorial.CABECERA_DEPARTAMENTAL),
    SEDE_CENTRAL_MANAGUA("Sede Central Managua", NivelTerritorial.SEDE_CENTRAL_MANAGUA),
    ADMINISTRADOR("Administrador de Red y Directorio", null)
}

/**
 * Estados verificables de una solicitud SOS (Anexo C, D y E).
 * Prohibido mostrar ENVIADO_A_RADIO solo por guardar un registro local.
 */
enum class EstadoSolicitud(val etiqueta: String, val descripcionOperativa: String) {
    CREADO("Creado localmente", "Guardado en base local antes de transmitir"),
    PENDIENTE_TRANSMISION("Pendiente de transmisión", "Guardado sin radio vinculado; no afirma llegada a autoridad"),
    ENVIADO_A_RADIO("Enviado a radio", "Entregado al radio Meshtastic local por BLE/Serie"),
    ACK_TECNICO_RADIO("ACK técnico de radio", "Acuse LoRa de nodo vecino; NO significa que autoridad recibió ni detiene reloj"),
    SIN_CONFIRMACION("Sin confirmación (60s)", "Venció plazo de acuse de aplicación; escalando al siguiente nivel"),
    RECIBIDO_AUTORIDAD("Recibido por autoridad", "Servicio receptor validó y persistió; pendiente de decisión operativa"),
    ACEPTADO_PARCIAL("Aceptado parcialmente", "Parte cubierta localmente; el pendiente residual fue escalado"),
    ESCALADO("Escalado al siguiente nivel", "Transferido en la cadena territorial conservando el mismo UUID"),
    ACEPTADO("Aceptado completamente", "Recursos comprometidos con responsable y seguimiento"),
    EN_CAMINO("En camino (Salida)", "Brigada o suministro en ruta hacia el punto de emergencia"),
    EN_ATENCION("En atención en sitio", "Llegada confirmada y atención activa en el lugar"),
    ATENDIDO("Atendido / Cerrado", "Necesidad cubierta y confirmada en historial"),
    PENDIENTE_APOYO_CENTRAL("Pendiente de apoyo central", "Managua tampoco puede atender; activo procedimiento institucional alternativo"),
    CANCELADO("Cancelado con motivo", "Cancelado por el emisor o autoridad con registro en bitácora")
}

/**
 * Motivos explícitos de capacidad insuficiente o cambio (Anexo C).
 */
enum class MotivoCapacidad(val codigo: Int, val etiqueta: String) {
    NINGUNO(0, "Sin motivo adicional"),
    FALTA_BRIGADA(1, "Falta de brigada disponible"),
    FALTA_TRANSPORTE(2, "Falta de transporte operativo"),
    FALTA_SUMINISTROS(3, "Falta de suministros en inventario"),
    FALTA_CENTRO_RECEPTOR(4, "Falta de capacidad en centro receptor"),
    VENCIMIENTO_SIN_RECEPCION_60S(5, "Sin confirmación de aplicación en 60 s"),
    VENCIMIENTO_SIN_DECISION_180S(6, "Recepción sin decisión operativa en 180 s"),
    ESCALAMIENTO_PARCIAL_RESIDUAL(7, "Escalamiento de cantidad pendiente tras aceptación parcial"),
    NOTIFICACION_VITAL_PARALELA(8, "Urgencia vital: notificación paralela autorizada"),
    CORRECCION_O_CANCELACION_OPERADOR(9, "Corrección o cancelación justificada por operador");

    companion object {
        fun fromCodigo(codigo: Int): MotivoCapacidad =
            entries.firstOrNull { it.codigo == codigo } ?: NINGUNO
    }
}

enum class TipoNecesidad(val codigo: Int, val etiqueta: String, val unidadPorDefecto: String) {
    RESCATE_BRIGADA(1, "Brigada de rescate y evacuación", "brigadas"),
    AGUA_Y_ALIMENTOS(2, "Kits de agua potable y raciones", "kits"),
    ATENCION_MEDICA_PRIMEROS_AUXILIOS(3, "Botiquines / trauma y traslado", "unidades"),
    TRANSPORTE_EVACUACION(4, "Vehículos / lanchas de evacuación", "unidades"),
    REFUGIO_Y_FRAZADAS(5, "Carpas y kits de albergue", "kits"),
    DESPEJE_VIAL(6, "Equipo de despeje de vía bloqueada", "cuadrillas");

    companion object {
        fun fromCodigo(codigo: Int): TipoNecesidad =
            entries.firstOrNull { it.codigo == codigo } ?: RESCATE_BRIGADA
    }
}

enum class UrgenciaDeclarada(val codigo: Int, val etiqueta: String) {
    CRITICA_VITAL(3, "Crítica / Riesgo vital inmediato"),
    ALTA(2, "Alta / Requiere atención pronta"),
    MODERADA(1, "Moderada / Suministro prioritario");

    companion object {
        fun fromCodigo(codigo: Int): UrgenciaDeclarada =
            entries.firstOrNull { it.codigo == codigo } ?: ALTA
    }
}

enum class FuenteUbicacion(val codigo: Int, val etiqueta: String) {
    GNSS_TELEFONO(1, "GNSS del teléfono"),
    SELECCION_MAPA_LOCAL(2, "Selección manual en mapa sin conexión"),
    REFERENCIA_SITIO_COMUNITARIO(3, "Código de sitio / referencia comunitaria");

    companion object {
        fun fromCodigo(codigo: Int): FuenteUbicacion =
            entries.firstOrNull { it.codigo == codigo } ?: REFERENCIA_SITIO_COMUNITARIO
    }
}

enum class CalidadReloj(val codigo: Int, val etiqueta: String) {
    SINCRONIZADO_GNSS_RED(1, "Reloj sincronizado (GNSS/Red)"),
    MONOTONICO_RECONSTRUIDO(2, "Reconstruido tras reinicio (secuencia local)"),
    DESFASADO_SOSPECHOSO(3, "Reloj local con desfase detectado");

    companion object {
        fun fromCodigo(codigo: Int): CalidadReloj =
            entries.firstOrNull { it.codigo == codigo } ?: SINCRONIZADO_GNSS_RED
    }
}

/**
 * Mensajes de aplicación de Nexo SOS sobre PRIVATE_APP = 256 (Anexo E).
 */
enum class TipoMensajeApp(val codigo: Int, val etiqueta: String) {
    SOS_ACTUALIZACION(1, "SOS / ACTUALIZACION"),
    RECIBIDO_AUTORIDAD(2, "RECIBIDO_AUTORIDAD"),
    ACEPTADO_PARCIAL(3, "ACEPTADO / PARCIAL"),
    SIN_CAPACIDAD_ESCALADO(4, "SIN_CAPACIDAD / ESCALADO"),
    ESTADO_OPERATIVO(5, "EN_CAMINO / ATENDIDO / CANCELADO");

    companion object {
        const val MESHTASTIC_PRIVATE_APP_PORTNUM = 256
        const val MAX_SERIALIZED_BYTES = 160

        fun fromCodigo(codigo: Int): TipoMensajeApp =
            entries.firstOrNull { it.codigo == codigo } ?: SOS_ACTUALIZACION
    }
}

enum class TipoAcuse {
    ACK_TECNICO_RADIO_LORA,
    RECIBIDO_AUTORIDAD_APLICACION
}

enum class EstadoOutbox {
    PENDIENTE_SIN_RADIO,
    ENCOLADO_PARA_RADIO,
    TRANSMITIDO_ESPERANDO_ACUSE_APP,
    PENDIENTE_SALIDA_MANAGUA,
    ENTREGADO_CONFIRMADO,
    RECHAZADO_TAMANO_O_FIRMA
}

enum class CanalTransporte(val etiqueta: String) {
    BLE_MESHTASTIC_REAL("BLE Meshtastic (Radio Externo)"),
    MALLA_SIMULADA_RALLY("Malla Simulada (Demo Rally)"),
    PASARELA_IP_TERRESTRE("Pasarela IP Institucional"),
    RESPALDO_SATELITAL_SBD("Respaldo Satelital SBD")
}

enum class EstadoAsignacion(val etiqueta: String) {
    RESERVADO("Recurso reservado"),
    EN_CAMINO("En camino (Salida)"),
    EN_SITIO("En atención en sitio"),
    ATENDIDO("Atendido / Completado"),
    CANCELADO("Cancelado con motivo")
}

enum class EstadoRuta(val etiqueta: String) {
    COMPROBADA_HABILITADA("Acceso comprobado habilitado"),
    CANDIDATA_NO_VERIFICADA("Ruta candidata (por verificar)"),
    REPORTADA_BLOQUEADA("Reportada como bloqueada"),
    VERIFICADA_BLOQUEADA("Verificada bloqueada por autoridad")
}

enum class TipoPuntoMapa(val etiqueta: String) {
    PUNTO_REUNION_COMUNITARIO("Punto de reunión / Radio comunitario"),
    CENTRO_RECEPTOR_SALUD_ALBERGUE("Centro receptor"),
    TORRE_REPETIDORA_LORA("Torre repetidora LoRa (Máx 3: T1-T3)"),
    SEDE_INSTITUCIONAL("Sede de coordinación")
}

// ============================================================================
// Las 15 Entidades de Negocio exigidas en el Anexo E
// ============================================================================

data class Institucion(
    val id: String,
    val nombre: String,
    val nivel: NivelTerritorial,
    val territorioId: String,
    val responsableTurno: String,
    val ciudad: String,
    val codigoFuncion: String
)

data class Territorio(
    val id: String,
    val nombre: String,
    val nivel: NivelTerritorial,
    val padreId: String?,
    val torreCercanaId: String?
)

data class Usuario(
    val id: String,
    val nombre: String,
    val rol: RolActor,
    val institucionId: String,
    val nivel: NivelTerritorial,
    val claveId: Int,
    val activo: Boolean = true
)

data class Dispositivo(
    val id: String,
    val nodoMeshId: String,
    val alias: String,
    val macBle: String,
    val tipoTransporte: CanalTransporte,
    val autorizado: Boolean,
    val firmwareVersion: String,
    val regionLoRa: String,
    val canalConfigurado: String,
    val bateriaPorcentaje: Int
)

data class Solicitud(
    val uuidEvento: String,
    val revision: Int,
    val institucionOrigenId: String,
    val institucionDestinoActualId: String,
    val nivelActual: NivelTerritorial,
    val creadoPorUsuarioId: String,
    val fechaCreacionEpochMs: Long,
    val calidadReloj: CalidadReloj,
    val latitud: Double?,
    val longitud: Double?,
    val precisionMetros: Int?,
    val antiguedadUbicacionSeg: Int?,
    val referenciaSitio: String,
    val fuenteUbicacion: FuenteUbicacion,
    val personasAfectadas: Int,
    val urgencia: UrgenciaDeclarada,
    val estadoGeneral: EstadoSolicitud,
    val motivoUltimoCambio: MotivoCapacidad,
    val notaMotivo: String,
    val descripcionAmpliadaLocal: String,
    val requiereConciliacionHumana: Boolean = false
)

data class Necesidad(
    val id: String,
    val uuidEvento: String,
    val tipo: TipoNecesidad,
    val unidad: String,
    val cantidadSolicitada: Int,
    val cantidadAceptadaTotal: Int,
    val cantidadPendiente: Int,
    val nivelResponsableActual: NivelTerritorial
)

data class EventoEstado(
    val id: String,
    val uuidEvento: String,
    val revision: Int,
    val estadoAnterior: EstadoSolicitud?,
    val estadoNuevo: EstadoSolicitud,
    val nivel: NivelTerritorial,
    val institucionId: String,
    val autorId: String,
    val autorNombre: String,
    val motivo: String,
    val timestampEpochMs: Long,
    val calidadReloj: CalidadReloj,
    val esTardioReconciliado: Boolean = false
)

data class Asignacion(
    val id: String,
    val uuidEvento: String,
    val necesidadId: String,
    val institucionId: String,
    val nivel: NivelTerritorial,
    val recursoId: String,
    val recursoNombre: String,
    val cantidadComprometida: Int,
    val responsableNombre: String,
    val plazoSeguimientoEpochMs: Long,
    val estadoOperativo: EstadoAsignacion,
    val revision: Int
)

data class Reserva(
    val id: String,
    val codigoRecurso: String,
    val nombreRecurso: String,
    val tipoNecesidadCubierta: TipoNecesidad,
    val institucionPropietariaId: String,
    val nivel: NivelTerritorial,
    val cantidadTotalInventario: Int,
    val cantidadReservadaLocal: Int,
    val unidad: String,
    val responsableInventario: String,
    val enConflictoParticion: Boolean = false
) {
    val cantidadDisponible: Int
        get() = (cantidadTotalInventario - cantidadReservadaLocal).coerceAtLeast(0)
}

data class Mensaje(
    val idMensajeLogico: String,
    val paqueteRadioId: Int,
    val uuidEvento: String,
    val revision: Int,
    val tipoMensaje: TipoMensajeApp,
    val portNum: Int = TipoMensajeApp.MESHTASTIC_PRIVATE_APP_PORTNUM,
    val institucionOrigenId: String,
    val institucionDestinoId: String,
    val nivelOrigen: NivelTerritorial,
    val nivelDestino: NivelTerritorial,
    val bytesSerializados: Int,
    val claveId: Int,
    val hmacHex: String,
    val verificadoAutentico: Boolean,
    val timestampEpochMs: Long
)

data class Acuse(
    val id: String,
    val idMensajeLogico: String,
    val uuidEvento: String,
    val tipoAcuse: TipoAcuse,
    val receptorInstitucionId: String,
    val receptorNivel: NivelTerritorial,
    val timestampEpochMs: Long,
    val detieneRelojRecepcion: Boolean,
    val detieneRelojDecision: Boolean
)

data class Outbox(
    val id: String,
    val idMensajeLogico: String,
    val uuidEvento: String,
    val revision: Int,
    val tipoMensaje: TipoMensajeApp,
    val nivelDestino: NivelTerritorial,
    val payloadBinario: ByteArray,
    val tamanoBytes: Int,
    val estadoOutbox: EstadoOutbox,
    val intentos: Int,
    val proximoIntentoEpochMs: Long,
    val canalPreferido: CanalTransporte,
    val creadoEpochMs: Long
)

data class Vencimiento(
    val id: String,
    val uuidEvento: String,
    val nivel: NivelTerritorial,
    val primerEnvioNivelEpochMs: Long,
    val venceAcuse60sEpochMs: Long,
    val venceDecision180sEpochMs: Long,
    val acuseAplicacionRecibido: Boolean,
    val decisionTomada: Boolean,
    val escaladoAutomaticoEjecutado: Boolean
) {
    companion object {
        const val PLAZO_ACUSE_SIMULACRO_MS = 60_000L
        const val PLAZO_DECISION_SIMULACRO_MS = 180_000L
    }
}

data class ReporteVial(
    val id: String,
    val tramoNombre: String,
    val referenciaUbicacion: String,
    val latitud: Double,
    val longitud: Double,
    val estadoRuta: EstadoRuta,
    val reportadoPor: String,
    val verificadoPorAutoridad: Boolean,
    val observacion: String,
    val timestampEpochMs: Long
)

data class CentroAtencion(
    val id: String,
    val codigoSitio: String,
    val nombre: String,
    val tipo: TipoPuntoMapa,
    val nivel: NivelTerritorial,
    val latitud: Double,
    val longitud: Double,
    val capacidadDisponible: String,
    val responsable: String,
    val activo: Boolean = true
)

/**
 * Vista agregada completa de una emergencia SOS para la UI y el motor de escalamiento.
 */
data class SolicitudConDetalle(
    val solicitud: Solicitud,
    val necesidad: Necesidad,
    val historial: List<EventoEstado>,
    val asignaciones: List<Asignacion>,
    val vencimientoActual: Vencimiento?,
    val ultimoOutbox: Outbox?
)
