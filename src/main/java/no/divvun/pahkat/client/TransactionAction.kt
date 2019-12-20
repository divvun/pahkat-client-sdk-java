package no.divvun.pahkat.client

import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.Serializable

enum class PackageActionType(val value: String): Serializable {
    Install("install"),
    Uninstall("uninstall")
}

data class TransactionAction<Target: Serializable>(
    val action: PackageActionType,
    val id: PackageKey,
    val target: Target
): Serializable {
    companion object {
        fun <Target: Serializable> install(packageKey: PackageKey, target: Target): TransactionAction<Target> {
            return TransactionAction(PackageActionType.Install, packageKey, target)
        }

        fun <Target: Serializable> uninstall(packageKey: PackageKey, target: Target): TransactionAction<Target> {
            return TransactionAction(PackageActionType.Uninstall, packageKey, target)
        }
    }
}