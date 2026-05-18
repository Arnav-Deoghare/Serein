package com.zen.launcher.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.zen.launcher.launcher.getAllowedNotifPackages
import com.zen.launcher.launcher.recordBlockedNotification

class NotificationFilterService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in getAllowedNotifPackages()) {
            recordBlockedNotification(sbn.packageName)
            cancelNotification(sbn.key)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}
