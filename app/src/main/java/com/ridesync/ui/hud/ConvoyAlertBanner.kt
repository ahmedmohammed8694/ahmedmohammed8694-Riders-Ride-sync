package com.ridesync.ui.hud

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridesync.data.model.AlertBanner
import com.ridesync.data.model.AlertSeverity

@Composable
fun ConvoyAlertBanner(
    banner: AlertBanner?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = banner != null,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (banner != null) {
            val (backgroundColor, contentColor, icon) = when (banner.severity) {
                AlertSeverity.INFO -> Triple(Color(0xFF0284C7), Color.White, Icons.Default.Info)
                AlertSeverity.WARNING -> Triple(Color(0xFFD97706), Color.Black, Icons.Default.Warning)
                AlertSeverity.CRITICAL -> Triple(Color(0xFFDC2626), Color.White, Icons.Default.Warning)
            }

            Surface(
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = banner.title,
                                color = contentColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Text(
                                text = banner.message,
                                color = contentColor.copy(alpha = 0.9f),
                                fontSize = 14.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = contentColor
                        )
                    }
                }
            }
        }
    }
}
