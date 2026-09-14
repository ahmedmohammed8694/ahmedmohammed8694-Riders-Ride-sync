package com.ridesync.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.ridesync.data.local.BufferedTelemetryEntity
import com.ridesync.data.local.RideSyncDatabase
import com.ridesync.data.local.TelemetryDao
import com.ridesync.data.model.RiderLocationPing
import com.ridesync.util.NetworkMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class TelemetryBufferRepository(
    context: Context,
    private val database: RideSyncDatabase = RideSyncDatabase.getInstance(context),
    private val networkMonitor: NetworkMonitor = NetworkMonitor(context),
    private val firebaseRtdb: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    private val telemetryDao: TelemetryDao = database.telemetryDao()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isFlushing = false

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    init {
        networkMonitor.startMonitoring()
        observeNetworkAndFlush()
    }

    fun processIncomingPing(tripId: String, userId: String, ping: RiderLocationPing) {
        repositoryScope.launch {
            if (isOnline.value) {
                val success = sendPingToFirebase(tripId, userId, ping)
                if (!success) {
                    bufferPingLocally(tripId, userId, ping)
                }
            } else {
                bufferPingLocally(tripId, userId, ping)
            }
        }
    }

    private suspend fun bufferPingLocally(tripId: String, userId: String, ping: RiderLocationPing) {
        val entity = BufferedTelemetryEntity(
            tripId = tripId,
            userId = userId,
            latitude = ping.latitude,
            longitude = ping.longitude,
            speedKmh = ping.speedKmh,
            bearing = ping.bearing,
            altitude = ping.altitude,
            timestamp = ping.timestamp,
            alongTrackProgressMeters = ping.alongTrackProgressMeters
        )
        telemetryDao.insertPing(entity)
        Log.d(TAG, "Buffered telemetry ping locally (Timestamp: ${ping.timestamp})")
    }

    private fun observeNetworkAndFlush() {
        repositoryScope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online && !isFlushing) {
                    flushBufferedTelemetryFifo()
                }
            }
        }
    }

    private suspend fun flushBufferedTelemetryFifo() {
        if (isFlushing) return
        isFlushing = true

        try {
            var bufferedCount = telemetryDao.getBufferedCount()
            Log.d(TAG, "Starting FIFO buffer flush. Total pending pings: $bufferedCount")

            while (bufferedCount > 0 && isOnline.value) {
                val batch = telemetryDao.getFifoBatch(batchSize = 50)
                if (batch.isEmpty()) break

                val successfullySentIds = mutableListOf<Long>()

                for (entity in batch) {
                    val ping = RiderLocationPing(
                        latitude = entity.latitude,
                        longitude = entity.longitude,
                        speedKmh = entity.speedKmh,
                        bearing = entity.bearing,
                        altitude = entity.altitude,
                        timestamp = entity.timestamp,
                        alongTrackProgressMeters = entity.alongTrackProgressMeters,
                        isBuffered = true
                    )

                    val sent = sendPingToFirebase(entity.tripId, entity.userId, ping)
                    if (sent) {
                        successfullySentIds.add(entity.id)
                    } else {
                        break // Break batch if network fails mid-flush
                    }
                }

                if (successfullySentIds.isNotEmpty()) {
                    telemetryDao.deletePingsByIds(successfullySentIds)
                    Log.d(TAG, "Flushed ${successfullySentIds.size} pings from local buffer")
                } else {
                    break
                }

                bufferedCount = telemetryDao.getBufferedCount()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during telemetry buffer flush", e)
        } finally {
            isFlushing = false
        }
    }

    private suspend fun sendPingToFirebase(
        tripId: String,
        userId: String,
        ping: RiderLocationPing
    ): Boolean {
        return try {
            val pingRef = firebaseRtdb.getReference("trips")
                .child(tripId)
                .child("telemetry")
                .child(userId)

            val updateMap = mapOf(
                "lat" to ping.latitude,
                "lng" to ping.longitude,
                "speedKmh" to ping.speedKmh,
                "bearing" to ping.bearing,
                "timestamp" to ping.timestamp,
                "progressMeters" to ping.alongTrackProgressMeters,
                "isBuffered" to ping.isBuffered
            )

            pingRef.setValue(updateMap).await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to transmit ping to RTDB", e)
            false
        }
    }

    fun cleanup() {
        networkMonitor.stopMonitoring()
        repositoryScope.cancel()
    }

    companion object {
        private const val TAG = "TelemetryBufferRepo"
    }
}
