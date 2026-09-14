package com.ridesync.ui.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.ridesync.data.model.ConvoyMember
import com.ridesync.data.model.RiderLocationPing
import com.ridesync.data.model.RiderStatus
import com.ridesync.data.model.StopEvent

@Composable
fun ConvoyMapScreen(
    routePolyline: List<LatLng>,
    riderLocations: Map<String, RiderLocationPing>,
    convoyMembers: Map<String, ConvoyMember>,
    stopEvents: List<StopEvent>,
    modifier: Modifier = Modifier
) {
    val initialPos = routePolyline.firstOrNull() ?: LatLng(37.7749, -122.4194)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPos, 14f)
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = true,
            compassEnabled = true
        ),
        properties = MapProperties(
            isMyLocationEnabled = true
        )
    ) {
        // Render polyline
        if (routePolyline.isNotEmpty()) {
            Polyline(
                points = routePolyline,
                color = Color(0xFF38BDF8),
                width = 14f,
                geodesic = true
            )
        }

        // Render dynamic rider markers with interpolated position & status halo
        riderLocations.forEach { (userId, ping) ->
            val member = convoyMembers[userId]
            val status = member?.status ?: RiderStatus.RIDING

            InterpolatedRiderMarker(
                targetLocation = LatLng(ping.latitude, ping.longitude),
                bearing = ping.bearing,
                displayName = member?.displayName ?: "Rider",
                status = status
            )
        }

        // Render stop event flags
        stopEvents.forEach { stop ->
            Marker(
                state = MarkerState(position = LatLng(stop.latitude, stop.longitude)),
                title = "Stop: ${stop.reason.name}",
                snippet = "Rider: ${stop.riderName}"
            )
        }
    }
}

@Composable
private fun InterpolatedRiderMarker(
    targetLocation: LatLng,
    bearing: Float,
    displayName: String,
    status: RiderStatus
) {
    var previousLocation by remember { mutableStateOf(targetLocation) }
    val animFraction = remember { Animatable(0f) }

    LaunchedEffect(targetLocation) {
        animFraction.snapTo(0f)
        animFraction.animateTo(1f, animationSpec = tween(durationMillis = 400))
        previousLocation = targetLocation
    }

    val currentLatLng = LatLngEvaluator.interpolate(
        animFraction.value,
        previousLocation,
        targetLocation
    )

    val haloColor = when (status) {
        RiderStatus.RIDING -> Color(0xFF22C55E) // Green
        RiderStatus.STOPPED -> Color(0xFFF59E0B) // Amber
        RiderStatus.DELAYED -> Color(0xFFF97316) // Orange
        RiderStatus.SOS -> Color(0xFFEF4444) // Red
    }

    MarkerComposable(
        state = MarkerState(position = currentLatLng),
        title = displayName
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(haloColor.copy(alpha = 0.3f), CircleShape)
                    .border(3.dp, haloColor, CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(bearing)
                )
            }
            Text(
                text = displayName,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color(0xCC0F172A), shape = CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
