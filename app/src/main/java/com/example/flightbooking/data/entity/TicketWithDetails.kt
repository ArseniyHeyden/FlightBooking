package com.example.flightbooking.data.entity

import androidx.room.Embedded

data class TicketWithDetails(
    @Embedded
    val ticket: Ticket,

    @Embedded(prefix = "flight_")
    val flight: Flight
)