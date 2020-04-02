package no.divvun.example.pahkat.client

import android.content.Context
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import arrow.core.Either
import arrow.core.orNull
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables
import kotlinx.coroutines.*
import no.divvun.pahkat.client.*
import no.divvun.pahkat.client.delegate.PackageDownloadDelegate
import no.divvun.pahkat.client.delegate.PackageTransactionDelegate
import java.io.*
import java.net.URI
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        runPackageStoreExample(this)
    }
}

const val TAG_DOWNLOAD = "no.divvun.pahkat.client.DOWNLOAD"
const val TAG_TRANSACTION = "no.divvun.pahkat.client.TRANSACTION"

const val KEY_PACKAGE_KEY = "no.divvun.pahkat.client.packageKey"
const val KEY_TRANSACTION_ACTIONS = "no.divvun.pahkat.client.transactionActions"
const val KEY_PACKAGE_STORE_PATH = "no.divvun.pahkat.client.packageStorePath"
const val KEY_OBJECT = "no.divvun.pahkat.client.object"
const val KEY_OBJECT_TYPE = "no.divvun.pahkat.client.objectType"

fun PackageKey.workName(): String {
    return "download-${this}"
}

fun PackageKey.workData(path: String): Data {
    return Data.Builder()
        .putString(KEY_PACKAGE_KEY, this.toString())
        .putString(KEY_PACKAGE_STORE_PATH, path)
        .build()
}

fun <T> PackageTransaction<T>.workName(): String {
    return "transaction-${this.id}"
}
fun <T> PackageTransaction<T>.workData(path: String, actions: List<TransactionAction<T>>): Data {
    return Data.Builder()
        .putByteArray(KEY_TRANSACTION_ACTIONS, actions.map { it.toString() }.toData().toByteArray())
        .putString(KEY_PACKAGE_STORE_PATH, path)
        .build()
}

inline fun <reified T> T.toData(): Data {
    val baos = ByteArrayOutputStream()
    val oos = ObjectOutputStream(baos)

    oos.writeObject(this)
    oos.flush()

    return Data.Builder()
        .putString(KEY_OBJECT_TYPE, T::class.java.canonicalName)
        .putByteArray(KEY_OBJECT, baos.toByteArray())
        .build()
}

inline fun <reified T> Data.into(): T? {
    val bytes = this.getByteArray(KEY_OBJECT) ?: return null
    val bais = ByteArrayInputStream(bytes)
    val ois = ObjectInputStream(bais)
    return ois.readObject() as? T
}

fun runPackageStoreExample(context: Context) {
    PahkatClient.enableLogging()
    PahkatClient.Android.init(context.applicationInfo.dataDir)

    Log.wtf("ENV", System.getenv().map { "${it.key}: ${it.value}" }.joinToString(", "))
    val prefixPath = "${context.applicationInfo.dataDir}/no_backup/pahkat"

    val result = when (val result = PrefixPackageStore.open(prefixPath)) {
        is Either.Left -> PrefixPackageStore.create(prefixPath)
        is Either.Right -> result
    }

    val prefix = when (result) {
        is Either.Left -> {
            Log.wtf("PREFIX", result.a)
            return
        }
        is Either.Right -> result.b
    }

    val config = prefix.config().orNull() ?: return
//    config.setRepos(listOf(RepoRecord())
    config.setRepos(listOf(RepoRecord(URI("https://x.brendan.so/mobile-repo/"), Repository.Channel.NIGHTLY)))
    prefix.forceRefreshRepos()

    val repos = prefix.repoIndexes(true).orNull() ?: return
    Log.d("PREFIX", repos.toMutableList().toString())

    val key = PackageKey.from("${repos[0].meta.base}packages/speller-se#nightly")

//    val packageKey = PackageKey.from(repos.first(), "speller-se")
    prefix.downloadInBackground(context, prefixPath, key)

    val subscription = prefix.observeDownloadProgress(context, key)
        .doOnNext { Log.d("Process", it.toString()) }
        .takeUntil { it.isFinished }
        .filter { it.isCompleted }
        .switchMap {
            val tx = when (val r = prefix.transaction(listOf(TransactionAction.install(key, Unit)))) {
                is Either.Left -> return@switchMap Flowable.error<TransactionProgress>(r.a)
                is Either.Right -> r.b
            }
            tx.processInBackground(context, prefixPath)
        }
        .subscribe({
            Log.d("Process", it.toString())
        }, { Log.wtf("Process", it) })
}

sealed class TransactionProgress: Serializable {
    object Completed: TransactionProgress() { override fun toString() = "Completed" }
    object Cancelled: TransactionProgress() { override fun toString() = "Cancelled" }
    data class Install(val packageKey: PackageKey): TransactionProgress()
    data class Uninstall(val packageKey: PackageKey): TransactionProgress()
    data class UnknownEvent(val packageKey: PackageKey, val event: Long): TransactionProgress()
    data class Error(val packageKey: PackageKey?, val error: java.lang.Exception?): TransactionProgress()

    val isFinished get() = false
}

