package com.example.flightbooking.data.entity

data class FlightWithDetails(
    val flight: Flight,
    val isFavorite: Boolean = false
)