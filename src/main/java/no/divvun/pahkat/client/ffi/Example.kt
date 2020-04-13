package no.divvun.pahkat.client.ffi

import arrow.core.Either
import arrow.core.orNull
import no.divvun.pahkat.client.*
import no.divvun.pahkat.client.delegate.PackageDownloadDelegate
import no.divvun.pahkat.client.delegate.PackageTransactionDelegate
import java.io.File
import java.net.URI

fun main() {
    Example.run()
}

object Example {
    fun run() {
        PahkatClient.enableLogging(PahkatClient.LogLevel.TRACE)

//        File("./pahkat-client-java-test").deleteRecursively()

        val packageStore = PrefixPackageStore.openOrCreate("./pahkat-client-java-test").orThrow()
        val config = packageStore.config().orThrow()
        println(config.repos())
        println(packageStore)

        config.setRepos(
            mapOf("https://x.brendan.so/divvun-pahkat-repo/" to RepoRecord(channel = "nightly"))
        )

        packageStore.refreshRepos().orThrow()

        val key = PackageKey("https://x.brendan.so/divvun-pahkat-repo",
            "speller-sms", PackageKeyParams(platform = "mobile"))
        println(key.toString(withQueryParams = true))

        val status = packageStore.status(key).orThrow()
        println(status)

        when (status) {
            PackageInstallStatus.UpToDate -> return
            else -> {}
        }

        packageStore.download(key, object : PackageDownloadDelegate {
            override val isDownloadCancelled: Boolean
                get() = false

            override fun onDownloadProgress(packageKey: PackageKey, current: Long, maximum: Long) {
                println("progress: $packageKey $current/$maximum")
            }

            override fun onDownloadComplete(packageKey: PackageKey, path: String) {
                println("complete: $packageKey $path")

                when (val r = packageStore.transaction(listOf(TransactionAction.install(key, Unit)))) {
                    is Either.Left -> println("An error occurred creating tx: ${r.a}")
                    is Either.Right -> runTx(r.b)
                }
            }

            override fun onDownloadCancel(packageKey: PackageKey) {
                println("cancel: $packageKey")
            }

            override fun onDownloadError(packageKey: PackageKey, error: java.lang.Exception) {
                println("error: $packageKey $error")
            }
        })
    }

    fun runTx(tx: PackageTransaction<Unit>) {
        val delegate = object : PackageTransactionDelegate {
            override fun isTransactionCancelled(id: Long): Boolean {
                return false
            }

            override fun onTransactionInstall(id: Long, packageKey: PackageKey?) {
                println("install $packageKey")
            }

            override fun onTransactionUninstall(id: Long, packageKey: PackageKey?) {
                println("uninstall $packageKey")
            }

            override fun onTransactionCompleted(id: Long) {
                println("completed")
            }

            override fun onTransactionCancelled(id: Long) {
                println("cancelled")
            }

            override fun onTransactionError(id: Long, packageKey: PackageKey?, error: java.lang.Exception?) {
                println("error: $error")
            }

            override fun onTransactionUnknownEvent(id: Long, event: Long) {
                println("unknown event: $event")
            }
        }

        println("Running transaction for ${tx}")
        tx.process(delegate)
    }
}