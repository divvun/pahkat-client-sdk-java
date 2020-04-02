package no.divvun.pahkat.client

import com.google.gson.reflect.TypeToken
import com.sun.jna.Pointer
import no.divvun.pahkat.client.ffi.Result
import no.divvun.pahkat.client.ffi.assertNoError
import no.divvun.pahkat.client.ffi.errorCallback
import no.divvun.pahkat.client.ffi.pahkat_client
import javax.annotation.CheckReturnValue

class StoreConfig internal constructor(private val handle: Pointer) {
//    @CheckReturnValue
//    fun configPath(): Result<String> {
//        val slice = pahkat_client.pahkat_store_config_config_path(handle, errorCallback)
//        return assertNoError { slice.decode()!! }
//    }
//
//    @CheckReturnValue
//    fun setUiSetting(key: String, value: String?): Result<Unit> {
//        pahkat_client.pahkat_store_config_set_ui_value(handle, key, value, errorCallback)
//        return assertNoError {}
//    }
//
//    @CheckReturnValue
//    fun getUiSetting(key: String): Result<String?> {
//        val ptr = pahkat_client.pahkat_store_config_ui_value(handle, key, errorCallback)
//        return assertNoError { ptr.string() }
//    }
//
//    @CheckReturnValue
//    fun repos(): Result<Array<RepoRecord>> {
//        val ptr = pahkat_client.pahkat_store_config_repos(handle, errorCallback)
//        return assertNoError {
//            gson.fromJson(ptr.string()!!, Array<RepoRecord>::class.java)
//        }
//    }
//
//    @CheckReturnValue
//    fun setRepos(repos: List<RepoRecord>): Result<Unit> {
//        pahkat_client.pahkat_store_config_set_repos(handle, gson.toJson(repos), errorCallback)
//        return assertNoError {}
//    }
}