package com.makeshio.asciiwidgets.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.makeshio.asciiwidgets.data.AppDatabase
import com.makeshio.asciiwidgets.data.Repository
import com.makeshio.asciiwidgets.data.TemplateEntity
import com.makeshio.asciiwidgets.data.WidgetConfigEntity
import com.makeshio.asciiwidgets.data.WidgetKind
import com.makeshio.asciiwidgets.ui.AsciiTheme
import kotlinx.coroutines.launch

class WidgetConfigureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))

        val kind = resolveKind(appWidgetId)
        val database = AppDatabase.get(this)

        setContent {
            AsciiTheme {
                val templates by produceState(initialValue = emptyList<TemplateEntity>(), kind) {
                    Repository.ensureSeeded(database)
                    database.templateDao().byKind(kind.name).collect { value = it }
                }
                ConfigureScreen(kind, templates) { templateId ->
                    lifecycleScope.launch {
                        database.widgetConfigDao().upsert(
                            WidgetConfigEntity(appWidgetId, kind.name, templateId),
                        )
                        WidgetUpdater.update(this@WidgetConfigureActivity, appWidgetId, kind)
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                        )
                        finish()
                    }
                }
            }
        }
    }

    private fun resolveKind(appWidgetId: Int): WidgetKind {
        val className = AppWidgetManager.getInstance(this)
            .getAppWidgetInfo(appWidgetId)?.provider?.className
        return when (className) {
            WeatherWidgetProvider::class.java.name -> WidgetKind.WEATHER
            BatteryWidgetProvider::class.java.name -> WidgetKind.BATTERY
            MusicWidgetProvider::class.java.name -> WidgetKind.MUSIC
            else -> WidgetKind.CLOCK
        }
    }
}

@androidx.compose.runtime.Composable
private fun ConfigureScreen(
    kind: WidgetKind,
    templates: List<TemplateEntity>,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Выберите шаблон: ${kind.name}", style = MaterialTheme.typography.titleLarge)
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(templates, key = { it.id }) { template ->
                Text(
                    text = template.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(template.id) }
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        Button(onClick = { onSelect(com.makeshio.asciiwidgets.data.DefaultTemplates.defaultTemplate(kind).id) }) {
            Text("Готовый шаблон")
        }
    }
}
