// swift-tools-version:5.9

// Root manifest for integrators: File → Add Package Dependencies → https://github.com/Radarpayments/mobile-sdk
// Written by scripts/package-release.sh for each release: the SDK is the XCFramework attached to the GitHub release.

import PackageDescription

let package = Package(
    name: "PayrdrSDK",
    platforms: [.iOS(.v14)],
    products: [
        .library(name: "PayrdrSDK", targets: ["sdk_payment_scanner"])
    ],
    targets: [
        .binaryTarget(
            name: "sdk_payment_scanner",
            url: "https://github.com/Radarpayments/mobile-sdk/releases/download/4.0.0-beta.2/sdk_payment_scanner.xcframework.zip",
            checksum: "9a3d3547fde117eef52e8a64c3eb366b6f8d9205b10acb97ad4702bd0921dfac"
        )
    ]
)
