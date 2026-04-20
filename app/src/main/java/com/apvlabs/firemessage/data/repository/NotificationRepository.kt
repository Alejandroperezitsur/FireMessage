package com.apvlabs.firemessage.data.repository

import com.apvlabs.firemessage.data.local.NotificationCache
import com.apvlabs.firemessage.data.model.Notification
import com.apvlabs.firemessage.data.remote.NotificationApiService
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de notificaciones - implementa patrón Repository
 * Centraliza toda la lógica de acceso a datos de notificaciones
 */
class NotificationRepository(
    private val cache: NotificationCache,
    private val apiService: NotificationApiService
) {
    
    /**
     * Obtener notificaciones desde caché (offline-first)
     */
    fun getNotifications(): Flow<List<Notification>> {
        return cache.getNotifications()
    }
    
    /**
     * Guardar notificación en caché
     */
    suspend fun saveNotification(notification: Notification) {
        cache.addNotification(notification)
    }
    
    /**
     * Marcar notificación como leída
     */
    suspend fun markAsRead(notificationId: String) {
        cache.markAsRead(notificationId)
    }
    
    /**
     * Enviar notificación al backend
     */
    suspend fun sendNotification(
        token: String?,
        title: String,
        body: String,
        type: String,
        targetRole: String? = null,
        targetCareer: String? = null,
        sendToAll: Boolean = false
    ): Result<Unit> {
        return try {
            val request = com.apvlabs.firemessage.data.model.NotificationRequest(
                token = token,
                title = title,
                body = body,
                type = type,
                targetRole = targetRole,
                targetCareer = targetCareer,
                sendToAll = sendToAll
            )
            apiService.sendNotification(request)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Limpiar caché de notificaciones
     */
    suspend fun clearCache() {
        cache.clearCache()
    }
}
