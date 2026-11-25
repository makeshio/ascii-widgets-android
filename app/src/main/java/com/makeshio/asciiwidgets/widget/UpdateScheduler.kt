package com.makeshio.asciiwidgets.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

object UpdateScheduler {
    const val ACTION_TICK = "com.makeshio.asciiwidgets.TICK"
    private const val REQUEST_CODE = 1001

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(
            ClockWidgetProvider::class.java,
            WeatherWidgetProvider::class.java,
            BatteryWidgetProvider::class.java,
            MusicWidgetProvider::class.java,
        ).forEachIndexed { index, provider ->
            val intent = Intent(context, provider).setAction(ACTION_TICK)
            val pending = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 60_000L,
                60_000L,
                pending,
            )
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        UpdateScheduler.schedule(context)
    }
}
