package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.HayatiApp
import com.example.ui.HayatiViewModel
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {
    private val viewModel: HayatiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure App Check with launch intent before UI or Firebase operations
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            HayatiApp(viewModel = viewModel)
        }
    }
}
