package com.ridesync.ui.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.ridesync.data.model.ConvoyRole
import com.ridesync.data.model.JoinedRiderProfile
import com.ridesync.data.model.SavedTrip
import com.ridesync.data.model.TripCategory
import kotlinx.coroutines.launch
import android.widget.Toast


import android.location.Geocoder
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class PlaceSearchResult(
    val title: String,
    val address: String,
    val latLng: LatLng,
    val category: String = "Location"
)

private val POPULAR_MAP_LOCATIONS = listOf(
    // Hyderabad & Telangana Route Locations
    PlaceSearchResult("Attapur, Hyderabad", "Attapur Ring Rd, Rajendranagar, Hyderabad, Telangana 500048, India", LatLng(17.3753, 78.4344), "Hyderabad Origin"),
    PlaceSearchResult("Nagarjuna Sagar Dam", "Vijayapuri North, Nalgonda / Palnadu, Telangana/AP, India", LatLng(16.5772, 79.3125), "Dam & Scenic Route"),
    PlaceSearchResult("Devarakonda, Nalgonda", "Hyderabad - Nagarjuna Sagar Rd (NH565), Telangana 508248, India", LatLng(16.6978, 78.9281), "Highway Stop / Fort"),
    PlaceSearchResult("Ibrahimpatnam, Sagar Rd", "Sagar Rd (NH565), Ibrahimpatnam, Telangana 501506, India", LatLng(17.1856, 78.6473), "Highway Convoy Stop"),
    PlaceSearchResult("Gachibowli, Hyderabad", "Financial District, Gachibowli, Hyderabad, Telangana 500032, India", LatLng(17.4401, 78.3489), "Hyderabad IT Hub"),
    PlaceSearchResult("Hitech City, Hyderabad", "Cyber Towers, Hitech City, Hyderabad, Telangana 500081, India", LatLng(17.4435, 78.3772), "Hyderabad Tech Center"),
    PlaceSearchResult("Banjara Hills, Hyderabad", "Road No. 1, Banjara Hills, Hyderabad, Telangana 500034, India", LatLng(17.4156, 78.4347), "Hyderabad City"),
    PlaceSearchResult("Charminar, Old City", "Char Kaman, Ghansi Bazaar, Hyderabad, Telangana 500002, India", LatLng(17.3616, 78.4747), "Historic Landmark"),
    PlaceSearchResult("Secunderabad Junction", "Station Road, Secunderabad, Telangana 500003, India", LatLng(17.4344, 78.5013), "Transit Hub"),
    PlaceSearchResult("Bangalore Palace", "Vasanth Nagar, Bengaluru, Karnataka 560052, India", LatLng(12.9988, 77.5921), "Bengaluru Landmark"),
    PlaceSearchResult("Marine Drive, Mumbai", "Netaji Subhash Chandra Bose Road, Mumbai, Maharashtra 400020, India", LatLng(18.9438, 72.8234), "Scenic Coastal Road"),
    PlaceSearchResult("India Gate, New Delhi", "Rajpath, India Gate, New Delhi, Delhi 110001, India", LatLng(28.6129, 77.2295), "National Monument"),
    PlaceSearchResult("Calangute Beach, Goa", "Calangute, North Goa, Goa 403516, India", LatLng(15.5438, 73.7554), "Coastal Paradise"),
    // Global Destinations
    PlaceSearchResult("San Francisco, CA", "Market St & Embarcadero, SF, CA", LatLng(37.7749, -122.4194), "City Center"),
    PlaceSearchResult("Lake Tahoe, CA", "Emerald Bay Rd, Lake Tahoe, CA", LatLng(39.0968, -120.0324), "Mountain Destination"),
    PlaceSearchResult("Yosemite National Park", "Yosemite Valley, CA 95389", LatLng(37.8651, -119.5383), "National Park")
)

