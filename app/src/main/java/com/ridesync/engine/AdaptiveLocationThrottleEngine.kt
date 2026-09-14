package com.ridesync.engine

import android.location.Location
import com.ridesync.data.model.RiderLocationPing

enum class KinematicState(
    val gpsIntervalMs: Long,
    val minUpdateIntervalMs: Long,
    val minDistanceMeters: Float,
    val maxDispatchIntervalMs: Long
) {
    HIGH_SPEED(
        gpsIntervalMs = 3_000L,
        minUpdateIntervalMs = 1_500L,
        minDistanceMeters = 50f,
        maxDispatchIntervalMs = 6_000L
    ),
    MODERATE_SPEED(
        gpsIntervalMs = 5_000L,
        minUpdateIntervalMs = 2_500L,
        minDistanceMeters = 25f,
        maxDispatchIntervalMs = 8_000L
    ),
    STATIONARY(
        gpsIntervalMs = 20_000L,
        minUpdateIntervalMs = 10_000L,
        minDistanceMeters = 0f,
        maxDispatchIntervalMs = 60_000L
    )
}

class AdaptiveLocationThrottleEngine {

    private var currentState: KinematicState = KinematicState.MODERATE_SPEED
    private var lastDispatchedPing: RiderLocationPing? = null
    private var stationaryStartTimeMs: Long = 0L

    fun evaluateKinematicState(currentSpeedKmh: Float, currentTimeMs: Long): KinematicState {
        val newState = when {
            currentSpeedKmh > 50f -> {
                stationaryStartTimeMs = 0L
                KinematicState.HIGH_SPEED
            }
            currentSpeedKmh in 15f..50f -> {
                stationaryStartTimeMs = 0L
                KinematicState.MODERATE_SPEED
            }
            else -> { // Speed < 15 km/h (Low/Stationary)
                if (currentSpeedKmh < 3f) {
                    if (stationaryStartTimeMs == 0L) {
                        stationaryStartTimeMs = currentTimeMs
                    }
                    if (currentTimeMs - stationaryStartTimeMs >= 60_000L) {
                        KinematicState.STATIONARY
                    } else {
                        KinematicState.MODERATE_SPEED
                    }
                } else {
                    stationaryStartTimeMs = 0L
                    KinematicState.MODERATE_SPEED
                }
            }
        }

        currentState = newState
        return newState
    }

    fun shouldDispatchPing(newPing: RiderLocationPing): Boolean {
        val lastPing = lastDispatchedPing ?: run {
            lastDispatchedPing = newPing
            return true
        }

        val timeDiffMs = newPing.timestamp - lastPing.timestamp
        val distanceMeters = calculateDistanceMeters(
            lastPing.latitude, lastPing.longitude,
            newPing.latitude, newPing.longitude
        )

        val shouldDispatch = when (currentState) {
            KinematicState.HIGH_SPEED -> {
                distanceMeters >= KinematicState.HIGH_SPEED.minDistanceMeters ||
                        timeDiffMs >= KinematicState.HIGH_SPEED.maxDispatchIntervalMs
            }
            KinematicState.MODERATE_SPEED -> {
                distanceMeters >= KinematicState.MODERATE_SPEED.minDistanceMeters ||
                        timeDiffMs >= KinematicState.MODERATE_SPEED.maxDispatchIntervalMs
            }
            KinematicState.STATIONARY -> {
                timeDiffMs >= KinematicState.STATIONARY.maxDispatchIntervalMs
            }
        }

        if (shouldDispatch) {
            lastDispatchedPing = newPing
        }

        return shouldDispatch
    }

    private fun calculateDistanceMeters(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(startLat, startLng, endLat, endLng, results)
        return results[0]
    }

    fun getCurrentState(): KinematicState = currentState
}
