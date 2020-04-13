package no.divvun.pahkat.client

import arrow.core.Either
import com.sun.jna.Pointer
import no.divvun.pahkat.client.delegate.PackageTransactionDelegate
import no.divvun.pahkat.client.ffi.assertNoError
import no.divvun.pahkat.client.handler.transactionProcessCallbacks
import java.io.Serializable

private var nextTxId = 1L

class PackageTransaction<Target> internal constructor(
    private val handle: Pointer,
    val actions: List<TransactionAction<Target>>,
    private val processFunc: (Pointer, Long) -> Unit
): Serializable {
    val id = nextTxId

    // Increment txId each time a transaction is created
    init {
        nextTxId += 1
    }

    fun process(delegate: PackageTransactionDelegate) {
        transactionProcessCallbacks[id] = delegate
        processFunc(handle, id)

        when (val result = assertNoError { }) {
            is Either.Left -> delegate.onTransactionError(id, null, result.a)
            is Either.Right -> {}
        }

        transactionProcessCallbacks.remove(id)
    }
}