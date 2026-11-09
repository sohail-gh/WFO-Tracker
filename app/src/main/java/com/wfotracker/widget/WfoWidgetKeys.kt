package com.wfotracker.widget

import androidx.datastore.preferences.core.stringPreferencesKey

object WfoWidgetKeys {
    /**
     * Key storing the currently displayed month for a specific widget instance in "YYYY-MM" format.
     */
    val PREF_DISPLAYED_MONTH = stringPreferencesKey("widget_displayed_month")
}
