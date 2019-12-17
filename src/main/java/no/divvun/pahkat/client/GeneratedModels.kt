@file:Suppress("MoveLambdaOutsideParentheses")
package no.divvun.pahkat.client

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.Serializable
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

@Target(AnnotationTarget.TYPE)
@MustBeDocumented
annotation class Format(val value: String)

interface JsonEnum {
    val value: String
}

internal fun createGson(): Gson {
    fun createDateFormatter(pattern: String, tz: String): SimpleDateFormat {
        val df = SimpleDateFormat(pattern, Locale.ROOT)
        df.timeZone = TimeZone.getTimeZone(tz)
        return df
    }

    class EnumTypeAdapter<T>(private val type: T) : TypeAdapter<T>() where T: JsonEnum {
        override fun write(writer: JsonWriter, value: T) {
            writer.value(value.value)
        }

        override fun read(reader: JsonReader): T {
            val s = reader.nextString()
            return type::class.java.enumConstants.first { it.value == s }
                ?: throw Exception("Invalid value: $s")
        }
    }

    class DateAdapter(format: String) : TypeAdapter<Date>() {
        private val formatter = when (format) {
            "date" -> createDateFormatter("yyyy-MM-dd", "UTC")
            else -> createDateFormatter("yyyy-MM-dd'T'HH:mm:ss'Z'", "UTC")
        }

        override fun write(writer: JsonWriter, value: Date) {
            writer.value(formatter.format(value))
        }

        override fun read(reader: JsonReader): Date {
            return formatter.parse(reader.nextString())
        }
    }

    class DateAdapterFactory : TypeAdapterFactory {
        override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
            if (type.rawType != Date::class.java) {
                return null
            }

            val format = type.rawType.getAnnotation(Format::class.java)?.value ?: "date-time"
            @Suppress("UNCHECKED_CAST")
            return DateAdapter(format).nullSafe() as TypeAdapter<T>
        }
    }

    class EnumTypeAdapterFactory : TypeAdapterFactory {
        override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
            if (!type.rawType.interfaces.contains(JsonEnum::class.java)) {
                return null
            }

            @Suppress("UNCHECKED_CAST")
            return EnumTypeAdapter(type.rawType as JsonEnum) as TypeAdapter<T>
        }
    }

    return GsonBuilder()
        .registerTypeAdapterFactory(EnumTypeAdapterFactory())
        .registerTypeAdapterFactory(DateAdapterFactory())
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        .create()
}

data class Repository(
    @SerializedName("@type")
    val _type: _Type?,
    val agent: RepositoryAgent?,
    val base: @Format("uri") URI,
    val name: @Format("bcp47-tag") Map<String, String>,
    val description: @Format("bcp47-tag") Map<String, String>,
    val primaryFilter: PrimaryFilter,
    val channels: List<Channel>
) : Serializable {
    data class RepositoryAgent(
        val name: String,
        val version: String,
        val url: @Format("uri") URI?
    ) : Serializable {
    }
    enum class _Type(override val value: String): JsonEnum {
        REPOSITORY("Repository");

        override fun toString() = value
    }

    enum class PrimaryFilter(override val value: String): JsonEnum {
        CATEGORY("category"),
        LANGUAGE("language");

        override fun toString() = value
    }

    enum class Channel(override val value: String): JsonEnum {
        STABLE("stable"),
        BETA("beta"),
        ALPHA("alpha"),
        NIGHTLY("nightly");

        override fun toString() = value
    }
}

data class RepositoryAgent(
    val name: String,
    val version: String,
    val url: @Format("uri") URI?
) : Serializable {
}

data class Packages(
    @SerializedName("@type")
    val _type: _Type?,
    val packages: Map<String, Package>
) : Serializable {
    enum class _Type(override val value: String): JsonEnum {
        PACKAGES("Packages");

        override fun toString() = value
    }
}

sealed class Installer {
    data class MacOS(val value: MacOsInstaller) : Installer()
    data class Prefix(val value: PrefixInstaller) : Installer()
    data class Windows(val value: WindowsInstaller) : Installer()
}

data class Package(
    @SerializedName("@type")
    val _type: _Type?,
    val id: String,
    val name: @Format("bcp47-tag") Map<String, String>,
    val description: @Format("bcp47-tag") Map<String, String>,
    val version: @Format("version") String,
    val category: String,
    val languages: List<String>,
    val platform: Map<String, String>,
    val dependencies: Map<String, String>,
    val virtualDependencies: Map<String, String>,
    val installer: Installer
) : Serializable {
    enum class _Type(override val value: String): JsonEnum {
        PACKAGE("Package");

        override fun toString() = value
    }
}

data class PrefixInstaller(
    @SerializedName("@type")
    val _type: _Type?,
    val url: @Format("uri") URI,
    val size: @Format("uint64") Long,
    val installedSize: @Format("uint64") Long
) : Serializable {
    enum class _Type(override val value: String): JsonEnum {
        PREFIX_INSTALLER("PrefixInstaller");

        override fun toString() = value
    }
}

data class WindowsInstaller(
    @SerializedName("@type")
    val _type: _Type?,
    val url: @Format("uri") URI,
    val type: Type?,
    val args: String?,
    val uninstallArgs: String?,
    val productCode: String,
    val requiresReboot: Boolean,
    val requiresUninstallReboot: Boolean,
    val size: @Format("uint64") Long,
    val installedSize: @Format("uint64") Long
) : Serializable {
    enum class _Type(override val value: String): JsonEnum {
        WINDOWS_INSTALLER("WindowsInstaller");

        override fun toString() = value
    }

    enum class Type(override val value: String): JsonEnum {
        MSI("msi"),
        INNO("inno"),
        NSIS("nsis");

        override fun toString() = value
    }
}

data class MacOsInstaller(
    @SerializedName("@type")
    val _type: _Type?,
    val url: @Format("uri") URI,
    val pkgId: String,
    val targets: List<Targets>,
    val requiresReboot: Boolean,
    val requiresUninstallReboot: Boolean,
    val size: @Format("uint64") Long,
    val installedSize: @Format("uint64") Long
) : Serializable {
    enum class _Type(override val value: String): JsonEnum {
        MAC_OS_INSTALLER("MacOSInstaller");

        override fun toString() = value
    }
}

data class Virtuals(
    @SerializedName("@type")
    val _type: _Type?,
    val virtuals: Map<String, Virtual>
) : Serializable {
    enum class _Type(override val value: String): JsonEnum {
        VIRTUALS("Virtuals");

        override fun toString() = value
    }
}

data class Virtual(
    @SerializedName("@type")
    val _type: _Type?,
    val virtual: Boolean,
    val id: String,
    val name: @Format("bcp47-tag") Map<String, String>,
    val description: @Format("bcp47-tag") Map<String, String>,
    val version: @Format("version") String,
    val url: @Format("uri") URI,
    val target: VirtualTarget
) : Serializable {
    enum class _Type(override val value: String): JsonEnum {
        VIRTUAL("Virtual");

        override fun toString() = value
    }
}

class VirtualTarget(
) : Serializable {
}

enum class Targets(override val value: String): JsonEnum {
    SYSTEM("system"),
    USER("user");

    override fun toString() = value
}

