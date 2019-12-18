package no.divvun.pahkat.client

import org.apache.hc.core5.net.URIBuilder
import java.net.URI

class PackageKey private constructor(val url: String, val id: String, val channel: String) {
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