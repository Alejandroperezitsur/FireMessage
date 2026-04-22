package com.apvlabs.firemessage.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apvlabs.firemessage.data.model.Notification
import com.apvlabs.firemessage.data.repository.NotificationRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para notificaciones
 * Maneja el estado de las notificaciones
 */
class NotificationViewModel(
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    
    // Usa Flow del repositorio para actualizaciones en tiempo real
    val notifications: StateFlow<List<Notification>> = notificationRepository.notifications
    
    /**
     * Guardar nueva notificación
     */
    fun saveNotification(notification: Notification) {
        viewModelScope.launch {
            notificationRepository.saveNotification(notification)
        }
    }
    
    /**
     * Marcar notificación como leída
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }
    
    /**
     * Marcar todas como leídas
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
        }
    }
    
    /**
     * Eliminar notificación
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
        }
    }
    
    /**
     * Obtiene count de no leídas
     */
    suspend fun getUnreadCount(): Int {
        return notificationRepository.getUnreadCount()
    }
}
