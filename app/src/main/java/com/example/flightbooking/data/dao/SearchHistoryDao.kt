package com.example.flightbooking.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.flightbooking.data.entity.SearchHistory

@Dao
interface SearchHistoryDao {
    @Insert
    suspend fun insert(history: SearchHistory)

    @Query("SELECT * FROM search_history WHERE userId = :userId ORDER BY searchDate DESC LIMIT 10")
    fun getHistory(userId: Int): LiveData<List<SearchHistory>>

    @Query("SELECT * FROM search_history WHERE userId = :userId ORDER BY searchDate DESC LIMIT :limit")
    suspend fun getRecentSearches(userId: Int, limit: Int = 5): List<SearchHistory>

    @Query("DELETE FROM search_history WHERE userId = :userId")
    suspend fun clearHistory(userId: Int)

    @Query("SELECT DISTINCT fromCity, toCity FROM search_history WHERE userId = :userId ORDER BY searchDate DESC LIMIT 5")
    suspend fun getRecentRoutes(userId: Int): List<RecentRoute>
}

data class RecentRoute(
    val fromCity: String,
    val toCity: String
)