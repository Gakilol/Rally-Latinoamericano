package ni.edu.rally.nexosos.protocol

import ni.edu.rally.nexosos.domain.model.*
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.roundToInt

/**
 * Excepciones de protocolo verificables (Anexos E, H y J).
 */
class PacketSizeExceededException(val actualBytes: Int, val maxBytes: Int = TipoMensajeApp.MAX_SERIALIZED_BYTES) :
    IllegalArgumentException("El paquete serializado ($actualBytes B) excede el límite de ingeniería de $maxBytes B para LoRa.")

class PacketAuthenticationException(message: String) : SecurityException(message)

/**
 * Representación de una trama compacta de aplicación Nexo SOS sobre Meshtastic PRIVATE_APP = 256.
 * Incluye todos los campos mínimos del Anexo E y autenticador de aplicación de 16 bytes (Anexo H).
 */
data class NexoCompactFrame(
    val protocolVersion: Int = 1,
    val portNum: Int = TipoMensajeApp.MESHTASTIC_PRIVATE_APP_PORTNUM,
    val tipoMensaje: TipoMensajeApp,
    val uuidEvento: String,
    val idMensajeCorto: Int,
    val revision: Int,
    val institucionOrigenCodigo: String,
    val institucionDestinoCodigo: String,
    val nivelOrigen: NivelTerritorial,
    val nivelDestino: NivelTerritorial,
    val timestampEpochSec: Long,
    val calidadReloj: CalidadReloj,
    val fuenteUbicacion: FuenteUbicacion,
    val latitud: Double?,
    val longitud: Double?,
    val precisionMetros: Int,
    val antiguedadUbicacionSeg: Int,
    val referenciaSitioCompacta: String,
    val tipoNecesidad: TipoNecesidad,
    val cantidadSolicitada: Int,
    val cantidadAceptadaTotal: Int,
    val cantidadPendiente: Int,
    val personasAfectadas: Int,
    val urgencia: UrgenciaDeclarada,
    val estadoGeneral: EstadoSolicitud,
    val motivo: MotivoCapacidad,
    val vencimientoRestanteSeg: Int,
    val responsableORecursoCompacto: String,
    val keyId: Int,
    val hmacAuthenticatorHex: String = ""
)

/**
 * Directorio de claves pre-provisionadas de institución/rol para el piloto (Anexo H y J).
 * En Android se respaldan en AndroidKeyStore; aquí se provee el verificador criptográfico determinista.
 */
object KeyDirectory {
    private val provisionedKeys = mapOf(
        101 to "NEXO-KEY-COMARCA-BRIGADA-2026-NIC".toByteArray(StandardCharsets.UTF_8),
        102 to "NEXO-KEY-COMARCA-AUTORIDAD-2026-NIC".toByteArray(StandardCharsets.UTF_8),
        201 to "NEXO-KEY-MUNICIPIO-COORD-2026-NIC".toByteArray(StandardCharsets.UTF_8),
        301 to "NEXO-KEY-DEPARTAMENTO-CAB-2026-NIC".toByteArray(StandardCharsets.UTF_8),
        401 to "NEXO-KEY-MANAGUA-CENTRAL-2026-NIC".toByteArray(StandardCharsets.UTF_8),
        999 to "NEXO-KEY-PASARELA-ADMIN-2026-NIC".toByteArray(StandardCharsets.UTF_8)
    )

    fun isAuthorizedKey(keyId: Int): Boolean = provisionedKeys.containsKey(keyId)

    fun secretForKey(keyId: Int): ByteArray =
        provisionedKeys[keyId] ?: throw PacketAuthenticationException("Emisor no autorizado: keyId=$keyId desconocido.")
}

/**
 * Codificador y decodificador binario compacto para Nexo SOS (Anexo E).
 *
 * Garantiza:
 * - Uso de PortNum PRIVATE_APP = 256.
 * - Tamaño total serializado <= 160 bytes (incluyendo los 16 bytes del autenticador HMAC-SHA256).
 * - Rechazo explícito de paquetes modificados, emisores no autorizados o tamaño excesivo.
 */
object NexoPacketCodec {

    private const val MAGIC_BYTE_0: Byte = 0x4E // 'N'
    private const val MAGIC_BYTE_1: Byte = 0x58 // 'X'
    private const val HMAC_TRUNCATED_LEN = 16

