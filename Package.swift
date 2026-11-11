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
            url: "https://github.com/Radarpayments/mobile-sdk/releases/download/4.0.0-beta.3/RadarpaymentsSDKPayment.xcframework.zip",
            checksum: "01c345f921ad455aa2afdf825eed003e48db67f03abb81c08dafc6e859230c47"
        ),
        .binaryTarget(
            name: "RadarpaymentsSDKCore",
            url: "https://github.com/Radarpayments/mobile-sdk/releases/download/4.0.0-beta.3/RadarpaymentsSDKCore.xcframework.zip",
            checksum: "c759090dc8af9bc569a10217fa3803345a3717e05757cc5dceb05b6e65aef6c1"
        ),
        .target(
            name: "RadarpaymentsSDKScanner",
            dependencies: ["RadarpaymentsSDKPayment"],
            path: "sdk-payment-scanner/ios/Sources/RadarpaymentsSDKScanner"
        ),
    ]
)
