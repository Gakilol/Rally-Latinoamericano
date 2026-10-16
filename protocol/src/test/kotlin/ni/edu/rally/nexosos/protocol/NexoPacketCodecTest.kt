package ni.edu.rally.nexosos.protocol

import ni.edu.rally.nexosos.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class NexoPacketCodecTest {

    @Test
    fun `SOS maximo serializado cumple limite de ingenieria de 160 bytes y preserva campos`() {
        val uuid = UUID.randomUUID().toString()
        val frame = NexoCompactFrame(
            tipoMensaje = TipoMensajeApp.SOS_ACTUALIZACION,
            uuidEvento = uuid,
            idMensajeCorto = 10042,
            revision = 1,
            institucionOrigenCodigo = "COM-01",
            institucionDestinoCodigo = "MUN-01",
            nivelOrigen = NivelTerritorial.COMARCA,
            nivelDestino = NivelTerritorial.MUNICIPIO,
            timestampEpochSec = 1792180000L,
            calidadReloj = CalidadReloj.SINCRONIZADO_GNSS_RED,
            fuenteUbicacion = FuenteUbicacion.GNSS_TELEFONO,
            latitud = 12.136389,
            longitud = -86.251389,
            precisionMetros = 8,
            antiguedadUbicacionSeg = 12,
            referenciaSitioCompacta = "ESC-COMARCA-LAS-NUBES-01",
            tipoNecesidad = TipoNecesidad.AGUA_Y_ALIMENTOS,
            cantidadSolicitada = 10,
            cantidadAceptadaTotal = 4,
            cantidadPendiente = 6,
            personasAfectadas = 45,
            urgencia = UrgenciaDeclarada.CRITICA_VITAL,
            estadoGeneral = EstadoSolicitud.ACEPTADO_PARCIAL,
            motivo = MotivoCapacidad.ESCALAMIENTO_PARCIAL_RESIDUAL,
            vencimientoRestanteSeg = 180,
            responsableORecursoCompacto = "BRIGADA-MUN-NORTE",
            keyId = 201
        )

        val (bytes, hmacHex) = NexoPacketCodec.encode(frame)
        assertTrue("El tamaño serializado (${bytes.size} B) debe ser <= 160 B", bytes.size <= 160)
        assertEquals(32, hmacHex.length)

        val decoded = NexoPacketCodec.decodeAndVerify(bytes)
        assertEquals(uuid, decoded.uuidEvento)
        assertEquals(10, decoded.cantidadSolicitada)
        assertEquals(4, decoded.cantidadAceptadaTotal)
        assertEquals(6, decoded.cantidadPendiente)
        assertEquals(NivelTerritorial.MUNICIPIO, decoded.nivelDestino)
        assertEquals(EstadoSolicitud.ACEPTADO_PARCIAL, decoded.estadoGeneral)
    }

    @Test(expected = PacketAuthenticationException::class)
    fun `paquete modificado en transito es rechazado por autenticador HMAC`() {
        val frame = NexoCompactFrame(
            tipoMensaje = TipoMensajeApp.ACEPTADO_PARCIAL,
            uuidEvento = UUID.randomUUID().toString(),
            idMensajeCorto = 1,
            revision = 2,
            institucionOrigenCodigo = "MUN-01",
            institucionDestinoCodigo = "DEP-01",
            nivelOrigen = NivelTerritorial.MUNICIPIO,
            nivelDestino = NivelTerritorial.CABECERA_DEPARTAMENTAL,
            timestampEpochSec = 1792180100L,
            calidadReloj = CalidadReloj.SINCRONIZADO_GNSS_RED,
            fuenteUbicacion = FuenteUbicacion.REFERENCIA_SITIO_COMUNITARIO,
            latitud = null,
            longitud = null,
            precisionMetros = 0,
            antiguedadUbicacionSeg = 0,
            referenciaSitioCompacta = "PUENTE-VIEJO-KM14",
            tipoNecesidad = TipoNecesidad.RESCATE_BRIGADA,
            cantidadSolicitada = 10,
            cantidadAceptadaTotal = 4,
            cantidadPendiente = 6,
            personasAfectadas = 20,
            urgencia = UrgenciaDeclarada.CRITICA_VITAL,
            estadoGeneral = EstadoSolicitud.ACEPTADO_PARCIAL,
            motivo = MotivoCapacidad.ESCALAMIENTO_PARCIAL_RESIDUAL,
            vencimientoRestanteSeg = 180,
            responsableORecursoCompacto = "RESP-MUN",
            keyId = 201
        )
        val (bytes, _) = NexoPacketCodec.encode(frame)
        // Alterar un byte del cuerpo (ej. cantidad o estado)
        bytes[25] = (bytes[25].toInt() xor 0xFF).toByte()
        NexoPacketCodec.decodeAndVerify(bytes)
    }
}
