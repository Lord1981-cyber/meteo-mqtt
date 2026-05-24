package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MeteoReading
import com.example.data.mqtt.MqttConnectionState
import com.example.ui.MeteoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MeteoViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Temps Réel", "Historique", "Broker MQTT")

    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val lastErrorMessage by viewModel.lastErrorMessage.collectAsStateWithLifecycle()
    val msgCount by viewModel.msgCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Météo IoT",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    letterSpacing = (-0.5).sp
                                ),
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "NODEMCU ESP8266",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = Color(0xFF64748B)
                            )
                        }

                        // Emerald Status Badge
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color(0xFFECFDF5))
                                .border(1.dp, Color(0xFFD1FAE5), RoundedCornerShape(100.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(
                                        if (connectionState == MqttConnectionState.CONNECTED) 
                                            Color(0xFF10B981) 
                                        else 
                                            Color(0xFF94A3B8)
                                    )
                            )
                            Text(
                                text = "MQTT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = if (connectionState == MqttConnectionState.CONNECTED) 
                                    Color(0xFF047857) 
                                else 
                                    Color(0xFF475569)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.triggerMockReading() },
                        modifier = Modifier.testTag("appbar_mock_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddChart,
                            contentDescription = "Simuler donnée",
                            tint = Color(0xFF475569)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF0F172A)
                ),
                modifier = Modifier.border(0.dp, Color(0xFFF1F5F9))
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .border(1.dp, Color(0xFFF1F5F9))
            ) {
                tabs.forEachIndexed { index, title ->
                    val icon = when (index) {
                        0 -> Icons.Default.Cloud
                        1 -> Icons.AutoMirrored.Filled.List
                        else -> Icons.Default.Settings
                    }
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { 
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        },
                        icon = { Icon(icon, contentDescription = title) },
                        modifier = Modifier.testTag("tab_$index")
                    )
                }
            }
        },
        containerColor = Color(0xFFF7F9FC), // Soft slate bg from Professional Polish HTML
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Status Header Connection Bar
            ConnectionStatusBar(
                state = connectionState,
                error = lastErrorMessage,
                count = msgCount,
                onReconnect = { viewModel.connectMqtt() }
            )

            // Dynamic Tab Switcher Content
            when (selectedTab) {
                0 -> LiveFeedTab(viewModel)
                1 -> HistoryTab(viewModel)
                2 -> ConfigurationTab(viewModel)
            }
        }
    }
}

