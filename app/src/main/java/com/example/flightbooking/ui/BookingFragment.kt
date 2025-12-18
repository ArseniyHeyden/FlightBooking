package com.example.flightbooking.ui

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
import com.example.flightbooking.R
import com.example.flightbooking.data.SearchPreferences
import com.example.flightbooking.data.entity.Passenger
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BookingFragment : Fragment() {
    private lateinit var viewModel: FlightViewModel

    private lateinit var routeText: TextView
    private lateinit var airlineText: TextView
    private lateinit var durationText: TextView
    private lateinit var departureDateText: TextView
    private lateinit var returnCheckbox: CheckBox
    private lateinit var returnDateButton: Button
    private lateinit var returnDateText: TextView
    private lateinit var passengerNameInput: EditText
    private lateinit var passengerDocumentInput: EditText
    private lateinit var passengersExtraContainer: LinearLayout
    private lateinit var seatClassSpinner: Spinner
    private lateinit var discountLevelText: TextView
    private lateinit var priceText: TextView
    private lateinit var selectedSeatText: TextView
    private lateinit var selectSeatButton: Button
    private lateinit var bookButton: Button

    private val passengerFields = mutableListOf<Pair<EditText, EditText>>()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private var departureDateString: String? = null
    private var returnDateString: String? = null
    private var needsReturn = false
    private var selectedSeatClass = "economy"
    private var selectedSeatNumber: String? = null
    private var selectedSeatPriceModifier: Double = 1.0
    private val passengerSeats = mutableMapOf<Int, Pair<String, Double>>() // passengerNumber -> (seatNumber, priceModifier)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_booking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        routeText = view.findViewById(R.id.route_text)
        airlineText = view.findViewById(R.id.airline_text)
        durationText = view.findViewById(R.id.duration_text)
        departureDateText = view.findViewById(R.id.departure_date_text)
        returnCheckbox = view.findViewById(R.id.return_checkbox)
        returnDateButton = view.findViewById(R.id.return_date_button)
        returnDateText = view.findViewById(R.id.return_date_text)
        passengerNameInput = view.findViewById(R.id.passenger_name_input)
        passengerDocumentInput = view.findViewById(R.id.passenger_document_input)
        passengersExtraContainer = view.findViewById(R.id.passengers_extra_container)
        seatClassSpinner = view.findViewById(R.id.seat_class_spinner)
        discountLevelText = view.findViewById(R.id.discount_level_text)
        priceText = view.findViewById(R.id.price_text)
        selectedSeatText = view.findViewById(R.id.selected_seat_text)
        selectSeatButton = view.findViewById(R.id.select_seat_button)
        bookButton = view.findViewById(R.id.book_button)

        viewModel = ViewModelProvider(requireActivity())[FlightViewModel::class.java]

        setupSeatClassSpinner()
        observeViewModel()
        setupListeners()
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

        seatClassSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSeatClass = when (position) {
                    0 -> "economy"
                    1 -> "comfort"
                    2 -> "business"
                    else -> "economy"
                }
                updatePriceFromFlight()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedSeatClass = "economy"
            }
        }
    }

    private fun observeViewModel() {
        viewModel.selectedFlight.observe(viewLifecycleOwner) { (flight, date) ->
            departureDateString = date

            routeText.text = "${flight.fromCity} → ${flight.toCity}"
            airlineText.text = flight.airline
            durationText.text = formatDuration(flight.duration)
            departureDateText.text = "Вылет: ${formatDateForDisplay(date)}"

            renderPassengerFields(viewModel.searchPreferences.value ?: SearchPreferences())
            updatePrice(flight.basePrice)
        }

        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                discountLevelText.text =
                    "Ваш уровень: ${viewModel.getDiscountLevelName(user.discountLevel)}"

                // Автозаполняем имя пассажира из профиля пользователя
                passengerNameInput.setText(user.name)
                
                // Обновляем цену при изменении пользователя (может измениться уровень скидки)
                viewModel.selectedFlight.value?.let { (flight, _) ->
                    updatePrice(flight.basePrice)
                }
                
                // Обновляем подсказку для документа
                updateDocumentHint()
            }
        }

        // Обновляем цену и поля при изменении настроек пассажиров
        viewModel.searchPreferences.observe(viewLifecycleOwner) { prefs ->
            renderPassengerFields(prefs)
            viewModel.selectedFlight.value?.let { (flight, _) ->
                updatePrice(flight.basePrice)
            }
            updateDocumentHint()
            updateSelectedSeatsDisplay()
        }
    }
    
    private fun updateSelectedSeatsDisplay() {
        val passengers = buildPassengersForPrice()
        if (passengers.isEmpty()) {
            selectedSeatText.text = "Место не выбрано"
            return
        }
        
        if (passengerSeats.isEmpty()) {
            selectedSeatText.text = "Место не выбрано"
            return
        }
        
        val seatsText = if (passengers.size == 1) {
            val seatInfo = passengerSeats[1]
            if (seatInfo != null) {
                "Выбрано место: ${seatInfo.first}"
            } else {
                "Место не выбрано"
            }
        } else {
            val seatsList = passengers.mapIndexedNotNull { index, passenger ->
                val passengerNumber = index + 1
                val seatInfo = passengerSeats[passengerNumber]
                if (seatInfo != null) {
                    "${passenger.name}: ${seatInfo.first}"
                } else {
                    null
                }
            }
            
            if (seatsList.size == passengers.size) {
                "Выбраны места:\n${seatsList.joinToString("\n")}"
            } else {
                val selectedCount = seatsList.size
                "Выбрано мест: $selectedCount из ${passengers.size}\n${seatsList.joinToString("\n")}"
            }
        }
        
        selectedSeatText.text = seatsText
    }

    private fun setupListeners() {
        returnCheckbox.setOnCheckedChangeListener { _, isChecked ->
            needsReturn = isChecked
            returnDateButton.visibility = if (isChecked) View.VISIBLE else View.GONE
            returnDateText.visibility = if (isChecked && returnDateString != null) View.VISIBLE else View.GONE
            updatePriceFromFlight()
        }

        returnDateButton.setOnClickListener {
            showReturnDatePicker()
        }
        
        selectSeatButton.setOnClickListener {
            val flight = viewModel.selectedFlight.value?.first ?: run {
                Toast.makeText(context, "Рейс не выбран", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val passengers = buildPassengersForPrice()
            if (passengers.isEmpty()) {
                Toast.makeText(context, "Добавьте пассажиров в настройках", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Если пассажир один - сразу открываем выбор места
            if (passengers.size == 1) {
                openSeatSelection(flight.id, 1)
            } else {
                // Если пассажиров несколько - показываем диалог выбора пассажира
                showPassengerSelectionDialog(flight.id, passengers)
            }
        }

        bookButton.setOnClickListener {
            bookFlight()
        }
        
        // Проверяем, было ли выбрано место из SeatSelectionFragment
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("selectedSeat")?.observe(viewLifecycleOwner) { seatNumber ->
            seatNumber?.let {
                val passengerNumber = findNavController().previousBackStackEntry?.savedStateHandle?.get<Int>("currentPassengerNumber") 
                    ?: findNavController().currentBackStackEntry?.savedStateHandle?.get<Int>("passengerNumber") 
                    ?: 1
                
                // Получаем модификатор цены
                val modifier = findNavController().currentBackStackEntry?.savedStateHandle?.get<Double>("seatPriceModifier") ?: 1.0
                
                // Сохраняем место для конкретного пассажира
                passengerSeats[passengerNumber] = Pair(seatNumber, modifier)
                
                // Обновляем отображение выбранных мест
                updateSelectedSeatsDisplay()
                updatePriceFromFlight()
            }
        }
    }

    private fun showReturnDatePicker() {
        val calendar = Calendar.getInstance()

        // Устанавливаем минимальную дату (день после вылета)
        departureDateString?.let { date ->
            try {
                val depDate = dateFormat.parse(date)
                if (depDate != null) {
                    calendar.time = depDate
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
            } catch (e: Exception) {
                // Если не удалось распарсить дату, используем завтрашний день
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        } ?: run {
            // Если дата вылета не установлена, используем завтрашний день
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        val minYear = calendar.get(Calendar.YEAR)
        val minMonth = calendar.get(Calendar.MONTH)
        val minDay = calendar.get(Calendar.DAY_OF_MONTH)

        // Сбрасываем календарь на текущую минимальную дату для отображения
        calendar.set(minYear, minMonth, minDay)

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                returnDateString = dateFormat.format(
                    Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, selectedDay)
                    }.time
                )
                returnDateText.text = "Возврат: ${formatDateForDisplay(returnDateString!!)}"
                returnDateText.visibility = View.VISIBLE
                updatePriceFromFlight()
            },
            minYear,
            minMonth,
            minDay
        ).apply {
            datePicker.minDate = calendar.timeInMillis
            show()
        }
    }

    private fun updatePriceFromFlight() {
        viewModel.selectedFlight.value?.let { (flight, _) ->
            updatePrice(flight.basePrice)
        }
    }

    private fun updatePrice(basePrice: Double) {
        departureDateString?.let { date ->
            viewModel.currentUser.value?.let { user ->
                // Получаем список пассажиров для расчета итоговой цены
                val passengers = buildPassengersForPrice()

                if (passengers.isNotEmpty()) {
                    // Рассчитываем итоговую цену за всех пассажиров с учетом модификаторов места для каждого
                    val flight = viewModel.selectedFlight.value?.first
                    var totalPrice = 0.0
                    passengers.forEachIndexed { index, passenger ->
                        val passengerNumber = index + 1
                        val seatModifier = passengerSeats[passengerNumber]?.second ?: selectedSeatPriceModifier
                        val basePricePerPassenger = viewModel.calculatePrice(
                            basePrice = basePrice,
                            date = date,
                            hasReturn = needsReturn,
                            discountLevel = user.discountLevel,
                            seatClass = selectedSeatClass,
                            isHotDeal = flight?.isHotDeal ?: false,
                            hotDealDiscount = flight?.hotDealDiscount ?: 0,
                            seatPriceModifier = seatModifier
                        )
                        val coefficient = Passenger.getPriceCoefficient(passenger.passengerType)
                        totalPrice += basePricePerPassenger * coefficient
                    }
                    priceText.text = String.format(Locale.getDefault(), "%.2f ₽", totalPrice)
                } else {
                    // Если пассажиров нет, показываем цену за одного взрослого
                    val flight = viewModel.selectedFlight.value?.first
                    val finalPrice = viewModel.calculatePrice(
                        basePrice = basePrice,
                        date = date,
                        hasReturn = needsReturn,
                        discountLevel = user.discountLevel,
                        seatClass = selectedSeatClass,
                        isHotDeal = flight?.isHotDeal ?: false,
                        hotDealDiscount = flight?.hotDealDiscount ?: 0,
                        seatPriceModifier = selectedSeatPriceModifier
                    )
                    priceText.text = String.format(Locale.getDefault(), "%.2f ₽", finalPrice)
                }
            }
        }
    }

    private fun bookFlight() {
        val flight = viewModel.selectedFlight.value?.first ?: return
        val depDate = departureDateString ?: return

        if (needsReturn && returnDateString == null) {
            Toast.makeText(context, "Выберите дату возврата", Toast.LENGTH_SHORT).show()
            return
        }

        // Получаем данные всех пассажиров с документами
        val passengersWithDocs = collectPassengersWithDocs()
        if (passengersWithDocs.isEmpty()) {
            return
        }

        viewModel.currentUser.value?.let { user ->
            val passengers = passengersWithDocs.map { it.first }
            // Рассчитываем общую цену с учетом выбранных мест для каждого пассажира
            var totalPrice = 0.0
            passengers.forEachIndexed { index, passenger ->
                val passengerNumber = index + 1
                val seatModifier = passengerSeats[passengerNumber]?.second ?: selectedSeatPriceModifier
                val basePricePerPassenger = viewModel.calculatePrice(
                    basePrice = flight.basePrice,
                    date = depDate,
                    hasReturn = needsReturn,
                    discountLevel = user.discountLevel,
                    seatClass = selectedSeatClass,
                    isHotDeal = flight.isHotDeal,
                    hotDealDiscount = flight.hotDealDiscount,
                    seatPriceModifier = seatModifier
                )
                val coefficient = Passenger.getPriceCoefficient(passenger.passengerType)
                totalPrice += basePricePerPassenger * coefficient
            }

            lifecycleScope.launch {
                // Бронируем для каждого пассажира и сохраняем идентификаторы билетов
                val ticketIds = mutableListOf<Long>()
                passengersWithDocs.forEachIndexed { index, (passenger, document) ->
                    val passengerNumber = index + 1
                    // Используем выбранное место для этого пассажира или генерируем случайное
                    val seatInfo = passengerSeats[passengerNumber]
                    val seatNumber = seatInfo?.first ?: generateSeatNumber(passenger)
                    
                    // Рассчитываем цену с учетом модификатора места для этого пассажира
                    val seatModifier = seatInfo?.second ?: 1.0
                    val basePricePerPassenger = viewModel.calculatePrice(
                        basePrice = flight.basePrice,
                        date = depDate,
                        hasReturn = needsReturn,
                        discountLevel = user.discountLevel,
                        seatClass = selectedSeatClass,
                        isHotDeal = flight.isHotDeal,
                        hotDealDiscount = flight.hotDealDiscount,
                        seatPriceModifier = seatModifier
                    )
                    val passengerCoefficient = Passenger.getPriceCoefficient(passenger.passengerType)
                    val passengerPrice = basePricePerPassenger * passengerCoefficient
                    
                    val ticketId = viewModel.bookTicket(
                        flightId = flight.id,
                        departureDate = depDate,
                        returnDate = returnDateString,
                        passengerName = passenger.name,
                        passengerDocument = document,
                        finalPrice = passengerPrice,
                        seatNumber = seatNumber
                    )

                    if (ticketId > 0) {
                        ticketIds.add(ticketId)
                    }
                }

                if (ticketIds.isNotEmpty()) {
                    showPaymentDialog(ticketIds, totalPrice)
                } else {
                    Toast.makeText(context, "Ошибка при бронировании", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun buildPassengersForPrice(): List<Passenger> {
        val preferences = viewModel.searchPreferences.value ?: return emptyList()
        val passengers = mutableListOf<Passenger>()
        var passengerNumber = 1

        repeat(preferences.adultCount) {
            val name = passengerFields.getOrNull(passengerNumber - 1)?.first?.text?.toString()
                ?.takeIf { it.isNotBlank() } ?: "Пассажир $passengerNumber"
            passengers.add(
                Passenger(
                    passengerNumber = passengerNumber++,
                    name = name,
                    age = 30,
                    passengerType = Passenger.TYPE_ADULT,
                    seatClass = preferences.seatClass
                )
            )
        }

        repeat(preferences.childCount) {
            val name = passengerFields.getOrNull(passengerNumber - 1)?.first?.text?.toString()
                ?.takeIf { it.isNotBlank() } ?: "Пассажир $passengerNumber"
            passengers.add(
                Passenger(
                    passengerNumber = passengerNumber++,
                    name = name,
                    age = 7,
                    passengerType = Passenger.TYPE_CHILD,
                    seatClass = preferences.seatClass
                )
            )
        }

        repeat(preferences.infantCount) {
            val name = passengerFields.getOrNull(passengerNumber - 1)?.first?.text?.toString()
                ?.takeIf { it.isNotBlank() } ?: "Пассажир $passengerNumber"
            passengers.add(
                Passenger(
                    passengerNumber = passengerNumber++,
                    name = name,
                    age = 1,
                    passengerType = Passenger.TYPE_INFANT,
                    seatClass = preferences.seatClass
                )
            )
        }

        return passengers
    }

    private fun collectPassengersWithDocs(): List<Pair<Passenger, String>> {
        val preferences = viewModel.searchPreferences.value ?: return emptyList()
        val passengers = mutableListOf<Pair<Passenger, String>>()
        var passengerNumber = 1

        fun readNameDoc(index: Int): Pair<String, String> {
            val (nameField, docField) = passengerFields.getOrNull(index)
                ?: (passengerNameInput to passengerDocumentInput)
            val name = nameField.text.toString().ifBlank { "Пассажир ${index + 1}" }
            val doc = docField.text.toString().trim()
            return name to doc
        }

        // Взрослые
        repeat(preferences.adultCount) { idx ->
            val (name, doc) = readNameDoc(passengerNumber - 1)
            val age = 30
            if (!validatePassengerDocument(doc, age)) {
                Toast.makeText(
                    context,
                    "Для пассажира $passengerNumber требуется паспорт (10 цифр)",
                    Toast.LENGTH_LONG
                ).show()
                return emptyList()
            }
            passengers.add(
                Passenger(
                    passengerNumber = passengerNumber++,
                    name = name,
                    age = age,
                    passengerType = Passenger.TYPE_ADULT,
                    seatClass = preferences.seatClass
                ) to doc
            )
        }

        // Дети
        repeat(preferences.childCount) { idx ->
            val (name, doc) = readNameDoc(passengerNumber - 1)
            val age = 7
            if (!validatePassengerDocument(doc, age)) {
                Toast.makeText(
                    context,
                    "Для пассажира $passengerNumber требуется свидетельство о рождении (10 символов)",
                    Toast.LENGTH_LONG
                ).show()
                return emptyList()
            }
            passengers.add(
                Passenger(
                    passengerNumber = passengerNumber++,
                    name = name,
                    age = age,
                    passengerType = Passenger.TYPE_CHILD,
                    seatClass = preferences.seatClass
                ) to doc
            )
        }

        // Младенцы
        repeat(preferences.infantCount) { idx ->
            val (name, doc) = readNameDoc(passengerNumber - 1)
            val age = 1
            if (!validatePassengerDocument(doc, age)) {
                Toast.makeText(
                    context,
                    "Для пассажира $passengerNumber требуется свидетельство о рождении (10 символов)",
                    Toast.LENGTH_LONG
                ).show()
                return emptyList()
            }
            passengers.add(
                Passenger(
                    passengerNumber = passengerNumber++,
                    name = name,
                    age = age,
                    passengerType = Passenger.TYPE_INFANT,
                    seatClass = preferences.seatClass
                ) to doc
            )
        }

        return passengers
    }

    private fun validatePassengerDocument(document: String, age: Int): Boolean {
        // Документ должен быть ровно 10 символов
        if (document.length != 10) {
            return false
        }

        return if (age >= 14) {
            // Паспорт: только цифры
            document.all { it.isDigit() }
        } else {
            // Свидетельство о рождении: буквы или цифры
            document.all { it.isLetterOrDigit() }
        }
    }

    private fun openSeatSelection(flightId: Int, passengerNumber: Int) {
        val bundle = Bundle().apply {
            putInt("flightId", flightId)
            putInt("passengerNumber", passengerNumber)
        }
        findNavController().previousBackStackEntry?.savedStateHandle?.set("currentPassengerNumber", passengerNumber)
        findNavController().navigate(R.id.action_booking_to_seat_selection, bundle)
    }
    
    private fun showPassengerSelectionDialog(flightId: Int, passengers: List<Passenger>) {
        val passengerNames = passengers.mapIndexed { index, passenger ->
            "${index + 1}. ${passenger.name} (${if (passenger.passengerType == "adult") "Взрослый" else if (passenger.passengerType == "child") "Ребенок" else "Младенец"})"
        }.toTypedArray()
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Выберите пассажира")
            .setItems(passengerNames) { _, which ->
                val passengerNumber = which + 1
                openSeatSelection(flightId, passengerNumber)
            }
            .setNeutralButton("Выбрать для всех", null) // Можно добавить функционал выбора места для всех
            .show()
    }
    
    private fun generateSeatNumber(passenger: Passenger): String {
        // Генерация номера места в зависимости от класса
        val row = (1..30).random()
        val seat = when (passenger.seatClass) {
            "business" -> listOf("A", "B", "C").random()
            "comfort" -> listOf("D", "E", "F").random()
            else -> listOf("G", "H", "J", "K").random()
        }
        return "$row$seat"
    }

    private fun renderPassengerFields(preferences: SearchPreferences) {
        // Очистка старых полей, первая пара — основной пассажир
        passengerFields.clear()
        passengerFields.add(passengerNameInput to passengerDocumentInput)
        passengersExtraContainer.removeAllViews()

        val total = preferences.passengerCount
        val types = mutableListOf<String>().apply {
            repeat(preferences.adultCount) { add(Passenger.TYPE_ADULT) }
            repeat(preferences.childCount) { add(Passenger.TYPE_CHILD) }
            repeat(preferences.infantCount) { add(Passenger.TYPE_INFANT) }
        }

        // Уже есть первое поле (позиция 0). Добавляем остальные, если они нужны.
        for (index in 1 until total) {
            val type = types.getOrNull(index) ?: Passenger.TYPE_ADULT
            val ctx = requireContext()

            val title = TextView(ctx).apply {
                text = when (type) {
                    Passenger.TYPE_ADULT -> "Пассажир ${index + 1} (Взрослый)"
                    Passenger.TYPE_CHILD -> "Пассажир ${index + 1} (Ребёнок)"
                    Passenger.TYPE_INFANT -> "Пассажир ${index + 1} (Младенец)"
                    else -> "Пассажир ${index + 1}"
                }
                textSize = 16f
                setPadding(0, 8, 0, 4)
            }

            val nameInput = EditText(ctx).apply {
                hint = "Имя пассажира ${index + 1}"
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
                setPadding(24, 16, 24, 16)
                background = passengerNameInput.background
            }

            val docHint = if (type == Passenger.TYPE_ADULT) {
                "Номер паспорта (10 цифр)"
            } else {
                "Свидетельство о рождении (10 символов)"
            }

            val docInput = EditText(ctx).apply {
                hint = docHint
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                setPadding(24, 16, 24, 16)
                background = passengerDocumentInput.background
            }

            passengersExtraContainer.addView(title)
            passengersExtraContainer.addView(nameInput)
            passengersExtraContainer.addView(docInput)

            passengerFields.add(nameInput to docInput)
        }

        // Обновляем подсказку для первого пассажира
        updateDocumentHint()
    }

    private fun showPaymentDialog(ticketIds: List<Long>, amount: Double) {
        val ticketCount = ticketIds.size
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Оплата билета")
            .setMessage(
                "Вы успешно забронировали $ticketCount билета(ов)." +
                        "\nСумма к оплате: ${String.format(Locale.getDefault(), "%.2f", amount)} ₽" +
                        "\n\nПодтвердите оплату"
            )
            .setPositiveButton("Оплатить") { _, _ ->
                lifecycleScope.launch {
                    // Оплачиваем все созданные билеты и обновляем лояльность пользователя
                    var successCount = 0
                    ticketIds.forEach { id ->
                        val paid = viewModel.payTicket(id.toInt())
                        if (paid) successCount++
                    }

                    val message = when {
                        successCount == ticketIds.size -> "Оплата прошла успешно!"
                        successCount > 0 -> "Часть билетов успешно оплачена ($successCount из ${ticketIds.size})."
                        else -> "Ошибка при оплате. Попробуйте еще раз."
                    }

                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()

                    // Если оплата прошла хотя бы частично, уходим с экрана бронирования,
                    // чтобы пользователь увидел обновлённый статус/скидку в профиле
                    if (successCount > 0) {
                        // Переходим в профиль, где отображается уровень лояльности
                        findNavController().navigate(R.id.profileFragment)
                    }
                }
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        dialog.show()
    }

    private fun formatDateForDisplay(dateString: String): String {
        return try {
            val date = dateFormat.parse(dateString) ?: return dateString
            displayFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    private fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return "${hours}ч ${mins}м"
    }

    private fun updateDocumentHint() {
        val passengers = buildPassengersForPrice()
        val firstPassenger = passengers.firstOrNull()
        
        if (firstPassenger != null) {
            val hint = if (firstPassenger.age >= 14) {
                "Номер паспорта (10 цифр)"
            } else {
                "Свидетельство о рождении (10 символов)"
            }
            passengerDocumentInput.hint = hint
        } else {
            passengerDocumentInput.hint = "Номер документа (10 символов)"
        }
    }
}