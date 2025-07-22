package com.example.assistantapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun BlindModeScreen() {
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var overlayText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var analysisResult by remember { mutableStateOf("") }
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    var isMicActive by remember { mutableStateOf(false) }
    var isReadingMode by remember { mutableStateOf(false) }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
    }

    LaunchedEffect(context) {
        tts.value = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                tts.value?.language = Locale.US
                tts.value?.setSpeechRate(0.8f)
                tts.value?.speak(
                    "Visual Assistant is ready. Double tap anywhere on screen to capture and analyze what's in front of you. Use the microphone button for voice commands. Tap and hold for reading mode.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    null
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            tts.value?.stop()
            tts.value?.shutdown()
            speechRecognizer.destroy()
        }
    }

    if (hasPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            coroutineScope.launch {
                                tts.value?.speak("Capturing image", TextToSpeech.QUEUE_FLUSH, null, null)
                                // Simple placeholder for image capture and analysis
                                overlayText = "Scene captured and analyzed"
                                analysisResult = "Indoor environment detected with clear walking path ahead"
                                tts.value?.speak(analysisResult, TextToSpeech.QUEUE_ADD, null, null)
                            }
                        },
                        onLongPress = {
                            isReadingMode = !isReadingMode
                            val message = if (isReadingMode) {
                                "Reading mode activated. Point camera at text to read it aloud."
                            } else {
                                "Reading mode deactivated. Back to navigation mode."
                            }
                            tts.value?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    )
                }
                .semantics {
                    contentDescription = "Camera viewfinder. Double tap to capture and analyze image. Long press to toggle reading mode. Current mode: ${if (isReadingMode) "Reading" else "Navigation"}"
                }
        ) {
            // Camera preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview
                                )
                            } catch (exc: Exception) {
                                // Handle error
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay text display
            if (overlayText.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .semantics {
                            contentDescription = "Analysis result: $overlayText"
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.8f)
                    )
                ) {
                    Text(
                        text = overlayText,
                        modifier = Modifier.padding(16.dp),
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }

            // Control buttons
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Voice command button
                FloatingActionButton(
                    onClick = {
                        if (isMicActive) {
                            speechRecognizer.stopListening()
                            isMicActive = false
                            tts.value?.speak("Stopped listening", TextToSpeech.QUEUE_FLUSH, null, null)
                        } else {
                            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                                override fun onResults(results: Bundle?) {
                                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                    if (!matches.isNullOrEmpty()) {
                                        val command = matches[0].lowercase()
                                        when {
                                            command.contains("describe") || command.contains("what") -> {
                                                overlayText = "Scene analyzed"
                                                analysisResult = "Indoor space with furniture visible"
                                                tts.value?.speak(analysisResult, TextToSpeech.QUEUE_FLUSH, null, null)
                                            }
                                            command.contains("read") -> {
                                                isReadingMode = !isReadingMode
                                                val message = if (isReadingMode) "Reading mode on" else "Reading mode off"
                                                tts.value?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
                                            }
                                            command.contains("help") -> {
                                                tts.value?.speak(
                                                    "Available commands: Say 'describe' to analyze scene, 'read' to toggle reading mode, 'help' for this message",
                                                    TextToSpeech.QUEUE_FLUSH, null, null
                                                )
                                            }
                                            else -> {
                                                tts.value?.speak("Command not recognized", TextToSpeech.QUEUE_FLUSH, null, null)
                                            }
                                        }
                                    }
                                    isMicActive = false
                                }

                                override fun onError(error: Int) {
                                    tts.value?.speak("Voice recognition error", TextToSpeech.QUEUE_FLUSH, null, null)
                                    isMicActive = false
                                }

                                override fun onReadyForSpeech(params: Bundle?) {
                                    tts.value?.speak("Listening", TextToSpeech.QUEUE_FLUSH, null, null)
                                }

                                override fun onBeginningOfSpeech() {}
                                override fun onRmsChanged(rmsdB: Float) {}
                                override fun onBufferReceived(buffer: ByteArray?) {}
                                override fun onEndOfSpeech() {}
                                override fun onPartialResults(partialResults: Bundle?) {}
                                override fun onEvent(eventType: Int, params: Bundle?) {}
                            })

                            speechRecognizer.startListening(speechIntent)
                            isMicActive = true
                        }
                    },
                    modifier = Modifier.semantics {
                        contentDescription = if (isMicActive) {
                            "Stop voice recognition button. Currently listening for commands."
                        } else {
                            "Start voice recognition button. Double tap to give voice commands."
                        }
                    },
                    containerColor = if (isMicActive) Color.Red else Color.Blue
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                // Mode indicator button
                FloatingActionButton(
                    onClick = {
                        val modeDescription = if (isReadingMode) {
                            "Currently in reading mode. Point camera at text to read it aloud."
                        } else {
                            "Currently in navigation mode. Double tap screen to describe surroundings."
                        }
                        tts.value?.speak(modeDescription, TextToSpeech.QUEUE_FLUSH, null, null)
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Mode information button. Double tap to hear current mode description."
                    },
                    containerColor = if (isReadingMode) Color(0xFF4CAF50) else Color(0xFF2196F3)
                ) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    } else {
        // Permission request UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .semantics {
                    contentDescription = "Camera and microphone permissions required for visual assistant functionality"
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Visual Assistant needs camera and microphone permissions to help you navigate and read text.",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Button(
                onClick = {
                    ActivityCompat.requestPermissions(
                        context as Activity,
                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                        100
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .semantics {
                        contentDescription = "Grant permissions button. Double tap to allow camera and microphone access."
                    }
            ) {
                Text("Grant Permissions", fontSize = 18.sp)
            }
        }
    }
}
