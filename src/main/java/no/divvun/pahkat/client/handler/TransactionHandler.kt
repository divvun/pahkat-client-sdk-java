package no.divvun.pahkat.client.handler

import com.sun.jna.Pointer
import no.divvun.pahkat.client.PackageKey
import no.divvun.pahkat.client.delegate.PackageTransactionDelegate
import no.divvun.pahkat.client.ffi.SlicePointer
import no.divvun.pahkat.client.ffi.pahkat_client

enum class PackageTransactionEvent(val value: Byte) {
    NotStarted(0),
    Uninstalling(1),
    Installing(2),
    Completed(3),
    Error(4);

    companion object {
        fun from(value: Int): PackageTransactionEvent? {
            return when (value) {
                0 -> NotStarted
                1 -> Installing
                2 -> Uninstalling
                3 -> Completed
                4 -> Error
                else -> null
            }
        }
    }
}

internal var transactionProcessCallbacks = mutableMapOf<Long, PackageTransactionDelegate>()
internal val transactionProcessHandler: pahkat_client.TransactionProcessCallback = object : pahkat_client.TransactionProcessCallback {
    override fun invoke(cTag: pahkat_client.uint32_t, packageKeyStr: SlicePointer.ByValue, cEvent: pahkat_client.uint32_t): Byte {
        val tag = cTag.toLong()
        val delegate = transactionProcessCallbacks[tag] ?: return 0

        if (delegate.isTransactionCancelled(tag)) {
            delegate.onTransactionCancelled(tag)
            transactionProcessCallbacks.remove(tag)
            return 0
        }


        val event = PackageTransactionEvent.from(cEvent.toInt())
        if (event == null) {
            delegate.onTransactionUnknownEvent(tag, cEvent.toLong())
            return if (delegate.isTransactionCancelled(tag)) { 0 } else { 1 }
        }

        val packageKey = if (Pointer.nativeValue(packageKeyStr.len).toInt() != 0) {
            val cPackageKey = packageKeyStr.decode()
            if (cPackageKey != null) {
                PackageKey.from(cPackageKey)
            } else {
                null
            }
        } else {
            null
        }

        when (event) {
            PackageTransactionEvent.Installing -> delegate.onTransactionInstall(tag, packageKey)
            PackageTransactionEvent.Uninstalling -> delegate.onTransactionUninstall(tag, packageKey)
            PackageTransactionEvent.Error -> delegate.onTransactionError(tag, packageKey, null)
            PackageTransactionEvent.Completed -> delegate.onTransactionCompleted(tag)
            PackageTransactionEvent.NotStarted -> {}
        }

        return if (delegate.isTransactionCancelled(tag)) { 0 } else { 1 }
    }

}