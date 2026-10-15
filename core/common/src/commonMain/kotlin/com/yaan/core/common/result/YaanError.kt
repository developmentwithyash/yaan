package com.yaan.core.common.result

/**
 * Domain-specific error taxonomy across YAAN sub-systems.
 */
sealed interface YaanError {
    val message: String

    data class StorageError(
        override val message: String,
        val cause: Throwable? = null
    ) : YaanError

    data class CryptoError(
        override val message: String,
        val cause: Throwable? = null
    ) : YaanError

    data class SyncError(
        override val message: String,
        val code: Int = 0
    ) : YaanError

    data class NetworkError(
        override val message: String,
        val statusCode: Int? = null
    ) : YaanError

    data class ValidationError(
        val field: String,
        override val message: String
    ) : YaanError

    data class AuthenticationError(
        override val message: String,
        val cause: Throwable? = null
    ) : YaanError

    data class NotFoundError(
        override val message: String,
        val cause: Throwable? = null
    ) : YaanError

    data class UnknownError(
        override val message: String,
        val cause: Throwable? = null
    ) : YaanError
}
