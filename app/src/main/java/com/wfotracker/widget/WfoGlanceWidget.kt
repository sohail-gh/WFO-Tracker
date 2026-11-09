package com.wfotracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.wfotracker.MainActivity
import com.wfotracker.WfoApplication
import com.wfotracker.domain.WfoCalculator
import com.wfotracker.domain.model.MonthlyWfoSummary
import com.wfotracker.domain.model.WfoAttendanceState
import com.wfotracker.domain.model.WfoDayState
import com.wfotracker.domain.model.WfoSettings
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class WfoGlanceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = WfoApplication.getRepository(context)
        val attendanceState = repository.attendanceState.first()
        val settings = repository.settings.first()
        val today = LocalDate.now()

        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                val displayedMonthStr = prefs[WfoWidgetKeys.PREF_DISPLAYED_MONTH]
                val displayedMonth = displayedMonthStr?.let {
                    runCatching { YearMonth.parse(it) }.getOrNull()
                } ?: YearMonth.from(today)

                val summary = WfoCalculator.calculateMonthlySummary(
                    month = displayedMonth,
                    attendanceState = attendanceState,
                    settings = settings,
                    today = today
                )

                WfoWidgetBody(
                    context = LocalContext.current,
                    displayedMonth = displayedMonth,
                    summary = summary,
                    attendanceState = attendanceState,
                    settings = settings,
                    today = today
                )
            }
        }
    }

    @Composable
    private fun WfoWidgetBody(
        context: Context,
        displayedMonth: YearMonth,
        summary: MonthlyWfoSummary,
        attendanceState: WfoAttendanceState,
        settings: WfoSettings,
        today: LocalDate
    ) {
        val size = LocalSize.current
        val isCompactWidth = size.width < 260.dp
        val isCompactHeight = size.height < 210.dp
        val isVeryCompactHeight = size.height < 170.dp

        // Glass background with configurable opacity and subtle frosted border
        val opacityPercent = settings.glassOpacity.coerceIn(0, 100)
        val opacityAlpha = (opacityPercent / 100f * 255).toInt()
        val surfaceColor = Color(0x14, 0x17, 0x22, opacityAlpha)
        val borderAlpha = ((opacityPercent / 100f) * 0x30).toInt().coerceIn(0, 0x30)
        val borderColor = Color(0xFF, 0xFF, 0xFF, borderAlpha)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(ColorProvider(borderColor))
                .padding(1.dp)
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(15.dp)
                    .background(ColorProvider(surfaceColor))
                    .padding(if (isCompactHeight) 6.dp else 10.dp)
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Month Navigation Header
                    MonthNavHeader(
                        displayedMonth = displayedMonth,
                        isCompactWidth = isCompactWidth,
                        isCompactHeight = isCompactHeight
                    )

                    Spacer(modifier = GlanceModifier.height(if (isVeryCompactHeight) 2.dp else 4.dp))

                    // 2. Metrics & Out-of-Reach Warning
                    ComplianceMetricsBar(
                        summary = summary,
                        isCompactWidth = isCompactWidth,
                        isCompactHeight = isCompactHeight
                    )

                    Spacer(modifier = GlanceModifier.height(if (isVeryCompactHeight) 3.dp else 5.dp))

                    // 3. 7-Column Calendar Grid
                    CalendarGrid(
                        context = context,
                        displayedMonth = displayedMonth,
                        attendanceState = attendanceState,
                        today = today,
                        isCompactWidth = isCompactWidth,
                        isCompactHeight = isCompactHeight
                    )
                }
            }
        }
    }

    @Composable
    private fun MonthNavHeader(
        displayedMonth: YearMonth,
        isCompactWidth: Boolean,
        isCompactHeight: Boolean
    ) {
        val pattern = if (isCompactWidth) "MMM yyyy" else "MMMM yyyy"
        val monthFormatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        val monthTitle = displayedMonth.format(monthFormatter)

        val btnWidth = if (isCompactWidth) 26.dp else 32.dp
        val btnHeight = if (isCompactHeight) 24.dp else 28.dp

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous month button
            Box(
                modifier = GlanceModifier
                    .width(btnWidth)
                    .height(btnHeight)
                    .cornerRadius(6.dp)
                    .background(ColorProvider(Color(0x33FFFFFF)))
                    .clickable(
                        actionRunCallback<NavigateMonthAction>(
                            actionParametersOf(NavigateMonthAction.DIRECTION_KEY to -1)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "◀",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = if (isCompactHeight) 10.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
            }

            // Month title (tapping resets to current month)
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(
                        actionRunCallback<NavigateMonthAction>(
                            actionParametersOf(NavigateMonthAction.RESET_TO_TODAY_KEY to true)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = monthTitle,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = if (isCompactHeight) 12.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
            }

            // Next month button
            Box(
                modifier = GlanceModifier
                    .width(btnWidth)
                    .height(btnHeight)
                    .cornerRadius(6.dp)
                    .background(ColorProvider(Color(0x33FFFFFF)))
                    .clickable(
                        actionRunCallback<NavigateMonthAction>(
                            actionParametersOf(NavigateMonthAction.DIRECTION_KEY to 1)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = if (isCompactHeight) 10.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }

    @Composable
    private fun ComplianceMetricsBar(
        summary: MonthlyWfoSummary,
        isCompactWidth: Boolean,
        isCompactHeight: Boolean
    ) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attended / Required days
                Text(
                    text = "${summary.attendedDays}/${summary.requiredOfficeDays} Days (${String.format(Locale.US, "%.0f%%", summary.progressPercentage)})",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFE0E0E0)),
                        fontSize = if (isCompactHeight) 9.sp else 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                // Status or Out-of-reach warning
                val statusText = when {
                    summary.isTargetMet -> "Achieved 🎉"
                    summary.isTargetOutOfReach -> if (isCompactWidth) "⚠️ Out of reach" else "⚠️ Target Out of Reach"
                    else -> "${summary.remainingOfficeDays} needed"
                }

                val statusColor = when {
                    summary.isTargetMet -> Color(0xFF81C784)
                    summary.isTargetOutOfReach -> Color(0xFFEF5350)
                    else -> Color(0xFFFFB74D)
                }

                Text(
                    text = statusText,
                    style = TextStyle(
                        color = ColorProvider(statusColor),
                        fontSize = if (isCompactHeight) 9.sp else 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(2.dp))

            // Thin progress indicator bar
            val progressFraction = (summary.progressPercentage / 100.0).coerceIn(0.0, 1.0).toFloat()
            val barColor = when {
                summary.isTargetMet -> Color(0xFF4CAF50)
                summary.isTargetOutOfReach -> Color(0xFFE53935)
                else -> Color(0xFF42A5F5)
            }

            LinearProgressIndicator(
                progress = progressFraction,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(if (isCompactHeight) 2.dp else 3.dp)
                    .cornerRadius(2.dp),
                color = ColorProvider(barColor),
                backgroundColor = ColorProvider(Color(0x33FFFFFF))
            )
        }
    }

    @Composable
    private fun ColumnScope.CalendarGrid(
        context: Context,
        displayedMonth: YearMonth,
        attendanceState: WfoAttendanceState,
        today: LocalDate,
        isCompactWidth: Boolean,
        isCompactHeight: Boolean
    ) {
        val weekHeaders = if (isCompactWidth) {
            listOf("M", "T", "W", "T", "F", "S", "S")
        } else {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        }

        Column(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            // Weekday Headers
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                weekHeaders.forEachIndexed { index, header ->
                    val isWeekendCol = index >= 5
                    Box(
                        modifier = GlanceModifier.defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = header,
                            style = TextStyle(
                                color = ColorProvider(if (isWeekendCol) Color(0xFF757575) else Color(0xFFB0BEC5)),
                                fontSize = if (isCompactHeight) 8.sp else 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(2.dp))

            // Days calculation
            val firstDayOfMonth = displayedMonth.atDay(1)
            val leadingEmptyDays = firstDayOfMonth.dayOfWeek.value - 1
            val daysInMonth = displayedMonth.lengthOfMonth()
            val totalCells = leadingEmptyDays + daysInMonth
            val totalRows = (totalCells + 6) / 7

            for (row in 0 until totalRows) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - leadingEmptyDays + 1

                        if (dayNumber in 1..daysInMonth) {
                            val date = displayedMonth.atDay(dayNumber)
                            val isToday = date == today
                            val override = attendanceState.overrides[date]
                            val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
                            val isOffice = attendanceState.officeAttendanceDates.contains(date)

                            val dayState = when {
                                override != null -> override.state
                                isWeekend -> WfoDayState.WEEKEND
                                isOffice -> WfoDayState.OFFICE
                                else -> WfoDayState.WORKING_DAY
                            }

                            // Day Cell Colors
                            // Office: Green, Holiday: Purple, Leave: Amber, Weekend: Dimmed, Today: Blue outline
                            val (cellBgColor, textColor) = when (dayState) {
                                WfoDayState.OFFICE -> Color(0xFF2E7D32) to Color.White
                                WfoDayState.HOLIDAY -> Color(0xFF6A1B9A) to Color.White
                                WfoDayState.LEAVE -> Color(0xFFE65100) to Color.White
                                WfoDayState.WEEKEND -> Color(0x1AFFFFFF) to Color(0xFF757575)
                                WfoDayState.WORKING_DAY -> Color(0x12FFFFFF) to Color(0xFFECEFF1)
                            }

                            val openDateIntent = Intent(context, MainActivity::class.java).apply {
                                action = Intent.ACTION_VIEW
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra(MainActivity.EXTRA_SELECTED_DATE, date.toString())
                            }

                            // If today: blue outline border around cell
                            Box(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .fillMaxHeight()
                                    .padding(if (isCompactHeight) 0.5.dp else 1.dp)
                                    .cornerRadius(4.dp)
                                    .then(
                                        if (isToday) GlanceModifier.background(ColorProvider(Color(0xFF1976D2)))
                                        else GlanceModifier
                                    )
                                    .clickable(actionStartActivity(openDateIntent)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = GlanceModifier
                                        .fillMaxSize()
                                        .padding(if (isToday) 1.dp else 0.dp)
                                        .cornerRadius(3.dp)
                                        .background(ColorProvider(cellBgColor)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        style = TextStyle(
                                            color = ColorProvider(textColor),
                                            fontSize = if (isCompactHeight) 9.sp else 11.sp,
                                            fontWeight = if (isToday || dayState == WfoDayState.OFFICE) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }
                            }
                        } else {
                            // Empty slot before month start or after month end
                            Box(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .fillMaxHeight()
                            ) {}
                        }
                    }
                }
            }
        }
    }
}
