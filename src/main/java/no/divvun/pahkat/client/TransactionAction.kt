package no.divvun.pahkat.client

enum class PackageActionType(val value: String) {
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
    }
}