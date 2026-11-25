package com.makeshio.asciiwidgets

import android.app.Application
import com.makeshio.asciiwidgets.data.AppDatabase
import com.makeshio.asciiwidgets.data.Repository
import com.makeshio.asciiwidgets.widget.UpdateScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AsciiWidgetsApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.get(this)
        scope.launch { Repository.ensureSeeded(database) }
        UpdateScheduler.schedule(this)
    }
}
