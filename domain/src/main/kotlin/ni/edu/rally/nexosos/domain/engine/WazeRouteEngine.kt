package ni.edu.rally.nexosos.domain.engine

import ni.edu.rally.nexosos.domain.model.EstadoRuta
import kotlin.math.roundToInt

/**
 * Nodo geográfico real del corredor de San Rafael del Sur (Departamento de Managua, Nicaragua).
 */
data class NodoVialCorredor(
    val id: String,
    val nombre: String,
    val subtitulo: String,
    val latitud: Double,
    val longitud: Double,
    val xRelativoMapa: Float,
    val yRelativoMapa: Float,
    val esTorreLoRa: String? = null
)

/**
 * Tramo de carretera entre dos nodos del corredor de San Rafael del Sur,
 * con soporte de alertas en tiempo real estilo Waze.
 */
data class TramoVialWaze(
    val id: String,
    val codigoCarretera: String,
    val nombreTramo: String,
    val origenId: String,
    val destinoId: String,
    val distanciaKm: Double,
    val tiempoBaseMin: Int,
    val estadoRuta: EstadoRuta,
    val tipoAlertaWaze: String,
    val reportadoPor: String
) {
    val estaBloqueado: Boolean
        get() = estadoRuta == EstadoRuta.REPORTADA_BLOQUEADA || estadoRuta == EstadoRuta.VERIFICADA_BLOQUEADA

    /**
     * Costo efectivo en minutos para el algoritmo de ruta óptima (Dijkstra):
     * - Si está bloqueado -> infinito (intransitable, busca desvío).
     * - Si es ruta candidata no verificada -> penalización x1.45 por precaución/terracería.
     * - Si es acceso comprobado -> tiempo base nominal.
     */
    val costoMinutosEfectivo: Double
        get() = when (estadoRuta) {
            EstadoRuta.COMPROBADA_HABILITADA -> tiempoBaseMin.toDouble()
            EstadoRuta.CANDIDATA_NO_VERIFICADA -> tiempoBaseMin * 1.45
            EstadoRuta.REPORTADA_BLOQUEADA, EstadoRuta.VERIFICADA_BLOQUEADA -> Double.POSITIVE_INFINITY
        }
}

/**
 * Resultado del cálculo de la ruta más óptima estilo Waze hacia la zona del desastre.
 */
data class RutaOptimaWaze(
    val origen: NodoVialCorredor,
    val destino: NodoVialCorredor,
    val nodosEnRuta: List<NodoVialCorredor>,
    val tramosEnRuta: List<TramoVialWaze>,
    val tramosBloqueadosEvitados: List<TramoVialWaze>,
    val distanciaTotalKm: Double,
    val tiempoEstimadoMin: Int,
    val instruccionesGiroAGiro: List<String>,
    val hayRutaDisponible: Boolean,
    val resumenRuta: String
)

/**
 * Motor de navegación y cálculo de ruta óptima estilo Waze para el corredor de San Rafael del Sur.
 * Utiliza el algoritmo de Dijkstra sobre la red vial real entre Managua, El Crucero,
 * Empalme Santa Rita (Carretera Vieja a León), El Salto, San Bartolo, San Rafael del Sur,
 * Montelimar, Masachapa y Pochomil.
 */
object WazeRouteEngine {

