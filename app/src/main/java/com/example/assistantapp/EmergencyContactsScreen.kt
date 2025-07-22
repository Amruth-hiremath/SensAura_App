package com.example.assistantapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(navController: NavController) {
    val context = LocalContext.current
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }

    // Initialize TTS
    LaunchedEffect(Unit) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                textToSpeech?.speak(
                    "Emergency Contacts screen. You can add up to 3 emergency contacts here. Double tap on buttons to activate them.",
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

    val sharedPrefs = context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE)

    var contact1 by remember { mutableStateOf(sharedPrefs.getString("contact_1", "") ?: "") }
    var contact1Name by remember { mutableStateOf(sharedPrefs.getString("contact_1_name", "") ?: "") }
    var contact2 by remember { mutableStateOf(sharedPrefs.getString("contact_2", "") ?: "") }
    var contact2Name by remember { mutableStateOf(sharedPrefs.getString("contact_2_name", "") ?: "") }
    var contact3 by remember { mutableStateOf(sharedPrefs.getString("contact_3", "") ?: "") }
    var contact3Name by remember { mutableStateOf(sharedPrefs.getString("contact_3_name", "") ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .semantics { contentDescription = "Emergency Contacts Management Screen" },
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header with back button
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
                    contentDescription = "Back button. Double tap to go back to previous screen"
                }
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "Emergency Contacts",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics {
                    contentDescription = "Emergency Contacts screen title"
                }
            )
        }

        // Instructions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Instructions: Add up to 3 emergency contacts. These contacts will receive SMS alerts if a fall is detected."
                },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                text = "Add up to 3 emergency contacts. These contacts will receive SMS alerts if a fall is detected.",
                modifier = Modifier.padding(16.dp),
                fontSize = 16.sp
            )
        }

        // Contact 1
        EmergencyContactCard(
            title = "Emergency Contact 1",
            name = contact1Name,
            phoneNumber = contact1,
            onNameChange = {
                contact1Name = it
                saveContact(context, "contact_1_name", it)
            },
            onPhoneChange = {
                contact1 = it
                saveContact(context, "contact_1", it)
            },
            onClear = {
                contact1Name = ""
                contact1 = ""
                clearContact(context, "contact_1")
                clearContact(context, "contact_1_name")
                textToSpeech?.speak("Contact 1 cleared", TextToSpeech.QUEUE_FLUSH, null, null)
            },
            textToSpeech = textToSpeech
        )

        // Contact 2
        EmergencyContactCard(
            title = "Emergency Contact 2",
            name = contact2Name,
            phoneNumber = contact2,
            onNameChange = {
                contact2Name = it
                saveContact(context, "contact_2_name", it)
            },
            onPhoneChange = {
                contact2 = it
                saveContact(context, "contact_2", it)
            },
            onClear = {
                contact2Name = ""
                contact2 = ""
                clearContact(context, "contact_2")
                clearContact(context, "contact_2_name")
                textToSpeech?.speak("Contact 2 cleared", TextToSpeech.QUEUE_FLUSH, null, null)
            },
            textToSpeech = textToSpeech
        )

        // Contact 3
        EmergencyContactCard(
            title = "Emergency Contact 3",
            name = contact3Name,
            phoneNumber = contact3,
            onNameChange = {
                contact3Name = it
                saveContact(context, "contact_3_name", it)
            },
            onPhoneChange = {
                contact3 = it
                saveContact(context, "contact_3", it)
            },
            onClear = {
                contact3Name = ""
                contact3 = ""
                clearContact(context, "contact_3")
                clearContact(context, "contact_3_name")
                textToSpeech?.speak("Contact 3 cleared", TextToSpeech.QUEUE_FLUSH, null, null)
            },
            textToSpeech = textToSpeech
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Test Emergency Alert Button
        Button(
            onClick = {
                textToSpeech?.speak("Testing emergency alert", TextToSpeech.QUEUE_FLUSH, null, null)
                // Here you could add test functionality
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .semantics {
                    contentDescription = "Test emergency alert button. Double tap to send a test message to all emergency contacts"
                },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Test Emergency Alert",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactCard(
    title: String,
    name: String,
    phoneNumber: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onClear: () -> Unit,
    textToSpeech: TextToSpeech?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$title card. Contains name and phone number fields"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics {
                        contentDescription = title
                    }
                )

                if (name.isNotEmpty() || phoneNumber.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.semantics {
                            contentDescription = "Clear $title button. Double tap to remove this contact"
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Clear contact",
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Contact Name") },
                placeholder = { Text("Enter contact name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Contact name field for $title. Current value: ${if (name.isEmpty()) "empty" else name}"
                    },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                }
            )

            // Phone number field
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = onPhoneChange,
                label = { Text("Phone Number") },
                placeholder = { Text("Enter phone number") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Phone number field for $title. Current value: ${if (phoneNumber.isEmpty()) "empty" else phoneNumber}"
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                leadingIcon = {
                    Text("📞", fontSize = 20.sp)
                }
            )
        }
    }
}

private fun saveContact(context: Context, key: String, value: String) {
    val sharedPrefs = context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE)
    sharedPrefs.edit().putString(key, value).apply()
}

private fun clearContact(context: Context, key: String) {
    val sharedPrefs = context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE)
    sharedPrefs.edit().remove(key).apply()
}
