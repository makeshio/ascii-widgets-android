package com.makeshio.asciiwidgets.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.makeshio.asciiwidgets.data.AppDatabase
import com.makeshio.asciiwidgets.data.EffectiveSettings
import com.makeshio.asciiwidgets.data.PresetEntity
import com.makeshio.asciiwidgets.data.Repository
import com.makeshio.asciiwidgets.data.SettingsEntity
import com.makeshio.asciiwidgets.data.TemplateEntity
import com.makeshio.asciiwidgets.data.WidgetConfigEntity
import com.makeshio.asciiwidgets.data.WidgetKind
import com.makeshio.asciiwidgets.ui.AsciiTheme
import com.makeshio.asciiwidgets.ui.ColorField
import com.makeshio.asciiwidgets.ui.MainActivity
import com.makeshio.asciiwidgets.ui.WidgetPreview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WidgetConfigureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val editId = intent?.extras?.getInt(
            MainActivity.EXTRA_EDIT_WIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val createId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val appWidgetId = if (editId != AppWidgetManager.INVALID_APPWIDGET_ID) editId else createId

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        if (editId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        }

        val className = AppWidgetManager.getInstance(this)
            .getAppWidgetInfo(appWidgetId)?.provider?.className
        val kind = WidgetUpdater.kindForProvider(className)
        val defaultTemplate = WidgetUpdater.templateForProvider(className)
        val database = AppDatabase.get(this)

        setContent {
            AsciiTheme {
                ConfigureScreen(
                    appWidgetId = appWidgetId,
                    kind = kind,
                    defaultTemplate = defaultTemplate,
                    database = database,
                    onSave = { config -> save(appWidgetId, kind, config) },
                )
            }
        }
    }

    private fun save(appWidgetId: Int, kind: WidgetKind, config: WidgetConfigEntity) {
        lifecycleScope.launch {
            val database = AppDatabase.get(this@WidgetConfigureActivity)
            val finalConfig = config.copy(kind = kind.name)
            database.widgetConfigDao().upsert(finalConfig)
            WidgetUpdater.forget(appWidgetId)
            WidgetUpdater.update(
                context = this@WidgetConfigureActivity,
                appWidgetId = appWidgetId,
                kindHint = kind,
                templateHint = finalConfig.templateId,
            )
            setResult(
                RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
            )
            finish()
        }
    }
}

@Composable
private fun ConfigureScreen(
    appWidgetId: Int,
    kind: WidgetKind,
    defaultTemplate: String,
    database: AppDatabase,
    onSave: (WidgetConfigEntity) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var global by remember { mutableStateOf(SettingsEntity()) }
    var templates by remember { mutableStateOf(emptyList<TemplateEntity>()) }
    var presets by remember { mutableStateOf(emptyList<PresetEntity>()) }
    var draft by remember {
        mutableStateOf(WidgetConfigEntity(appWidgetId, kind.name, defaultTemplate))
    }

    LaunchedEffect(database, appWidgetId) {
        Repository.ensureSeeded(database)
        global = database.settingsDao().get() ?: SettingsEntity()
        templates = database.templateDao().all().first().filter { it.kind == kind.name }
        presets = database.presetDao().allOnce()
        database.widgetConfigDao().get(appWidgetId)?.let { draft = it }
        if (database.templateDao().byId(draft.templateId) == null) {
            draft = draft.copy(templateId = defaultTemplate)
        }
    }

    val effective = EffectiveSettings.merge(global, draft)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Настройка: $kind", style = MaterialTheme.typography.headlineSmall)

        WidgetPreview(
            kind = kind,
            templateId = draft.templateId,
            settings = effective,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(vertical = 8.dp),
        )

        SectionTitle("Шаблон")
        templates.forEach { template ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { draft = draft.copy(templateId = template.id) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${template.name} (${template.cols}×${template.rows})" +
                        if (template.id == draft.templateId) " ✓" else "",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        SectionTitle("Пресет")
        presets.forEach { preset ->
            OutlinedButton(
                onClick = {
                    draft = draft.copy(
                        presetId = preset.id,
                        styleMap = preset.styleMap,
                        textColor = preset.textColor,
                        dimColor = preset.dimColor,
                        frame = preset.frame,
                        animationEnabled = preset.animationEnabled,
                    )
                },
                modifier = Modifier.padding(end = 6.dp),
            ) {
                Text(preset.name + if (preset.id == draft.presetId) " ✓" else "")
            }
        }

        SectionTitle("Свои значения")
        ColorField(draft.textColor ?: global.textColor, "Основной цвет") {
            draft = draft.copy(textColor = it)
        }
        ColorField(draft.dimColor ?: global.dimColor, "Приглушённый цвет") {
            draft = draft.copy(dimColor = it)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Рамка", modifier = Modifier.weight(1f))
            Switch(
                checked = draft.frame ?: global.frame,
                onCheckedChange = { draft = draft.copy(frame = it) },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Анимация", modifier = Modifier.weight(1f))
            Switch(
                checked = draft.animationEnabled ?: global.animationEnabled,
                onCheckedChange = { draft = draft.copy(animationEnabled = it) },
            )
        }

        if (kind == WidgetKind.MUSIC) {
            SectionTitle("Источник музыки")
            OutlinedTextField(
                value = draft.musicSource ?: global.musicSource,
                onValueChange = { draft = draft.copy(musicSource = it) },
                label = { Text("Пакет приложения") },
                modifier = Modifier.fillMaxWidth(),
            )
            MediaNotificationListener.lastPackage?.let { last ->
                OutlinedButton(onClick = { draft = draft.copy(musicSource = last) }) {
                    Text("Последний плеер: ${last.take(24)}")
                }
            }
        }

        SectionTitle("Настройки стилей")
        val mapping = EffectiveSettings.merge(global, draft)
        Text("Стиль символов: ${mapping.styleMap}", style = MaterialTheme.typography.bodySmall)
        Button(
            onClick = { draft = draft.copy(styleMap = com.makeshio.asciiwidgets.data.StyleMap.DEFAULT) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Сбросить стили") }

        Text(
            "Если лаунчер не добавил виджет, разрешите ярлыки на рабочем столе в настройках телефона.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )

        Button(
            onClick = { onSave(draft) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) { Text("Сохранить") }

        OutlinedButton(
            onClick = {
                scope.launch {
                    database.widgetConfigDao().delete(appWidgetId)
                    WidgetUpdater.forget(appWidgetId)
                    onSave(draft.copy(
                        styleMap = null,
                        textColor = null,
                        dimColor = null,
                        frame = null,
                        animationEnabled = null,
                        presetId = null,
                        musicSource = null,
                    ))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Использовать глобальные настройки") }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}
