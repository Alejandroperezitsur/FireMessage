package com.apvlabs.firemessage.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.apvlabs.firemessage.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones de base de datos Room
 */
@Dao
interface NotificationDao {
    
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>
    
    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY timestamp DESC")
    fun getUnreadNotifications(): Flow<List<NotificationEntity>>
    
    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getNotificationById(id: String): NotificationEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)
    
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)
    
    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()
    
    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: String)
    
    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()
    
    @Query("DELETE FROM notifications WHERE syncedWithFirestore = 1")
    suspend fun clearSyncedNotifications()
    
    @Query("SELECT * FROM notifications WHERE syncedWithFirestore = 0")
    suspend fun getUnsyncedNotifications(): List<NotificationEntity>
    
    @Query("UPDATE notifications SET syncedWithFirestore = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
    
    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    suspend fun getUnreadCount(): Int
}
