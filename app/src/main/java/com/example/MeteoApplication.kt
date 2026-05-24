package com.example

import android.app.Application
import com.example.data.db.MeteoDatabase
import com.example.data.mqtt.MqttManager
import com.example.data.repository.MeteoRepository

class MeteoApplication : Application() {
    
    // Lazy-initialized Database
    val database by lazy { MeteoDatabase.getDatabase(this) }
    
    // Lazy-initialized Repository
    val repository by lazy { MeteoRepository(database.meteoDao()) }
    
    // Lazy-initialized MqttManager
    val mqttManager by lazy { MqttManager(this, repository) }

    override fun onCreate() {
        super.onCreate()
        // Avoid auto-connecting during standard Robolectric/JVM local unit tests
        val isUnitTest = try {
            Class.forName("org.robolectric.Robolectric") != null
        } catch (e: ClassNotFoundException) {
            false
        }

        if (!isUnitTest) {
            try {
                mqttManager.connect()
            } catch (t: Throwable) {
                android.util.Log.e("MeteoApplication", "Failed to auto-connect MQTT on start", t)
            }
        }
    }
}
