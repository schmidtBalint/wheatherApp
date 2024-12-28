package com.example.jetpackcompose.service

import android.app.*
import android.content.*
import android.os.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.PendingIntent
import androidx.core.content.ContextCompat
import com.example.jetpackcompose.MainActivity
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.jetpackcompose.ui.views.dataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PopupService : Service() {

    // Handler to manage delayed tasks on the main thread
    private val handler = Handler(Looper.getMainLooper())
    private var delayMillis: Long = -1L
    private var i = 0
    private val dataStore by lazy { applicationContext.dataStore }
    private var isNotificationEnabled: Boolean = false

    // BroadcastReceiver to handle updates to the timer option
    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val newTimerOption = intent?.getStringExtra("timer_option") ?: "Deactivated"
            updateTimerOption(newTimerOption)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()
        registerUpdateReceiver()
        initializeTimerFromSettings()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(showNotificationRunnable)
        unregisterReceiver(updateReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (delayMillis != -1L) {
            handler.removeCallbacks(showNotificationRunnable)
            handler.post(showNotificationRunnable)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Runnable to show notifications at specified intervals
    private val showNotificationRunnable = object : Runnable {
        override fun run() {
            if (isNotificationEnabled) {
                sendNotification("Hello World $i")
                i++
            }
            handler.postDelayed(this, delayMillis)
        }
    }

    // Update the timer option and manage the notification schedule
    private fun updateTimerOption(option: String) {
        delayMillis = timerOptionToMillis(option)
        isNotificationEnabled = delayMillis != -1L
        handler.removeCallbacks(showNotificationRunnable)

        if (delayMillis == -1L) {
            stopSelf()
        } else {
            handler.postDelayed(showNotificationRunnable, delayMillis)
        }
    }

    // Fetch the timer option from DataStore settings
    private suspend fun fetchTimerOptionFromSettings(): String {
        val key = stringPreferencesKey("timer_option_key")
        val timerOption = dataStore.data.map { preferences ->
            preferences[key] ?: "Deactivated"
        }.first()

        return timerOption
    }

    // Register the BroadcastReceiver for timer updates
    private fun registerUpdateReceiver() {
        ContextCompat.registerReceiver(
            this,
            updateReceiver,
            IntentFilter("com.example.jetpackcompose.UPDATE_TIMER"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    // Convert timer option string to milliseconds
    private fun timerOptionToMillis(option: String): Long {
        return when (option) {
            "10s" -> 10_000L
            "30s" -> 30_000L
            "60s" -> 60_000L
            "30 min" -> 30 * 60 * 1000L
            "60 min" -> 60 * 60 * 1000L
            else -> -1L
        }
    }

    // Initialize the timer settings from DataStore
    private fun initializeTimerFromSettings() {
        CoroutineScope(Dispatchers.IO).launch {
            val timerOption = fetchTimerOptionFromSettings()
            delayMillis = timerOptionToMillis(timerOption)
            isNotificationEnabled = delayMillis != -1L

            if (isNotificationEnabled) {
                handler.post(showNotificationRunnable)
            } else {
                stopSelf()
            }
        }
    }

    // Send a notification with the specified message
    private fun sendNotification(message: String) {
        if (ActivityCompat.checkSelfPermission(
                this@PopupService,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager = NotificationManagerCompat.from(this)
        val notification = getNotification(message)
        notificationManager.notify(1, notification)
    }

    // Create a notification with the specified content text
    private fun getNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "popup_service_channel")
            .setContentTitle("Popup Service")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    // Create a notification channel for the service
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "popup_service_channel",
                "Popup Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications from Popup Service"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    // Start the service in the foreground with an initial notification
    private fun startForegroundService() {
        val notification = getNotification("Hello World")
        startForeground(1, notification)
    }
}