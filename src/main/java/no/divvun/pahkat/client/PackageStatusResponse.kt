package no.divvun.pahkat.client

data class PackageStatusResponse(
    val status: PackageInstallStatus,
    val target: InstallerTarget
)

enum class PackageInstallStatus constructor(val value: Byte) {
    NotInstalled(0),
    UpToDate(1),
    RequiresUpdate(2),
    VersionSkipped(3),

    // Errors
    NoPackage(-1),
    NoInstaller(-2),
    WrongInstallerType(-3),
    ParsingVersion(-4),
    InvalidInstallPath(-5),
    InvalidMetadata(-6);

    companion object {
        fun from(value: Byte): PackageInstallStatus? {
            return PackageInstallStatus.values().firstOrNull { it.value == value }
        }
    }
}

enum class InstallerTarget {
    System,
    User;

    fun toInt(): Int {
        return if (this == System) {
            0
        } else {
            1
        }
    }

    override fun toString(): String {
        return if (this == System) {
            "system"
        } else {
            "user"
        }
    }
}
