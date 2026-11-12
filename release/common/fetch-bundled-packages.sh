#!/usr/bin/env bash
# fetch-bundled-packages.sh DEST_DIR
#
# Puts the package zips listed in release/bundled-packages.tsv into DEST_DIR (the bundle's
# packages/ folder), after checking the SHA-256 of each against the list. Downloads are
# kept in target/bundled-packages/ of the repository, so a second bundle build (another
# platform, a rebuild) does not download them again. Exits non-zero if a download fails or
# a checksum does not match, so a bundle is never built with the wrong package files.
#
# Used by release/Mac/build-sign-dmg.sh and release/{Linux,Windows}/assemble-bundle.sh.
set -euo pipefail

DEST="${1:?usage: fetch-bundled-packages.sh DEST_DIR}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LIST="$REPO_ROOT/release/bundled-packages.tsv"
CACHE="$REPO_ROOT/target/bundled-packages"
mkdir -p "$DEST" "$CACHE"

sha256() {
    if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | cut -d' ' -f1
    else shasum -a 256 "$1" | cut -d' ' -f1; fi
}

count=0
while IFS=$'\t' read -r name version url sum; do
    case "$name" in ''|'#'*) continue ;; esac
    file="$CACHE/$(basename "$url")"
    if [ ! -f "$file" ] || [ "$(sha256 "$file")" != "$sum" ]; then
        echo "    Downloading $name $version"
        curl -fsSL --retry 3 -o "$file.part" "$url" || { echo "ERROR: cannot download $url" >&2; exit 1; }
        mv "$file.part" "$file"
    fi
    actual="$(sha256 "$file")"
    if [ "$actual" != "$sum" ]; then
        echo "ERROR: checksum of $(basename "$url") is $actual, release/bundled-packages.tsv says $sum" >&2
        exit 1
    fi
    cp "$file" "$DEST/"
    echo "    Bundled $name $version ($(basename "$url"))"
    count=$((count + 1))
done < "$LIST"
echo "    $count bundled package(s) in $(basename "$DEST")/"
