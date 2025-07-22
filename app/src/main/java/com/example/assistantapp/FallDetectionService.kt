package com.example.assistantapp

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.sqrt

class FallDetectionService : Service(), SensorEventListener, LocationListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var locationManager: LocationManager
    private lateinit var vibrator: Vibrator
    private lateinit var textToSpeech: TextToSpeech
    private var currentLocation: Location? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Fall detection parameters
    private val fallThreshold = 15.0f // Acceleration threshold for fall detection
    private val impactThreshold = 2.0f // Low acceleration threshold after fall
    private var isInFreeFall = false
    private var freeFallStartTime = 0L
    private val freeFallDuration = 300L // 300ms
    private val impactWindow = 1000L // 1 second after free fall

    companion object {
        const val CHANNEL_ID = "FallDetectionChannel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Initialize Text-to-Speech
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.getDefault()
            }
        }

        createNotificationChannel()
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        // Register accelerometer listener
        accelerometer?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates(this)
        textToSpeech.shutdown()
        serviceScope.cancel()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            if (sensorEvent.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = sensorEvent.values[0]
                val y = sensorEvent.values[1]
                val z = sensorEvent.values[2]

                val acceleration = sqrt(x * x + y * y + z * z)
                val currentTime = System.currentTimeMillis()

                // Detect free fall (sudden drop in acceleration)
                if (acceleration < impactThreshold && !isInFreeFall) {
                    isInFreeFall = true
                    freeFallStartTime = currentTime
                }

                // Detect impact after free fall
                if (isInFreeFall && acceleration > fallThreshold) {
                    val freeFallDuration = currentTime - freeFallStartTime
                    if (freeFallDuration in 100..800) { // Reasonable free fall duration
                        detectFall()
                    }
                    isInFreeFall = false
                }

                // Reset free fall state if too much time has passed
                if (isInFreeFall && (currentTime - freeFallStartTime) > impactWindow) {
                    isInFreeFall = false
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun detectFall() {
        serviceScope.launch {
            // Provide audio feedback
            speakText("Fall detected! Sending emergency alert in 10 seconds. Say 'Cancel' to stop.")

            // Vibrate to alert user
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(longArrayOf(0, 1000, 500, 1000), -1)
            }

            // Wait for 10 seconds for user to cancel
            delay(10000)

            // Send emergency alerts
            sendEmergencyAlert()
        }
    }

    private fun sendEmergencyAlert() {
        val emergencyContacts = getEmergencyContacts()
        val locationText = currentLocation?.let { location ->
            "Location: https://maps.google.com/?q=${location.latitude},${location.longitude}"
        } ?: "Location unavailable"

        val message = "EMERGENCY: Fall detected! Please check on me immediately. $locationText"

        emergencyContacts.forEach { contact ->
            sendSMS(contact, message)
        }

        speakText("Emergency alert sent to your contacts.")
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getEmergencyContacts(): List<String> {
        val sharedPrefs = getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE)
        val contacts = mutableListOf<String>()

        for (i in 1..3) {
            val contact = sharedPrefs.getString("contact_$i", null)
            if (!contact.isNullOrEmpty()) {
                contacts.add(contact)
            }
        }

        return contacts
    }

    private fun speakText(text: String) {
        if (::textToSpeech.isInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun startLocationUpdates() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000L, // 10 seconds
                    10f,    // 10 meters
                    this
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Fall Detection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitors for falls and sends emergency alerts"
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fall Detection Active")
            .setContentText("Monitoring for falls in the background")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
