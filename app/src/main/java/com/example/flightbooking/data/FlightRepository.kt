package com.example.flightbooking.data

import androidx.lifecycle.LiveData
import com.example.flightbooking.data.entity.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class FlightRepository(private val database: AppDatabase) {
    private val userDao = database.userDao()
    private val flightDao = database.flightDao()
    private val ticketDao = database.ticketDao()
    private val searchHistoryDao = database.searchHistoryDao()
    private val favoriteRouteDao = database.favoriteRouteDao()
    private val seatDao = database.seatDao()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getUser(userId: Int): LiveData<User?> = userDao.getUser(userId)

    suspend fun authenticateUser(email: String, password: String): User? {
        val user = userDao.getUserByEmail(email) ?: return null
        return if (User.verifyPassword(password, user)) user else null
    }

    suspend fun searchFlights(from: String, to: String, userId: Int): List<Flight> {
        println("Repository: Поиск рейсов from='$from' to='$to' userId=$userId")

        // Ищем прямые рейсы
        val directFlights = flightDao.searchFlights(from, to)
        
        // Ищем рейсы с пересадками
        val flightsWithTransfers = flightDao.searchFlightsWithTransfers(from, to)
        
        // Объединяем результаты: сначала прямые, потом с пересадками
        val allFlights = directFlights + flightsWithTransfers
        
        println("Repository: Найдено ${allFlights.size} рейсов (${directFlights.size} прямых, ${flightsWithTransfers.size} с пересадками)")

        // Сохраняем историю поиска только если пользователь авторизован и существует в базе
        if (userId > 0 && allFlights.isNotEmpty()) {
            try {
                // Проверяем существование пользователя в базе
                val userExists = userDao.getUserSuspend(userId) != null
                if (userExists) {
                    // Нормализуем названия городов (убираем пробелы, приводим к нормальному формату)
                    val normalizedFrom = from.trim().replace(Regex("\\s+"), " ")
                    val normalizedTo = to.trim().replace(Regex("\\s+"), " ")
                    
                    val history = SearchHistory(
                        userId = userId,
                        fromCity = normalizedFrom,
                        toCity = normalizedTo,
                        searchDate = getCurrentDateTime(),
                        resultCount = allFlights.size
                    )
                    searchHistoryDao.insert(history)
                    println("Repository: История поиска сохранена: $normalizedFrom → $normalizedTo")
                } else {
                    println("Repository: Пользователь с ID $userId не найден, история не сохранена")
                }
            } catch (e: Exception) {
                println("Repository: Ошибка при сохранении истории поиска: ${e.message}")
                e.printStackTrace()
            }
        }

        return allFlights
    }
    
    suspend fun getHotDeals(limit: Int = 10): List<Flight> {
        return flightDao.getHotDeals(limit)
    }
    
    suspend fun getAvailableSeats(flightId: Int): List<Seat> {
        return seatDao.getAvailableSeatsForFlight(flightId)
    }
    
    suspend fun getAllSeats(flightId: Int): List<Seat> {
        return seatDao.getSeatsForFlight(flightId)
    }
    
    suspend fun selectSeat(flightId: Int, seatNumber: String): Boolean {
        val seat = seatDao.getSeatByNumber(flightId, seatNumber)
        return if (seat != null && !seat.isOccupied) {
            seatDao.occupySeat(flightId, seatNumber)
            true
        } else {
            false
        }
    }

    suspend fun getAllFlights(): List<Flight> {
        return flightDao.getAllFlights()
    }

    suspend fun getFlightById(id: Int): Flight? = flightDao.getFlightById(id)

    suspend fun getPopularRoutes(limit: Int = 5): List<Pair<String, String>> {
        val history = searchHistoryDao.getRecentSearches(limit)
        return history.map { it.fromCity to it.toCity }
    }

    fun calculatePrice(
        basePrice: Double,
        departureDate: String,
        discountLevel: Int,
        hasReturn: Boolean,
        seatClass: String = "economy",
        isHotDeal: Boolean = false,
        hotDealDiscount: Int = 0,
        seatPriceModifier: Double = 1.0
    ): Double {
        var price = basePrice

        val daysUntilFlight = getDaysUntilFlight(departureDate)
        val dateCoefficient = when {
            daysUntilFlight < 3 -> 1.5
            daysUntilFlight < 7 -> 1.3
            daysUntilFlight < 14 -> 1.1
            daysUntilFlight < 30 -> 1.0
            else -> 0.9
        }
        price *= dateCoefficient

        // Получаем месяц из даты
        val month = try {
            departureDate.substring(5, 7).toInt()
        } catch (e: Exception) {
            // Если не удалось распарсить дату, используем текущий месяц
            Calendar.getInstance().get(Calendar.MONTH) + 1
        }

        val seasonCoefficient = when (month) {
            6, 7, 8, 12 -> 1.4
            1, 2 -> 0.8
            else -> 1.0
        }
        price *= seasonCoefficient

        if (hasReturn) {
            price *= 1.8
        }

        val classCoefficient = when (seatClass) {
            "business" -> 2.5
            "comfort" -> 1.5
            else -> 1.0
        }
        price *= classCoefficient

        val discountCoefficient = when (discountLevel) {
            0 -> 1.0
            1 -> 0.95
            2 -> 0.90
            else -> 1.0
        }
        price *= discountCoefficient
        
        // Применяем скидку горячего билета
        if (isHotDeal && hotDealDiscount > 0) {
            price *= (1.0 - hotDealDiscount / 100.0)
        }
        
        // Применяем модификатор места
        price *= seatPriceModifier

        return (price * 100).roundToInt() / 100.0
    }

    private fun getDaysUntilFlight(dateString: String): Long {
        return try {
            val flightDate = dateFormat.parse(dateString) ?: return 0L
            val today = Calendar.getInstance().time

            val diff = flightDate.time - today.time
            val days = diff / (24 * 60 * 60 * 1000)
            days
        } catch (e: Exception) {
            0L
        }
    }

    private fun getCurrentDateTime(): String {
        return dateFormat.format(Calendar.getInstance().time)
    }

    suspend fun bookTicket(
        userId: Int,
        flightId: Int,
        departureDate: String,
        returnDate: String?,
        passengerName: String,
        passengerDocument: String,
        finalPrice: Double,
        seatNumber: String? = null
    ): Long {
        // Проверяем существование пользователя и рейса перед бронированием
        val userExists = userDao.getUserSuspend(userId) != null
        if (!userExists) {
            println("Repository: Пользователь с ID $userId не найден, бронирование невозможно")
            return -1L
        }
        
        val flightExists = flightDao.getFlightById(flightId) != null
        if (!flightExists) {
            println("Repository: Рейс с ID $flightId не найден, бронирование невозможно")
            return -1L
        }
        
        val ticket = Ticket(
            userId = userId,
            flightId = flightId,
            departureDate = departureDate,
            returnDate = returnDate,
            seatNumber = seatNumber,
            passengerName = passengerName,
            passengerDocument = passengerDocument,
            finalPrice = finalPrice,
            isPaid = false,
            bookingDate = getCurrentDateTime()
        )
        return ticketDao.insert(ticket)
    }
    
    suspend fun getTicketById(ticketId: Long): Ticket? {
        return ticketDao.getTicketById(ticketId.toInt())
    }

    suspend fun payTicket(ticketId: Int, userId: Int): Boolean {
        // Получаем НЕоплаченные билеты
        val unpaidTickets = ticketDao.getUnpaidTickets(userId)
        val ticket = unpaidTickets.find { it.id == ticketId } ?: return false

        // Помечаем как оплаченный
        ticketDao.markAsPaid(ticketId, getCurrentDateTime())

        // Обновляем статистику пользователя
        userDao.updateUserStats(userId, ticket.finalPrice)

        // Проверяем нужно ли повысить уровень
        val paidCount = ticketDao.getPaidTicketsCount(userId)
        val newDiscountLevel = when {
            paidCount >= 20 -> 2
            paidCount >= 10 -> 1
            else -> 0
        }

        val currentLevel = userDao.getUserDiscountLevel(userId)
        if (newDiscountLevel > currentLevel) {
            userDao.updateDiscountLevel(userId, newDiscountLevel)
        }

        return true
    }

    suspend fun cancelTicket(ticketId: Int, userId: Int): Boolean {
        val tickets = ticketDao.getTicketsByStatus(userId, Ticket.STATUS_BOOKED)
        val ticket = tickets.find { it.id == ticketId } ?: return false

        // Используем найденный билет для дополнительной проверки и логирования
        println("Отмена бронирования билета с ID: ${ticket.id} для пользователя $userId")
        ticketDao.updateStatus(ticket.id, Ticket.STATUS_CANCELLED)
        return true
    }

    fun getUserTickets(userId: Int): LiveData<List<Ticket>> = ticketDao.getUserTickets(userId)

    fun getTicketsWithDetails(userId: Int): LiveData<List<TicketWithDetails>> =
        ticketDao.getTicketsWithDetails(userId)

    fun getSearchHistory(userId: Int): LiveData<List<SearchHistory>> =
        searchHistoryDao.getHistory(userId)

    suspend fun clearSearchHistory(userId: Int) = searchHistoryDao.clearHistory(userId)

    suspend fun addToFavorites(userId: Int, flightId: Int) {
        // Проверяем существование пользователя перед добавлением в избранное
        val userExists = userDao.getUserSuspend(userId) != null
        if (!userExists) {
            println("Repository: Пользователь с ID $userId не найден, избранное не добавлено")
            return
        }
        
        val existing = favoriteRouteDao.getFavorite(userId, flightId)
        if (existing == null) {
            val favorite = FavoriteRoute(
                userId = userId,
                flightId = flightId,
                addedDate = getCurrentDateTime()
            )
            favoriteRouteDao.insert(favorite)
        }
    }

    suspend fun removeFromFavorites(userId: Int, flightId: Int) {
        val favorite = favoriteRouteDao.getFavorite(userId, flightId)
        favorite?.let { favoriteRouteDao.delete(it) }
    }

    suspend fun isFavorite(userId: Int, flightId: Int): Boolean {
        return favoriteRouteDao.getFavorite(userId, flightId) != null
    }

    fun getUserFavorites(userId: Int): LiveData<List<FavoriteRoute>> =
        favoriteRouteDao.getUserFavorites(userId)

    suspend fun getUserFavoritesWithDetails(userId: Int): List<Flight> {
        val favorites = favoriteRouteDao.getUserFavoritesSuspend(userId)
        return favorites.mapNotNull { flightDao.getFlightById(it.flightId) }
    }

    fun getDiscountLevelName(level: Int): String = when (level) {
        0 -> "Бронза"
        1 -> "Серебро"
        2 -> "Золото"
        else -> "Нет"
    }

    suspend fun cleanupExpiredBookings() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -30) // 30 дней назад
        val thirtyDaysAgo = dateFormat.format(calendar.time)

        ticketDao.cleanupExpiredBookings(thirtyDaysAgo)
    }

    // Вспомогательные методы для работы с датами
    fun formatDateForDisplay(dateString: String): String {
        return try {
            val date = dateFormat.parse(dateString) ?: return dateString
            val displayFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            displayFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    fun getCurrentDateForPicker(): Triple<Int, Int, Int> {
        val calendar = Calendar.getInstance()
        return Triple(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun getDateFromPicker(year: Int, month: Int, day: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        return dateFormat.format(calendar.time)
    }
}