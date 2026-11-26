package com.makeshio.asciiwidgets.ui

import android.graphics.Color
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ColorField(
    current: String,
    label: String,
    onChange: (String) -> Unit,
) {
    var text by remember(current) { mutableStateOf(current) }
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { value ->
                text = value
                if (isValidColor(value)) onChange(value)
            },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(
            onClick = {
                text = "#FFFFFFFF"
                onChange(text)
            },
            modifier = Modifier.padding(start = 6.dp),
        ) { Text("Белый") }
    }
}

private fun isValidColor(value: String): Boolean = try {
    Color.parseColor(value)
    true
} catch (_: IllegalArgumentException) {
    false
}
