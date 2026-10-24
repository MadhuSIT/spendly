#!/bin/sh
set -eu
GRADLE_VERSION=8.9
CACHE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/spendly-wrapper"
DIST="$CACHE_DIR/gradle-$GRADLE_VERSION"
if [ ! -x "$DIST/bin/gradle" ]; then
  mkdir -p "$CACHE_DIR"
  ARCHIVE="$CACHE_DIR/gradle-$GRADLE_VERSION-bin.zip"
  if [ ! -f "$ARCHIVE" ]; then
    curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ARCHIVE"
  fi
  rm -rf "$DIST.tmp"
  mkdir -p "$DIST.tmp"
  unzip -q "$ARCHIVE" -d "$DIST.tmp"
  mv "$DIST.tmp/gradle-$GRADLE_VERSION" "$DIST"
  rm -rf "$DIST.tmp"
fi
exec "$DIST/bin/gradle" "$@"
