package com.wfotracker.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * BroadcastReceiver powering the WFO Tracker Glance AppWidget.
 * Handles AppWidget lifecycle as well as best-effort date rollover without exact alarms.
 */
class WfoWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = WfoGlanceWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // Best-effort date rollover and system time change update
                WfoWidgetUpdater.updateAll(context)
            }
        }
    }
}