    fun encode(frame: NexoCompactFrame): Pair<ByteArray, String> {
        val secret = KeyDirectory.secretForKey(frame.keyId)

        val origBytes = truncateUtf8(frame.institucionOrigenCodigo, 8)
        val destBytes = truncateUtf8(frame.institucionDestinoCodigo, 8)
        val refBytes = truncateUtf8(frame.referenciaSitioCompacta, 24)
        val respBytes = truncateUtf8(frame.responsableORecursoCompacto, 18)

        val uuidBytes = uuidTo16Bytes(frame.uuidEvento)
        val bodySize = 2 + 1 + 1 + 16 + 4 + 2 + 2 + 4 + 2 + 1 + 1 + 1 +
            (1 + origBytes.size) +
            (1 + destBytes.size) +
            1 + 4 + 4 + 2 + 2 +
            (1 + refBytes.size) +
            1 + 2 + 2 + 2 + 2 + 1 + 1 + 1 +
            (1 + respBytes.size)

        val totalSize = bodySize + HMAC_TRUNCATED_LEN
        if (totalSize > TipoMensajeApp.MAX_SERIALIZED_BYTES) {
            throw PacketSizeExceededException(totalSize)
        }

        val buffer = ByteBuffer.allocate(totalSize)
        buffer.put(MAGIC_BYTE_0)
        buffer.put(MAGIC_BYTE_1)
        buffer.put(frame.protocolVersion.toByte())
        buffer.put(frame.tipoMensaje.codigo.toByte())
        buffer.put(uuidBytes)
        buffer.putInt(frame.idMensajeCorto)
        buffer.putShort(frame.revision.coerceIn(0, 65535).toShort())
        buffer.putShort(frame.keyId.coerceIn(0, 65535).toShort())
        buffer.putInt(frame.timestampEpochSec.toInt())
        buffer.putShort(frame.vencimientoRestanteSeg.coerceIn(0, 65535).toShort())
        buffer.put(frame.nivelOrigen.orden.toByte())
        buffer.put(frame.nivelDestino.orden.toByte())
        buffer.put(frame.calidadReloj.codigo.toByte())

        buffer.put(origBytes.size.toByte())
        buffer.put(origBytes)
        buffer.put(destBytes.size.toByte())
        buffer.put(destBytes)

        buffer.put(frame.fuenteUbicacion.codigo.toByte())
        val latE6 = frame.latitud?.let { (it * 1_000_000.0).roundToInt() } ?: Int.MIN_VALUE
        val lonE6 = frame.longitud?.let { (it * 1_000_000.0).roundToInt() } ?: Int.MIN_VALUE
        buffer.putInt(latE6)
        buffer.putInt(lonE6)
        buffer.putShort(frame.precisionMetros.coerceIn(0, 65535).toShort())
        buffer.putShort(frame.antiguedadUbicacionSeg.coerceIn(0, 65535).toShort())

        buffer.put(refBytes.size.toByte())
        buffer.put(refBytes)

        buffer.put(frame.tipoNecesidad.codigo.toByte())
        buffer.putShort(frame.cantidadSolicitada.coerceIn(0, 32767).toShort())
        buffer.putShort(frame.cantidadAceptadaTotal.coerceIn(0, 32767).toShort())
        buffer.putShort(frame.cantidadPendiente.coerceIn(0, 32767).toShort())
        buffer.putShort(frame.personasAfectadas.coerceIn(0, 32767).toShort())
        buffer.put(frame.urgencia.codigo.toByte())
        buffer.put(frame.estadoGeneral.ordinal.toByte())
        buffer.put(frame.motivo.codigo.toByte())

        buffer.put(respBytes.size.toByte())
        buffer.put(respBytes)

        val bodyBytes = ByteArray(bodySize)
        buffer.position(0)
        buffer.get(bodyBytes)

        val mac16 = computeHmac16(secret, bodyBytes)
        buffer.position(bodySize)
        buffer.put(mac16)

        val finalPacket = buffer.array()
        if (finalPacket.size > TipoMensajeApp.MAX_SERIALIZED_BYTES) {
            throw PacketSizeExceededException(finalPacket.size)
        }
        return finalPacket to mac16.toHex()
    }

