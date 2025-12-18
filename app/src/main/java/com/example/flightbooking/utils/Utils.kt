package com.example.flightbooking.utils

import android.content.Context
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

object Utils {

    /**
     * Форматирует цену с разделителями тысяч
     */
    fun formatPrice(price: Double): String {
        return if (price >= 1000) {
            val formatted = (price / 1000).roundToInt()
            "$formatted 000 ₽"
        } else {
            "${price.roundToInt()} ₽"
        }
    }

    /**
     * Форматирует дату для отображения
     */
    fun formatDisplayDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
            val date = inputFormat.parse(dateString) ?: return dateString
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    /**
     * Получает название месяца по номеру
     */
    fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "Января"
            2 -> "Февраля"
            3 -> "Марта"
            4 -> "Апреля"
            5 -> "Мая"
            6 -> "Июня"
            7 -> "Июля"
            8 -> "Августа"
            9 -> "Сентября"
            10 -> "Октября"
            11 -> "Ноября"
            12 -> "Декабря"
            else -> ""
        }
    }

    /**
     * Проверяет, является ли дата сегодняшней
     */
    fun isToday(dateString: String): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(dateString)
            val today = Calendar.getInstance()
            val compareDate = Calendar.getInstance()
            date?.let { compareDate.time = it }

            today.get(Calendar.YEAR) == compareDate.get(Calendar.YEAR) &&
                    today.get(Calendar.MONTH) == compareDate.get(Calendar.MONTH) &&
                    today.get(Calendar.DAY_OF_MONTH) == compareDate.get(Calendar.DAY_OF_MONTH)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Получает день недели для даты
     */
    fun getDayOfWeek(dateString: String): String {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(dateString) ?: return ""
            val calendar = Calendar.getInstance()
            calendar.time = date

            when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Понедельник"
                Calendar.TUESDAY -> "Вторник"
                Calendar.WEDNESDAY -> "Среда"
                Calendar.THURSDAY -> "Четверг"
                Calendar.FRIDAY -> "Пятница"
                Calendar.SATURDAY -> "Суббота"
                Calendar.SUNDAY -> "Воскресенье"
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Показывает короткое уведомление
     */
    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Показывает длинное уведомление
     */
    fun showLongToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Рассчитывает время прибытия
     */
    fun calculateArrivalTime(departureTime: String, durationMinutes: Int): String {
        return try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val departure = timeFormat.parse(departureTime) ?: return ""

            val calendar = Calendar.getInstance()
            calendar.time = departure
            calendar.add(Calendar.MINUTE, durationMinutes)

            timeFormat.format(calendar.time)
        } catch (e: Exception) {
            ""
        }
    }
}