package com.ridesync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.ridesync.ui.auth.*

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    val authState by authViewModel.uiState.collectAsState()

                    when (val state = authState) {
                        is AuthState.Idle, is AuthState.Authenticating -> {
                            LoginScreen(
                                onGoogleSignInClick = { idToken ->
                                    authViewModel.onGoogleIdTokenReceived(idToken)
                                },
                                isLoading = state is AuthState.Authenticating,
                                errorMessage = null
                            )
                        }

                        is AuthState.Error -> {
                            LoginScreen(
                                onGoogleSignInClick = { idToken ->
                                    authViewModel.onGoogleIdTokenReceived(idToken)
                                },
                                isLoading = false,
                                errorMessage = state.message
                            )
                        }

                        is AuthState.ProfileSetupRequired -> {
                            ProfileSetupScreen(
                                initialDisplayName = state.user.displayName ?: "Rider",
                                onSaveProfile = { vehicle, tank, shareLoc, phone ->
                                    authViewModel.saveUserProfile(vehicle, tank, shareLoc, phone)
                                },
                                isLoading = false
                            )
                        }

                        is AuthState.Authenticated -> {
                            MainAuthenticatedScreen(userProfile = state.userProfile)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainAuthenticatedScreen(userProfile: com.ridesync.data.model.UserProfile) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Welcome to RideSync, ${userProfile.displayName}!\nVehicle: ${userProfile.vehicleModel}",
            color = Color.White,
            fontSize = 20.sp
        )
    }
}
