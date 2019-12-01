package no.divvun.pahkat.client

import arrow.core.Either
import arrow.core.flatMap
import com.google.gson.Gson
import com.sun.jna.Pointer
import no.divvun.pahkat.client.delegate.PackageDownloadDelegate
import no.divvun.pahkat.client.ffi.assertNoError
import no.divvun.pahkat.client.ffi.errorCallback
import no.divvun.pahkat.client.ffi.pahkat_client
import no.divvun.pahkat.client.ffi.Result
import no.divvun.pahkat.client.handler.transactionProcessHandler
import java.nio.charset.StandardCharsets

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

    private val gson = createGson()

    override fun config(): Result<StoreConfig> {
        val ptr = pahkat_client.pahkat_prefix_package_store_config(handle, errorCallback)
        return assertNoError { StoreConfig(ptr) }
    }

    override fun repoIndexes(withStatuses: Boolean): Result<List<RepositoryIndex>> {
        val ptr = pahkat_client.pahkat_prefix_package_store_repo_indexes(handle, errorCallback)
        return assertNoError {
            gson.fromJson<List<RepositoryIndex>>(ptr.string()!!)
        }
    }

    override fun allStatuses(repo: RepoRecord, target: Unit): Result<Map<String, PackageStatusResponse>> {
        val ptr = pahkat_client.pahkat_prefix_package_store_all_statuses(handle, gson.toJson(repo), errorCallback)
        return assertNoError {
            gson.fromJson<Map<String, PackageStatusResponse>>(ptr.string()!!)
        }
    }


    override fun download(packageKey: PackageKey, delegate: PackageDownloadDelegate): Result<Unit> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        val ptr = pahkat_client.pahkat_prefix_transaction_new(handle, gson.toJson(actions), errorCallback)
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