package com.apvlabs.firemessage.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.apvlabs.firemessage.data.model.Notification
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore para caché de notificaciones offline
 */
private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "notifications")

class NotificationCache(private val context: Context) {
    
    private val gson = Gson()
    private val NOTIFICATIONS_KEY = stringPreferencesKey("notifications_list")
    
    /**
     * Guardar lista de notificaciones en caché
     */
    suspend fun saveNotifications(notifications: List<Notification>) {
        context.notificationDataStore.edit { preferences ->
            val json = gson.toJson(notifications)
            preferences[NOTIFICATIONS_KEY] = json
        }
    }
    
    /**
     * Obtener notificaciones desde caché
     */
    fun getNotifications(): Flow<List<Notification>> {
        return context.notificationDataStore.data.map { preferences ->
            val json = preferences[NOTIFICATIONS_KEY] ?: "[]"
            val type = object : TypeToken<List<Notification>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        }
    }
    
    /**
     * Agregar una notificación a la caché
     */
    suspend fun addNotification(notification: Notification) {
        getNotifications().collect { currentList ->
            val updatedList = listOf(notification) + currentList
            saveNotifications(updatedList)
        }
    }
    
    /**
     * Marcar notificación como leída
     */
    suspend fun markAsRead(notificationId: String) {
        getNotifications().collect { currentList ->
            val updatedList = currentList.map { notification ->
                if (notification.id == notificationId) {
                    notification.copy(isRead = true)
                } else {
                    notification
                }
            }
            saveNotifications(updatedList)
        }
    }
    
    /**
     * Limpiar caché de notificaciones
     */
    suspend fun clearCache() {
        context.notificationDataStore.edit { preferences ->
            preferences.remove(NOTIFICATIONS_KEY)
        }
    }
}
