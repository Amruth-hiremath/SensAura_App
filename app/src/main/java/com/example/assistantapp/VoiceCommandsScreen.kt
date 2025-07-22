package com.example.assistantapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import java.util.*

@Composable
fun VoiceCommandsScreen(navController: NavController) {
    val context = LocalContext.current
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var lastCommand by remember { mutableStateOf("") }
    var recognizedText by remember { mutableStateOf("") }

    // Initialize TTS and Speech Recognition
    LaunchedEffect(Unit) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                textToSpeech?.speak(
                    "Voice Commands screen. You can use voice commands for navigation and emergency alerts. Say 'Help' to hear available commands.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    null
                )
            }
        }

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            textToSpeech?.shutdown()
            speechRecognizer?.destroy()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .semantics { contentDescription = "Voice Commands screen for hands-free control" },
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
                text = "Voice Commands",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Voice Recognition Status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = if (isListening) {
                        "Voice recognition is active and listening for commands"
                    } else {
                        "Voice recognition is inactive. Double tap the microphone button to start listening"
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = if (isListening) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isListening) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column {
                    Text(
                        text = if (isListening) "Listening..." else "Voice Recognition Ready",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isListening) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (recognizedText.isNotEmpty()) "\"$recognizedText\"" else "Tap microphone to start",
                        fontSize = 14.sp,
                        color = if (isListening) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Voice Control Button
        Button(
            onClick = {
                if (isListening) {
                    stopListening(speechRecognizer, textToSpeech) { isListening = false }
                } else {
                    startListening(context, speechRecognizer, textToSpeech,
                        onResult = { result ->
                            recognizedText = result
                            processVoiceCommand(result, context, navController, textToSpeech)
                            isListening = false
                        },
                        onError = { isListening = false }
                    ) { isListening = true }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .semantics {
                    contentDescription = if (isListening) {
                        "Stop listening button. Double tap to stop voice recognition"
                    } else {
                        "Start listening button. Double tap to begin voice recognition"
                    }
                },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isListening) Color(0xFFFF5722) else Color(0xFF2196F3)
            )
        ) {
            Icon(
                if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isListening) "Stop Listening" else "Start Listening",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Emergency Button
        Button(
            onClick = {
                textToSpeech?.speak("Emergency alert activated", TextToSpeech.QUEUE_FLUSH, null, null)
                triggerEmergencyAlert(context)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .semantics {
                    contentDescription = "Emergency alert button. Double tap to immediately send emergency alerts to all emergency contacts"
                },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
        ) {
            Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Emergency Alert",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Available Commands
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Available voice commands reference card"
                }
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Available Commands",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                val commands = listOf(
                    "\"Help\" - Hear available commands",
                    "\"Emergency\" - Send emergency alert",
                    "\"Camera\" - Open visual assistant",
                    "\"Contacts\" - Manage emergency contacts",
                    "\"Settings\" - Open app settings",
                    "\"Go back\" - Return to previous screen",
                    "\"What can you see\" - Describe surroundings",
                    "\"Read text\" - Read text from camera"
                )

                commands.forEach { command ->
                    Text(
                        text = "• $command",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        // Last Command Display
        if (lastCommand.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Last executed command: $lastCommand"
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = "Last Command: \"$lastCommand\"",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun startListening(
    context: Context,
    speechRecognizer: SpeechRecognizer?,
    textToSpeech: TextToSpeech?,
    onResult: (String) -> Unit,
    onError: () -> Unit,
    onStarted: () -> Unit
) {
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
        textToSpeech?.speak("Microphone permission required", TextToSpeech.QUEUE_FLUSH, null, null)
        return
    }

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a command")
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    speechRecognizer?.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            onStarted()
            textToSpeech?.speak("Listening", TextToSpeech.QUEUE_FLUSH, null, null)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                onResult(matches[0])
            }
        }

        override fun onError(error: Int) {
            onError()
            textToSpeech?.speak("Voice recognition error", TextToSpeech.QUEUE_FLUSH, null, null)
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })

    speechRecognizer?.startListening(intent)
}

private fun stopListening(
    speechRecognizer: SpeechRecognizer?,
    textToSpeech: TextToSpeech?,
    onStopped: () -> Unit
) {
    speechRecognizer?.stopListening()
    textToSpeech?.speak("Stopped listening", TextToSpeech.QUEUE_FLUSH, null, null)
    onStopped()
}

private fun processVoiceCommand(
    command: String,
    context: Context,
    navController: NavController,
    textToSpeech: TextToSpeech?
) {
    val lowerCommand = command.lowercase()

    when {
        lowerCommand.contains("help") -> {
            textToSpeech?.speak(
                "Available commands: Emergency, Camera, Contacts, Settings, Go back, What can you see, Read text",
                TextToSpeech.QUEUE_FLUSH, null, null
            )
        }
        lowerCommand.contains("emergency") -> {
            triggerEmergencyAlert(context)
            textToSpeech?.speak("Emergency alert sent", TextToSpeech.QUEUE_FLUSH, null, null)
        }
        lowerCommand.contains("camera") || lowerCommand.contains("visual") -> {
            navController.navigate("blind_mode")
            textToSpeech?.speak("Opening camera", TextToSpeech.QUEUE_FLUSH, null, null)
        }
        lowerCommand.contains("contact") -> {
            navController.navigate("emergency_contacts")
            textToSpeech?.speak("Opening contacts", TextToSpeech.QUEUE_FLUSH, null, null)
        }
        lowerCommand.contains("setting") -> {
            navController.navigate("settings")
            textToSpeech?.speak("Opening settings", TextToSpeech.QUEUE_FLUSH, null, null)
        }
        lowerCommand.contains("back") || lowerCommand.contains("return") -> {
            navController.popBackStack()
            textToSpeech?.speak("Going back", TextToSpeech.QUEUE_FLUSH, null, null)
        }
        else -> {
            textToSpeech?.speak("Command not recognized. Say 'Help' for available commands", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
}

private fun triggerEmergencyAlert(context: Context) {
    // This would integrate with the emergency contact system
    val intent = Intent(context, FallDetectionService::class.java)
    intent.action = "MANUAL_EMERGENCY"
    context.startService(intent)
}
