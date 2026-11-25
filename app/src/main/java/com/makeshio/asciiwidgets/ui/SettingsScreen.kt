package com.makeshio.asciiwidgets.ui

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.makeshio.asciiwidgets.data.AppDatabase
import com.makeshio.asciiwidgets.data.Repository
import com.makeshio.asciiwidgets.data.SettingsEntity
import com.makeshio.asciiwidgets.data.TemplateEntity
import com.makeshio.asciiwidgets.data.WidgetKind
import com.makeshio.asciiwidgets.widget.WidgetUpdater
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    database: AppDatabase,
    onRequestLocation: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editingTemplateId by remember { mutableStateOf<String?>(null) }

    if (editingTemplateId != null) {
        TemplateEditorScreen(
            database = database,
            templateId = editingTemplateId!!,
            onBack = { editingTemplateId = null },
        )
        return
    }

    val settings by produceState(initialValue = SettingsEntity(), database) {
        Repository.ensureSeeded(database)
        database.settingsDao().flow().collect { value = it ?: SettingsEntity() }
    }
    val templates by produceState(initialValue = emptyList<TemplateEntity>(), database) {
        database.templateDao().all().collect { value = it }
    }
    var draft by remember(settings) { mutableStateOf(settings) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Ascii Widgets", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Часы 4×2 · Погода 2×2 · Батарея 2×2 · Музыка 4×1",
            style = MaterialTheme.typography.bodySmall,
        )

        SectionTitle("Виджеты")
        WidgetKind.entries.forEach { kind ->
            val templateName = templates.firstOrNull { it.kind == kind.name }?.name ?: "—"
            Text("${kind.name}: $templateName", style = MaterialTheme.typography.bodyLarge)
        }
        Button(
            onClick = {
                scope.launch {
                    WidgetKind.entries.forEach { WidgetUpdater.updateAll(context, it) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Обновить все виджеты") }

        SectionTitle("Внешний вид")
        ColorPicker(draft.textColor) { draft = draft.copy(textColor = it) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("24-часовой формат", modifier = Modifier.weight(1f))
            Switch(checked = draft.use24Hour, onCheckedChange = { draft = draft.copy(use24Hour = it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Рамка", modifier = Modifier.weight(1f))
            Switch(checked = draft.frame, onCheckedChange = { draft = draft.copy(frame = it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Анимация индикаторов", modifier = Modifier.weight(1f))
            Switch(
                checked = draft.animationEnabled,
                onCheckedChange = { draft = draft.copy(animationEnabled = it) },
            )
        }
        Row {
            OutlinedButton(onClick = { draft = draft.copy(tempUnit = "C") }) { Text("°C") }
            OutlinedButton(onClick = { draft = draft.copy(tempUnit = "F") }) { Text("°F") }
        }

        SectionTitle("Погода")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Определять город", modifier = Modifier.weight(1f))
            Switch(checked = draft.autoLocation, onCheckedChange = { draft = draft.copy(autoLocation = it) })
        }
        OutlinedTextField(
            value = draft.city,
            onValueChange = { draft = draft.copy(city = it) },
            label = { Text("Город") },
            enabled = !draft.autoLocation,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = onRequestLocation, modifier = Modifier.fillMaxWidth()) {
            Text("Разрешить геолокацию")
        }

        SectionTitle("Музыка")
        Button(
            onClick = {
                try {
                    context.startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (_: Exception) {
                    Unit
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Дать доступ к уведомлениям") }

        Button(
            onClick = {
                scope.launch {
                    database.settingsDao().save(draft)
                    WidgetKind.entries.forEach { WidgetUpdater.updateAll(context, it) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Сохранить и применить") }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        SectionTitle("Библиотека ASCII-шаблонов")
        templates.forEach { template ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${template.kind}: ${template.name}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = { editingTemplateId = template.id }) { Text("Править") }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun ColorPicker(current: String, onChange: (String) -> Unit) {
    Row {
        listOf(
            "#FFFFFFFF" to "Белый",
            "#FFB0B0B0" to "Серый",
            "#FF707070" to "Тусклый",
        ).forEach { (color, name) ->
            OutlinedButton(
                onClick = { onChange(color) },
                modifier = Modifier.padding(end = 8.dp),
            ) { Text(name + if (current == color) " ✓" else "") }
        }
    }
}
