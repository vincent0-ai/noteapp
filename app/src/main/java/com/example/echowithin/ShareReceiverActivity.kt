package com.example.echowithin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.echowithin.data.local.NoteDatabaseHelper
import com.example.echowithin.data.model.AppNote
import com.example.echowithin.ui.theme.EchoWithinTheme
import com.example.echowithin.ui.theme.BrandOrange

/**
 * Handles incoming share intents (ACTION_SEND) from other apps.
 * Creates a new note from the shared text and finishes.
 */
class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = extractSharedText(intent)

        setContent {
            EchoWithinTheme {
                ShareReceiverScreen(
                    sharedText = sharedText,
                    onSave = { content ->
                        saveAsNote(content)
                        finish()
                    },
                    onDiscard = { finish() }
                )
            }
        }
    }

    private fun extractSharedText(intent: Intent?): String {
        if (intent?.action != Intent.ACTION_SEND) return ""
        return when {
            intent.type?.startsWith("text/") == true -> {
                intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            }
            else -> ""
        }
    }

    private fun saveAsNote(content: String) {
        if (content.isBlank()) return
        val db = NoteDatabaseHelper(this)
        val titleLine = content.lineSequence().firstOrNull()?.trim()?.take(60) ?: "Shared Note"
        val noteId = "local_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}"
        val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date())

        val note = AppNote(
            id = noteId,
            title = titleLine,
            content = content,
            reference = "",
            tags = emptyList(),
            updatedAt = now,
            isLocked = false,
            isPinned = false,
            isSynced = false,
            pendingOp = "create"
        )
        db.saveNote(note, isSynced = false, pendingOp = "create")
    }
}

@Composable
private fun ShareReceiverScreen(
    sharedText: String,
    onSave: (String) -> Unit,
    onDiscard: () -> Unit
) {
    var content by remember { mutableStateOf(sharedText) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Save to Echo Within",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BrandOrange
            )

            Text(
                text = "Review the shared content below, then save it as a new note.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("Note Content") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandOrange,
                    cursorColor = BrandOrange
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Discard")
                }
                Button(
                    onClick = { onSave(content) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                    enabled = content.isNotBlank()
                ) {
                    Text("Save Note")
                }
            }
        }
    }
}