fun <T> PackageTransaction<T>.processInBackground(context: Context, packageStorePath: String): Flowable<TransactionProgress> {
    val workManager = WorkManager.getInstance(context)

    val req = OneTimeWorkRequest.Builder(PrefixTransactionWorker::class.java)
        .addTag(TAG_TRANSACTION)
        .setInputData(this.workData(packageStorePath, this.actions))
        .keepResultsForAtLeast(1, TimeUnit.DAYS)
        .setConstraints(Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresStorageNotLow(true)
            .setRequiresBatteryNotLow(true)
            .build())
        .build()

    val workName = this.workName()

    workManager
        .beginUniqueWork(workName, ExistingWorkPolicy.KEEP, req)
        .enqueue()

    return observeProgress(context)
}

fun <T> PackageTransaction<T>.observeProgress(context: Context): Flowable<TransactionProgress> {
    val workManager = WorkManager.getInstance(context)
    val liveData = workManager.getWorkInfosForUniqueWorkLiveData(this.workName())

    return Flowables.create(BackpressureStrategy.LATEST) { emitter ->
        val observer: (List<WorkInfo>) -> Unit = {
            val workInfo = it.last()
            val progress = workInfo.progress.into<TransactionProgress>()

            if (progress != null) {
                emitter.onNext(progress)
                if (progress.isFinished) {
                    emitter.onComplete()
                }
            }
        }

        liveData.observeForever(observer)

        emitter.setCancellable {
            liveData.removeObserver(observer)
        }
    }
}

fun PrefixPackageStore.downloadInBackground(context: Context, packageStorePath: String, packageKey: PackageKey): Flowable<DownloadProgress> {
    val workManager = WorkManager.getInstance(context)

    val req = OneTimeWorkRequest.Builder(DownloadWorker::class.java)
        .addTag(TAG_DOWNLOAD)
        .setInputData(packageKey.workData(packageStorePath))
        .keepResultsForAtLeast(1, TimeUnit.DAYS)
        .setConstraints(Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresStorageNotLow(true)
            .build())
        .build()

    val workName = packageKey.workName()

    workManager
        .beginUniqueWork(workName, ExistingWorkPolicy.KEEP, req)
        .enqueue()

    return observeDownloadProgress(context, packageKey)
}

fun PrefixPackageStore.downloadProgressSync(context: Context, packageKey: PackageKey): DownloadProgress? {
    val workManager = WorkManager.getInstance(context)

    val workInfos = workManager
        .getWorkInfosForUniqueWork(packageKey.workName())
        .get(1, TimeUnit.SECONDS)

    return workInfos.last().progress.into()
}

fun PrefixPackageStore.observeDownloadProgress(context: Context, packageKey: PackageKey): Flowable<DownloadProgress> {
    val workManager = WorkManager.getInstance(context)
    val liveData = workManager.getWorkInfosForUniqueWorkLiveData(packageKey.workName())

    return Flowables.create(BackpressureStrategy.LATEST) { emitter ->
        val observer: (List<WorkInfo>) -> Unit = {
            val workInfo = it.last()
            val progress = workInfo.progress.into<DownloadProgress>()

            if (progress != null) {
                emitter.onNext(progress)
                if (progress.isFinished) {
                    emitter.onComplete()
                }
            }
        }

        liveData.observeForever(observer)

        emitter.setCancellable {
            liveData.removeObserver(observer)
        }
    }
}

sealed class DownloadProgress: Serializable {
    data class Starting(val packageKey: String): DownloadProgress()
    data class Downloading(
        val packageKey: String,
        val current: Long,
        val total: Long
    ): DownloadProgress()
    data class Cancelled(val packageKey: String): DownloadProgress()
    data class Completed(val packageKey: String, val path: String): DownloadProgress()
    data class Error(val packageKey: String, val value: Throwable): DownloadProgress()

    val isFinished: Boolean get() = when (this) {
        is Cancelled, is Completed, is Error -> true
        else -> false
    }

    val isCompleted: Boolean get() = this is Completed
    val destPath: String? get() = if (this is Completed) { this.path } else { null }
}

class PrefixTransactionWorker(context: Context, params: WorkerParameters): Worker(context, params) {
    private val transactionActionsValue get() =
        inputData.getByteArray(KEY_TRANSACTION_ACTIONS)?.let {
            Data.fromByteArray(it).into<List<String>>()?.map { TransactionAction.fromJson<Unit>(it) }
        }
    private val packageStorePathValue get() = inputData.getString(KEY_PACKAGE_STORE_PATH)

