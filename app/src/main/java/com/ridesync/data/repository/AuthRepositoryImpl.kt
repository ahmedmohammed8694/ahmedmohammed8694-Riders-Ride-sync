package com.ridesync.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.ridesync.data.model.AuthRepository
import com.ridesync.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AuthRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    private val cloudflareEdgeUrl = "https://ahmedmohammed8694-riders-ride-sync.mdahmed08061994.workers.dev"

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override fun signInWithGoogleIdToken(idToken: String): Flow<Result<FirebaseUser>> = flow {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                emit(Result.success(user))
            } else {
                emit(Result.failure(Exception("Firebase auth returned null user")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Flow<Result<FirebaseUser>> = flow {
        try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                if (displayName.isNotBlank()) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName.trim())
                        .build()
                    user.updateProfile(profileUpdates).await()
                }
                emit(Result.success(user))
            } else {
                emit(Result.failure(Exception("Email signup returned null user")))
            }
        } catch (e: Exception) {
            Log.w("AuthRepositoryImpl", "Firebase Identity Toolkit blocked/failed. Falling back to Cloudflare Edge Database...", e)
            val cfResult = performCloudflareSignUp(email, password, displayName)
            emit(cfResult)
        }
    }.flowOn(Dispatchers.IO)

    override fun signInWithEmail(
        email: String,
        password: String
    ): Flow<Result<FirebaseUser>> = flow {
        try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                emit(Result.success(user))
            } else {
                emit(Result.failure(Exception("Email login returned null user")))
            }
        } catch (e: Exception) {
            Log.w("AuthRepositoryImpl", "Firebase Auth sign-in failed. Falling back to Cloudflare Edge Auth...", e)
            val cfResult = performCloudflareSignIn(email, password)
            emit(cfResult)
        }
    }.flowOn(Dispatchers.IO)

    override fun sendPasswordResetEmail(email: String): Flow<Result<Unit>> = flow {
        try {
            auth.sendPasswordResetEmail(email).await()
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun fetchUserProfile(userId: String): Flow<Result<UserProfile?>> = flow {
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            if (snapshot.exists()) {
                val profile = try {
                    snapshot.toObject(UserProfile::class.java)
                } catch (e: Exception) {
                    Log.e("AuthRepositoryImpl", "Failed to deserialize UserProfile document for $userId", e)
                    null
                }
                emit(Result.success(profile))
            } else {
                val cfProfile = fetchCloudflareProfile(userId)
                emit(Result.success(cfProfile))
            }
        } catch (e: Exception) {
            Log.w("AuthRepositoryImpl", "Firestore fetch error, fetching from Cloudflare Database...", e)
            val cfProfile = fetchCloudflareProfile(userId)
            emit(Result.success(cfProfile))
        }
    }.flowOn(Dispatchers.IO)

    override fun saveUserProfile(userProfile: UserProfile): Flow<Result<Unit>> = flow {
        try {
            firestore.collection("users")
                .document(userProfile.userId)
                .set(userProfile)
                .await()
            saveCloudflareProfile(userProfile)
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Log.w("AuthRepositoryImpl", "Firestore save failed. Saving profile to Cloudflare Edge Database...", e)
            val saved = saveCloudflareProfile(userProfile)
            if (saved) {
                emit(Result.success(Unit))
            } else {
                emit(Result.failure(e))
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun signOut() {
        auth.signOut()
    }

    // --- Cloudflare Edge Database Network Calls ---

    private fun performCloudflareSignUp(email: String, pass: String, name: String): Result<FirebaseUser> {
        return try {
            val url = URL("$cloudflareEdgeUrl/api/auth/signup")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("email", email)
                put("password", pass)
                put("displayName", name)
            }.toString()

            OutputStreamWriter(conn.outputStream).use { it.write(payload) }

            if (conn.responseCode in 200..299) {
                val current = auth.currentUser
                if (current != null) {
                    Result.success(current)
                } else {
                    Result.failure(Exception("Registered on Cloudflare Database successfully. Please Sign In to continue."))
                }
            } else {
                Result.failure(Exception("Cloudflare Edge Auth Sign-Up failed (HTTP ${conn.responseCode})"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepositoryImpl", "Cloudflare Sign-Up Network Error", e)
            Result.failure(e)
        }
    }

    private fun performCloudflareSignIn(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val url = URL("$cloudflareEdgeUrl/api/auth/signin")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("email", email)
                put("password", pass)
            }.toString()

            OutputStreamWriter(conn.outputStream).use { it.write(payload) }

            if (conn.responseCode in 200..299) {
                val current = auth.currentUser
                if (current != null) {
                    Result.success(current)
                } else {
                    Result.failure(Exception("Signed in on Cloudflare Edge Auth."))
                }
            } else {
                Result.failure(Exception("Cloudflare Sign-In failed (HTTP ${conn.responseCode})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveCloudflareProfile(profile: UserProfile): Boolean {
        return try {
            val url = URL("$cloudflareEdgeUrl/api/auth/profile")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("userId", profile.userId)
                put("displayName", profile.displayName)
                put("vehicleModel", profile.vehicleModel)
                put("fuelTankCapacityLiters", profile.fuelTankCapacityLiters)
                put("shareRealtimeLocation", profile.shareRealtimeLocation)
                put("emergencyContactNumber", profile.emergencyContactNumber)
            }.toString()

            OutputStreamWriter(conn.outputStream).use { it.write(payload) }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }

    private fun fetchCloudflareProfile(userId: String): UserProfile? {
        return try {
            val url = URL("$cloudflareEdgeUrl/api/auth/profile?userId=$userId")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Content-Type", "application/json")

            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                if (json.has("profile") && !json.isNull("profile")) {
                    val p = json.getJSONObject("profile")
                    UserProfile(
                        userId = p.optString("userId", userId),
                        displayName = p.optString("displayName", "Rider"),
                        vehicleModel = p.optString("vehicleModel", "Motorcycle"),
                        fuelTankCapacityLiters = p.optDouble("fuelTankCapacityLiters", 15.0).toFloat(),
                        shareRealtimeLocation = p.optBoolean("shareRealtimeLocation", true),
                        emergencyContactNumber = p.optString("emergencyContactNumber", "")
                    )
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
