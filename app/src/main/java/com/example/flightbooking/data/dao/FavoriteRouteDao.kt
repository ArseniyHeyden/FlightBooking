package com.example.flightbooking.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.flightbooking.data.entity.FavoriteRoute

@Dao
interface FavoriteRouteDao {
    @Insert
    suspend fun insert(favorite: FavoriteRoute)

    @Delete
    suspend fun delete(favorite: FavoriteRoute)

    @Update
    suspend fun update(favorite: FavoriteRoute)

    @Query("SELECT * FROM favorite_routes WHERE userId = :userId AND flightId = :flightId")
    suspend fun getFavorite(userId: Int, flightId: Int): FavoriteRoute?

    @Query("SELECT * FROM favorite_routes WHERE userId = :userId")
    fun getUserFavorites(userId: Int): LiveData<List<FavoriteRoute>>

    @Query("SELECT * FROM favorite_routes WHERE userId = :userId")
    suspend fun getUserFavoritesSuspend(userId: Int): List<FavoriteRoute>

    @Query("SELECT COUNT(*) FROM favorite_routes WHERE userId = :userId")
    suspend fun getFavoritesCount(userId: Int): Int
}