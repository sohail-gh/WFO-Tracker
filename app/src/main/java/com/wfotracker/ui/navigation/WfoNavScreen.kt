package com.wfotracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class WfoNavScreen(
    val title: String,
    val icon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    CALENDAR("Calendar", Icons.Default.CalendarMonth),
    OVERRIDES("Holidays & Leave", Icons.Default.EventBusy),
    SETTINGS("Settings", Icons.Default.Settings)
}

