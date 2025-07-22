package com.example.assistantapp

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun Navigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "accessible_main") {
        composable("accessible_main") { AccessibleMainScreen(navController) }
        composable("mainPage") { MainPage(navController) }
        composable("blind_mode") { BlindModeScreen() }
        composable("emergency_contacts") { EmergencyContactsScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
        composable("voice_commands") { VoiceCommandsScreen(navController) }
    }
}