    private inner class Delegate: PackageTransactionDelegate {
        val coroutineScope = CoroutineScope(Dispatchers.IO)

        internal var isCancelled = false

        override fun isTransactionCancelled(id: Long): Boolean = isCancelled

        override fun onTransactionCancelled(id: Long) {
            coroutineScope.launch {
                setProgressAsync(TransactionProgress.Cancelled.toData()).await()
            }
        }

        override fun onTransactionCompleted(id: Long) {
            coroutineScope.launch {
                setProgressAsync(TransactionProgress.Completed.toData()).await()
            }
        }

        override fun onTransactionError(
            id: Long,
            packageKey: PackageKey?,
            error: java.lang.Exception?
        ) {
            coroutineScope.launch {
                setProgressAsync(TransactionProgress.Error(packageKey, error).toData()).await()
            }
        }

        override fun onTransactionInstall(id: Long, packageKey: PackageKey) {
            coroutineScope.launch {
                setProgressAsync(TransactionProgress.Install(packageKey).toData()).await()
            }
        }

        override fun onTransactionUninstall(id: Long, packageKey: PackageKey) {
            coroutineScope.launch {
                setProgressAsync(TransactionProgress.Uninstall(packageKey).toData()).await()
            }
        }

        override fun onTransactionUnknownEvent(id: Long, packageKey: PackageKey, event: Long) {
            coroutineScope.launch {
                setProgressAsync(TransactionProgress.UnknownEvent(packageKey, event).toData()).await()
            }
        }

    }

    private val delegate = Delegate()

    override fun onStopped() {
        delegate.isCancelled = true
        super.onStopped()
    }

    override fun doWork(): Result {
        val actions = transactionActionsValue ?: return Result.failure(
            IllegalArgumentException("actions cannot be null").toData())
        val packageStorePath = packageStorePathValue ?: return Result.failure(
            IllegalArgumentException("packageStorePath cannot be null").toData())

        val packageStore = when (val result = PrefixPackageStore.open(packageStorePath)) {
            is Either.Left -> return Result.failure(result.a.toData())
            is Either.Right -> result.b
        }

        val tx = when (val result = packageStore.transaction(actions)) {
            is Either.Left -> return Result.failure(result.a.toData())
            is Either.Right -> result.b
        }

//        setProgressAsync(DownloadProgress.Starting(packageKey.toString()).toData())

        // This blocks to completion.
        tx.process(delegate)

        if (delegate.coroutineScope.isActive) {
            Log.d("TRANSACTION", "Looping 250ms delay")
            Thread.sleep(250)
        }

        return Result.success()
    }

}

class DownloadWorker(context: Context, params: WorkerParameters): Worker(context, params) {
    private val packageKeyValue get() = inputData.getString(KEY_PACKAGE_KEY)?.let { PackageKey.from(it) }
    private val packageStorePathValue get() = inputData.getString(KEY_PACKAGE_STORE_PATH)

    private inner class Delegate: PackageDownloadDelegate {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        override var isDownloadCancelled = false

        override fun onDownloadCancel(packageKey: PackageKey) {
            Log.d("DOWNLOAD", "cancel: $packageKey")
            coroutineScope.launch {
                setProgressAsync(DownloadProgress.Cancelled(packageKey.toString()).toData()).await()
            }
        }

        override fun onDownloadComplete(packageKey: PackageKey, path: String) {
            Log.d("DOWNLOAD", "complete: $packageKey $path")
            coroutineScope.launch {
                setProgressAsync(DownloadProgress.Completed(packageKey.toString(), path).toData()).await()
            }
        }

        override fun onDownloadError(packageKey: PackageKey, error: Exception) {
            Log.e("DOWNLOAD", "error: $packageKey", error)

            coroutineScope.launch {
                Log.d("DOWNLOAD", "OH LAWD HE COMIN'")
                val data = DownloadProgress.Error(packageKey.toString(), error).toData()
                Log.d("DOWNLOAD", "$data")
                setProgressAsync(data).await()
                Log.d("DOWNLOAD", "Finished waiting.")
            }
        }

        override fun onDownloadProgress(packageKey: PackageKey, current: Long, maximum: Long) {
            Log.d("DOWNLOAD", "progress: $packageKey $current/$maximum")

            coroutineScope.launch {
                setProgressAsync(
                    DownloadProgress.Downloading(
                        packageKey.toString(),
                        current,
                        maximum
                    ).toData()
                ).await()
            }
        }
    }

    private val delegate = Delegate()

    override fun onStopped() {
        delegate.isDownloadCancelled = true
        super.onStopped()
    }

    override fun doWork(): Result {
        val packageKey = packageKeyValue ?: return Result.failure(
            IllegalArgumentException("packageKey cannot be null").toData())
        val packageStorePath = packageStorePathValue ?: return Result.failure(
            IllegalArgumentException("packageStorePath cannot be null").toData())

        val packageStore = when (val result = PrefixPackageStore.open(packageStorePath)) {
            is Either.Left -> return Result.failure(result.a.toData())
            is Either.Right -> result.b
        }

        setProgressAsync(DownloadProgress.Starting(packageKey.toString()).toData())

        // This blocks to completion.
        val result = when (val r = packageStore.download(packageKey, delegate)) {
            is Either.Left -> Result.failure(r.a.toData())
            is Either.Right -> Result.success(r.b.toData())
        }

        if (delegate.coroutineScope.isActive) {
            Log.d("DOWNLOAD", "Looping 250ms delay")
            Thread.sleep(250)
        }

        return result
    }
}

