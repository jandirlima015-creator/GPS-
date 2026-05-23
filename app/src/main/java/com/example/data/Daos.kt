package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {
    @Query("SELECT * FROM daily_step_records ORDER BY date DESC")
    fun getAllStepRecords(): Flow<List<DailyStepRecord>>

    @Query("SELECT * FROM daily_step_records WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: String): DailyStepRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DailyStepRecord)

    @Query("DELETE FROM daily_step_records")
    suspend fun clearAllRecords()
}

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY steps DESC")
    fun getAllFriends(): Flow<List<Friend>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: Friend)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(friends: List<Friend>)

    @Query("DELETE FROM friends WHERE id = :id")
    suspend fun deleteFriend(id: String)

    @Query("DELETE FROM friends")
    suspend fun clearAllFriends()
}

@Dao
interface LocationDao {
    @Query("SELECT * FROM location_points ORDER BY timestamp ASC")
    fun getAllTrackPoints(): Flow<List<LocationPoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: LocationPoint)

    @Query("DELETE FROM location_points")
    suspend fun clearAllPoints()
}
