package com.wfotracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wfotracker.domain.model.WfoDayState
import com.wfotracker.ui.theme.HolidayPurpleContainerDark
import com.wfotracker.ui.theme.HolidayPurpleContainerLight
import com.wfotracker.ui.theme.HolidayPurpleDark
import com.wfotracker.ui.theme.HolidayPurpleLight
import com.wfotracker.ui.theme.LeaveAmberContainerDark
import com.wfotracker.ui.theme.LeaveAmberContainerLight
import com.wfotracker.ui.theme.LeaveAmberDark
import com.wfotracker.ui.theme.LeaveAmberLight
import com.wfotracker.ui.theme.OfficeBlueContainerDark
import com.wfotracker.ui.theme.OfficeBlueContainerLight
import com.wfotracker.ui.theme.OfficeBlueDark
import com.wfotracker.ui.theme.OfficeBlueLight
import com.wfotracker.ui.theme.WeekendContainerDark
import com.wfotracker.ui.theme.WeekendContainerLight
import com.wfotracker.ui.theme.WeekendGreyDark
import com.wfotracker.ui.theme.WeekendGreyLight
import java.time.LocalDate

@Composable
fun DayCell(
    date: LocalDate,
    state: WfoDayState,
    isToday: Boolean,
    overrideLabel: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val (backgroundColor, textColor, badgeColor) = when (state) {
        WfoDayState.OFFICE -> Triple(
            if (darkTheme) OfficeBlueContainerDark else OfficeBlueContainerLight,
            if (darkTheme) OfficeBlueDark else OfficeBlueLight,
            if (darkTheme) OfficeBlueDark else OfficeBlueLight
        )
        WfoDayState.HOLIDAY -> Triple(
            if (darkTheme) HolidayPurpleContainerDark else HolidayPurpleContainerLight,
            if (darkTheme) HolidayPurpleDark else HolidayPurpleLight,
            if (darkTheme) HolidayPurpleDark else HolidayPurpleLight
        )
        WfoDayState.LEAVE -> Triple(
            if (darkTheme) LeaveAmberContainerDark else LeaveAmberContainerLight,
            if (darkTheme) LeaveAmberDark else LeaveAmberLight,
            if (darkTheme) LeaveAmberDark else LeaveAmberLight
        )
        WfoDayState.WEEKEND -> Triple(
            if (darkTheme) WeekendContainerDark else WeekendContainerLight,
            if (darkTheme) WeekendGreyDark else WeekendGreyLight,
            Color.Transparent
        )
        WfoDayState.WORKING_DAY -> Triple(
            Color.Transparent,
            MaterialTheme.colorScheme.onSurface,
            Color.Transparent
        )
    }

    val stateDescription = when (state) {
        WfoDayState.OFFICE -> "Office attendance"
        WfoDayState.HOLIDAY -> "Holiday${overrideLabel?.let { ": $it" } ?: ""}"
        WfoDayState.LEAVE -> "Leave${overrideLabel?.let { ": $it" } ?: ""}"
        WfoDayState.WEEKEND -> "Weekend"
        WfoDayState.WORKING_DAY -> "Working day"
    }

    val todayBorder = if (isToday) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else null

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (todayBorder != null) Modifier.border(todayBorder, RoundedCornerShape(8.dp))
                else Modifier
            )
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${date.dayOfMonth} ${date.month.name}, $stateDescription"
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = if (isToday || state == WfoDayState.OFFICE) FontWeight.Bold else FontWeight.Normal
                ),
                color = textColor,
                textAlign = TextAlign.Center
            )

            // State indicator icon/badge
            when (state) {
                WfoDayState.OFFICE -> {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                WfoDayState.HOLIDAY -> {
                    Icon(
                        imageVector = Icons.Default.Celebration,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                WfoDayState.LEAVE -> {
                    Icon(
                        imageVector = Icons.Default.BeachAccess,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                WfoDayState.WEEKEND, WfoDayState.WORKING_DAY -> {
                    // Clean, minimal dot or empty space
                    if (isToday) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            if (!overrideLabel.isNullOrBlank()) {
                Text(
                    text = overrideLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
