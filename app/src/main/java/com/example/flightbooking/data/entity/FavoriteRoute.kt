package com.example.flightbooking.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorite_routes",
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
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId"),
        Index("flightId"),
        Index(value = ["userId", "flightId"], unique = true)
    ]
)
data class FavoriteRoute(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val flightId: Int,
    val addedDate: String = ""
)