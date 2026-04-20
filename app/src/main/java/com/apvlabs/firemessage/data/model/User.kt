package com.apvlabs.firemessage.data.model

/**
 * Modelo de usuario para Firestore
 * Contiene información del usuario y su segmentación
 */
data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: UserRole = UserRole.STUDENT,
    val career: String = "",
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Roles de usuario disponibles
 */
enum class UserRole {
    STUDENT,
    PROFESSOR
}
