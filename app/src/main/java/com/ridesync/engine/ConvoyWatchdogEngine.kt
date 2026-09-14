package com.ridesync.engine

import com.ridesync.audio.HelmetAudioEngine
import com.ridesync.data.model.AlertBanner
import com.ridesync.data.model.AlertSeverity
import com.ridesync.data.model.ConvoyMember
import com.ridesync.data.model.ConvoyRole
import com.ridesync.data.model.StopEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConvoyWatchdogEngine(
    private val audioEngine: HelmetAudioEngine
) {

    private val _activeBanner = MutableStateFlow<AlertBanner?>(null)
    val activeBanner: StateFlow<AlertBanner?> = _activeBanner.asStateFlow()

    private val notifiedRiderDropSet = mutableSetOf<String>()
    private val notifiedStopsSet = mutableSetOf<String>()

    fun evaluateConvoyState(
        currentUserId: String,
        currentUserRole: ConvoyRole,
        convoyMembers: List<ConvoyMember>
    ) {
        // Watchdog active for Lead and Sweep riders
        val isWatchdogRole = currentUserRole == ConvoyRole.LEAD || currentUserRole == ConvoyRole.SWEEP
        if (!isWatchdogRole) return

        for (member in convoyMembers) {
            if (member.userId == currentUserId) continue

            // Distance Behind Lead Check (> 3,000 meters / 3.0 km)
            val distanceKm = member.distanceBehindLeadMeters / 1000.0
            if (member.distanceBehindLeadMeters > 3000.0) {
                if (!notifiedRiderDropSet.contains(member.userId)) {
                    notifiedRiderDropSet.add(member.userId)

                    audioEngine.announceRiderDrop(member.displayName, distanceKm)

                    _activeBanner.value = AlertBanner(
                        title = "Pack Gap Warning",
                        message = "${member.displayName} is ${String.format("%.1f", distanceKm)} km behind the lead pack",
                        severity = AlertSeverity.WARNING
                    )
                }
            } else {
                // Reset drop alert if rider catches back up within 3km
                notifiedRiderDropSet.remove(member.userId)
            }
        }
    }

    fun processNewStopEvent(stopEvent: StopEvent) {
        if (!notifiedStopsSet.contains(stopEvent.stopId)) {
            notifiedStopsSet.add(stopEvent.stopId)

            val reasonName = stopEvent.reason.name.lowercase().replaceFirstChar { it.uppercase() }
            audioEngine.announceStopEvent(stopEvent.riderName, reasonName)

            val severity = if (stopEvent.reason.name == "BREAKDOWN") {
                AlertSeverity.CRITICAL
            } else {
                AlertSeverity.INFO
            }

            _activeBanner.value = AlertBanner(
                title = "Rider Stop Reported",
                message = "${stopEvent.riderName} stopped for $reasonName",
                severity = severity
            )
        }
    }

    fun dismissActiveBanner() {
        _activeBanner.value = null
    }
}
