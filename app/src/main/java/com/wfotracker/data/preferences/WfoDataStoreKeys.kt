package com.wfotracker.data.preferences

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/**
 * Storage keys for Preferences DataStore.
 */
object WfoDataStoreKeys {
    const val DATASTORE_NAME = "wfo_tracker_preferences"

    val KEY_TARGET_PERCENT = intPreferencesKey("target_percent")
    val KEY_GLASS_OPACITY = intPreferencesKey("glass_opacity")
    val KEY_OFFICE_DATES = stringSetPreferencesKey("office_dates")
    val KEY_OVERRIDES_JSON = stringPreferencesKey("overrides_json")
}