@Composable
fun ConnectionStatusBar(
    state: MqttConnectionState,
    error: String?,
    count: Int,
    onReconnect: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = when (state) {
            MqttConnectionState.CONNECTED -> Color(0xFF2E7D32) // Soft Green
            MqttConnectionState.CONNECTING -> Color(0xFFEF6C00) // Soft Orange
            MqttConnectionState.ERROR -> Color(0xFFC62828) // Soft Red
            MqttConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "status_color"
    )

    val textColor = if (state == MqttConnectionState.DISCONNECTED) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color.White
    }

    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        onClick = { if (state == MqttConnectionState.ERROR || state == MqttConnectionState.DISCONNECTED) onReconnect() },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReconnect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = when (state) {
                        MqttConnectionState.CONNECTED -> Icons.Default.CheckCircle
                        MqttConnectionState.CONNECTING -> Icons.Default.Sync
                        MqttConnectionState.ERROR -> Icons.Default.Error
                        MqttConnectionState.DISCONNECTED -> Icons.Default.CloudOff
                    },
                    contentDescription = "Status",
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )

                Column {
                    val statusText = when (state) {
                        MqttConnectionState.CONNECTED -> "Connecté au Broker"
                        MqttConnectionState.CONNECTING -> "Connexion en cours..."
                        MqttConnectionState.ERROR -> "Erreur de connexion"
                        MqttConnectionState.DISCONNECTED -> "Broker déconnecté"
                    }
                    Text(
                        text = statusText,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    error?.let {
                        Text(
                            text = it,
                            color = textColor.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } ?: run {
                        if (state == MqttConnectionState.CONNECTED) {
                            Text(
                                text = "Maqiatto en écoute",
                                color = textColor.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text(
                                text = "Cliquez pour reconnecter",
                                color = textColor.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(textColor.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$count Reçus",
                    color = textColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LiveFeedTab(viewModel: MeteoViewModel) {
    val latestReading by viewModel.latestReading.collectAsStateWithLifecycle()

    if (latestReading == null) {
        EmptyLiveState(onMock = { viewModel.triggerMockReading() })
    } else {
        val reading = latestReading!!
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Dernière mise à jour: ${reading.getFormattedDateTime()}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    color = Color(0xFF64748B),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }

            // Top Hero Card showing overall comfort metrics
            item {
                MeteoHeroCard(reading)
            }

            // Temperature & Humidity Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SensorCard(
                            title = "Température",
                            value = "${String.format("%.1f", reading.temperature)} °C",
                            icon = Icons.Default.DeviceThermostat,
                            tint = Color(0xFFEA580C), // Orange-600
                            subtitle = if (reading.temperature > 28) "Chaud" else if (reading.temperature < 18) "Frais" else "Agréable"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SensorCard(
                            title = "Humidité",
                            value = "${String.format("%.1f", reading.humidity)} %",
                            icon = Icons.Default.WaterDrop,
                            tint = Color(0xFF2563EB), // Blue-600
                            subtitle = if (reading.humidity > 60) "Humide" else if (reading.humidity < 35) "Sec" else "Optimal"
                        )
                    }
                }
            }

            // Barometric Pressure & Altitude
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SensorCard(
                            title = "Pression",
                            value = "${String.format("%.0f", reading.pressure)} hPa",
                            icon = Icons.Default.Compress,
                            tint = Color(0xFF9333EA), // Purple-600
                            subtitle = if (reading.pressure > 1013.25) "Anticyclone" else "Dépression"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SensorCard(
                            title = "Altitude",
                            value = "${String.format("%.0f", reading.altitude)} m",
                            icon = Icons.Default.FilterHdr,
                            tint = Color(0xFF475569), // Slate-600
                            subtitle = "Alt. Estimée"
                        )
                    }
                }
            }

            // Premium custom styled LDR Sunshine Card - Spans full width
            item {
                SolarRadiationCard(reading)
            }

            // Displays the raw CSV string parsed via broker
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(Color(0xFF3B82F6))
                            )
                            Text(
                                text = "Trame série reçue (MQTT)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF1E293B)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = reading.rawPayload,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }

            // Premium NTP Footer Panel showing Date/Time/IP
            item {
                MeteoFooterPanel(reading)
            }
        }
    }
}

@Composable
fun SolarRadiationCard(reading: MeteoReading) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFD1E4FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // White Circular avatar for the sun icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LightMode,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B), // amber-500
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RADIATION SOLAIRE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color(0xFF1E40AF) // blue-800
                )
                
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = String.format("%.0f", reading.radiation),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        ),
                        color = Color(0xFF1E3A8A) // blue-900
                    )
                    Text(
                        text = "Lux",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF1D4ED8), // blue-700
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "STATION OK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF1D4ED8)
                )
                Text(
                    text = "LDR_PIN A0",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp
                    ),
                    color = Color(0xFF1D4ED8).copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun MeteoFooterPanel(reading: MeteoReading) {
    val dateSdf = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.FRANCE)
    val timeSdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.FRANCE)
    
    val dateStr = dateSdf.format(java.util.Date(reading.timestamp))
    val timeStr = timeSdf.format(java.util.Date(reading.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // First Row: Date & Time NTP
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "DATE & HEURE NTP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFF1E293B)
                    )
                }
                
                Text(
                    text = timeStr,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = Color(0xFF334155)
                )
            }

            Divider(color = Color(0xFFF1F5F9))

            // Second Row: IP Address & API Code
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Small Pulse Blue Dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFF3B82F6))
                    )
                    
                    Text(
                        text = "IP Locale: 192.168.1.104",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color(0xFF475569)
                    )
                }

                Text(
                    text = "API: tPmAT5Ab...",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun EmptyLiveState(onMock: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CloudQueue,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Aucune valeur reçue",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Le terminal MQTT de la station météo n'a pas encore envoyé de données au topic souscrit, ou la connexion n'est pas établie.\n\nVous pouvez cliquer ci-dessous pour injecter des valeurs d'essai.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onMock,
            modifier = Modifier.testTag("onboarding_mock_btn")
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Injecter une trame test")
        }
    }
}

@Composable
fun MeteoHeroCard(reading: MeteoReading) {
    // Calcul de comfort - Humidex ou Indice simplifié
    val isComfortable = reading.temperature in 18.0f..26.0f && reading.humidity in 35.0f..65.0f

    val gradient = Brush.linearGradient(
        colors = if (isComfortable) {
            listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6)) // Gorgeous premium blue gradients matching the style
        } else {
            listOf(Color(0xFF334155), Color(0xFF64748B)) // Modern Slate Grey/Blue
        }
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Statut Ambiant",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isComfortable) "Ambiance Confortable" else "Conditions Extérieures",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Altitude locale calculée de ${String.format("%.1f", reading.altitude)}m au dessus du niveau de la mer.",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Icon(
                        imageVector = if (reading.radiation > 300f) Icons.Default.WbSunny else Icons.Default.WbCloudy,
                        contentDescription = null,
                        tint = if (reading.radiation > 300f) Color(0xFFFFD54F) else Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SensorCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF64748B) // Soft Slate text
                )
                
                Spacer(modifier = Modifier.height(2.dp))

                // Parse value if it has unity to make unity font size smaller
                val parts = value.split(" ")
                if (parts.size >= 2) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = parts[0],
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = parts.subList(1, parts.size).joinToString(" "),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Normal
                            ),
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                } else {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color(0xFF0F172A)
                    )
                }

                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = tint.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryTab(viewModel: MeteoViewModel) {
    val readings by viewModel.allReadings.collectAsStateWithLifecycle()

    if (readings.isEmpty()) {
        EmptyHistoryState()
    } else {
        // Calculate statistics reactive from history readings
        val avgTemp = readings.map { it.temperature }.average().toFloat()
        val maxTemp = readings.maxOf { it.temperature }
        val minTemp = readings.minOf { it.temperature }

        val avgHum = readings.map { it.humidity }.average().toFloat()
        val maxLux = readings.maxOf { it.radiation }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Header Panel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Moyennes & Records",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF0F172A)
                            )
                            IconButton(
                                onClick = { viewModel.clearHistory() },
                                modifier = Modifier.testTag("clear_history_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = "Effacer",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }
                        
                        Divider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color(0xFFF1F5F9)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "TEMPÉRATURE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color(0xFFEA580C)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Moy: ${String.format("%.1f", avgTemp)}°C",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF334155)
                                )
                                Text(
                                    text = "Max: ${String.format("%.1f", maxTemp)}°C / Min: ${String.format("%.1f", minTemp)}°C",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "HUMIDITÉ / LUX MAX",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color(0xFF2563EB)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Moy Hum: ${String.format("%.1f", avgHum)}%",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF334155)
                                )
                                Text(
                                    text = "Max Soleil: ${String.format("%.0f", maxLux)} Lux",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Journal de bord (${readings.size} trames enregistrées)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color(0xFF0F172A)
                )
            }

            items(readings) { itemReading ->
                HistoryRow(itemReading)
            }
        }
    }
}

