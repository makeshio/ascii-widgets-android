package com.makeshio.asciiwidgets.ui

import android.appwidget.AppWidgetProvider
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.makeshio.asciiwidgets.data.AppDatabase
import com.makeshio.asciiwidgets.data.EffectiveSettings
import com.makeshio.asciiwidgets.data.SettingsEntity
import com.makeshio.asciiwidgets.data.WidgetConfigEntity
import com.makeshio.asciiwidgets.data.WidgetKind
import com.makeshio.asciiwidgets.widget.BatteryWidgetProvider
import com.makeshio.asciiwidgets.widget.ClockCompactWidgetProvider
import com.makeshio.asciiwidgets.widget.ClockLargeWidgetProvider
import com.makeshio.asciiwidgets.widget.ClockWidgetProvider
import com.makeshio.asciiwidgets.widget.HourlyWeatherWidgetProvider
import com.makeshio.asciiwidgets.widget.MusicWidgetProvider
import com.makeshio.asciiwidgets.widget.WeatherWidgetProvider

private data class WidgetOption(
    val label: String,
    val description: String,
    val kind: WidgetKind,
    val templateId: String,
    val provider: Class<out AppWidgetProvider>,
)

private val OPTIONS = listOf(
    WidgetOption("Часы 4×2", "Классические figlet-цифры и дата", WidgetKind.CLOCK,
        "builtin_clock", ClockWidgetProvider::class.java),
    WidgetOption("Часы 4×1", "Компактно: только время", WidgetKind.CLOCK,
        "builtin_clock_compact", ClockCompactWidgetProvider::class.java),
    WidgetOption("Часы 4×3", "Крупные часы, дата и день", WidgetKind.CLOCK,
        "builtin_clock_large", ClockLargeWidgetProvider::class.java),
    WidgetOption("Погода 2×2", "Температура и минимум/максимум", WidgetKind.WEATHER,
        "builtin_weather", WeatherWidgetProvider::class.java),
    WidgetOption("Погода 4×2", "Температура и ближайшие часы", WidgetKind.WEATHER,
        "builtin_weather_hourly", HourlyWeatherWidgetProvider::class.java),
    WidgetOption("Батарея 2×2", "Заряд и статусы Wi‑Fi/BT", WidgetKind.BATTERY,
        "builtin_battery", BatteryWidgetProvider::class.java),
    WidgetOption("Музыка 4×1", "Название текущего трека", WidgetKind.MUSIC,
        "builtin_music", MusicWidgetProvider::class.java),
)

@Composable
fun MainScreen(
    database: AppDatabase,
    onRequestLocation: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onAddWidget: (Class<out AppWidgetProvider>, String) -> Unit,
    onEditWidget: (Int) -> Unit,
    onResetWidget: (Int) -> Unit,
) {
    val settings by produceState(initialValue = SettingsEntity(), database) {
        database.settingsDao().flow().collect { value = it ?: SettingsEntity() }
    }
    val configs by produceState(initialValue = emptyList<WidgetConfigEntity>(), database) {
        database.widgetConfigDao().flow().collect { value = it }
    }
    var tab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            listOf("Виджеты", "Мои виджеты", "Настройки").forEachIndexed { index, title ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = { Text(title) },
                )
            }
        }
        when (tab) {
            0 -> GalleryTab(EffectiveSettings.merge(settings, null), onAddWidget)
            1 -> MyWidgetsTab(settings, configs, onEditWidget, onResetWidget)
            2 -> SettingsScreen(
                database = database,
                onRequestLocation = onRequestLocation,
                onRequestExactAlarm = onRequestExactAlarm,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GalleryTab(
    settings: EffectiveSettings,
    onAddWidget: (Class<out AppWidgetProvider>, String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(OPTIONS, key = { it.templateId }) { option ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    WidgetPreview(
                        kind = option.kind,
                        templateId = option.templateId,
                        settings = settings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                    )
                    Text(option.label, style = MaterialTheme.typography.titleMedium)
                    Text(option.description, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { onAddWidget(option.provider, option.templateId) }) {
                        Text("Добавить на экран")
                    }
                }
            }
        }
    }
}

@Composable
private fun MyWidgetsTab(
    globalSettings: SettingsEntity,
    configs: List<WidgetConfigEntity>,
    onEditWidget: (Int) -> Unit,
    onResetWidget: (Int) -> Unit,
) {
    if (configs.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text("На домашнем экране пока нет настроенных виджетов.", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Добавьте виджет на вкладке «Виджеты» или долгим нажатием на рабочий стол.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(configs, key = { it.appWidgetId }) { config ->
            val kind = WidgetKind.from(config.kind)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    WidgetPreview(
                        kind = kind,
                        templateId = config.templateId,
                        settings = EffectiveSettings.merge(globalSettings, config),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                    )
                    Text("id ${config.appWidgetId} · ${config.kind}", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { onEditWidget(config.appWidgetId) }) {
                            Text("Изменить")
                        }
                        OutlinedButton(
                            onClick = { onResetWidget(config.appWidgetId) },
                            modifier = Modifier.padding(start = 8.dp),
                        ) { Text("Сбросить") }
                    }
                }
            }
        }
    }
}
