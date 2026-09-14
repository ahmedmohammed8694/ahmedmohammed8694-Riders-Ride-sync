package com.ridesync.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
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

    override fun fetchUserProfile(userId: String): Flow<Result<UserProfile?>> = flow {
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            if (snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
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
