package com.ridesync.data.repository

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

class AuthRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

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
            emit(Result.failure(e))
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
            emit(Result.failure(e))
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
                    android.util.Log.e("AuthRepositoryImpl", "Failed to deserialize UserProfile document for $userId", e)
                    null
                }
                emit(Result.success(profile))
            } else {
                emit(Result.success(null))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun saveUserProfile(userProfile: UserProfile): Flow<Result<Unit>> = flow {
        try {
            firestore.collection("users")
                .document(userProfile.userId)
                .set(userProfile)
                .await()
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun signOut() {
        auth.signOut()
    }
}
