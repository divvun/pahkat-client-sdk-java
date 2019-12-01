package no.divvun.pahkat.client.delegate

import no.divvun.pahkat.client.PackageKey
import java.lang.Exception

interface PackageTransactionDelegate {
    fun isTransactionCancelled(id: Long): Boolean

    fun onTransactionInstall(id: Long, packageKey: PackageKey)
    fun onTransactionUninstall(id: Long, packageKey: PackageKey)
    fun onTransactionCompleted(id: Long)
    fun onTransactionCancelled(id: Long)
    fun onTransactionError(id: Long, packageKey: PackageKey?, error: Exception?)
    fun onTransactionUnknownEvent(id: Long, packageKey: PackageKey, event: Long)
}