package com.apvlabs.firemessage.data.model

/**
 * Modelo de notificación
 * Contiene toda la información de una notificación recibida
 */
data class Notification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: NotificationType = NotificationType.MENSAJE_GENERAL,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val data: Map<String, String> = emptyMap()
)

/**
 * Tipos de notificación disponibles
 * Cada tipo tiene su propio icono, color y comportamiento de navegación
 */
enum class NotificationType(val displayName: String) {
    CAFETERIA_READY("Cafetería"),
    ALERTA_IMPORTANTE("Alerta Importante"),
    EVENTO("Evento"),
    MENSAJE_GENERAL("Mensaje General")
}
