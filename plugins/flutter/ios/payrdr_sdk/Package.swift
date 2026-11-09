// swift-tools-version: 5.9

import PackageDescription

// Swift Package Manager form of the plugin (Flutter 3.44+ uses it by default); payrdr_sdk.podspec is the CocoaPods form of
// the same sources and framework.
let package = Package(
    name: "payrdr_sdk",
    platforms: [
        .iOS("15.0")
    ],
    products: [
        .library(name: "payrdr-sdk", targets: ["payrdr_sdk"])
    ],
    dependencies: [
        .package(name: "FlutterFramework", path: "../FlutterFramework")
    ],
    targets: [
        .target(
            name: "payrdr_sdk",
            dependencies: [
                .product(name: "FlutterFramework", package: "FlutterFramework"),
                "PayrdrSdkBridge",
            ],
            resources: [
                .process("PrivacyInfo.xcprivacy")
            ]
        ),
        // The whole SDK as one Kotlin framework; scripts/build-bridge.sh puts it here.
        .binaryTarget(name: "PayrdrSdkBridge", path: "Frameworks/PayrdrSdkBridge.xcframework"),
    ]
)
