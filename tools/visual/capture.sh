#!/bin/bash
# Captures the checkout's screenshots into tools/visual/out/<set-name>/.
#
#   tools/visual/capture.sh <set-name> [test-name]     e.g. capture.sh baseline
#                                                           capture.sh try testCardForm
#
# Starts the fake gateway, pins the status bar, runs SampleAppUITests/CheckoutVisualTests on the simulator and stops
# the gateway. SIMULATOR_UDID and VISUAL_PORT override the defaults; VISUAL_CLASS runs another test class
# (e.g. CheckoutAccessibilityTests).
#
# The checkout is SwiftUI on iOS. golden/compose was recorded from the Compose checkout of 63598d0 (plus the
# worktree changes of the SwiftUI port), before Compose left iOS; the SwiftUI screens are compared against it.
set -euo pipefail

SET="${1:?usage: capture.sh <set-name> [test-name]}"
ONLY="SampleAppUITests/${VISUAL_CLASS:-CheckoutVisualTests}${2:+/$2}"
UDID="${SIMULATOR_UDID:-EF5F96B4-835E-4989-B438-10E395AB3775}"
PORT="${VISUAL_PORT:-8787}"
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
OUT="$HERE/out/$SET"
WORK="$REPO/build/visual"

# VISUAL_TREE: a copy of the sources to build instead of this checkout (e.g. the committed state while the working
# tree is mid-change); the test class is copied into it.
TREE="${VISUAL_TREE:-$REPO}"
DERIVED="$WORK/DerivedData"
TEST_FILE="samples/ios-app/SampleAppUITests/CheckoutVisualTests.swift"
if [ "$TREE" != "$REPO" ]; then
  DERIVED="$WORK/DerivedData-$(basename "$TREE")"
  cp "$REPO/$TEST_FILE" "$TREE/$TEST_FILE"
fi

mkdir -p "$WORK"
# Xcode resolves the XCFramework before the app's Gradle build phase runs, so a fresh tree needs it built once.
if [ ! -d "$TREE/sdk-payment-scanner/build/XCFrameworks/debug/RadarpaymentsSDKPayment.xcframework" ]; then
  echo "Building the debug XCFramework (first run in this tree, a few minutes)…"
  # The JDK of the app's own build phase: Gradle here runs on 17–21 only.
  JAVA_HOME="${VISUAL_JAVA_HOME:-$HOME/.sdkman/candidates/java/21.0.2-tem}" "$TREE/gradlew" -p "$TREE" \
    :sdk-payment-scanner:assembleRadarpaymentsSDKPaymentDebugXCFramework > "$WORK/gradle-$SET.log" 2>&1 \
    || { echo "Gradle failed, see $WORK/gradle-$SET.log"; exit 1; }
fi
rm -rf "$OUT"
mkdir -p "$OUT"

python3 "$HERE/fake_gateway.py" --port "$PORT" > "$WORK/gateway-$SET.log" 2>&1 &
GATEWAY=$!
trap 'kill $GATEWAY 2>/dev/null || true; wait $GATEWAY 2>/dev/null || true; xcrun simctl status_bar "$UDID" clear 2>/dev/null || true' EXIT
for _ in $(seq 1 50); do
  curl -fs "http://127.0.0.1:$PORT/__health" > /dev/null && break
  sleep 0.1
done
curl -fs "http://127.0.0.1:$PORT/__health" > /dev/null || { echo "The fake gateway did not start, see $WORK/gateway-$SET.log"; exit 1; }

xcrun simctl bootstatus "$UDID" -b > /dev/null
xcrun simctl status_bar "$UDID" override --time 9:41 --batteryState charged --batteryLevel 100 --cellularBars 4 --wifiBars 3
xcrun simctl ui "$UDID" appearance light

STARTED=$(date +%s)
STATUS=0
# TEST_RUNNER_* variables reach the test runner without the prefix.
TEST_RUNNER_VISUAL_OUT="$OUT" TEST_RUNNER_VISUAL_DUMP="${VISUAL_DUMP:-}" TEST_RUNNER_VISUAL_GATEWAY="http://127.0.0.1:$PORT" \
  xcodebuild -project "$TREE/samples/ios-app/SampleApp.xcodeproj" -scheme SampleApp \
    -destination "platform=iOS Simulator,id=$UDID" \
    -derivedDataPath "$DERIVED" \
    -only-testing:"$ONLY" \
    -parallel-testing-enabled NO \
    -collect-test-diagnostics never \
    test > "$WORK/xcodebuild-$SET.log" 2>&1 || STATUS=$?

grep -E "Test Case .* (passed|failed)|error:|\*\* TEST" "$WORK/xcodebuild-$SET.log" | cut -c1-240 || true
echo "$(ls "$OUT" | grep -c '\.png$' || true) screenshots in $OUT, $(( $(date +%s) - STARTED )) s; log: $WORK/xcodebuild-$SET.log"
exit $STATUS
