package com.apvlabs.firemessage.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.apvlabs.firemessage.data.local.converter.Converters
import com.apvlabs.firemessage.data.local.entity.NotificationEntity

/**
 * Base de datos Room para persistencia offline real
 */
@Database(
    entities = [NotificationEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NotificationDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: NotificationDatabase? = null

        fun getDatabase(context: Context): NotificationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotificationDatabase::class.java,
                    "notification_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
