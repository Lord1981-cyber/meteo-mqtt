package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.MeteoReading
import com.example.data.mqtt.MqttConnectionState
import com.example.data.mqtt.MqttManager
import com.example.data.repository.MeteoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MeteoViewModel(
    private val repository: MeteoRepository,
    private val mqttManager: MqttManager
) : ViewModel() {

    // Reactive streams from local database
    val allReadings: StateFlow<List<MeteoReading>> = repository.allReadings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val latestReading: StateFlow<MeteoReading?> = repository.latestReading
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Reactive states from MQTT Client
    val connectionState: StateFlow<MqttConnectionState> = mqttManager.connectionState
    val lastErrorMessage: StateFlow<String?> = mqttManager.lastErrorMessage
    val msgCount: StateFlow<Int> = mqttManager.receivedMessageCount

    // Local UI values representing settings form
    val brokerHostForm = MutableStateFlow(mqttManager.brokerHost)
    val brokerPortForm = MutableStateFlow(mqttManager.brokerPort.toString())
    val usernameForm = MutableStateFlow(mqttManager.username)
    val passwordForm = MutableStateFlow(mqttManager.password)
    val clientIdForm = MutableStateFlow(mqttManager.clientId)
    val topicForm = MutableStateFlow(mqttManager.topic)

    private val prefs = mqttManager.context.getSharedPreferences("meteo_settings", android.content.Context.MODE_PRIVATE)

    // Alert & Bulletin settings states
    val backgroundNotificationsEnabled = MutableStateFlow(prefs.getBoolean("bg_notifications_enabled", true))
    val bulletinEmailEnabled = MutableStateFlow(prefs.getBoolean("bulletin_email_enabled", false))
    val bulletinEmailAddress = MutableStateFlow(prefs.getString("bulletin_email_address", "") ?: "")
    val bulletinDuration = MutableStateFlow(prefs.getString("bulletin_duration", "Toutes les 2 heures") ?: "Toutes les 2 heures")
    val appThemeMode = MutableStateFlow(prefs.getString("app_theme_mode", "Système") ?: "Système")

    fun setAppThemeMode(mode: String) {
        appThemeMode.value = mode
        prefs.edit().putString("app_theme_mode", mode).apply()
    }

    fun setBackgroundNotifications(enabled: Boolean) {
        backgroundNotificationsEnabled.value = enabled
        prefs.edit().putBoolean("bg_notifications_enabled", enabled).apply()
    }

    fun setBulletinEmailEnabled(enabled: Boolean) {
        bulletinEmailEnabled.value = enabled
        prefs.edit().putBoolean("bulletin_email_enabled", enabled).apply()
    }

    fun setBulletinEmailAddress(email: String) {
        bulletinEmailAddress.value = email
        prefs.edit().putString("bulletin_email_address", email).apply()
    }

    fun setBulletinDuration(duration: String) {
        bulletinDuration.value = duration
        prefs.edit().putString("bulletin_duration", duration).apply()
    }

    /**
     * Connect to broker with currently typed credentials.
     */
    fun connectMqtt() {
        // Apply settings form to MqttManager before connecting
        mqttManager.brokerHost = brokerHostForm.value.trim()
        mqttManager.brokerPort = brokerPortForm.value.toIntOrNull() ?: 1883
        mqttManager.username = usernameForm.value.trim()
        mqttManager.password = passwordForm.value
        mqttManager.clientId = clientIdForm.value.trim().ifEmpty { "android_" + System.currentTimeMillis().toString().takeLast(5) }
        mqttManager.topic = topicForm.value.trim()

        mqttManager.connect()
    }

    /**
     * Disconnects from active session.
     */
    fun disconnectMqtt() {
        mqttManager.disconnect()
    }

    /**
     * Reset config form to official Maqiatto Arduino presets.
     */
    fun resetToDefaults() {
        brokerHostForm.value = "maqiatto.com"
        brokerPortForm.value = "1883"
        usernameForm.value = "addou.ahmed.lord@gmail.com"
        passwordForm.value = "Test@ihfr31"
        clientIdForm.value = "user_android_" + (1000..9999).random().toString()
        topicForm.value = "addou.ahmed.lord@gmail.com/meteo1"
    }

    /**
     * Delete entire saved sensor reading history from database.
     */
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    /**
     * Emulates an incoming sensor message block to quickly test plotting or offline views.
     */
    fun triggerMockReading() {
        mqttManager.generateMockPayload()
    }

    /**
     * Custom provider Factory to pass Repository & MqttManager references.
     */
    class Factory(
        private val repository: MeteoRepository,
        private val mqttManager: MqttManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MeteoViewModel::class.java)) {
                return MeteoViewModel(repository, mqttManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
