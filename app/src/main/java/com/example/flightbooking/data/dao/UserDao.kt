package com.example.flightbooking.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.flightbooking.data.entity.User

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Query("UPDATE users SET totalTrips = totalTrips + 1, totalSpent = totalSpent + :amount WHERE id = :userId")
    suspend fun updateUserStats(userId: Int, amount: Double)

    @Query("UPDATE users SET discountLevel = :level WHERE id = :userId")
    suspend fun updateDiscountLevel(userId: Int, level: Int)

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUser(userId: Int): LiveData<User?>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserSuspend(userId: Int): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT discountLevel FROM users WHERE id = :userId")
    suspend fun getUserDiscountLevel(userId: Int): Int

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
    suspend fun emailExists(email: String): Boolean
}