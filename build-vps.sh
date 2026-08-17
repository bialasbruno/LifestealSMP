#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 [all|core|scoreboard]"
}

if [[ "$#" -gt 1 ]]; then
  usage >&2
  exit 64
fi

TARGET="${1:-all}"
case "$TARGET" in
  all|core|scoreboard) ;;
  -h|--help)
    usage
    exit 0
    ;;
  *)
    echo "Unknown build target: $TARGET" >&2
    usage >&2
    exit 64
    ;;
esac

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required." >&2
  exit 1
fi

./verify-source.sh

case "$TARGET" in
  all)
    GRADLE_TASKS=(clean build)
    EXPECTED_JARS=(
      "LifestealCore/build/libs/LifestealCore-0.2.1.jar"
      "LifestealScoreboard/build/libs/LifestealScoreboard-0.1.0.jar"
    )
    ;;
  core)
    GRADLE_TASKS=(:LifestealCore:clean :LifestealCore:build)
    EXPECTED_JARS=("LifestealCore/build/libs/LifestealCore-0.2.1.jar")
    ;;
  scoreboard)
    GRADLE_TASKS=(:LifestealScoreboard:clean :LifestealScoreboard:build)
    EXPECTED_JARS=("LifestealScoreboard/build/libs/LifestealScoreboard-0.1.0.jar")
    ;;
esac

echo "Building target '$TARGET' with the official Gradle JDK 25 image..."
docker run --rm \
  -v "$PWD":/workspace \
  -w /workspace \
  gradle:jdk25-noble \
  gradle --no-daemon "${GRADLE_TASKS[@]}"

for jar in "${EXPECTED_JARS[@]}"; do
  if [[ ! -f "$jar" ]]; then
    echo "Build finished, but expected jar was not found at $jar" >&2
    exit 2
  fi
done

echo
for jar in "${EXPECTED_JARS[@]}"; do
  echo "SUCCESS: $jar"
done
