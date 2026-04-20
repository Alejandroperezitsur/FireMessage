package com.apvlabs.firemessage.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Servicio para tracking de interacción de usuarios
 * Permite analytics reales de uso
 */
class AnalyticsService {
    private val db = FirebaseFirestore.getInstance()
    
    /**
     * Registra que un usuario abrió una notificación
     */
    suspend fun trackNotificationOpened(
        notificationId: String,
        notificationType: String,
        userId: String
    ): Boolean {
        return try {
            db.collection("notification_events").add(
                hashMapOf(
                    "notificationId" to notificationId,
                    "notificationType" to notificationType,
                    "eventType" to "opened",
                    "userId" to userId,
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Registra que un usuario ignoró/descartó una notificación
     */
    suspend fun trackNotificationDismissed(
        notificationId: String,
        notificationType: String,
        userId: String
    ): Boolean {
        return try {
            db.collection("notification_events").add(
                hashMapOf(
                    "notificationId" to notificationId,
                    "notificationType" to notificationType,
                    "eventType" to "dismissed",
                    "userId" to userId,
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Registra interacción genérica
     */
    suspend fun trackEvent(
        eventType: String,
        data: Map<String, Any>,
        userId: String
    ): Boolean {
        return try {
            db.collection("notification_events").add(
                hashMapOf(
                    "eventType" to eventType,
                    "data" to data,
                    "userId" to userId,
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
