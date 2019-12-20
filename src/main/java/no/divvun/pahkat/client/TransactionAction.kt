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

data class TransactionAction<Target>(
    val action: PackageActionType,
    val id: PackageKey,
    val target: Target
) {
    companion object {
        fun <Target> install(packageKey: PackageKey, target: Target): TransactionAction<Target> {
            return TransactionAction(PackageActionType.Install, packageKey, target)
        }

        fun <Target> uninstall(packageKey: PackageKey, target: Target): TransactionAction<Target> {
            return TransactionAction(PackageActionType.Uninstall, packageKey, target)
        }

        fun <Target> fromJson(json: String): TransactionAction<Target> {
            return gson.fromJson<TransactionAction<Target>>(json)
        }
    }

    override fun toString(): String = gson.toJson(this)
}
