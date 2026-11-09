package com.wfotracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wfotracker.domain.model.WfoDayState
import com.wfotracker.ui.WfoUiState
import com.wfotracker.ui.components.DayActionDialog
import com.wfotracker.ui.components.DayCell
import com.wfotracker.ui.components.MonthSelectorHeader
import com.wfotracker.ui.theme.HolidayPurpleLight
import com.wfotracker.ui.theme.LeaveAmberLight
import com.wfotracker.ui.theme.OfficeBlueLight
import com.wfotracker.ui.theme.WeekendGreyLight
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    uiState: WfoUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToTodayMonth: () -> Unit,
    onToggleOffice: (LocalDate) -> Unit,
    onMarkHoliday: (LocalDate, String?) -> Unit,
    onMarkLeave: (LocalDate, String?) -> Unit,
    onClearOverride: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDialogDate by remember { mutableStateOf<LocalDate?>(null) }

    val daysInMonth = remember(uiState.selectedMonth) {
        val totalDays = uiState.selectedMonth.lengthOfMonth()
        (1..totalDays).map { day ->
            uiState.selectedMonth.atDay(day)
        }
    }

    val leadingEmptyDays = remember(uiState.selectedMonth) {
        val firstDay = uiState.selectedMonth.atDay(1)
        // Monday = 1, Sunday = 7 -> leading empty slots before Monday
        firstDay.dayOfWeek.value - 1
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MonthSelectorHeader(
            selectedMonth = uiState.selectedMonth,
            onPrevious = onPreviousMonth,
            onNext = onNextMonth,
            onGoToToday = onGoToTodayMonth
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Weekday headers: Mon Tue Wed Thu Fri Sat Sun
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            weekDays.forEachIndexed { index, day ->
                val isWeekendCol = index >= 5
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = if (isWeekendCol) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Calendar Grid
        val totalCells = leadingEmptyDays + daysInMonth.size
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayIndex = cellIndex - leadingEmptyDays
                    if (dayIndex in daysInMonth.indices) {
                        val date = daysInMonth[dayIndex]
                        val override = uiState.attendanceState.overrides[date]
                        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
                        val isOffice = uiState.attendanceState.officeAttendanceDates.contains(date)

                        val dayState = when {
                            override != null -> override.state
                            isWeekend -> WfoDayState.WEEKEND
                            isOffice -> WfoDayState.OFFICE
                            else -> WfoDayState.WORKING_DAY
                        }

                        DayCell(
                            date = date,
                            state = dayState,
                            isToday = date == uiState.today,
                            overrideLabel = override?.name,
                            onClick = { selectedDialogDate = date },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Empty slot
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Monthly Summary Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Office Progress",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${uiState.summary.attendedDays} / ${uiState.summary.requiredOfficeDays} Days (${String.format(java.util.Locale.US, "%.0f%%", uiState.summary.progressPercentage)})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = when {
                        uiState.summary.isTargetMet -> MaterialTheme.colorScheme.primaryContainer
                        uiState.summary.isTargetOutOfReach -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = when {
                            uiState.summary.isTargetMet -> "Achieved"
                            uiState.summary.isTargetOutOfReach -> "Out of reach"
                            else -> "${uiState.summary.remainingOfficeDays} needed"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            uiState.summary.isTargetMet -> MaterialTheme.colorScheme.onPrimaryContainer
                            uiState.summary.isTargetOutOfReach -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LegendItem(color = OfficeBlueLight, label = "Office")
            LegendItem(color = HolidayPurpleLight, label = "Holiday")
            LegendItem(color = LeaveAmberLight, label = "Leave")
            LegendItem(color = WeekendGreyLight, label = "Weekend")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    selectedDialogDate?.let { date ->
        val override = uiState.attendanceState.overrides[date]
        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        val isOffice = uiState.attendanceState.officeAttendanceDates.contains(date)

        val dayState = when {
            override != null -> override.state
            isWeekend -> WfoDayState.WEEKEND
            isOffice -> WfoDayState.OFFICE
            else -> WfoDayState.WORKING_DAY
        }

        DayActionDialog(
            date = date,
            currentState = dayState,
            currentOverride = override,
            onDismiss = { selectedDialogDate = null },
            onToggleOffice = { onToggleOffice(date) },
            onMarkHoliday = { name -> onMarkHoliday(date, name) },
            onMarkLeave = { reason -> onMarkLeave(date, reason) },
            onClearOverride = { onClearOverride(date) }
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

