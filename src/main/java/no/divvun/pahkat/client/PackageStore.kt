package no.divvun.pahkat.client

import no.divvun.pahkat.client.delegate.PackageDownloadDelegate
import no.divvun.pahkat.client.ffi.Result
import javax.annotation.CheckReturnValue

interface PackageStore<Target> {
    @CheckReturnValue
    fun config(): Result<StoreConfig>
    @CheckReturnValue
    fun repoIndexes(withStatuses: Boolean): Result<List<RepositoryIndex>>
    @CheckReturnValue
    fun allStatuses(repo: RepoRecord, target: Target): Result<Map<String, PackageStatusResponse>>
    @CheckReturnValue
    fun download(packageKey: PackageKey, delegate: PackageDownloadDelegate): Result<String?>
    @CheckReturnValue
    fun import(packageKey: PackageKey, installerPath: String): Result<String>
//    func install()
//    func uninstall()
//    func status()
//    fun findPackage(byId: String): Result<Pair<PackageKey, Package>?>
    @CheckReturnValue
    fun findPackage(byKey: PackageKey): Result<Package?>
    @CheckReturnValue
    fun refreshRepos(): Result<Unit>
    @CheckReturnValue
    fun clearCache(): Result<Unit>
    @CheckReturnValue
    fun forceRefreshRepos(): Result<Unit>
//    func addRepo()
//    func removeRepo()
//    func updateRepo()

    fun transaction(actions: List<TransactionAction<Target>>): Result<PackageTransaction<Target>>
}