#!/usr/bin/env bash
# Prints the versions of the next release, "<sdk-version> <plugin-version>", taken from the release tags (<sdk-version>),
# or fails when the rules do not allow that release:
#   - an alpha may come from any branch; a beta, an rc and a stable release only from main;
#   - a version goes through its stages forward only (alpha, beta, rc, stable; any may be skipped), so once 4.1.0 has a
#     beta, the alphas of other branches go to a later version;
#   - a stable release whose version had an rc is built from the commit that rc was built from.
# Without a bump the release continues the lowest version in pre-release that the stage may continue. A bump opens the
# next version of that kind after the last stable release that no tag has taken yet. With no release tag at all the
# versions come from gradle.properties and pubspec.yaml. The plugins take the same bump and the same pre-release suffix.
#
# Usage: scripts/release-version.sh <alpha|beta|rc|stable> [major|minor|patch]
# The branch is RELEASE_BRANCH, or the checked-out one.
set -euo pipefail

stage="${1:-}"
bump="${2:-}"
if [[ ! "$stage" =~ ^(alpha|beta|rc|stable)$ || ( -n "$bump" && ! "$bump" =~ ^(major|minor|patch)$ ) ]]; then
  echo "usage: $0 <alpha|beta|rc|stable> [major|minor|patch]" >&2
  exit 2
fi

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"

fail() { echo "$*" >&2; exit 1; }

branch="${RELEASE_BRANCH:-$(git rev-parse --abbrev-ref HEAD)}"
[[ "$stage" == alpha || "$branch" == main ]] || fail "A $stage is released from main only, not from $branch."

rank() { case "$1" in alpha) echo 1 ;; beta) echo 2 ;; rc) echo 3 ;; stable) echo 4 ;; esac; }

# 0 when version core $1 is greater than $2.
newer() {
  local a b
  IFS=. read -ra a <<< "$1"
  IFS=. read -ra b <<< "$2"
  for i in 0 1 2; do
    (( a[i] > b[i] )) && return 0
    (( a[i] < b[i] )) && return 1
  done
  return 1
}

bumped() {
  local v
  IFS=. read -ra v <<< "$1"
  case "$2" in
    major) echo "$((v[0] + 1)).0.0" ;;
    minor) echo "${v[0]}.$((v[1] + 1)).0" ;;
    patch) echo "${v[0]}.${v[1]}.$((v[2] + 1))" ;;
  esac
}

core() { echo "${1%%-*}"; }

# The plugins' version core at a tag, or in the working tree without one.
plugin_core() {
  local pubspec
  if [[ -n "$1" ]]; then pubspec="$(git show "$1:plugins/flutter/pubspec.yaml")"; else pubspec="$(cat plugins/flutter/pubspec.yaml)"; fi
  core "$(sed -n 's/^version: *//p' <<< "$pubspec")"
}

tags=()
while IFS= read -r tag; do
  [[ "$tag" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-(alpha|beta|rc)\.[0-9]+)?$ ]] && tags+=("$tag")
done < <(git tag -l)

last_stable=""
for tag in ${tags[@]+"${tags[@]}"}; do
  [[ "$tag" == *-* ]] && continue
  [[ -z "$last_stable" ]] || newer "$tag" "$last_stable" && last_stable="$tag"
done

# What a version has reached: its highest stage, the tag of it and the last number of the stage asked for.
reached() {
  local version="$1" top=0 top_n=0 top_tag="" number=0 tag suffix tag_stage n
  for tag in ${tags[@]+"${tags[@]}"}; do
    [[ "$(core "$tag")" == "$version" ]] || continue
    if [[ "$tag" == *-* ]]; then
      suffix="${tag#*-}"; tag_stage="${suffix%.*}"; n="${suffix#*.}"
    else
      tag_stage=stable; n=0
    fi
    if (( $(rank "$tag_stage") > top || ($(rank "$tag_stage") == top && n > top_n) )); then
      top=$(rank "$tag_stage"); top_n=$n; top_tag="$tag"
    fi
    [[ "$tag_stage" == "$stage" ]] && (( n > number )) && number=$n
  done
  echo "$top $top_tag $number"
}

if (( ${#tags[@]} == 0 )); then
  [[ -z "$bump" ]] || fail "No release yet: the first one takes the versions in gradle.properties and pubspec.yaml, without a bump."
  version="$(core "$(sed -n 's/^version=//p' gradle.properties)")"
  plugin="$(plugin_core "")"
  number=0
elif [[ -n "$bump" ]]; then
  [[ -n "$last_stable" ]] || fail "No stable release yet to bump from: continue the version in pre-release, without a bump."
  version="$last_stable"
  plugin="$(plugin_core "$last_stable")"
  while :; do
    version="$(bumped "$version" "$bump")"
    plugin="$(bumped "$plugin" "$bump")"
    read -r top _ _ <<< "$(reached "$version")"
    (( top == 0 )) && break
  done
  number=0
else
  version=""
  open=()
  for tag in ${tags[@]+"${tags[@]}"}; do
    candidate="$(core "$tag")"
    [[ -z "$last_stable" ]] || newer "$candidate" "$last_stable" || continue
    [[ " ${open[*]-} " == *" $candidate "* ]] || open+=("$candidate")
  done
  for candidate in $(printf '%s\n' ${open[@]+"${open[@]}"} | sort -t. -k1,1n -k2,2n -k3,3n); do
    read -r top top_tag number <<< "$(reached "$candidate")"
    if (( top <= $(rank "$stage") )); then
      version="$candidate"
      plugin="$(plugin_core "$top_tag")"
      break
    fi
  done
  [[ -n "$version" ]] || fail "No version in pre-release can go on to $stage (${open[*]:-none}): pass a bump to open a new one."
  if [[ "$stage" == stable && "$top_tag" == *-rc.* ]]; then
    built_from="$(git rev-parse "$top_tag^{commit}^")"
    [[ "$built_from" == "$(git rev-parse HEAD)" ]] ||
      fail "$version is released from the commit of $top_tag ($built_from); main has moved since: release an rc of it first."
  fi
fi

if [[ "$stage" == stable ]]; then
  echo "$version $plugin"
else
  echo "$version-$stage.$((number + 1)) $plugin-$stage.$((number + 1))"
fi
