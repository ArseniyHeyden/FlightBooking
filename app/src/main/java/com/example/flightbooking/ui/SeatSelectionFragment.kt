package com.example.flightbooking.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.flightbooking.R
import com.example.flightbooking.data.entity.Seat
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class SeatSelectionFragment : Fragment() {
    private lateinit var viewModel: FlightViewModel
    private lateinit var seatGrid: GridLayout
    private lateinit var selectedSeatText: TextView
    private lateinit var confirmButton: Button
    private lateinit var flightInfoText: TextView
    
    private var selectedSeat: Seat? = null
    private var flightId: Int = 0
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_seat_selection, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[FlightViewModel::class.java]
        
        seatGrid = view.findViewById(R.id.seat_grid)
        selectedSeatText = view.findViewById(R.id.selected_seat_text)
        confirmButton = view.findViewById(R.id.confirm_seat_button)
        flightInfoText = view.findViewById(R.id.flight_info_text)
        
        // Получаем flightId и passengerNumber из аргументов
        flightId = arguments?.getInt("flightId") 
            ?: viewModel.selectedFlight.value?.first?.id 
            ?: 0
        
        val passengerNumber = arguments?.getInt("passengerNumber", 1) ?: 1
        if (passengerNumber > 1) {
            flightInfoText.text = "${flightInfoText.text} (Пассажир $passengerNumber)"
        }
        
        if (flightId == 0) {
            Toast.makeText(context, "Ошибка: рейс не выбран", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }
        
        setupFlightInfo()
        loadSeats()
        setupConfirmButton()
    }
    
    private fun setupFlightInfo() {
        viewModel.selectedFlight.value?.let { (flight, _) ->
            val route = if (flight.hasTransfers && flight.transferCity != null) {
                "${flight.fromCity} → ${flight.transferCity} → ${flight.toCity}"
            } else {
                "${flight.fromCity} → ${flight.toCity}"
            }
            flightInfoText.text = "$route • ${flight.airline}"
        }
    }
    
    private fun loadSeats() {
        lifecycleScope.launch {
            try {
                println("SeatSelectionFragment: Загрузка мест для рейса flightId=$flightId")
                val seats = viewModel.getAllSeats(flightId)
                println("SeatSelectionFragment: Получено ${seats.size} мест из базы")
                
                if (seats.isEmpty()) {
                    println("SeatSelectionFragment: Места не найдены для рейса $flightId")
                    Toast.makeText(context, "Места недоступны для этого рейса. Попробуйте позже.", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                displaySeats(seats)
            } catch (e: Exception) {
                println("SeatSelectionFragment: Ошибка при загрузке мест: ${e.message}")
                e.printStackTrace()
                Toast.makeText(context, "Ошибка загрузки мест: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun displaySeats(seats: List<Seat>) {
        seatGrid.removeAllViews()
        seatGrid.columnCount = 7 // 1 для номера ряда + 6 для мест (A-F)
        
        // Группируем места по рядам
        val seatsByRow = seats.groupBy { it.rowNumber }.toSortedMap()
        
        // Заголовок с позициями (пустая ячейка + A-F)
        addHeaderRow()
        
        seatsByRow.forEach { (row, rowSeats) ->
            // Номер ряда
            addRowLabel(row)
            
            // Создаем кнопки для каждого места в ряду
            val positions = listOf("A", "B", "C", "D", "E", "F")
            positions.forEach { position ->
                val seat = rowSeats.find { it.position == position }
                if (seat != null) {
                    addSeatButton(seat)
                } else {
                    addEmptySpace()
                }
            }
        }
    }
    
    private fun addHeaderRow() {
        val ctx = requireContext()
        // Пустая ячейка для выравнивания с номерами рядов
        val emptyHeader = TextView(ctx).apply {
            text = ""
            minWidth = dpToPx(40)
        }
        val emptyParams = GridLayout.LayoutParams().apply {
            width = dpToPx(40)
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            columnSpec = GridLayout.spec(0)
        }
        seatGrid.addView(emptyHeader, emptyParams)
        
        // Заголовки для позиций A-F
        val positions = listOf("A", "B", "C", "D", "E", "F")
        positions.forEachIndexed { index, pos ->
            val header = TextView(ctx).apply {
                text = pos
                textSize = 12f
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            }
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(index + 1, 1f)
            }
            seatGrid.addView(header, params)
        }
    }
    
    private fun addRowLabel(row: Int) {
        val ctx = requireContext()
        val label = TextView(ctx).apply {
            text = row.toString()
            textSize = 12f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            minWidth = dpToPx(40)
        }
        val params = GridLayout.LayoutParams().apply {
            width = dpToPx(40)
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            columnSpec = GridLayout.spec(0)
        }
        seatGrid.addView(label, params)
    }
    
    private fun addSeatButton(seat: Seat) {
        val ctx = requireContext()
        val paddingPx = dpToPx(8)
        val button = MaterialButton(ctx).apply {
            text = seat.position
            textSize = 10f
            minWidth = 0
            minHeight = 0
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            
            // Разные цвета для разных типов мест и статусов
            when {
                seat.isOccupied -> {
                    setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
                    isEnabled = false
                    alpha = 0.5f
                }
                seat.seatType == "business" -> {
                    backgroundTintList = resources.getColorStateList(android.R.color.holo_purple, null)
                }
                seat.seatType == "comfort" -> {
                    backgroundTintList = resources.getColorStateList(android.R.color.holo_blue_light, null)
                }
                else -> {
                    backgroundTintList = resources.getColorStateList(android.R.color.darker_gray, null)
                }
            }
            
            setOnClickListener {
                selectSeat(seat)
            }
        }
        
        // Определяем колонку по позиции места
        val columnIndex = when (seat.position) {
            "A" -> 1
            "B" -> 2
            "C" -> 3
            "D" -> 4
            "E" -> 5
            "F" -> 6
            else -> 1
        }
        
        val marginPx = dpToPx(4)
        val params = GridLayout.LayoutParams().apply {
            width = 0
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            columnSpec = GridLayout.spec(columnIndex, 1f)
            setMargins(marginPx, marginPx, marginPx, marginPx)
        }
        
        seatGrid.addView(button, params)
    }
    
    private fun addEmptySpace() {
        // Пустое место для заполнения ряда
        val space = View(requireContext()).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
        }
        seatGrid.addView(space)
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
    
    private fun selectSeat(seat: Seat) {
        selectedSeat = seat
        selectedSeatText.text = "Выбрано место: ${seat.seatNumber}"
        confirmButton.isEnabled = true
        
        // Визуально выделяем выбранное место
        updateSeatButtons()
    }
    
    private fun updateSeatButtons() {
        for (i in 0 until seatGrid.childCount) {
            val view = seatGrid.getChildAt(i)
            if (view is MaterialButton && view.isEnabled) {
                val position = view.text.toString()
                // Находим место по позиции в текущем ряду
                val seat = findSeatByPosition(position)
                if (seat != null && seat.seatNumber == selectedSeat?.seatNumber) {
                    view.setBackgroundColor(resources.getColor(android.R.color.holo_green_light, null))
                }
            }
        }
    }
    
    private fun findSeatByPosition(position: String): Seat? {
        // Простой способ - ищем среди всех мест в базе
        // В реальном приложении лучше хранить список мест в памяти
        return null // Упрощенная версия
    }
    
    private fun setupConfirmButton() {
        confirmButton.isEnabled = false
        confirmButton.setOnClickListener {
            selectedSeat?.let { seat ->
                // Получаем номер пассажира из аргументов
                val passengerNumber = arguments?.getInt("passengerNumber", 1) ?: 1
                
                // Сохраняем выбранное место для возврата в BookingFragment
                findNavController().previousBackStackEntry?.savedStateHandle?.set("selectedSeat", seat.seatNumber)
                findNavController().previousBackStackEntry?.savedStateHandle?.set("seatPriceModifier", seat.priceModifier)
                findNavController().previousBackStackEntry?.savedStateHandle?.set("passengerNumber", passengerNumber)
                findNavController().popBackStack()
            }
        }
    }
}
