package com.yaan.core.common.result

/**
 * Functional Result monad for deterministic, fail-safe operations.
 */
sealed class YaanResult<out T> {
    data class Success<out T>(val data: T) : YaanResult<T>()
    data class Failure(val error: YaanError) : YaanResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Failure -> defaultValue
    }

    inline fun <R> map(transform: (T) -> R): YaanResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> Failure(error)
    }

    inline fun <R> flatMap(transform: (T) -> YaanResult<R>): YaanResult<R> = when (this) {
        is Success -> transform(data)
        is Failure -> Failure(error)
    }

    inline fun onSuccess(action: (T) -> Unit): YaanResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onFailure(action: (YaanError) -> Unit): YaanResult<T> {
        if (this is Failure) action(error)
        return this
    }

    inline fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (YaanError) -> R
    ): R = when (this) {
        is Success -> onSuccess(data)
        is Failure -> onFailure(error)
    }
}
