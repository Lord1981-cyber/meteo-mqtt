package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MeteoReading
import kotlinx.coroutines.flow.Flow

@Dao
interface MeteoDao {
    @Query("SELECT * FROM meteo_readings ORDER BY timestamp DESC")
    fun getAllReadings(): Flow<List<MeteoReading>>

    @Query("SELECT * FROM meteo_readings ORDER BY timestamp DESC LIMIT 1")
    fun getLatestReading(): Flow<MeteoReading?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: MeteoReading): Long

    @Query("DELETE FROM meteo_readings")
    suspend fun clearAllReadings()
}
