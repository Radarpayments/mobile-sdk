// swift-tools-version:5.9

// The integrator's side of the root package, used by .github/workflows/release.yml.
// PAYRDR_SDK_URL + PAYRDR_SDK_VERSION (a released tag) resolve the SDK from git like an integrator would; without them
// the XCFramework is the one a release has just built, copied into Frameworks/.

import Foundation
import PackageDescription

let dependencies: [Package.Dependency]
let sdk: Target.Dependency
var targets: [Target] = []
if let url = Context.environment["PAYRDR_SDK_URL"], let version = Context.environment["PAYRDR_SDK_VERSION"] {
    dependencies = [.package(url: url, exact: Version(version)!)]
    // SwiftPM names a remote package after the last URL component, e.g. "mobile-sdk".
    sdk = .product(name: "PayrdrSDK", package: URL(string: url)!.deletingPathExtension().lastPathComponent)
} else {
    dependencies = []
    targets.append(.binaryTarget(name: "sdk_payment_scanner", path: "Frameworks/sdk_payment_scanner.xcframework"))
    sdk = "sdk_payment_scanner"
}

let package = Package(
    name: "SPMCheck",
    platforms: [.iOS(.v14)],
    dependencies: dependencies,
    targets: targets + [
        .testTarget(name: "SPMCheckTests", dependencies: [sdk])
    ]
)
