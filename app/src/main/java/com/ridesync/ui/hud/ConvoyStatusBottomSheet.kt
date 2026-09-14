package com.ridesync.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridesync.data.model.ConvoyMember
import com.ridesync.data.model.ConvoyRole
import com.ridesync.data.model.RiderLocationPing
import com.ridesync.data.model.RiderStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvoyStatusBottomSheet(
    convoyMembers: List<ConvoyMember>,
    riderPings: Map<String, RiderLocationPing>,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Convoy Roster (${convoyMembers.size} Riders)",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(convoyMembers) { member ->
                    val ping = riderPings[member.userId]
                    ConvoyMemberCard(member = member, ping = ping)
                }
            }
        }
    }
}

@Composable
private fun ConvoyMemberCard(
    member: ConvoyMember,
    ping: RiderLocationPing?
) {
    val statusColor = when (member.status) {
        RiderStatus.RIDING -> Color(0xFF22C55E)
        RiderStatus.STOPPED -> Color(0xFFF59E0B)
        RiderStatus.DELAYED -> Color(0xFFF97316)
        RiderStatus.SOS -> Color(0xFFEF4444)
    }

    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Role Icon Badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .background(statusColor.copy(alpha = 0.2f), CircleShape)
                ) {
                    when (member.role) {
                        ConvoyRole.LEAD -> Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Lead",
                            tint = Color(0xFFF59E0B)
                        )
                        ConvoyRole.SWEEP -> Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Sweep",
                            tint = Color(0xFF38BDF8)
                        )
                        ConvoyRole.MEMBER -> Text(
                            text = member.displayName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = member.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (member.role != ConvoyRole.MEMBER) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = member.role.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (member.role == ConvoyRole.LEAD) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                                modifier = Modifier
                                    .background(Color(0xFF0F172A), CircleShape)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "${member.vehicleModel} • ${member.status.name}",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )

                    if (member.isRouteDeviated) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Route Off-Track (>150m)",
                                fontSize = 12.sp,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Metrics (Speed & Distance Behind Lead)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("%.0f km/h", ping?.speedKmh ?: 0f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                val behindText = if (member.distanceBehindLeadMeters > 1000) {
                    String.format("-%.1f km", member.distanceBehindLeadMeters / 1000)
                } else {
                    String.format("-%.0f m", member.distanceBehindLeadMeters)
                }

                Text(
                    text = behindText,
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}
