#!/usr/bin/env bash
# Builds what the Flutter and React Native plugins take from this repository:
#   - the SDK's Android libraries into build/maven (the plugins' Gradle files read them from there);
#   - the iOS framework PayrdrSdkBridge.xcframework, copied into each plugin's iOS folder;
#   - the card scanner's camera (Swift sources of RadarpaymentsSDKScanner), copied next to each plugin's own sources:
#     the framework has no camera code, so that an app without the scanner needs no camera permission.
#
# Usage: scripts/build-bridge.sh [debug|release]   (default: debug; release is optimised and smaller, but slower to build)
set -euo pipefail

variant="${1:-debug}"
case "$variant" in
  debug) task=assemblePayrdrSdkBridgeDebugXCFramework ;;
  release) task=assemblePayrdrSdkBridgeReleaseXCFramework ;;
  *) echo "usage: $0 [debug|release]" >&2; exit 2 ;;
esac

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"

# The build needs JDK 17-21; Android Studio's runtime is one, a newer system JDK may not be.
studio_jbr="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
if [[ -d "$studio_jbr" ]]; then export JAVA_HOME="$studio_jbr"; fi

./gradlew publishAndroidToRepo ":sdk-bridge:$task"

framework="sdk-bridge/build/XCFrameworks/$variant/PayrdrSdkBridge.xcframework"
for dest in plugins/flutter/ios/payrdr_sdk/Frameworks plugins/react-native/ios/Frameworks; do
  [[ -d "$(dirname "$dest")" ]] || continue
  mkdir -p "$dest"
  rsync -a --delete "$framework" "$dest/"
  echo "PayrdrSdkBridge.xcframework ($variant) -> $dest"
done
scanner="sdk-payment-scanner/ios/Sources/RadarpaymentsSDKScanner"
for dest in plugins/flutter/ios/payrdr_sdk/Sources/payrdr_sdk/Scanner plugins/react-native/ios/Scanner; do
  [[ -d "$(dirname "$dest")" ]] || continue
  mkdir -p "$dest"
  rsync -a --delete "$scanner/" "$dest/"
  echo "RadarpaymentsSDKScanner sources -> $dest"
done
echo "Android libraries -> build/maven"
