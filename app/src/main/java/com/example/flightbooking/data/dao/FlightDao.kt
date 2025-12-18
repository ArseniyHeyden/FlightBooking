package com.example.flightbooking.data.dao

import androidx.room.*
import com.example.flightbooking.data.entity.Flight

@Dao
interface FlightDao {
    @Insert
    suspend fun insertAll(flights: List<Flight>)

    @Query("SELECT * FROM flights WHERE LOWER(TRIM(fromCity)) = LOWER(TRIM(:from)) AND LOWER(TRIM(toCity)) = LOWER(TRIM(:to)) AND hasTransfers = 0 ORDER BY basePrice ASC")
    suspend fun searchFlights(from: String, to: String): List<Flight>
    
    @Query("SELECT * FROM flights WHERE isHotDeal = 1 ORDER BY hotDealDiscount DESC LIMIT :limit")
    suspend fun getHotDeals(limit: Int = 10): List<Flight>
    
    @Query("SELECT * FROM flights WHERE hasTransfers = 1 AND LOWER(TRIM(fromCity)) = LOWER(TRIM(:from)) AND LOWER(TRIM(toCity)) = LOWER(TRIM(:to))")
    suspend fun searchFlightsWithTransfers(from: String, to: String): List<Flight>

    @Query("SELECT * FROM flights")
    suspend fun getAllFlights(): List<Flight>

    @Query("SELECT * FROM flights WHERE id = :id")
    suspend fun getFlightById(id: Int): Flight?
}