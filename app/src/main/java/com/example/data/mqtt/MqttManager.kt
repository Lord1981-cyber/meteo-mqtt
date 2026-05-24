package com.example.data.mqtt

import android.content.Context
import android.util.Log
import com.example.data.model.MeteoReading
import com.example.data.repository.MeteoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.nio.charset.StandardCharsets

enum class MqttConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

class MqttManager(
    private val context: Context,
    private val repository: MeteoRepository
) {
    private val tag = "MqttManager"
    private var mqttClient: MqttClient? = null
    
    // Defensive handling of uncaught exceptions in back background coroutines
    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        Log.e(tag, "Uncaught coroutine error", throwable)
        _connectionState.value = MqttConnectionState.ERROR
        _lastErrorMessage.value = throwable.message ?: "Erreur inattendue en arrière-plan"
    }
    private val scope = CoroutineScope(Dispatchers.IO + exceptionHandler)

    // Observable states
    private val _connectionState = MutableStateFlow(MqttConnectionState.DISCONNECTED)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage

    private val _receivedMessageCount = MutableStateFlow(0)
    val receivedMessageCount: StateFlow<Int> = _receivedMessageCount

    // Configurable parameters with preconfigured Maqiatto defaults from Arduino sketch
    var brokerHost = "maqiatto.com"
    var brokerPort = 1883
    var username = "addou.ahmed.lord@gmail.com"
    var password = "Test@ihfr31"
    var clientId = "user_android_" + System.currentTimeMillis().toString().takeLast(5)
    var topic = "addou.ahmed.lord@gmail.com/meteo1"

    init {
        Log.d(tag, "MqttManager initialized with default parameters")
    }

    /**
     * Connects asynchronously to the MQTT broker using the stored configurations.
     */
    fun connect() {
        if (_connectionState.value == MqttConnectionState.CONNECTED || _connectionState.value == MqttConnectionState.CONNECTING) {
            Log.d(tag, "Already connecting or connected, skipping")
            return
        }

        _connectionState.value = MqttConnectionState.CONNECTING
        _lastErrorMessage.value = null

        scope.launch {
            try {
                val serverURI = "tcp://$brokerHost:$brokerPort"
                Log.d(tag, "Connecting to MQTT Broker: $serverURI with ClientID: $clientId")

                mqttClient = MqttClient(serverURI, clientId, MemoryPersistence())
                
                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    connectionTimeout = 15
                    keepAliveInterval = 60
                    if (this@MqttManager.username.isNotEmpty()) {
                        this.userName = this@MqttManager.username
                    }
                    if (this@MqttManager.password.isNotEmpty()) {
                        this.password = this@MqttManager.password.toCharArray()
                    }
                }

                mqttClient?.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        Log.e(tag, "Connection lost: ${cause?.message}")
                        _connectionState.value = MqttConnectionState.DISCONNECTED
                        _lastErrorMessage.value = cause?.message ?: "Connexion perdue avec le broker"
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        Log.d(tag, "Message arrived on topic $topic")
                        if (message != null) {
                            val payload = String(message.payload, StandardCharsets.UTF_8)
                            handleRawPayload(payload)
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        // Not publishing usually, but callback required
                    }
                })

                mqttClient?.connect(options)
                Log.d(tag, "MQTT client connected. Subscribing to topic: $topic")
                mqttClient?.subscribe(topic, 1)

                _connectionState.value = MqttConnectionState.CONNECTED
            } catch (e: MqttException) {
                Log.e(tag, "Error connecting to MQTT: ${e.reasonCode} - ${e.message}", e)
                _connectionState.value = MqttConnectionState.ERROR
                _lastErrorMessage.value = "Erreur MQTT (${e.reasonCode}): ${e.message ?: "Inconnu"}"
            } catch (e: Throwable) {
                Log.e(tag, "Unexpected error connecting to MQTT", e)
                _connectionState.value = MqttConnectionState.ERROR
                _lastErrorMessage.value = e.message ?: "Erreur inattendue lors de la connexion"
            }
        }
    }

    /**
     * Disconnects from the MQTT broker.
     */
    fun disconnect() {
        scope.launch {
            try {
                mqttClient?.let {
                    if (it.isConnected) {
                        it.disconnect()
                        Log.d(tag, "MQTT client disconnected successfully")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error disconnecting MQTT client", e)
            } finally {
                mqttClient = null
                _connectionState.value = MqttConnectionState.DISCONNECTED
            }
        }
    }

    /**
     * Publishes a message to the broker (optional usage).
     */
    fun publishMessage(pubTopic: String, messageText: String) {
        scope.launch {
            try {
                mqttClient?.let {
                    if (it.isConnected) {
                        val message = MqttMessage(messageText.toByteArray(StandardCharsets.UTF_8)).apply {
                            qos = 1
                        }
                        it.publish(pubTopic, message)
                        Log.d(tag, "Published message to topic $pubTopic: $messageText")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to publish message", e)
            }
        }
    }

    /**
     * Process the received raw payload string and inserts into the database.
     * Expects CSV: "temp,humidity,pressure,lux,altitude"
     * Example: "24.50,55.00,1012.30,450.00,150.20"
     */
    fun handleRawPayload(payload: String) {
        scope.launch {
            try {
                Log.d(tag, "Parsing payload: $payload")
                val parts = payload.trim().split(",")
                
                if (parts.size >= 3) {
                    val temp = parts[0].toFloatOrNull() ?: 0.0f
                    val humidity = parts[1].toFloatOrNull() ?: 0.0f
                    val pressure = parts[2].toFloatOrNull() ?: 0.0f
                    
                    // Radiation (lux) is 4th parameter, fallback to LDR reading
                    val radiation = if (parts.size >= 4) parts[3].toFloatOrNull() ?: 0.0f else 0.0f
                    
                    // Altitude is 5th parameter, fallback to 1013.25 calculated altitude
                    val altitude = if (parts.size >= 5) parts[4].toFloatOrNull() ?: 0.0f else 0.0f

                    val reading = MeteoReading(
                        temperature = temp,
                        humidity = humidity,
                        pressure = pressure,
                        radiation = radiation,
                        altitude = altitude,
                        rawPayload = payload
                    )

                    repository.insert(reading)
                    _receivedMessageCount.value += 1
                    Log.d(tag, "Successfully saved sensor reading into database: $reading")
                } else {
                    Log.w(tag, "Received poorly formatted payload (fewer than 3 parameters): $payload")
                    // Still insert a fallback if possible to let users observe the raw data
                    val rawNum = payload.toFloatOrNull() ?: 0.0f
                    val reading = MeteoReading(
                        temperature = rawNum,
                        humidity = 0.0f,
                        pressure = 0.0f,
                        radiation = 0.0f,
                        altitude = 0.0f,
                        rawPayload = payload
                    )
                    repository.insert(reading)
                    _receivedMessageCount.value += 1
                }
            } catch (e: Exception) {
                Log.e(tag, "Exception parsing MQTT payload", e)
            }
        }
    }

    /**
     * Helper to insert a mock reading for easy debugging.
     */
    fun generateMockPayload() {
        val mockTemp = (18..35).random() + (0..9).random() / 10.0f
        val mockHum = (40..85).random() + (0..9).random() / 10.0f
        val mockPress = (980..1025).random() + (0..9).random() / 10.0f
        val mockLux = (5..850).random().toFloat()
        val mockAlt = (120..320).random() + (0..9).random() / 10.0f

        val mockCsv = "$mockTemp,$mockHum,$mockPress,$mockLux,$mockAlt"
        handleRawPayload(mockCsv)
    }
}
