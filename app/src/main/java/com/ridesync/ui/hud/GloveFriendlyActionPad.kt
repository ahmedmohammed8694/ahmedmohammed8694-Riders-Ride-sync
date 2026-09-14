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
            // "I'm Stopping" Button (68dp height)
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showStopPickerModal = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF59E0B), // Amber
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "I'm Stopping",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // "SOS / Emergency" Button (68dp height)
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSosReported()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444), // Crimson Red
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SOS",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }

    if (showStopPickerModal) {
        AlertDialog(
            onDismissRequest = { showStopPickerModal = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Text(
                    text = "Select Stop Reason",
                    color = Color.White,
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
                        color = Color(0xFF38BDF8),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStopReported(StopReason.FUEL)
                            showStopPickerModal = false
                        }
                    )
                    StopReasonOptionCard(
                        title = "Food / Rest 🍔",
                        icon = Icons.Default.Restaurant,
                        color = Color(0xFF22C55E),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStopReported(StopReason.FOOD)
                            showStopPickerModal = false
                        }
                    )
                    StopReasonOptionCard(
                        title = "Breakdown 🛠️",
                        icon = Icons.Default.Build,
                        color = Color(0xFFF97316),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStopReported(StopReason.BREAKDOWN)
                            showStopPickerModal = false
                        }
                    )
                    StopReasonOptionCard(
                        title = "Short Rest ☕",
                        icon = Icons.Default.Coffee,
                        color = Color(0xFFA855F7),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStopReported(StopReason.REST)
                            showStopPickerModal = false
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStopPickerModal = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8), fontSize = 16.sp)
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
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp)
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
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}
