package com.apvlabs.firemessage.data.repository

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para gestión de tokens FCM con topics profesionales
 */
class FcmTokenRepository {
    
    /**
     * Obtiene el token FCM actual
     */
    suspend fun getFcmToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Suscribe al tópico de rol (role_alumno o role_profesor)
     */
    suspend fun subscribeToRoleTopic(role: String): Boolean {
        return try {
            val topic = "role_${role.lowercase()}"
            FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Suscribe al tópico de carrera (career_sistemas, career_medicina, etc.)
     */
    suspend fun subscribeToCareerTopic(career: String): Boolean {
        return try {
            // Normalizar nombre de carrera para tópico
            val topic = "career_${career.lowercase().replace(" ", "_").replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")}"
            FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Suscribe al tópico combinado (role_career)
     */
    suspend fun subscribeToCombinedTopic(role: String, career: String): Boolean {
        return try {
            val normalizedCareer = career.lowercase().replace(" ", "_").replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
            val topic = "role_${role.lowercase()}_$normalizedCareer"
            FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Suscribe al tópico global para todos los usuarios
     */
    suspend fun subscribeToAllUsers(): Boolean {
        return try {
            FirebaseMessaging.getInstance().subscribeToTopic("all_users").await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Cancela suscripción a un tópico
     */
    suspend fun unsubscribeFromTopic(topic: String): Boolean {
        return try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
