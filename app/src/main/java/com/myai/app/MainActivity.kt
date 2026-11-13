package com.myai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.myai.app.ui.chat.ChatScreen
import com.myai.app.ui.memory.MemoryScreen
import com.myai.app.ui.settings.SettingsScreen
import com.myai.app.ui.theme.MyAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyAITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MyAIApp()
                }
            }
        }
    }
}

@Composable
fun MyAIApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "chat"
    ) {
        composable("chat") {
            ChatScreen(
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToMemory = {
                    navController.navigate("memory")
                }
            )
        }
        composable("memory") {
            MemoryScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
