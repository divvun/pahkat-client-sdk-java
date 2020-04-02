package no.divvun.pahkat.client.ffi

import arrow.core.Either
import java.nio.charset.StandardCharsets.UTF_8

private var lastError: String? = null

public typealias Result<T> = Either<Exception, T>

internal val errorCallback = ErrorCallback { ptr, size ->
    val bytes = ptr.getByteArray(0, size.toInt())
    lastError = String(bytes, UTF_8)
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
