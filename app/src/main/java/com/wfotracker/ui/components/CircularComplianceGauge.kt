package com.wfotracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wfotracker.domain.model.MonthlyWfoSummary

@Composable
fun CircularComplianceGauge(
    summary: MonthlyWfoSummary,
    modifier: Modifier = Modifier
) {
    val progress = (summary.progressPercentage / 100.0).toFloat().coerceIn(0f, 1f)

    val indicatorColor = when {
        summary.isTargetMet -> MaterialTheme.colorScheme.primary
        summary.isTargetOutOfReach -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }

    val statusText = when {
        summary.isTargetMet -> "Target Achieved"
        summary.isTargetOutOfReach -> "Target Out of Reach"
        summary.remainingOfficeDays == 0 -> "Completed"
        else -> "${summary.remainingOfficeDays} days needed (${summary.daysLeft} left)"
    }

    val statusContainerColor = when {
        summary.isTargetMet -> MaterialTheme.colorScheme.primaryContainer
        summary.isTargetOutOfReach -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val statusContentColor = when {
        summary.isTargetMet -> MaterialTheme.colorScheme.onPrimaryContainer
        summary.isTargetOutOfReach -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(190.dp)
        ) {
            // Track background circle
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(180.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 14.dp,
                trackColor = Color.Transparent,
                strokeCap = StrokeCap.Round
            )
            // Progress circle
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(180.dp),
                color = indicatorColor,
                strokeWidth = 14.dp,
                trackColor = Color.Transparent,
                strokeCap = StrokeCap.Round
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format(java.util.Locale.US, "%.0f%%", summary.progressPercentage),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${summary.attendedDays} / ${summary.requiredOfficeDays} days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = MaterialTheme.shapes.small,
            color = statusContainerColor,
            contentColor = statusContentColor,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

