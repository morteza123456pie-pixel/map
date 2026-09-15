package com.aimaps.app.data.search

import com.aimaps.app.domain.repository.Place
import com.aimaps.app.domain.repository.SearchRepository
import com.aimaps.app.domain.repository.SearchResult
import javax.inject.Inject

/**
 * Phase 1 stand-in for [SearchRepository].
 *
 * The search field is deliberately inert at this stage, but the dependency graph is
 * already complete: the ViewModel injects `SearchRepository`, and Phase 2 replaces this
 * binding with a networked implementation without touching the UI or domain layers.
 *
 * Querying returns an empty result rather than throwing, so wiring up the call site early
 * cannot crash the app.
 */
class SearchRepositoryStub @Inject constructor() : SearchRepository {

    override suspend fun searchPlaces(query: String): SearchResult = SearchResult(places = emptyList<Place>())
}
