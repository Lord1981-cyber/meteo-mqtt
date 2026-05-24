package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.MeteoViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MeteoSplashScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    
    // Instantiate our ViewModel with the Factory referencing our Application singletons
    private val viewModel: MeteoViewModel by viewModels {
        MeteoViewModel.Factory(
            (application as MeteoApplication).repository,
            (application as MeteoApplication).mqttManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var showSplash by remember { mutableStateOf(true) }

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Crossfade(
                        targetState = showSplash,
                        label = "AppStartTransition"
                    ) { isSplash ->
                        if (isSplash) {
                            MeteoSplashScreen(
                                onFinished = { showSplash = false }
                            )
                        } else {
                            DashboardScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

