// swift-tools-version:5.9

// Root manifest for integrators: File → Add Package Dependencies → https://github.com/Radarpayments/mobile-sdk
// Written by scripts/package-release.sh for each release: the frameworks are attached to the GitHub release.

import PackageDescription

let package = Package(
    name: "RadarpaymentsSDK",
    platforms: [.iOS(.v14)],
    products: [
        // The checkout, with SDK Core inside.
        .library(name: "RadarpaymentsSDKPayment", targets: ["RadarpaymentsSDKPayment"]),
        // The camera of the checkout's card scanner; add it next to RadarpaymentsSDKPayment.
        .library(name: "RadarpaymentsSDKScanner", targets: ["RadarpaymentsSDKScanner"]),
        // SDK Core alone, for an app with its own payment form. Not together with RadarpaymentsSDKPayment.
        .library(name: "RadarpaymentsSDKCore", targets: ["RadarpaymentsSDKCore"]),
    ],
    targets: [
        .binaryTarget(
            name: "RadarpaymentsSDKPayment",
            url: "https://github.com/Radarpayments/mobile-sdk/releases/download/4.0.0-beta.4/RadarpaymentsSDKPayment-4.0.0-beta.4.xcframework.zip",
            checksum: "72f7914c3bbdf1bad9b52033137d618b4bea25f4f0cfbb98a9f77aa188eefa8c"
        ),
        .binaryTarget(
            name: "RadarpaymentsSDKCore",
            url: "https://github.com/Radarpayments/mobile-sdk/releases/download/4.0.0-beta.4/RadarpaymentsSDKCore-4.0.0-beta.4.xcframework.zip",
            checksum: "12272c42bff02bfcbe3b4cbbcaf0edd82f346786ae081006c08a2026c6c7251a"
        ),
        .target(
            name: "RadarpaymentsSDKScanner",
            dependencies: ["RadarpaymentsSDKPayment"],
            path: "sdk-payment-scanner/ios/Sources/RadarpaymentsSDKScanner"
        ),
    ]
)
