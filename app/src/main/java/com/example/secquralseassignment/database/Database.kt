package com.example.secquralseassignment.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DataEntity::class],
    version = 2,
    exportSchema = false
)
abstract class Database: RoomDatabase() {
    abstract fun dataDao(): DataDAO

    companion object {
        private var Instance: com.example.secquralseassignment.database.Database?= null

        fun getDatabaseInstance(mContext: Context): com.example.secquralseassignment.database.Database {
            return Instance?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    mContext.applicationContext,
                    com.example.secquralseassignment.database.Database::class.java,
                    "Db"
                ).fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
                Instance = instance
                instance
            }
        }
    }
}