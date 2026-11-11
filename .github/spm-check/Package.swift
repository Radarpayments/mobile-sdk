// swift-tools-version:5.9

// The integrator's side of the root package, used by .github/workflows/release.yml.
// PAYRDR_SDK_URL + PAYRDR_SDK_VERSION (a released tag) resolve the SDK from git like an integrator would; without them
// the XCFrameworks are the ones a release has just built, copied into Frameworks/, and the scanner's sources are
// copied into Sources/.

import Foundation
import PackageDescription

let dependencies: [Package.Dependency]
let payment: Target.Dependency
let scanner: Target.Dependency
let core: Target.Dependency
var targets: [Target] = []
if let url = Context.environment["PAYRDR_SDK_URL"], let version = Context.environment["PAYRDR_SDK_VERSION"] {
    dependencies = [.package(url: url, exact: Version(version)!)]
    // SwiftPM names a remote package after the last URL component, e.g. "mobile-sdk".
    let package = URL(string: url)!.deletingPathExtension().lastPathComponent
    payment = .product(name: "RadarpaymentsSDKPayment", package: package)
    scanner = .product(name: "RadarpaymentsSDKScanner", package: package)
    core = .product(name: "RadarpaymentsSDKCore", package: package)
} else {
    dependencies = []
    targets = [
        .binaryTarget(name: "RadarpaymentsSDKPayment", path: "Frameworks/RadarpaymentsSDKPayment.xcframework"),
        .binaryTarget(name: "RadarpaymentsSDKCore", path: "Frameworks/RadarpaymentsSDKCore.xcframework"),
        .target(name: "RadarpaymentsSDKScanner", dependencies: ["RadarpaymentsSDKPayment"]),
    ]
    payment = "RadarpaymentsSDKPayment"
    scanner = "RadarpaymentsSDKScanner"
    core = "RadarpaymentsSDKCore"
}

let package = Package(
    name: "SPMCheck",
    platforms: [.iOS(.v14)],
    dependencies: dependencies,
    targets: targets + [
        .testTarget(name: "SPMCheckTests", dependencies: [payment, scanner]),
        // An app takes either the payment framework or SDK Core alone, hence a test bundle of its own.
        .testTarget(name: "SPMCoreCheckTests", dependencies: [core]),
    ]
)
