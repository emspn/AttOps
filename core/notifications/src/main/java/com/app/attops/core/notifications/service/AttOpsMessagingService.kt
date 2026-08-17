package com.app.attops.core.notifications.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.app.attops.core.notifications.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.app.attops.core.common.util.RefreshBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AttOpsMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var refreshBus: RefreshBus

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New Token: $token")
        // We'll handle token upload in the AuthRepository/Login flow 
        // to ensure it's linked to the correct user.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM", "Message Received: ${message.data}")

        // Trigger a silent background refresh if the data payload says so
        if (message.data["refresh"] == "true") {
            scope.launch {
                refreshBus.trigger()
            }
        }

        val title = message.notification?.title ?: message.data["title"] ?: "AttOps Alert"
        val body = message.notification?.body ?: message.data["body"] ?: ""

        if (body.isNotEmpty()) {
            showNotification(title, body, message.data)
        }
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "attops_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AttOps Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Task updates and integrity alerts"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Deep linking logic: Point to MainActivity explicitly
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            setComponent(ComponentName(packageName, "com.app.attops.MainActivity"))
            putExtra("task_id", data["task_id"])
            putExtra("type", data["type"])
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 
            (data["task_id"]?.hashCode() ?: 0) + System.currentTimeMillis().toInt(), 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }
}
