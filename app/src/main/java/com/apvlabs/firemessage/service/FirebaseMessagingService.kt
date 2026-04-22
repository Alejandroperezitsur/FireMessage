package com.apvlabs.firemessage.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.apvlabs.firemessage.MainActivity
import com.apvlabs.firemessage.R
import com.apvlabs.firemessage.data.local.NotificationCache
import com.apvlabs.firemessage.data.model.Notification
import com.apvlabs.firemessage.data.model.NotificationType
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Servicio de Firebase Cloud Messaging
 * Maneja la recepción de notificaciones push
 */
class FirebaseMessagingService : FirebaseMessagingService() {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var notificationCache: NotificationCache
    
    companion object {
        private const val CHANNEL_ID = "alerta_campus_channel"
        private const val CHANNEL_NAME = "AlertaCampus"
        private const val CHANNEL_DESCRIPTION = "Notificaciones de AlertaCampus"
    }
    
    override fun onCreate() {
        super.onCreate()
        notificationCache = NotificationCache(applicationContext)
        createNotificationChannel()
    }
    
    /**
     * Called when a new message is received
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "AlertaCampus"
            val body = notification.body ?: ""
            val type = remoteMessage.data["type"] ?: "MENSAJE_GENERAL"
            
            // Mostrar notificación en sistema
            showSystemNotification(title, body, type)
            
            // Guardar en caché local
            saveNotificationLocally(title, body, type, remoteMessage.data)
        }
    }
    
    /**
     * Called when FCM token is refreshed
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Aquí se debería enviar el nuevo token al backend
        // Esto se manejará en el HomeViewModel
    }
    
    /**
     * Crear canal de notificación (requerido para Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Mostrar notificación del sistema
     */
    private fun showSystemNotification(title: String, body: String, type: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", type)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(getNotificationIcon(type))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
    
    /**
     * Guardar notificación en caché local
     */
    private fun saveNotificationLocally(
        title: String,
        body: String,
        type: String,
        data: Map<String, String>
    ) {
        serviceScope.launch {
            val notification = Notification(
                id = UUID.randomUUID().toString(),
                title = title,
                body = body,
                type = getNotificationType(type),
                isRead = false,
                data = data
            )
            notificationCache.saveNotification(notification)
        }
    }
    
    /**
     * Obtener tipo de notificación desde string
     */
    private fun getNotificationType(type: String): NotificationType {
        return try {
            NotificationType.valueOf(type)
        } catch (e: IllegalArgumentException) {
            NotificationType.MENSAJE_GENERAL
        }
    }
    
    /**
     * Obtener icono según tipo de notificación
     */
    private fun getNotificationIcon(type: String): Int {
        return when (getNotificationType(type)) {
            NotificationType.CAFETERIA_READY -> R.drawable.ic_restaurant
            NotificationType.ALERTA_IMPORTANTE -> R.drawable.ic_alert
            NotificationType.EVENTO -> R.drawable.ic_event
            NotificationType.MENSAJE_GENERAL -> R.drawable.ic_notification
        }
    }
}
