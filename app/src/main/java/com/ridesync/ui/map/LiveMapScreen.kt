package com.ridesync.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.ridesync.data.model.ConvoyMember
import com.ridesync.data.model.RiderLocationPing
import com.ridesync.data.model.RiderStatus
import com.ridesync.data.model.StopEvent
import com.ridesync.ui.theme.HudColors
import com.ridesync.ui.theme.frostedGlassHud
import com.ridesync.ui.theme.hud3dCard
import kotlinx.coroutines.launch

/**
 * Modern High-Contrast Cockpit HUD Live Map Screen.
 * Features 3D tilted camera perspective (45°-55°), multi-layered neon vector route polyline,
 * 3D rider avatar puck markers, and tactile frosted glass overlay controls.
 */
@Composable
fun LiveMapScreen(
    routePolyline: List<LatLng>,
    riderLocations: Map<String, RiderLocationPing>,
    convoyMembers: Map<String, ConvoyMember>,
    stopEvents: List<StopEvent>,
    isOnline: Boolean = true,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val initialPos = routePolyline.firstOrNull() ?: LatLng(17.3753, 78.4344)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.Builder()
            .target(initialPos)
            .zoom(16f)
            .tilt(50f) // Default 3D Tilted Perspective (45°-55°)
            .bearing(0f)
            .build()
    }

    // Interactive Map Options State
    var selectedMapType by remember { mutableStateOf(MapType.NORMAL) }
    var isTrafficEnabled by remember { mutableStateOf(false) }
    var isFollowMode by remember { mutableStateOf(true) }
    var is3dTilt by remember { mutableStateOf(true) }
    var showMapTypePickerModal by remember { mutableStateOf(false) }
    var showKeySetupDialog by remember { mutableStateOf(false) }
    val isDefaultKey = remember {
        com.ridesync.BuildConfig.MAPS_API_KEY == "AIzaSyBYs6gsD4eKkKIvsMMC4YpukGC5XIh9UkU" ||
                com.ridesync.BuildConfig.MAPS_API_KEY.isBlank()
    }

    // Follow lead rider location automatically with 3D tilt and bearing alignment
    val leadRiderPos = remember(riderLocations) {
        val leadPing = riderLocations.values.firstOrNull()
        if (leadPing != null) LatLng(leadPing.latitude, leadPing.longitude) else initialPos
    }

    LaunchedEffect(leadRiderPos, isFollowMode, is3dTilt) {
        if (isFollowMode) {
            val tiltAngle = if (is3dTilt) 50f else 0f
            val bearingAngle = riderLocations.values.firstOrNull()?.bearing ?: 0f
            val targetCam = CameraPosition.Builder()
                .target(leadRiderPos)
                .zoom(16.5f)
                .tilt(tiltAngle)
                .bearing(bearingAngle)
                .build()
            cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(targetCam), 800)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(HudColors.ObsidianCanvas)) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = false
            ),
            properties = MapProperties(
                mapType = selectedMapType,
                isTrafficEnabled = isTrafficEnabled,
                isMyLocationEnabled = false
            )
        ) {
            // Multi-Layered Neon Route Polyline (Glowing Underlayer + Vibrant Core Ribbon)
            if (routePolyline.isNotEmpty()) {
                // Outer Cyan Glow Line (width = 16f, alpha = 0.3)
                Polyline(
                    points = routePolyline,
                    color = HudColors.CyanGlow,
                    width = 16f,
                    geodesic = true
                )
                // Core Sharp Neon Ribbon Line (width = 8f, #06B6D4)
                Polyline(
                    points = routePolyline,
                    color = HudColors.CyanPrimary,
                    width = 8f,
                    geodesic = true
                )
            }

            // Render 3D Rider Markers with Position Interpolation & Status Glow Halos
            riderLocations.forEach { (userId, ping) ->
                val member = convoyMembers[userId]
                val status = member?.status ?: RiderStatus.RIDING

                InterpolatedRiderMarker3D(
                    targetLocation = LatLng(ping.latitude, ping.longitude),
                    bearing = ping.bearing,
                    displayName = member?.displayName ?: "Rider",
                    status = status
                )
            }

            // Render stop event markers
            stopEvents.forEach { stop ->
                Marker(
                    state = MarkerState(position = LatLng(stop.latitude, stop.longitude)),
                    title = "Stop: ${stop.reason.name}",
                    snippet = "Rider: ${stop.riderName}"
                )
            }
        }

        // Top Right: Floating Frosted HUD Map Controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Map Type Selector Button
            HudMapOptionIconButton(
                icon = Icons.Default.Layers,
                contentDescription = "Map Options",
                active = showMapTypePickerModal,
                activeColor = HudColors.StatusStopped,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showMapTypePickerModal = true
                }
            )

            // Follow Mode / Free Pan Lock Toggle
            HudMapOptionIconButton(
                icon = if (isFollowMode) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "Interaction Mode",
                active = isFollowMode,
                activeColor = HudColors.StatusRiding,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    isFollowMode = !isFollowMode
                }
            )

            // 3D Tilt Perspective Toggle (50° Pitch)
            HudMapOptionIconButton(
                icon = Icons.Default.Explore,
                contentDescription = "3D Camera Perspective",
                active = is3dTilt,
                activeColor = HudColors.CyanPrimary,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    is3dTilt = !is3dTilt
                }
            )

            // Traffic Layer Toggle
            HudMapOptionIconButton(
                icon = Icons.Default.Traffic,
                contentDescription = "Traffic Layer",
                active = isTrafficEnabled,
                activeColor = HudColors.StatusSos,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    isTrafficEnabled = !isTrafficEnabled
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Glove-Friendly Zoom In
            HudMapOptionIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Zoom In",
                active = false,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                    }
                }
            )

            // Glove-Friendly Zoom Out
            HudMapOptionIconButton(
                icon = Icons.Default.Remove,
                contentDescription = "Zoom Out",
                active = false,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                    }
                }
            )
        }

        // Top Left: Frosted Glass Interaction Mode & Key Status Badge
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .frostedGlassHud(shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (isFollowMode) HudColors.StatusRiding else HudColors.StatusStopped,
                                CircleShape
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFollowMode) "HUD 3D: LOCKED (LEAD)" else "HUD 3D: FREE PAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HudColors.TextCrispWhite
                    )
                }
            }

            // Map Setup Status Badge
            Box(
                modifier = Modifier
                    .frostedGlassHud(
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = if (isDefaultKey) Color(0xCC991B1B) else Color(0xCC065F46)
                    )
                    .clickable { showKeySetupDialog = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isDefaultKey) "MAP SETUP: INFO ℹ️" else "MAPS API: CONFIGURED ✓",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = HudColors.TextCrispWhite
                )
            }

            // Realtime Auto-Sync Telemetry Status Badge
            Box(
                modifier = Modifier
                    .frostedGlassHud(
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = if (isOnline) Color(0xCC065F46) else Color(0xCCD97706)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (isOnline) Color(0xFF10B981) else Color(0xFFF59E0B), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOnline) "AUTO-SYNC: LIVE ONLINE ✓" else "AUTO-SYNC: OFFLINE BUFFERING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = HudColors.TextCrispWhite
                    )
                }
            }
        }
    }

    // Google Map Options Dialog
    if (showMapTypePickerModal) {
        AlertDialog(
            onDismissRequest = { showMapTypePickerModal = false },
            containerColor = HudColors.ObsidianModal,
            title = {
                Text(
                    text = "3D HUD Map Options",
                    color = HudColors.TextCrispWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Map Type", color = HudColors.TextCoolSilver, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HudMapTypeCard(
                            title = "Default",
                            icon = Icons.Default.Map,
                            isSelected = selectedMapType == MapType.NORMAL,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedMapType = MapType.NORMAL
                                showMapTypePickerModal = false
                            }
                        )
                        HudMapTypeCard(
                            title = "Satellite",
                            icon = Icons.Default.Satellite,
                            isSelected = selectedMapType == MapType.SATELLITE,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedMapType = MapType.SATELLITE
                                showMapTypePickerModal = false
                            }
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HudMapTypeCard(
                            title = "Terrain",
                            icon = Icons.Default.Terrain,
                            isSelected = selectedMapType == MapType.TERRAIN,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedMapType = MapType.TERRAIN
                                showMapTypePickerModal = false
                            }
                        )
                        HudMapTypeCard(
                            title = "Hybrid",
                            icon = Icons.Default.Layers,
                            isSelected = selectedMapType == MapType.HYBRID,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedMapType = MapType.HYBRID
                                showMapTypePickerModal = false
                            }
                        )
                    }

                    HorizontalDivider(color = HudColors.ObsidianBorder, modifier = Modifier.padding(vertical = 4.dp))

                    Text("3D Camera & Layer Controls", color = HudColors.TextCoolSilver, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Live Traffic Layer", color = HudColors.TextCrispWhite, fontSize = 15.sp)
                        Switch(
                            checked = isTrafficEnabled,
                            onCheckedChange = { isTrafficEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = HudColors.StatusStopped)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("3D Driving Pitch (50°)", color = HudColors.TextCrispWhite, fontSize = 15.sp)
                        Switch(
                            checked = is3dTilt,
                            onCheckedChange = { is3dTilt = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = HudColors.CyanPrimary)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMapTypePickerModal = false }) {
                    Text("Close", color = HudColors.TextCoolSilver, fontSize = 15.sp)
                }
            }
        )
    }

    // Map Integration Info Modal
    if (showKeySetupDialog) {
        AlertDialog(
            onDismissRequest = { showKeySetupDialog = false },
            containerColor = HudColors.ObsidianModal,
            title = {
                Text(
                    text = "Google Maps Integration Guide",
                    color = HudColors.TextCrispWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "To render vector tiles cleanly without blank grid tiles, ensure your API Key has Maps SDK for Android enabled in Google Cloud Console.",
                        color = HudColors.TextCoolSilver,
                        fontSize = 13.sp
                    )

                    Box(
                        modifier = Modifier
                            .hud3dCard(shape = RoundedCornerShape(12.dp), elevation = 4.dp)
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("PROJECT DETAILS", color = HudColors.StatusStopped, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Package: com.ridesync", color = HudColors.TextCrispWhite, fontSize = 12.sp)
                            Text("SHA-1: 00:B1:B7:EF:3E:02:F5:41:F4:22:72:80:92:87:E0:61:65:9B:D0:6D", color = HudColors.TextCoolSilver, fontSize = 11.sp)
                        }
                    }

                    Text("Quick Setup Steps:", color = HudColors.TextCrispWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("1. Open Google Cloud Console -> APIs & Services -> Credentials", color = HudColors.TextCoolSilver, fontSize = 12.sp)
                    Text("2. Enable 'Maps SDK for Android'", color = HudColors.TextCoolSilver, fontSize = 12.sp)
                    Text("3. Add MAPS_API_KEY=YOUR_KEY in local.properties", color = HudColors.CyanLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showKeySetupDialog = false }) {
                    Text("Got It", color = HudColors.StatusStopped, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun HudMapOptionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    active: Boolean,
    activeColor: Color = HudColors.CyanPrimary,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .frostedGlassHud(
                shape = RoundedCornerShape(16.dp),
                backgroundColor = if (active) activeColor else HudColors.FrostedOverlay,
                borderColor = if (active) activeColor else HudColors.FrostedBorder
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) Color.Black else HudColors.TextCrispWhite,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun HudMapTypeCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .hud3dCard(
                shape = RoundedCornerShape(14.dp),
                startColor = if (isSelected) HudColors.CyanPrimary.copy(alpha = 0.2f) else HudColors.ObsidianElevated,
                endColor = HudColors.ObsidianSurface,
                rimColor = if (isSelected) HudColors.CyanPrimary else HudColors.RimHighlight
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) HudColors.CyanPrimary else HudColors.TextCoolSilver,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) HudColors.TextCrispWhite else HudColors.TextCoolSilver
            )
        }
    }
}
