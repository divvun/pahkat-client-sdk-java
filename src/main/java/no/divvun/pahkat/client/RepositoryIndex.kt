package no.divvun.pahkat.client

import com.google.gson.annotations.SerializedName

data class RepositoryIndex(
    val meta: Repository,
    val channel: Repository.Channel,
    @SerializedName("packages")
    private val packagesMeta: Packages,
    @SerializedName("virtuals")
    private val virtualsMeta: Virtuals
) {
    var statuses: Map<PackageKey, PackageStatusResponse> = emptyMap()

    val packages: Map<String, Package> get() = packagesMeta.packages
    val virtuals: Map<String, Virtual> get() = virtualsMeta.virtuals

    fun status(packageKey: PackageKey): PackageStatusResponse? {
        return statuses[packageKey]
    }
}

//@objc public class RepositoryIndex: NSObject, Decodable, Comparable {
//    public let meta: Repository
//    public let channel: Repository.Channels
//    private let packagesMeta: Packages
//    private let virtualsMeta: Virtuals
//
//    public var statuses: [PackageKey: PackageStatusResponse] = [:]
//
//    init(repository: Repository, packages: Packages, virtuals: Virtuals, channel: Repository.Channels) {
//        self.meta = repository
//        self.packagesMeta = packages
//        self.virtualsMeta = virtuals
//        self.channel = channel
//    }
//
//    public var packages: [String: Package] {
//        return packagesMeta.packages
//    }
//
//    public var virtuals: [String: String] {
//        return virtualsMeta.virtuals
//    }
//
////    func url(for package: Package) -> URI {
////        return packagesMeta.base.appendingPathComponent(package.id)
////    }
//
//    public func status(for key: PackageKey) -> PackageStatusResponse? {
//        return statuses[key]
//    }
//
//    public func package(for key: PackageKey) -> Package? {
//        if key.url != meta.base.absoluteString || key.channel != channel.rawValue {
//            return nil
//        }
//
//        return packages[key.id]
//    }
//
//    @available(*, deprecated, message: "use status(for:)")
//    public func status(forPackage package: Package) -> PackageStatusResponse? {
//        if let key = statuses.keys.first(where: { $0.id == package.id }) {
//            return self.status(for: key)
//        }
//        return nil
//    }
//
////    func status(forPackage package: Package) -> PackageStatusResponse? {
////        return statuses[package.id]
////    }
//
//    public func absoluteKey(for package: Package) -> PackageKey {
//        var builder = URLComponents(url: meta.base
//                .appendingPathComponent("packages")
//            .appendingPathComponent(package.id), resolvingAgainstBaseURL: false)!
//        builder.fragment = channel.rawValue
//
//        return PackageKey(from: builder.url!)
//    }
//
//    func set(statuses: [PackageKey: PackageStatusResponse]) {
//        self.statuses = statuses
//    }
//
//    private enum CodingKeys: String, CodingKey {
//        case meta = "meta"
//        case channel = "channel"
//        case packagesMeta = "packages"
//        case virtualsMeta = "virtuals"
//    }
//
//    public static func ==(lhs: RepositoryIndex, rhs: RepositoryIndex) -> Bool {
//        return lhs.meta == rhs.meta &&
//                lhs.packagesMeta == rhs.packagesMeta &&
//                lhs.virtualsMeta == rhs.virtualsMeta
//    }
//
//    public static func <(lhs: RepositoryIndex, rhs: RepositoryIndex) -> Bool {
//        // BTree keys break if you don't break contention yourself...
//        if lhs.meta.nativeName == rhs.meta.nativeName {
//            return lhs.hashValue < rhs.hashValue
//        }
//        return lhs.meta.nativeName < rhs.meta.nativeName
//    }

//    override var hashValue: Int {
//        return meta.hashValue ^ packagesMeta.hashValue ^ virtualsMeta.hashValue
//    }
//}