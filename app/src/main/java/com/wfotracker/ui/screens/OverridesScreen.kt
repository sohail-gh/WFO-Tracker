package com.wfotracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wfotracker.domain.model.DateOverride
import com.wfotracker.domain.model.WfoDayState
import com.wfotracker.ui.WfoUiState
import com.wfotracker.ui.components.AddOverrideDialog
import com.wfotracker.ui.components.DayActionDialog
import com.wfotracker.ui.components.MonthSelectorHeader
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun OverridesScreen(
    uiState: WfoUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToTodayMonth: () -> Unit,
    onMarkHoliday: (LocalDate, String?) -> Unit,
    onMarkHolidayRange: (LocalDate, LocalDate, String?) -> Unit,
    onMarkLeave: (LocalDate, String?) -> Unit,
    onMarkLeaveRange: (LocalDate, LocalDate, String?) -> Unit,
    onDeleteOverride: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var filterThisMonthOnly by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingOverride by remember { mutableStateOf<DateOverride?>(null) }
    var deleteConfirmDate by remember { mutableStateOf<LocalDate?>(null) }

    val overridesList = remember(uiState.attendanceState.overrides, filterThisMonthOnly, uiState.selectedMonth) {
        val all = uiState.attendanceState.overrides.values.toList().sortedBy { it.date }
        if (filterThisMonthOnly) {
            all.filter { YearMonth.from(it.date) == uiState.selectedMonth }
        } else {
            all
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Holiday or Leave")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            MonthSelectorHeader(
                selectedMonth = uiState.selectedMonth,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                onGoToToday = onGoToTodayMonth
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterThisMonthOnly,
                    onClick = { filterThisMonthOnly = true },
                    label = { Text("This Month (${overridesList.size})") }
                )
                FilterChip(
                    selected = !filterThisMonthOnly,
                    onClick = { filterThisMonthOnly = false },
                    label = { Text("All Overrides (${uiState.attendanceState.overrides.size})") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (overridesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (filterThisMonthOnly) "No holidays or leaves recorded for this month."
                            else "No holidays or leaves recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the + button to add one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(overridesList, key = { it.date.toString() }) { override ->
                        OverrideCard(
                            override = override,
                            onEdit = { editingOverride = override },
                            onDelete = { deleteConfirmDate = override.date }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddOverrideDialog(
            initialDate = uiState.selectedMonth.atDay(1),
            onDismiss = { showAddDialog = false },
            onSaveSingle = { date, state, label ->
                if (state == WfoDayState.HOLIDAY) {
                    onMarkHoliday(date, label)
                } else {
                    onMarkLeave(date, label)
                }
            },
            onSaveRange = { start, end, state, label ->
                if (state == WfoDayState.HOLIDAY) {
                    onMarkHolidayRange(start, end, label)
                } else {
                    onMarkLeaveRange(start, end, label)
                }
            }
        )
    }

    editingOverride?.let { override ->
        DayActionDialog(
            date = override.date,
            currentState = override.state,
            currentOverride = override,
            onDismiss = { editingOverride = null },
            onToggleOffice = { /* Overrides can be cleared or re-marked */ },
            onMarkHoliday = { name -> onMarkHoliday(override.date, name) },
            onMarkLeave = { reason -> onMarkLeave(override.date, reason) },
            onClearOverride = { onDeleteOverride(override.date) }
        )
    }

    deleteConfirmDate?.let { date ->
        val override = uiState.attendanceState.overrides[date]
        AlertDialog(
            onDismissRequest = { deleteConfirmDate = null },
            title = { Text("Delete Override") },
            text = {
                Text(
                    "Are you sure you want to remove the ${override?.state?.name?.lowercase() ?: "override"} on $date? It will be restored to a normal Working Day."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteOverride(date)
                        deleteConfirmDate = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmDate = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun OverrideCard(
    override: DateOverride,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)
    val isHoliday = override.state == WfoDayState.HOLIDAY

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isHoliday) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.secondaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isHoliday) Icons.Default.Celebration else Icons.Default.BeachAccess,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isHoliday) MaterialTheme.colorScheme.onTertiaryContainer
                        else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Column {
                    Text(
                        text = override.date.format(formatter),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (!override.name.isNullOrBlank()) override.name
                        else if (isHoliday) "Public Holiday" else "Personal Leave",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Override",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Override",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

