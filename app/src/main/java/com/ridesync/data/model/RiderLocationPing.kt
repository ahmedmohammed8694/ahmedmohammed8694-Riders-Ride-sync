package com.ridesync.data.model

data class RiderLocationPing(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speedKmh: Float = 0f,
    val bearing: Float = 0f,
    val altitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val alongTrackProgressMeters: Double = 0.0,
    val isBuffered: Boolean = false
)