    fun decodeAndVerify(rawBytes: ByteArray): NexoCompactFrame {
        if (rawBytes.size > TipoMensajeApp.MAX_SERIALIZED_BYTES) {
            throw PacketSizeExceededException(rawBytes.size)
        }
        if (rawBytes.size < 45 + HMAC_TRUNCATED_LEN) {
            throw PacketAuthenticationException("Trama demasiado corta (${rawBytes.size} B).")
        }
        val buf = ByteBuffer.wrap(rawBytes)
        val m0 = buf.get()
        val m1 = buf.get()
        if (m0 != MAGIC_BYTE_0 || m1 != MAGIC_BYTE_1) {
            throw PacketAuthenticationException("Cabecera mágica inválida en paquete Meshtastic PRIVATE_APP.")
        }

        val version = buf.get().toInt() and 0xFF
        val tipoMensaje = TipoMensajeApp.fromCodigo(buf.get().toInt() and 0xFF)
        val uuidBytes = ByteArray(16)
        buf.get(uuidBytes)
        val uuidStr = bytes16ToUuidString(uuidBytes)

        val idMensajeCorto = buf.int
        val revision = buf.short.toInt() and 0xFFFF
        val keyId = buf.short.toInt() and 0xFFFF

        if (!KeyDirectory.isAuthorizedKey(keyId)) {
            throw PacketAuthenticationException("Clave de aplicación no autorizada: keyId=$keyId")
        }

        val bodyLength = rawBytes.size - HMAC_TRUNCATED_LEN
        val bodyBytes = rawBytes.copyOfRange(0, bodyLength)
        val receivedMac = rawBytes.copyOfRange(bodyLength, rawBytes.size)
        val expectedMac = computeHmac16(KeyDirectory.secretForKey(keyId), bodyBytes)
        if (!MessageDigest.isEqual(receivedMac, expectedMac)) {
            throw PacketAuthenticationException("Fallo de autenticador HMAC-SHA256: paquete modificado o firma inválida.")
        }

        val timestampEpochSec = buf.int.toLong() and 0xFFFFFFFFL
        val vencimientoRestanteSeg = buf.short.toInt() and 0xFFFF
        val nivelOrigen = NivelTerritorial.fromOrden(buf.get().toInt() and 0xFF)
        val nivelDestino = NivelTerritorial.fromOrden(buf.get().toInt() and 0xFF)
        val calidadReloj = CalidadReloj.fromCodigo(buf.get().toInt() and 0xFF)

        val origLen = buf.get().toInt() and 0xFF
        val origBytes = ByteArray(origLen).also { buf.get(it) }
        val destLen = buf.get().toInt() and 0xFF
        val destBytes = ByteArray(destLen).also { buf.get(it) }

        val fuenteUbicacion = FuenteUbicacion.fromCodigo(buf.get().toInt() and 0xFF)
        val latE6 = buf.int
        val lonE6 = buf.int
        val latitud = if (latE6 == Int.MIN_VALUE) null else latE6 / 1_000_000.0
        val longitud = if (lonE6 == Int.MIN_VALUE) null else lonE6 / 1_000_000.0
        val precisionMetros = buf.short.toInt() and 0xFFFF
        val antiguedadSeg = buf.short.toInt() and 0xFFFF

        val refLen = buf.get().toInt() and 0xFF
        val refBytes = ByteArray(refLen).also { buf.get(it) }

        val tipoNecesidad = TipoNecesidad.fromCodigo(buf.get().toInt() and 0xFF)
        val cantSolicitada = buf.short.toInt() and 0xFFFF
        val cantAceptada = buf.short.toInt() and 0xFFFF
        val cantPendiente = buf.short.toInt() and 0xFFFF
        val personasAfectadas = buf.short.toInt() and 0xFFFF
        val urgencia = UrgenciaDeclarada.fromCodigo(buf.get().toInt() and 0xFF)
        val estadoOrd = (buf.get().toInt() and 0xFF).coerceIn(0, EstadoSolicitud.entries.size - 1)
        val estadoGeneral = EstadoSolicitud.entries[estadoOrd]
        val motivo = MotivoCapacidad.fromCodigo(buf.get().toInt() and 0xFF)

        val respLen = buf.get().toInt() and 0xFF
        val respBytes = ByteArray(respLen).also { buf.get(it) }

        return NexoCompactFrame(
            protocolVersion = version,
            portNum = TipoMensajeApp.MESHTASTIC_PRIVATE_APP_PORTNUM,
            tipoMensaje = tipoMensaje,
            uuidEvento = uuidStr,
            idMensajeCorto = idMensajeCorto,
            revision = revision,
            institucionOrigenCodigo = String(origBytes, StandardCharsets.UTF_8),
            institucionDestinoCodigo = String(destBytes, StandardCharsets.UTF_8),
            nivelOrigen = nivelOrigen,
            nivelDestino = nivelDestino,
            timestampEpochSec = timestampEpochSec,
            calidadReloj = calidadReloj,
            fuenteUbicacion = fuenteUbicacion,
            latitud = latitud,
            longitud = longitud,
            precisionMetros = precisionMetros,
            antiguedadUbicacionSeg = antiguedadSeg,
            referenciaSitioCompacta = String(refBytes, StandardCharsets.UTF_8),
            tipoNecesidad = tipoNecesidad,
            cantidadSolicitada = cantSolicitada,
            cantidadAceptadaTotal = cantAceptada,
            cantidadPendiente = cantPendiente,
            personasAfectadas = personasAfectadas,
            urgencia = urgencia,
            estadoGeneral = estadoGeneral,
            motivo = motivo,
            vencimientoRestanteSeg = vencimientoRestanteSeg,
            responsableORecursoCompacto = String(respBytes, StandardCharsets.UTF_8),
            keyId = keyId,
            hmacAuthenticatorHex = receivedMac.toHex()
        )
    }

