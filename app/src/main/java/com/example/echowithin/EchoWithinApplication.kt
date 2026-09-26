package com.example.echowithin

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.example.echowithin.data.network.SessionManager

class EchoWithinApplication : Application() {
    companion object {
        lateinit var instance: EchoWithinApplication
            private set
        const val CHANNEL_ID = "echowithin_notifications"
        const val CHANNEL_DEFAULT_ID = "default"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        SessionManager.init(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val mainChannel = NotificationChannel(
                CHANNEL_ID,
                "EchoWithin Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for notes, interactions, and updates"
                enableLights(true)
                enableVibration(true)
                setSound(defaultSoundUri, audioAttributes)
            }

            val defaultChannel = NotificationChannel(
                CHANNEL_DEFAULT_ID,
                "General Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "General and system alerts"
                enableLights(true)
                enableVibration(true)
                setSound(defaultSoundUri, audioAttributes)
            }

            notificationManager.createNotificationChannels(listOf(mainChannel, defaultChannel))
        }
    }
}

