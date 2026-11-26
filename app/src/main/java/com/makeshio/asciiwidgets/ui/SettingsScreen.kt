package com.makeshio.asciiwidgets.ui

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
            .padding(bottom = 24.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                "НАСТРОЙКИ",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                "Значения по умолчанию. У каждого виджета могут быть свои.",
                style = MaterialTheme.typography.bodySmall.copy(color = AsciiColors.TextMuted),
            )
        }

        SectionCard(title = "ВИДЖЕТЫ") {
            WidgetKind.entries.forEach { kind ->
                val names = templates.filter { it.kind == kind.name }.joinToString(", ") { it.name }
                Text(
                    "$kind · $names",
                    style = MaterialTheme.typography.bodySmall.copy(color = AsciiColors.TextMuted),
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            GhostButton(
                text = "Обновить все виджеты",
                onClick = {
                    scope.launch {
                        WidgetKind.entries.forEach { WidgetUpdater.updateAll(context, it) }
                    }
                },
            )
        }

        SectionCard(title = "ЦВЕТА И СТИЛИ") {
            ColorField(draft.textColor, "Основной цвет HEX") {
                draft = draft.copy(textColor = it)
            }
            Spacer(Modifier.height(12.dp))
            ColorField(draft.dimColor, "Приглушённый цвет HEX") {
                draft = draft.copy(dimColor = it)
            }
            Spacer(Modifier.height(16.dp))

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
                        GhostButton(
                            text = currentName,
                            onClick = {
                                if (styles.isEmpty()) return@GhostButton
                                val index = styles.indexOfFirst { it.id == currentId }
                                val next = styles[(index + 1) % styles.size]
                                draft = draft.copy(
                                    styleMap = StyleMap.encode(selectedStyles + (group to next.id)),
                                )
                            },
                        )
                    }
                }
        }

        SectionCard(title = "ПОВЕДЕНИЕ") {
            ToggleRow(
                label = "24-часовой формат",
                checked = draft.use24Hour,
                onCheckedChange = { draft = draft.copy(use24Hour = it) },
            )
            ToggleRow(
                label = "Рамка",
                checked = draft.frame,
                onCheckedChange = { draft = draft.copy(frame = it) },
            )
            ToggleRow(
                label = "Анимация индикаторов",
                checked = draft.animationEnabled,
                onCheckedChange = { draft = draft.copy(animationEnabled = it) },
            )
            Spacer(Modifier.height(8.dp))
            Row {
                GhostButton(
                    text = "°C",
                    onClick = { draft = draft.copy(tempUnit = "C") },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                GhostButton(
                    text = "°F",
                    onClick = { draft = draft.copy(tempUnit = "F") },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        SectionCard(title = "ОБНОВЛЕНИЯ") {
            Text(
                if (UpdateScheduler.canScheduleExact(context)) {
                    "● Точные будильники разрешены"
                } else {
                    "○ Точные будильники не разрешены — тики могут опаздывать"
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (UpdateScheduler.canScheduleExact(context)) {
                        AsciiColors.Accent
                    } else {
                        AsciiColors.Danger
                    },
                ),
            )
            Spacer(Modifier.height(12.dp))
            GhostButton(text = "Разрешить точные обновления", onClick = onRequestExactAlarm)
        }

        SectionCard(title = "ПОГОДА") {
            ToggleRow(
                label = "Определять город",
                checked = draft.autoLocation,
                onCheckedChange = { draft = draft.copy(autoLocation = it) },
            )
            OutlinedTextField(
                value = draft.city,
                onValueChange = { draft = draft.copy(city = it) },
                label = { Text("Город") },
                enabled = !draft.autoLocation,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            GhostButton(text = "Разрешить геолокацию", onClick = onRequestLocation)
        }

        SectionCard(title = "МУЗЫКА") {
            GhostButton(
                text = "Дать доступ к уведомлениям",
                onClick = {
                    try {
                        context.startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    } catch (_: Exception) {
                        Unit
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = draft.musicSource,
                onValueChange = { draft = draft.copy(musicSource = it) },
                label = { Text("Пакет музыкального приложения") },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )
            MediaNotificationListener.lastPackage?.let { last ->
                Spacer(Modifier.height(8.dp))
                GhostButton(
                    text = "Последний: ${last.take(24)}",
                    onClick = { draft = draft.copy(musicSource = last) },
                )
            }
            val known = MediaNotificationListener.knownPackages.take(3)
            if (known.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "НАЙДЕННЫЕ ПЛЕЕРЫ",
                    style = MaterialTheme.typography.labelSmall.copy(color = AsciiColors.TextMuted),
                )
                known.forEach { packageName ->
                    GhostButton(
                        text = packageName.take(28),
                        onClick = { draft = draft.copy(musicSource = packageName) },
                    )
                }
            }
        }

        SectionCard(title = "ПРИМЕНИТЬ") {
            PrimaryButton(
                text = "Сохранить и применить",
                onClick = {
                    scope.launch {
                        database.settingsDao().save(draft)
                        WidgetKind.entries.forEach { WidgetUpdater.updateAll(context, it) }
                    }
                },
            )
        }

        SectionCard(title = "БИБЛИОТЕКА ASCII-ШАБЛОНОВ") {
            OutlinedTextField(
                value = templateQuery,
                onValueChange = { templateQuery = it },
                label = { Text("Поиск шаблона") },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
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
                            style = MaterialTheme.typography.bodySmall,
                        )
                        GhostButton(
                            text = "Править",
                            onClick = { editingTemplateId = template.id },
                        )
                    }
                }
        }
    }
}
