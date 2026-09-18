package com.ridesync.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
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
import com.ridesync.data.model.StopReason

@Composable
fun GloveFriendlyActionPad(
    onStopReported: (StopReason) -> Unit,
    onSosReported: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var showStopPickerModal by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // "I'm Stopping" Tactile 3D Button (68dp height)
            TactileGloveButton(
                onClick = {
                    showStopPickerModal = true
                },
                modifier = Modifier.weight(1f),
                minHeight = 68.dp,
                containerGradient = listOf(Color(0xFFD97706), Color(0xFFB45309)),
                accentGlow = Color(0xFFFBBF24)
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I'm Stopping",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }

            // "SOS / Emergency" Tactile 3D Button (68dp height)
            TactileGloveButton(
                onClick = {
                    onSosReported()
                },
                modifier = Modifier.weight(1f),
                minHeight = 68.dp,
                containerGradient = listOf(Color(0xFFDC2626), Color(0xFF991B1B)),
                accentGlow = Color(0xFFFCA5A5)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SOS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }

    if (showStopPickerModal) {
        AlertDialog(
            onDismissRequest = { showStopPickerModal = false },
            containerColor = com.ridesync.ui.theme.HudColors.ObsidianSurface,
            title = {
                Text(
                    text = "Select Stop Reason",
                    color = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StopReasonOptionCard(
                        title = "Fuel Stop ⛽",
                        icon = Icons.Default.LocalGasStation,
                        color = com.ridesync.ui.theme.HudColors.CyanPrimary,
                        onClick = {
                            onStopReported(StopReason.FUEL)
                            showStopPickerModal = false
                        }
                    )
                    StopReasonOptionCard(
                        title = "Food / Rest 🍔",
                        icon = Icons.Default.Restaurant,
                        color = com.ridesync.ui.theme.HudColors.StatusRiding,
                        onClick = {
                            onStopReported(StopReason.FOOD)
                            showStopPickerModal = false
                        }
                    )
                    StopReasonOptionCard(
                        title = "Breakdown 🛠️",
                        icon = Icons.Default.Build,
                        color = Color(0xFFEA580C),
                        onClick = {
                            onStopReported(StopReason.BREAKDOWN)
                            showStopPickerModal = false
                        }
                    )
                    StopReasonOptionCard(
                        title = "Short Rest ☕",
                        icon = Icons.Default.Coffee,
                        color = com.ridesync.ui.theme.HudColors.CobaltBlue,
                        onClick = {
                            onStopReported(StopReason.REST)
                            showStopPickerModal = false
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStopPickerModal = false }) {
                    Text("Cancel", color = com.ridesync.ui.theme.HudColors.TextCoolSilver, fontSize = 16.sp)
                }
            }
        )
    }
}

@Composable
private fun StopReasonOptionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    TactileGloveButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        minHeight = 64.dp,
        containerGradient = listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9)),
        accentGlow = color
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                color = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}
