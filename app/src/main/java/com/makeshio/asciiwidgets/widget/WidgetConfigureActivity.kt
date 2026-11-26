package com.makeshio.asciiwidgets.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.makeshio.asciiwidgets.data.AppDatabase
import com.makeshio.asciiwidgets.data.ArtPiece
import com.makeshio.asciiwidgets.data.ArtPieces
import com.makeshio.asciiwidgets.data.DefaultTemplates
import com.makeshio.asciiwidgets.data.EffectiveSettings
import com.makeshio.asciiwidgets.data.PresetEntity
import com.makeshio.asciiwidgets.data.Repository
import com.makeshio.asciiwidgets.data.SettingsEntity
import com.makeshio.asciiwidgets.data.StyleEntity
import com.makeshio.asciiwidgets.data.StyleMap
import com.makeshio.asciiwidgets.data.TemplateEntity
import com.makeshio.asciiwidgets.data.WidgetConfigEntity
import com.makeshio.asciiwidgets.data.WidgetKind
import com.makeshio.asciiwidgets.ui.AppHeader
import com.makeshio.asciiwidgets.ui.AsciiColors
import com.makeshio.asciiwidgets.ui.AsciiTheme
import com.makeshio.asciiwidgets.ui.ColorField
import com.makeshio.asciiwidgets.ui.GhostButton
import com.makeshio.asciiwidgets.ui.MainActivity
import com.makeshio.asciiwidgets.ui.PreviewFrame
import com.makeshio.asciiwidgets.ui.PrimaryButton
import com.makeshio.asciiwidgets.ui.SectionCard
import com.makeshio.asciiwidgets.ui.StatusBadge
import com.makeshio.asciiwidgets.ui.ToggleRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        val templateHint = intent?.extras?.getString(MainActivity.EXTRA_TEMPLATE_ID)
        val defaultTemplate = if (!templateHint.isNullOrBlank() &&
            DefaultTemplates.byId(templateHint)?.kind == kind.name
        ) {
            templateHint
        } else {
            WidgetUpdater.templateForProvider(className)
        }
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
    val context = LocalContext.current
    var global by remember { mutableStateOf(SettingsEntity()) }
    var templates by remember { mutableStateOf(emptyList<TemplateEntity>()) }
    var presets by remember { mutableStateOf(emptyList<PresetEntity>()) }
    var styles by remember { mutableStateOf(emptyList<StyleEntity>()) }
    var pieces by remember { mutableStateOf(emptyList<ArtPiece>()) }
    var draft by remember {
        mutableStateOf(WidgetConfigEntity(appWidgetId, kind.name, defaultTemplate))
    }

    LaunchedEffect(database, appWidgetId) {
        Repository.ensureSeeded(database)
        global = database.settingsDao().get() ?: SettingsEntity()
        templates = database.templateDao().all().first().filter { it.kind == kind.name }
        presets = database.presetDao().allOnce()
        styles = database.styleDao().allOnce()
        pieces = withContext(Dispatchers.IO) { ArtPieces.load(context) }
        database.widgetConfigDao().get(appWidgetId)?.let { draft = it }
        if (database.templateDao().byId(draft.templateId) == null) {
            draft = draft.copy(templateId = defaultTemplate)
        }
        if (kind == WidgetKind.ART && draft.artPieceId.isNullOrBlank()) {
            draft = draft.copy(artPieceId = pieces.firstOrNull()?.id)
        }
    }

    val effective = EffectiveSettings.merge(global, draft)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppHeader(
                subtitle = "настройка виджета #$appWidgetId",
                modifier = Modifier.statusBarsPadding(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(bottom = 24.dp),
        ) {
            SectionCard(title = "${kind.name} · ПРЕДПРОСМОТР") {
                Box {
                    PreviewFrame(
                        kind = kind,
                        templateId = draft.templateId,
                        settings = effective,
                        height = 132.dp,
                        artPieceId = draft.artPieceId,
                        artLookId = draft.artLookId,
                    )
                    StatusBadge(
                        if (kind == WidgetKind.ART) {
                            (pieces.firstOrNull { it.id == draft.artPieceId }?.name ?: "ART").uppercase()
                        } else {
                            draft.templateId.replace("builtin_", "").uppercase()
                        },
                    )
                }
            }

            if (kind != WidgetKind.ART) {
                SectionCard(title = "ШАБЛОН") {
                    templates.forEach { template ->
                        val selected = template.id == draft.templateId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) {
                                        AsciiColors.AccentDim.copy(alpha = 0.45f)
                                    } else {
                                        Color.Transparent
                                    },
                                )
                                .clickable { draft = draft.copy(templateId = template.id) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${template.name} · ${template.cols}×${template.rows}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (selected) AsciiColors.Accent else AsciiColors.Text,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            if (selected) {
                                Text("●", color = AsciiColors.Accent)
                            }
                        }
                    }
                }
            }

            if (kind == WidgetKind.ART) {
                SectionCard(title = "СЦЕНА") {
                    Text(
                        "24 сцены из открытой библиотеки ASCII Magic. У каждой своя палитра, кадры сохранены в приложении — интернет не нужен.",
                        style = MaterialTheme.typography.bodySmall.copy(color = AsciiColors.TextMuted),
                    )
                    Spacer(Modifier.height(10.dp))
                    pieces.forEach { piece ->
                        GhostButton(
                            text = "${piece.name} · ${piece.category}" +
                                if (piece.id == draft.artPieceId) "  ●" else "",
                            onClick = { draft = draft.copy(artPieceId = piece.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            if (kind == WidgetKind.ART) {
                SectionCard(title = "ЛУК АРТА") {
                    Text(
                        "Чем рисовать сцену. Только символы на тёмной подложке терминала. По умолчанию — плотный ASCII.",
                        style = MaterialTheme.typography.bodySmall.copy(color = AsciiColors.TextMuted),
                    )
                    Spacer(Modifier.height(10.dp))
                    val currentLook = draft.artLookId ?: StyleMap.DEFAULT_ART_LOOK
                    StyleMap.LOOK_IDS.forEach { id ->
                        val name = styles.firstOrNull { it.id == id }?.name ?: id
                        GhostButton(
                            text = name + if (id == currentLook) "  ●" else "",
                            onClick = { draft = draft.copy(artLookId = id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    GhostButton(
                        text = "Сбросить на плотный ASCII",
                        onClick = { draft = draft.copy(artLookId = null) },
                    )
                }
            }

            if (kind != WidgetKind.ART) {
                SectionCard(title = "ПРЕСЕТ") {
                    presets.forEach { preset ->
                        GhostButton(
                            text = preset.name + if (preset.id == draft.presetId) "  ●" else "",
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
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            SectionCard(title = "СВОИ ЗНАЧЕНИЯ") {
                if (kind == WidgetKind.ART) {
                    Text(
                        "У артов свои цвета из палитры сцены — они применятся автоматически.",
                        style = MaterialTheme.typography.bodySmall.copy(color = AsciiColors.TextMuted),
                    )
                    Spacer(Modifier.height(12.dp))
                } else {
                    ColorField(draft.textColor ?: global.textColor, "Основной цвет") {
                        draft = draft.copy(textColor = it)
                    }
                    Spacer(Modifier.height(12.dp))
                    ColorField(draft.dimColor ?: global.dimColor, "Приглушённый цвет") {
                        draft = draft.copy(dimColor = it)
                    }
                    Spacer(Modifier.height(12.dp))
                }
                if (kind != WidgetKind.ART) {
                    ToggleRow(
                        label = "Рамка",
                        checked = draft.frame ?: global.frame,
                        onCheckedChange = { draft = draft.copy(frame = it) },
                    )
                }
                ToggleRow(
                    label = if (kind == WidgetKind.ART) "Медленная смена кадров" else "Анимация",
                    description = if (kind == WidgetKind.ART) "Один кадр в секунду" else null,
                    checked = draft.animationEnabled ?: global.animationEnabled,
                    onCheckedChange = { draft = draft.copy(animationEnabled = it) },
                )
            }

            if (kind == WidgetKind.MUSIC) {
                SectionCard(title = "ИСТОЧНИК МУЗЫКИ") {
                    OutlinedTextField(
                        value = draft.musicSource ?: global.musicSource,
                        onValueChange = { draft = draft.copy(musicSource = it) },
                        label = { Text("Плеер (необязательно)") },
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
                }
            }

            if (kind != WidgetKind.ART) {
                SectionCard(title = "ВИД СИМВОЛОВ") {
                val chosen = StyleMap.decode(effective.styleMap)
                listOf("digits" to "Цифры", "symbols" to "Знаки", "letters" to "Буквы")
                    .forEach { (group, label) ->
                        val currentId = chosen[group].orEmpty()
                        val currentStyle = styles.firstOrNull { it.id == currentId }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "$label: ${currentStyle?.name ?: currentId}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    if (!currentStyle?.chars.isNullOrBlank()) {
                                        " ${currentStyle.chars} · сменить справа"
                                    } else {
                                        "Нажмите справа, чтобы сменить"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = AsciiColors.TextMuted,
                                    ),
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
                Spacer(Modifier.height(8.dp))
                GhostButton(
                    text = "Сбросить стили",
                    onClick = { draft = draft.copy(styleMap = StyleMap.DEFAULT) },
                )
                }
            }

            SectionCard(title = "СОХРАНЕНИЕ") {
                Text(
                    "Если лаунчер не добавил виджет, разрешите ярлыки на рабочем столе в настройках телефона.",
                    style = MaterialTheme.typography.bodySmall.copy(color = AsciiColors.TextMuted),
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton(text = "Сохранить", onClick = { onSave(draft) })
                Spacer(Modifier.height(8.dp))
                GhostButton(
                    text = "Использовать глобальные настройки",
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
                                artPieceId = null,
                                artLookId = null,
                            ))
                        }
                    },
                )
            }
        }
    }
}
