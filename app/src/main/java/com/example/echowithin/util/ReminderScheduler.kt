package com.example.echowithin.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.echowithin.data.local.NoteDatabaseHelper
import java.time.Instant

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"

    const val ACTION_TRIGGER_REMINDER = "com.example.echowithin.ACTION_TRIGGER_REMINDER"
    const val ACTION_SNOOZE_REMINDER = "com.example.echowithin.ACTION_SNOOZE_REMINDER"
    const val ACTION_DISMISS_REMINDER = "com.example.echowithin.ACTION_DISMISS_REMINDER"

    const val EXTRA_NOTE_ID = "extra_note_id"
    const val EXTRA_NOTE_TITLE = "extra_note_title"
    const val EXTRA_NOTE_SNIPPET = "extra_note_snippet"

    /**
     * Schedules an exact alarm for a note reminder.
     * Uses setExactAndAllowWhileIdle so the alarm fires even when the device is in Doze mode.
     */
    fun schedule(
        context: Context,
        noteId: String,
        title: String,
        contentSnippet: String,
        triggerAtMillis: Long
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) {
            Log.w(TAG, "Cannot schedule reminder in the past for note $noteId")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_NOTE_ID, noteId)
            putExtra(EXTRA_NOTE_TITLE, title)
            putExtra(EXTRA_NOTE_SNIPPET, contentSnippet)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getPendingIntentId(noteId),
            intent,
            flags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled reminder for note $noteId at $triggerAtMillis")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling exact alarm: ${e.message}")
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (ex: Exception) {
                Log.e(TAG, "Failed fallback alarm scheduling: ${ex.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule reminder: ${e.message}")
        }
    }

    /**
     * Cancels an existing reminder for a note.
     */
    fun cancel(context: Context, noteId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
        }
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getPendingIntentId(noteId),
            intent,
            flags
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled reminder alarm for note $noteId")
        }
    }

    /**
     * Re-registers all future reminders saved in the local database.
     * Called on device boot or after app update.
     */
    fun rescheduleAllActiveReminders(context: Context) {
        val dbHelper = NoteDatabaseHelper(context)
        val activeNotes = dbHelper.getNotesWithActiveReminders()
        val now = System.currentTimeMillis()

        for (note in activeNotes) {
            val reminderIso = note.reminderAt ?: continue
            try {
                val triggerEpochMs = Instant.parse(reminderIso).toEpochMilli()
                if (triggerEpochMs > now) {
                    val snippet = note.content.lineSequence().firstOrNull()?.take(80) ?: ""
                    schedule(
                        context = context,
                        noteId = note.id,
                        title = note.title.ifBlank { "Task Reminder" },
                        contentSnippet = snippet,
                        triggerAtMillis = triggerEpochMs
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing reminder timestamp for note ${note.id}: ${e.message}")
            }
        }
    }

    fun getPendingIntentId(noteId: String): Int {
        return noteId.hashCode() and 0x7FFFFFFF
    }
}
