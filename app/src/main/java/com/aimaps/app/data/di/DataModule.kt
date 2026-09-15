package com.aimaps.app.data.di

import com.aimaps.app.data.location.AndroidLocationDataSource
import com.aimaps.app.data.location.LocationDataSource
import com.aimaps.app.data.location.LocationRepositoryImpl
import com.aimaps.app.data.network.AndroidNetworkMonitor
import com.aimaps.app.data.network.NetworkMonitor
import com.aimaps.app.data.search.SearchRepositoryStub
import com.aimaps.app.domain.repository.LocationRepository
import com.aimaps.app.domain.repository.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    /**
     * Binds the platform-backed implementation. Swapping in a fused provider later means
     * changing this one line and nothing else.
     */
    @Binds
    @Singleton
    abstract fun bindLocationDataSource(implementation: AndroidLocationDataSource): LocationDataSource

    @Binds
    @Singleton
    abstract fun bindLocationRepository(implementation: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(implementation: AndroidNetworkMonitor): NetworkMonitor

    /**
     * Phase 1 binds the inert stub. Phase 2 binds a networked implementation here instead,
     * and no other file has to change.
     */
    @Binds
    @Singleton
    abstract fun bindSearchRepository(implementation: SearchRepositoryStub): SearchRepository

    /*
     * `PlacesRepository` is intentionally left unbound: it has no Phase 1 implementation and
     * no Phase 1 code injects it. The interface exists so the map layer can depend on it in
     * Phase 3 without a redesign. A stub binding now would only disguise that it does nothing.
     */
}
