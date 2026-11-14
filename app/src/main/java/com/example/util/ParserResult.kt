package com.example.util

/**
 * A sealed class representing the result of an HTML parsing operation,
 * capturing whether parsing succeeded with data or failed with a descriptive reason.
 */
sealed class ParserResult<out T> {

    data class Success<out T>(val data: T) : ParserResult<T>()

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : ParserResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> default
    }

    inline fun <R> map(transform: (T) -> R): ParserResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    inline fun onSuccess(action: (T) -> Unit): ParserResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onFailure(action: (Error) -> Unit): ParserResult<T> {
        if (this is Error) action(this)
        return this
    }
}
