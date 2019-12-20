@file:Suppress("MoveLambdaOutsideParentheses")
package no.divvun.pahkat.client

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.bind.JsonTreeReader
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.io.Serializable
import java.lang.reflect.Method
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

@Target(AnnotationTarget.TYPE)
@MustBeDocumented
annotation class Format(val value: String)

private fun createGson(): Gson {
    fun createDateFormatter(pattern: String, tz: String): SimpleDateFormat {
        val df = SimpleDateFormat(pattern, Locale.ROOT)
        df.timeZone = TimeZone.getTimeZone(tz)
        return df
    }

    class EnumTypeAdapter<T>(private val type: TypeToken<T>, private val valueGetter: Method) : TypeAdapter<T>() {
        override fun write(writer: JsonWriter, value: T) {
            when (val v = valueGetter.invoke(value)) {
                is Boolean -> writer.value(v)
                is String -> writer.value(v)
                is Number -> writer.value(v)
                else -> writer.nullValue()
            }
        }

        override fun read(reader: JsonReader): T {
            val s = reader.nextString()

            @Suppress("UNCHECKED_CAST")
            val constants = type.rawType.enumConstants as Array<T>

            return constants.firstOrNull { (valueGetter.invoke(it) as? String) == s }
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
            if (!(type.rawType.isEnum && (type.rawType.declaredConstructors.firstOrNull()?.parameterCount ?: 0) == 3)) {
                return null
            }

            val method = type.rawType.methods
                .firstOrNull { it.declaringClass == type.type && !listOf("toString", "values", "valueOf").contains(it.name) }
                ?: return null

            return EnumTypeAdapter(type, method)
        }
    }

    return GsonBuilder()
        .serializeNulls()
        .registerTypeAdapter(Unit::class.java, object : TypeAdapter<Unit>() {
            override fun write(writer: JsonWriter?, value: Unit?) { writer?.nullValue() }
            override fun read(`in`: JsonReader?) = Unit
        })
        .registerTypeAdapterFactory(EnumTypeAdapterFactory())
        .registerTypeAdapterFactory(DateAdapterFactory())
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        .create()
}

internal val gson = createGson()

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
    enum class _Type(val value: String) {
        REPOSITORY("Repository");

        override fun toString() = value
    }

    enum class PrimaryFilter(val value: String) {
        CATEGORY("category"),
        LANGUAGE("language");

        override fun toString() = value
    }

    enum class Channel(val value: String) {
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
    enum class _Type(val value: String) {
        PACKAGES("Packages");

        override fun toString() = value
    }
}

class InstallerAdapter: TypeAdapter<Installer>() {
    override fun write(writer: JsonWriter, value: Installer) {
        when (value) {
            is Installer.MacOS -> writer.jsonValue(gson.toJson(value.value))
            is Installer.Windows -> writer.jsonValue(gson.toJson(value.value))
            is Installer.Tarball -> writer.jsonValue(gson.toJson(value.value))
        }
    }

    override fun read(reader: JsonReader): Installer {
        val tree = JsonParser.parseReader(reader)
        val ty = tree.asJsonObject.get("@type").asString

        return when (ty) {
            "MacOSInstaller" -> Installer.MacOS(gson.fromJson(tree, MacOsInstaller::class.java))
            "WindowsInstaller" -> Installer.Windows(gson.fromJson(tree, WindowsInstaller::class.java))
            "TarballInstaller" -> Installer.Tarball(gson.fromJson(tree, TarballInstaller::class.java))
            else -> throw IOException("Invalid type for InstallerAdapter: $ty")
        }
    }
}

@JsonAdapter(InstallerAdapter::class)
sealed class Installer {
    data class MacOS(val value: MacOsInstaller) : Installer()
    data class Tarball(val value: TarballInstaller) : Installer()
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
    enum class _Type(val value: String) {
        PACKAGE("Package");

        override fun toString() = value
    }
}

data class TarballInstaller(
    @SerializedName("@type")
    val _type: _Type?,
    val url: @Format("uri") URI,
    val size: @Format("uint64") Long,
    val installedSize: @Format("uint64") Long
) : Serializable {
    enum class _Type(val value: String) {
        TARBALL_INSTALLER("TarballInstaller");

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
    enum class _Type(val value: String) {
        WINDOWS_INSTALLER("WindowsInstaller");

        override fun toString() = value
    }

    enum class Type(val value: String) {
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
    enum class _Type(val value: String) {
        MAC_OS_INSTALLER("MacOSInstaller");

        override fun toString() = value
    }
}

data class Virtuals(
    @SerializedName("@type")
    val _type: _Type?,
    val virtuals: Map<String, Virtual>
) : Serializable {
    enum class _Type(val value: String) {
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
    enum class _Type(val value: String) {
        VIRTUAL("Virtual");

        override fun toString() = value
    }
}

class VirtualTarget(
) : Serializable {
}

enum class Targets(val value: String) {
    SYSTEM("system"),
    USER("user");

    override fun toString() = value
}

