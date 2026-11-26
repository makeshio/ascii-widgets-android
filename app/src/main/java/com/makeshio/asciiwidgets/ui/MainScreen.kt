package com.makeshio.asciiwidgets.ui

import android.appwidget.AppWidgetProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    WidgetOption("ЧАСЫ 4×2", "Классические figlet-цифры и дата", WidgetKind.CLOCK,
        "builtin_clock", ClockWidgetProvider::class.java),
    WidgetOption("ЧАСЫ 4×1", "Компактно: только время", WidgetKind.CLOCK,
        "builtin_clock_compact", ClockCompactWidgetProvider::class.java),
    WidgetOption("ЧАСЫ 4×3", "Крупные часы, дата и день", WidgetKind.CLOCK,
        "builtin_clock_large", ClockLargeWidgetProvider::class.java),
    WidgetOption("ПОГОДА 2×2", "Температура и минимум/максимум", WidgetKind.WEATHER,
        "builtin_weather", WeatherWidgetProvider::class.java),
    WidgetOption("ПОГОДА 4×2", "Температура и ближайшие часы", WidgetKind.WEATHER,
        "builtin_weather_hourly", HourlyWeatherWidgetProvider::class.java),
    WidgetOption("БАТАРЕЯ 2×2", "Заряд и статусы Wi‑Fi/BT", WidgetKind.BATTERY,
        "builtin_battery", BatteryWidgetProvider::class.java),
    WidgetOption("МУЗЫКА 4×1", "Название текущего трека", WidgetKind.MUSIC,
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppHeader(
                subtitle = "figlet-виджеты · room v4",
                modifier = Modifier.statusBarsPadding(),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SegmentedTabs(
                items = listOf("Виджеты", "Мои виджеты", "Настройки"),
                selected = tab,
                onSelect = { tab = it },
            )
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
}

@Composable
private fun GalleryTab(
    settings: EffectiveSettings,
    onAddWidget: (Class<out AppWidgetProvider>, String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    "КАТАЛОГ",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = AsciiColors.Text,
                    ),
                )
                Text(
                    "Выберите виджет. Настройки можно поменять после добавления.",
                    style = MaterialTheme.typography.bodySmall.copy(color = AsciiColors.TextMuted),
                )
            }
        }
        items(OPTIONS, key = { it.templateId }) { option ->
            SectionCard {
                Box {
                    PreviewFrame(
                        kind = option.kind,
                        templateId = option.templateId,
                        settings = settings,
                        height = 104.dp,
                    )
                    StatusBadge(option.label)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    option.label,
                    style = MaterialTheme.typography.titleMedium.copy(color = AsciiColors.Text),
                )
                Text(
                    option.description,
                    style = MaterialTheme.typography.bodySmall.copy(color = AsciiColors.TextMuted),
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    text = "+ Добавить на экран",
                    onClick = { onAddWidget(option.provider, option.templateId) },
                )
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
        Column(modifier = Modifier.fillMaxSize()) {
            SectionCard(title = "ПУСТО") {
                Text(
                    "На домашнем экране пока нет настроенных виджетов.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Добавьте виджет на вкладке «Виджеты» или долгим нажатием по рабочему столу.",
                    style = MaterialTheme.typography.bodySmall.copy(color = AsciiColors.TextMuted),
                )
            }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Text(
                "МОИ ВИДЖЕТЫ · ${configs.size}",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        items(configs, key = { it.appWidgetId }) { config ->
            val kind = WidgetKind.from(config.kind)
            SectionCard(title = "ID ${config.appWidgetId} · ${config.kind}") {
                PreviewFrame(
                    kind = kind,
                    templateId = config.templateId,
                    settings = EffectiveSettings.merge(globalSettings, config),
                    height = 88.dp,
                )
                Spacer(Modifier.height(12.dp))
                Row {
                    GhostButton(
                        text = "Изменить",
                        onClick = { onEditWidget(config.appWidgetId) },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    GhostButton(
                        text = "Сбросить",
                        onClick = { onResetWidget(config.appWidgetId) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
