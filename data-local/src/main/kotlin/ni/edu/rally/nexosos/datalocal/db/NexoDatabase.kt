package ni.edu.rally.nexosos.datalocal.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        InstitucionEntity::class,
        TerritorioEntity::class,
        SolicitudEntity::class,
        NecesidadEntity::class,
        EventoEstadoEntity::class,
        AsignacionEntity::class,
        ReservaEntity::class,
        OutboxEntity::class,
        VencimientoEntity::class,
        ReporteVialEntity::class,
        CentroAtencionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NexoDatabase : RoomDatabase() {
    abstract fun nexoDao(): NexoDao

    companion object {
        @Volatile
        private var INSTANCE: NexoDatabase? = null

        fun getInstance(context: Context): NexoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexoDatabase::class.java,
                    "nexo_sos_offline.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
