package com.makeshio.asciiwidgets.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.makeshio.asciiwidgets.data.AppDatabase
import com.makeshio.asciiwidgets.widget.UpdateScheduler

class MainActivity : ComponentActivity() {
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* результат читается через системные настройки виджета */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UpdateScheduler.schedule(this)
        val database = AppDatabase.get(this)
        setContent {
            AsciiTheme {
                SettingsScreen(
                    database = database,
                    onRequestLocation = {
                        locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                )
            }
        }
    }
}
