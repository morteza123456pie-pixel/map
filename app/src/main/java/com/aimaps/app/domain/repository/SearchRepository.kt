package com.aimaps.app.domain.repository

/**
 * Placeholder for the Phase 2 place-search feature.
 *
 * The search bar in Phase 1 is intentionally UI-only. This interface exists now so that
 * adding a real implementation later is a matter of providing a new binding in
 * `RepositoryModule` — no call sites, no ViewModel signatures, and no UI code need to
 * change.
 */
interface SearchRepository {

    /**
     * Runs a free-text query. Returning [SearchResult] rather than a list leaves room for
     * pagination cursors and result attribution without breaking callers.
     */
    suspend fun searchPlaces(query: String): SearchResult
}

/** Phase 2 will populate this from a geocoding/POI backend. */
data class SearchResult(
    val places: List<Place>,
    val nextPageToken: String? = null,
)

/**
 * A place as the search/places features will model it. Phase 1 renders no places, but the
 * type is fixed here so the domain contract is stable across phases.
 */
data class Place(
    val id: String,
    val name: String,
    val point: com.aimaps.app.domain.model.GeoPoint,
    val address: String? = null,
)
