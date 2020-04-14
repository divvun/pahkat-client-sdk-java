package no.divvun.pahkat.client

import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.apache.hc.core5.http.NameValuePair
import org.apache.hc.core5.net.URIBuilder
import java.io.Serializable

class PackageKeyAdapter : TypeAdapter<PackageKey>() {
    override fun write(writer: JsonWriter, value: PackageKey?) {
        if (value == null) {
            writer.nullValue()
            return
        }

        writer.value(value.toString())
    }

    override fun read(reader: JsonReader): PackageKey {
        val value = reader.nextString()
        return PackageKey.from(value)
    }
}

data class PackageKeyParams(
    val channel: String? = null,
    val platform: String? = null,
    val version: String? = null,
    val arch: String? = null
)

@JsonAdapter(PackageKeyAdapter::class)
data class PackageKey constructor(
    val repositoryUrl: String,
    val id: String,
    val params: PackageKeyParams
) : Serializable {
    companion object {
        fun from(urlString: String): PackageKey {
            val uri = URIBuilder(urlString)
            val id = uri.pathSegments.last()

            val params = uri.queryParams.fold(PackageKeyParams(), { acc, cur ->
                when (cur.name) {
                    "channel" -> acc.copy(channel = cur.value)
                    "platform" -> acc.copy(platform = cur.value)
                    "version" -> acc.copy(version = cur.value)
                    "arch" -> acc.copy(arch = cur.value)
                    else -> acc
                }
            })

            val repoUrl = {
                uri.fragment = null
                uri.clearParameters()

                // Pop last two segments
                val segments = uri.pathSegments
                segments.removeAt(segments.size - 1)
                segments.removeAt(segments.size - 1)

                uri.pathSegments = segments
                uri.toString()
            }()

            return PackageKey(repoUrl, id, params)
        }
    }

    override fun toString() = toString(true)

    fun toString(withQueryParams: Boolean): String {
        val uri = URIBuilder(repositoryUrl)

        val segments = uri.pathSegments ?: mutableListOf()
        segments.add("packages")
        segments.add(id)
        uri.pathSegments = segments

        if (withQueryParams) {
            params.channel?.let { uri.setParameter("platform", it) }
            params.platform?.let { uri.setParameter("platform", it) }
            params.version?.let { uri.setParameter("version", it) }
            params.arch?.let { uri.setParameter("arch", it) }
        }

        return uri.build().toString()
    }
}
