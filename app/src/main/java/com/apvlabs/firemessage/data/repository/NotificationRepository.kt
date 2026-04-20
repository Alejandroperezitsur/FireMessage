package com.apvlabs.firemessage.data.repository

import com.apvlabs.firemessage.data.local.NotificationDao
import com.apvlabs.firemessage.data.local.NotificationCache
import com.apvlabs.firemessage.data.local.entity.NotificationEntity
import com.apvlabs.firemessage.data.model.Notification
import com.apvlabs.firemessage.data.remote.FirestoreNotificationService
import com.apvlabs.firemessage.data.remote.NotificationApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repositorio profesional para gestión de notificaciones
 * Combina Room (offline), Firestore (global sync), API (backend), y Analytics
 */
class NotificationRepository(
    private val notificationCache: NotificationCache,
    private val notificationApiService: NotificationApiService,
    private val notificationDao: NotificationDao,
    private val firestoreNotificationService: FirestoreNotificationService,
    private val analyticsService: AnalyticsService
) {
    
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()
    
    /**
     * Obtiene todas las notificaciones de Room
     */
    fun getNotificationsFlow(): Flow<List<Notification>> {
        return notificationDao.getAllNotifications()
    }
    
    /**
     * Guarda una notificación en Room (offline) y Firestore (global sync)
     */
    suspend fun saveNotification(notification: Notification) {
        // Guardar en Room para persistencia offline
        val entity = NotificationEntity(
            id = notification.id,
            title = notification.title,
            body = notification.body,
            type = notification.type,
            priority = notification.priority,
            isRead = notification.isRead,
            timestamp = notification.timestamp,
            syncedWithFirestore = false,
            data = notification.data
        )
        notificationDao.insertNotification(entity)
        
        // Intentar guardar en Firestore para sync global (no bloquear si falla)
        try {
            firestoreNotificationService.saveNotification(notification)
            notificationDao.markAsSynced(notification.id)
        } catch (e: Exception) {
            // Se marcará como no sincronizado para retry con WorkManager
        }
    }
    
    /**
     * Marca una notificación como leída y tracking de analytics
     */
    suspend fun markAsRead(notificationId: String, notificationType: String? = null, userId: String? = null) {
        notificationDao.markAsRead(notificationId)
        
        // Tracking de interacción (no bloquear si falla)
        if (notificationType != null && userId != null) {
            try {
                analyticsService.trackNotificationOpened(notificationId, notificationType, userId)
            } catch (e: Exception) {
                // Ignorar errores de tracking
            }
        }
    }
    
    /**
     * Marca todas como leídas
     */
    suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
    }
    
    /**
     * Elimina una notificación con tracking
     */
    suspend fun deleteNotification(notificationId: String, notificationType: String? = null, userId: String? = null) {
        notificationDao.deleteNotification(notificationId)
        
        // Tracking de interacción
        if (notificationType != null && userId != null) {
            try {
                analyticsService.trackNotificationDismissed(notificationId, notificationType, userId)
            } catch (e: Exception) {
                // Ignorar errores de tracking
            }
        }
    }
    
    /**
     * Obtiene count de notificaciones no leídas
     */
    suspend fun getUnreadCount(): Int {
        return notificationDao.getUnreadCount()
    }
}
