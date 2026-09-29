package com.example.echowithin.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.echowithin.EchoWithinApplication
import com.example.echowithin.MainActivity
import com.example.echowithin.R
import com.example.echowithin.data.local.NoteDatabaseHelper
import java.time.Instant

class ReminderBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReminderReceiver"
        const val EXTRA_OPEN_NOTE_ID = "open_note_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val noteId = intent.getStringExtra(ReminderScheduler.EXTRA_NOTE_ID) ?: return
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_NOTE_TITLE) ?: "Task Reminder"
        val snippet = intent.getStringExtra(ReminderScheduler.EXTRA_NOTE_SNIPPET) ?: ""
        val notificationId = ReminderScheduler.getPendingIntentId(noteId)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        when (action) {
            ReminderScheduler.ACTION_TRIGGER_REMINDER -> {
                showReminderNotification(context, notificationManager, noteId, title, snippet, notificationId)
            }

            ReminderScheduler.ACTION_SNOOZE_REMINDER -> {
                notificationManager.cancel(notificationId)
                val snoozeTimeMs = System.currentTimeMillis() + (15 * 60 * 1000L) // +15 mins
                ReminderScheduler.schedule(context, noteId, title, snippet, snoozeTimeMs)
                try {
                    val dbHelper = NoteDatabaseHelper(context)
                    dbHelper.updateReminder(noteId, Instant.ofEpochMilli(snoozeTimeMs).toString())
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update snooze time in DB: ${e.message}")
                }
            }

            ReminderScheduler.ACTION_DISMISS_REMINDER -> {
                notificationManager.cancel(notificationId)
                try {
                    val dbHelper = NoteDatabaseHelper(context)
                    dbHelper.updateReminder(noteId, null)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to clear reminder in DB: ${e.message}")
                }
            }
        }
    }

    private fun showReminderNotification(
        context: Context,
        notificationManager: NotificationManager,
        noteId: String,
        title: String,
        snippet: String,
        notificationId: Int
    ) {
        // Main tap intent -> Opens MainActivity and targets note
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_NOTE_ID, noteId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            flags
        )

        // Action: Snooze 15 minutes
        val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_SNOOZE_REMINDER
            putExtra(ReminderScheduler.EXTRA_NOTE_ID, noteId)
            putExtra(ReminderScheduler.EXTRA_NOTE_TITLE, title)
            putExtra(ReminderScheduler.EXTRA_NOTE_SNIPPET, snippet)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            snoozeIntent,
            flags
        )

        // Action: Mark Done / Clear Reminder
        val doneIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_DISMISS_REMINDER
            putExtra(ReminderScheduler.EXTRA_NOTE_ID, noteId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            doneIntent,
            flags
        )

        val channelId = EchoWithinApplication.CHANNEL_ID

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(snippet.ifBlank { "You have an active reminder for this note." })
            .setStyle(NotificationCompat.BigTextStyle().bigText(snippet.ifBlank { "You have an active reminder for this note." }))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_stat_notification, "Snooze 15m", snoozePendingIntent)
            .addAction(R.drawable.ic_stat_notification, "Mark Done", donePendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
