package com.wfotracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wfotracker.domain.model.WfoDayState
import com.wfotracker.ui.WfoUiState
import com.wfotracker.ui.components.CircularComplianceGauge
import com.wfotracker.ui.components.DayActionDialog
import com.wfotracker.ui.components.MonthSelectorHeader
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    uiState: WfoUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToTodayMonth: () -> Unit,
    onToggleTodayOffice: () -> Unit,
    onMarkTodayHoliday: (String?) -> Unit,
    onMarkTodayLeave: (String?) -> Unit,
    onClearTodayOverride: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTodayActionDialog by remember { mutableStateOf(false) }

    val today = uiState.today
    val todayOverride = uiState.attendanceState.overrides[today]
    val isTodayWeekend = today.dayOfWeek == DayOfWeek.SATURDAY || today.dayOfWeek == DayOfWeek.SUNDAY
    val isTodayOffice = uiState.attendanceState.officeAttendanceDates.contains(today)

    val todayState = when {
        todayOverride != null -> todayOverride.state
        isTodayWeekend -> WfoDayState.WEEKEND
        isTodayOffice -> WfoDayState.OFFICE
        else -> WfoDayState.WORKING_DAY
    }

    val todayFormatted = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MonthSelectorHeader(
            selectedMonth = uiState.selectedMonth,
            onPrevious = onPreviousMonth,
            onNext = onNextMonth,
            onGoToToday = onGoToTodayMonth
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Circular Compliance Progress Gauge
        CircularComplianceGauge(summary = uiState.summary)

        Spacer(modifier = Modifier.height(16.dp))

        // Today Quick-Action Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = todayFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = when (todayState) {
                            WfoDayState.OFFICE -> MaterialTheme.colorScheme.primaryContainer
                            WfoDayState.HOLIDAY -> MaterialTheme.colorScheme.tertiaryContainer
                            WfoDayState.LEAVE -> MaterialTheme.colorScheme.secondaryContainer
                            WfoDayState.WEEKEND -> MaterialTheme.colorScheme.surfaceContainerHighest
                            WfoDayState.WORKING_DAY -> MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = todayState.name.replace('_', ' '),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = when (todayState) {
                                WfoDayState.OFFICE -> MaterialTheme.colorScheme.onPrimaryContainer
                                WfoDayState.HOLIDAY -> MaterialTheme.colorScheme.onTertiaryContainer
                                WfoDayState.LEAVE -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isTodayWeekend) {
                    Text(
                        text = "Weekend: Relax and recharge! Weekends are not counted toward office days.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onToggleTodayOffice,
                            modifier = Modifier.weight(1f),
                            colors = if (isTodayOffice) {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            } else {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            }
                        ) {
                            Icon(
                                imageVector = if (isTodayOffice) Icons.Default.CheckCircle else Icons.Default.Business,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp).padding(end = 4.dp)
                            )
                            Text(if (isTodayOffice) "Office Marked" else "Mark Office")
                        }

                        OutlinedButton(
                            onClick = { showTodayActionDialog = true }
                        ) {
                            Text("More")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Monthly Breakdown Metrics Grid
        Text(
            text = "Monthly Breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2
        ) {
            MetricCard(
                title = "Working Days",
                value = "${uiState.summary.workingDays}",
                subtitle = "Out of ${uiState.summary.totalCalendarDays} total days",
                icon = Icons.Default.CalendarToday,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Office Attended",
                value = "${uiState.summary.attendedDays}",
                subtitle = "Target: ${uiState.settings.targetPercentage}%",
                icon = Icons.Default.Business,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Required Days",
                value = "${uiState.summary.requiredOfficeDays}",
                subtitle = "Remaining: ${uiState.summary.remainingOfficeDays} days",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Days Left",
                value = "${uiState.summary.daysLeft}",
                subtitle = if (uiState.summary.isTargetOutOfReach) "Out of reach" else "Working days left",
                icon = Icons.Default.DateRange,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showTodayActionDialog) {
        DayActionDialog(
            date = today,
            currentState = todayState,
            currentOverride = todayOverride,
            onDismiss = { showTodayActionDialog = false },
            onToggleOffice = onToggleTodayOffice,
            onMarkHoliday = onMarkTodayHoliday,
            onMarkLeave = onMarkTodayLeave,
            onClearOverride = onClearTodayOverride
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
