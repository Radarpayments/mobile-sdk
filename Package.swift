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
            url: "https://github.com/Radarpayments/mobile-sdk/releases/download/4.0.0-beta.1/sdk_payment_scanner.xcframework.zip",
            checksum: "42d0f885bae011b807e3a2685697b91ef3c7759fe7f0ae30097c0a3d6719090a"
        )
    ]
)
