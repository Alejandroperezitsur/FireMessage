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
    
    // Usa Flow directo de Room para actualizaciones en tiempo real
    val notifications = notificationRepository.getNotificationsFlow()
    
    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Idle)
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()
    
    init {
        loadNotifications()
    }
    
    /**
     * Cargar notificaciones desde caché
     */
    private fun loadNotifications() {
        viewModelScope.launch {
            notificationRepository.getNotifications().collect { notificationList ->
                // No es necesario actualizar _notifications ya que se usa el flow de Room
            }
        }
    }
    
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
            _uiState.value = NotificationUiState.Loading
            val result = notificationRepository.markAsRead(notificationId)
            if (result.isSuccess) {
                _uiState.value = NotificationUiState.Idle
            } else {
                _uiState.value = NotificationUiState.Error(
                    result.exceptionOrNull()?.message ?: "Error al marcar como leída"
                )
            }
        }
    }
    
    /**
     * Enviar notificación
     */
    fun sendNotification(
        token: String?,
        title: String,
        body: String,
        type: String,
        targetRole: String? = null,
        targetCareer: String? = null,
        sendToAll: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.value = NotificationUiState.Loading
            val result = notificationRepository.sendNotification(
                token, title, body, type, targetRole, targetCareer, sendToAll
            )
            if (result.isSuccess) {
                _uiState.value = NotificationUiState.Success
            } else {
                _uiState.value = NotificationUiState.Error(
                    result.exceptionOrNull()?.message ?: "Error al enviar notificación"
                )
            }
        }
    }
    
    /**
     * Limpiar caché de notificaciones
     */
    fun clearCache() {
        viewModelScope.launch {
            notificationRepository.clearCache()
        }
    }
    
    /**
     * Resetear estado de UI
     */
    fun resetUiState() {
        _uiState.value = NotificationUiState.Idle
    }
}

/**
 * Estados de UI para notificaciones
 */
sealed class NotificationUiState {
    object Idle : NotificationUiState()
    object Loading : NotificationUiState()
    object Success : NotificationUiState()
    data class Error(val message: String) : NotificationUiState()
}
