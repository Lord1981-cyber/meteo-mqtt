package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.MeteoReading

@Database(entities = [MeteoReading::class], version = 1, exportSchema = false)
abstract class MeteoDatabase : RoomDatabase() {
    abstract fun meteoDao(): MeteoDao

    companion object {
        @Volatile
        private var INSTANCE: MeteoDatabase? = null

        fun getDatabase(context: Context): MeteoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeteoDatabase::class.java,
                    "meteo_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
