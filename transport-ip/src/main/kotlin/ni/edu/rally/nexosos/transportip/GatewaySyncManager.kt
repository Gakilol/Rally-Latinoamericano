package ni.edu.rally.nexosos.transportip

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Estado de la pasarela institucional IP / SBD hacia Sede Central Managua (Anexos B, F, G y J).
 */
data class EstadoPasarelaManagua(
    val enlaceIpTerrestreActivo: Boolean = false,
    val respaldoSatelitalSbdActivo: Boolean = true,
    val sedeCentralOperativa: Boolean = true,
    val consolaAlternaAutorizadaActiva: Boolean = false,
    val brokerPrivadoAutenticado: String = "mqtts://pasarela-privada.nexosos.ni:8883 (Broker público Meshtastic DESACTIVADO)",
    val filtroAntiBucleLoRaIpLoRaActivo: Boolean = true,
    val colaPendienteSalidaCentral: Int = 0,
    val ultimoEstadoSincronizacion: String = "Enlace satelital SBD habilitado hacia Managua; sin dependencia de broker público.",
    val latenciaSatelitalMedidaSeg: Double = 14.2
) {
    val salidaHaciaManaguaDisponible: Boolean
        get() = (enlaceIpTerrestreActivo || respaldoSatelitalSbdActivo) &&
            (sedeCentralOperativa || consolaAlternaAutorizadaActiva)
}

class GatewaySyncManager {

    private val _estado = MutableStateFlow(EstadoPasarelaManagua())
    val estado: StateFlow<EstadoPasarelaManagua> = _estado.asStateFlow()

    /**
     * Simula el escenario "Malla aislada de Managua" (Anexo G y J):
     * Cae tanto el enlace IP como el respaldo satelital SBD.
     * Comarca, Municipio y Cabecera continúan por radio local, pero el escalamiento central queda pendiente de entrega.
     */
    fun configurarMallaAisladaDeManagua(aislada: Boolean) {
        _estado.update { current ->
            if (aislada) {
                current.copy(
                    enlaceIpTerrestreActivo = false,
                    respaldoSatelitalSbdActivo = false,
                    colaPendienteSalidaCentral = current.colaPendienteSalidaCentral + 1,
                    ultimoEstadoSincronizacion = "CORTE EXTERNO: Sin IP ni SBD hacia Managua. Escalamiento central figura PENDIENTE DE ENTREGA."
                )
            } else {
                current.copy(
                    enlaceIpTerrestreActivo = true,
                    respaldoSatelitalSbdActivo = true,
                    colaPendienteSalidaCentral = 0,
                    ultimoEstadoSincronizacion = "ENLACE RESTAURADO: Cola Outbox entregada y acusada con timestamp real de reconexión (no afirma llegada durante el corte)."
                )
            }
        }
    }

    /**
     * Simula el escenario "Sede central destruida" (Anexo G):
     * Una consola de operación alterna fuera de Managua asume el rol central mediante autorización.
     */
    fun alternarSedeCentralDestruida(destruida: Boolean, activarConsolaAlterna: Boolean = true) {
        _estado.update { current ->
            if (destruida) {
                current.copy(
                    sedeCentralOperativa = false,
                    consolaAlternaAutorizadaActiva = activarConsolaAlterna,
                    ultimoEstadoSincronizacion = if (activarConsolaAlterna) {
                        "Sede Central indisponible: Consola de Operación Alterna (RPO 24h / RTO 4h) asumió rol central mediante autorización."
                    } else {
                        "ALERTA: Sede Central caída y sin consola alterna autorizada."
                    }
                )
            } else {
                current.copy(
                    sedeCentralOperativa = true,
                    consolaAlternaAutorizadaActiva = false,
                    ultimoEstadoSincronizacion = "Sede Central Managua operativa."
                )
            }
        }
    }

    fun alternarIpTerrestre(activo: Boolean) {
        _estado.update {
            it.copy(
                enlaceIpTerrestreActivo = activo,
                ultimoEstadoSincronizacion = "Enlace IP terrestre: ${if (activo) "ACTIVO" else "CAÍDO"}"
            )
        }
    }

    fun alternarRespaldoSbd(activo: Boolean) {
        _estado.update {
            it.copy(
                respaldoSatelitalSbdActivo = activo,
                ultimoEstadoSincronizacion = "Respaldo satelital Iridium SBD: ${if (activo) "ACTIVO" else "DESHABILITADO"}"
            )
        }
    }
}
