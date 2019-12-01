package no.divvun.pahkat.client.delegate

import no.divvun.pahkat.client.PackageKey
import java.lang.Exception

interface PackageDownloadDelegate {
    val isDownloadCancelled: Boolean

    fun onDownloadProgress(packageKey: PackageKey, current: Long, maximum: Long)
    fun onDownloadComplete(packageKey: PackageKey, path: String)
    fun onDownloadCancel(packageKey: PackageKey)
    fun onDownloadError(packageKey: PackageKey, error: Exception)
}