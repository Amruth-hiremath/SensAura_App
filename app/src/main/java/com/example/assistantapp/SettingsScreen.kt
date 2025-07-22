package com.example.assistantapp

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }

    // Settings state
    val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    var speechRate by remember { mutableStateOf(sharedPrefs.getFloat("speech_rate", 1.0f)) }
    var speechVolume by remember { mutableStateOf(sharedPrefs.getFloat("speech_volume", 1.0f)) }
    var vibrationEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("vibration_enabled", true)) }
    var highContrastMode by remember { mutableStateOf(sharedPrefs.getBoolean("high_contrast", false)) }
    var extendedTouchTargets by remember { mutableStateOf(sharedPrefs.getBoolean("extended_touch", true)) }
    var fallSensitivity by remember { mutableStateOf(sharedPrefs.getFloat("fall_sensitivity", 0.7f)) }

    // Initialize TTS
    LaunchedEffect(Unit) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                textToSpeech?.setSpeechRate(speechRate)
                textToSpeech?.speak(
                    "Settings screen. Here you can customize accessibility options, speech settings, and fall detection sensitivity.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    null
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            textToSpeech?.shutdown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .semantics { contentDescription = "Settings and accessibility preferences screen" },
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    textToSpeech?.speak("Going back", TextToSpeech.QUEUE_FLUSH, null, null)
                    navController.popBackStack()
                },
                modifier = Modifier.semantics {
                    contentDescription = "Back button. Double tap to return to main screen"
                }
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(32.dp))
            }

            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Speech Settings Section
        SettingsSection(
            title = "Speech Settings",
            icon = Icons.Default.VolumeUp,
            description = "Customize text-to-speech options"
        ) {
            // Speech Rate
            SliderSetting(
                title = "Speech Rate",
                value = speechRate,
                onValueChange = { newRate ->
                    speechRate = newRate
                    textToSpeech?.setSpeechRate(newRate)
                    saveSetting(context, "speech_rate", newRate)
                },
                valueRange = 0.5f..2.0f,
                description = "Current rate: ${String.format("%.1f", speechRate)}x",
                contentDescription = "Speech rate slider. Current value ${String.format("%.1f", speechRate)} times normal speed"
            )

            // Speech Volume
            SliderSetting(
                title = "Speech Volume",
                value = speechVolume,
                onValueChange = { newVolume ->
                    speechVolume = newVolume
                    saveSetting(context, "speech_volume", newVolume)
                },
                valueRange = 0.1f..1.0f,
                description = "Current volume: ${(speechVolume * 100).toInt()}%",
                contentDescription = "Speech volume slider. Current value ${(speechVolume * 100).toInt()} percent"
            )

            // Test Speech Button
            Button(
                onClick = {
                    textToSpeech?.speak(
                        "This is a test of the current speech settings",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Test speech settings button. Double tap to hear a sample with current settings"
                    }
            ) {
                Text("Test Speech Settings")
            }
        }

        // Accessibility Settings Section
        SettingsSection(
            title = "Accessibility",
            icon = Icons.Default.Accessibility,
            description = "Enhanced accessibility features"
        ) {
            // Vibration Feedback
            SwitchSetting(
                title = "Vibration Feedback",
                checked = vibrationEnabled,
                onCheckedChange = { enabled ->
                    vibrationEnabled = enabled
                    saveSetting(context, "vibration_enabled", enabled)
                    if (enabled) {
                        textToSpeech?.speak("Vibration feedback enabled", TextToSpeech.QUEUE_FLUSH, null, null)
                    } else {
                        textToSpeech?.speak("Vibration feedback disabled", TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                },
                description = "Provide haptic feedback for interactions",
                contentDescription = "Vibration feedback toggle. Currently ${if (vibrationEnabled) "enabled" else "disabled"}"
            )

            // High Contrast Mode
            SwitchSetting(
                title = "High Contrast Mode",
                checked = highContrastMode,
                onCheckedChange = { enabled ->
                    highContrastMode = enabled
                    saveSetting(context, "high_contrast", enabled)
                    textToSpeech?.speak(
                        if (enabled) "High contrast mode enabled" else "High contrast mode disabled",
                        TextToSpeech.QUEUE_FLUSH, null, null
                    )
                },
                description = "Increase visual contrast for better visibility",
                contentDescription = "High contrast mode toggle. Currently ${if (highContrastMode) "enabled" else "disabled"}"
            )

            // Extended Touch Targets
            SwitchSetting(
                title = "Extended Touch Targets",
                checked = extendedTouchTargets,
                onCheckedChange = { enabled ->
                    extendedTouchTargets = enabled
                    saveSetting(context, "extended_touch", enabled)
                    textToSpeech?.speak(
                        if (enabled) "Extended touch targets enabled" else "Extended touch targets disabled",
                        TextToSpeech.QUEUE_FLUSH, null, null
                    )
                },
                description = "Larger touch areas for easier interaction",
                contentDescription = "Extended touch targets toggle. Currently ${if (extendedTouchTargets) "enabled" else "disabled"}"
            )
        }

        // Fall Detection Settings Section
        SettingsSection(
            title = "Fall Detection",
            icon = Icons.Default.Speed,
            description = "Customize fall detection sensitivity"
        ) {
            SliderSetting(
                title = "Fall Detection Sensitivity",
                value = fallSensitivity,
                onValueChange = { newSensitivity ->
                    fallSensitivity = newSensitivity
                    saveSetting(context, "fall_sensitivity", newSensitivity)
                },
                valueRange = 0.3f..1.0f,
                description = when {
                    fallSensitivity < 0.5f -> "Low sensitivity (fewer false alarms)"
                    fallSensitivity < 0.8f -> "Medium sensitivity (balanced)"
                    else -> "High sensitivity (more responsive)"
                },
                contentDescription = "Fall detection sensitivity slider. Current level: ${when {
                    fallSensitivity < 0.5f -> "Low"
                    fallSensitivity < 0.8f -> "Medium"
                    else -> "High"
                }}"
            )
        }

        // Reset Settings
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Reset settings section"
                }
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Reset Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        // Reset to defaults
                        speechRate = 1.0f
                        speechVolume = 1.0f
                        vibrationEnabled = true
                        highContrastMode = false
                        extendedTouchTargets = true
                        fallSensitivity = 0.7f

                        // Save defaults
                        val editor = sharedPrefs.edit()
                        editor.putFloat("speech_rate", 1.0f)
                        editor.putFloat("speech_volume", 1.0f)
                        editor.putBoolean("vibration_enabled", true)
                        editor.putBoolean("high_contrast", false)
                        editor.putBoolean("extended_touch", true)
                        editor.putFloat("fall_sensitivity", 0.7f)
                        editor.apply()

                        textToSpeech?.setSpeechRate(1.0f)
                        textToSpeech?.speak("Settings reset to defaults", TextToSpeech.QUEUE_FLUSH, null, null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Reset all settings to default values button. Double tap to reset"
                        }
                ) {
                    Text("Reset All Settings to Defaults")
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$title section. $description"
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            content()
        }
    }
}

@Composable
fun SliderSetting(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    description: String,
    contentDescription: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, fontWeight = FontWeight.Medium)
            Text(text = description, fontSize = 14.sp)
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    this.contentDescription = contentDescription
                }
        )
    }
}

@Composable
fun SwitchSetting(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String,
    contentDescription: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                this.contentDescription = contentDescription
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Medium)
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

private fun saveSetting(context: Context, key: String, value: Float) {
    val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    sharedPrefs.edit().putFloat(key, value).apply()
}

private fun saveSetting(context: Context, key: String, value: Boolean) {
    val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean(key, value).apply()
}
