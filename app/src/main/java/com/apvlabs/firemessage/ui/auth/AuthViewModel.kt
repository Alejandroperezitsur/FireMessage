package com.apvlabs.firemessage.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apvlabs.firemessage.data.model.User
import com.apvlabs.firemessage.data.model.UserRole
import com.apvlabs.firemessage.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para autenticación
 * Maneja el estado de login y registro
 */
class AuthViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    /**
     * Registrar nuevo usuario
     */
    fun register(
        email: String,
        password: String,
        name: String,
        role: UserRole,
        career: String
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = userRepository.register(email, password, name, role, career)
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
                _uiState.value = AuthUiState.Success(result.getOrNull()!!)
            } else {
                _uiState.value = AuthUiState.Error(
                    result.exceptionOrNull()?.message ?: "Error desconocido"
                )
            }
        }
    }
    
    /**
     * Iniciar sesión
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = userRepository.login(email, password)
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
                _uiState.value = AuthUiState.Success(result.getOrNull()!!)
            } else {
                _uiState.value = AuthUiState.Error(
                    result.exceptionOrNull()?.message ?: "Error desconocido"
                )
            }
        }
    }
    
    /**
     * Cerrar sesión
     */
    fun logout() {
        viewModelScope.launch {
            val result = userRepository.logout()
            if (result.isSuccess) {
                _currentUser.value = null
                _uiState.value = AuthUiState.Idle
            } else {
                _uiState.value = AuthUiState.Error(
                    result.exceptionOrNull()?.message ?: "Error al cerrar sesión"
                )
            }
        }
    }
    
    /**
     * Verificar si hay usuario autenticado y cargar sus datos
     */
    fun checkAuthStatus() {
        viewModelScope.launch {
            if (userRepository.isUserLoggedIn()) {
                _uiState.value = AuthUiState.Loading
                val user = userRepository.getCurrentUser()
                if (user != null) {
                    _currentUser.value = user
                    _uiState.value = AuthUiState.Success(user)
                } else {
                    _uiState.value = AuthUiState.Idle
                }
            } else {
                _uiState.value = AuthUiState.Idle
            }
        }
    }
    
    /**
     * Resetear estado de UI
     */
    fun resetUiState() {
        _uiState.value = AuthUiState.Idle
    }
}

/**
 * Estados de UI para autenticación
 */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
