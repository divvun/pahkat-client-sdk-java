package no.divvun.pahkat.client.ffi

import arrow.core.extensions.either.applicativeError.handleError
import arrow.core.extensions.either.applicativeError.handleErrorWith
import arrow.core.extensions.either.applicativeError.raiseError
import arrow.core.extensions.either.foldable.get
import arrow.core.extensions.either.monad.map
import arrow.core.orNull
import no.divvun.pahkat.client.PrefixPackageStore
import no.divvun.pahkat.client.PrefixPackageStore.Companion.create

object Example {
    fun run() {
        val packageStore = PrefixPackageStore.open("/tmp/pahkat-client-java-test")
            .orNull()
            ?: throw Exception("Ffs")

        val config = packageStore.config().orNull() ?: throw Exception("no config")
        config.setUiSetting("hello", "world")
        println(config.getUiSetting("hello"))

        val repos = packageStore.repoIndexes(false).orNull()
        println(repos)
    }
}