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
import com.makeshio.asciiwidgets.data.EffectiveSettings
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
    val exactAllowed = UpdateScheduler.canScheduleExact(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("НАСТРОЙКИ", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Простые значения для всех виджетов. У конкретного виджета могут быть свои — они важнее.",
                style = MaterialTheme.typography.bodySmall.copy(color = AsciiColors.TextMuted),
            )
        }

        SectionCard(title = "ВИД ВИДЖЕТОВ И СИМВОЛОВ") {
            PreviewFrame(
                kind = WidgetKind.CLOCK,
                templateId = "builtin_clock",
                settings = EffectiveSettings.merge(draft, null),
                height = 118.dp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Пример обновляется сразу. Чтобы применить ко всем виджетам, нажмите «Сохранить» внизу.",
                style = MaterialTheme.typography.bodySmall.copy(color = AsciiColors.TextMuted),
            )
            Spacer(Modifier.height(16.dp))

            ColorField(draft.textColor, "Яркий цвет") {
                draft = draft.copy(textColor = it)
            }
            Spacer(Modifier.height(12.dp))
            ColorField(draft.dimColor, "Цвет разделителей") {
                draft = draft.copy(dimColor = it)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Арт-сцены используют свою палитру и не зависят от этих цветов.",
                style = MaterialTheme.typography.bodySmall.copy(color = AsciiColors.TextMuted),
            )
            Spacer(Modifier.height(16.dp))

            val chosen = StyleMap.decode(draft.styleMap)
            listOf("digits" to "Цифры", "symbols" to "Знаки", "letters" to "Буквы")
                .forEach { (group, label) ->
                    val currentId = chosen[group].orEmpty()
                    val currentStyle = styles.firstOrNull { it.id == currentId }
                    val currentName = currentStyle?.name ?: currentId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("$label: $currentName", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (!currentStyle?.chars.isNullOrBlank()) {
                                    " Symbolic: ${currentStyle.chars} · сменить справа"
                                } else {
                                    "Нажмите справа, чтобы сменить"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(color = AsciiColors.TextMuted),
                            )
                        }
                        GhostButton(
                            text = "Сменить",
                            onClick = {
                                if (styles.isEmpty()) return@GhostButton
                                val index = styles.indexOfFirst { it.id == currentId }
                                val next = styles[(index + 1) % styles.size]
                                draft = draft.copy(
                                    styleMap = StyleMap.encode(chosen + (group to next.id)),
                                )
                            },
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
        }

        SectionCard(title = "ЧАСЫ И ПОГОДА") {
            ToggleRow(
                label = "24-часовое время",
                description = "14:30 вместо 02:30",
                checked = draft.use24Hour,
                onCheckedChange = { draft = draft.copy(use24Hour = it) },
            )
            ToggleRow(
                label = "Определять город самому",
                description = "По геолокации телефона",
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
            if (!draft.autoLocation) {
                Spacer(Modifier.height(8.dp))
                GhostButton(text = "Разрешить геолокацию", onClick = onRequestLocation)
            }
            Spacer(Modifier.height(16.dp))
            Text("Температура", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Row {
                GhostButton(
                    text = if (draft.tempUnit == "C") "°C  ✓" else "°C",
                    onClick = { draft = draft.copy(tempUnit = "C") },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                GhostButton(
                    text = if (draft.tempUnit == "F") "°F  ✓" else "°F",
                    onClick = { draft = draft.copy(tempUnit = "F") },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        SectionCard(title = "МУЗЫКА") {
            Text(
                "Один раз разрешите доступ к уведомлениям — виджет увидит название трека.",
                style = MaterialTheme.typography.bodySmall.copy(color = AsciiColors.TextMuted),
            )
            Spacer(Modifier.height(10.dp))
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
                label = { Text("Плеер (необязательно)") },
                placeholder = { Text("любой плеер") },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Оставьте пустым, чтобы показывать музыку из любого приложения.",
                style = MaterialTheme.typography.bodySmall.copy(color = AsciiColors.TextMuted),
            )
            MediaNotificationListener.lastPackage?.let { last ->
                Spacer(Modifier.height(8.dp))
                GhostButton(
                    text = "Взять последний: ${last.take(20)}",
                    onClick = { draft = draft.copy(musicSource = last) },
                )
            }
            val known = MediaNotificationListener.knownPackages.take(2)
            if (known.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                known.forEach { packageName ->
                    GhostButton(
                        text = packageName.take(26),
                        onClick = { draft = draft.copy(musicSource = packageName) },
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        SectionCard(title = "ОБНОВЛЕНИЯ") {
            Text(
                if (exactAllowed) {
                    "Виджеты обновляются каждую минуту вовремя."
                } else {
                    "Обновления могут опаздывать. Разрешите точное время — и часы всегда будут точными."
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (exactAllowed) AsciiColors.Accent else AsciiColors.Danger,
                ),
            )
            Spacer(Modifier.height(10.dp))
            if (!exactAllowed) {
                GhostButton(text = "Разрешить точное время", onClick = onRequestExactAlarm)
                Spacer(Modifier.height(8.dp))
            }
            GhostButton(
                text = "Обновить виджеты сейчас",
                onClick = {
                    scope.launch {
                        WidgetKind.entries.forEach { WidgetUpdater.updateAll(context, it) }
                    }
                },
            )
        }

        SectionCard(title = "ГОТОВО") {
            PrimaryButton(
                text = "Сохранить и применить",
                onClick = {
                    scope.launch {
                        database.settingsDao().save(draft)
                        WidgetKind.entries.forEach { WidgetUpdater.updateAll(context, it) }
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Виджеты со своими настройками не изменятся.",
                style = MaterialTheme.typography.bodySmall.copy(color = AsciiColors.TextMuted),
            )
        }

        SectionCard(title = "ДОПОЛНИТЕЛЬНО") {
            Text("Библиотека шаблонов", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Тут можно подправить сами ASCII-символы.",
                style = MaterialTheme.typography.bodySmall.copy(color = AsciiColors.TextMuted),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = templateQuery,
                onValueChange = { templateQuery = it },
                label = { Text("Поиск") },
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
                    Spacer(Modifier.height(6.dp))
                }
        }
    }
}
