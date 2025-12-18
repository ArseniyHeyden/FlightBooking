package com.example.flightbooking.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.flightbooking.data.entity.Ticket
import com.example.flightbooking.data.entity.TicketWithDetails

@Dao
interface TicketDao {
    @Insert
    suspend fun insert(ticket: Ticket): Long

    @Update
    suspend fun update(ticket: Ticket)

    @Query("SELECT * FROM tickets WHERE id = :ticketId")
    suspend fun getTicketById(ticketId: Int): Ticket?
    
    @Query("UPDATE tickets SET status = :status WHERE id = :ticketId")
    suspend fun updateStatus(ticketId: Int, status: String)

    @Query("UPDATE tickets SET isPaid = 1, paymentDate = :paymentDate, status = :status WHERE id = :ticketId")
    suspend fun markAsPaid(ticketId: Int, paymentDate: String, status: String = Ticket.STATUS_PAID)

    @Query("SELECT * FROM tickets WHERE userId = :userId ORDER BY bookingDate DESC")
    fun getUserTickets(userId: Int): LiveData<List<Ticket>>

    @Query("SELECT * FROM tickets WHERE userId = :userId AND isPaid = 1")
    suspend fun getPaidTickets(userId: Int): List<Ticket>

    @Query("SELECT * FROM tickets WHERE userId = :userId AND isPaid = 0")
    suspend fun getUnpaidTickets(userId: Int): List<Ticket>

    @Query("SELECT * FROM tickets WHERE userId = :userId AND status = :status")
    suspend fun getTicketsByStatus(userId: Int, status: String): List<Ticket>

    @Transaction
    @Query("""
        SELECT 
            tickets.*,
            flights.id AS flight_id,
            flights.fromCity AS flight_fromCity,
            flights.toCity AS flight_toCity,
            flights.basePrice AS flight_basePrice,
            flights.duration AS flight_duration,
            flights.airline AS flight_airline,
            flights.hasTransfers AS flight_hasTransfers,
            flights.transferCity AS flight_transferCity,
            flights.transferDuration AS flight_transferDuration,
            flights.isHotDeal AS flight_isHotDeal,
            flights.hotDealDiscount AS flight_hotDealDiscount,
            flights.departureTime AS flight_departureTime,
            flights.arrivalTime AS flight_arrivalTime,
            flights.includesBaggage AS flight_includesBaggage,
            flights.fromAirport AS flight_fromAirport,
            flights.toAirport AS flight_toAirport
        FROM tickets 
        INNER JOIN flights ON tickets.flightId = flights.id
        WHERE tickets.userId = :userId
        ORDER BY tickets.bookingDate DESC
    """)
    fun getTicketsWithDetails(userId: Int): LiveData<List<TicketWithDetails>>

    @Query("DELETE FROM tickets WHERE bookingDate < :date AND isPaid = 0")
    suspend fun cleanupExpiredBookings(date: String)

    @Query("SELECT COUNT(*) FROM tickets WHERE userId = :userId AND isPaid = 1")
    suspend fun getPaidTicketsCount(userId: Int): Int

    @Query("SELECT SUM(finalPrice) FROM tickets WHERE userId = :userId AND isPaid = 1")
    suspend fun getTotalSpent(userId: Int): Double?
}