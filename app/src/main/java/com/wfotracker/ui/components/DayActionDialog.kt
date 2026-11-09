package com.wfotracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wfotracker.domain.model.DateOverride
import com.wfotracker.domain.model.WfoDayState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DayActionDialog(
    date: LocalDate,
    currentState: WfoDayState,
    currentOverride: DateOverride?,
    onDismiss: () -> Unit,
    onToggleOffice: () -> Unit,
    onMarkHoliday: (name: String?) -> Unit,
    onMarkLeave: (reason: String?) -> Unit,
    onClearOverride: () -> Unit
) {
    var dialogMode by remember { mutableStateOf<DialogMode>(DialogMode.Default) }
    var inputLabel by remember(currentOverride) { mutableStateOf(currentOverride?.name ?: "") }

    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US)
    val formattedDate = date.format(formatter)

    when (dialogMode) {
        DialogMode.Default -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Column {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Status: ${currentState.name.replace('_', ' ')}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (currentState) {
                            WfoDayState.WEEKEND -> {
                                Text(
                                    text = "This date is on a weekend. Weekends are automatically counted as non-working days.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            WfoDayState.OFFICE -> {
                                Button(
                                    onClick = {
                                        onToggleOffice()
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text("Unmark Office Attendance")
                                }
                                OutlinedButton(
                                    onClick = { dialogMode = DialogMode.MarkHoliday },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text("Mark as Holiday")
                                }
                                OutlinedButton(
                                    onClick = { dialogMode = DialogMode.MarkLeave },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.BeachAccess, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text("Mark as Leave")
                                }
                            }
                            WfoDayState.WORKING_DAY -> {
                                Button(
                                    onClick = {
                                        onToggleOffice()
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text("Mark as Attended Office")
                                }
                                OutlinedButton(
                                    onClick = { dialogMode = DialogMode.MarkHoliday },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text("Mark as Holiday")
                                }
                                OutlinedButton(
                                    onClick = { dialogMode = DialogMode.MarkLeave },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.BeachAccess, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text("Mark as Leave")
                                }
                            }
                            WfoDayState.HOLIDAY -> {
                                if (!currentOverride?.name.isNullOrBlank()) {
                                    Text("Holiday: ${currentOverride?.name}", fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = { dialogMode = DialogMode.MarkHoliday },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text("Edit Holiday Name")
                                }
                                OutlinedButton(
                                    onClick = {
                                        onClearOverride()
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text("Remove Holiday (Restore Working Day)")
                                }
                            }
                            WfoDayState.LEAVE -> {
                                if (!currentOverride?.name.isNullOrBlank()) {
                                    Text("Leave Reason: ${currentOverride?.name}", fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = { dialogMode = DialogMode.MarkLeave },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text("Edit Leave Reason")
                                }
                                OutlinedButton(
                                    onClick = {
                                        onClearOverride()
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text("Remove Leave (Restore Working Day)")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            )
        }
        DialogMode.MarkHoliday -> {
            AlertDialog(
                onDismissRequest = { dialogMode = DialogMode.Default },
                title = { Text("Mark Holiday for $date") },
                text = {
                    Column {
                        Text("Marking as Holiday strictly clears any office attendance on this date.")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = inputLabel,
                            onValueChange = { inputLabel = it },
                            label = { Text("Holiday Name (Optional)") },
                            placeholder = { Text("e.g. Diwali, Christmas") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        onMarkHoliday(inputLabel.ifBlank { null })
                        onDismiss()
                    }) {
                        Text("Save Holiday")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialogMode = DialogMode.Default }) {
                        Text("Back")
                    }
                }
            )
        }
        DialogMode.MarkLeave -> {
            AlertDialog(
                onDismissRequest = { dialogMode = DialogMode.Default },
                title = { Text("Mark Leave for $date") },
                text = {
                    Column {
                        Text("Marking as Leave strictly clears any office attendance on this date.")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = inputLabel,
                            onValueChange = { inputLabel = it },
                            label = { Text("Leave Reason (Optional)") },
                            placeholder = { Text("e.g. Sick Leave, Vacation") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        onMarkLeave(inputLabel.ifBlank { null })
                        onDismiss()
                    }) {
                        Text("Save Leave")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialogMode = DialogMode.Default }) {
                        Text("Back")
                    }
                }
            )
        }
    }
}

private sealed interface DialogMode {
    data object Default : DialogMode
    data object MarkHoliday : DialogMode
    data object MarkLeave : DialogMode
}

