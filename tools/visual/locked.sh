#!/bin/bash
# Runs a command while holding the build lock, so that two agents never run Gradle, xcodebuild or the simulator at once.
#
#   tools/visual/locked.sh tools/visual/capture.sh swiftui-methods test02Methods
#   tools/visual/locked.sh ./gradlew :sdk-payment:linkDebugFrameworkIosSimulatorArm64 -q
set -uo pipefail

REPO="$(cd "$(dirname "$0")/../.." && pwd)"
LOCK="$REPO/build/visual.lock"
mkdir -p "$REPO/build"

until mkdir "$LOCK" 2>/dev/null; do sleep 10; done
trap 'rmdir "$LOCK" 2>/dev/null' EXIT

cd "$REPO"
export JAVA_HOME="${VISUAL_JAVA_HOME:-$HOME/.sdkman/candidates/java/21.0.2-tem}"
"$@"
