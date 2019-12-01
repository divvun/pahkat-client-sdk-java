package no.divvun.pahkat.client

import com.sun.jna.Pointer
import no.divvun.pahkat.client.ffi.Result
import no.divvun.pahkat.client.ffi.assertNoError
import no.divvun.pahkat.client.ffi.errorCallback
import no.divvun.pahkat.client.ffi.pahkat_client

class StoreConfig internal constructor(private val handle: Pointer) {
    private val gson = createGson()

    fun configPath(): Result<String> {
        val slice = pahkat_client.pahkat_store_config_config_path(handle, errorCallback)
        return assertNoError { slice.decode()!! }
    }

    fun setUiSetting(key: String, value: String?): Result<Unit> {
        pahkat_client.pahkat_store_config_set_ui_value(handle, key, value, errorCallback)
        return assertNoError {}
    }

    fun getUiSetting(key: String): Result<String?> {
        val ptr = pahkat_client.pahkat_store_config_ui_value(handle, key, errorCallback)
        return assertNoError { ptr.string() }
    }

    fun repos(): Result<List<RepoRecord>> {
        val ptr = pahkat_client.pahkat_store_config_repos(handle, errorCallback)
        return assertNoError {
            gson.fromJson<List<RepoRecord>>(ptr.string()!!)
        }
    }

    fun setRepos(repos: List<RepoRecord>): Result<Unit> {
        pahkat_client.pahkat_store_config_set_repos(handle, gson.toJson(repos), errorCallback)
        return assertNoError {}
    }


}