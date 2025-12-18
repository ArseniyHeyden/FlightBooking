package com.example.flightbooking.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.flightbooking.R
import com.example.flightbooking.data.entity.Ticket
import java.text.SimpleDateFormat
import java.util.*

class TicketAdapter : ListAdapter<Ticket, TicketAdapter.TicketViewHolder>(TicketDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ticket, parent, false)
        return TicketViewHolder(view)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ticketIdText: TextView = itemView.findViewById(R.id.ticket_id_text)
        private val departureDateText: TextView = itemView.findViewById(R.id.departure_date_text)
        private val returnDateText: TextView = itemView.findViewById(R.id.return_date_text)
        private val priceText: TextView = itemView.findViewById(R.id.price_text)
        private val statusText: TextView = itemView.findViewById(R.id.status_text)
        private val bookingDateText: TextView = itemView.findViewById(R.id.booking_date_text)

        // Добавляем форматтер для совместимости с API < 26
        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val outputFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        fun bind(ticket: Ticket) {
            // Сохраняем оригинальный текст
            ticketIdText.text = "Билет #${ticket.id}"
            departureDateText.text = "Вылет: ${formatDate(ticket.departureDate)}"

            if (ticket.returnDate != null) {
                returnDateText.text = "Возврат: ${formatDate(ticket.returnDate)}"
                returnDateText.visibility = View.VISIBLE
            } else {
                returnDateText.visibility = View.GONE
            }

            // Исправляем проблему с локалью
            priceText.text = "${String.format(Locale.getDefault(), "%.0f", ticket.finalPrice)} ₽"
            statusText.text = if (ticket.isPaid) "Оплачен" else "Забронирован"
            bookingDateText.text = "Дата бронирования: ${formatDate(ticket.bookingDate)}"
        }

        private fun formatDate(dateString: String): String {
            // Заменяем java.time на SimpleDateFormat с безопасным парсингом
            return try {
                val date = dateFormatter.parse(dateString)
                // Исправляем проблему с nullable Date
                if (date != null) {
                    outputFormatter.format(date)
                } else {
                    dateString // Если парсинг вернул null
                }
            } catch (e: Exception) {
                dateString // Если не удалось распарсить, возвращаем оригинальную строку
            }
        }
    }

    class TicketDiffCallback : DiffUtil.ItemCallback<Ticket>() {
        override fun areItemsTheSame(oldItem: Ticket, newItem: Ticket): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Ticket, newItem: Ticket): Boolean {
            return oldItem == newItem
        }
    }
}