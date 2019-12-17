package no.divvun.pahkat.client

import no.divvun.pahkat.client.ffi.assertNoError
import no.divvun.pahkat.client.ffi.errorCallback
import no.divvun.pahkat.client.ffi.pahkat_client
import no.divvun.pahkat.client.ffi.Result
import javax.annotation.CheckReturnValue

object PahkatClient {
    fun enableLogging() {
        pahkat_client.pahkat_enable_logging()
    }

    object Android {
        @CheckReturnValue
        fun init(containerPath: String): Result<Unit> {
            pahkat_client.pahkat_android_init(containerPath, errorCallback)
            return assertNoError { }
        }
    }
}