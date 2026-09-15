package com.aimaps.app.core.common

/**
 * Minimal result wrapper used by repositories so the UI layer never has to catch
 * exceptions thrown by data sources. Kotlin's own `Result` is avoided because it cannot
 * carry a typed [AppError].
 */
sealed interface AppResult<out T> {

    data class Success<T>(val data: T) : AppResult<T>

    data class Failure(val error: AppError) : AppResult<Nothing>
}

/** Error taxonomy shared by every data source in the app. */
sealed interface AppError {

    /** The user has not granted the permission an operation requires. */
    data object PermissionDenied : AppError

    /** A device-level service the operation depends on is switched off. */
    data object ServiceDisabled : AppError

    /** The service is available but produced no value — no satellite fix indoors, typically. */
    data object Unavailable : AppError

    /** No network connection is available. */
    data object NoConnection : AppError

    /** The operation did not produce a value in the allotted time. */
    data object Timeout : AppError

    /** Anything we did not anticipate. [cause] is for logging, never for the UI. */
    data class Unexpected(val cause: Throwable? = null) : AppError
}
