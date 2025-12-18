package com.example.flightbooking.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flightbooking.R
import com.example.flightbooking.data.SearchPreferences
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SearchFragment : Fragment() {
    private lateinit var viewModel: FlightViewModel
    private lateinit var adapter: FlightAdapter
    private var selectedCalendar: Calendar? = null

    private lateinit var fromCityInput: AutoCompleteTextView
    private lateinit var toCityInput: AutoCompleteTextView
    private lateinit var dateButton: Button
    private lateinit var searchButton: Button
    private lateinit var flightsRecycler: RecyclerView
    private lateinit var passengerInfoButton: Button
    private lateinit var filterButton: Button
    private lateinit var preferencesInfoText: TextView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var hotDealsRecycler: RecyclerView
    private lateinit var hotDealsTitle: TextView
    private lateinit var hotDealsAdapter: FlightAdapter

    private val cities = listOf(
        "Москва", "Санкт-Петербург", "Сочи", "Казань",
        "Екатеринбург", "Новосибирск", "Владивосток",
        "Краснодар", "Калининград"
    )

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search_enhanced, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fromCityInput = view.findViewById(R.id.from_city_input)
        toCityInput = view.findViewById(R.id.to_city_input)
        dateButton = view.findViewById(R.id.date_button)
        searchButton = view.findViewById(R.id.search_button)
        flightsRecycler = view.findViewById(R.id.flights_recycler)
        passengerInfoButton = view.findViewById(R.id.passenger_info_button)
        filterButton = view.findViewById(R.id.filter_button)
        preferencesInfoText = view.findViewById(R.id.preferences_info_text)
        loadingIndicator = view.findViewById(R.id.loading_indicator)
        hotDealsRecycler = view.findViewById(R.id.hot_deals_recycler)
        hotDealsTitle = view.findViewById(R.id.hot_deals_title)

        viewModel = ViewModelProvider(requireActivity())[FlightViewModel::class.java]

        setupAdapters()
        setupRecyclerView()
        setupHotDealsRecyclerView()
        setupListeners()
        observeViewModel()
        updatePreferencesInfo()
        loadHotDeals()
    }

    override fun onResume() {
        super.onResume()
        updatePreferencesInfo()
    }

    private fun clearForm() {
        fromCityInput.text.clear()
        toCityInput.text.clear()
        dateButton.text = "Выбрать дату"
        selectedCalendar = null
        adapter.submitList(emptyList())
    }

    private fun setupAdapters() {
        val cityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            cities
        )
        fromCityInput.setAdapter(cityAdapter)
        toCityInput.setAdapter(cityAdapter)
    }

    private fun setupHotDealsRecyclerView() {
        hotDealsAdapter = FlightAdapter(
            onFlightClick = { flight ->
                if (selectedCalendar == null) {
                    Toast.makeText(context, "Выберите дату вылета", Toast.LENGTH_SHORT).show()
                    return@FlightAdapter
                }
                showFlightOptionsDialog(flight, dateFormat.format(selectedCalendar!!.time))
            },
            onFavoriteClick = { flight ->
                lifecycleScope.launch {
                    viewModel.toggleFavorite(flight.id)
                }
            },
            allFlights = emptyList(),
            getPriceText = { flight ->
                val prefs = viewModel.searchPreferences.value ?: SearchPreferences()
                val user = viewModel.currentUser.value
                val discountLevel = user?.discountLevel ?: 0
                val dateForPrice = selectedCalendar?.let { cal ->
                    java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                } ?: getCurrentDate()
                
                val pricePerAdult = viewModel.calculatePrice(
                    basePrice = flight.basePrice,
                    date = dateForPrice,
                    hasReturn = false,
                    discountLevel = discountLevel,
                    seatClass = prefs.seatClass,
                    isHotDeal = flight.isHotDeal,
                    hotDealDiscount = flight.hotDealDiscount,
                    seatPriceModifier = 1.0
                )
                
                val totalCoeff = prefs.adultCount * 1.0 + prefs.childCount * 0.75 + prefs.infantCount * 0.1
                val totalPrice = pricePerAdult * totalCoeff
                String.format(Locale.getDefault(), "%.0f ₽", totalPrice)
            }
        )
        hotDealsRecycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        hotDealsRecycler.adapter = hotDealsAdapter
    }
    
    private fun setupRecyclerView() {
        adapter = FlightAdapter(
            onFlightClick = { flight ->
                if (selectedCalendar == null) {
                    Toast.makeText(context, "Выберите дату вылета", Toast.LENGTH_SHORT).show()
                    return@FlightAdapter
                }

                // Показываем диалог с выбором действий
                showFlightOptionsDialog(flight, dateFormat.format(selectedCalendar!!.time))
            },
            onFavoriteClick = { flight ->
                lifecycleScope.launch {
                    viewModel.toggleFavorite(flight.id)
                }
            },
            getPriceText = { flight ->
                val prefs = viewModel.searchPreferences.value ?: SearchPreferences()
                val user = viewModel.currentUser.value
                val discountLevel = user?.discountLevel ?: 0

                // Дата для расчёта: выбранная или сегодняшняя
                val dateForPrice = selectedCalendar?.let { cal ->
                    java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                } ?: getCurrentDate()

                // Цена за взрослого с учётом даты, класса, скидки, горячего билета
                val pricePerAdult = viewModel.calculatePrice(
                    basePrice = flight.basePrice,
                    date = dateForPrice,
                    hasReturn = false,
                    discountLevel = discountLevel,
                    seatClass = prefs.seatClass,
                    isHotDeal = flight.isHotDeal,
                    hotDealDiscount = flight.hotDealDiscount,
                    seatPriceModifier = 1.0 // Базовый модификатор, выбранное место будет учтено при бронировании
                )

                // Общий коэффициент по количеству взрослых/детей/младенцев
                val totalCoeff =
                    prefs.adultCount * 1.0 +
                            prefs.childCount * 0.75 +
                            prefs.infantCount * 0.1

                val totalPrice = pricePerAdult * totalCoeff
                String.format(Locale.getDefault(), "%.0f ₽", totalPrice)
            }
        )
        flightsRecycler.layoutManager = LinearLayoutManager(context)
        flightsRecycler.adapter = adapter
    }

    private fun setupListeners() {
        dateButton.setOnClickListener {
            showDatePicker()
        }

        searchButton.setOnClickListener {
            performSearch()
        }

        passengerInfoButton.setOnClickListener {
            // Переходим к настройке пассажиров через Navigation Component
            findNavController().navigate(R.id.action_search_to_passengerPreferences)
        }

        filterButton.setOnClickListener {
            // Переходим к фильтрам (можно создать отдельный фрагмент)
            showFilterDialog()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedCalendar = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }
                dateButton.text = dateFormat.format(selectedCalendar!!.time)
            },
            year, month, day
        ).apply {
            datePicker.minDate = calendar.timeInMillis
            show()
        }
    }

    private fun performSearch() {
        val from = fromCityInput.text.toString().trim()
        val to = toCityInput.text.toString().trim()

        if (from.isEmpty() || to.isEmpty()) {
            Toast.makeText(context, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        if (from == to) {
            Toast.makeText(context, "Города должны различаться", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedCalendar == null) {
            Toast.makeText(context, "Выберите дату вылета", Toast.LENGTH_SHORT).show()
            return
        }

        loadingIndicator.visibility = View.VISIBLE

        lifecycleScope.launch {
            viewModel.searchFlights(from, to)
            loadingIndicator.visibility = View.GONE
        }
    }

    private fun loadHotDeals() {
        lifecycleScope.launch {
            val hotDeals = viewModel.getHotDeals(5)
            if (hotDeals.isNotEmpty()) {
                val hotDealsWithDetails = hotDeals.map { flight ->
                    val isFav = viewModel.userId > 0 && viewModel.isFavorite(flight.id)
                    com.example.flightbooking.data.entity.FlightWithDetails(flight, isFav)
                }
                hotDealsAdapter.submitList(hotDealsWithDetails)
                hotDealsTitle.visibility = View.VISIBLE
                hotDealsRecycler.visibility = View.VISIBLE
            } else {
                hotDealsTitle.visibility = View.GONE
                hotDealsRecycler.visibility = View.GONE
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.searchResults.observe(viewLifecycleOwner) { flights ->
            if (flights.isEmpty()) {
                Toast.makeText(context, "Рейсы не найдены", Toast.LENGTH_SHORT).show()
            }
            // Обновляем адаптер с новым списком для определения лучших предложений
            adapter = FlightAdapter(
                onFlightClick = { flight ->
                    if (selectedCalendar != null) {
                        val dateString = viewModel.getDateFromPicker(
                            selectedCalendar!!.get(Calendar.YEAR),
                            selectedCalendar!!.get(Calendar.MONTH),
                            selectedCalendar!!.get(Calendar.DAY_OF_MONTH)
                        )
                        viewModel.selectFlight(flight, dateString)
                        findNavController().navigate(R.id.action_search_to_booking)
                    } else {
                        Toast.makeText(context, "Выберите дату вылета", Toast.LENGTH_SHORT).show()
                    }
                },
                onFavoriteClick = { flight ->
                    lifecycleScope.launch {
                        viewModel.toggleFavorite(flight.id)
                    }
                },
                getPriceText = { flight ->
                    val date = selectedCalendar?.let { cal ->
                        viewModel.getDateFromPicker(
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        )
                    } ?: return@FlightAdapter "${String.format("%.0f", flight.basePrice)} ₽"

                    viewModel.currentUser.value?.let { user ->
                        val finalPrice = viewModel.calculatePrice(
                            basePrice = flight.basePrice,
                            date = date,
                            hasReturn = false,
                            discountLevel = user.discountLevel,
                            seatClass = "economy",
                            isHotDeal = flight.isHotDeal,
                            hotDealDiscount = flight.hotDealDiscount,
                            seatPriceModifier = 1.0
                        )
                        String.format("%.0f", finalPrice)
                    } ?: "${String.format("%.0f", flight.basePrice)} ₽"
                },
                allFlights = flights
            )
            flightsRecycler.adapter = adapter
            adapter.submitList(flights)
        }

        viewModel.searchPreferences.observe(viewLifecycleOwner) {
            updatePreferencesInfo()
        }
    }

    private fun updatePreferencesInfo() {
        val preferences = viewModel.searchPreferences.value ?: return

        val seatClass = when (preferences.seatClass) {
            "economy" -> "Эконом"
            "comfort" -> "Комфорт"
            "business" -> "Бизнес"
            else -> "Эконом"
        }

        val info = """
            Пассажиры: ${preferences.passengerCount}
            Взрослых: ${preferences.adultCount}
            Детей: ${preferences.childCount}
            Младенцев: ${preferences.infantCount}
            Класс: $seatClass
        """.trimIndent()

        preferencesInfoText.text = info
    }

    private fun showFlightOptionsDialog(flight: com.example.flightbooking.data.entity.Flight, date: String) {
        val options = arrayOf("Бронировать", "Детали рейса", "Добавить в избранное")

        AlertDialog.Builder(requireContext())
            .setTitle("${flight.fromCity} → ${flight.toCity}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        viewModel.selectFlight(flight, date)
                        findNavController().navigate(R.id.action_search_to_booking)
                    }
                    1 -> {
                        // Показываем детали рейса
                        showFlightDetailsDialog(flight)
                    }
                    2 -> {
                        lifecycleScope.launch {
                            viewModel.toggleFavorite(flight.id)
                            Toast.makeText(
                                context,
                                if (viewModel.isFavorite(flight.id)) "Добавлено в избранное"
                                else "Удалено из избранного",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showFlightDetailsDialog(flight: com.example.flightbooking.data.entity.Flight) {
        val message = """
            Авиакомпания: ${flight.airline}
            Маршрут: ${flight.fromCity} → ${flight.toCity}
            Время в пути: ${formatDuration(flight.duration)}
            Базовая цена: ${flight.basePrice} ₽
            
            Цена за пассажира:
            - Взрослый: ${calculatePassengerPrice(flight, "adult")} ₽
            - Ребенок: ${calculatePassengerPrice(flight, "child")} ₽
            - Младенец: ${calculatePassengerPrice(flight, "infant")} ₽
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Детали рейса")
            .setMessage(message)
            .setPositiveButton("Забронировать") { _, _ ->
                if (selectedCalendar != null) {
                    viewModel.selectFlight(flight, dateFormat.format(selectedCalendar!!.time))
                    findNavController().navigate(R.id.action_search_to_booking)
                } else {
                    Toast.makeText(context, "Выберите дату вылета", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun calculatePassengerPrice(flight: com.example.flightbooking.data.entity.Flight, passengerType: String): String {
        val preferences = viewModel.searchPreferences.value ?: return "0"
        val date = selectedCalendar?.let { dateFormat.format(it.time) } ?: getCurrentDate()

        val price = viewModel.calculatePrice(
            basePrice = flight.basePrice,
            date = date,
            hasReturn = false,
            discountLevel = 0,
            seatClass = preferences.seatClass,
            isHotDeal = flight.isHotDeal,
            hotDealDiscount = flight.hotDealDiscount,
            seatPriceModifier = 1.0
        )

        val coefficient = when (passengerType) {
            "adult" -> 1.0
            "child" -> 0.75
            "infant" -> 0.1
            else -> 1.0
        }

        return String.format(Locale.getDefault(), "%.0f", price * coefficient)
    }

    private fun showFilterDialog() {
        // Создаем диалог с фильтрами
        val dialogView = layoutInflater.inflate(R.layout.dialog_filters, null)

        val minPriceInput = dialogView.findViewById<EditText>(R.id.min_price_input)
        val maxPriceInput = dialogView.findViewById<EditText>(R.id.max_price_input)
        val maxDurationInput = dialogView.findViewById<EditText>(R.id.max_duration_input)
        val airlinesCheckboxGroup = dialogView.findViewById<LinearLayout>(R.id.airlines_group)
        val sortSpinner = dialogView.findViewById<Spinner>(R.id.sort_spinner)

        // Заполняем текущие настройки
        val preferences = viewModel.searchPreferences.value ?: SearchPreferences()
        minPriceInput.setText(preferences.minPrice?.toInt()?.toString() ?: "")
        maxPriceInput.setText(preferences.maxPrice?.toInt()?.toString() ?: "")
        maxDurationInput.setText(preferences.maxDuration?.toString() ?: "")

        // Настраиваем сортировку
        val sortOptions = arrayOf("Цена (сначала дешевые)", "Цена (сначала дорогие)", "Время (сначала короткие)")
        val sortAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions)
        sortSpinner.adapter = sortAdapter
        sortSpinner.setSelection(when (preferences.sortBy) {
            "price_desc" -> 1
            "duration" -> 2
            else -> 0
        })

        // Заполняем авиакомпании
        val airlines = viewModel.getAvailableAirlines()
        airlines.forEach { airline ->
            val checkbox = CheckBox(requireContext())
            checkbox.text = airline
            checkbox.isChecked = preferences.selectedAirlines.contains(airline)
            airlinesCheckboxGroup.addView(checkbox)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Фильтры поиска")
            .setView(dialogView)
            .setPositiveButton("Применить") { _, _ ->
                applyFilters(
                    minPriceInput.text.toString(),
                    maxPriceInput.text.toString(),
                    maxDurationInput.text.toString(),
                    airlinesCheckboxGroup,
                    sortSpinner.selectedItemPosition
                )
            }
            .setNegativeButton("Сбросить") { _, _ ->
                resetFilters()
            }
            .setNeutralButton("Отмена", null)
            .show()
    }

    private fun applyFilters(
        minPriceStr: String,
        maxPriceStr: String,
        maxDurationStr: String,
        airlinesGroup: LinearLayout,
        sortPosition: Int
    ) {
        val minPrice = minPriceStr.toDoubleOrNull()
        val maxPrice = maxPriceStr.toDoubleOrNull()
        val maxDuration = maxDurationStr.toIntOrNull()

        val selectedAirlines = mutableListOf<String>()
        for (i in 0 until airlinesGroup.childCount) {
            val checkbox = airlinesGroup.getChildAt(i) as CheckBox
            if (checkbox.isChecked) {
                selectedAirlines.add(checkbox.text.toString())
            }
        }

        val sortBy = when (sortPosition) {
            0 -> "price_asc"
            1 -> "price_desc"
            2 -> "duration"
            else -> "price_asc"
        }

        val currentPreferences = viewModel.searchPreferences.value ?: SearchPreferences()
        val newPreferences = currentPreferences.copy(
            minPrice = minPrice,
            maxPrice = maxPrice,
            maxDuration = maxDuration,
            selectedAirlines = selectedAirlines,
            sortBy = sortBy
        )

        viewModel.updateSearchPreferences(newPreferences)
        Toast.makeText(context, "Фильтры применены", Toast.LENGTH_SHORT).show()
    }

    private fun resetFilters() {
        val currentPreferences = viewModel.searchPreferences.value ?: SearchPreferences()
        val newPreferences = currentPreferences.copy(
            minPrice = null,
            maxPrice = null,
            maxDuration = null,
            selectedAirlines = emptyList(),
            sortBy = "price_asc"
        )

        viewModel.updateSearchPreferences(newPreferences)
        Toast.makeText(context, "Фильтры сброшены", Toast.LENGTH_SHORT).show()
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return "${hours}ч ${mins}м"
    }
}