@Composable
fun EmptyHistoryState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SnippetFolder,
            contentDescription = null,
            tint = Color(0xFF94A3B8).copy(alpha = 0.4f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Historique vide",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFF0F172A)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Les données reçues seront sauvegardées localement dans le DataLogger SQLite.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HistoryRow(reading: MeteoReading) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reading.getFormattedDateTime(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = Color(0xFF1E3A8A)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🌡  ${String.format("%.1f", reading.temperature)}°C",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "💧  ${String.format("%.1f", reading.humidity)}%",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = "☀  ${String.format("%.0f", reading.radiation)} Lx",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color(0xFFD97706)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pression: ${String.format("%.0f", reading.pressure)} hPa  |  Altitude: ${String.format("%.0f", reading.altitude)}m",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp
                    ),
                    color = Color(0xFF64748B)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "#${reading.id}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF475569)
                )
            }
        }
    }
}

@Composable
fun ConfigurationTab(viewModel: MeteoViewModel) {
    val host by viewModel.brokerHostForm.collectAsStateWithLifecycle()
    val port by viewModel.brokerPortForm.collectAsStateWithLifecycle()
    val username by viewModel.usernameForm.collectAsStateWithLifecycle()
    val password by viewModel.passwordForm.collectAsStateWithLifecycle()
    val clientId by viewModel.clientIdForm.collectAsStateWithLifecycle()
    val topic by viewModel.topicForm.collectAsStateWithLifecycle()

    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Liaison Maqiatto Broker",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF1E3A8A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cette application Android s'abonne au flux MQTT publié par l'ESP8266 NodeMCU. Modifiez les champs ci-dessous pour connecter vos propres terminaux si nécessaire.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 20.sp
                        ),
                        color = Color(0xFF475569)
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { viewModel.brokerHostForm.value = it },
                    label = { Text("Broker MQTT URL") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("config_host_input")
                )

                OutlinedTextField(
                    value = port,
                    onValueChange = { viewModel.brokerPortForm.value = it },
                    label = { Text("Port (Défaut: 1883)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("config_port_input")
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { viewModel.usernameForm.value = it },
                    label = { Text("Nom d'utilisateur / Email") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("config_user_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { viewModel.passwordForm.value = it },
                    label = { Text("Mot de passe") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = "Afficher mot de passe", tint = Color(0xFF64748B))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("config_password_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1.5f)) {
                        OutlinedTextField(
                            value = topic,
                            onValueChange = { viewModel.topicForm.value = it },
                            label = { Text("Topic Souscription") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("config_topic_input")
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = clientId,
                            onValueChange = { viewModel.clientIdForm.value = it },
                            label = { Text("Client ID") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("config_client_id_input")
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (connectionState == MqttConnectionState.CONNECTED) {
                    Button(
                        onClick = { viewModel.disconnectMqtt() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("disconnect_btn")
                    ) {
                        Icon(Icons.Default.PowerOff, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Déconnecter", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { viewModel.connectMqtt() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("connect_btn")
                    ) {
                        Icon(Icons.Default.Power, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (connectionState == MqttConnectionState.CONNECTING) "En cours..." else "Connecter", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.resetToDefaults() },
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("defaults_btn")
                ) {
                    Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, tint = Color(0xFF475569))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Defaults", color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Divider(color = Color(0xFFF1F5F9))
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Outils de Simulation",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Utile pour s'assurer du bon fonctionnement de la liaison et de la base de données interne sans BME280 réel connecté.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
                Button(
                    onClick = { viewModel.triggerMockReading() },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("simulation_mock_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simuler la réception d'une mesure",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
