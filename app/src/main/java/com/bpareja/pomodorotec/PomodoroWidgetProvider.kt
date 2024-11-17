package com.bpareja.pomodorotec

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class PomodoroWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Actualizar cada widget
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_pomodoro)

            // Obtener el tiempo y fase desde SharedPreferences (o ViewModel)
            val prefs = context.getSharedPreferences("pomodoro_prefs", Context.MODE_PRIVATE)
            val phase = prefs.getString("phase", "Concentración")
            val timeLeft = prefs.getString("timeLeft", "25:00")
            val progress = prefs.getInt("progress", 0)

            // Actualizar el diseño
            views.setTextViewText(R.id.widget_phase, "Fase: $phase")
            views.setTextViewText(R.id.widget_time_left, timeLeft)
            views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)

            // Configurar un Intent para abrir la app al hacer clic
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_progress_bar, pendingIntent)

            // Actualizar el widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
