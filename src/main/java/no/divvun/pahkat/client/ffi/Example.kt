package no.divvun.pahkat.client.ffi

import arrow.core.Either
import arrow.core.orNull
import no.divvun.pahkat.client.*
import no.divvun.pahkat.client.delegate.PackageDownloadDelegate
import java.net.URI

object Example {
    fun run() {
        PahkatClient.enableLogging(PahkatClient.LogLevel.TRACE)

        val packageStore = PrefixPackageStore.open("/tmp/pahkat-client-java-test")
            .orNull()
            ?: throw Exception("Ffs")

        val config = packageStore.config().orNull() ?: throw Exception("no config")
        config.setUiSetting("hello", "world")
        println(config.getUiSetting("hello"))

        config.setRepos(listOf(RepoRecord(URI.create("https://x.brendan.so/mobile-repo/"), Repository.Channel.NIGHTLY)))
        packageStore.forceRefreshRepos()

        val repos = when (val r = packageStore.repoIndexes(true)) {
            is Either.Left -> throw r.a
            is Either.Right -> r.b
        }

        println("HELLO")
        println(repos.toList().first().statuses.toString())

        config.repos()
        val key = PackageKey.from(repos.first(), "speller-se")
        val key2 = PackageKey.from(repos.first(), "speller-se")

        assert(key == key2)

        packageStore.download(key, object : PackageDownloadDelegate {
            override val isDownloadCancelled: Boolean
                get() = false

            override fun onDownloadProgress(packageKey: PackageKey, current: Long, maximum: Long) {
                println("progress: $packageKey $current/$maximum")
            }

            override fun onDownloadComplete(packageKey: PackageKey, path: String) {
                println("complete: $packageKey $path")
            }

            override fun onDownloadCancel(packageKey: PackageKey) {
                println("cancel: $packageKey")
            }

            override fun onDownloadError(packageKey: PackageKey, error: java.lang.Exception) {
                println("error: $packageKey $error")
            }

        })
    }
}