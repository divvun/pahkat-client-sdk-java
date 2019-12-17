package no.divvun.pahkat.client

import no.divvun.pahkat.client.ffi.assertNoError
import no.divvun.pahkat.client.ffi.errorCallback
import no.divvun.pahkat.client.ffi.pahkat_client

object PahkatClient {
    fun enableLogging() {
        pahkat_client.pahkat_enable_logging()
    }

    object Android {
        fun init(containerPath: String) {
            pahkat_client.pahkat_android_init(containerPath, errorCallback)
            assertNoError { }
        }
    }
}