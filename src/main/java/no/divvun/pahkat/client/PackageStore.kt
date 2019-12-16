package no.divvun.pahkat.client

import no.divvun.pahkat.client.delegate.PackageDownloadDelegate
import no.divvun.pahkat.client.ffi.Result

interface PackageStore<Target> {
    fun config(): Result<StoreConfig>
    fun repoIndexes(withStatuses: Boolean): Result<List<RepositoryIndex>>
    fun allStatuses(repo: RepoRecord, target: Target): Result<Map<String, PackageStatusResponse>>
    fun download(packageKey: PackageKey, delegate: PackageDownloadDelegate): Result<String?>
    fun import(packageKey: PackageKey, installerPath: String): Result<String>
//    func install()
//    func uninstall()
//    func status()
//    fun findPackage(byId: String): Result<Pair<PackageKey, Package>?>
    fun findPackage(byKey: PackageKey): Result<Package?>
    fun refreshRepos(): Result<Unit>
    fun clearCache(): Result<Unit>
    fun forceRefreshRepos(): Result<Unit>
//    func addRepo()
//    func removeRepo()
//    func updateRepo()

    fun transaction(actions: List<TransactionAction<Target>>): Result<PackageTransaction<Target>>
}