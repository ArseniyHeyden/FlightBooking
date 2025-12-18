package com.example.flightbooking.data.entity

data class Passenger(
    val id: Int = 0,
    val passengerNumber: Int, // Порядковый номер пассажира (1, 2, 3...)
    val name: String,
    val age: Int, // Возраст в годах
    val passengerType: String, // adult, child, infant
    val seatClass: String = "economy" // economy, comfort, business
) {
    companion object {
        const val TYPE_ADULT = "adult"
        const val TYPE_CHILD = "child"
        const val TYPE_INFANT = "infant"

        // Определяет тип пассажира по возрасту
        fun getPassengerType(age: Int): String {
            return when {
                age >= 12 -> TYPE_ADULT
                age >= 2 -> TYPE_CHILD
                else -> TYPE_INFANT
            }
        }

        // Коэффициент цены для типа пассажира
        fun getPriceCoefficient(passengerType: String): Double {
            return when (passengerType) {
                TYPE_ADULT -> 1.0
                TYPE_CHILD -> 0.75  // 25% скидка для детей
                TYPE_INFANT -> 0.1   // 90% скидка для младенцев
                else -> 1.0
            }
        }
    }
}