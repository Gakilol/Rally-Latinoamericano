package ni.edu.rally.nexosos.datalocal.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NexoDao {

    @Query("SELECT * FROM instituciones ORDER BY nivelOrden ASC")
    fun observeInstituciones(): Flow<List<InstitucionEntity>>

    @Query("SELECT * FROM instituciones ORDER BY nivelOrden ASC")
    suspend fun getInstituciones(): List<InstitucionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstituciones(items: List<InstitucionEntity>)

    @Query("SELECT * FROM territorios ORDER BY nivelOrden ASC")
    fun observeTerritorios(): Flow<List<TerritorioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerritorios(items: List<TerritorioEntity>)

    @Query("SELECT * FROM solicitudes ORDER BY fechaCreacionEpochMs DESC")
    fun observeSolicitudes(): Flow<List<SolicitudEntity>>

    @Query("SELECT * FROM solicitudes WHERE uuidEvento = :uuid LIMIT 1")
    suspend fun getSolicitudByUuid(uuid: String): SolicitudEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSolicitud(item: SolicitudEntity)

    @Query("SELECT * FROM necesidades")
    fun observeNecesidades(): Flow<List<NecesidadEntity>>

    @Query("SELECT * FROM necesidades WHERE uuidEvento = :uuid LIMIT 1")
    suspend fun getNecesidadByUuid(uuid: String): NecesidadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNecesidad(item: NecesidadEntity)

    @Query("SELECT * FROM eventos_estado ORDER BY timestampEpochMs ASC")
    fun observeEventosEstado(): Flow<List<EventoEstadoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventoEstado(item: EventoEstadoEntity)

    @Query("SELECT * FROM asignaciones ORDER BY plazoSeguimientoEpochMs ASC")
    fun observeAsignaciones(): Flow<List<AsignacionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAsignacion(item: AsignacionEntity)

    @Query("SELECT * FROM reservas ORDER BY nivelOrden ASC, nombreRecurso ASC")
    fun observeReservas(): Flow<List<ReservaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReservas(items: List<ReservaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReserva(item: ReservaEntity)

    @Query("SELECT * FROM outbox ORDER BY creadoEpochMs DESC")
    fun observeOutbox(): Flow<List<OutboxEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOutbox(item: OutboxEntity)

    @Query("SELECT * FROM vencimientos")
    fun observeVencimientos(): Flow<List<VencimientoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVencimiento(item: VencimientoEntity)

    @Query("SELECT * FROM reportes_viales ORDER BY timestampEpochMs DESC")
    fun observeReportesViales(): Flow<List<ReporteVialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReportesViales(items: List<ReporteVialEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReporteVial(item: ReporteVialEntity)

    @Query("SELECT * FROM centros_atencion ORDER BY nivelOrden ASC")
    fun observeCentrosAtencion(): Flow<List<CentroAtencionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCentrosAtencion(items: List<CentroAtencionEntity>)

    /**
     * Transacción atómica exigida en Anexo F y G:
     * Guarda en una sola operación el estado de la solicitud, la necesidad, el evento de bitácora,
     * el registro en la cola Outbox, el vencimiento persistido y (si aplica) la reserva/asignación.
     */
    @Transaction
    suspend fun guardarTransicionYOutboxAtomico(
        solicitud: SolicitudEntity,
        necesidad: NecesidadEntity,
        evento: EventoEstadoEntity,
        outbox: OutboxEntity?,
        vencimiento: VencimientoEntity?,
        asignacion: AsignacionEntity? = null,
        reserva: ReservaEntity? = null
    ) {
        upsertSolicitud(solicitud)
        upsertNecesidad(necesidad)
        insertEventoEstado(evento)
        if (outbox != null) {
            upsertOutbox(outbox)
        }
        if (vencimiento != null) {
            upsertVencimiento(vencimiento)
        }
        if (asignacion != null) {
            upsertAsignacion(asignacion)
        }
        if (reserva != null) {
            upsertReserva(reserva)
        }
    }
}
