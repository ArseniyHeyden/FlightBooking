package com.example.flightbooking.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.flightbooking.data.dao.*
import kotlin.random.Random
import com.example.flightbooking.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Flight::class,
        Ticket::class,
        SearchHistory::class,
        FavoriteRoute::class,
        Seat::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun flightDao(): FlightDao
    abstract fun ticketDao(): TicketDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun favoriteRouteDao(): FavoriteRouteDao
    abstract fun seatDao(): SeatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flight_booking_db"
                )
                    .fallbackToDestructiveMigration() // Важно! Создаст базу заново при проблемах
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    println("DatabaseCallback: onCreate вызван, заполняем базу данных")
                    populateDatabase(database)
                    println("DatabaseCallback: База данных заполнена")
                }
            }
        }
        
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    // Проверяем, есть ли рейсы в базе, если нет - заполняем
                    val flightCount = database.flightDao().getAllFlights().size
                    println("DatabaseCallback: onOpen вызван, рейсов в базе: $flightCount")
                    if (flightCount == 0) {
                        println("DatabaseCallback: База пустая, заполняем данными")
                        populateDatabase(database)
                        println("DatabaseCallback: База данных заполнена после открытия")
                    }
                }
            }
        }

        private suspend fun populateDatabase(database: AppDatabase) {
            println("populateDatabase: Начинаем заполнение базы данных")
            val flightDao = database.flightDao()
            val seatDao = database.seatDao()

            // Функция для получения аэропорта по городу
            fun getAirport(city: String): String = when (city) {
                "Москва" -> "Шереметьево"
                "Санкт-Петербург" -> "Пулково"
                "Казань" -> "Казань"
                "Сочи" -> "Адлер"
                "Екатеринбург" -> "Кольцово"
                "Новосибирск" -> "Толмачёво"
                else -> ""
            }

            // Прямые рейсы - создаем больше вариантов с разными условиями
            val directFlights = listOf(
                // Москва - Санкт-Петербург (много вариантов) - 15+ вариантов
                Flight(fromCity = "Москва", toCity = "Санкт-Петербург", basePrice = 3500.0, duration = 90, airline = "Аэрофлот", departureTime = "06:00", arrivalTime = "07:30", isHotDeal = false, includesBaggage = true, fromAirport = "Шереметьево", toAirport = "Пулково"),
                Flight(fromCity = "Москва", toCity = "Санкт-Петербург", basePrice = 3200.0, duration = 92, airline = "Победа", departureTime = "06:30", arrivalTime = "08:02", isHotDeal = false, includesBaggage = false, fromAirport = "Внуково", toAirport = "Пулково"),
                Flight(fromCity = "Москва", toCity = "Санкт-Петербург", basePrice = 3600.0, duration = 88, airline = "Аэрофлот", departureTime = "08:00", arrivalTime = "09:28", isHotDeal = false, includesBaggage = true, fromAirport = "Шереметьево", toAirport = "Пулково"),
                Flight(fromCity = "Москва", toCity = "Санкт-Петербург", basePrice = 4200.0, duration = 85, airline = "S7 Airlines", departureTime = "10:30", arrivalTime = "11:55", isHotDeal = true, hotDealDiscount = 15, includesBaggage = true, fromAirport = "Домодедово", toAirport = "Пулково"),
                Flight(fromCity = "Москва", toCity = "Санкт-Петербург", basePrice = 3700.0, duration = 92, airline = "S7 Airlines", departureTime = "12:30", arrivalTime = "14:02", isHotDeal = false, includesBaggage = true, fromAirport = "Домодедово", toAirport = "Пулково"),
                Flight(fromCity = "Москва", toCity = "Санкт-Петербург", basePrice = 3800.0, duration = 95, airline = "Победа", departureTime = "14:00", arrivalTime = "15:35", isHotDeal = false, includesBaggage = false, fromAirport = "Внуково", toAirport = "Пулково"),
                Flight(fromCity = "Москва", toCity = "Санкт-Петербург", basePrice = 3900.0, duration = 93, airline = "Уральские авиалинии", departureTime = "16:00", arrivalTime = "17:33", isHotDeal = false, includesBaggage = true, fromAirport = "Шереметьево", toAirport = "Пулково"),
                Flight(fromCity = "Москва", toCity = "Санкт-Петербург", basePrice = 4000.0, duration = 87, airline = "Аэрофлот", departureTime = "18:00", arrivalTime = "19:27", isHotDeal = false, includesBaggage = true, fromAirport = "Шереметьево", toAirport = "Пулково"),
                Flight(fromCity = "Москва", toCity = "Санкт-Петербург", basePrice = 4100.0, duration = 89, airline = "Россия", departureTime = "20:30", arrivalTime = "21:59", isHotDeal = true, hotDealDiscount = 12, includesBaggage = true, fromAirport = "Шереметьево", toAirport = "Пулково"),
                // Дополнительные рейсы Москва-СПб
                Flight(fromCity = "Москва", toCity = "Санкт-Петербург", basePrice = 3300.0, duration = 91, airline = "Победа", departureTime = "05:30", arrivalTime = "07:01", isHotDeal = false, includesBaggage = false, fromAirport = "Внуково", toAirport = "Пулково"),
                Flight(fromCity = "Москва", toCity = "Санкт-Петербург", basePrice = 3400.0, duration = 89, airline = "Победа", departureTime = "09:00", arrivalTime = "10:29", isHotDeal = false, includesBaggage = false, fromAirport = "Внуково", toAirport = "Пулково"),
                Flight(fromCity = "Москва", toCity = "Санкт-Петербург", basePrice = 4300.0, duration = 86, airline = "Аэрофлот", departureTime = "11:00", arrivalTime = "12:26", isHotDeal = false, includesBaggage = true, fromAirport = "Шереметьево", toAirport = "Пулково"),
                Flight(fromCity = "Москва", toCity = "Санкт-Петербург", basePrice = 4400.0, duration = 84, airline = "S7 Airlines", departureTime = "13:00", arrivalTime = "14:24", isHotDeal = true, hotDealDiscount = 10, includesBaggage = true, fromAirport = "Домодедово", toAirport = "Пулково"),
                Flight(fromCity = "Москва", toCity = "Санкт-Петербург", basePrice = 4500.0, duration = 88, airline = "Аэрофлот", departureTime = "15:30", arrivalTime = "16:58", isHotDeal = false, includesBaggage = true, fromAirport = "Шереметьево", toAirport = "Пулково"),
                Flight(fromCity = "Москва", toCity = "Санкт-Петербург", basePrice = 4600.0, duration = 90, airline = "S7 Airlines", departureTime = "17:00", arrivalTime = "18:30", isHotDeal = false, includesBaggage = true, fromAirport = "Домодедово", toAirport = "Пулково"),
                Flight(fromCity = "Москва", toCity = "Санкт-Петербург", basePrice = 4700.0, duration = 92, airline = "Аэрофлот", departureTime = "19:00", arrivalTime = "20:32", isHotDeal = true, hotDealDiscount = 8, includesBaggage = true, fromAirport = "Шереметьево", toAirport = "Пулково"),
                Flight(fromCity = "Москва", toCity = "Санкт-Петербург", basePrice = 4800.0, duration = 91, airline = "Россия", departureTime = "21:30", arrivalTime = "23:01", isHotDeal = false, includesBaggage = true, fromAirport = "Шереметьево", toAirport = "Пулково"),
                
                Flight(fromCity = "Санкт-Петербург", toCity = "Москва", basePrice = 3500.0, duration = 90, airline = "Аэрофлот", departureTime = "07:00", arrivalTime = "08:30", isHotDeal = false),
                Flight(fromCity = "Санкт-Петербург", toCity = "Москва", basePrice = 3600.0, duration = 88, airline = "Аэрофлот", departureTime = "09:00", arrivalTime = "10:28", isHotDeal = false),
                Flight(fromCity = "Санкт-Петербург", toCity = "Москва", basePrice = 4000.0, duration = 85, airline = "S7 Airlines", departureTime = "11:00", arrivalTime = "12:25", isHotDeal = true, hotDealDiscount = 20),
                Flight(fromCity = "Санкт-Петербург", toCity = "Москва", basePrice = 3700.0, duration = 92, airline = "S7 Airlines", departureTime = "14:00", arrivalTime = "15:32", isHotDeal = false),
                Flight(fromCity = "Санкт-Петербург", toCity = "Москва", basePrice = 3800.0, duration = 95, airline = "Победа", departureTime = "16:30", arrivalTime = "18:05", isHotDeal = false),
                Flight(fromCity = "Санкт-Петербург", toCity = "Москва", basePrice = 3900.0, duration = 93, airline = "Уральские авиалинии", departureTime = "18:00", arrivalTime = "19:33", isHotDeal = false),
                Flight(fromCity = "Санкт-Петербург", toCity = "Москва", basePrice = 4100.0, duration = 87, airline = "Россия", departureTime = "21:00", arrivalTime = "22:27", isHotDeal = true, hotDealDiscount = 10),
                
                // Москва - Сочи (много вариантов)
                Flight(fromCity = "Москва", toCity = "Сочи", basePrice = 5500.0, duration = 140, airline = "S7 Airlines", departureTime = "06:00", arrivalTime = "08:20", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Сочи", basePrice = 5600.0, duration = 138, airline = "Аэрофлот", departureTime = "07:30", arrivalTime = "09:48", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Сочи", basePrice = 5800.0, duration = 142, airline = "S7 Airlines", departureTime = "09:00", arrivalTime = "11:22", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Сочи", basePrice = 6000.0, duration = 135, airline = "Аэрофлот", departureTime = "10:30", arrivalTime = "12:45", isHotDeal = true, hotDealDiscount = 25),
                Flight(fromCity = "Москва", toCity = "Сочи", basePrice = 5700.0, duration = 139, airline = "Уральские авиалинии", departureTime = "12:00", arrivalTime = "14:19", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Сочи", basePrice = 5200.0, duration = 145, airline = "Победа", departureTime = "14:00", arrivalTime = "16:25", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Сочи", basePrice = 5400.0, duration = 143, airline = "Победа", departureTime = "16:00", arrivalTime = "18:23", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Сочи", basePrice = 5900.0, duration = 137, airline = "Россия", departureTime = "18:30", arrivalTime = "20:47", isHotDeal = true, hotDealDiscount = 18),
                Flight(fromCity = "Москва", toCity = "Сочи", basePrice = 6100.0, duration = 136, airline = "Аэрофлот", departureTime = "20:00", arrivalTime = "22:16", isHotDeal = false),
                
                Flight(fromCity = "Сочи", toCity = "Москва", basePrice = 5500.0, duration = 140, airline = "S7 Airlines", departureTime = "06:00", arrivalTime = "08:20", isHotDeal = false),
                Flight(fromCity = "Сочи", toCity = "Москва", basePrice = 5600.0, duration = 138, airline = "Аэрофлот", departureTime = "09:00", arrivalTime = "11:18", isHotDeal = false),
                Flight(fromCity = "Сочи", toCity = "Москва", basePrice = 5800.0, duration = 142, airline = "S7 Airlines", departureTime = "11:30", arrivalTime = "13:52", isHotDeal = true, hotDealDiscount = 15),
                Flight(fromCity = "Сочи", toCity = "Москва", basePrice = 5700.0, duration = 139, airline = "Уральские авиалинии", departureTime = "13:00", arrivalTime = "15:19", isHotDeal = false),
                Flight(fromCity = "Сочи", toCity = "Москва", basePrice = 5300.0, duration = 144, airline = "Победа", departureTime = "15:00", arrivalTime = "17:24", isHotDeal = false),
                Flight(fromCity = "Сочи", toCity = "Москва", basePrice = 5900.0, duration = 137, airline = "Россия", departureTime = "17:30", arrivalTime = "19:47", isHotDeal = false),
                Flight(fromCity = "Сочи", toCity = "Москва", basePrice = 6000.0, duration = 136, airline = "Аэрофлот", departureTime = "19:00", arrivalTime = "21:16", isHotDeal = true, hotDealDiscount = 22),
                
                // Москва - Казань (много вариантов)
                Flight(fromCity = "Москва", toCity = "Казань", basePrice = 4000.0, duration = 100, airline = "Победа", departureTime = "07:00", arrivalTime = "08:40", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Казань", basePrice = 4100.0, duration = 98, airline = "Победа", departureTime = "08:30", arrivalTime = "10:08", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Казань", basePrice = 4300.0, duration = 97, airline = "S7 Airlines", departureTime = "10:00", arrivalTime = "11:37", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Казань", basePrice = 4500.0, duration = 95, airline = "Аэрофлот", departureTime = "12:00", arrivalTime = "13:35", isHotDeal = true, hotDealDiscount = 18),
                Flight(fromCity = "Москва", toCity = "Казань", basePrice = 4400.0, duration = 96, airline = "Аэрофлот", departureTime = "13:00", arrivalTime = "14:36", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Казань", basePrice = 4200.0, duration = 99, airline = "Уральские авиалинии", departureTime = "15:00", arrivalTime = "16:39", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Казань", basePrice = 4100.0, duration = 101, airline = "Победа", departureTime = "17:30", arrivalTime = "19:11", isHotDeal = true, hotDealDiscount = 10),
                Flight(fromCity = "Москва", toCity = "Казань", basePrice = 4600.0, duration = 94, airline = "Аэрофлот", departureTime = "19:00", arrivalTime = "20:34", isHotDeal = false),
                
                Flight(fromCity = "Казань", toCity = "Москва", basePrice = 4000.0, duration = 100, airline = "Победа", departureTime = "08:00", arrivalTime = "09:40", isHotDeal = false),
                Flight(fromCity = "Казань", toCity = "Москва", basePrice = 4100.0, duration = 98, airline = "Победа", departureTime = "11:00", arrivalTime = "12:38", isHotDeal = false),
                Flight(fromCity = "Казань", toCity = "Москва", basePrice = 4300.0, duration = 97, airline = "S7 Airlines", departureTime = "13:00", arrivalTime = "14:37", isHotDeal = true, hotDealDiscount = 15),
                Flight(fromCity = "Казань", toCity = "Москва", basePrice = 4500.0, duration = 95, airline = "Аэрофлот", departureTime = "14:30", arrivalTime = "16:05", isHotDeal = false),
                Flight(fromCity = "Казань", toCity = "Москва", basePrice = 4200.0, duration = 99, airline = "Уральские авиалинии", departureTime = "16:00", arrivalTime = "17:39", isHotDeal = false),
                Flight(fromCity = "Казань", toCity = "Москва", basePrice = 4100.0, duration = 101, airline = "Победа", departureTime = "18:00", arrivalTime = "19:41", isHotDeal = false),
                Flight(fromCity = "Казань", toCity = "Москва", basePrice = 4600.0, duration = 94, airline = "Аэрофлот", departureTime = "20:00", arrivalTime = "21:34", isHotDeal = true, hotDealDiscount = 12),
                
                // Казань - Санкт-Петербург (много вариантов)
                Flight(fromCity = "Казань", toCity = "Санкт-Петербург", basePrice = 4500.0, duration = 120, airline = "Аэрофлот", departureTime = "07:00", arrivalTime = "09:00", isHotDeal = false),
                Flight(fromCity = "Казань", toCity = "Санкт-Петербург", basePrice = 4600.0, duration = 118, airline = "Аэрофлот", departureTime = "09:30", arrivalTime = "11:28", isHotDeal = false),
                Flight(fromCity = "Казань", toCity = "Санкт-Петербург", basePrice = 4800.0, duration = 115, airline = "S7 Airlines", departureTime = "11:00", arrivalTime = "12:55", isHotDeal = true, hotDealDiscount = 18),
                Flight(fromCity = "Казань", toCity = "Санкт-Петербург", basePrice = 4700.0, duration = 117, airline = "Уральские авиалинии", departureTime = "13:00", arrivalTime = "14:57", isHotDeal = false),
                Flight(fromCity = "Казань", toCity = "Санкт-Петербург", basePrice = 4900.0, duration = 116, airline = "S7 Airlines", departureTime = "15:00", arrivalTime = "16:56", isHotDeal = false),
                Flight(fromCity = "Казань", toCity = "Санкт-Петербург", basePrice = 5000.0, duration = 114, airline = "Аэрофлот", departureTime = "17:30", arrivalTime = "19:24", isHotDeal = true, hotDealDiscount = 15),
                Flight(fromCity = "Казань", toCity = "Санкт-Петербург", basePrice = 4600.0, duration = 119, airline = "Россия", departureTime = "19:00", arrivalTime = "20:59", isHotDeal = false),
                Flight(fromCity = "Казань", toCity = "Санкт-Петербург", basePrice = 5100.0, duration = 113, airline = "Аэрофлот", departureTime = "21:00", arrivalTime = "22:53", isHotDeal = false),
                
                Flight(fromCity = "Санкт-Петербург", toCity = "Казань", basePrice = 4500.0, duration = 120, airline = "Аэрофлот", departureTime = "06:00", arrivalTime = "08:00", isHotDeal = false),
                Flight(fromCity = "Санкт-Петербург", toCity = "Казань", basePrice = 4600.0, duration = 118, airline = "Аэрофлот", departureTime = "08:30", arrivalTime = "10:28", isHotDeal = false),
                Flight(fromCity = "Санкт-Петербург", toCity = "Казань", basePrice = 4800.0, duration = 115, airline = "S7 Airlines", departureTime = "10:00", arrivalTime = "11:55", isHotDeal = true, hotDealDiscount = 20),
                Flight(fromCity = "Санкт-Петербург", toCity = "Казань", basePrice = 4700.0, duration = 117, airline = "Уральские авиалинии", departureTime = "12:00", arrivalTime = "13:57", isHotDeal = false),
                Flight(fromCity = "Санкт-Петербург", toCity = "Казань", basePrice = 4900.0, duration = 116, airline = "S7 Airlines", departureTime = "14:30", arrivalTime = "16:26", isHotDeal = false),
                Flight(fromCity = "Санкт-Петербург", toCity = "Казань", basePrice = 5000.0, duration = 114, airline = "Аэрофлот", departureTime = "16:00", arrivalTime = "17:54", isHotDeal = true, hotDealDiscount = 16),
                Flight(fromCity = "Санкт-Петербург", toCity = "Казань", basePrice = 4600.0, duration = 119, airline = "Россия", departureTime = "18:00", arrivalTime = "19:59", isHotDeal = false),
                Flight(fromCity = "Санкт-Петербург", toCity = "Казань", basePrice = 5100.0, duration = 113, airline = "Аэрофлот", departureTime = "20:00", arrivalTime = "21:53", isHotDeal = false),
                
                // Москва - Екатеринбург (много вариантов)
                Flight(fromCity = "Москва", toCity = "Екатеринбург", basePrice = 6000.0, duration = 140, airline = "Уральские авиалинии", departureTime = "06:00", arrivalTime = "08:20", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Екатеринбург", basePrice = 6100.0, duration = 138, airline = "Уральские авиалинии", departureTime = "07:30", arrivalTime = "09:48", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Екатеринбург", basePrice = 6300.0, duration = 137, airline = "S7 Airlines", departureTime = "09:00", arrivalTime = "11:17", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Екатеринбург", basePrice = 6400.0, duration = 136, airline = "Аэрофлот", departureTime = "11:30", arrivalTime = "13:46", isHotDeal = true, hotDealDiscount = 14),
                Flight(fromCity = "Москва", toCity = "Екатеринбург", basePrice = 6500.0, duration = 135, airline = "S7 Airlines", departureTime = "13:00", arrivalTime = "15:15", isHotDeal = true, hotDealDiscount = 12),
                Flight(fromCity = "Москва", toCity = "Екатеринбург", basePrice = 6200.0, duration = 139, airline = "Уральские авиалинии", departureTime = "15:00", arrivalTime = "17:19", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Екатеринбург", basePrice = 6600.0, duration = 134, airline = "Аэрофлот", departureTime = "17:30", arrivalTime = "19:44", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Екатеринбург", basePrice = 6700.0, duration = 133, airline = "S7 Airlines", departureTime = "19:00", arrivalTime = "21:13", isHotDeal = true, hotDealDiscount = 10),
                
                Flight(fromCity = "Екатеринбург", toCity = "Москва", basePrice = 6000.0, duration = 140, airline = "Уральские авиалинии", departureTime = "07:00", arrivalTime = "09:20", isHotDeal = false),
                Flight(fromCity = "Екатеринбург", toCity = "Москва", basePrice = 6100.0, duration = 138, airline = "Уральские авиалинии", departureTime = "10:00", arrivalTime = "12:18", isHotDeal = false),
                Flight(fromCity = "Екатеринбург", toCity = "Москва", basePrice = 6300.0, duration = 137, airline = "S7 Airlines", departureTime = "12:00", arrivalTime = "14:17", isHotDeal = false),
                Flight(fromCity = "Екатеринбург", toCity = "Москва", basePrice = 6400.0, duration = 136, airline = "Аэрофлот", departureTime = "14:00", arrivalTime = "16:16", isHotDeal = true, hotDealDiscount = 13),
                Flight(fromCity = "Екатеринбург", toCity = "Москва", basePrice = 6500.0, duration = 135, airline = "S7 Airlines", departureTime = "16:00", arrivalTime = "18:15", isHotDeal = false),
                Flight(fromCity = "Екатеринбург", toCity = "Москва", basePrice = 6200.0, duration = 139, airline = "Уральские авиалинии", departureTime = "18:00", arrivalTime = "20:19", isHotDeal = false),
                Flight(fromCity = "Екатеринбург", toCity = "Москва", basePrice = 6600.0, duration = 134, airline = "Аэрофлот", departureTime = "20:00", arrivalTime = "22:14", isHotDeal = true, hotDealDiscount = 11),
                
                // Москва - Новосибирск (длинные рейсы, много вариантов)
                Flight(fromCity = "Москва", toCity = "Новосибирск", basePrice = 8500.0, duration = 240, airline = "S7 Airlines", departureTime = "05:00", arrivalTime = "09:00", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Новосибирск", basePrice = 8600.0, duration = 238, airline = "S7 Airlines", departureTime = "07:30", arrivalTime = "11:28", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Новосибирск", basePrice = 8800.0, duration = 237, airline = "Аэрофлот", departureTime = "09:00", arrivalTime = "12:57", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Новосибирск", basePrice = 9000.0, duration = 235, airline = "Аэрофлот", departureTime = "12:00", arrivalTime = "15:55", isHotDeal = true, hotDealDiscount = 30),
                Flight(fromCity = "Москва", toCity = "Новосибирск", basePrice = 8700.0, duration = 239, airline = "Уральские авиалинии", departureTime = "14:00", arrivalTime = "17:59", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Новосибирск", basePrice = 9100.0, duration = 234, airline = "Аэрофлот", departureTime = "16:30", arrivalTime = "20:24", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Новосибирск", basePrice = 8900.0, duration = 236, airline = "S7 Airlines", departureTime = "18:00", arrivalTime = "21:56", isHotDeal = true, hotDealDiscount = 25),
                Flight(fromCity = "Москва", toCity = "Новосибирск", basePrice = 9200.0, duration = 233, airline = "Аэрофлот", departureTime = "20:00", arrivalTime = "23:53", isHotDeal = false),
                
                Flight(fromCity = "Новосибирск", toCity = "Москва", basePrice = 8500.0, duration = 240, airline = "S7 Airlines", departureTime = "06:00", arrivalTime = "10:00", isHotDeal = false),
                Flight(fromCity = "Новосибирск", toCity = "Москва", basePrice = 8600.0, duration = 238, airline = "S7 Airlines", departureTime = "09:00", arrivalTime = "12:58", isHotDeal = false),
                Flight(fromCity = "Новосибирск", toCity = "Москва", basePrice = 8800.0, duration = 237, airline = "Аэрофлот", departureTime = "11:00", arrivalTime = "14:57", isHotDeal = false),
                Flight(fromCity = "Новосибирск", toCity = "Москва", basePrice = 9000.0, duration = 235, airline = "Аэрофлот", departureTime = "13:00", arrivalTime = "16:55", isHotDeal = true, hotDealDiscount = 28),
                Flight(fromCity = "Новосибирск", toCity = "Москва", basePrice = 8700.0, duration = 239, airline = "Уральские авиалинии", departureTime = "15:00", arrivalTime = "18:59", isHotDeal = false),
                Flight(fromCity = "Новосибирск", toCity = "Москва", basePrice = 9100.0, duration = 234, airline = "Аэрофлот", departureTime = "17:00", arrivalTime = "20:54", isHotDeal = false),
                Flight(fromCity = "Новосибирск", toCity = "Москва", basePrice = 8900.0, duration = 236, airline = "S7 Airlines", departureTime = "19:00", arrivalTime = "22:56", isHotDeal = true, hotDealDiscount = 23),
                
                // Санкт-Петербург - Сочи (много вариантов)
                Flight(fromCity = "Санкт-Петербург", toCity = "Сочи", basePrice = 6000.0, duration = 180, airline = "Россия", departureTime = "07:00", arrivalTime = "10:00", isHotDeal = false),
                Flight(fromCity = "Санкт-Петербург", toCity = "Сочи", basePrice = 6100.0, duration = 178, airline = "Россия", departureTime = "08:00", arrivalTime = "10:58", isHotDeal = false),
                Flight(fromCity = "Санкт-Петербург", toCity = "Сочи", basePrice = 6300.0, duration = 177, airline = "Аэрофлот", departureTime = "10:30", arrivalTime = "13:27", isHotDeal = true, hotDealDiscount = 16),
                Flight(fromCity = "Санкт-Петербург", toCity = "Сочи", basePrice = 6200.0, duration = 179, airline = "S7 Airlines", departureTime = "13:00", arrivalTime = "15:59", isHotDeal = false),
                Flight(fromCity = "Санкт-Петербург", toCity = "Сочи", basePrice = 6400.0, duration = 176, airline = "Аэрофлот", departureTime = "15:00", arrivalTime = "17:56", isHotDeal = false),
                
                Flight(fromCity = "Сочи", toCity = "Санкт-Петербург", basePrice = 6000.0, duration = 180, airline = "Россия", departureTime = "08:00", arrivalTime = "11:00", isHotDeal = false),
                Flight(fromCity = "Сочи", toCity = "Санкт-Петербург", basePrice = 6100.0, duration = 178, airline = "Россия", departureTime = "12:00", arrivalTime = "14:58", isHotDeal = true, hotDealDiscount = 15),
                Flight(fromCity = "Сочи", toCity = "Санкт-Петербург", basePrice = 6300.0, duration = 177, airline = "Аэрофлот", departureTime = "14:00", arrivalTime = "16:57", isHotDeal = false),
                Flight(fromCity = "Сочи", toCity = "Санкт-Петербург", basePrice = 6200.0, duration = 179, airline = "S7 Airlines", departureTime = "16:00", arrivalTime = "18:59", isHotDeal = false),
                Flight(fromCity = "Сочи", toCity = "Санкт-Петербург", basePrice = 6400.0, duration = 176, airline = "Аэрофлот", departureTime = "18:00", arrivalTime = "20:56", isHotDeal = true, hotDealDiscount = 13),
                
                // Москва - Краснодар (много вариантов)
                Flight(fromCity = "Москва", toCity = "Краснодар", basePrice = 4500.0, duration = 130, airline = "Победа", departureTime = "07:00", arrivalTime = "09:10", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Краснодар", basePrice = 4600.0, duration = 128, airline = "Победа", departureTime = "09:00", arrivalTime = "11:08", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Краснодар", basePrice = 4800.0, duration = 127, airline = "S7 Airlines", departureTime = "11:00", arrivalTime = "13:07", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Краснодар", basePrice = 5000.0, duration = 125, airline = "Аэрофлот", departureTime = "13:00", arrivalTime = "15:05", isHotDeal = true, hotDealDiscount = 20),
                Flight(fromCity = "Москва", toCity = "Краснодар", basePrice = 4700.0, duration = 129, airline = "Уральские авиалинии", departureTime = "15:00", arrivalTime = "17:09", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Краснодар", basePrice = 4900.0, duration = 126, airline = "Аэрофлот", departureTime = "17:00", arrivalTime = "19:06", isHotDeal = false),
                
                Flight(fromCity = "Краснодар", toCity = "Москва", basePrice = 4500.0, duration = 130, airline = "Победа", departureTime = "08:00", arrivalTime = "10:10", isHotDeal = false),
                Flight(fromCity = "Краснодар", toCity = "Москва", basePrice = 4600.0, duration = 128, airline = "Победа", departureTime = "11:00", arrivalTime = "13:08", isHotDeal = false),
                Flight(fromCity = "Краснодар", toCity = "Москва", basePrice = 4800.0, duration = 127, airline = "S7 Airlines", departureTime = "13:00", arrivalTime = "15:07", isHotDeal = true, hotDealDiscount = 18),
                Flight(fromCity = "Краснодар", toCity = "Москва", basePrice = 5000.0, duration = 125, airline = "Аэрофлот", departureTime = "15:00", arrivalTime = "17:05", isHotDeal = false),
                Flight(fromCity = "Краснодар", toCity = "Москва", basePrice = 4700.0, duration = 129, airline = "Уральские авиалинии", departureTime = "17:00", arrivalTime = "19:09", isHotDeal = false),
                Flight(fromCity = "Краснодар", toCity = "Москва", basePrice = 4900.0, duration = 126, airline = "Аэрофлот", departureTime = "19:00", arrivalTime = "21:06", isHotDeal = true, hotDealDiscount = 16),
                
                // Москва - Калининград (много вариантов)
                Flight(fromCity = "Москва", toCity = "Калининград", basePrice = 5000.0, duration = 130, airline = "S7 Airlines", departureTime = "08:00", arrivalTime = "10:10", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Калининград", basePrice = 5100.0, duration = 128, airline = "S7 Airlines", departureTime = "10:00", arrivalTime = "12:08", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Калининград", basePrice = 5300.0, duration = 127, airline = "Аэрофлот", departureTime = "12:00", arrivalTime = "14:07", isHotDeal = true, hotDealDiscount = 15),
                Flight(fromCity = "Москва", toCity = "Калининград", basePrice = 5200.0, duration = 129, airline = "Уральские авиалинии", departureTime = "14:00", arrivalTime = "16:09", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Калининград", basePrice = 5400.0, duration = 126, airline = "Аэрофлот", departureTime = "16:00", arrivalTime = "18:06", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Калининград", basePrice = 5100.0, duration = 128, airline = "Победа", departureTime = "18:00", arrivalTime = "20:08", isHotDeal = true, hotDealDiscount = 10),
                
                Flight(fromCity = "Калининград", toCity = "Москва", basePrice = 5000.0, duration = 130, airline = "S7 Airlines", departureTime = "09:00", arrivalTime = "11:10", isHotDeal = false),
                Flight(fromCity = "Калининград", toCity = "Москва", basePrice = 5100.0, duration = 128, airline = "S7 Airlines", departureTime = "12:00", arrivalTime = "14:08", isHotDeal = false),
                Flight(fromCity = "Калининград", toCity = "Москва", basePrice = 5300.0, duration = 127, airline = "Аэрофлот", departureTime = "14:00", arrivalTime = "16:07", isHotDeal = true, hotDealDiscount = 14),
                Flight(fromCity = "Калининград", toCity = "Москва", basePrice = 5200.0, duration = 129, airline = "Уральские авиалинии", departureTime = "16:00", arrivalTime = "18:09", isHotDeal = false),
                Flight(fromCity = "Калининград", toCity = "Москва", basePrice = 5400.0, duration = 126, airline = "Аэрофлот", departureTime = "18:00", arrivalTime = "20:06", isHotDeal = false),
                Flight(fromCity = "Калининград", toCity = "Москва", basePrice = 5100.0, duration = 128, airline = "Победа", departureTime = "20:00", arrivalTime = "22:08", isHotDeal = true, hotDealDiscount = 10),
                
                // Рейсы с пересадками - Москва -> Владивосток (много вариантов)
                Flight(fromCity = "Москва", toCity = "Владивосток", basePrice = 12000.0, duration = 480, airline = "Аэрофлот", hasTransfers = true, transferCity = "Новосибирск", transferDuration = 120, departureTime = "06:00", arrivalTime = "14:00", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Владивосток", basePrice = 11500.0, duration = 495, airline = "Аэрофлот", hasTransfers = true, transferCity = "Новосибирск", transferDuration = 90, departureTime = "08:00", arrivalTime = "16:45", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Владивосток", basePrice = 11000.0, duration = 510, airline = "S7 Airlines", hasTransfers = true, transferCity = "Екатеринбург", transferDuration = 90, departureTime = "07:00", arrivalTime = "16:30", isHotDeal = true, hotDealDiscount = 35),
                Flight(fromCity = "Москва", toCity = "Владивосток", basePrice = 11300.0, duration = 500, airline = "S7 Airlines", hasTransfers = true, transferCity = "Казань", transferDuration = 105, departureTime = "09:00", arrivalTime = "18:20", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Владивосток", basePrice = 11700.0, duration = 485, airline = "Аэрофлот", hasTransfers = true, transferCity = "Новосибирск", transferDuration = 75, departureTime = "10:00", arrivalTime = "18:45", isHotDeal = true, hotDealDiscount = 28),
                
                Flight(fromCity = "Владивосток", toCity = "Москва", basePrice = 12000.0, duration = 480, airline = "Аэрофлот", hasTransfers = true, transferCity = "Новосибирск", transferDuration = 120, departureTime = "08:00", arrivalTime = "16:00", isHotDeal = false),
                Flight(fromCity = "Владивосток", toCity = "Москва", basePrice = 11500.0, duration = 495, airline = "Аэрофлот", hasTransfers = true, transferCity = "Новосибирск", transferDuration = 90, departureTime = "10:00", arrivalTime = "18:45", isHotDeal = false),
                Flight(fromCity = "Владивосток", toCity = "Москва", basePrice = 11000.0, duration = 510, airline = "S7 Airlines", hasTransfers = true, transferCity = "Екатеринбург", transferDuration = 90, departureTime = "09:00", arrivalTime = "18:30", isHotDeal = true, hotDealDiscount = 33),
                Flight(fromCity = "Владивосток", toCity = "Москва", basePrice = 11300.0, duration = 500, airline = "S7 Airlines", hasTransfers = true, transferCity = "Казань", transferDuration = 105, departureTime = "11:00", arrivalTime = "20:20", isHotDeal = false),
                Flight(fromCity = "Владивосток", toCity = "Москва", basePrice = 11700.0, duration = 485, airline = "Аэрофлот", hasTransfers = true, transferCity = "Новосибирск", transferDuration = 75, departureTime = "12:00", arrivalTime = "20:45", isHotDeal = true, hotDealDiscount = 26),
                
                // Рейсы с пересадками - Москва -> Сочи (много вариантов)
                Flight(fromCity = "Москва", toCity = "Сочи", basePrice = 4500.0, duration = 200, airline = "Победа", hasTransfers = true, transferCity = "Краснодар", transferDuration = 60, departureTime = "06:00", arrivalTime = "09:20", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Сочи", basePrice = 4600.0, duration = 205, airline = "Уральские авиалинии", hasTransfers = true, transferCity = "Ростов-на-Дону", transferDuration = 80, departureTime = "08:00", arrivalTime = "12:05", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Сочи", basePrice = 4700.0, duration = 195, airline = "S7 Airlines", hasTransfers = true, transferCity = "Краснодар", transferDuration = 45, departureTime = "10:00", arrivalTime = "13:15", isHotDeal = true, hotDealDiscount = 12),
                Flight(fromCity = "Москва", toCity = "Сочи", basePrice = 4800.0, duration = 210, airline = "Победа", hasTransfers = true, transferCity = "Минеральные Воды", transferDuration = 90, departureTime = "12:00", arrivalTime = "16:30", isHotDeal = false),
                
                Flight(fromCity = "Сочи", toCity = "Москва", basePrice = 4500.0, duration = 200, airline = "Победа", hasTransfers = true, transferCity = "Краснодар", transferDuration = 60, departureTime = "07:00", arrivalTime = "10:20", isHotDeal = false),
                Flight(fromCity = "Сочи", toCity = "Москва", basePrice = 4600.0, duration = 205, airline = "Уральские авиалинии", hasTransfers = true, transferCity = "Ростов-на-Дону", transferDuration = 80, departureTime = "09:00", arrivalTime = "13:05", isHotDeal = false),
                Flight(fromCity = "Сочи", toCity = "Москва", basePrice = 4700.0, duration = 195, airline = "S7 Airlines", hasTransfers = true, transferCity = "Краснодар", transferDuration = 45, departureTime = "11:00", arrivalTime = "14:15", isHotDeal = true, hotDealDiscount = 11),
                Flight(fromCity = "Сочи", toCity = "Москва", basePrice = 4800.0, duration = 210, airline = "Победа", hasTransfers = true, transferCity = "Минеральные Воды", transferDuration = 90, departureTime = "13:00", arrivalTime = "17:30", isHotDeal = false),
                
                // Москва -> Новосибирск с пересадкой (несколько вариантов)
                Flight(fromCity = "Москва", toCity = "Новосибирск", basePrice = 7500.0, duration = 300, airline = "Уральские авиалинии", hasTransfers = true, transferCity = "Екатеринбург", transferDuration = 60, departureTime = "06:00", arrivalTime = "11:00", isHotDeal = true, hotDealDiscount = 22),
                Flight(fromCity = "Москва", toCity = "Новосибирск", basePrice = 7600.0, duration = 310, airline = "S7 Airlines", hasTransfers = true, transferCity = "Казань", transferDuration = 90, departureTime = "08:00", arrivalTime = "14:10", isHotDeal = false),
                Flight(fromCity = "Москва", toCity = "Новосибирск", basePrice = 7700.0, duration = 295, airline = "Уральские авиалинии", hasTransfers = true, transferCity = "Екатеринбург", transferDuration = 45, departureTime = "10:00", arrivalTime = "14:55", isHotDeal = false),
                
                Flight(fromCity = "Новосибирск", toCity = "Москва", basePrice = 7500.0, duration = 300, airline = "Уральские авиалинии", hasTransfers = true, transferCity = "Екатеринбург", transferDuration = 60, departureTime = "07:00", arrivalTime = "12:00", isHotDeal = true, hotDealDiscount = 21),
                Flight(fromCity = "Новосибирск", toCity = "Москва", basePrice = 7600.0, duration = 310, airline = "S7 Airlines", hasTransfers = true, transferCity = "Казань", transferDuration = 90, departureTime = "09:00", arrivalTime = "15:10", isHotDeal = false),
                Flight(fromCity = "Новосибирск", toCity = "Москва", basePrice = 7700.0, duration = 295, airline = "Уральские авиалинии", hasTransfers = true, transferCity = "Екатеринбург", transferDuration = 45, departureTime = "11:00", arrivalTime = "15:55", isHotDeal = false),
                
                // Москва -> Санкт-Петербург с пересадкой (варианты)
                Flight(fromCity = "Москва", toCity = "Санкт-Петербург", basePrice = 3200.0, duration = 180, airline = "Победа", hasTransfers = true, transferCity = "Казань", transferDuration = 120, departureTime = "05:00", arrivalTime = "08:00", isHotDeal = false),
                Flight(fromCity = "Санкт-Петербург", toCity = "Москва", basePrice = 3200.0, duration = 180, airline = "Победа", hasTransfers = true, transferCity = "Казань", transferDuration = 120, departureTime = "06:00", arrivalTime = "09:00", isHotDeal = true, hotDealDiscount = 8),
                
                // Казань -> Санкт-Петербург с пересадкой (варианты)
                Flight(fromCity = "Казань", toCity = "Санкт-Петербург", basePrice = 4300.0, duration = 200, airline = "Победа", hasTransfers = true, transferCity = "Москва", transferDuration = 120, departureTime = "06:00", arrivalTime = "10:20", isHotDeal = false),
                Flight(fromCity = "Казань", toCity = "Санкт-Петербург", basePrice = 4400.0, duration = 195, airline = "Уральские авиалинии", hasTransfers = true, transferCity = "Москва", transferDuration = 90, departureTime = "08:00", arrivalTime = "12:15", isHotDeal = true, hotDealDiscount = 10),
                Flight(fromCity = "Казань", toCity = "Санкт-Петербург", basePrice = 4500.0, duration = 210, airline = "S7 Airlines", hasTransfers = true, transferCity = "Екатеринбург", transferDuration = 180, departureTime = "10:00", arrivalTime = "15:30", isHotDeal = false),
                
                Flight(fromCity = "Санкт-Петербург", toCity = "Казань", basePrice = 4300.0, duration = 200, airline = "Победа", hasTransfers = true, transferCity = "Москва", transferDuration = 120, departureTime = "07:00", arrivalTime = "11:20", isHotDeal = false),
                Flight(fromCity = "Санкт-Петербург", toCity = "Казань", basePrice = 4400.0, duration = 195, airline = "Уральские авиалинии", hasTransfers = true, transferCity = "Москва", transferDuration = 90, departureTime = "09:00", arrivalTime = "13:15", isHotDeal = true, hotDealDiscount = 12),
                Flight(fromCity = "Санкт-Петербург", toCity = "Казань", basePrice = 4500.0, duration = 210, airline = "S7 Airlines", hasTransfers = true, transferCity = "Екатеринбург", transferDuration = 180, departureTime = "11:00", arrivalTime = "16:30", isHotDeal = false),
                
                // Москва -> Екатеринбург с пересадкой
                Flight(fromCity = "Москва", toCity = "Екатеринбург", basePrice = 5500.0, duration = 220, airline = "Победа", hasTransfers = true, transferCity = "Казань", transferDuration = 90, departureTime = "05:00", arrivalTime = "09:40", isHotDeal = false),
                Flight(fromCity = "Екатеринбург", toCity = "Москва", basePrice = 5500.0, duration = 220, airline = "Победа", hasTransfers = true, transferCity = "Казань", transferDuration = 90, departureTime = "06:00", arrivalTime = "10:40", isHotDeal = true, hotDealDiscount = 8)
            )
            
            // Обновляем все рейсы, добавляя недостающие поля по умолчанию
            val flightsWithDefaults = directFlights.map { flight ->
                if (flight.fromAirport.isBlank() || flight.toAirport.isBlank()) {
                    flight.copy(
                        fromAirport = getAirport(flight.fromCity),
                        toAirport = getAirport(flight.toCity),
                        includesBaggage = if (flight.airline == "Победа") flight.includesBaggage else true
                    )
                } else {
                    flight
                }
            }
            
            println("populateDatabase: Вставляем ${flightsWithDefaults.size} рейсов")
            flightDao.insertAll(flightsWithDefaults)
            println("populateDatabase: Рейсы вставлены")
            
            // Получаем ID вставленных рейсов и создаем места
            val allFlights = flightDao.getAllFlights()
            println("populateDatabase: Всего рейсов для создания мест: ${allFlights.size}")
            println("populateDatabase: Получено ${allFlights.size} рейсов из базы для создания мест")
            allFlights.forEachIndexed { index, flight ->
                val seats = generateSeatsForFlight(flight.id)
                seatDao.insertAll(seats)
                if (index < 3) {
                    println("populateDatabase: Создано ${seats.size} мест для рейса ${flight.id} (${flight.fromCity} -> ${flight.toCity})")
                }
            }
            println("populateDatabase: Заполнение базы данных завершено. Всего рейсов: ${allFlights.size}")
        }
        
        private fun generateSeatsForFlight(flightId: Int): List<Seat> {
            val seats = mutableListOf<Seat>()
            val random = Random.Default
            
            // Создаем самолет: 25 рядов, 6 мест в ряду (A, B, C, D, E, F)
            val positions = listOf("A", "B", "C", "D", "E", "F")
            
            for (row in 1..25) {
                positions.forEach { position ->
                    val seatNumber = "$row$position"
                    val seatType = when {
                        row <= 5 -> "business"
                        row <= 10 -> "comfort"
                        else -> "economy"
                    }
                    
                    // Модификатор цены: окно (A, F) и места у экстренного выхода (12-13 ряды) дороже
                    val priceModifier = when {
                        (position == "A" || position == "F") && row in 12..13 -> 1.3 // Окно у экстренного выхода
                        position == "A" || position == "F" -> 1.1 // Окно
                        row in 12..13 -> 1.2 // Экстренный выход
                        position == "C" || position == "D" -> 1.05 // Проход
                        else -> 1.0
                    }
                    
                    // Рандомно занимаем ~30% мест
                    val isOccupied = random.nextDouble() < 0.3
                    
                    seats.add(Seat(
                        flightId = flightId,
                        seatNumber = seatNumber,
                        seatType = seatType,
                        rowNumber = row,
                        position = position,
                        isOccupied = isOccupied,
                        priceModifier = priceModifier
                    ))
                }
            }
            
            return seats
        }
    }
}