package com.aimaps.app.core.di

import javax.inject.Qualifier

/**
 * Qualifiers for the coroutine dispatchers the app injects. Hard-coding
 * `Dispatchers.IO` inside data sources makes them untestable; injecting the dispatcher
 * lets tests substitute a `TestDispatcher`.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
