package no.divvun.pahkat.client

import no.divvun.pahkat.client.ffi.assertNoError
import no.divvun.pahkat.client.ffi.errorCallback
import no.divvun.pahkat.client.ffi.pahkat_client
import no.divvun.pahkat.client.ffi.Result
import javax.annotation.CheckReturnValue

object PahkatClient {
    enum class LogLevel(val value: Byte) {
        NONE(0),
        ERROR(1),
        WARN(2),
        INFO(3),
        DEBUG(4),
        TRACE(5)
    }

    fun enableLogging(level: LogLevel = LogLevel.INFO) {
        pahkat_client.pahkat_enable_logging(level.value)
    }

    object Android {
        @CheckReturnValue
        fun init(containerPath: String): Result<Unit> {
//            pahkat_client.pahkat_android_init(containerPath, errorCallback)
            return assertNoError { }
        }
    }
}