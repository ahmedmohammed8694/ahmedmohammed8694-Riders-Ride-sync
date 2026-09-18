package com.ridesync.ui.location

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridesync.util.LocationPermissionHelper

@Composable
fun LocationPermissionGateway(
    isRideActive: Boolean,
    onPermissionsFullyGranted: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    var hasForeground by remember {
        mutableStateOf(LocationPermissionHelper.hasForegroundLocationPermission(context))
    }
    var hasBackground by remember {
        mutableStateOf(LocationPermissionHelper.hasBackgroundLocationPermission(context))
    }
    var isBatteryOptIgnored by remember {
        mutableStateOf(LocationPermissionHelper.isIgnoringBatteryOptimizations(context))
    }
    var showBackgroundRationaleModal by remember { mutableStateOf(false) }

    val foregroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasForeground = fineGranted || coarseGranted

        if (hasForeground && isRideActive && (!hasBackground || !isBatteryOptIgnored)) {
            showBackgroundRationaleModal = true
        }
    }

    // Trigger permission check whenever ride state changes to active
    LaunchedEffect(isRideActive, hasForeground, hasBackground, isBatteryOptIgnored) {
        if (hasForeground && hasBackground && isBatteryOptIgnored) {
            onPermissionsFullyGranted()
        } else if (isRideActive && hasForeground && (!hasBackground || !isBatteryOptIgnored)) {
            showBackgroundRationaleModal = true
        }
    }

    val backgroundColor = com.ridesync.ui.theme.HudColors.ObsidianCanvas
    val accentColor = com.ridesync.ui.theme.HudColors.CyanPrimary
    val surfaceColor = com.ridesync.ui.theme.HudColors.ObsidianSurface

    if (!hasForeground) {
        // Stage 1: Foreground Location Request Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // High-Contrast Rally Instrument Graphic Background Pattern
            com.ridesync.ui.theme.RallyGridGraphicBackground()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Location Access Required",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.ridesync.ui.theme.HudColors.TextCrispWhite
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Riders Ride Sync (RRS) requires precise GPS coordinates to show your location on the convoy map and calculate distance metrics.",
                    fontSize = 15.sp,
                    color = com.ridesync.ui.theme.HudColors.TextCoolSilver,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(36.dp))

                Button(
                    onClick = {
                        foregroundPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Grant Location Permission",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    } else {
        // Render target content once foreground location is available
        content()

        // Stage 2: Educational Modal for Background Location & Battery Optimization (Android 11+)
        if (showBackgroundRationaleModal && isRideActive) {
            AlertDialog(
                onDismissRequest = { showBackgroundRationaleModal = false },
                containerColor = surfaceColor,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = "Background Ride Tracking",
                            color = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "To keep you connected with the convoy while your phone screen is off or mounted:",
                            color = com.ridesync.ui.theme.HudColors.TextCoolSilver,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!hasBackground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            PermissionRationaleItem(
                                icon = Icons.Default.Security,
                                title = "Location: 'Allow all the time'",
                                description = "Ensures telemetry updates continue during dead-zones without app termination."
                            )
                        }

                        if (!isBatteryOptIgnored) {
                            Spacer(modifier = Modifier.height(12.dp))
                            PermissionRationaleItem(
                                icon = Icons.Default.BatteryAlert,
                                title = "Unrestricted Battery Usage",
                                description = "Prevents Android OS power saving from killing the RideTracking service."
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showBackgroundRationaleModal = false
                            if (!hasBackground) {
                                context.startActivity(LocationPermissionHelper.openAppSettingsIntent(context))
                            } else if (!isBatteryOptIgnored) {
                                context.startActivity(LocationPermissionHelper.openBatteryOptimizationSettingsIntent(context))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Configure System Settings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showBackgroundRationaleModal = false }
                    ) {
                        Text("Later", color = com.ridesync.ui.theme.HudColors.TextCoolSilver)
                    }
                }
            )
        }
    }
}

@Composable
private fun PermissionRationaleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = com.ridesync.ui.theme.HudColors.CyanPrimary,
            modifier = Modifier
                .size(24.dp)
                .padding(top = 2.dp, end = 12.dp)
        )
        Column {
            Text(
                text = title,
                color = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = description,
                color = com.ridesync.ui.theme.HudColors.TextCoolSilver,
                fontSize = 13.sp
            )
        }
    }
}
