package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.ArushiApplication
import com.example.MainActivity
import com.example.R
import com.example.audio.ArushiAudioPlayer
import com.example.audio.SpeechRecognizerHelper
import com.example.data.gemini.GeminiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArushiBackgroundService : Service() {

    companion object {
        private const val TAG = "ArushiBgService"
        private const val NOTIFICATION_ID = 2001
        
        const val ACTION_START = "com.example.action.START_SERVICE"
        const val ACTION_STOP = "com.example.action.STOP_SERVICE"
        const val ACTION_TRIGGER_LISTEN = "com.example.action.TRIGGER_LISTEN"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning = _isServiceRunning.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, ArushiBackgroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ArushiBackgroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var geminiService: GeminiService
    private lateinit var audioPlayer: ArushiAudioPlayer
    private var speechRecognizerHelper: SpeechRecognizerHelper? = null

    override fun onCreate() {
        super.onCreate()
        geminiService = GeminiService(applicationContext)
        audioPlayer = ArushiAudioPlayer(applicationContext)

        speechRecognizerHelper = SpeechRecognizerHelper(
            context = applicationContext,
            scope = serviceScope,
            onSpeechResult = { text ->
                handleVoiceCommand(text)
            },
            onSpeechPartial = { _ -> },
            onRmsChangedCallback = { _ -> },
            onErrorCallback = { error ->
                Log.w(TAG, "Background voice recognition error: $error")
                updateNotification("Arushi is idle in background")
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                _isServiceRunning.value = false
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_LISTEN -> {
                startBackgroundListening()
            }
            else -> {
                startInForeground()
            }
        }
        return START_STICKY
    }

    private fun startInForeground() {
        _isServiceRunning.value = true
        val notification = createNotification("Arushi is active in background • Ready to assist")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startBackgroundListening() {
        audioPlayer.stopPlayback()
        updateNotification("Listening for your command...")
        speechRecognizerHelper?.startListening()
    }

    private fun handleVoiceCommand(prompt: String) {
        updateNotification("Processing: \"$prompt\"")
        serviceScope.launch {
            val result = geminiService.processUserTurn(prompt, emptyList())
            updateNotification("Arushi: ${result.replyText.take(50)}...")
            
            if (!result.audioBase64.isNullOrBlank()) {
                audioPlayer.playAudioBase64(result.audioBase64) {
                    updateNotification("Arushi is active in background • Ready to assist")
                }
            } else {
                updateNotification("Arushi is active in background • Ready to assist")
            }
        }
    }

    private fun updateNotification(status: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(status))
    }

    private fun createNotification(statusText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val listenIntent = Intent(this, ArushiBackgroundService::class.java).apply {
            action = ACTION_TRIGGER_LISTEN
        }
        val listenPendingIntent = PendingIntent.getService(
            this, 1, listenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ArushiBackgroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ArushiApplication.CHANNEL_ID)
            .setContentTitle("Arushi AI Voice Assistant")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_btn_speak_now, "Tap to Talk", listenPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizerHelper?.stopListening()
        audioPlayer.stopPlayback()
        serviceScope.cancel()
        _isServiceRunning.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
