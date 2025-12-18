package com.example.flightbooking.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flightbooking.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FavoritesFragment : Fragment() {
    private lateinit var viewModel: FlightViewModel
    private lateinit var adapter: FlightAdapter
    private lateinit var favoritesRecycler: RecyclerView
    private lateinit var emptyStateText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        favoritesRecycler = view.findViewById(R.id.favorites_recycler)
        emptyStateText = view.findViewById(R.id.empty_state_text)

        viewModel = ViewModelProvider(requireActivity())[FlightViewModel::class.java]

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = FlightAdapter(
            onFlightClick = { flight ->
                showFavoriteFlightOptions(flight)
            },
            onFavoriteClick = { flight ->
                lifecycleScope.launch {
                    viewModel.toggleFavorite(flight.id)
                    Toast.makeText(
                        context,
                        "Удалено из избранного",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
        favoritesRecycler.layoutManager = LinearLayoutManager(context)
        favoritesRecycler.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.userFavorites.observe(viewLifecycleOwner) { flights ->
            if (flights.isNotEmpty()) {
                val flightsWithDetails = flights.map { flight ->
                    com.example.flightbooking.data.entity.FlightWithDetails(
                        flight = flight,
                        isFavorite = true
                    )
                }
                adapter.submitList(flightsWithDetails)
                emptyStateText.visibility = View.GONE
                favoritesRecycler.visibility = View.VISIBLE
            } else {
                adapter.submitList(emptyList())
                emptyStateText.visibility = View.VISIBLE
                favoritesRecycler.visibility = View.GONE
            }
        }
    }

    private fun showFavoriteFlightOptions(flight: com.example.flightbooking.data.entity.Flight) {
        AlertDialog.Builder(requireContext())
            .setTitle("${flight.fromCity} → ${flight.toCity}")
            .setMessage("Выберите действие для избранного рейса")
            .setPositiveButton("Забронировать") { _, _ ->
                showDatePickerForBooking(flight)
            }
            .setNegativeButton("Подробнее") { _, _ ->
                showFlightDetails(flight)
            }
            .setNeutralButton("Удалить из избранного") { _, _ ->
                lifecycleScope.launch {
                    viewModel.toggleFavorite(flight.id)
                    Toast.makeText(context, "Удалено из избранного", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showDatePickerForBooking(flight: com.example.flightbooking.data.entity.Flight) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        android.app.DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(selectedDate.time)

                // Переходим к бронированию
                viewModel.selectFlight(flight, dateStr)
                findNavController().navigate(R.id.action_favorites_to_booking)
            },
            year, month, day
        ).apply {
            datePicker.minDate = calendar.timeInMillis
            show()
        }
    }

    private fun showFlightDetails(flight: com.example.flightbooking.data.entity.Flight) {
        val message = """
            Авиакомпания: ${flight.airline}
            Маршрут: ${flight.fromCity} → ${flight.toCity}
            Время в пути: ${formatDuration(flight.duration)}
            Базовая цена: ${flight.basePrice} ₽
            
            Для бронирования выберите дату вылета.
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Детали рейса")
            .setMessage(message)
            .setPositiveButton("Выбрать дату") { _, _ ->
                showDatePickerForBooking(flight)
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return "${hours}ч ${mins}м"
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.userId > 0) {
            viewModel.loadFavorites()
        }
    }
}