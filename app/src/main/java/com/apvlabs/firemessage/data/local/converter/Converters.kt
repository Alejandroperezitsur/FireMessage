package com.apvlabs.firemessage.data.local.converter

import androidx.room.TypeConverter
import com.apvlabs.firemessage.data.model.NotificationPriority
import com.apvlabs.firemessage.data.model.NotificationType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Convertidores de Room para tipos complejos
 */
class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromNotificationType(type: NotificationType): String {
        return type.name
    }
    
    @TypeConverter
    fun toNotificationType(value: String): NotificationType {
        return try {
            NotificationType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            NotificationType.MENSAJE_GENERAL
        }
    }
    
    @TypeConverter
    fun fromNotificationPriority(priority: NotificationPriority): String {
        return priority.name
    }
    
    @TypeConverter
    fun toNotificationPriority(value: String): NotificationPriority {
        return try {
            NotificationPriority.valueOf(value)
        } catch (e: IllegalArgumentException) {
            NotificationPriority.NORMAL
        }
    }
    
    @TypeConverter
    fun fromStringMap(map: Map<String, String>): String {
        return gson.toJson(map)
    }
    
    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }
}
