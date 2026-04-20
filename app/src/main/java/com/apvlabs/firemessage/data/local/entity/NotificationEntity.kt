package com.apvlabs.firemessage.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.apvlabs.firemessage.data.local.converter.Converters
import com.apvlabs.firemessage.data.model.NotificationPriority
import com.apvlabs.firemessage.data.model.NotificationType

/**
 * Entidad de Room para persistencia offline
 */
@Entity(tableName = "notifications")
@TypeConverters(Converters::class)
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val body: String,
    val type: NotificationType,
    val priority: NotificationPriority,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val syncedWithFirestore: Boolean = false,
    val data: Map<String, String> = emptyMap()
)
