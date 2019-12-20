package no.divvun.pahkat.client

import arrow.core.Either
import arrow.core.flatMap
import com.google.gson.Gson
import com.sun.jna.Pointer
import no.divvun.pahkat.client.delegate.PackageDownloadDelegate
import no.divvun.pahkat.client.ffi.*
import no.divvun.pahkat.client.handler.downloadProcessCallbacks
import no.divvun.pahkat.client.handler.downloadProcessHandler
import no.divvun.pahkat.client.handler.transactionProcessHandler

inline fun <reified T> Gson.fromJson(value: String): T {
    return this.fromJson(value, T::class.java)
}

internal fun Pointer.string(): String? {
    if (this == Pointer.NULL) {
        return null
    }
    val s = this.getString(0, "UTF-8")
    pahkat_client.pahkat_str_free(this)
    return s
}

class PrefixPackageStore private constructor(private val handle: Pointer) : PackageStore<Unit> {
    companion object {
        fun open(path: String): Result<PrefixPackageStore> {
            val result = pahkat_client.pahkat_prefix_package_store_open(path, errorCallback)
            return assertNoError { PrefixPackageStore(result) }
        }

        fun create(path: String): Result<PrefixPackageStore> {
            val result = pahkat_client.pahkat_prefix_package_store_create(path, errorCallback)
            return assertNoError { PrefixPackageStore(result) }
        }
    }

    override fun config(): Result<StoreConfig> {
        val ptr = pahkat_client.pahkat_prefix_package_store_config(handle, errorCallback)
        return assertNoError { StoreConfig(ptr) }
    }

    override fun repoIndexes(withStatuses: Boolean): Result<Array<RepositoryIndex>> {
        val ptr = pahkat_client.pahkat_prefix_package_store_repo_indexes(handle, errorCallback)
        val result = assertNoError {
            gson.fromJson<Array<RepositoryIndex>>(ptr.string()!!)
        }

        if (withStatuses) {
            return result.flatMap { indexes ->
                for (index in indexes) {
                    val record = RepoRecord(index.meta.base, index.channel)
                    index.statuses = when (val r = allStatuses(record)) {
                        is Either.Left -> return@flatMap Either.left(r.a)
                        is Either.Right -> r.b.mapKeys { PackageKey.from(index, it.key) }
                    }
                }

                Either.right(indexes)
            }
        } else {
            return result
        }
    }

    fun allStatuses(repo: RepoRecord): Result<Map<String, PackageStatusResponse>> {
        return allStatuses(repo, Unit)
    }

    override fun allStatuses(repo: RepoRecord, target: Unit): Result<Map<String, PackageStatusResponse>> {
        val ptr = pahkat_client.pahkat_prefix_package_store_all_statuses(handle, gson.toJson(repo), errorCallback)
        return assertNoError {
            val jsonString = ptr.string()!!
            gson.fromJson<Map<String, Byte>>(jsonString)
                .mapValues {
                    PackageStatusResponse(
                        PackageInstallStatus.from(it.value) ?: PackageInstallStatus.InvalidMetadata,
                        InstallerTarget.System
                    )
                }
        }
    }

    class DownloadCancelledException : Exception()

    override fun download(packageKey: PackageKey, delegate: PackageDownloadDelegate): Result<String?> {
        if (downloadProcessCallbacks.containsKey(packageKey)) {
            return Either.Left(Exception("Package key already found in callbacks"))
        }

        downloadProcessCallbacks[packageKey] = delegate

        val slice = pahkat_client.pahkat_prefix_package_store_download(
            handle, packageKey.toString(), downloadProcessHandler, errorCallback)

        downloadProcessCallbacks.remove(packageKey)

        if (delegate.isDownloadCancelled) {
            delegate.onDownloadCancel(packageKey)
            return Either.Left(DownloadCancelledException())
        }

        return assertNoError({
            val path = slice.decode()!!
            delegate.onDownloadComplete(packageKey, path)
            path
        }, { message ->
            delegate.onDownloadError(packageKey, PahkatClientException(message))
            Either.right(null)
        })
    }

    override fun import(packageKey: PackageKey, installerPath: String): Result<String> {
        val string = pahkat_client.pahkat_prefix_package_store_import(handle, packageKey.toString(), installerPath, errorCallback)
        return assertNoError { string.decode()!! }
    }

//    override fun findPackage(byId: String): Result<Pair<PackageKey, Package>?> {
//        pahkat_client.pahkat_prefix_package_store_find_package_by_key()
//    }

    override fun findPackage(byKey: PackageKey): Result<Package?> {
        val ptr = pahkat_client.pahkat_prefix_package_store_find_package_by_key(handle, byKey.toString(), errorCallback)
        return assertNoError { ptr.string() }.flatMap {
            if (it == null) {
                Either.Right(null)
            } else {
                try {
                    Either.Right(gson.fromJson<Package>(it))
                } catch (e: Exception) {
                    Either.Left(e)
                }
            }
        }
    }

    override fun refreshRepos(): Result<Unit> {
        pahkat_client.pahkat_prefix_package_store_refresh_repos(handle, errorCallback)
        return assertNoError {}
    }

    override fun clearCache(): Result<Unit> {
        pahkat_client.pahkat_prefix_package_store_clear_cache(handle, errorCallback)
        return assertNoError {}
    }

    override fun forceRefreshRepos(): Result<Unit> {
        pahkat_client.pahkat_prefix_package_store_force_refresh_repos(handle, errorCallback)
        return assertNoError {}
    }

    override fun transaction(actions: List<TransactionAction<Unit>>): Result<PackageTransaction<Unit>> {
        val actionsJson = gson.toJson(actions)
        println(actionsJson)
        val ptr = pahkat_client.pahkat_prefix_transaction_new(handle, actionsJson, errorCallback)
        return assertNoError {
            PackageTransaction(
                ptr,
                actions
            ) { ptr, id ->
                pahkat_client.pahkat_prefix_transaction_process(
                    ptr,
                    pahkat_client.uint32_t(id.toInt()),
                    transactionProcessHandler,
                    errorCallback
                )
            }
        }
    }


}