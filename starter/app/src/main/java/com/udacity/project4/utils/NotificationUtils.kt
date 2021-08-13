package com.udacity.project4.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.getSystemService
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.ext.ifSupportsFromAndroidOreo
import com.udacity.project4.locationreminders.ReminderDescriptionActivity
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

private const val NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".channel"

var notificationId = 0

fun sendNotification(context: Context, reminderDataItem: ReminderDataItem) {
    val notificationManager = context.getSystemService<NotificationManager>()
    notificationManager ?: return

    // We need to create a NotificationChannel associated with our CHANNEL_ID before sending a notification.
    ifSupportsFromAndroidOreo {
        if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            val name = context.getString(R.string.app_name)
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    val intent = ReminderDescriptionActivity.newIntent(context.applicationContext, reminderDataItem)

    //create a pending intent that opens ReminderDescriptionActivity when the user clicks on the notification
    val stackBuilder = TaskStackBuilder.create(context)
        .addParentStack(ReminderDescriptionActivity::class.java)
        .addNextIntent(intent)
    val notificationPendingIntent = stackBuilder
        .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

//    build the notification object with the data to be shown
    val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(reminderDataItem.title)
        .setContentText(reminderDataItem.location)
        .setContentIntent(notificationPendingIntent)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(notificationId, notification)
    notificationId += 1
}