package com.bildirimbutce.app.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.bildirimbutce.app.service.NotificationService

/** Bildirim erisimi izninin durumu ve sistem ayarina yonlendirme. */
object NotificationAccess {

    fun isGranted(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ) ?: return false
        val target = ComponentName(context, NotificationService::class.java)
        return enabled.split(":").any {
            val cn = ComponentName.unflattenFromString(it)
            cn != null && cn.packageName == target.packageName
        }
    }

    fun settingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
