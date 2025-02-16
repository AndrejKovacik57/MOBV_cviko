package com.example.mobvcviko1.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mobvcviko1.data.db.entities.GeofenceEntity
import com.example.mobvcviko1.data.db.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        GeofenceEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppRoomDatabase : RoomDatabase() {
    abstract fun appDao(): DbDao
    companion object {
        @Volatile
        private var INSTANCE: AppRoomDatabase? = null
        fun getInstance(context: Context): AppRoomDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: buildDatabase(context).also { INSTANCE = it }
            }
        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                AppRoomDatabase::class.java, "treeCamDB"
            ).fallbackToDestructiveMigration()
                .build()
    }
}