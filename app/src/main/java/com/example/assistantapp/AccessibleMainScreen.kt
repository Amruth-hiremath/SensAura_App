package com.example.assistantapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibleMainScreen(navController: NavController) {
    val context = LocalContext.current
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var isFallDetectionActive by remember { mutableStateOf(false) }

    // Initialize TTS
    LaunchedEffect(Unit) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                textToSpeech?.speak(
                    "Welcome to Blind Navigation Assistant. This is the main screen with accessible navigation options. Double tap on any button to activate it.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    null
                )
            }
        }

        // Check if fall detection service is running
        isFallDetectionActive = isServiceRunning(context, FallDetectionService::class.java)
    }

    DisposableEffect(Unit) {
        onDispose {
            textToSpeech?.shutdown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
            .semantics { contentDescription = "Main navigation screen for Blind Navigation Assistant" },
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // App Title
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Blind Navigation Assistant - Your personal safety and navigation companion"
                },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Text(
                text = "Blind Navigation\nAssistant",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                lineHeight = 32.sp
            )
        }

        // Fall Detection Status Card
        FallDetectionStatusCard(
            isActive = isFallDetectionActive,
            onToggle = { active ->
                if (active) {
                    startFallDetectionService(context)
                    textToSpeech?.speak("Fall detection activated", TextToSpeech.QUEUE_FLUSH, null, null)
                } else {
                    stopFallDetectionService(context)
                    textToSpeech?.speak("Fall detection deactivated", TextToSpeech.QUEUE_FLUSH, null, null)
                }
                isFallDetectionActive = active
            },
            textToSpeech = textToSpeech
        )

        // Navigation Buttons
        AccessibleButton(
            icon = Icons.Default.Camera,
            title = "Visual Assistant",
            description = "Camera-based object recognition and scene description",
            onClick = {
                textToSpeech?.speak("Opening Visual Assistant", TextToSpeech.QUEUE_FLUSH, null, null)
                navController.navigate("blind_mode")
            },
            backgroundColor = Color(0xFF2196F3),
            textToSpeech = textToSpeech
        )

        AccessibleButton(
            icon = Icons.Default.Mic,
            title = "Voice Commands",
            description = "Voice-controlled navigation and assistance",
            onClick = {
                textToSpeech?.speak("Opening Voice Commands", TextToSpeech.QUEUE_FLUSH, null, null)
                navController.navigate("voice_commands")
            },
            backgroundColor = Color(0xFF4CAF50),
            textToSpeech = textToSpeech
        )

        AccessibleButton(
            icon = Icons.Default.ContactPhone,
            title = "Emergency Contacts",
            description = "Manage emergency contacts for fall detection alerts",
            onClick = {
                textToSpeech?.speak("Opening Emergency Contacts", TextToSpeech.QUEUE_FLUSH, null, null)
                navController.navigate("emergency_contacts")
            },
            backgroundColor = Color(0xFFFF5722),
            textToSpeech = textToSpeech
        )

        AccessibleButton(
            icon = Icons.Default.Settings,
            title = "Settings",
            description = "App settings and accessibility options",
            onClick = {
                textToSpeech?.speak("Opening Settings", TextToSpeech.QUEUE_FLUSH, null, null)
                navController.navigate("settings")
            },
            backgroundColor = Color(0xFF9C27B0),
            textToSpeech = textToSpeech
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Emergency Instructions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Emergency Instructions: In case of emergency, the app will automatically detect falls and send alerts to your emergency contacts. You can also manually trigger emergency alerts by saying 'Emergency Help' in voice commands."
                },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Emergency Instructions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "• Fall detection automatically sends alerts to emergency contacts\n• Manual emergency alerts available in voice commands\n• Ensure location services are enabled for accurate emergency location",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun AccessibleButton(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    textToSpeech: TextToSpeech?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable {
                onClick()
            }
            .semantics {
                contentDescription = "$title button. $description. Double tap to activate."
                role = Role.Button
            },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun FallDetectionStatusCard(
    isActive: Boolean,
    onToggle: (Boolean) -> Unit,
    textToSpeech: TextToSpeech?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = if (isActive) {
                    "Fall detection is currently active and monitoring for falls"
                } else {
                    "Fall detection is currently inactive. Double tap to activate fall monitoring."
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF4CAF50) else Color(0xFFFF9800)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )

                Column {
                    Text(
                        text = "Fall Detection",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isActive) "Active - Monitoring" else "Inactive - Tap to enable",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Switch(
                checked = isActive,
                onCheckedChange = onToggle,
                modifier = Modifier.semantics {
                    contentDescription = if (isActive) {
                        "Fall detection switch. Currently on. Double tap to turn off fall detection."
                    } else {
                        "Fall detection switch. Currently off. Double tap to turn on fall detection."
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.White.copy(alpha = 0.3f),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
        }
    }
}

private fun startFallDetectionService(context: Context) {
    val intent = Intent(context, FallDetectionService::class.java)
    context.startForegroundService(intent)
}

private fun stopFallDetectionService(context: Context) {
    val intent = Intent(context, FallDetectionService::class.java)
    context.stopService(intent)
}

private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}
