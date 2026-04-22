package com.apvlabs.firemessage.data.repository

import com.apvlabs.firemessage.data.model.User
import com.apvlabs.firemessage.data.model.UserRole
import com.apvlabs.firemessage.data.remote.FirebaseAuthService
import com.apvlabs.firemessage.data.remote.FirestoreService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repositorio de usuarios - implementa patrón Repository
 * Centraliza toda la lógica de acceso a datos de usuarios
 */
class UserRepository(
    private val authService: FirebaseAuthService,
    private val firestoreService: FirestoreService
) {
    
    /**
     * Registrar nuevo usuario
     */
    suspend fun register(email: String, password: String, name: String, role: UserRole, career: String): Result<User> {
        return try {
            // 1. Crear usuario en Firebase Auth
            val authResult = authService.register(email, password)
            if (authResult.isFailure) {
                return Result.failure(authResult.exceptionOrNull()!!)
            }
            
            val firebaseUser = authResult.getOrNull()!!
            
            // 2. Crear usuario en Firestore
            val user = User(
                id = firebaseUser.uid,
                email = email,
                name = name,
                role = role,
                career = career,
                fcmToken = ""
            )
            
            val firestoreResult = firestoreService.saveUser(user)
            if (firestoreResult.isFailure) {
                return Result.failure(firestoreResult.exceptionOrNull()!!)
            }
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Iniciar sesión
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            // 1. Autenticar con Firebase Auth
            val authResult = authService.login(email, password)
            if (authResult.isFailure) {
                return Result.failure(authResult.exceptionOrNull()!!)
            }
            
            val firebaseUser = authResult.getOrNull()!!
            
            // 2. Obtener datos del usuario desde Firestore
            val userResult = firestoreService.getUser(firebaseUser.uid)
            if (userResult.isFailure) {
                return Result.failure(userResult.exceptionOrNull()!!)
            }
            
            val user = userResult.getOrNull()
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Usuario no encontrado en Firestore"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Cerrar sesión
     */
    suspend fun logout(): Result<Unit> {
        return authService.logout()
    }
    
    /**
     * Obtener usuario actual desde Firestore
     */
    suspend fun getCurrentUser(): User? {
        val firebaseUser = authService.getCurrentUser() ?: return null
        val result = firestoreService.getUser(firebaseUser.uid)
        return result.getOrNull()
    }
    
    /**
     * Verificar si hay usuario autenticado en Firebase Auth
     */
    fun isUserLoggedIn(): Boolean {
        return authService.isLoggedIn()
    }
    
    /**
     * Actualizar token FCM del usuario
     */
    suspend fun updateFcmToken(userId: String, token: String): Result<Unit> {
        return firestoreService.updateFcmToken(userId, token)
    }
    
    /**
     * Obtener usuarios por rol
     */
    suspend fun getUsersByRole(role: String): Result<List<User>> {
        return firestoreService.getUsersByRole(role)
    }
    
    /**
     * Obtener usuarios por carrera
     */
    suspend fun getUsersByCareer(career: String): Result<List<User>> {
        return firestoreService.getUsersByCareer(career)
    }
    
    /**
     * Obtener todos los usuarios
     */
    suspend fun getAllUsers(): Result<List<User>> {
        return firestoreService.getAllUsers()
    }
    
    /**
     * Obtener usuarios por rol y carrera
     */
    suspend fun getUsersByRoleAndCareer(role: String, career: String): Result<List<User>> {
        return firestoreService.getUsersByRoleAndCareer(role, career)
    }
}
