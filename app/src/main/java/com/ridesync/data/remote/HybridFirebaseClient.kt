package com.ridesync.data.remote

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.ridesync.data.model.ConvoyMember
import com.ridesync.data.model.RiderLocationPing
import com.ridesync.data.model.RiderStatus
import com.ridesync.data.model.StopEvent
import com.ridesync.data.model.TripMetadata
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class HybridFirebaseClient(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val rtdb: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    fun observeTripMetadata(tripId: String): Flow<TripMetadata?> = callbackFlow {
        val docRef = firestore.collection("trips").document(tripId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing trip metadata", error)
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val trip = snapshot.toObject(TripMetadata::class.java)
                trySend(trip)
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeLiveConvoyTelemetry(tripId: String): Flow<Map<String, RiderLocationPing>> = callbackFlow {
        val telemetryRef = rtdb.getReference("trips").child(tripId).child("telemetry")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pingsMap = mutableMapOf<String, RiderLocationPing>()
                for (riderSnap in snapshot.children) {
                    val userId = riderSnap.key ?: continue
                    val lat = riderSnap.child("lat").getValue(Double::class.java) ?: 0.0
                    val lng = riderSnap.child("lng").getValue(Double::class.java) ?: 0.0
                    val speed = riderSnap.child("speedKmh").getValue(Float::class.java) ?: 0f
                    val bearing = riderSnap.child("bearing").getValue(Float::class.java) ?: 0f
                    val timestamp = riderSnap.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                    val progress = riderSnap.child("progressMeters").getValue(Double::class.java) ?: 0.0

                    pingsMap[userId] = RiderLocationPing(
                        latitude = lat,
                        longitude = lng,
                        speedKmh = speed,
                        bearing = bearing,
                        timestamp = timestamp,
                        alongTrackProgressMeters = progress
                    )
                }
                trySend(pingsMap)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Telemetry RTDB stream cancelled", error.toException())
            }
        }
        telemetryRef.addValueEventListener(listener)
        awaitClose { telemetryRef.removeEventListener(listener) }
    }

    fun observeStopEvents(tripId: String): Flow<List<StopEvent>> = callbackFlow {
        val stopsCollection = firestore.collection("trips")
            .document(tripId)
            .collection("stops")

        val registration = stopsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing stops", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val stops = snapshot?.documents?.mapNotNull { it.toObject(StopEvent::class.java) } ?: emptyList()
            trySend(stops)
        }
        awaitClose { registration.remove() }
    }

    fun updateRiderStatus(tripId: String, userId: String, status: RiderStatus): Flow<Result<Unit>> = flow {
        try {
            firestore.collection("trips")
                .document(tripId)
                .update("members.$userId.status", status.name)
                .await()
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun postStopEvent(stopEvent: StopEvent): Flow<Result<Unit>> = flow {
        try {
            val stopRef = firestore.collection("trips")
                .document(stopEvent.tripId)
                .collection("stops")
                .document()

            val finalEvent = stopEvent.copy(stopId = stopRef.id)
            stopRef.set(finalEvent).await()
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun joinTrip(tripId: String, member: ConvoyMember): Flow<Result<Unit>> = flow {
        try {
            firestore.collection("trips")
                .document(tripId)
                .update("members.${member.userId}", member)
                .await()
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    companion object {
        private const val TAG = "HybridFirebaseClient"
    }
}
