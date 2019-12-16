package no.divvun.pahkat.client.handler

import no.divvun.pahkat.client.PackageKey
import no.divvun.pahkat.client.delegate.PackageDownloadDelegate
import no.divvun.pahkat.client.ffi.DownloadCallback
import no.divvun.pahkat.client.ffi.pahkat_client

internal var downloadProcessCallbacks = mutableMapOf<PackageKey, PackageDownloadDelegate>()

internal val downloadProcessHandler: DownloadCallback = object : DownloadCallback {
    override fun invoke(packageId: String, currentRaw: pahkat_client.uint64_t, totalRaw: pahkat_client.uint64_t): Boolean {
        val current = currentRaw.toLong()
        val total = totalRaw.toLong()

        val packageKey = PackageKey.from(packageId)

        val delegate = downloadProcessCallbacks[packageKey] ?: return false

        delegate.onDownloadProgress(packageKey, current, total)

        return !delegate.isDownloadCancelled
    }
}