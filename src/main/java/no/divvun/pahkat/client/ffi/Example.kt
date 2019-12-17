package no.divvun.pahkat.client.ffi

import arrow.core.orNull
import no.divvun.pahkat.client.PrefixPackageStore
import no.divvun.pahkat.client.RepoRecord
import no.divvun.pahkat.client.Repository
import java.net.URI

object Example {
    fun run() {
        val packageStore = PrefixPackageStore.open("/tmp/pahkat-client-java-test")
            .orNull()
            ?: throw Exception("Ffs")

        val config = packageStore.config().orNull() ?: throw Exception("no config")
        config.setUiSetting("hello", "world")
        println(config.getUiSetting("hello"))

        config.setRepos(listOf(RepoRecord(URI.create("https://x.brendan.so/mobile-repo"), Repository.Channel.STABLE)))

        val repos = packageStore.repoIndexes(false).orNull()
        println(repos)
    }
}