    /**
     * Construye una trama compacta desde una Solicitud y su Necesidad actual.
     */
    fun fromSolicitud(
        solicitud: Solicitud,
        necesidad: Necesidad,
        tipoMensaje: TipoMensajeApp,
        nivelOrigen: NivelTerritorial,
        nivelDestino: NivelTerritorial,
        keyId: Int,
        responsableCompacto: String = "",
        vencimientoRestanteSeg: Int = 180
    ): NexoCompactFrame {
        return NexoCompactFrame(
            tipoMensaje = tipoMensaje,
            uuidEvento = solicitud.uuidEvento,
            idMensajeCorto = (solicitud.uuidEvento.hashCode() xor solicitud.revision),
            revision = solicitud.revision,
            institucionOrigenCodigo = solicitud.institucionOrigenId.take(8),
            institucionDestinoCodigo = solicitud.institucionDestinoActualId.take(8),
            nivelOrigen = nivelOrigen,
            nivelDestino = nivelDestino,
            timestampEpochSec = solicitud.fechaCreacionEpochMs / 1000L,
            calidadReloj = solicitud.calidadReloj,
            fuenteUbicacion = solicitud.fuenteUbicacion,
            latitud = solicitud.latitud,
            longitud = solicitud.longitud,
            precisionMetros = solicitud.precisionMetros ?: 0,
            antiguedadUbicacionSeg = solicitud.antiguedadUbicacionSeg ?: 0,
            referenciaSitioCompacta = solicitud.referenciaSitio.take(24),
            tipoNecesidad = necesidad.tipo,
            cantidadSolicitada = necesidad.cantidadSolicitada,
            cantidadAceptadaTotal = necesidad.cantidadAceptadaTotal,
            cantidadPendiente = necesidad.cantidadPendiente,
            personasAfectadas = solicitud.personasAfectadas,
            urgencia = solicitud.urgencia,
            estadoGeneral = solicitud.estadoGeneral,
            motivo = solicitud.motivoUltimoCambio,
            vencimientoRestanteSeg = vencimientoRestanteSeg,
            responsableORecursoCompacto = responsableCompacto.take(18),
            keyId = keyId
        )
    }

    private fun computeHmac16(secret: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret, "HmacSHA256"))
        return mac.doFinal(data).copyOfRange(0, HMAC_TRUNCATED_LEN)
    }

    private fun truncateUtf8(input: String, maxBytes: Int): ByteArray {
        val raw = input.toByteArray(StandardCharsets.UTF_8)
        return if (raw.size <= maxBytes) raw else raw.copyOfRange(0, maxBytes)
    }

    private fun uuidTo16Bytes(uuidStr: String): ByteArray {
        return try {
            val u = UUID.fromString(uuidStr)
            ByteBuffer.allocate(16)
                .putLong(u.mostSignificantBits)
                .putLong(u.leastSignificantBits)
                .array()
        } catch (_: Exception) {
            MessageDigest.getInstance("MD5")
                .digest(uuidStr.toByteArray(StandardCharsets.UTF_8))
                .copyOfRange(0, 16)
        }
    }

    private fun bytes16ToUuidString(bytes: ByteArray): String {
        val buf = ByteBuffer.wrap(bytes)
        return UUID(buf.long, buf.long).toString()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
