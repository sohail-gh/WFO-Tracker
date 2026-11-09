package com.wfotracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wfotracker.domain.model.WfoDayState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOverrideDialog(
    initialDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onSaveSingle: (date: LocalDate, state: WfoDayState, label: String?) -> Unit,
    onSaveRange: (startDate: LocalDate, endDate: LocalDate, state: WfoDayState, label: String?) -> Unit
) {
    var isRange by remember { mutableStateOf(false) }
    var selectedState by remember { mutableStateOf(WfoDayState.HOLIDAY) }
    var singleDate by remember { mutableStateOf(initialDate) }
    var startDate by remember { mutableStateOf(initialDate) }
    var endDate by remember { mutableStateOf(initialDate.plusDays(1)) }
    var labelText by remember { mutableStateOf("") }

    var showSinglePicker by remember { mutableStateOf(false) }
    var showRangePicker by remember { mutableStateOf(false) }

    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Holiday or Leave")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type selector: Holiday vs Leave
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedState == WfoDayState.HOLIDAY,
                        onClick = { selectedState = WfoDayState.HOLIDAY },
                        label = { Text("Holiday") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedState == WfoDayState.LEAVE,
                        onClick = { selectedState = WfoDayState.LEAVE },
                        label = { Text("Leave") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Range toggle: Single Date vs Date Range
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isRange,
                        onClick = { isRange = false },
                        label = { Text("Single Day") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isRange,
                        onClick = { isRange = true },
                        label = { Text("Date Range") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Date selection button
                if (!isRange) {
                    OutlinedButton(
                        onClick = { showSinglePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Date: ${singleDate.format(formatter)}")
                    }
                } else {
                    OutlinedButton(
                        onClick = { showRangePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Range: ${startDate.format(formatter)} – ${endDate.format(formatter)}")
                    }
                }

                // Label / Reason input
                OutlinedTextField(
                    value = labelText,
                    onValueChange = { labelText = it },
                    label = {
                        Text(if (selectedState == WfoDayState.HOLIDAY) "Holiday Name (Optional)" else "Leave Reason (Optional)")
                    },
                    placeholder = {
                        Text(if (selectedState == WfoDayState.HOLIDAY) "e.g. New Year's Day" else "e.g. Annual Vacation")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Note: Marking a holiday or leave strictly clears any office attendance on those dates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalLabel = labelText.trim().ifEmpty { null }
                    if (!isRange) {
                        onSaveSingle(singleDate, selectedState, finalLabel)
                    } else {
                        onSaveRange(startDate, endDate, selectedState, finalLabel)
                    }
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showSinglePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = singleDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showSinglePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            singleDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showSinglePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSinglePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            initialSelectedEndDateMillis = endDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startMillis = dateRangePickerState.selectedStartDateMillis
                        val endMillis = dateRangePickerState.selectedEndDateMillis
                        if (startMillis != null && endMillis != null) {
                            startDate = Instant.ofEpochMilli(startMillis).atZone(ZoneOffset.UTC).toLocalDate()
                            endDate = Instant.ofEpochMilli(endMillis).atZone(ZoneOffset.UTC).toLocalDate()
                        } else if (startMillis != null) {
                            startDate = Instant.ofEpochMilli(startMillis).atZone(ZoneOffset.UTC).toLocalDate()
                            endDate = startDate
                        }
                        showRangePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRangePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(state = dateRangePickerState)
        }
    }
}
