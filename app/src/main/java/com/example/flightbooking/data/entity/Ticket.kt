package com.example.flightbooking.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tickets",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Flight::class,
            parentColumns = ["id"],
            childColumns = ["flightId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("userId"),
        Index("flightId"),
        Index("bookingDate"),
        Index("isPaid")
    ]
)
data class Ticket(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val flightId: Int,
    val departureDate: String,
    val returnDate: String? = null,
    val seatNumber: String? = null,
    val passengerName: String? = null,  // Изменено на nullable
    val passengerDocument: String? = null,  // Изменено на nullable
    val finalPrice: Double,
    val isPaid: Boolean = false,
    val paymentDate: String? = null,
    val bookingDate: String,
    val status: String? = null  // Изменено на nullable
) {
    companion object {
        const val STATUS_BOOKED = "booked"
        const val STATUS_PAID = "paid"
        const val STATUS_CANCELLED = "cancelled"
        const val STATUS_COMPLETED = "completed"

        // Хелпер для получения дефолтного статуса
        fun getDefaultStatus(): String = STATUS_BOOKED
    }

    // Хелпер для безопасного получения статуса
    fun getStatusOrDefault(): String = status ?: STATUS_BOOKED

    // Хелпер для безопасного получения имени пассажира
    fun getPassengerNameOrEmpty(): String = passengerName ?: ""

    // Хелпер для безопасного получения документа
    fun getPassengerDocumentOrEmpty(): String = passengerDocument ?: ""
}