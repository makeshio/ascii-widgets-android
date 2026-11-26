package com.makeshio.asciiwidgets.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.makeshio.asciiwidgets.data.AppDatabase
import com.makeshio.asciiwidgets.data.DefaultTemplates
import com.makeshio.asciiwidgets.data.GlyphEntity
import com.makeshio.asciiwidgets.data.WidgetKind
import com.makeshio.asciiwidgets.widget.WidgetUpdater
import kotlinx.coroutines.launch

@Composable
fun TemplateEditorScreen(
    database: AppDatabase,
    templateId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val template by produceState<com.makeshio.asciiwidgets.data.TemplateEntity?>(
        initialValue = null,
        database,
        templateId,
    ) {
        value = database.templateDao().byId(templateId)
    }
    val storedGlyphs by produceState(initialValue = emptyList<GlyphEntity>(), database, templateId) {
        database.templateDao().glyphs(templateId).collect { value = it }
    }
    var glyphs by remember(storedGlyphs) {
        mutableStateOf(storedGlyphs.associate { it.symbol to it.rows })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        GhostButton(text = "← Назад", onClick = onBack)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "РЕДАКТОР ГЛИФОВ",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = template?.name ?: templateId,
            style = MaterialTheme.typography.titleMedium.copy(color = AsciiColors.Accent),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Каждая строка — ряд 5×6. # — включённая ячейка, точка — пустая.",
            style = MaterialTheme.typography.bodySmall.copy(color = AsciiColors.TextMuted),
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(glyphs.keys.sorted(), key = { it }) { symbol ->
                OutlinedTextField(
                    value = glyphs[symbol].orEmpty(),
                    onValueChange = { text -> glyphs = glyphs + (symbol to text) },
                    label = { Text(if (symbol == " ") "Пробел" else symbol) },
                    minLines = 6,
                    maxLines = 6,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        PrimaryButton(
            text = "Сохранить глифы",
            onClick = {
                scope.launch {
                    database.templateDao().deleteGlyphs(templateId)
                    database.templateDao().insertGlyphs(
                        glyphs.map { (symbol, rows) -> GlyphEntity(templateId, symbol, rows) },
                    )
                    database.templateDao().byId(templateId)?.let { saved ->
                        WidgetKind.from(saved.kind).let { kind ->
                            WidgetUpdater.updateAll(context, kind)
                        }
                    }
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        GhostButton(
            text = "Сбросить к умолчанию",
            onClick = {
                glyphs = DefaultTemplates.glyphsFor(templateId)
                    .associate { it.symbol to it.rows }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
