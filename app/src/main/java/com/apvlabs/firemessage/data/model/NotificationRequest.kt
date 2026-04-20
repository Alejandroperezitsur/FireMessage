package com.apvlabs.firemessage.data.model

/**
 * Modelo para solicitud de envío de notificación al backend
 */
data class NotificationRequest(
    val token: String? = null,
    val title: String,
    val body: String,
    val type: String,
    val targetRole: String? = null,
    val targetCareer: String? = null,
    val sendToAll: Boolean = false
)
