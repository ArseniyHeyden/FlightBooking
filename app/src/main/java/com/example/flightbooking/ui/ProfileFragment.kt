package com.example.flightbooking.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flightbooking.R
import com.example.flightbooking.utils.NotificationHelper

class ProfileFragment : Fragment() {
    private lateinit var viewModel: FlightViewModel
    private lateinit var ticketAdapter: TicketAdapter

    private lateinit var nameText: TextView
    private lateinit var emailText: TextView
    private lateinit var phoneText: TextView
    private lateinit var discountLevelText: TextView
    private lateinit var discountPercentText: TextView
    private lateinit var tripsCountText: TextView
    private lateinit var totalSpentText: TextView
    private lateinit var nextLevelText: TextView
    private lateinit var historyText: TextView
    private lateinit var noTicketsText: TextView
    private lateinit var ticketsRecycler: RecyclerView
    private lateinit var logoutButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nameText = view.findViewById(R.id.name_text)
        emailText = view.findViewById(R.id.email_text)
        phoneText = view.findViewById(R.id.phone_text)
        discountLevelText = view.findViewById(R.id.discount_level_text)
        discountPercentText = view.findViewById(R.id.discount_percent_text)
        tripsCountText = view.findViewById(R.id.trips_count_text)
        totalSpentText = view.findViewById(R.id.total_spent_text)
        nextLevelText = view.findViewById(R.id.next_level_text)
        historyText = view.findViewById(R.id.history_text)
        noTicketsText = view.findViewById(R.id.no_tickets_text)
        ticketsRecycler = view.findViewById(R.id.tickets_recycler)
        logoutButton = view.findViewById(R.id.logout_button)

        viewModel = ViewModelProvider(requireActivity())[FlightViewModel::class.java]

        setupRecyclerView()
        setupLogoutButton()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        ticketAdapter = TicketAdapter()
        ticketsRecycler.layoutManager = LinearLayoutManager(context)
        ticketsRecycler.adapter = ticketAdapter
    }

    private fun observeViewModel() {
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                nameText.text = user.name
                emailText.text = user.email
                phoneText.text = user.phone

                val levelName = viewModel.getDiscountLevelName(user.discountLevel)
                discountLevelText.text = "Статус: $levelName"

                val discount = when (user.discountLevel) {
                    0 -> "0%"
                    1 -> "5%"
                    2 -> "10%"
                    else -> "0%"
                }
                discountPercentText.text = "Скидка: $discount"

                tripsCountText.text = "Всего поездок: ${user.totalTrips}"
                totalSpentText.text = "Потрачено: ${String.format("%.0f", user.totalSpent)} ₽"

                val nextLevel = when (user.discountLevel) {
                    0 -> "Серебро (10 поездок)"
                    1 -> "Золото (20 поездок)"
                    else -> "Максимальный уровень"
                }
                nextLevelText.text = "Следующий уровень: $nextLevel"
            }
        }

        viewModel.getUserTickets().observe(viewLifecycleOwner) { tickets ->
            ticketAdapter.submitList(tickets)
            noTicketsText.visibility = if (tickets.isEmpty()) View.VISIBLE else View.GONE
            
            // Планируем уведомления для всех неоплаченных билетов
            if (tickets.isNotEmpty() && context != null) {
                val notificationHelper = NotificationHelper(requireContext())
                tickets.forEach { ticket ->
                    if (!ticket.isPaid) {
                        notificationHelper.scheduleFlightNotifications(ticket)
                    }
                }
            }
        }

        viewModel.searchHistory.observe(viewLifecycleOwner) { history ->
            if (history.isNotEmpty()) {
                // Убираем дубликаты одинаковых маршрутов, оставляя только последний поиск каждого маршрута
                val uniqueSearches = history
                    .distinctBy { "${it.fromCity.lowercase().trim()} → ${it.toCity.lowercase().trim()}" }
                    .take(3)
                
                val lastSearches = uniqueSearches.joinToString("\n") {
                    "${it.fromCity} → ${it.toCity}"
                }
                historyText.text = "Последние поиски:\n$lastSearches"
            } else {
                historyText.text = "История поиска пуста"
            }
        }
    }
    
    private fun setupLogoutButton() {
        logoutButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Выход из аккаунта")
                .setMessage("Вы действительно хотите выйти из аккаунта?")
                .setPositiveButton("Выйти") { _, _ ->
                    viewModel.logout()
                    Toast.makeText(context, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.loginFragment, true)
                        .build()
                    findNavController().navigate(R.id.loginFragment, null, navOptions)
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }
}