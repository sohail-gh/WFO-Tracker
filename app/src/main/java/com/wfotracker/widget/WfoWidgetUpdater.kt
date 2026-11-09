package com.wfotracker.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Utility to trigger immediate updates to all active WFO Tracker home screen widgets.
 */
object WfoWidgetUpdater {

    /**
     * Updates all active WFO Glance widgets asynchronously.
     */
    fun updateAll(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
        scope.launch {
            updateAllSuspend(context)
        }
    }

    /**
     * Suspending call to update all active WFO Glance widgets.
     */
    suspend fun updateAllSuspend(context: Context) {
        runCatching {
            WfoGlanceWidget().updateAll(context)
        }
    }
}
