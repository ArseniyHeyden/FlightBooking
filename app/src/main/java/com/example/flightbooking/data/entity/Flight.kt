package com.example.flightbooking.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flights")
data class Flight(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fromCity: String,
    val toCity: String,
    val basePrice: Double,
    val duration: Int,
    val airline: String,
    val hasTransfers: Boolean = false,
    val transferCity: String? = null, // Город пересадки, если есть
    val transferDuration: Int = 0, // Длительность пересадки в минутах
    val isHotDeal: Boolean = false, // Горячий билет со скидкой
    val hotDealDiscount: Int = 0, // Процент скидки для горячего билета (0-50)
    val departureTime: String = "10:00", // Время вылета
    val arrivalTime: String = "12:00", // Время прилета
    val includesBaggage: Boolean = true, // Включен ли багаж (hand luggage + checked baggage)
    val fromAirport: String = "", // Название аэропорта отправления
    val toAirport: String = "" // Название аэропорта прибытия
)