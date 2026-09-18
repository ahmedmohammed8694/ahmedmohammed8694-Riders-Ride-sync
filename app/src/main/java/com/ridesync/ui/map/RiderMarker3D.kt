package com.ridesync.ui.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.ridesync.data.model.RiderStatus
import com.ridesync.ui.theme.HudColors

/**
 * 3D Beveled Glowing Rider Avatar Puck Marker with Directional Bearing Arrow & Status Halo Ring.
 */
@Composable
fun InterpolatedRiderMarker3D(
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

    // Status Halo Color Pair (Core + Outer Glow)
    val (statusCore, statusGlow) = when (status) {
        RiderStatus.RIDING -> HudColors.StatusRiding to HudColors.StatusRidingGlow
        RiderStatus.STOPPED -> HudColors.StatusStopped to HudColors.StatusStoppedGlow
        RiderStatus.DELAYED -> HudColors.StatusDelayed to HudColors.StatusDelayedGlow
        RiderStatus.SOS -> HudColors.StatusSos to HudColors.StatusSosGlow
    }

    // Pulsing SOS effect if status == SOS
    val pulseTransition = rememberInfiniteTransition(label = "sos_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sos_alpha"
    )

    val haloAlpha = if (status == RiderStatus.SOS) pulseAlpha else 0.4f

    MarkerComposable(
        state = MarkerState(position = currentLatLng),
        title = displayName
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 3D Beveled Avatar Puck
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .shadow(elevation = 10.dp, shape = CircleShape, clip = false)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(statusGlow.copy(alpha = haloAlpha), statusCore.copy(alpha = 0.1f))
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 3.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(statusGlow, statusCore)
                        ),
                        shape = CircleShape
                    )
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF1F2937), Color(0xFF0B0F19))
                        )
                    )
            ) {
                // Directional 3D Arrow Puck
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = statusGlow,
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(bearing)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Frosted Glass Name Badge
            Box(
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(HudColors.FrostedOverlay)
                    .border(1.dp, statusCore.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = displayName,
                    color = HudColors.TextCrispWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
