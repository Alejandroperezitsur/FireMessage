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
 * ViewModel para la pantalla principal con suscripción profesional a topics
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
     * Inicializa el token FCM y lo actualiza en Firestore
     * Suscribe a topics profesionales: role, career, combined, all_users
     */
    fun initializeFcmToken(userId: String) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            
            try {
                val token = fcmTokenRepository.getFcmToken()
                if (token != null) {
                    _fcmToken.value = token
                    userRepository.updateFcmToken(userId, token)
                    _uiState.value = HomeUiState.Success("Token FCM actualizado")
                } else {
                    _uiState.value = HomeUiState.Error("No se pudo obtener el token FCM")
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Error al inicializar token FCM")
            }
        }
    }
    
    /**
     * Suscribe al tópico de rol (role_alumno o role_profesor)
     */
    fun subscribeToRoleTopic(role: String) {
        viewModelScope.launch {
            try {
                fcmTokenRepository.subscribeToRoleTopic(role)
            } catch (e: Exception) {
                // Silencioso, no afecta UX
            }
        }
    }
    
    /**
     * Suscribe al tópico de carrera (career_sistemas, career_medicina, etc.)
     */
    fun subscribeToCareerTopic(career: String) {
        viewModelScope.launch {
            try {
                fcmTokenRepository.subscribeToCareerTopic(career)
            } catch (e: Exception) {
                // Silencioso, no afecta UX
            }
        }
    }
    
    /**
     * Suscribe al tópico combinado (role_career)
     */
    fun subscribeToCombinedTopic(role: String, career: String) {
        viewModelScope.launch {
            try {
                fcmTokenRepository.subscribeToCombinedTopic(role, career)
            } catch (e: Exception) {
                // Silencioso, no afecta UX
            }
        }
    }
    
    /**
     * Suscribe al tópico global para todos los usuarios
     */
    fun subscribeToAllUsers() {
        viewModelScope.launch {
            try {
                fcmTokenRepository.subscribeToAllUsers()
            } catch (e: Exception) {
                // Silencioso, no afecta UX
            }
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
    data class Success(val message: String) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
