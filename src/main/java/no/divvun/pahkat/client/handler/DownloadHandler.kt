package no.divvun.pahkat.client.handler

import no.divvun.pahkat.client.PackageKey
import no.divvun.pahkat.client.delegate.PackageDownloadDelegate
import no.divvun.pahkat.client.ffi.DownloadCallback
import no.divvun.pahkat.client.ffi.SlicePointer
import no.divvun.pahkat.client.ffi.pahkat_client

internal var downloadProcessCallbacks = mutableMapOf<String, PackageDownloadDelegate>()

internal val downloadProcessHandler: DownloadCallback = object : DownloadCallback {
    override fun invoke(packageId: String, currentRaw: pahkat_client.uint64_t, totalRaw: pahkat_client.uint64_t): Boolean {
        val current = currentRaw.toLong()
        val total = totalRaw.toLong()
//        val cPackageKey = packageId.decode() ?: return false

//        val packageKey = PackageKey.from(cPackageKey)
        val packageKey = PackageKey.from(packageId)
        var s = packageKey.toString(withQueryParams = false)
        val delegate = downloadProcessCallbacks[s] ?: return false

        delegate.onDownloadProgress(packageKey, current, total)

        return !delegate.isDownloadCancelled
    }
}