    val nodosSanRafaelDelSur: List<NodoVialCorredor> = listOf(
        NodoVialCorredor(
            id = "NODO_MANAGUA",
            nombre = "Sede Central Managua (7 Sur)",
            subtitulo = "Nivel 4 · Centro Operaciones SINAPRED",
            latitud = 12.1180,
            longitud = -86.2890,
            xRelativoMapa = 0.86f,
            yRelativoMapa = 0.14f,
            esTorreLoRa = null
        ),
        NodoVialCorredor(
            id = "NODO_EL_CRUCERO",
            nombre = "Cumbre El Crucero / Las Nubes",
            subtitulo = "Nivel 3 · Cabecera / Torre T3 (930 msnm)",
            latitud = 11.9921,
            longitud = -86.3085,
            xRelativoMapa = 0.72f,
            yRelativoMapa = 0.30f,
            esTorreLoRa = "T3"
        ),
        NodoVialCorredor(
            id = "NODO_STA_RITA",
            nombre = "Empalme Santa Rita (Ctra. Vieja a León)",
            subtitulo = "Desvío Regional Norte (NIC-12)",
            latitud = 11.9820,
            longitud = -86.4620,
            xRelativoMapa = 0.36f,
            yRelativoMapa = 0.22f,
            esTorreLoRa = null
        ),
        NodoVialCorredor(
            id = "NODO_EL_SALTO",
            nombre = "Comarca El Salto (Bajada La Sierra)",
            subtitulo = "Carretera Sur NIC-2 (Zona de taludes)",
            latitud = 11.8920,
            longitud = -86.3880,
            xRelativoMapa = 0.62f,
            yRelativoMapa = 0.48f,
            esTorreLoRa = null
        ),
        NodoVialCorredor(
            id = "NODO_SAN_BARTOLO",
            nombre = "Comarca San Bartolo / Chilamatillo",
            subtitulo = "Ruta alterna rural hacia San Rafael",
            latitud = 11.8840,
            longitud = -86.4450,
            xRelativoMapa = 0.40f,
            yRelativoMapa = 0.46f,
            esTorreLoRa = null
        ),
        NodoVialCorredor(
            id = "NODO_SAN_RAFAEL",
            nombre = "San Rafael del Sur (Alcaldía / COMUPRED)",
            subtitulo = "Nivel 2 · Municipio / Torre T2 (126 msnm)",
            latitud = 11.8465,
            longitud = -86.4389,
            xRelativoMapa = 0.50f,
            yRelativoMapa = 0.64f,
            esTorreLoRa = "T2"
        ),
        NodoVialCorredor(
            id = "NODO_MONTELIMAR",
            nombre = "Cruce Montelimar",
            subtitulo = "Vía alterna costera hacia playas",
            latitud = 11.8125,
            longitud = -86.5110,
            xRelativoMapa = 0.24f,
            yRelativoMapa = 0.72f,
            esTorreLoRa = null
        ),
        NodoVialCorredor(
            id = "NODO_MASACHAPA",
            nombre = "Comarca Masachapa (Zona SOS / Puerto)",
            subtitulo = "Nivel 1 · Comarca / Torre T1 (15 msnm)",
            latitud = 11.7809,
            longitud = -86.5193,
            xRelativoMapa = 0.22f,
            yRelativoMapa = 0.88f,
            esTorreLoRa = "T1"
        ),
        NodoVialCorredor(
            id = "NODO_POCHOMIL",
            nombre = "Comarca Pochomil (Centro Receptor Costero)",
            subtitulo = "Nivel 1 · Punto de Reunión y Albergue",
            latitud = 11.7724,
            longitud = -86.5044,
            xRelativoMapa = 0.44f,
            yRelativoMapa = 0.90f,
            esTorreLoRa = null
        )
    )

