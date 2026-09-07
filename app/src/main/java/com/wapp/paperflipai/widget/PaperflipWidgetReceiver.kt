package com.wapp.paperflipai.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Manifest entry point for [PaperflipWidget]. iOS gets this for free from
 * `PaperFlipWidgetBundle`; on Android the launcher talks to an
 * `AppWidgetProvider`, and Glance supplies the bridge.
 */
class PaperflipWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget get() = PaperflipWidget()
}
