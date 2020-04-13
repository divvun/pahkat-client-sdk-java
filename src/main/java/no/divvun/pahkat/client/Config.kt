package no.divvun.pahkat.client

import com.google.gson.reflect.TypeToken
import com.sun.jna.Pointer
import no.divvun.pahkat.client.ffi.Result
import no.divvun.pahkat.client.ffi.assertNoError
import no.divvun.pahkat.client.ffi.errorCallback
import no.divvun.pahkat.client.ffi.pahkat_client
import javax.annotation.CheckReturnValue

class Config internal constructor(private val handle: Pointer) {
//    @CheckReturnValue
//    fun configPath(): Result<String> {
//        val slice = pahkat_client.pahkat_store_config_config_path(handle, errorCallback)
//        return assertNoError { slice.decode()!! }
//    }

    @CheckReturnValue
    fun repos(): Result<Map<String, RepoRecord>> {
        val ptr = pahkat_client.pahkat_config_repos_get(handle, errorCallback)
        return assertNoError {
            gson.fromJson<Map<String, RepoRecord>>(ptr.decode()!!)
        }
    }

    @CheckReturnValue
    fun setRepos(repos: Map<String, RepoRecord>): Result<Unit> {
        gson.toJson(repos).withSlice {
            pahkat_client.pahkat_config_repos_set(handle, it, errorCallback)
        }
        return assertNoError {}
    }
}