    val tramosInicialesSanRafael: List<TramoVialWaze> = listOf(
        TramoVialWaze(
            id = "TRAMO-MGA-CRU",
            codigoCarretera = "NIC-2",
            nombreTramo = "Carretera Sur (Managua 7 Sur ↔ El Crucero)",
            origenId = "NODO_MANAGUA",
            destinoId = "NODO_EL_CRUCERO",
            distanciaKm = 21.5,
            tiempoBaseMin = 24,
            estadoRuta = EstadoRuta.COMPROBADA_HABILITADA,
            tipoAlertaWaze = "Vía libre comprobada",
            reportadoPor = "CODEPRED Managua"
        ),
        TramoVialWaze(
            id = "TRAMO-CRU-SALTO",
            codigoCarretera = "NIC-2",
            nombreTramo = "Bajada de Las Sierras (El Crucero ↔ Comarca El Salto)",
            origenId = "NODO_EL_CRUCERO",
            destinoId = "NODO_EL_SALTO",
            distanciaKm = 13.8,
            tiempoBaseMin = 16,
            estadoRuta = EstadoRuta.VERIFICADA_BLOQUEADA,
            tipoAlertaWaze = "⚠️ DERRUMBE DE TALUD KM 34 (Paso cerrado)",
            reportadoPor = "COMUPRED San Rafael del Sur"
        ),
        TramoVialWaze(
            id = "TRAMO-SALTO-SRS",
            codigoCarretera = "NIC-2",
            nombreTramo = "Empalme El Salto ↔ Casco Urbano San Rafael del Sur",
            origenId = "NODO_EL_SALTO",
            destinoId = "NODO_SAN_RAFAEL",
            distanciaKm = 8.4,
            tiempoBaseMin = 10,
            estadoRuta = EstadoRuta.COMPROBADA_HABILITADA,
            tipoAlertaWaze = "Tránsito habilitado",
            reportadoPor = "Brigada Vial Municipal"
        ),
        TramoVialWaze(
            id = "TRAMO-MGA-STARITA",
            codigoCarretera = "NIC-12",
            nombreTramo = "Carretera Vieja a León (Managua ↔ Empalme Santa Rita)",
            origenId = "NODO_MANAGUA",
            destinoId = "NODO_STA_RITA",
            distanciaKm = 24.0,
            tiempoBaseMin = 26,
            estadoRuta = EstadoRuta.COMPROBADA_HABILITADA,
            tipoAlertaWaze = "Desvío pavimentado despejado",
            reportadoPor = "Sede Central Managua"
        ),
        TramoVialWaze(
            id = "TRAMO-STARITA-BARTOLO",
            codigoCarretera = "NIC-12B",
            nombreTramo = "Empalme Santa Rita ↔ Comarca San Bartolo / Chilamatillo",
            origenId = "NODO_STA_RITA",
            destinoId = "NODO_SAN_BARTOLO",
            distanciaKm = 11.2,
            tiempoBaseMin = 14,
            estadoRuta = EstadoRuta.COMPROBADA_HABILITADA,
            tipoAlertaWaze = "Acceso comprobado por Villa El Carmen",
            reportadoPor = "COORDINACION_MUNICIPAL"
        ),
        TramoVialWaze(
            id = "TRAMO-CRU-BARTOLO",
            codigoCarretera = "RUTA-SIERRA",
            nombreTramo = "Enlace Rural El Crucero ↔ San Bartolo",
            origenId = "NODO_EL_CRUCERO",
            destinoId = "NODO_SAN_BARTOLO",
            distanciaKm = 15.0,
            tiempoBaseMin = 20,
            estadoRuta = EstadoRuta.CANDIDATA_NO_VERIFICADA,
            tipoAlertaWaze = "Ruta candidata 4x4 sin derrumbe",
            reportadoPor = "Brigada Comunitaria"
        ),
        TramoVialWaze(
            id = "TRAMO-BARTOLO-SRS",
            codigoCarretera = "NIC-2N",
            nombreTramo = "San Bartolo ↔ Casco Urbano San Rafael del Sur",
            origenId = "NODO_SAN_BARTOLO",
            destinoId = "NODO_SAN_RAFAEL",
            distanciaKm = 6.5,
            tiempoBaseMin = 8,
            estadoRuta = EstadoRuta.COMPROBADA_HABILITADA,
            tipoAlertaWaze = "Entrada norte a San Rafael despejada",
            reportadoPor = "COMUPRED San Rafael del Sur"
        ),
        TramoVialWaze(
            id = "TRAMO-SRS-MASACHAPA",
            codigoCarretera = "NIC-2-COSTA",
            nombreTramo = "Carretera Directa San Rafael del Sur ↔ Masachapa",
            origenId = "NODO_SAN_RAFAEL",
            destinoId = "NODO_MASACHAPA",
            distanciaKm = 11.5,
            tiempoBaseMin = 12,
            estadoRuta = EstadoRuta.COMPROBADA_HABILITADA,
            tipoAlertaWaze = "Acceso principal a costa habilitado",
            reportadoPor = "Brigada Masachapa"
        ),
        TramoVialWaze(
            id = "TRAMO-SRS-MONTELIMAR",
            codigoCarretera = "RUTA-MONTE",
            nombreTramo = "Desvío San Rafael del Sur ↔ Montelimar",
            origenId = "NODO_SAN_RAFAEL",
            destinoId = "NODO_MONTELIMAR",
            distanciaKm = 10.2,
            tiempoBaseMin = 13,
            estadoRuta = EstadoRuta.CANDIDATA_NO_VERIFICADA,
            tipoAlertaWaze = "Ruta alterna por Montelimar",
            reportadoPor = "Puesto Montelimar"
        ),
        TramoVialWaze(
            id = "TRAMO-MONTE-MASACHAPA",
            codigoCarretera = "COSTERA-N",
            nombreTramo = " Enlace Costero Montelimar ↔ Masachapa",
            origenId = "NODO_MONTELIMAR",
            destinoId = "NODO_MASACHAPA",
            distanciaKm = 4.6,
            tiempoBaseMin = 6,
            estadoRuta = EstadoRuta.COMPROBADA_HABILITADA,
            tipoAlertaWaze = "Acceso norte a Masachapa libre",
            reportadoPor = "Brigada Masachapa"
        ),
        TramoVialWaze(
            id = "TRAMO-SRS-POCHOMIL",
            codigoCarretera = "RUTA-POCHO",
            nombreTramo = "Ramal San Rafael del Sur ↔ Balneario Pochomil",
            origenId = "NODO_SAN_RAFAEL",
            destinoId = "NODO_POCHOMIL",
            distanciaKm = 13.0,
            tiempoBaseMin = 15,
            estadoRuta = EstadoRuta.COMPROBADA_HABILITADA,
            tipoAlertaWaze = "Vía a Centro Receptor Pochomil libre",
            reportadoPor = "COMUPRED San Rafael del Sur"
        ),
        TramoVialWaze(
            id = "TRAMO-MASACHAPA-POCHOMIL",
            codigoCarretera = "COSTERA-SUR",
            nombreTramo = "Bulevar Costero Masachapa ↔ Pochomil",
            origenId = "NODO_MASACHAPA",
            destinoId = "NODO_POCHOMIL",
            distanciaKm = 2.8,
            tiempoBaseMin = 4,
            estadoRuta = EstadoRuta.COMPROBADA_HABILITADA,
            tipoAlertaWaze = "Conexión entre playas habilitada",
            reportadoPor = "Puesto Comunitario Masachapa"
        )
    )

