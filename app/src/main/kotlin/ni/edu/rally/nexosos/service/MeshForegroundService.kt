package ni.edu.rally.nexosos.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Servicio en primer plano de tipo connectedDevice (Anexo F y Anexo J).
 * Mantiene la sesión BLE/Serie hacia el radio Meshtastic con notificación visible
 * sin prometer supervivencia si el sistema operativo o el usuario apagan el teléfono
 * (las pasarelas institucionales persisten los plazos de las solicitudes recibidas).
 */
class MeshForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "nexo_sos_mesh_channel"
        const val NOTIFICATION_ID = 25601
        const val EXTRA_STATUS_TEXT = "extra_status_text"
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val statusText = intent?.getStringExtra(EXTRA_STATUS_TEXT)
            ?: "Enlace radio Meshtastic (PRIVATE_APP=256) y cola Outbox supervisados."
        val notification = buildNotification(statusText)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nexo SOS · Enlace Radio Meshtastic",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Supervisión de radio BLE/LoRa y cola local Outbox sin conexión"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("Nexo SOS · Enlace Meshtastic Activo")
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }
}
