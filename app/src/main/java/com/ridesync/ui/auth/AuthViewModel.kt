package com.ridesync.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.ridesync.data.model.AuthRepository
import com.ridesync.data.model.PrivacySettings
import com.ridesync.data.model.UserProfile
import com.ridesync.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Idle : AuthState
    data object Authenticating : AuthState
    data class ProfileSetupRequired(val user: FirebaseUser) : AuthState
    data class Authenticated(val userProfile: UserProfile) : AuthState
    data class Error(val message: String) : AuthState
}

class AuthViewModel(
    private val repository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    init {
        checkExistingSession()
    }

    fun checkExistingSession() {
        val user = repository.currentUser
        if (user != null) {
            loadUserProfile(user)
        } else {
            _uiState.value = AuthState.Idle
        }
    }

    fun onGoogleIdTokenReceived(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthState.Authenticating
            repository.signInWithGoogleIdToken(idToken).collect { result ->
                result.fold(
                    onSuccess = { firebaseUser ->
                        loadUserProfile(firebaseUser)
                    },
                    onFailure = { throwable ->
                        _uiState.value = AuthState.Error(
                            throwable.localizedMessage ?: "Google Sign-In failed"
                        )
                    }
                )
            }
        }
    }

    private fun loadUserProfile(firebaseUser: FirebaseUser) {
        viewModelScope.launch {
            repository.fetchUserProfile(firebaseUser.uid).collect { result ->
                result.fold(
                    onSuccess = { profile ->
                        if (profile != null) {
                            _uiState.value = AuthState.Authenticated(profile)
                        } else {
                            _uiState.value = AuthState.ProfileSetupRequired(firebaseUser)
                        }
                    },
                    onFailure = { throwable ->
                        _uiState.value = AuthState.Error(
                            "Failed to load profile: ${throwable.localizedMessage}"
                        )
                    }
                )
            }
        }
    }

    fun saveUserProfile(
        vehicleModel: String,
        tankCapacityLiters: Double,
        shareLocationWithGroup: Boolean,
        emergencyContactPhone: String
    ) {
        val currentState = _uiState.value
        val firebaseUser = when (currentState) {
            is AuthState.ProfileSetupRequired -> currentState.user
            is AuthState.Authenticated -> repository.currentUser
            else -> repository.currentUser
        } ?: run {
            _uiState.value = AuthState.Error("No authenticated session found")
            return
        }

        val newProfile = UserProfile(
            userId = firebaseUser.uid,
            displayName = firebaseUser.displayName ?: "Rider",
            email = firebaseUser.email ?: "",
            photoUrl = firebaseUser.photoUrl?.toString() ?: "",
            vehicleModel = vehicleModel.trim(),
            tankCapacityLiters = tankCapacityLiters,
            privacySettings = PrivacySettings(
                shareLocationWithGroup = shareLocationWithGroup,
                emergencyContactPhone = emergencyContactPhone.trim()
            )
        )

        viewModelScope.launch {
            _uiState.value = AuthState.Authenticating
            repository.saveUserProfile(newProfile).collect { result ->
                result.fold(
                    onSuccess = {
                        _uiState.value = AuthState.Authenticated(newProfile)
                    },
                    onFailure = { throwable ->
                        _uiState.value = AuthState.Error(
                            "Failed to save profile: ${throwable.localizedMessage}"
                        )
                    }
                )
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _uiState.value = AuthState.Idle
    }
}
