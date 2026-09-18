package com.ridesync.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val mobileNumber: String = "",
    val dateOfBirth: String = "",
    val photoUrl: String = "",
    val vehicleModel: String = "",
    val tankCapacityLiters: Double = 15.0,
    val privacySettings: PrivacySettings = PrivacySettings(),
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class PrivacySettings(
    @get:PropertyName("shareLocationWithGroup")
    @set:PropertyName("shareLocationWithGroup")
    var shareLocationWithGroup: Boolean = true,

    @get:PropertyName("emergencyContactPhone")
    @set:PropertyName("emergencyContactPhone")
    var emergencyContactPhone: String = ""
)
