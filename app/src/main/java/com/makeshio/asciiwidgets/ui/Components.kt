package com.makeshio.asciiwidgets.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makeshio.asciiwidgets.data.EffectiveSettings
import com.makeshio.asciiwidgets.data.WidgetKind

@Composable
fun AppHeader(subtitle: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AsciiColors.Accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = ">_",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = AsciiColors.Background,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "ASCII WIDGETS",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = AsciiColors.Text,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(color = AsciiColors.TextMuted),
            )
        }
    }
}

@Composable
fun SegmentedTabs(
    items: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
            .padding(4.dp),
    ) {
        items.forEachIndexed { index, title ->
            val active = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .background(if (active) AsciiColors.Accent else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = if (active) AsciiColors.Background else AsciiColors.TextMuted,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title != null) {
                SectionLabel(title)
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = AsciiColors.Accent,
            contentColor = AsciiColors.Background,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, AsciiColors.AccentDim),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge.copy(color = AsciiColors.Accent))
    }
}

@Composable
fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall.copy(color = AsciiColors.TextMuted),
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Оформление превью как «экранчика» с тонкой сеткой на тёмном фоне. */
@Composable
fun PreviewFrame(
    kind: WidgetKind,
    templateId: String?,
    settings: EffectiveSettings,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 116.dp,
    artPieceId: String? = null,
    artLookId: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(MaterialTheme.shapes.medium)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF080D10), Color(0xFF121B20)),
                ),
            )
            .border(1.dp, AsciiColors.Outline, MaterialTheme.shapes.medium),
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            val step = 28f
            val gridColor = AsciiColors.Outline.copy(alpha = 0.35f)
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
        }
        WidgetPreview(
            kind = kind,
            templateId = templateId,
            settings = settings,
            artPieceId = artPieceId,
            artLookId = artLookId,
            modifier = Modifier
                .matchParentSize()
                .padding(18.dp),
        )
    }
}

@Composable
fun BoxScope.StatusBadge(text: String) {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(AsciiColors.AccentDim.copy(alpha = 0.55f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(color = AsciiColors.Accent),
        )
    }
}
