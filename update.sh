#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  echo "Usage: $0 [all|core|scoreboard|souls|soulshop|spawn]"
}

if [[ "$#" -gt 1 ]]; then
  usage >&2
  exit 64
fi

TARGET="${1:-all}"
case "$TARGET" in
  all|core|scoreboard|souls|soulshop|spawn) ;;
  -h|--help)
    usage
    exit 0
    ;;
  *)
    echo "Unknown update target: $TARGET" >&2
    usage >&2
    exit 64
    ;;
esac

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "ERROR: Ten katalog nie jest repozytorium Git." >&2
  exit 1
fi

if ! git remote get-url origin >/dev/null 2>&1; then
  echo "ERROR: Repozytorium nie ma skonfigurowanego remote 'origin'." >&2
  exit 1
fi

echo "Pobieranie zmian z GitHub..."
git pull --ff-only
echo

exec "$ROOT/deploy.sh" "$TARGET"
