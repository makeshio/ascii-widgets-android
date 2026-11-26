package com.makeshio.asciiwidgets.widget

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Даёт приложению доступ к активным медиасессиям и запоминает источник последнего трека. */
class MediaNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val packageName = sbn?.packageName
        if (!packageName.isNullOrBlank()) {
            lastPackage = packageName
            knownPackages.add(packageName)
        }
    }

    companion object {
        @Volatile
        var lastPackage: String? = null
            private set

        /** Пакеты плееров, от которых приходили уведомления в этой сессии. */
        val knownPackages: MutableSet<String> = linkedSetOf()
    }
}
