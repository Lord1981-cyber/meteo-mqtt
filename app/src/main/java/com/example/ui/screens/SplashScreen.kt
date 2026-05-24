package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun MeteoSplashScreen(onFinished: () -> Unit) {
    // Animation states
    var startAnimate by remember { mutableStateOf(false) }
    
    // Scale, alpha and rotation animations for premium aesthetics
    val scale = animateFloatAsState(
        targetValue = if (startAnimate) 1.0f else 0.82f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val alpha = animateFloatAsState(
        targetValue = if (startAnimate) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 1000),
        label = "alpha"
    )

    val spinTransition = rememberInfiniteTransition(label = "spin")
    val sunRotation by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sunRotate"
    )

    LaunchedEffect(Unit) {
        startAnimate = true
        delay(2600) // Delay representation for splash screen
        onFinished()
    }

    // Gorgeous Cosmic Deep Space Gradient Background
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Slate 900
            Color(0xFF1E1B4B), // Indigo 950
            Color(0xFF030712)  // Gray 950
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
                .scale(scale.value)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Main Creative IoT / Weather Interactive Orb Visual
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF3B82F6).copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(1.5.dp, Color(0xFF3B82F6).copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Outer rotating/sun orb weather element
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(1.2f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier
                            .size(54.dp)
                            .scale(if (startAnimate) 1.0f else 0.5f)
                    )
                }

                // Cloud / overlay weather representation
                Box(
                    modifier = Modifier
                        .offset(x = 22.dp, y = 14.dp)
                        .scale(1.1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = Color(0xFF93C5FD),
                        modifier = Modifier.size(46.dp)
                    )
                }

                // MQTT Network pipeline overlay representation
                Box(
                    modifier = Modifier
                        .offset(x = (-28).dp, y = (-22).dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFF10B981))
                        .padding(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = "Liaison MQTT",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Text Titles
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "STATION MÉTÉO CONNECTÉE",
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.5.sp
                    ),
                    color = Color(0xFF3B82F6),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Météo Nowcast IoT",
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Liaison Temps Réel ESP8266 & MQTT Broker",
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // The Three Pillars: Meteo, MQTT, Previsions (Visual Chips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Meteo Pillar
                PillarBadge(
                    icon = Icons.Default.Speed,
                    label = "Capteurs",
                    sub = "BME280 Réel",
                    color = Color(0xFF0EA5E9)
                )

                // 2. MQTT Pillar
                PillarBadge(
                    icon = Icons.Default.Hub,
                    label = "MQTT Server",
                    sub = "MqttBroker",
                    color = Color(0xFF10B981)
                )

                // 3. Previsions Pillar
                PillarBadge(
                    icon = Icons.Default.TrendingUp,
                    label = "Prévisions",
                    sub = "Nowcast 3j",
                    color = Color(0xFF8B5CF6)
                )
            }
        }

        // Bottom Loading & Copy Indicator Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = Color(0xFF3B82F6),
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )

            Text(
                text = "Établissement du flux de télémétrie...",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
fun RowScope.PillarBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    sub: String,
    color: Color
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.45f))
            .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            Text(
                text = sub,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color(0xFF64748B)
            )
        }
    }
}
