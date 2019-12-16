package no.divvun.pahkat.client.ffi

import arrow.core.Either

private var lastError: String? = null

public typealias Result<T> = Either<Exception, T>

internal val errorCallback = ErrorCallback { error ->
    lastError = error.getString(0, "UTF-8")
}

class PahkatClientException(message: String?): Exception(message)

internal fun <T> assertNoError(callback: () -> T): Result<T> {
    return lastError?.let {
        lastError = null
        Either.left(PahkatClientException(it))
    } ?: Either.right(callback())
}

internal fun <T> assertNoError(callback: () -> T, errorCallback: ((String) -> Result<T>) = { Either.left(PahkatClientException(it)) }): Result<T> {
    return lastError?.let {
        val message = it
        lastError = null
        errorCallback(message)
    } ?: Either.right(callback())
}
