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
import com.makeshio.asciiwidgets.data.StyleEntity
import com.makeshio.asciiwidgets.data.StyleMap
import com.makeshio.asciiwidgets.data.TemplateEntity
import com.makeshio.asciiwidgets.data.WidgetKind
import com.makeshio.asciiwidgets.widget.MediaNotificationListener
import com.makeshio.asciiwidgets.widget.UpdateScheduler
import com.makeshio.asciiwidgets.widget.WidgetUpdater
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    database: AppDatabase,
    onRequestLocation: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editingTemplateId by remember { mutableStateOf<String?>(null) }
    var templateQuery by remember { mutableStateOf("") }

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
    val styles by produceState(initialValue = emptyList<StyleEntity>(), database) {
        Repository.ensureSeeded(database)
        database.styleDao().all().collect { value = it }
    }
    var draft by remember(settings) { mutableStateOf(settings) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Значения по умолчанию", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Применяются ко всем виджетам, у которых нет своих настроек. Индивидуальные настройки задаются в конфигураторе виджета.",
            style = MaterialTheme.typography.bodySmall,
        )

        SectionTitle("Виджеты")
        WidgetKind.entries.forEach { kind ->
            val names = templates.filter { it.kind == kind.name }.joinToString(", ") { it.name }
            Text("$kind: $names", style = MaterialTheme.typography.bodyMedium)
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
        ColorField(draft.textColor, "Основной цвет HEX") { draft = draft.copy(textColor = it) }
        ColorField(draft.dimColor, "Приглушённый цвет HEX") { draft = draft.copy(dimColor = it) }

        SectionTitle("Стили символов")
        val selectedStyles = StyleMap.decode(draft.styleMap)
        listOf("digits" to "Цифры", "symbols" to "Символы", "letters" to "Буквы")
            .forEach { (group, label) ->
                val currentId = selectedStyles[group].orEmpty()
                val currentName = styles.firstOrNull { it.id == currentId }?.name ?: currentId
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = {
                            if (styles.isEmpty()) return@OutlinedButton
                            val index = styles.indexOfFirst { it.id == currentId }
                            val next = styles[(index + 1) % styles.size]
                            draft = draft.copy(
                                styleMap = StyleMap.encode(selectedStyles + (group to next.id)),
                            )
                        },
                    ) { Text(currentName) }
                }
            }

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

        SectionTitle("Обновления")
        Text(
            if (UpdateScheduler.canScheduleExact(context)) {
                "Точные будильники разрешены."
            } else {
                "Точные будильники не разрешены — тики могут опаздывать."
            },
            style = MaterialTheme.typography.bodySmall,
        )
        Button(onClick = onRequestExactAlarm, modifier = Modifier.fillMaxWidth()) {
            Text("Разрешить точные обновления")
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
        OutlinedTextField(
            value = draft.musicSource,
            onValueChange = { draft = draft.copy(musicSource = it) },
            label = { Text("Пакет музыкального приложения") },
            modifier = Modifier.fillMaxWidth(),
        )
        MediaNotificationListener.lastPackage?.let { last ->
            OutlinedButton(onClick = { draft = draft.copy(musicSource = last) }) {
                Text("Использовать: ${last.take(24)}")
            }
        }
        val known = MediaNotificationListener.knownPackages.take(3)
        if (known.isNotEmpty()) {
            Text("Найденные плееры:", style = MaterialTheme.typography.bodySmall)
            known.forEach { packageName ->
                OutlinedButton(onClick = { draft = draft.copy(musicSource = packageName) }) {
                    Text(packageName.take(28))
                }
            }
        }

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
        OutlinedTextField(
            value = templateQuery,
            onValueChange = { templateQuery = it },
            label = { Text("Поиск шаблона") },
            modifier = Modifier.fillMaxWidth(),
        )
        templates
            .filter { template ->
                templateQuery.isBlank() ||
                    template.name.contains(templateQuery, ignoreCase = true) ||
                    template.kind.contains(templateQuery, ignoreCase = true)
            }
            .forEach { template ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${template.kind}: ${template.name} (${template.cols}×${template.rows})",
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
