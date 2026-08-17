#!/usr/bin/env bash
set -euo pipefail

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required." >&2
  exit 1
fi

./verify-source.sh

echo "Building LifestealCore and LifestealScoreboard with the official Gradle JDK 25 image..."
docker run --rm \
  -v "$PWD":/workspace \
  -w /workspace \
  gradle:jdk25-noble \
  gradle --no-daemon clean build

CORE_JAR="build/libs/LifestealCore-0.2.1.jar"
SCOREBOARD_JAR="LifestealScoreboard/build/libs/LifestealScoreboard-0.1.0.jar"

for jar in "$CORE_JAR" "$SCOREBOARD_JAR"; do
  if [[ ! -f "$jar" ]]; then
    echo "Build finished, but expected jar was not found at $jar" >&2
    exit 2
  fi
done

echo
echo "SUCCESS: $CORE_JAR"
echo "SUCCESS: $SCOREBOARD_JAR"
