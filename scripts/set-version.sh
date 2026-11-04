#!/usr/bin/env bash
# Sets the version everywhere it is written:
#   - the SDK's: gradle.properties (the code and the iOS frameworks take it from there at build time) and the SDK version
#     the Flutter and React Native plugins depend on (written as text: they are built in the app, outside this build);
#   - the plugins' own, when given: pubspec.yaml, package.json and the lockfiles that record them.
# ./gradlew verifyVersions checks that they agree.
#
# Usage: scripts/set-version.sh <sdk-version> [<plugin-version>]   e.g. scripts/set-version.sh 4.0.0-beta.2 1.0.0-beta.2
set -euo pipefail

semver='^[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z.]+)?$'
sdk="${1:-}"
plugin="${2:-}"
if [[ ! "$sdk" =~ $semver || ( -n "$plugin" && ! "$plugin" =~ $semver ) ]]; then
  echo "usage: $0 <sdk-version> [<plugin-version>]   (versions as 4.0.0 or 4.0.0-beta.1)" >&2
  exit 2
fi

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"

# Replaces the regex match in a file and fails if the file does not end up with the version.
set_in() {
  local file="$1" version="$2" expr="$3"
  V="$version" perl -0pi -e "$expr" "$file"
  grep -qF "$version" "$file" || { echo "$file: version not set" >&2; exit 1; }
  echo "$file -> $version"
}

set_in gradle.properties "$sdk" 's/^version=.*$/version=$ENV{V}/m'
for file in plugins/flutter/android/build.gradle.kts plugins/react-native/android/build.gradle; do
  set_in "$file" "$sdk" 's/(payrdrSdkVersion.*\?: ")[^"]+"/$1$ENV{V}"/'
done

[[ -n "$plugin" ]] || exit 0
set_in plugins/flutter/pubspec.yaml "$plugin" 's/^version: .*$/version: $ENV{V}/m'
set_in samples/flutter-app/pubspec.lock "$plugin" \
  's/(path: "\.\.\/\.\.\/plugins\/flutter"\n(?:.*\n){2}\s*version: ")[^"]+"/$1$ENV{V}"/'
for file in plugins/react-native/package.json plugins/react-native/package-lock.json samples/react-native-app/package-lock.json; do
  set_in "$file" "$plugin" 's/("name": "\@payrdr\/react-native-sdk",\n\s*"version": ")[^"]+"/$1$ENV{V}"/g'
done
