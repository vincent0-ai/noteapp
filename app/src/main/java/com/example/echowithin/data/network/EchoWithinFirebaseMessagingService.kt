package com.example.echowithin.data.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.echowithin.EchoWithinApplication
import com.example.echowithin.MainActivity
import com.example.echowithin.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class EchoWithinFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed FCM token: $token")
        // Automatically register new token with backend if user has a session
        ApiClient.registerFcmToken(applicationContext)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "From: ${remoteMessage.from}")

        // Extract message notification payload or data payload fallback
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "EchoWithin"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: remoteMessage.data["text"]
            ?: "New notification"

        sendNotification(title, body, remoteMessage.data)
    }

    private fun sendNotification(title: String, messageBody: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            // Forward extra payload data
            for ((key, value) in data) {
                putExtra(key, value)
            }
        }

        val requestCode = (System.currentTimeMillis() % 10000).toInt()
        val pendingIntent = PendingIntent.getActivity(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = EchoWithinApplication.CHANNEL_ID
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setColor(0xFF7A00.toInt()) // Brand Orange
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Safety fallback: ensure channel exists
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                channelId,
                "EchoWithin Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for notes, interactions, and updates"
                enableLights(true)
                enableVibration(true)
                setSound(defaultSoundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}

