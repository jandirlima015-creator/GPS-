package com.example.data

import kotlinx.coroutines.flow.Flow

class Repository(private val database: AppDatabase) {
    private val stepDao = database.stepDao()
    private val friendDao = database.friendDao()
    private val locationDao = database.locationDao()

    val allStepRecords: Flow<List<DailyStepRecord>> = stepDao.getAllStepRecords()
    val allFriends: Flow<List<Friend>> = friendDao.getAllFriends()
    val allTrackPoints: Flow<List<LocationPoint>> = locationDao.getAllTrackPoints()

    suspend fun getRecordByDate(date: String): DailyStepRecord? {
        return stepDao.getRecordByDate(date)
    }

    suspend fun insertStepRecord(record: DailyStepRecord) {
        stepDao.insertRecord(record)
    }

    suspend fun clearStepRecords() {
        stepDao.clearAllRecords()
    }

    suspend fun insertFriend(friend: Friend) {
        friendDao.insertFriend(friend)
    }

    suspend fun insertFriends(friends: List<Friend>) {
        friendDao.insertFriends(friends)
    }

    suspend fun deleteFriend(id: String) {
        friendDao.deleteFriend(id)
    }

    suspend fun clearAllFriends() {
        friendDao.clearAllFriends()
    }

    suspend fun insertLocationPoint(point: LocationPoint) {
        locationDao.insertPoint(point)
    }

    suspend fun clearAllLocationPoints() {
        locationDao.clearAllPoints()
    }
}
