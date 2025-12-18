package com.example.flightbooking.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "seats",
    foreignKeys = [
        ForeignKey(
            entity = Flight::class,
            parentColumns = ["id"],
            childColumns = ["flightId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("flightId")]
)
data class Seat(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val flightId: Int,
    val seatNumber: String, // Например "1A", "12B"
    val seatType: String = "economy", // economy, comfort, business
    val rowNumber: Int,
    val position: String, // A, B, C, D, E, F
    val isOccupied: Boolean = false, // Занято ли место
    val priceModifier: Double = 1.0 // Модификатор цены (окно/проход/экстренный выход)
)
