package com.example.data.repository

import com.example.data.db.MeteoDao
import com.example.data.model.MeteoReading
import kotlinx.coroutines.flow.Flow

class MeteoRepository(private val meteoDao: MeteoDao) {
    val allReadings: Flow<List<MeteoReading>> = meteoDao.getAllReadings()
    val latestReading: Flow<MeteoReading?> = meteoDao.getLatestReading()

    suspend fun insert(reading: MeteoReading): Long {
        return meteoDao.insertReading(reading)
    }

    suspend fun clearAll() {
        meteoDao.clearAllReadings()
    }
}
