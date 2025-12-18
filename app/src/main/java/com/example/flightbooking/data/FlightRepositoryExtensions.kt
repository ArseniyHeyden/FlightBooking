package com.example.flightbooking.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Расширения для FlightRepository с общими методами
 */

fun FlightRepository.getAvailableDestinations(
    fromCity: String,
    callback: (List<String>) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        val allFlights = getAllFlights()
        val destinations = allFlights
            .filter { it.fromCity.equals(fromCity, ignoreCase = true) }
            .map { it.toCity }
            .distinct()
            .sorted()
        callback(destinations)
    }
}

fun FlightRepository.getAvailableOrigins(
    toCity: String,
    callback: (List<String>) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        val allFlights = getAllFlights()
        val origins = allFlights
            .filter { it.toCity.equals(toCity, ignoreCase = true) }
            .map { it.fromCity }
            .distinct()
            .sorted()
        callback(origins)
    }
}