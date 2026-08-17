#!/usr/bin/env bash
set -euo pipefail

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required." >&2
  exit 1
fi

./verify-source.sh

echo "Building LifestealCore with the official Gradle JDK 25 image..."
docker run --rm \
  -v "$PWD":/workspace \
  -w /workspace \
  gradle:jdk25-noble \
  gradle --no-daemon clean build

JAR="build/libs/LifestealCore-0.2.0.jar"
if [[ ! -f "$JAR" ]]; then
  echo "Build finished, but expected jar was not found at $JAR" >&2
  echo "Contents of build/libs:" >&2
  ls -la build/libs >&2 || true
  exit 2
fi

echo
echo "SUCCESS: $JAR"
