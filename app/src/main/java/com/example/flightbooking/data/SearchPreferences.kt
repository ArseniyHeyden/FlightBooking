package com.example.flightbooking.data

data class SearchPreferences(
    val adultCount: Int = 1,
    val childCount: Int = 0,
    val infantCount: Int = 0,
    val seatClass: String = "economy", // economy, comfort, business
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val maxDuration: Int? = null,
    val selectedAirlines: List<String> = emptyList(),
    val sortBy: String = "price_asc" // price_asc, price_desc, duration, departure
) {
    val passengerCount: Int
        get() = adultCount + childCount + infantCount

    fun getPassengerTypes(): List<String> {
        val types = mutableListOf<String>()
        repeat(adultCount) { types.add("adult") }
        repeat(childCount) { types.add("child") }
        repeat(infantCount) { types.add("infant") }
        return types
    }

    fun copyWithDefaults(): SearchPreferences {
        return if (adultCount == 0 && childCount == 0 && infantCount == 0) {
            this.copy(adultCount = 1)
        } else {
            this
        }
    }
}