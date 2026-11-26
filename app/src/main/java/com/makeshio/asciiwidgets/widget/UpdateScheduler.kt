package com.makeshio.asciiwidgets.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings

object UpdateScheduler {
    const val ACTION_TICK = "com.makeshio.asciiwidgets.TICK"
    const val ACTION_ANIM_TICK = "com.makeshio.asciiwidgets.ANIM_TICK"
    private const val REQUEST_CODE = 1001
    private const val ANIM_REQUEST_CODE = 3001

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return manager.canScheduleExactAlarms()
    }

    fun requestExactAlarm(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        try {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            Unit
        }
    }

    /** Вызывается после каждого тика, чтобы точный будильник не «завис» на одном срабатывании. */
    fun onTick(context: Context) = schedule(context)

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exact = canScheduleExact(context)
        WidgetUpdater.allProviders.forEachIndexed { index, provider ->
            val intent = Intent(context, provider).setAction(ACTION_TICK)
            val pending = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            if (exact) {
                alarmManager.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 60_000L,
                    pending,
                )
            } else {
                alarmManager.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 60_000L,
                    60_000L,
                    pending,
                )
            }

            val animIntent = Intent(context, provider).setAction(ACTION_ANIM_TICK)
            val animPending = PendingIntent.getBroadcast(
                context,
                ANIM_REQUEST_CODE + index,
                animIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1_000L,
                1_000L,
                animPending,
            )
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        UpdateScheduler.schedule(context)
    }
}
