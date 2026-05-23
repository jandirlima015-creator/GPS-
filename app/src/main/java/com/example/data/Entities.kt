package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_step_records")
data class DailyStepRecord(
    @PrimaryKey val date: String, // format: "YYYY-MM-DD"
    val steps: Int,
    val distanceMeters: Double,
    val caloriesKcal: Double
)

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey val id: String,
    val name: String,
    val avatarColorIdx: Int, // Index of color in Theme palette
    val latitude: Double,
    val longitude: Double,
    val steps: Int,
    val isOnline: Boolean,
    val lastUpdateEpoch: Long
)

@Entity(tableName = "location_points")
data class LocationPoint(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)
