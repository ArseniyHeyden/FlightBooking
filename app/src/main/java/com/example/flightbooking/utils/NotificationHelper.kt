package com.example.flightbooking.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.flightbooking.MainActivity
import com.example.flightbooking.R
import com.example.flightbooking.data.entity.Ticket
import java.text.SimpleDateFormat
import java.util.*

class NotificationHelper(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Уведомления о рейсах",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминания о предстоящих рейсах"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun scheduleFlightNotifications(ticket: Ticket) {
        try {
            val departureDate = dateFormat.parse(ticket.departureDate)
            if (departureDate == null) return
            
            val calendar = Calendar.getInstance()
            calendar.time = departureDate
            
            // Уведомление за сутки до вылета
            val oneDayBefore = Calendar.getInstance().apply {
                time = departureDate
                add(Calendar.DAY_OF_YEAR, -1)
                set(Calendar.HOUR_OF_DAY, 9) // 9 утра
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            
            if (oneDayBefore.timeInMillis > System.currentTimeMillis()) {
                scheduleNotification(
                    ticketId = ticket.id,
                    notificationId = ticket.id * 10 + 1,
                    title = "Напоминание о рейсе",
                    message = "Ваш рейс состоится завтра! Не забудьте проверить детали.",
                    timeInMillis = oneDayBefore.timeInMillis
                )
            }
            
            // Уведомление в день вылета
            val dayOfFlight = Calendar.getInstance().apply {
                time = departureDate
                set(Calendar.HOUR_OF_DAY, 7) // 7 утра в день вылета
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            
            if (dayOfFlight.timeInMillis > System.currentTimeMillis()) {
                scheduleNotification(
                    ticketId = ticket.id,
                    notificationId = ticket.id * 10 + 2,
                    title = "Сегодня ваш рейс!",
                    message = "Сегодня день вылета! Проверьте время и место отправления.",
                    timeInMillis = dayOfFlight.timeInMillis
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun scheduleNotification(
        ticketId: Int,
        notificationId: Int,
        title: String,
        message: String,
        timeInMillis: Long
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmIntent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("title", title)
            putExtra("message", message)
            putExtra("ticket_id", ticketId)
        }
        
        val alarmPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            alarmPendingIntent
        )
    }
    
    fun cancelNotifications(ticketId: Int) {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                ticketId * 10 + 1,
                Intent(context, NotificationReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                ticketId * 10 + 2,
                Intent(context, NotificationReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }
    
    fun showNotification(notificationId: Int, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
    
    companion object {
        const val CHANNEL_ID = "flight_notifications"
    }
}
