package com.apvlabs.firemessage.data.model

/**
 * Prioridad de notificación con comportamiento diferenciado
 */
enum class NotificationPriority {
    HIGH,      // Alerta urgente: sonido + vibración + pantalla emergente
    NORMAL,    // Notificación estándar: sonido normal
    LOW        // Silenciosa: sin sonido ni vibración
}

fun NotificationType.getDefaultPriority(): NotificationPriority {
    return when (this) {
        NotificationType.ALERTA_IMPORTANTE -> NotificationPriority.HIGH
        NotificationType.EVENTO -> NotificationPriority.NORMAL
        NotificationType.CAFETERIA_READY -> NotificationPriority.NORMAL
        NotificationType.MENSAJE_GENERAL -> NotificationPriority.LOW
    }
}
