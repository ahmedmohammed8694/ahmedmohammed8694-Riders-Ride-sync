package com.ridesync.ui.map

import com.google.android.gms.maps.model.LatLng
import kotlin.math.*

object LatLngEvaluator {

    fun interpolate(fraction: Float, start: LatLng, end: LatLng): LatLng {
        val lat = (end.latitude - start.latitude) * fraction + start.latitude
        var lngDelta = end.longitude - start.longitude

        // Take the shortest path across 180th meridian
        if (abs(lngDelta) > 180) {
            lngDelta -= sign(lngDelta) * 360
        }
        val lng = lngDelta * fraction + start.longitude
        return LatLng(lat, lng)
    }

    fun interpolateBearing(fraction: Float, startBearing: Float, endBearing: Float): Float {
        var diff = (endBearing - startBearing) % 360
        if (diff < -180) diff += 360
        if (diff > 180) diff -= 360
        return (startBearing + diff * fraction + 360) % 360
    }
}
