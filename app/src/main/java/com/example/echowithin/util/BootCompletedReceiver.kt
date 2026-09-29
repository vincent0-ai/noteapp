package com.example.echowithin.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received broadcast action: $action")
        if (
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            try {
                ReminderScheduler.rescheduleAllActiveReminders(context)
                Log.i(TAG, "Successfully restored active note reminders after reboot")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore reminders after reboot: ${e.message}")
            }
        }
    }
}
