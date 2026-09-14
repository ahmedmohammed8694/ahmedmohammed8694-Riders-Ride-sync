package com.ridesync.data.model

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: FirebaseUser?
    
    fun signInWithGoogleIdToken(idToken: String): Flow<Result<FirebaseUser>>
    fun fetchUserProfile(userId: String): Flow<Result<UserProfile?>>
    fun saveUserProfile(userProfile: UserProfile): Flow<Result<Unit>>
    fun signOut()
}
