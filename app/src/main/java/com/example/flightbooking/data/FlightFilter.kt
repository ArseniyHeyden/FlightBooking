package com.example.flightbooking.data

import com.example.flightbooking.data.entity.Flight

class FlightFilter {

    /**
     * Фильтрует рейсы по цене
     */
    fun filterByPrice(flights: List<Flight>, minPrice: Double?, maxPrice: Double?): List<Flight> {
        return flights.filter { flight ->
            (minPrice == null || flight.basePrice >= minPrice) &&
                    (maxPrice == null || flight.basePrice <= maxPrice)
        }
    }

    /**
     * Фильтрует рейсы по авиакомпании
     */
    fun filterByAirline(flights: List<Flight>, airlines: List<String>): List<Flight> {
        return if (airlines.isEmpty()) {
            flights
        } else {
            flights.filter { flight ->
                airlines.any { airline ->
                    flight.airline.contains(airline, ignoreCase = true)
                }
            }
        }
    }

    /**
     * Фильтрует рейсы по времени в пути
     */
    fun filterByDuration(flights: List<Flight>, maxDuration: Int?): List<Flight> {
        return flights.filter { flight ->
            maxDuration == null || flight.duration <= maxDuration
        }
    }

    /**
     * Сортирует рейсы по цене (по возрастанию)
     */
    fun sortByPriceAscending(flights: List<Flight>): List<Flight> {
        return flights.sortedBy { it.basePrice }
    }

    /**
     * Сортирует рейсы по цене (по убыванию)
     */
    fun sortByPriceDescending(flights: List<Flight>): List<Flight> {
        return flights.sortedByDescending { it.basePrice }
    }

    /**
     * Сортирует рейсы по времени в пути
     */
    fun sortByDuration(flights: List<Flight>): List<Flight> {
        return flights.sortedBy { it.duration }
    }

    /**
     * Сортирует рейсы по авиакомпании
     */
    fun sortByAirline(flights: List<Flight>): List<Flight> {
        return flights.sortedBy { it.airline }
    }

    /**
     * Получает уникальные авиакомпании из списка рейсов
     */
    fun getUniqueAirlines(flights: List<Flight>): List<String> {
        return flights.map { it.airline }.distinct().sorted()
    }
}