package com.aimaps.app.domain.repository

import com.aimaps.app.domain.model.GeoPoint

/**
 * Placeholder for the Phase 3 places feature (reverse geocoding, nearby POIs, details).
 *
 * Deliberately unimplemented: Phase 1 must not resolve addresses or fetch POIs. The
 * signature is fixed so the map layer can start calling `pointsOfInterestAround(cameraTarget)`
 * in a later phase without this file changing.
 */
interface PlacesRepository {

    /** Human-readable addresses for a coordinate, used to label the "you are here" chip. */
    suspend fun reverseGeocode(point: GeoPoint): List<String>

    /** Nearby points of interest, e.g. to populate a "what's around me" sheet. */
    suspend fun pointsOfInterestAround(point: GeoPoint, radiusMetres: Int): List<Place>
}
