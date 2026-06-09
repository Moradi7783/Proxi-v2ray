package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ProxyAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ProxyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fully support full edge to edge bleeds safe navigation overlays
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                val viewModel: ProxyViewModel = viewModel()
                ProxyAppScreen(viewModel = viewModel)
            }
        }
    }
}
