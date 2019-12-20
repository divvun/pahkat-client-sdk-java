package no.divvun.pahkat.client

import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.apache.hc.core5.net.URIBuilder

class PackageKeyAdapter: TypeAdapter<PackageKey>() {
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

@JsonAdapter(PackageKeyAdapter::class)
data class PackageKey internal constructor(val url: String, val id: String, val channel: String) {
    companion object {
        fun from(urlString: String): PackageKey {
            val uri = URIBuilder(urlString)
            val channel = uri.fragment ?: "stable"
            val id = uri.pathSegments.last()

            val url = {
                uri.fragment = null

                // Pop last two segments
                val segments = uri.pathSegments
                segments.removeAt(segments.size - 1)
                segments.removeAt(segments.size - 1)

                uri.pathSegments = segments
                uri.toString()
            }()

            return PackageKey(url, id, channel)
        }

        fun from(index: RepositoryIndex, packageId: String): PackageKey {
            return from("${index.meta.base}packages/${packageId}#${index.channel.value}")
        }
    }

    override fun toString(): String {
        return "${url}/packages/${id}#${channel}"
    }
}