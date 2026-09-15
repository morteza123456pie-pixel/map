package com.aimaps.app.data.location

import android.location.Location
import com.aimaps.app.domain.model.GeoPoint
import com.aimaps.app.domain.model.UserLocation

/**
 * Maps the platform [Location] to the domain snapshot the UI consumes.
 *
 * `hasBearing()` returns true for many stationary network fixes that report 0 degrees,
 * which would peg a heading indicator to north. A bearing is therefore only trusted from
 * a provider that also reports speed, i.e. one genuinely tracking movement.
 */
internal fun Location.toUserLocation(): UserLocation = UserLocation(
    point = GeoPoint(latitude = latitude, longitude = longitude),
    accuracyMetres = if (hasAccuracy()) accuracy else null,
    bearingDegrees = if (hasBearing() && hasSpeed() && bearing.isFinite()) bearing else null,
    hasSpeed = hasSpeed(),
)
