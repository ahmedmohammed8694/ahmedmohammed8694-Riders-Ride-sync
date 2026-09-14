package com.ridesync.engine

import android.location.Location
import kotlin.math.*

data class LatLng(val latitude: Double, val longitude: Double)

data class RouteTelemetryProjection(
    val alongTrackProgressMeters: Double,
    val crossTrackErrorMeters: Double,
    val isRouteDeviated: Boolean,
    val closestPolylineIndex: Int
)

class ConvoyGeometryEngine {

    private var consecutiveDeviationCount: Int = 0

    fun decodePolyline(encodedPolyline: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encodedPolyline.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encodedPolyline[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encodedPolyline[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }

    fun calculateRouteProjection(
        riderLocation: LatLng,
        routePolyline: List<LatLng>
    ): RouteTelemetryProjection {
        if (routePolyline.size < 2) {
            return RouteTelemetryProjection(0.0, 0.0, false, 0)
        }

        var minDistanceMeters = Double.MAX_VALUE
        var bestSegmentIndex = 0
        var bestAlongTrackMeters = 0.0

        val segmentCumulativeDistances = DoubleArray(routePolyline.size)
        var cumulative = 0.0
        segmentCumulativeDistances[0] = 0.0

        for (i in 0 until routePolyline.size - 1) {
            val dist = computeDistanceMeters(routePolyline[i], routePolyline[i + 1])
            cumulative += dist
            segmentCumulativeDistances[i + 1] = cumulative
        }

        for (i in 0 until routePolyline.size - 1) {
            val a = routePolyline[i]
            val b = routePolyline[i + 1]

            val (projectedPoint, fraction) = projectPointOntoSegment(riderLocation, a, b)
            val crossTrackDist = computeDistanceMeters(riderLocation, projectedPoint)

            if (crossTrackDist < minDistanceMeters) {
                minDistanceMeters = crossTrackDist
                bestSegmentIndex = i
                val segmentLength = computeDistanceMeters(a, b)
                bestAlongTrackMeters = segmentCumulativeDistances[i] + (fraction * segmentLength)
            }
        }

        // Deviation calculation (Trigger flag if cross-track error > 150m for 3 consecutive points)
        if (minDistanceMeters > 150.0) {
            consecutiveDeviationCount++
        } else {
            consecutiveDeviationCount = 0
        }

        val isDeviated = consecutiveDeviationCount >= 3

        return RouteTelemetryProjection(
            alongTrackProgressMeters = bestAlongTrackMeters,
            crossTrackErrorMeters = minDistanceMeters,
            isRouteDeviated = isDeviated,
            closestPolylineIndex = bestSegmentIndex
        )
    }

    fun calculateDistanceBehindLead(
        leadProgressMeters: Double,
        riderProgressMeters: Double
    ): Double {
        return (leadProgressMeters - riderProgressMeters).coerceAtLeast(0.0)
    }

    private fun projectPointOntoSegment(p: LatLng, a: LatLng, b: LatLng): Pair<LatLng, Double> {
        val x = p.longitude - a.longitude
        val y = p.latitude - a.latitude
        val dx = b.longitude - a.longitude
        val dy = b.latitude - a.latitude

        val segLenSq = dx * dx + dy * dy
        if (segLenSq == 0.0) return Pair(a, 0.0)

        val t = ((x * dx + y * dy) / segLenSq).coerceIn(0.0, 1.0)
        val projLng = a.longitude + t * dx
        val projLat = a.latitude + t * dy

        return Pair(LatLng(projLat, projLng), t)
    }

    fun computeDistanceMeters(p1: LatLng, p2: LatLng): Double {
        val results = FloatArray(1)
        Location.distanceBetween(
            p1.latitude, p1.longitude,
            p2.latitude, p2.longitude,
            results
        )
        return results[0].toDouble()
    }
}
