package no.divvun.pahkat.client

import arrow.core.Either
import com.sun.jna.Pointer
import no.divvun.pahkat.client.delegate.PackageTransactionDelegate
import no.divvun.pahkat.client.ffi.assertNoError
import no.divvun.pahkat.client.handler.transactionProcessCallbacks

private var nextTxId = 1L

class PackageTransaction<Target> internal constructor(
    private val handle: Pointer,
    private val actions: List<TransactionAction<Target>>,
    private val processFunc: (Pointer, Long) -> Unit
) {
    fun process(delegate: PackageTransactionDelegate) {
        val id = nextTxId
        nextTxId += 1

        transactionProcessCallbacks[id] = delegate
        processFunc(handle, id)

        when (val result = assertNoError { }) {
            is Either.Left -> delegate.onTransactionError(id, null, result.a)
            is Either.Right -> delegate.onTransactionCompleted(id)
        }

        transactionProcessCallbacks.remove(id)
    }
}