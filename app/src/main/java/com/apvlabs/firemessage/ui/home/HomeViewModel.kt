package com.apvlabs.firemessage.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apvlabs.firemessage.data.model.User
import com.apvlabs.firemessage.data.repository.FcmTokenRepository
import com.apvlabs.firemessage.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla principal
 * Maneja el token FCM y información del usuario
 */
class HomeViewModel(
    private val userRepository: UserRepository,
    private val fcmTokenRepository: FcmTokenRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()
    
    /**
     * Inicializar y obtener token FCM
     */
    fun initializeFcmToken(userId: String) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            val result = fcmTokenRepository.getFcmToken()
            if (result.isSuccess) {
                val token = result.getOrNull()!!
                _fcmToken.value = token
                
                // Actualizar token en Firestore
                val updateResult = userRepository.updateFcmToken(userId, token)
                if (updateResult.isSuccess) {
                    _uiState.value = HomeUiState.TokenUpdated(token)
                } else {
                    _uiState.value = HomeUiState.Error(
                        updateResult.exceptionOrNull()?.message ?: "Error al actualizar token"
                    )
                }
            } else {
                _uiState.value = HomeUiState.Error(
                    result.exceptionOrNull()?.message ?: "Error al obtener token FCM"
                )
            }
        }
    }
    
    /**
     * Suscribirse a tópico basado en rol
     */
    fun subscribeToRoleTopic(role: String) {
        viewModelScope.launch {
            val topic = "role_$role"
            fcmTokenRepository.subscribeToTopic(topic)
        }
    }
    
    /**
     * Suscribirse a tópico basado en carrera
     */
    fun subscribeToCareerTopic(career: String) {
        viewModelScope.launch {
            val topic = "career_${career.lowercase()}"
            fcmTokenRepository.subscribeToTopic(topic)
        }
    }
    
    /**
     * Resetear estado de UI
     */
    fun resetUiState() {
        _uiState.value = HomeUiState.Idle
    }
}

/**
 * Estados de UI para Home
 */
sealed class HomeUiState {
    object Idle : HomeUiState()
    object Loading : HomeUiState()
    data class TokenUpdated(val token: String) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
