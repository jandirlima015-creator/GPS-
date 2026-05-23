package com.example.ui

import android.app.Application
import android.content.Context
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.sensor.StepDetector
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*
import kotlin.random.Random

class StepMapViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = Repository(database)

    // Current date identifier
    private val currentDate: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // UI States
    private val _userSteps = MutableStateFlow(0)
    val userSteps: StateFlow<Int> = _userSteps.asStateFlow()

    private val _userLatitude = MutableStateFlow(-23.550520) // Default: São Paulo
    val userLatitude: StateFlow<Double> = _userLatitude.asStateFlow()

    private val _userLongitude = MutableStateFlow(-46.633308)
    val userLongitude: StateFlow<Double> = _userLongitude.asStateFlow()

    // Tracking state
    private val _isTrackingLocation = MutableStateFlow(false)
    val isTrackingLocation: StateFlow<Boolean> = _isTrackingLocation.asStateFlow()

    // Sensor & Geofencing Simulation Status
    private val _isSimulatingMovement = MutableStateFlow(true)
    val isSimulatingMovement: StateFlow<Boolean> = _isSimulatingMovement.asStateFlow()

    // Exposed lists from Room
    val stepHistory: StateFlow<List<DailyStepRecord>> = repository.allStepRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friendsList: StateFlow<List<Friend>> = repository.allFriends
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val walkPath: StateFlow<List<LocationPoint>> = repository.allTrackPoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Hardware Step Detector
    private var stepDetector: StepDetector? = null

    // Fine Fused Location Client
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    // Coroutine job for live friend tracking updates
    private var socialSimulationJob: Job? = null

    // For trajectory calculations in manual walks
    private var currentHeadingDegrees = Random.nextDouble(0.0, 360.0)

    init {
        // Load initial steps for today
        loadTodayStepsAndDetails()

        // Setup step sensor
        setupHardwareStepSensor()

        // Populate friends list if database is empty on first launch
        viewModelScope.launch(Dispatchers.IO) {
            repository.allFriends.first().let { currentFriends ->
                if (currentFriends.isEmpty()) {
                    createDefaultFriends()
                }
            }
        }

        // Start real-time social telemetry loops
        startSocialSimulation()
    }

    private fun loadTodayStepsAndDetails() {
        viewModelScope.launch(Dispatchers.IO) {
            val record = repository.getRecordByDate(currentDate)
            if (record != null) {
                _userSteps.value = record.steps
            } else {
                _userSteps.value = 0
                // Create clean first record
                saveStepData(0)
            }
        }
    }

    private fun setupHardwareStepSensor() {
        try {
            stepDetector = StepDetector(getApplication()) { delta ->
                incrementUserSteps(delta)
            }
            stepDetector?.startListening()
        } catch (e: Exception) {
            Log.e("StepMapViewModel", "Error starting step detector: ${e.message}")
        }
    }

    fun incrementUserSteps(delta: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val newSteps = _userSteps.value + delta
            _userSteps.value = newSteps
            saveStepData(newSteps)

            // When user walks, simulate slight position change to show real path tracing!
            if (_isSimulatingMovement.value) {
                advanceUserPositionRandomly(delta)
            }
        }
    }

    private suspend fun saveStepData(steps: Int) {
        val distance = steps * 0.75 // average step distance = 0.75 meters
        val calories = steps * 0.04 // average energy = 0.04 kcal/step
        val record = DailyStepRecord(
            date = currentDate,
            steps = steps,
            distanceMeters = distance,
            caloriesKcal = calories
        )
        repository.insertStepRecord(record)
    }

    private fun advanceUserPositionRandomly(stepDelta: Int) {
        val metersToMove = stepDelta * 0.75
        // Random drift or following heading
        currentHeadingDegrees += Random.nextDouble(-15.0, 15.0)
        val headingRad = Math.toRadians(currentHeadingDegrees)

        // Earth radius in meters
        val earthRadius = 6378137.0
        val dLat = (metersToMove * cos(headingRad)) / earthRadius
        val dLon = (metersToMove * sin(headingRad)) / (earthRadius * cos(Math.toRadians(_userLatitude.value)))

        val newLat = _userLatitude.value + Math.toDegrees(dLat)
        val newLon = _userLongitude.value + Math.toDegrees(dLon)

        updateUserLocation(newLat, newLon)
    }

    fun updateUserLocation(lat: Double, lon: Double) {
        _userLatitude.value = lat
        _userLongitude.value = lon

        viewModelScope.launch(Dispatchers.IO) {
            // Persist the coordinate in the history tracker
            repository.insertLocationPoint(
                LocationPoint(
                    latitude = lat,
                    longitude = lon,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    // Manual walks simulation
    fun simulateWalkAction(stepsCount: Int) {
        incrementUserSteps(stepsCount)
    }

    // Toggle simulation of friend walking behavior
    fun toggleSocialSimulation(enabled: Boolean) {
        _isSimulatingMovement.value = enabled
        if (enabled) {
            startSocialSimulation()
        } else {
            socialSimulationJob?.cancel()
        }
    }

    private fun startSocialSimulation() {
        socialSimulationJob?.cancel()
        socialSimulationJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(3000) // Update coordinates/steps every 3s
                if (_isSimulatingMovement.value) {
                    val friends = repository.allFriends.first()
                    friends.forEach { friend ->
                        if (friend.isOnline) {
                            // Friend takes steps
                            val stepsDelta = Random.nextInt(1, 6)
                            val fNewSteps = friend.steps + stepsDelta

                            // Friend migrates slightly on map near user
                            val friendHeading = Random.nextDouble(0.0, 360.0)
                            val walkMeters = stepsDelta * 0.75
                            val earthRadius = 6378137.0
                            val dLat = (walkMeters * cos(Math.toRadians(friendHeading))) / earthRadius
                            val dLon = (walkMeters * sin(Math.toRadians(friendHeading))) / (earthRadius * cos(Math.toRadians(friend.latitude)))

                            val fNewLat = friend.latitude + Math.toDegrees(dLat)
                            val fNewLon = friend.longitude + Math.toDegrees(dLon)

                            repository.insertFriend(
                                friend.copy(
                                    steps = fNewSteps,
                                    latitude = fNewLat,
                                    longitude = fNewLon,
                                    lastUpdateEpoch = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Setup active hardware GPS tracking
    fun startGpsTracking() {
        val context: Context = getApplication()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000L
        ).apply {
            setMinUpdateIntervalMillis(1500L)
            setMinUpdateDistanceMeters(1.0f)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    updateUserLocation(location.latitude, location.longitude)
                }
            }
        }

        try {
            _isTrackingLocation.value = true
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (unsecured: SecurityException) {
            _isTrackingLocation.value = false
            Log.e("StepMapViewModel", "Location permission missing: ${unsecured.message}")
        }
    }

    fun stopGpsTracking() {
        _isTrackingLocation.value = false
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    // Add friend dynamically
    fun addNewFriend(name: String, colorIdx: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val uuid = "friend_${System.currentTimeMillis()}"
            // Place slightly displaced from the user coordinates so they appear on the map immediately
            val displacementLat = Random.nextDouble(-0.003, 0.003)
            val displacementLon = Random.nextDouble(-0.003, 0.003)

            val fNew = Friend(
                id = uuid,
                name = name,
                avatarColorIdx = colorIdx,
                latitude = _userLatitude.value + displacementLat,
                longitude = _userLongitude.value + displacementLon,
                steps = Random.nextInt(0, 500),
                isOnline = true,
                lastUpdateEpoch = System.currentTimeMillis()
            )
            repository.insertFriend(fNew)
        }
    }

    // Remove friend
    fun removeFriend(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFriend(id)
        }
    }

    // Clear active trail/points
    fun clearGpsTrail() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllLocationPoints()
            // Reset to current location
            updateUserLocation(_userLatitude.value, _userLongitude.value)
        }
    }

    // Initialize mock database entries
    private suspend fun createDefaultFriends() {
        val lat = _userLatitude.value
        val lon = _userLongitude.value

        val defaultFriends = listOf(
            Friend(
                id = "f1",
                name = "Mariana Silva",
                avatarColorIdx = 0, // Orange
                latitude = lat + 0.0015,
                longitude = lon + 0.002,
                steps = 4280,
                isOnline = true,
                lastUpdateEpoch = System.currentTimeMillis()
            ),
            Friend(
                id = "f2",
                name = "Carlos Souza",
                avatarColorIdx = 1, // Green
                latitude = lat - 0.0018,
                longitude = lon - 0.0008,
                steps = 7350,
                isOnline = true,
                lastUpdateEpoch = System.currentTimeMillis()
            ),
            Friend(
                id = "f3",
                name = "Beatriz Lima",
                avatarColorIdx = 2, // Blue
                latitude = lat + 0.0004,
                longitude = lon - 0.0015,
                steps = 1940,
                isOnline = true,
                lastUpdateEpoch = System.currentTimeMillis()
            ),
            Friend(
                id = "f4",
                name = "Rodrigo Santos",
                avatarColorIdx = 3, // Pink
                latitude = lat - 0.0005,
                longitude = lon + 0.0022,
                steps = 8990,
                isOnline = true,
                lastUpdateEpoch = System.currentTimeMillis()
            )
        )
        repository.insertFriends(defaultFriends)
    }

    fun resetDailyStats() {
        viewModelScope.launch(Dispatchers.IO) {
            _userSteps.value = 0
            saveStepData(0)
            repository.clearStepRecords()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stepDetector?.stopListening()
        stopGpsTracking()
        socialSimulationJob?.cancel()
    }
}
