package com.ridesync.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BufferedTelemetryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RideSyncDatabase : RoomDatabase() {

    abstract fun telemetryDao(): TelemetryDao

    companion object {
        @Volatile
        private var INSTANCE: RideSyncDatabase? = null

        fun getInstance(context: Context): RideSyncDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RideSyncDatabase::class.java,
                    "ridesync_offline_buffer.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
