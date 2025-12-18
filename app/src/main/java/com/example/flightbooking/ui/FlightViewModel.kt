package com.example.flightbooking.ui

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.flightbooking.data.AppDatabase
import com.example.flightbooking.data.FlightRepository
import com.example.flightbooking.data.SearchPreferences
import com.example.flightbooking.data.entity.*
import kotlinx.coroutines.launch

class FlightViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FlightRepository
    private val _searchResults = MutableLiveData<List<FlightWithDetails>>()
    val searchResults: LiveData<List<FlightWithDetails>> = _searchResults

    private val _selectedFlight = MutableLiveData<Pair<Flight, String>>()
    val selectedFlight: LiveData<Pair<Flight, String>> = _selectedFlight

    private val _userFavorites = MutableLiveData<List<Flight>>()
    val userFavorites: LiveData<List<Flight>> = _userFavorites

    private val _loginError = MutableLiveData<String?>()
    val loginError: LiveData<String?> = _loginError

    private val _searchPreferences = MutableLiveData<SearchPreferences>(SearchPreferences())
    val searchPreferences: LiveData<SearchPreferences> = _searchPreferences

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser
    var userId: Int = 0
    private var userObserver: androidx.lifecycle.Observer<User?>? = null
    
    private val _searchHistory = MediatorLiveData<List<SearchHistory>>()
    val searchHistory: LiveData<List<SearchHistory>> = _searchHistory
    private var searchHistorySource: LiveData<List<SearchHistory>>? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FlightRepository(database)

        // Загружаем ID пользователя из SharedPreferences
        val sharedPref = application.getSharedPreferences("FlightBooking", Context.MODE_PRIVATE)
        userId = sharedPref.getInt("userId", 0)

        if (userId > 0) {
            // Подписываемся на изменения пользователя из базы
            userObserver = androidx.lifecycle.Observer { user: User? ->
                if (user == null) {
                    // Пользователь был удален из базы, сбрасываем userId
                    userId = 0
                    clearUserId()
                    _currentUser.postValue(null)
                    _searchHistory.postValue(emptyList())
                    updateSearchHistorySource()
                } else {
                    _currentUser.postValue(user)
                }
            }
            repository.getUser(userId).observeForever(userObserver!!)
            loadFavorites()
            updateSearchHistorySource()
        } else {
            _currentUser.postValue(null)
            _searchHistory.postValue(emptyList())
        }

        // Автоматически запускаем проверку базы при создании ViewModel
        checkDatabase()
    }

    fun loadFavorites() {
        viewModelScope.launch {
            val favorites = repository.getUserFavoritesWithDetails(userId)
            _userFavorites.postValue(favorites)
        }
    }

    suspend fun authenticateUser(email: String, password: String): User? {
        _loginError.postValue(null)
        val user = repository.authenticateUser(email, password)
        if (user == null) {
            _loginError.postValue("Неверный email или пароль")
        }
        return user
    }

    suspend fun setCurrentUser(user: User) {
        // Удаляем старый observer, если он был
        userObserver?.let { observer ->
            if (userId > 0) {
                repository.getUser(userId).removeObserver(observer)
            }
        }
        
        userId = user.id
        saveUserId(user.id)
        _currentUser.postValue(user)
        loadFavorites()
        _loginError.postValue(null)
        updateSearchHistorySource()
        
        // Подписываемся на изменения пользователя из базы
        userObserver = androidx.lifecycle.Observer { updatedUser: User? ->
            _currentUser.postValue(updatedUser)
        }
        repository.getUser(userId).observeForever(userObserver!!)
    }
    
    private fun updateSearchHistorySource() {
        // Удаляем старый источник, если он был
        searchHistorySource?.let { source ->
            _searchHistory.removeSource(source)
        }
        
        // Добавляем новый источник с актуальным userId
        if (userId > 0) {
            searchHistorySource = repository.getSearchHistory(userId)
            searchHistorySource?.let { source ->
                _searchHistory.addSource(source) { history ->
                    _searchHistory.postValue(history)
                }
            }
        } else {
            _searchHistory.postValue(emptyList())
        }
    }

    private fun saveUserId(userId: Int) {
        val sharedPref = getApplication<Application>()
            .getSharedPreferences("FlightBooking", Context.MODE_PRIVATE)
        sharedPref.edit {
            putInt("userId", userId)
        }
    }

    fun logout() {
        // Удаляем observer пользователя
        userObserver?.let { observer ->
            if (userId > 0) {
                repository.getUser(userId).removeObserver(observer)
            }
        }
        
        // Очищаем данные
        userId = 0
        clearUserId()
        _currentUser.postValue(null)
        _userFavorites.postValue(emptyList())
        _searchResults.postValue(emptyList())
        _searchHistory.postValue(emptyList())
        updateSearchHistorySource()
    }
    
    private fun clearUserId() {
        val sharedPref = getApplication<Application>()
            .getSharedPreferences("FlightBooking", Context.MODE_PRIVATE)
        sharedPref.edit {
            remove("userId")
        }
    }

    suspend fun searchFlights(from: String, to: String) {
        println("====== SEARCH START ======")
        println("DEBUG: searchFlights вызван. userId = $userId")
        println("DEBUG: Параметры поиска: from='$from' to='$to'")

        // Поиск работает даже без авторизации (история просто не сохранится)
        val flights = repository.searchFlights(from, to, userId)
        println("DEBUG: Найдено рейсов: ${flights.size}")

        // Отладочный вывод найденных рейсов
        flights.forEachIndexed { index, flight ->
            println("DEBUG: Рейс $index: ${flight.id} - ${flight.fromCity} → ${flight.toCity}")
        }

        // Проверяем избранное для каждого рейса (только если пользователь авторизован)
        val flightsWithDetails = flights.map { flight ->
            val isFav = if (userId > 0) repository.isFavorite(userId, flight.id) else false
            println("DEBUG: Рейс ${flight.id} избранный: $isFav")
            FlightWithDetails(
                flight = flight,
                isFavorite = isFav
            )
        }
        _searchResults.postValue(flightsWithDetails)
        println("====== SEARCH END ======")
    }

    fun selectFlight(flight: Flight, date: String) {
        _selectedFlight.value = Pair(flight, date)
    }

    suspend fun toggleFavorite(flightId: Int) {
        if (userId == 0) return

        if (repository.isFavorite(userId, flightId)) {
            repository.removeFromFavorites(userId, flightId)
        } else {
            repository.addToFavorites(userId, flightId)
        }

        // Обновляем список избранного
        loadFavorites()

        // Обновляем текущие результаты поиска
        _searchResults.value?.let { current ->
            val updated = current.map { item ->
                if (item.flight.id == flightId) {
                    item.copy(isFavorite = !item.isFavorite)
                } else {
                    item
                }
            }
            _searchResults.value = updated
        }
    }

    suspend fun isFavorite(flightId: Int): Boolean {
        return if (userId > 0) {
            repository.isFavorite(userId, flightId)
        } else {
            false
        }
    }

    fun calculatePrice(
        basePrice: Double,
        date: String,
        hasReturn: Boolean,
        discountLevel: Int,
        seatClass: String = "economy",
        isHotDeal: Boolean = false,
        hotDealDiscount: Int = 0,
        seatPriceModifier: Double = 1.0
    ): Double {
        return repository.calculatePrice(basePrice, date, discountLevel, hasReturn, seatClass, isHotDeal, hotDealDiscount, seatPriceModifier)
    }

    fun calculateTotalPrice(
        basePrice: Double,
        date: String,
        hasReturn: Boolean,
        discountLevel: Int,
        seatClass: String,
        passengers: List<Passenger>
    ): Double {
        val pricePerAdult = calculatePrice(basePrice, date, hasReturn, discountLevel, seatClass)
        var total = 0.0

        passengers.forEach { passenger ->
            val coefficient = Passenger.getPriceCoefficient(passenger.passengerType)
            total += pricePerAdult * coefficient
        }

        return total
    }

    suspend fun bookTicket(
        flightId: Int,
        departureDate: String,
        returnDate: String?,
        passengerName: String,
        passengerDocument: String,
        finalPrice: Double,
        seatNumber: String? = null
    ): Long {
        if (userId == 0) return -1L

        val ticketId = repository.bookTicket(
            userId,
            flightId,
            departureDate,
            returnDate,
            passengerName,
            passengerDocument,
            finalPrice,
            seatNumber
        )
        
        // Планируем уведомления для билета
        if (ticketId > 0) {
            val ticket = repository.getTicketById(ticketId)
            ticket?.let {
                val notificationHelper = com.example.flightbooking.utils.NotificationHelper(getApplication())
                notificationHelper.scheduleFlightNotifications(it)
            }
        }
        
        return ticketId
    }
    
    suspend fun getHotDeals(limit: Int = 10): List<Flight> {
        return repository.getHotDeals(limit)
    }
    
    suspend fun getAvailableSeats(flightId: Int): List<Seat> {
        return repository.getAvailableSeats(flightId)
    }
    
    suspend fun getAllSeats(flightId: Int): List<Seat> {
        return repository.getAllSeats(flightId)
    }
    
    suspend fun selectSeat(flightId: Int, seatNumber: String): Boolean {
        return repository.selectSeat(flightId, seatNumber)
    }

    suspend fun payTicket(ticketId: Int): Boolean {
        if (userId == 0) return false
        return repository.payTicket(ticketId, userId)
    }


    fun getUserTickets(): LiveData<List<Ticket>> {
        return repository.getUserTickets(userId)
    }

    fun getDiscountLevelName(level: Int): String {
        return repository.getDiscountLevelName(level)
    }

    // Вспомогательный метод для очистки ошибок
    fun clearLoginError() {
        _loginError.postValue(null)
    }

    // ПЕРЕНЕСЕННАЯ ФУНКЦИЯ - теперь внутри класса
    fun checkDatabase() {
        viewModelScope.launch {
            println("====== DATABASE CHECK START ======")

            // 1. Все рейсы
            val allFlights = repository.getAllFlights()
            println("DEBUG: Всего рейсов в базе: ${allFlights.size}")

            // 2. Вывести все рейсы
            allFlights.forEachIndexed { index, flight ->
                println("DEBUG: Рейс $index: ID=${flight.id} ${flight.fromCity} -> ${flight.toCity} цена=${flight.basePrice}")
            }

            // 3. Проверить конкретный запрос
            val testResult = repository.searchFlights("Москва", "Санкт-Петербург", userId)
            println("DEBUG: Москва -> СПб найдено: ${testResult.size}")

            // 4. Проверить с маленькими буквами
            val testResultLower = repository.searchFlights("москва", "санкт-петербург", userId)
            println("DEBUG: москва -> санкт-петербург найдено: ${testResultLower.size}")

            // 5. Проверить с пробелами
            val testResultSpaces = repository.searchFlights(" Москва ", " Санкт-Петербург ", userId)
            println("DEBUG: ' Москва ' -> ' Санкт-Петербург ' найдено: ${testResultSpaces.size}")

            // 6. Проверить авторизацию
            println("DEBUG: Текущий userId: $userId")

            println("====== DATABASE CHECK END ======")
        }
    }

    fun getAvailableAirlines(): List<String> {
        // Заглушка - в реальном приложении нужно получать из базы данных
        return listOf("Аэрофлот", "S7 Airlines", "Победа", "Уральские авиалинии", "Россия")
    }

    fun updateSearchPreferences(preferences: SearchPreferences) {
        _searchPreferences.value = preferences
    }

    fun getDateFromPicker(year: Int, month: Int, day: Int): String {
        return repository.getDateFromPicker(year, month, day)
    }
}