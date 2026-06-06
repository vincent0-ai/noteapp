package com.example.echowithin.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.echowithin.ui.theme.ErrorRed

/**
 * Review dialog shown when the note owner accepts or rejects a
 * collaboration proposal. Mirrors the website's behavior:
 *   - Optional comment (max 180 chars, server-trimmed)
 *   - On accept: optional "auto-approve this user for future proposals"
 *     checkbox, which adds the editor to the share's
 *     auto_approved_users list server-side
 *
 * The submit callback is invoked once with whatever the user typed /
 * toggled, and the caller is responsible for routing to the right
 * ViewModel method (approve vs reject) — see the `isApprove` param
 * the caller passed in.
 */
@Composable
fun ProposalReviewDialog(
    isApprove: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (comment: String, autoApproveSubsequent: Boolean) -> Unit
) {
    var comment by remember { mutableStateOf("") }
    var autoApproveNext by remember { mutableStateOf(false) }
    val maxChars = 180

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isApprove) "Approve proposal" else "Decline proposal",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = comment,
                    onValueChange = { if (it.length <= maxChars) comment = it },
                    label = { Text("Comment (optional)") },
                    placeholder = { Text("e.g. \"Good catch — merged\"") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2,
                    maxLines = 4,
                    supportingText = { Text("${comment.length} / $maxChars") }
                )
                if (isApprove) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Auto-approve this user for future proposals",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Switch(
                            checked = autoApproveNext,
                            onCheckedChange = { autoApproveNext = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(comment, autoApproveNext) }
            ) {
                Text(
                    text = if (isApprove) "Approve" else "Decline",
                    color = if (isApprove) MaterialTheme.colorScheme.primary else ErrorRed,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