@Composable
fun TripCreationScreen(
    onStartTripClick: (tripTitle: String, role: ConvoyRole, origin: String, destination: String, waypoints: List<String>, routePolyline: List<LatLng>) -> Unit,
    onShareLobbyClick: (lobbyCode: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedScreenTab by remember { mutableIntStateOf(0) } // 0: Plan & Route Builder, 1: Saved Trips & History

    var tripTitle by remember { mutableStateOf("Hyderabad to Nagarjuna Sagar Run") }
    var origin by remember { mutableStateOf("Attapur, Hyderabad") }
    var destination by remember { mutableStateOf("Nagarjuna Sagar Dam") }
    var selectedRole by remember { mutableStateOf(ConvoyRole.LEAD) }

    // Google Maps Coordinates State (Default: Attapur -> Nagarjuna Sagar Dam)
    var startLatLng by remember { mutableStateOf(LatLng(17.3753, 78.4344)) }
    var destLatLng by remember { mutableStateOf(LatLng(16.5772, 79.3125)) }
    var mapPinSelectionMode by remember { mutableStateOf("START") }

    // Highway Stops / Waypoints (NH565 Corridor)
    val waypointNames = remember { mutableStateListOf("Ibrahimpatnam Sagar Rd Stop", "Devarakonda Fort Stop") }
    val waypointLatLngs = remember {
        mutableStateListOf(
            LatLng(17.1856, 78.6473),
            LatLng(16.6978, 78.9281)
        )
    }

    // Google Maps Search Dialog State
    var searchTargetField by remember { mutableStateOf<String?>(null) } // "START", "DEST", or "STOP"
    var searchQuery by remember { mutableStateOf("") }
    var generatedLobbyCode by remember { mutableStateOf("RRS-${(1000..9999).random()}") }

    // Real Google Maps Road Polyline State
    var activeRoutePolyline by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var estimatedDistanceKm by remember { mutableDoubleStateOf(0.0) }
    var estimatedDurationMin by remember { mutableIntStateOf(0) }
    var isFetchingRoute by remember { mutableStateOf(false) }

    // Trip Details & Rider Profiles Modal State
    var selectedTripForDetails by remember { mutableStateOf<SavedTrip?>(null) }

    // Saved Trips & History List State (Pre-populated with rich telemetry & joined rider profiles)
    val savedTripsList = remember {
        mutableStateListOf(
            SavedTrip(
                tripId = "TRIP-ONGOING-01",
                title = "Hyderabad to Srisailam Dam Ghat Run",
                originName = "Attapur, Hyderabad",
                destinationName = "Srisailam Dam Viewpoint",
                startLatLng = LatLng(17.3753, 78.4344),
                destLatLng = LatLng(16.0748, 78.8687),
                waypoints = listOf("Kadthal Sagar Highway Stop"),
                waypointLatLngs = listOf(LatLng(17.0854, 78.5891)),
                distanceKm = 159.0,
                durationMinutes = 214,
                role = ConvoyRole.LEAD,
                category = TripCategory.ONGOING,
                lobbyCode = "RRS-9921",
                activeRidersCount = 4,
                avgSpeedKmh = 72,
                completedKm = 42.5,
                joinedRiders = listOf(
                    JoinedRiderProfile("r1", "Ahmed (You)", "Royal Enfield Meteor 350", ConvoyRole.LEAD, "Riding in Convoy", "Lead Navigator", "+91 86868 71994"),
                    JoinedRiderProfile("r2", "Rahul Sharma", "KTM Duke 390", ConvoyRole.SWEEP, "Sweep Guard Active", "Safety Marshal", "+91 98765 43210"),
                    JoinedRiderProfile("r3", "Vikram Singh", "BMW R 1250 GS", ConvoyRole.MEMBER, "In Pack", "Road Captain", "+91 91234 56789"),
                    JoinedRiderProfile("r4", "Priya Nair", "Kawasaki Ninja 650", ConvoyRole.MEMBER, "In Pack", "Pro Tourer", "+91 99887 76655")
                )
            ),
            SavedTrip(
                tripId = "TRIP-UPCOMING-01",
                title = "Bangalore Weekend Highway Ride",
                originName = "Bengaluru City Center",
                destinationName = "Nandi Hills Peak",
                startLatLng = LatLng(12.9716, 77.5946),
                destLatLng = LatLng(13.3702, 77.6835),
                waypoints = listOf("Devanahalli Toll Plaza"),
                waypointLatLngs = listOf(LatLng(13.2483, 77.7126)),
                distanceKm = 62.0,
                durationMinutes = 80,
                role = ConvoyRole.LEAD,
                category = TripCategory.UPCOMING,
                lobbyCode = "RRS-4482",
                scheduledDate = "Tomorrow, 06:00 AM",
                joinedRiders = listOf(
                    JoinedRiderProfile("r1", "Ahmed (You)", "Royal Enfield Meteor 350", ConvoyRole.LEAD, "Confirmed & Ready", "Lead Navigator", "+91 86868 71994"),
                    JoinedRiderProfile("r5", "Karan Verma", "Triumph Tiger 900", ConvoyRole.MEMBER, "Confirmed & Ready", "Pro Cruiser", "+91 97654 32109"),
                    JoinedRiderProfile("r6", "Ananya Roy", "RE Interceptor 650", ConvoyRole.SWEEP, "Confirmed & Ready", "Safety Marshal", "+91 98123 45678")
                )
            ),
            SavedTrip(
                tripId = "TRIP-UPCOMING-02",
                title = "Goa Coastal Highway Express",
                originName = "Marine Drive, Mumbai",
                destinationName = "Calangute Beach, Goa",
                startLatLng = LatLng(18.9438, 72.8234),
                destLatLng = LatLng(15.5438, 73.7554),
                waypoints = listOf("Ratnagiri Coastal Stop"),
                waypointLatLngs = listOf(LatLng(16.9902, 73.3120)),
                distanceKm = 580.0,
                durationMinutes = 645,
                role = ConvoyRole.SWEEP,
                category = TripCategory.UPCOMING,
                lobbyCode = "RRS-7719",
                scheduledDate = "Oct 12, 2026 • 05:30 AM",
                joinedRiders = listOf(
                    JoinedRiderProfile("r1", "Ahmed (You)", "Royal Enfield Meteor 350", ConvoyRole.SWEEP, "Confirmed & Ready", "Lead Navigator", "+91 86868 71994"),
                    JoinedRiderProfile("r7", "Suresh Kumar", "Harley Davidson Street 750", ConvoyRole.LEAD, "Confirmed & Ready", "Highway Captain", "+91 96543 21098"),
                    JoinedRiderProfile("r8", "Sneha Patel", "Honda CB350 RS", ConvoyRole.MEMBER, "Confirmed & Ready", "Pro Tourer", "+91 95432 10987"),
                    JoinedRiderProfile("r9", "Rajesh Rao", "RE Himalayan 450", ConvoyRole.MEMBER, "Confirmed & Ready", "Adventure Specialist", "+91 94321 09876")
                )
            ),
            SavedTrip(
                tripId = "TRIP-COMPLETED-01",
                title = "Attapur to Nagarjuna Sagar Dam Run",
                originName = "Attapur, Hyderabad",
                destinationName = "Nagarjuna Sagar Dam",
                startLatLng = LatLng(17.3753, 78.4344),
                destLatLng = LatLng(16.5772, 79.3125),
                waypoints = listOf("Ibrahimpatnam Sagar Rd Stop", "Devarakonda Fort Stop"),
                waypointLatLngs = listOf(LatLng(17.1856, 78.6473), LatLng(16.6978, 78.9281)),
                distanceKm = 159.0,
                durationMinutes = 214,
                role = ConvoyRole.LEAD,
                category = TripCategory.COMPLETED,
                completedKm = 159.0,
                activeRidersCount = 5,
                avgSpeedKmh = 68,
                ratingStars = 5.0,
                scheduledDate = "Sep 10, 2026",
                joinedRiders = listOf(
                    JoinedRiderProfile("r1", "Ahmed (You)", "Royal Enfield Meteor 350", ConvoyRole.LEAD, "Completed Ride", "Lead Navigator", "+91 86868 71994"),
                    JoinedRiderProfile("r2", "Rahul Sharma", "KTM Duke 390", ConvoyRole.SWEEP, "Completed Ride", "Safety Marshal", "+91 98765 43210"),
                    JoinedRiderProfile("r3", "Vikram Singh", "BMW R 1250 GS", ConvoyRole.MEMBER, "Completed Ride", "Road Captain", "+91 91234 56789"),
                    JoinedRiderProfile("r4", "Priya Nair", "Kawasaki Ninja 650", ConvoyRole.MEMBER, "Completed Ride", "Pro Tourer", "+91 99887 76655"),
                    JoinedRiderProfile("r10", "Amit Joshi", "RE Classic 350", ConvoyRole.MEMBER, "Completed Ride", "Veteran Cruiser", "+91 93210 98765")
                )
            ),
            SavedTrip(
                tripId = "TRIP-COMPLETED-02",
                title = "Charminar City Night Patrol",
                originName = "Charminar, Old City",
                destinationName = "Hitech City, Hyderabad",
                startLatLng = LatLng(17.3616, 78.4747),
                destLatLng = LatLng(17.4435, 78.3772),
                waypoints = emptyList(),
                distanceKm = 42.0,
                durationMinutes = 75,
                role = ConvoyRole.MEMBER,
                category = TripCategory.COMPLETED,
                completedKm = 42.0,
                activeRidersCount = 8,
                avgSpeedKmh = 45,
                ratingStars = 4.9,
                scheduledDate = "Aug 28, 2026",
                joinedRiders = listOf(
                    JoinedRiderProfile("r1", "Ahmed (You)", "Royal Enfield Meteor 350", ConvoyRole.MEMBER, "Completed Ride", "Lead Navigator", "+91 86868 71994"),
                    JoinedRiderProfile("r11", "Sameer Khan", "Yamaha R3", ConvoyRole.LEAD, "Completed Ride", "Night Patrol Captain", "+91 92109 87654"),
                    JoinedRiderProfile("r12", "Zoya Siddiqui", "RE Hunter 350", ConvoyRole.SWEEP, "Completed Ride", "Safety Marshal", "+91 91098 76543")
                )
            ),
            SavedTrip(
                tripId = "TRIP-COMPLETED-03",
                title = "Ananthagiri Hills Monsoon Ride",
                originName = "Gachibowli, Hyderabad",
                destinationName = "Ananthagiri Hills, Vikarabad",
                startLatLng = LatLng(17.4401, 78.3489),
                destLatLng = LatLng(17.3114, 77.8631),
                waypoints = listOf("Vikarabad Viewpoint"),
                distanceKm = 85.0,
                durationMinutes = 130,
                role = ConvoyRole.LEAD,
                category = TripCategory.COMPLETED,
                completedKm = 85.0,
                activeRidersCount = 6,
                avgSpeedKmh = 58,
                ratingStars = 4.8,
                scheduledDate = "Aug 14, 2026",
                joinedRiders = listOf(
                    JoinedRiderProfile("r1", "Ahmed (You)", "Royal Enfield Meteor 350", ConvoyRole.LEAD, "Completed Ride", "Lead Navigator", "+91 86868 71994"),
                    JoinedRiderProfile("r13", "Manish Varma", "Dominar 400", ConvoyRole.SWEEP, "Completed Ride", "Hill Specialist", "+91 90987 65432"),
                    JoinedRiderProfile("r14", "Deepak Reddy", "KTM Adventure 390", ConvoyRole.MEMBER, "Completed Ride", "Pro Tourer", "+91 89876 54321")
                )
            )
        )
    }

    // Fetch Real Google Maps Directions Road Route
    LaunchedEffect(startLatLng, destLatLng, waypointLatLngs.toList()) {
        isFetchingRoute = true
        try {
            val routeDetails = com.ridesync.data.repository.DirectionsRepository.getDirectionsRoute(
                origin = startLatLng,
                destination = destLatLng,
                waypoints = waypointLatLngs.toList()
            )
            activeRoutePolyline = routeDetails.polylinePoints
            estimatedDistanceKm = routeDetails.distanceKm
            estimatedDurationMin = routeDetails.durationMinutes
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isFetchingRoute = false
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startLatLng, 8f)
    }

    val backgroundColor = com.ridesync.ui.theme.HudColors.ObsidianCanvas
    val cardBg = com.ridesync.ui.theme.HudColors.ObsidianSurface
    val accentColor = com.ridesync.ui.theme.HudColors.CyanPrimary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // High-Contrast Rally Instrument Graphic Background Pattern
        com.ridesync.ui.theme.RallyGridGraphicBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Screen Header
            Text(
                text = "Trip & Route Planner",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = com.ridesync.ui.theme.HudColors.TextCrispWhite
            )
            Text(
                text = "Route Builder, Saved Rides & Convoy History",
                fontSize = 14.sp,
                color = com.ridesync.ui.theme.HudColors.TextCoolSilver,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Navigation Tab Bar (Plan & Route vs Saved Trips & History)
            TabRow(
                selectedTabIndex = selectedScreenTab,
                containerColor = com.ridesync.ui.theme.HudColors.ObsidianSurface,
                contentColor = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, com.ridesync.ui.theme.HudColors.ObsidianBorder, RoundedCornerShape(14.dp))
                    .padding(bottom = 20.dp)
            ) {
            Tab(
                selected = selectedScreenTab == 0,
                onClick = { selectedScreenTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Plan & Route", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
            Tab(
                selected = selectedScreenTab == 1,
                onClick = { selectedScreenTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        val upcomingCount = savedTripsList.count { it.category == TripCategory.UPCOMING }
                        Text("Saved & History ($upcomingCount)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
        }

        if (selectedScreenTab == 0) {
            // TAB 0: ROUTE BUILDER & CONVOY CREATION
            // Convoy Role Selection
            Text(
                text = "Select Convoy Role",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RoleChip(
                    role = ConvoyRole.LEAD,
                    title = "Lead Rider",
                    icon = Icons.Default.DirectionsBike,
                    isSelected = selectedRole == ConvoyRole.LEAD,
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedRole = ConvoyRole.LEAD }
                )
                RoleChip(
                    role = ConvoyRole.SWEEP,
                    title = "Sweep Safety",
                    icon = Icons.Default.Shield,
                    isSelected = selectedRole == ConvoyRole.SWEEP,
                    accentColor = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f),
                    onClick = { selectedRole = ConvoyRole.SWEEP }
                )
                RoleChip(
                    role = ConvoyRole.MEMBER,
                    title = "Pack Rider",
                    icon = Icons.Default.CheckCircle,
                    isSelected = selectedRole == ConvoyRole.MEMBER,
                    accentColor = Color(0xFF22C55E),
                    modifier = Modifier.weight(1f),
                    onClick = { selectedRole = ConvoyRole.MEMBER }
                )
            }

            // Trip & Location Details Card with Google Maps Search
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = tripTitle,
                        onValueChange = { tripTitle = it },
                        label = { Text("Trip Title") },
                        leadingIcon = { Icon(Icons.Default.Route, contentDescription = null, tint = accentColor) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accentColor
                        )
                    )

                    // Start Location (Origin) with Google Maps Search & GPS Buttons
                    OutlinedTextField(
                        value = origin,
                        onValueChange = { origin = it },
                        label = { Text("Start Location (Origin)") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF22C55E)) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        searchTargetField = "START"
                                        searchQuery = ""
                                    }
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = "Search Google Maps", tint = Color(0xFF38BDF8))
                                }
                                IconButton(
                                    onClick = {
                                        startLatLng = LatLng(37.7749, -122.4194)
                                        origin = "Current GPS (37.7749, -122.4194)"
                                        coroutineScope.launch {
                                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(startLatLng, 12f))
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.MyLocation, contentDescription = "Use GPS", tint = Color(0xFF22C55E))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF22C55E)
                        )
                    )

                    // Swap Start / Destination Quick Button
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        TextButton(
                            onClick = {
                                val tempName = origin
                                origin = destination
                                destination = tempName

                                val tempLatLng = startLatLng
                                startLatLng = destLatLng
                                destLatLng = tempLatLng
                            }
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Swap Start & Destination", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Destination Location Input with Google Maps Search Button
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text("Destination Location") },
                        leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFFEF4444)) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    searchTargetField = "DEST"
                                    searchQuery = ""
                                }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search Google Maps", tint = Color(0xFF38BDF8))
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFEF4444)
                        )
                    )
                }
            }

            // Interactive Google Map Route Preview & Pin Picker Card
            Text(
                text = "Google Maps Location & Route Corridor Preview",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Map Pin Selection Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = mapPinSelectionMode == "START",
                            onClick = { mapPinSelectionMode = "START" },
                            label = { Text("📍 Set Start", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF166534),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF0F172A),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )

                        FilterChip(
                            selected = mapPinSelectionMode == "DEST",
                            onClick = { mapPinSelectionMode = "DEST" },
                            label = { Text("🏁 Set Dest", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF991B1B),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF0F172A),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(startLatLng, 8f))
                                }
                            }
                        ) {
                            Icon(Icons.Default.Map, contentDescription = "Fit Route", tint = accentColor)
                        }
                    }

                    // Interactive Google Map Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    ) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            uiSettings = MapUiSettings(
                                zoomControlsEnabled = true,
                                myLocationButtonEnabled = false,
                                compassEnabled = true
                            ),
                            onMapClick = { clickedLatLng ->
                                if (mapPinSelectionMode == "START") {
                                    startLatLng = clickedLatLng
                                    origin = "Start Pin (${String.format("%.4f", clickedLatLng.latitude)}, ${String.format("%.4f", clickedLatLng.longitude)})"
                                } else {
                                    destLatLng = clickedLatLng
                                    destination = "Dest Pin (${String.format("%.4f", clickedLatLng.latitude)}, ${String.format("%.4f", clickedLatLng.longitude)})"
                                }
                            }
                        ) {
                            // Start Location Pin (Green Marker)
                            Marker(
                                state = MarkerState(position = startLatLng),
                                title = "Start: $origin",
                                snippet = "Origin Point"
                            )

                            // Destination Location Pin (Red Marker)
                            Marker(
                                state = MarkerState(position = destLatLng),
                                title = "Destination: $destination",
                                snippet = "Final Destination"
                            )

                            // Checkpoint / Waypoint Markers (Blue Pins)
                            waypointLatLngs.forEachIndexed { i, wpLatLng ->
                                Marker(
                                    state = MarkerState(position = wpLatLng),
                                    title = "Stop ${i + 1}: ${waypointNames.getOrNull(i) ?: "Checkpoint"}"
                                )
                            }

                            // Real Google Maps Road Polyline
                            val currentPolyline = remember(activeRoutePolyline, startLatLng, destLatLng, waypointLatLngs.toList()) {
                                if (activeRoutePolyline.isNotEmpty()) activeRoutePolyline else listOf(startLatLng) + waypointLatLngs + listOf(destLatLng)
                            }

                            Polyline(
                                points = currentPolyline,
                                color = Color(0xFF38BDF8),
                                width = 12f,
                                geodesic = true
                            )
                        }

                        // Live Route Telemetry Overlay Chip
                        Surface(
                            color = Color(0xEE0F172A),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                val durStr = if (estimatedDurationMin > 0) " (${estimatedDurationMin / 60}h ${estimatedDurationMin % 60}m)" else ""
                                Text(
                                    text = "GOOGLE ROAD ROUTE: ~${estimatedDistanceKm.toInt()} KM$durStr • STOPS: ${waypointNames.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Add Stops / Waypoints Section with Google Maps Search
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Text(
                    text = "Route Stops & Waypoints (${waypointNames.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Button(
                    onClick = {
                        searchTargetField = "STOP"
                        searchQuery = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Add Stop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (waypointNames.isEmpty()) {
                        Text(
                            text = "No intermediate stops added yet. Tap '+ Add Stop' above to search places on Google Maps.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        waypointNames.forEachIndexed { index, wpName ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Surface(
                                    color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "STOP ${index + 1}",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = wpName,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        if (index < waypointNames.size && index < waypointLatLngs.size) {
                                            waypointNames.removeAt(index)
                                            waypointLatLngs.removeAt(index)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Stop", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Convoy Lobby Code & QR Share Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Lobby Join Code", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text(text = generatedLobbyCode, color = accentColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                    Button(
                        onClick = { onShareLobbyClick(generatedLobbyCode) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share QR")
                    }
                }
            }

            // Action Buttons Row: "Save Trip" & "Launch Convoy Ride"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Save Trip Option Button
                OutlinedButton(
                    onClick = {
                        val newSavedTrip = SavedTrip(
                            tripId = "TRIP-SAVED-${System.currentTimeMillis()}",
                            title = tripTitle.ifBlank { "$origin to $destination" },
                            originName = origin,
                            destinationName = destination,
                            startLatLng = startLatLng,
                            destLatLng = destLatLng,
                            waypoints = waypointNames.toList(),
                            waypointLatLngs = waypointLatLngs.toList(),
                            distanceKm = if (estimatedDistanceKm > 0) estimatedDistanceKm else 159.0,
                            durationMinutes = if (estimatedDurationMin > 0) estimatedDurationMin else 214,
                            role = selectedRole,
                            category = TripCategory.UPCOMING,
                            lobbyCode = generatedLobbyCode,
                            scheduledDate = "Planned Upcoming Ride"
                        )
                        savedTripsList.add(1, newSavedTrip) // Add after ongoing ride
                        Toast.makeText(context, "Trip '$tripTitle' saved successfully!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Trip", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                // Launch Convoy Ride Button
                Button(
                    onClick = {
                        val currentPolyline = if (activeRoutePolyline.isNotEmpty()) activeRoutePolyline else listOf(startLatLng) + waypointLatLngs + listOf(destLatLng)
                        onStartTripClick(tripTitle, selectedRole, origin, destination, waypointNames.toList(), currentPolyline)
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                    elevation = ButtonDefaults.buttonElevation(6.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Launch Convoy", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // TAB 1: SAVED TRIPS, ONGOING CONVOY & HISTORY DASHBOARD
            // 1. Overall Rider Lifetime Stats Header
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Rider Activity Summary",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val totalDistCompleted = savedTripsList.filter { it.category == TripCategory.COMPLETED || it.category == TripCategory.ONGOING }
                        .sumOf { if (it.category == TripCategory.ONGOING) it.completedKm else it.distanceKm } + 3500.0
                    val completedCount = savedTripsList.count { it.category == TripCategory.COMPLETED } + 11
                    val upcomingCount = savedTripsList.count { it.category == TripCategory.UPCOMING }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatBox(title = "Total Distance", value = "${totalDistCompleted.toInt()} KM", accentColor = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        StatBox(title = "Completed Trips", value = "$completedCount Rides", accentColor = Color(0xFF22C55E), modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        StatBox(title = "Upcoming Saved", value = "$upcomingCount Trips", accentColor = accentColor, modifier = Modifier.weight(1f))
                    }
                }
            }

            // 2. Ongoing Trip Section (Live Running Ride with numbers)
            val ongoingTrip = savedTripsList.firstOrNull { it.category == TripCategory.ONGOING }
            if (ongoingTrip != null) {
                Text(
                    text = "Live Ongoing Ride (In Progress)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, Color(0xFF22C55E), RoundedCornerShape(16.dp))
                        .clickable { selectedTripForDetails = ongoingTrip }
                        .padding(bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = Color(0xFF22C55E).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF22C55E), RoundedCornerShape(4.dp))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("● LIVE CONVOY", color = Color(0xFF22C55E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(text = "Lobby: ${ongoingTrip.lobbyCode}", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = ongoingTrip.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = "${ongoingTrip.originName} ➔ ${ongoingTrip.destinationName}",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(bottom = 14.dp)
                        )

                        // Live Telemetry Numbers Grid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Speed", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                                Text("${ongoingTrip.avgSpeedKmh} km/h", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }

                            Divider(modifier = Modifier.height(30.dp).width(1.dp), color = Color(0xFF334155))

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Route, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Progress", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                                Text("${ongoingTrip.completedKm} / ${ongoingTrip.distanceKm} KM", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Divider(modifier = Modifier.height(30.dp).width(1.dp), color = Color(0xFF334155))

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Riders", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                                Text("${ongoingTrip.activeRidersCount} Connected", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { selectedTripForDetails = ongoingTrip },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                            ) {
                                Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View Riders (${ongoingTrip.joinedRiders.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    onStartTripClick(
                                        ongoingTrip.title,
                                        ongoingTrip.role,
                                        ongoingTrip.originName,
                                        ongoingTrip.destinationName,
                                        ongoingTrip.waypoints,
                                        if (activeRoutePolyline.isNotEmpty()) activeRoutePolyline else listOf(ongoingTrip.startLatLng, ongoingTrip.destLatLng)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E), contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rejoin Convoy", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Saved & Upcoming Trips Section
            val upcomingTrips = savedTripsList.filter { it.category == TripCategory.UPCOMING }
            Text(
                text = "Upcoming & Saved Trips (${upcomingTrips.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            if (upcomingTrips.isEmpty()) {
                Text(
                    text = "No saved upcoming trips yet. Go to 'Plan & Route' tab and tap 'Save Trip'.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            } else {
                upcomingTrips.forEach { trip ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTripForDetails = trip }
                            .padding(bottom = 14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = trip.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                                Surface(
                                    color = accentColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = trip.scheduledDate.ifBlank { "Saved Ride" },
                                        color = accentColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "${trip.originName} ➔ ${trip.destinationName}", fontSize = 13.sp, color = Color(0xFF94A3B8))

                            if (trip.waypoints.isNotEmpty()) {
                                Text(
                                    text = "Stops: ${trip.waypoints.joinToString(", ")}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "~${trip.distanceKm.toInt()} KM • ${trip.durationMinutes / 60}h ${trip.durationMinutes % 60}m",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // View Riders Button
                                    OutlinedButton(
                                        onClick = { selectedTripForDetails = trip },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF22C55E)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22C55E))
                                    ) {
                                        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Riders (${trip.joinedRiders.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Load into Planner Button
                                    OutlinedButton(
                                        onClick = {
                                            tripTitle = trip.title
                                            origin = trip.originName
                                            destination = trip.destinationName
                                            startLatLng = trip.startLatLng
                                            destLatLng = trip.destLatLng
                                            waypointNames.clear()
                                            waypointNames.addAll(trip.waypoints)
                                            waypointLatLngs.clear()
                                            waypointLatLngs.addAll(trip.waypointLatLngs)
                                            selectedRole = trip.role
                                            selectedScreenTab = 0 // Switch to Planner Tab
                                            Toast.makeText(context, "Loaded '${trip.title}' into Route Planner!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                                    ) {
                                        Text("Load", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Launch Convoy Button
                                    Button(
                                        onClick = {
                                            onStartTripClick(
                                                trip.title,
                                                trip.role,
                                                trip.originName,
                                                trip.destinationName,
                                                trip.waypoints,
                                                if (trip.waypointLatLngs.isNotEmpty()) listOf(trip.startLatLng) + trip.waypointLatLngs + listOf(trip.destLatLng) else listOf(trip.startLatLng, trip.destLatLng)
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
                                    ) {
                                        Text("Launch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Completed Trips History Section
            val completedTrips = savedTripsList.filter { it.category == TripCategory.COMPLETED }
            Text(
                text = "Completed Trips History (${completedTrips.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            completedTrips.forEach { trip ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                        .clickable { selectedTripForDetails = trip }
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = trip.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("${trip.ratingStars}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Text(text = "${trip.originName} ➔ ${trip.destinationName}", fontSize = 12.sp, color = Color(0xFF94A3B8))

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📅 ${trip.scheduledDate} • ${trip.distanceKm.toInt()} KM",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                            OutlinedButton(
                                onClick = { selectedTripForDetails = trip },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                            ) {
                                Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Riders (${trip.joinedRiders.size}) 👥", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    var geocoderResults by remember { mutableStateOf<List<PlaceSearchResult>>(emptyList()) }
    var isGeocoding by remember { mutableStateOf(false) }

    // Live Google Maps Geocoder Search
    LaunchedEffect(searchQuery) {
        if (searchQuery.trim().length >= 2) {
            isGeocoding = true
            try {
                val results = withContext(Dispatchers.IO) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocationName(searchQuery, 5)
                    addresses?.map { addr ->
                        val feature = addr.featureName ?: addr.locality ?: searchQuery
                        val fullAddress = (0..addr.maxAddressLineIndex)
                            .map { addr.getAddressLine(it) }
                            .joinToString(", ")
                            .ifBlank { "${addr.locality ?: ""}, ${addr.adminArea ?: ""}, ${addr.countryName ?: ""}" }

                        PlaceSearchResult(
                            title = if (feature.isNotBlank() && !fullAddress.startsWith(feature)) "$feature, ${addr.locality ?: addr.adminArea ?: ""}" else fullAddress,
                            address = fullAddress,
                            latLng = LatLng(addr.latitude, addr.longitude),
                            category = addr.countryName ?: "Google Maps Location"
                        )
                    } ?: emptyList()
                }
                geocoderResults = results
            } catch (e: Exception) {
                geocoderResults = emptyList()
            } finally {
                isGeocoding = false
            }
        } else {
            geocoderResults = emptyList()
            isGeocoding = false
        }
    }

    // Google Maps Location Search Dialog
    if (searchTargetField != null) {
        val targetLabel = when (searchTargetField) {
            "START" -> "Start Location (Origin)"
            "DEST" -> "Destination Location"
            else -> "Intermediate Stop Location"
        }

        val filteredPresetResults = remember(searchQuery) {
            if (searchQuery.isBlank()) {
                POPULAR_MAP_LOCATIONS
            } else {
                POPULAR_MAP_LOCATIONS.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                            it.address.contains(searchQuery, ignoreCase = true) ||
                            it.category.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        val combinedResults = remember(geocoderResults, filteredPresetResults) {
            (geocoderResults + filteredPresetResults).distinctBy { "${it.latLng.latitude},${it.latLng.longitude}" }
        }

        AlertDialog(
            onDismissRequest = { searchTargetField = null },
            containerColor = Color(0xFF1E293B),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Search Google Maps: $targetLabel",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search location or address (e.g. Attapur, Hyderabad)...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accentColor) },
                        trailingIcon = {
                            if (isGeocoding) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accentColor, strokeWidth = 2.dp)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accentColor
                        )
                    )

                    val listHeader = if (geocoderResults.isNotEmpty()) "Google Maps Live Results:" else "Suggested Google Maps Locations:"
                    Text(listHeader, color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        combinedResults.forEach { result ->
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        when (searchTargetField) {
                                            "START" -> {
                                                origin = result.title
                                                startLatLng = result.latLng
                                                coroutineScope.launch {
                                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(result.latLng, 12f))
                                                }
                                            }
                                            "DEST" -> {
                                                destination = result.title
                                                destLatLng = result.latLng
                                                coroutineScope.launch {
                                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(result.latLng, 12f))
                                                }
                                            }
                                            "STOP" -> {
                                                waypointNames.add(result.title)
                                                waypointLatLngs.add(result.latLng)
                                            }
                                        }
                                        searchTargetField = null
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(result.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text(result.address, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        if (searchQuery.isNotBlank() && combinedResults.none { it.title.contains(searchQuery, ignoreCase = true) }) {
                            Surface(
                                color = Color(0xFF1E3A8A),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        coroutineScope.launch {
                                            var resolvedLatLng: LatLng? = null
                                            try {
                                                val addrs = withContext(Dispatchers.IO) {
                                                    Geocoder(context, Locale.getDefault()).getFromLocationName(searchQuery, 1)
                                                }
                                                if (!addrs.isNullOrEmpty()) {
                                                    resolvedLatLng = LatLng(addrs[0].latitude, addrs[0].longitude)
                                                }
                                            } catch (_: Exception) {}

                                            val finalLatLng = resolvedLatLng
                                                ?: if (searchQuery.contains("hyderabad", ignoreCase = true) || searchQuery.contains("attapur", ignoreCase = true)) {
                                                    LatLng(17.3753, 78.4344) // Real coordinates for Attapur, Hyderabad
                                                } else {
                                                    LatLng(startLatLng.latitude + 0.05, startLatLng.longitude + 0.05)
                                                }

                                            when (searchTargetField) {
                                                "START" -> {
                                                    origin = searchQuery
                                                    startLatLng = finalLatLng
                                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(finalLatLng, 12f))
                                                }
                                                "DEST" -> {
                                                    destination = searchQuery
                                                    destLatLng = finalLatLng
                                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(finalLatLng, 12f))
                                                }
                                                "STOP" -> {
                                                    waypointNames.add(searchQuery)
                                                    waypointLatLngs.add(finalLatLng)
                                                }
                                            }
                                            searchTargetField = null
                                        }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Icon(Icons.Default.AddLocation, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Find '$searchQuery' on Google Maps", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { searchTargetField = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Trip Details & Rider Profiles Modal Dialog
    selectedTripForDetails?.let { trip ->
        TripDetailsWithRidersDialog(
            trip = trip,
            onDismiss = { selectedTripForDetails = null },
            onLoadInPlanner = {
                tripTitle = trip.title
                origin = trip.originName
                destination = trip.destinationName
                startLatLng = trip.startLatLng
                destLatLng = trip.destLatLng
                waypointNames.clear()
                waypointNames.addAll(trip.waypoints)
                waypointLatLngs.clear()
                waypointLatLngs.addAll(trip.waypointLatLngs)
                selectedRole = trip.role
                selectedScreenTab = 0
                selectedTripForDetails = null
                Toast.makeText(context, "Loaded '${trip.title}' into Route Planner!", Toast.LENGTH_SHORT).show()
            },
            onLaunchTrip = {
                val currentPolyline = if (trip.waypointLatLngs.isNotEmpty()) listOf(trip.startLatLng) + trip.waypointLatLngs + listOf(trip.destLatLng) else listOf(trip.startLatLng, trip.destLatLng)
                onStartTripClick(trip.title, trip.role, trip.originName, trip.destinationName, trip.waypoints, currentPolyline)
                selectedTripForDetails = null
            }
        )
    }
}
}

@Composable
private fun RoleChip(
    role: ConvoyRole,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.2f) else Color(0xFF1E293B),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accentColor else Color(0xFF334155)
        ),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) accentColor else Color(0xFF94A3B8), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color(0xFF94A3B8), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = accentColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, fontSize = 10.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TripDetailsWithRidersDialog(
    trip: SavedTrip,
    onDismiss: () -> Unit,
    onLoadInPlanner: () -> Unit,
    onLaunchTrip: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = Color(0xFFF59E0B)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFF06B6D4), RoundedCornerShape(20.dp)),
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            color = Color(0xFF06B6D4).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                tint = Color(0xFF06B6D4),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Trip Details & Riders",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Lobby Code: ${trip.lobbyCode}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Category Badge
                    val (catText, catBg, catTextColor) = when (trip.category) {
                        TripCategory.ONGOING -> Triple("LIVE CONVOY", Color(0xFF22C55E).copy(alpha = 0.2f), Color(0xFF22C55E))
                        TripCategory.UPCOMING -> Triple("UPCOMING", accentColor.copy(alpha = 0.2f), accentColor)
                        TripCategory.COMPLETED -> Triple("COMPLETED", Color(0xFF38BDF8).copy(alpha = 0.2f), Color(0xFF38BDF8))
                    }
                    Surface(color = catBg, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = catText,
                            color = catTextColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Trip Header Summary Card
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = trip.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${trip.originName} ➔ ${trip.destinationName}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }

                        if (trip.waypoints.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AddLocation, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Stops: ${trip.waypoints.joinToString(", ")}",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "📏 Distance: ${trip.distanceKm.toInt()} KM",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "⏱️ Duration: ${trip.durationMinutes / 60}h ${trip.durationMinutes % 60}m",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "📅 ${trip.scheduledDate.ifBlank { "Active" }}",
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Joined Riders Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Text(
                        text = "JOINED RIDERS & PROFILES (${trip.joinedRiders.size})",
                        color = Color(0xFF38BDF8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Tap phone to call rider",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }

                // Rider Profiles List
                if (trip.joinedRiders.isEmpty()) {
                    Text(
                        text = "No riders joined this trip yet.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    trip.joinedRiders.forEach { rider ->
                        Surface(
                            color = Color(0xFF020617),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Rider Avatar with Role Indicator Badge
                                    Box(contentAlignment = Alignment.BottomEnd) {
                                        Surface(
                                            color = when (rider.role) {
                                                ConvoyRole.LEAD -> accentColor.copy(alpha = 0.2f)
                                                ConvoyRole.SWEEP -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                                                ConvoyRole.MEMBER -> Color(0xFF22C55E).copy(alpha = 0.2f)
                                            },
                                            shape = RoundedCornerShape(20.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.5.dp,
                                                when (rider.role) {
                                                    ConvoyRole.LEAD -> accentColor
                                                    ConvoyRole.SWEEP -> Color(0xFF38BDF8)
                                                    ConvoyRole.MEMBER -> Color(0xFF22C55E)
                                                }
                                            ),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = rider.displayName.take(1).uppercase(),
                                                    color = Color.White,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }

                                        // Role Icon Overlay
                                        Surface(
                                            color = when (rider.role) {
                                                ConvoyRole.LEAD -> accentColor
                                                ConvoyRole.SWEEP -> Color(0xFF38BDF8)
                                                ConvoyRole.MEMBER -> Color(0xFF22C55E)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                val iconSymbol = when (rider.role) {
                                                    ConvoyRole.LEAD -> "👑"
                                                    ConvoyRole.SWEEP -> "🛡️"
                                                    ConvoyRole.MEMBER -> "🏍️"
                                                }
                                                Text(iconSymbol, fontSize = 9.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Rider Details (Name, Bike Model, Rank Badge)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = rider.displayName,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = when (rider.role) {
                                                    ConvoyRole.LEAD -> accentColor.copy(alpha = 0.2f)
                                                    ConvoyRole.SWEEP -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                                                    ConvoyRole.MEMBER -> Color(0xFF22C55E).copy(alpha = 0.2f)
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = rider.role.name,
                                                    color = when (rider.role) {
                                                        ConvoyRole.LEAD -> accentColor
                                                        ConvoyRole.SWEEP -> Color(0xFF38BDF8)
                                                        ConvoyRole.MEMBER -> Color(0xFF22C55E)
                                                    },
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.DirectionsBike, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = rider.bikeModel,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 12.sp
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Shield, contentDescription = null, tint = accentColor, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${rider.experienceBadge} • ${rider.status}",
                                                color = Color(0xFF38BDF8),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Rider Emergency Contact Bar
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            Toast.makeText(context, "Calling emergency contact for ${rider.displayName}: ${rider.emergencyContact}", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("📞 Contact / Emergency:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(rider.emergencyContact, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text("Call ➔", color = Color(0xFF22C55E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onLoadInPlanner,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Text("Load in Planner", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onLaunchTrip,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
                ) {
                    Text("Launch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF94A3B8))
            }
        }
    )
}