    /**
     * Calcula la ruta más óptima (menor tiempo efectivo evitando tramos bloqueados)
     * desde el nodo de despacho hasta el nodo del incidente usando el algoritmo de Dijkstra.
     */
    fun calcularRutaOptima(
        origenId: String,
        destinoId: String,
        tramosActuales: List<TramoVialWaze>
    ): RutaOptimaWaze {
        val mapaNodos = nodosSanRafaelDelSur.associateBy { it.id }
        val nodoOrigen = mapaNodos[origenId] ?: nodosSanRafaelDelSur.first()
        val nodoDestino = mapaNodos[destinoId] ?: nodosSanRafaelDelSur.last()

        val dist = mutableMapOf<String, Double>().withDefault { Double.POSITIVE_INFINITY }
        val prevNodo = mutableMapOf<String, String?>()
        val prevTramo = mutableMapOf<String, TramoVialWaze?>()
        val visitados = mutableSetOf<String>()

        nodosSanRafaelDelSur.forEach { dist[it.id] = Double.POSITIVE_INFINITY }
        dist[nodoOrigen.id] = 0.0

        while (visitados.size < nodosSanRafaelDelSur.size) {
            val u = dist.entries
                .filter { it.key !in visitados }
                .minByOrNull { it.value }
                ?.key ?: break

            val costoU = dist.getValue(u)
            if (costoU == Double.POSITIVE_INFINITY || u == nodoDestino.id) break
            visitados.add(u)

            // Grafo bidireccional
            val incidentes = tramosActuales.filter { it.origenId == u || it.destinoId == u }
            for (tramo in incidentes) {
                if (tramo.estaBloqueado) continue
                val v = if (tramo.origenId == u) tramo.destinoId else tramo.origenId
                if (v in visitados) continue

                val nuevoCosto = costoU + tramo.costoMinutosEfectivo
                if (nuevoCosto < dist.getValue(v)) {
                    dist[v] = nuevoCosto
                    prevNodo[v] = u
                    prevTramo[v] = tramo
                }
            }
        }

        val bloqueadosActivos = tramosActuales.filter { it.estaBloqueado }
        if (dist.getValue(nodoDestino.id) == Double.POSITIVE_INFINITY) {
            return RutaOptimaWaze(
                origen = nodoOrigen,
                destino = nodoDestino,
                nodosEnRuta = listOf(nodoOrigen),
                tramosEnRuta = emptyList(),
                tramosBloqueadosEvitados = bloqueadosActivos,
                distanciaTotalKm = 0.0,
                tiempoEstimadoMin = 0,
                instruccionesGiroAGiro = listOf(
                    "⛔ ALERTA WAZE SOS: Todas las rutas terrestres entre ${nodoOrigen.nombre} y ${nodoDestino.nombre} están bloqueadas.",
                    "Se requiere despeje vial urgente o traslado marítimo/aéreo."
                ),
                hayRutaDisponible = false,
                resumenRuta = "SIN RUTA TERRESTRE DISPONIBLE (${bloqueadosActivos.size} bloqueos activos)"
            )
        }

        val caminoNodos = mutableListOf<NodoVialCorredor>()
        val caminoTramos = mutableListOf<TramoVialWaze>()
        var cursor: String? = nodoDestino.id
        while (cursor != null) {
            mapaNodos[cursor]?.let { caminoNodos.add(0, it) }
            val tr = prevTramo[cursor]
            if (tr != null) {
                caminoTramos.add(0, tr)
            }
            cursor = prevNodo[cursor]
        }

        val kmTotal = (caminoTramos.sumOf { it.distanciaKm } * 10.0).roundToInt() / 10.0
        val minTotal = caminoTramos.sumOf { it.costoMinutosEfectivo }.roundToInt()

        val pasos = mutableListOf<String>()
        pasos.add("🚗 Salida desde ${nodoOrigen.nombre}")
        if (bloqueadosActivos.isNotEmpty()) {
            pasos.add(
                "⚡ Ruteo inteligente Waze: Evitando ${bloqueadosActivos.size} bloqueo(s): " +
                    bloqueadosActivos.joinToString("; ") { "${it.nombreTramo} (${it.tipoAlertaWaze})" }
            )
        }
        caminoTramos.forEachIndexed { idx, tr ->
            val sigNodo = caminoNodos.getOrNull(idx + 1)?.nombre ?: tr.destinoId
            val etiquetaTipo = if (tr.estadoRuta == EstadoRuta.CANDIDATA_NO_VERIFICADA) {
                "[Ruta Candidata / Precaución]"
            } else {
                "[Acceso Comprobado]"
            }
            pasos.add(
                "${idx + 1}. Tomar ${tr.codigoCarretera} (${tr.nombreTramo}) → hacia $sigNodo (${tr.distanciaKm} km · ~${tr.tiempoBaseMin} min) $etiquetaTipo"
            )
        }
        pasos.add("🏁 Llegada al punto de emergencia: ${nodoDestino.nombre} (${nodoDestino.latitud}, ${nodoDestino.longitud})")

        return RutaOptimaWaze(
            origen = nodoOrigen,
            destino = nodoDestino,
            nodosEnRuta = caminoNodos,
            tramosEnRuta = caminoTramos,
            tramosBloqueadosEvitados = bloqueadosActivos,
            distanciaTotalKm = kmTotal,
            tiempoEstimadoMin = minTotal,
            instruccionesGiroAGiro = pasos,
            hayRutaDisponible = true,
            resumenRuta = "Ruta Óptima Waze: $kmTotal km · ETA $minTotal min (${caminoNodos.joinToString(" → ") { it.nombre.substringBefore(" (") }})"
        )
    }
}
