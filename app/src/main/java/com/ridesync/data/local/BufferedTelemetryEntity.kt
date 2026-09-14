package com.ridesync.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buffered_telemetry")
data class BufferedTelemetryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripId: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float,
    val bearing: Float,
    val altitude: Double,
    val timestamp: Long,
    val alongTrackProgressMeters: Double
)
