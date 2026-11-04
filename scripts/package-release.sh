#!/usr/bin/env bash
# Packs a release that has been built (scripts/set-version.sh, scripts/build-bridge.sh release,
# :sdk-payment-scanner:assembleSdk_payment_scannerReleaseXCFramework and publishAllPublicationsToStagingRepository):
#   - build/central-bundle.zip, the Maven Central upload (build/staging-deploy without Maven's metadata files);
#   - into build/release, for the GitHub release:
#     - sdk_payment_scanner.xcframework.zip, the iOS SDK, which Package.swift (written here) points Swift Package
#       Manager at, and which an app can also embed by hand;
#     - the Android libraries as AARs, to download; an alpha, which does not go to Maven Central, also gets the Maven
#       repository itself (payrdr-sdk-maven-<sdk-version>.zip);
#     - the Flutter and React Native plugins with PayrdrSdkBridge.xcframework inside;
#     - notes.md, the release text: CHANGELOG's [Unreleased] part, which for a stable release becomes its dated section.
#
# Usage: scripts/package-release.sh <sdk-version> <plugin-version>
set -euo pipefail

sdk="${1:-}"
plugin="${2:-}"
if [[ -z "$sdk" || -z "$plugin" ]]; then
  echo "usage: $0 <sdk-version> <plugin-version>" >&2
  exit 2
fi

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"
repository="${GITHUB_REPOSITORY:-Radarpayments/mobile-sdk}"
dist="$root/build/release"
rm -rf "$dist" build/central-bundle.zip
mkdir -p "$dist"

ditto -c -k --keepParent sdk-payment-scanner/build/XCFrameworks/release/sdk_payment_scanner.xcframework \
  "$dist/sdk_payment_scanner.xcframework.zip"
checksum="$(swift package compute-checksum "$dist/sdk_payment_scanner.xcframework.zip")"
cat > Package.swift <<EOF
// swift-tools-version:5.9

// Root manifest for integrators: File → Add Package Dependencies → https://github.com/$repository
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
            url: "https://github.com/$repository/releases/download/$sdk/sdk_payment_scanner.xcframework.zip",
            checksum: "$checksum"
        )
    ]
)
EOF

(cd build/staging-deploy && find . -name 'maven-metadata*' -delete && zip -qr "$root/build/central-bundle.zip" .)
for module in sdk-core sdk-payment sdk-payment-scanner; do
  cp "$module/build/outputs/aar/$module-release.aar" "$dist/$module-$sdk.aar"
done
if [[ "$sdk" == *-alpha.* ]]; then
  cp build/central-bundle.zip "$dist/payrdr-sdk-maven-$sdk.zip"
fi

(cd plugins && git ls-files flutter && find flutter/ios/payrdr_sdk/Frameworks -type f) |
  tar -czf "$dist/payrdr_sdk-$plugin.tar.gz" -C plugins -s '/^flutter/payrdr_sdk/' -T -
(cd plugins/react-native && npm pack --silent --pack-destination "$dist" > /dev/null)

changes="$(awk '$0 == "## [Unreleased]" { found = 1; next } found && /^## / { exit } found' CHANGELOG.md | sed '/./,$!d')"
if [[ "$sdk" != *-* ]]; then
  V="$sdk" D="$(date -u +%F)" perl -0pi -e 's/^## \[Unreleased\]\n/## [Unreleased]\n\n## [$ENV{V}] - $ENV{D}\n/m' CHANGELOG.md
fi

if [[ "$sdk" == *-alpha.* ]]; then
  android="unpack \`payrdr-sdk-maven-$sdk.zip\` and add the folder as a Maven repository (alphas are not on Maven Central),
  then \`implementation(\"io.github.radarpayments:sdk-payment:$sdk\")\`"
  plugins_android="The plugins' Android part takes the SDK from that Maven repository: set \`payrdrMavenUrl\` in the app's
\`android/gradle.properties\` to it."
else
  android="\`implementation(\"io.github.radarpayments:sdk-payment:$sdk\")\` from Maven Central; the AARs below are the
  same libraries to download (their dependencies are listed in the POMs on Maven Central)"
  plugins_android="The plugins' Android part takes the SDK from Maven Central."
fi

{
  if [[ "$sdk" == *-* ]]; then
    echo "Pre-release from \`${RELEASE_BRANCH:-$(git rev-parse --abbrev-ref HEAD)}\`: not for production."
    echo
  fi
  echo "${changes:-No changes for integrators.}"
  cat <<EOF

## Install

- **iOS**: Swift Package Manager, \`https://github.com/$repository\`, exact version \`$sdk\`, product \`PayrdrSDK\`
  (\`import sdk_payment_scanner\`); or embed \`sdk_payment_scanner.xcframework\` from the zip below.
- **Android**: $android.
- **Flutter** \`$plugin\`: unpack \`payrdr_sdk-$plugin.tar.gz\` and depend on it by \`path\`.
- **React Native** \`$plugin\`: \`npm install ./payrdr-react-native-sdk-$plugin.tgz\`.

$plugins_android
EOF
} > "$dist/notes.md"

ls -1 "$dist"
