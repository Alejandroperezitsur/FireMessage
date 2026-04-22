package com.apvlabs.firemessage.data.local

import android.content.Context
import android.content.SharedPreferences
import com.apvlabs.firemessage.data.model.Notification
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Caché simplificado para notificaciones
 * Usa SharedPreferences para persistencia básica
 */
class NotificationCache(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("notification_cache", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun saveNotification(notification: Notification) {
        val notifications = getNotifications().toMutableList()
        notifications.add(0, notification) // Add at beginning
        saveNotificationsList(notifications)
    }
    
    fun getNotifications(): List<Notification> {
        val json = prefs.getString("notifications", null) ?: return emptyList()
        val type = object : TypeToken<List<Notification>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    private fun saveNotificationsList(notifications: List<Notification>) {
        val json = gson.toJson(notifications)
        prefs.edit().putString("notifications", json).apply()
    }
    
    fun markAsRead(notificationId: String) {
        val notifications = getNotifications().map { notification ->
            if (notification.id == notificationId) {
                notification.copy(isRead = true)
            } else {
                notification
            }
        }
        saveNotificationsList(notifications)
    }
    
    fun markAllAsRead() {
        val notifications = getNotifications().map { it.copy(isRead = true) }
        saveNotificationsList(notifications)
    }
    
    fun deleteNotification(notificationId: String) {
        val notifications = getNotifications().filter { it.id != notificationId }
        saveNotificationsList(notifications)
    }
    
    fun getUnreadCount(): Int {
        return getNotifications().count { !it.isRead }
    }
}
