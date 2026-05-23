package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class StepDetector(
    context: Context,
    private val onStepDetected: (Int) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val accelerometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var initialStepCount = -1f
    private var lastAccelerometerStepTime: Long = 0
    private val accelerometerStepThreshold = 12.0f // m/s^2 trigger
    private val stepDelayMs = 350L

    fun startListening() {
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
        } else if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val currentSteps = event.values[0]
                if (initialStepCount < 0f) {
                    initialStepCount = currentSteps
                }
                val delta = (currentSteps - initialStepCount).toInt()
                if (delta > 0) {
                    onStepDetected(delta)
                    initialStepCount = currentSteps // track differences incrementally
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt(x * x + y * y + z * z)

                if (magnitude > accelerometerStepThreshold) {
                    val now = System.currentTimeMillis()
                    if (now - lastAccelerometerStepTime > stepDelayMs) {
                        lastAccelerometerStepTime = now
                        onStepDetected(1)
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action needed
    }
}
