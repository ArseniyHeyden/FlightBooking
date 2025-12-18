package com.example.flightbooking.data.dao

import androidx.room.*
import com.example.flightbooking.data.entity.Seat

@Dao
interface SeatDao {
    @Insert
    suspend fun insert(seat: Seat): Long
    
    @Insert
    suspend fun insertAll(seats: List<Seat>)
    
    @Query("SELECT * FROM seats WHERE flightId = :flightId ORDER BY rowNumber, position")
    suspend fun getSeatsForFlight(flightId: Int): List<Seat>
    
    @Query("SELECT * FROM seats WHERE flightId = :flightId AND isOccupied = 0 ORDER BY rowNumber, position")
    suspend fun getAvailableSeatsForFlight(flightId: Int): List<Seat>
    
    @Query("SELECT * FROM seats WHERE flightId = :flightId AND seatNumber = :seatNumber")
    suspend fun getSeatByNumber(flightId: Int, seatNumber: String): Seat?
    
    @Update
    suspend fun updateSeat(seat: Seat)
    
    @Query("UPDATE seats SET isOccupied = 1 WHERE flightId = :flightId AND seatNumber = :seatNumber")
    suspend fun occupySeat(flightId: Int, seatNumber: String)
    
    @Query("UPDATE seats SET isOccupied = 0 WHERE flightId = :flightId AND seatNumber = :seatNumber")
    suspend fun freeSeat(flightId: Int, seatNumber: String)
}
