package com.ridesync.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridesync.R
import com.ridesync.data.model.PrivacySettings
import com.ridesync.data.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userProfile: UserProfile,
    onSaveProfile: (UserProfile) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }

    var displayName by remember(userProfile) { mutableStateOf(userProfile.displayName) }
    var dateOfBirth by remember(userProfile) { mutableStateOf(userProfile.dateOfBirth) }
    var emergencyContactPhone by remember(userProfile) { mutableStateOf(userProfile.privacySettings.emergencyContactPhone) }
    var email by remember(userProfile) { mutableStateOf(userProfile.email) }
    var mobileNumber by remember(userProfile) { mutableStateOf(userProfile.mobileNumber) }
    var vehicleModel by remember(userProfile) { mutableStateOf(userProfile.vehicleModel) }
    var tankCapacityText by remember(userProfile) { mutableStateOf(userProfile.tankCapacityLiters.toString()) }
    var photoUrl by remember(userProfile) { mutableStateOf(userProfile.photoUrl) }

    val backgroundColor = com.ridesync.ui.theme.HudColors.ObsidianCanvas
    val cardColor = com.ridesync.ui.theme.HudColors.ObsidianSurface
    val accentColor = com.ridesync.ui.theme.HudColors.CyanPrimary

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "RRS User Profile",
                        fontWeight = FontWeight.Bold,
                        color = com.ridesync.ui.theme.HudColors.TextCrispWhite
                    )
                },
                actions = {
                    IconButton(onClick = { isEditing = !isEditing }) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Save Profile" else "Edit Profile",
                            tint = accentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = com.ridesync.ui.theme.HudColors.TextCrispWhite
                )
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // High-Contrast Rally Instrument Graphic Background Pattern
            com.ridesync.ui.theme.RallyGridGraphicBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // User Profile Image (Avatar)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(3.dp, accentColor, CircleShape)
                        .background(cardColor)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo_badge),
                        contentDescription = "RRS Profile Picture",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(76.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = displayName.ifBlank { "Rider" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.ridesync.ui.theme.HudColors.TextCrispWhite
                )

                Text(
                    text = if (vehicleModel.isNotBlank()) "Bike: $vehicleModel" else "Riders Ride Sync (RRS) Member",
                    fontSize = 14.sp,
                    color = com.ridesync.ui.theme.HudColors.TextCoolSilver,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // Profile Detail Cards / Editable Fields
                if (isEditing) {
                    // Editable Mode
                    OutlinedProfileField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = "Full Name",
                        icon = Icons.Default.Person,
                        accentColor = accentColor,
                        cardColor = cardColor
                    )

                    OutlinedProfileField(
                        value = dateOfBirth,
                        onValueChange = { dateOfBirth = it },
                        label = "Date of Birth (e.g. YYYY-MM-DD)",
                        icon = Icons.Default.CalendarToday,
                        accentColor = accentColor,
                        cardColor = cardColor
                    )

                    OutlinedProfileField(
                        value = mobileNumber,
                        onValueChange = { mobileNumber = it },
                        label = "Register Mobile Number",
                        icon = Icons.Default.Phone,
                        keyboardType = KeyboardType.Phone,
                        accentColor = accentColor,
                        cardColor = cardColor
                    )

                    OutlinedProfileField(
                        value = emergencyContactPhone,
                        onValueChange = { emergencyContactPhone = it },
                        label = "Emergency Contact Number",
                        icon = Icons.Default.Phone,
                        keyboardType = KeyboardType.Phone,
                        accentColor = accentColor,
                        cardColor = cardColor
                    )

                    OutlinedProfileField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email ID",
                        icon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email,
                        accentColor = accentColor,
                        cardColor = cardColor
                    )

                    OutlinedProfileField(
                        value = vehicleModel,
                        onValueChange = { vehicleModel = it },
                        label = "Bike Model Name",
                        icon = Icons.Default.TwoWheeler,
                        accentColor = accentColor,
                        cardColor = cardColor
                    )

                    OutlinedProfileField(
                        value = tankCapacityText,
                        onValueChange = { tankCapacityText = it },
                        label = "Fuel Tank Capacity (Liters)",
                        icon = Icons.Default.LocalGasStation,
                        keyboardType = KeyboardType.Number,
                        accentColor = accentColor,
                        cardColor = cardColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save Changes CTA
                    Button(
                        onClick = {
                            val capacity = tankCapacityText.toDoubleOrNull() ?: 15.0
                            val updated = userProfile.copy(
                                displayName = displayName.trim(),
                                dateOfBirth = dateOfBirth.trim(),
                                mobileNumber = mobileNumber.trim(),
                                email = email.trim(),
                                vehicleModel = vehicleModel.trim(),
                                tankCapacityLiters = capacity,
                                photoUrl = photoUrl.trim(),
                                privacySettings = userProfile.privacySettings.copy(
                                    emergencyContactPhone = emergencyContactPhone.trim()
                                )
                            )
                            onSaveProfile(updated)
                            isEditing = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Profile Changes", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }

                } else {
                    // Read-Only Detail View
                    ProfileDetailItem(label = "Full Name", value = displayName, icon = Icons.Default.Person, cardColor = cardColor)
                    ProfileDetailItem(label = "Date of Birth", value = dateOfBirth.ifBlank { "Not set" }, icon = Icons.Default.CalendarToday, cardColor = cardColor)
                    ProfileDetailItem(label = "Register Mobile Number", value = mobileNumber.ifBlank { "Not set" }, icon = Icons.Default.Phone, cardColor = cardColor)
                    ProfileDetailItem(label = "Emergency Contact Number", value = emergencyContactPhone.ifBlank { "Not set" }, icon = Icons.Default.Phone, cardColor = cardColor)
                    ProfileDetailItem(label = "Email ID", value = email.ifBlank { "Not set" }, icon = Icons.Default.Email, cardColor = cardColor)
                    ProfileDetailItem(label = "Bike Model Name", value = vehicleModel.ifBlank { "Not set" }, icon = Icons.Default.TwoWheeler, cardColor = cardColor)
                    ProfileDetailItem(label = "Fuel Tank Capacity", value = "${tankCapacityText} Liters", icon = Icons.Default.LocalGasStation, cardColor = cardColor)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Edit Profile Button
                    Button(
                        onClick = { isEditing = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cardColor,
                            contentColor = com.ridesync.ui.theme.HudColors.TextCrispWhite
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Profile Details", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sign Out Button
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFDC2626)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFECACA))
                    )
                ) {
                    Text("Sign Out of RRS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ProfileDetailItem(
    label: String,
    value: String,
    icon: ImageVector,
    cardColor: Color
) {
    Surface(
        color = cardColor,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(1.dp, com.ridesync.ui.theme.HudColors.ObsidianBorder, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = com.ridesync.ui.theme.HudColors.CyanPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = com.ridesync.ui.theme.HudColors.TextCoolSilver
                )
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun OutlinedProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    accentColor: Color,
    cardColor: Color
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor)
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = cardColor,
            unfocusedContainerColor = cardColor,
            focusedBorderColor = accentColor,
            unfocusedBorderColor = com.ridesync.ui.theme.HudColors.ObsidianBorder,
            focusedLabelColor = accentColor,
            unfocusedLabelColor = com.ridesync.ui.theme.HudColors.TextCoolSilver,
            focusedTextColor = com.ridesync.ui.theme.HudColors.TextCrispWhite,
            unfocusedTextColor = com.ridesync.ui.theme.HudColors.TextCrispWhite
        )
    )
}
