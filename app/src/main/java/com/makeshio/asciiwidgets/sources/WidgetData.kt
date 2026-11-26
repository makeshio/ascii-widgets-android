package com.makeshio.asciiwidgets.sources

import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationManager
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.wifi.WifiManager
import android.os.BatteryManager
import com.makeshio.asciiwidgets.data.EffectiveSettings
import com.makeshio.asciiwidgets.widget.MediaNotificationListener
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime

object WidgetData {
    fun clock(settings: EffectiveSettings): List<String> {
        val now = LocalDateTime.now()
        val hour = if (settings.use24Hour) now.hour else now.hour % 12
        val time = "%02d:%02d".format(hour, now.minute)
        val date = "%02d.%02d".format(now.dayOfMonth, now.monthValue)
        return listOf(time, date)
    }

    fun battery(context: Context): List<String> {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.takeIf { it > 0 } ?: 100
        val percent = if (level >= 0) level * 100 / scale else -1
        val charging = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ==
            BatteryManager.BATTERY_STATUS_CHARGING

        val main = if (percent >= 0) {
            (if (charging) "C" else "") + "$percent%"
        } else {
            "----"
        }

        val wifi = try {
            (context.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.isWifiEnabled == true
        } catch (_: Exception) {
            false
        }
        val bluetooth = try {
            BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
        } catch (_: Exception) {
            false
        }
        val status = buildString {
            if (wifi) append("W")
            if (bluetooth) append(" B")
        }.ifBlank { "-" }

        return listOf(main.take(4), status.take(4))
    }

    suspend fun weather(
        context: Context,
        settings: EffectiveSettings,
        hourly: Boolean = false,
    ): List<String> {
        val point = if (settings.autoLocation) detectLocation(context) else CITIES[settings.city.trim()]
        val latitude = point?.first ?: 55.7558
        val longitude = point?.second ?: 37.6173
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude&longitude=$longitude" +
            "&current=temperature_2m&daily=temperature_2m_max,temperature_2m_min" +
            (if (hourly) "&hourly=temperature_2m" else "") +
            "&timezone=auto&forecast_days=1"

        val json = try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return fallbackWeather
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            return fallbackWeather
        }

        val current = number(json, "temperature_2m")?.toInt()?.toString() ?: "----"
        val max = firstArrayNumber(json, "temperature_2m_max")?.toInt()?.toString() ?: "--"
        val min = firstArrayNumber(json, "temperature_2m_min")?.toInt()?.toString() ?: "--"
        val unit = if (settings.tempUnit == "F") "F" else "°C"
        val firstLine = "$current$unit".take(4)

        if (!hourly) {
            return listOf(firstLine, "$max/$min".take(4))
        }
        val secondLine = formatHourlyTemps(nextHourlyTemps(json))
        return listOf(firstLine, secondLine.ifBlank { "----" })
    }

    /** Чистая функция для тестов и превью: 19, 20, 21 → "19°20°21°". */
    fun formatHourlyTemps(values: List<Int>): String =
        values.take(3).joinToString("") { "%02d°".format(it) }

    fun music(context: Context, source: String? = null): List<String> {
        val title = try {
            val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val listener = ComponentName(context, MediaNotificationListener::class.java)
            val sessions = manager.getActiveSessions(listener)
            val filtered = if (source.isNullOrBlank()) sessions else {
                sessions.filter { it.packageName == source }
            }
            val controller = filtered.firstOrNull {
                it.playbackState?.state == PlaybackState.STATE_PLAYING
            } ?: filtered.firstOrNull()
            controller?.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?.uppercase()
                ?.take(7)
                ?.ifBlank { "NO MEDIA" }
                ?: "NO MEDIA"
        } catch (_: Exception) {
            "NO MEDIA"
        }
        return listOf(title)
    }

    private val fallbackWeather = listOf("----", "----")

    private fun number(json: String, key: String): String? =
        Regex("\"$key\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").find(json)?.groupValues?.get(1)

    private fun firstArrayNumber(json: String, key: String): String? =
        Regex("\"$key\"\\s*:\\s*\\[\\s*(-?\\d+(?:\\.\\d+)?)").find(json)?.groupValues?.get(1)

    private fun nextHourlyTemps(json: String): List<Int> {
        val hourlyBlock = json.substringAfter("\"hourly\"", "")
        if (hourlyBlock.isBlank()) return emptyList()
        val timeArray = Regex("\"time\"\\s*:\\s*\\[([^]]*)]")
            .find(hourlyBlock)?.groupValues?.get(1).orEmpty()
        val tempArray = Regex("\"temperature_2m\"\\s*:\\s*\\[([^]]*)]")
            .find(hourlyBlock)?.groupValues?.get(1).orEmpty()
        if (timeArray.isBlank() || tempArray.isBlank()) return emptyList()

        val hours = Regex("T(\\d{2}):").findAll(timeArray)
            .map { it.groupValues[1].toIntOrNull() ?: -1 }.toList()
        val temps = Regex("-?\\d+(?:\\.\\d+)?").findAll(tempArray)
            .map { it.value.toDouble().toInt() }.toList()
        val currentHour = LocalDateTime.now().hour
        val start = hours.indexOfFirst { it >= currentHour }.takeIf { it >= 0 } ?: 0
        return temps.drop(start).take(3)
    }

    @Suppress("MissingPermission")
    private fun detectLocation(context: Context): Pair<Double, Double>? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val location: Location? = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstNotNullOfOrNull { provider ->
                try {
                    manager.getLastKnownLocation(provider)
                } catch (_: SecurityException) {
                    null
                }
            }
        return location?.let { it.latitude to it.longitude }
    }

    private val CITIES = mapOf(
        "Москва" to Pair(55.7558, 37.6173),
        "Санкт-Петербург" to Pair(59.9343, 30.3351),
        "Казань" to Pair(55.7961, 49.1064),
        "Екатеринбург" to Pair(56.8389, 60.6057),
        "Новосибирск" to Pair(55.0084, 82.9357),
        "Краснодар" to Pair(45.0355, 38.9753),
        "Сочи" to Pair(43.5855, 39.7231),
    )
}
