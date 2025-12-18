package com.example.flightbooking.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.flightbooking.R
import com.example.flightbooking.data.SearchPreferences

class PassengerPreferencesFragment : Fragment() {
    private lateinit var viewModel: FlightViewModel

    private lateinit var adultCountText: TextView
    private lateinit var adultMinusBtn: Button
    private lateinit var adultPlusBtn: Button
    private lateinit var childCountText: TextView
    private lateinit var childMinusBtn: Button
    private lateinit var childPlusBtn: Button
    private lateinit var infantCountText: TextView
    private lateinit var infantMinusBtn: Button
    private lateinit var infantPlusBtn: Button
    private lateinit var seatClassSpinner: Spinner
    private lateinit var applyButton: Button
    private lateinit var totalPassengersText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_passenger_preferences, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[FlightViewModel::class.java]

        // Находим элементы
        adultCountText = view.findViewById(R.id.adult_count_text)
        adultMinusBtn = view.findViewById(R.id.adult_minus_btn)
        adultPlusBtn = view.findViewById(R.id.adult_plus_btn)
        childCountText = view.findViewById(R.id.child_count_text)
        childMinusBtn = view.findViewById(R.id.child_minus_btn)
        childPlusBtn = view.findViewById(R.id.child_plus_btn)
        infantCountText = view.findViewById(R.id.infant_count_text)
        infantMinusBtn = view.findViewById(R.id.infant_minus_btn)
        infantPlusBtn = view.findViewById(R.id.infant_plus_btn)
        seatClassSpinner = view.findViewById(R.id.seat_class_spinner)
        applyButton = view.findViewById(R.id.apply_button)
        totalPassengersText = view.findViewById(R.id.total_passengers_text)

        setupSeatClassSpinner()
        setupListeners()
        loadCurrentPreferences()
    }

    private fun setupSeatClassSpinner() {
        val seatClasses = arrayOf("Эконом", "Комфорт", "Бизнес")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            seatClasses
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        seatClassSpinner.adapter = adapter
    }

    private fun setupListeners() {
        // Управление количеством взрослых
        adultPlusBtn.setOnClickListener { updateAdultCount(1) }
        adultMinusBtn.setOnClickListener { updateAdultCount(-1) }

        // Управление количеством детей
        childPlusBtn.setOnClickListener { updateChildCount(1) }
        childMinusBtn.setOnClickListener { updateChildCount(-1) }

        // Управление количеством младенцев
        infantPlusBtn.setOnClickListener { updateInfantCount(1) }
        infantMinusBtn.setOnClickListener { updateInfantCount(-1) }

        // Применение настроек
        applyButton.setOnClickListener {
            savePreferences()
            Toast.makeText(context, "Настройки сохранены", Toast.LENGTH_SHORT).show()
            // Возвращаемся назад через Navigation Component
            findNavController().navigateUp()
        }
    }

    private fun updateAdultCount(delta: Int) {
        val current = adultCountText.text.toString().toIntOrNull() ?: 0
        val newCount = (current + delta).coerceAtLeast(0)
        adultCountText.text = newCount.toString()
        updateTotalPassengers()
    }

    private fun updateChildCount(delta: Int) {
        val current = childCountText.text.toString().toIntOrNull() ?: 0
        val newCount = (current + delta).coerceAtLeast(0)
        childCountText.text = newCount.toString()
        updateTotalPassengers()
    }

    private fun updateInfantCount(delta: Int) {
        val current = infantCountText.text.toString().toIntOrNull() ?: 0
        val newCount = (current + delta).coerceAtLeast(0)
        infantCountText.text = newCount.toString()
        updateTotalPassengers()
    }

    private fun updateTotalPassengers() {
        val adults = adultCountText.text.toString().toIntOrNull() ?: 0
        val children = childCountText.text.toString().toIntOrNull() ?: 0
        val infants = infantCountText.text.toString().toIntOrNull() ?: 0
        val total = adults + children + infants

        // Проверяем, что есть хотя бы один взрослый
        if (adults == 0 && total > 0) {
            Toast.makeText(context, "Должен быть хотя бы один взрослый", Toast.LENGTH_SHORT).show()
            adultCountText.text = "1"
            updateTotalPassengers()
            return
        }

        totalPassengersText.text = "Всего пассажиров: $total"
    }

    private fun loadCurrentPreferences() {
        val preferences = viewModel.searchPreferences.value ?: SearchPreferences()

        adultCountText.text = preferences.adultCount.toString()
        childCountText.text = preferences.childCount.toString()
        infantCountText.text = preferences.infantCount.toString()

        val seatClassPosition = when (preferences.seatClass) {
            "economy" -> 0
            "comfort" -> 1
            "business" -> 2
            else -> 0
        }
        seatClassSpinner.setSelection(seatClassPosition)

        updateTotalPassengers()
    }

    private fun savePreferences() {
        val adults = adultCountText.text.toString().toIntOrNull() ?: 0
        val children = childCountText.text.toString().toIntOrNull() ?: 0
        val infants = infantCountText.text.toString().toIntOrNull() ?: 0
        val seatClass = when (seatClassSpinner.selectedItemPosition) {
            0 -> "economy"
            1 -> "comfort"
            2 -> "business"
            else -> "economy"
        }

        val preferences = SearchPreferences(
            adultCount = adults,
            childCount = children,
            infantCount = infants,
            seatClass = seatClass
        ).copyWithDefaults()

        viewModel.updateSearchPreferences(preferences)
    }
}