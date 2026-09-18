package com.ridesync.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.LatLng
import com.ridesync.data.model.ConvoyMember
import com.ridesync.data.model.RiderLocationPing
import com.ridesync.data.model.StopEvent

/**
 * ConvoyMapScreen delegating to modern 3D HUD LiveMapScreen to maintain 100% backward compatibility
 * with all existing navigation routes and parameters.
 */
@Composable
fun ConvoyMapScreen(
    routePolyline: List<LatLng>,
    riderLocations: Map<String, RiderLocationPing>,
    convoyMembers: Map<String, ConvoyMember>,
    stopEvents: List<StopEvent>,
    modifier: Modifier = Modifier
) {
    LiveMapScreen(
        routePolyline = routePolyline,
        riderLocations = riderLocations,
        convoyMembers = convoyMembers,
        stopEvents = stopEvents,
        modifier = modifier
    )
}
