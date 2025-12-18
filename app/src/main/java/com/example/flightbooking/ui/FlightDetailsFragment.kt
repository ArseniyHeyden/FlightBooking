package com.example.flightbooking.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.flightbooking.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FlightDetailsFragment : Fragment() {
    private lateinit var viewModel: FlightViewModel

    // Данные о рейсе
    private var flightId: Int = 0
    private lateinit var fromCity: String
    private lateinit var toCity: String
    private lateinit var airline: String
    private var price: Double = 0.0
    private var duration: Int = 0

    private lateinit var routeText: TextView
    private lateinit var airlineText: TextView
    private lateinit var durationText: TextView
    private lateinit var priceText: TextView
    private lateinit var favoriteButton: Button
    private lateinit var bookButton: Button

    companion object {
        private const val ARG_FLIGHT_ID = "flight_id"
        private const val ARG_FLIGHT_FROM = "flight_from"
        private const val ARG_FLIGHT_TO = "flight_to"
        private const val ARG_FLIGHT_AIRLINE = "airline"
        private const val ARG_FLIGHT_PRICE = "price"
        private const val ARG_FLIGHT_DURATION = "duration"

        fun newInstance(flightId: Int, from: String, to: String, airline: String, price: Double, duration: Int): FlightDetailsFragment {
            return FlightDetailsFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_FLIGHT_ID, flightId)
                    putString(ARG_FLIGHT_FROM, from)
                    putString(ARG_FLIGHT_TO, to)
                    putString(ARG_FLIGHT_AIRLINE, airline)
                    putDouble(ARG_FLIGHT_PRICE, price)
                    putInt(ARG_FLIGHT_DURATION, duration)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_flight_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем данные из аргументов
        flightId = arguments?.getInt(ARG_FLIGHT_ID) ?: 0
        fromCity = arguments?.getString(ARG_FLIGHT_FROM) ?: ""
        toCity = arguments?.getString(ARG_FLIGHT_TO) ?: ""
        airline = arguments?.getString(ARG_FLIGHT_AIRLINE) ?: ""
        price = arguments?.getDouble(ARG_FLIGHT_PRICE) ?: 0.0
        duration = arguments?.getInt(ARG_FLIGHT_DURATION) ?: 0

        routeText = view.findViewById(R.id.route_text)
        airlineText = view.findViewById(R.id.airline_text)
        durationText = view.findViewById(R.id.duration_text)
        priceText = view.findViewById(R.id.price_text)
        favoriteButton = view.findViewById(R.id.favorite_button)
        bookButton = view.findViewById(R.id.book_button)

        viewModel = ViewModelProvider(requireActivity())[FlightViewModel::class.java]

        displayFlightDetails()
        setupListeners()
        updateFavoriteButton()
    }

    private fun displayFlightDetails() {
        routeText.text = "$fromCity → $toCity"
        airlineText.text = airline
        durationText.text = "Время в пути: ${formatDuration(duration)}"
        priceText.text = String.format(Locale.getDefault(), "%.0f ₽", price)
    }

    private fun updateFavoriteButton() {
        favoriteButton.isEnabled = viewModel.userId > 0
        if (viewModel.userId == 0) {
            favoriteButton.text = "Войдите для добавления"
            return
        }

        lifecycleScope.launch {
            try {
                val isFavorite = isFlightFavorite()
                favoriteButton.text = if (isFavorite) "Удалить из избранного" else "В избранное"
            } catch (e: Exception) {
                favoriteButton.text = "Ошибка"
            }
        }
    }

    private fun isFlightFavorite(): Boolean {
        val favorites = viewModel.userFavorites.value ?: emptyList()
        return favorites.any { it.id == flightId }
    }

    private fun setupListeners() {
        favoriteButton.setOnClickListener {
            if (viewModel.userId == 0) {
                Toast.makeText(context, "Войдите для добавления", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                viewModel.toggleFavorite(flightId)
                updateFavoriteButton()
                val message = if (isFlightFavorite()) "Добавлено в избранное" else "Удалено из избранного"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }

        bookButton.setOnClickListener {
            if (viewModel.userId == 0) {
                Toast.makeText(context, "Войдите для бронирования", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Создаем объект Flight для передачи
            val flight = com.example.flightbooking.data.entity.Flight(
                id = flightId,
                fromCity = fromCity,
                toCity = toCity,
                basePrice = price,
                duration = duration,
                airline = airline
            )

            // Выбираем рейс для бронирования
            viewModel.selectFlight(flight, getCurrentDate())

            // Переходим к бронированию через Navigation Component
            try {
                // Пытаемся использовать навигацию этого фрагмента, если он часть графа
                findNavController().navigate(R.id.bookingFragment)
            } catch (e: IllegalStateException) {
                // Если фрагмент не часть графа навигации, получаем NavController через NavHostFragment
                val navHostFragment = requireActivity().supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                navHostFragment?.navController?.navigate(R.id.bookingFragment)
            }
        }
    }

    private fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return "${hours}ч ${mins}м"
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}