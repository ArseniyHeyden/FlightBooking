package com.example.flightbooking.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", 0)
        val title = intent.getStringExtra("title") ?: "Напоминание о рейсе"
        val message = intent.getStringExtra("message") ?: "Не забудьте о вашем рейсе!"
        
        NotificationHelper(context).showNotification(notificationId, title, message)
    }
}
