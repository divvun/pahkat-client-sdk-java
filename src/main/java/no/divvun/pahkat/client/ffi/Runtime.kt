package no.divvun.pahkat.client.ffi

import arrow.core.Either

private var lastError: String? = null

public typealias Result<T> = Either<Exception, T>

internal val errorCallback = ErrorCallback { error ->
    lastError = error.getString(0, "UTF-8")
}

class PahkatClientException(message: String?): Exception(message)

internal fun <T> assertNoError(callback: () -> T): Result<T> {
    if (lastError != null) {
        val message = lastError
        lastError = null
        return Either.left(PahkatClientException(message))
    }

    return Either.right(callback())
}