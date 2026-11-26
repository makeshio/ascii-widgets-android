package com.makeshio.asciiwidgets.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val QUICK_COLORS = listOf(
    "#FFFFFFFF",
    "#FF808080",
    "#FF7DF9C0",
    "#FFFF7B72",
)

@Composable
fun ColorField(
    current: String,
    label: String,
    onChange: (String) -> Unit,
) {
    var text by remember(current) { mutableStateOf(current) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(parseSwatch(current))
                .border(1.dp, AsciiColors.Outline, RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(10.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { value ->
                text = value
                if (isValidColor(value)) onChange(value)
            },
            label = { Text(label) },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.weight(1f),
        )
    }

    Row(modifier = Modifier.padding(start = 48.dp, top = 6.dp)) {
        QUICK_COLORS.forEach { color ->
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(parseSwatch(color))
                    .border(1.dp, AsciiColors.Outline, RoundedCornerShape(6.dp))
                    .clickable {
                        text = color
                        onChange(color)
                    },
            )
            Spacer(Modifier.width(8.dp))
        }
    }
}

private fun isValidColor(value: String): Boolean = try {
    AndroidColor.parseColor(value)
    true
} catch (_: IllegalArgumentException) {
    false
}

private fun parseSwatch(value: String): Color = try {
    Color(AndroidColor.parseColor(value))
} catch (_: IllegalArgumentException) {
    AsciiColors.TextMuted
}
