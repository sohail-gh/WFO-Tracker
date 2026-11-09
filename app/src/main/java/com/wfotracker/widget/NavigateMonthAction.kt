package com.wfotracker.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import java.time.YearMonth

/**
 * ActionCallback handling month navigation (previous, next, or reset to today)
 * for a specific widget instance without affecting other widget instances.
 */
class NavigateMonthAction : ActionCallback {

    companion object {
        val DIRECTION_KEY = ActionParameters.Key<Int>("direction")
        val RESET_TO_TODAY_KEY = ActionParameters.Key<Boolean>("reset_to_today")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val direction = parameters[DIRECTION_KEY] ?: 0
        val resetToToday = parameters[RESET_TO_TODAY_KEY] ?: false

        updateAppWidgetState(context, glanceId) { prefs ->
            if (resetToToday) {
                prefs[WfoWidgetKeys.PREF_DISPLAYED_MONTH] = YearMonth.now().toString()
            } else if (direction != 0) {
                val currentStr = prefs[WfoWidgetKeys.PREF_DISPLAYED_MONTH]
                val current = currentStr?.let {
                    runCatching { YearMonth.parse(it) }.getOrNull()
                } ?: YearMonth.now()

                val newMonth = if (direction > 0) current.plusMonths(1) else current.minusMonths(1)
                prefs[WfoWidgetKeys.PREF_DISPLAYED_MONTH] = newMonth.toString()
            }
        }
        WfoGlanceWidget().update(context, glanceId)
    }
}
