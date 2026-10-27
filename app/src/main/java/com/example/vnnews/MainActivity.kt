package com.example.vnnews

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.vnnews.ui.navigation.NewsApp
import com.example.vnnews.ui.theme.VnNewsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VnNewsTheme {
                NewsApp()
            }
        }
    }
}
