package com.apvlabs.firemessage.data.repository

import com.apvlabs.firemessage.data.local.NotificationCache
import com.apvlabs.firemessage.data.model.Notification
import com.apvlabs.firemessage.data.remote.FirestoreNotificationService
import com.apvlabs.firemessage.data.remote.NotificationApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repositorio simplificado para gestión de notificaciones
 * Temporarily without Room - using cache only
 */
class NotificationRepository(
    private val notificationCache: NotificationCache,
    private val notificationApiService: NotificationApiService,
    private val firestoreNotificationService: FirestoreNotificationService
) {
    
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()
    
    /**
     * Obtiene todas las notificaciones del caché
     */
    fun getNotificationsFlow(): Flow<List<Notification>> {
        return _notifications.asStateFlow()
    }
    
    /**
     * Guarda una notificación en caché
     */
    suspend fun saveNotification(notification: Notification) {
        notificationCache.saveNotification(notification)
        _notifications.value = notificationCache.getNotifications()
    }
    
    /**
     * Marca notificación como leída
     */
    suspend fun markAsRead(notificationId: String) {
        notificationCache.markAsRead(notificationId)
        _notifications.value = notificationCache.getNotifications()
    }
    
    /**
     * Marca todas como leídas
     */
    suspend fun markAllAsRead() {
        notificationCache.markAllAsRead()
        _notifications.value = notificationCache.getNotifications()
    }
    
    /**
     * Elimina notificación
     */
    suspend fun deleteNotification(notificationId: String) {
        notificationCache.deleteNotification(notificationId)
        _notifications.value = notificationCache.getNotifications()
    }
    
    /**
     * Obtiene count de no leídas
     */
    suspend fun getUnreadCount(): Int {
        return notificationCache.getUnreadCount()
    }
}
