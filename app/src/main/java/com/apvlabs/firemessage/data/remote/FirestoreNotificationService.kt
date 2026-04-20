package com.apvlabs.firemessage.data.remote

import com.apvlabs.firemessage.data.model.Notification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Servicio Firestore para persistencia global de notificaciones
 * Permite sincronización entre múltiples dispositivos
 */
class FirestoreNotificationService {
    private val db = FirebaseFirestore.getInstance()
    private val notificationsCollection = db.collection("notifications")
    
    /**
     * Obtiene notificaciones de Firestore para un usuario específico
     */
    suspend fun getNotifications(userId: String): List<Notification> {
        return try {
            val snapshot = notificationsCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    Notification(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        body = doc.getString("body") ?: "",
                        type = com.apvlabs.firemessage.data.model.NotificationType.valueOf(
                            doc.getString("type") ?: "MENSAJE_GENERAL"
                        ),
                        priority = com.apvlabs.firemessage.data.model.NotificationPriority.valueOf(
                            doc.getString("priority") ?: "NORMAL"
                        ),
                        isRead = false, // Firestore no guarda estado de lectura por dispositivo
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        data = emptyMap()
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Guarda una notificación en Firestore para persistencia global
     */
    suspend fun saveNotification(notification: Notification): Boolean {
        return try {
            val notificationData = hashMapOf(
                "id" to notification.id,
                "title" to notification.title,
                "body" to notification.body,
                "type" to notification.type.name,
                "priority" to notification.priority.name,
                "timestamp" to notification.timestamp,
                "data" to notification.data
            )
            
            notificationsCollection.document(notification.id).set(notificationData).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Escucha cambios en tiempo real de notificaciones
     */
    fun listenToNotifications(onNotificationAdded: (Notification) -> Unit) {
        notificationsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                
                for (docChange in snapshot.documentChanges) {
                    if (docChange.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        try {
                            val doc = docChange.document
                            val notification = Notification(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                body = doc.getString("body") ?: "",
                                type = com.apvlabs.firemessage.data.model.NotificationType.valueOf(
                                    doc.getString("type") ?: "MENSAJE_GENERAL"
                                ),
                                priority = com.apvlabs.firemessage.data.model.NotificationPriority.valueOf(
                                    doc.getString("priority") ?: "NORMAL"
                                ),
                                isRead = false,
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                data = emptyMap()
                            )
                            onNotificationAdded(notification)
                        } catch (e: Exception) {
                            // Ignorar errores de parsing
                        }
                    }
                }
            }
    }
}
