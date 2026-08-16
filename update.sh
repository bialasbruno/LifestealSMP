#!/usr/bin/env bash
set -Eeuo pipefail

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

exec "$ROOT/deploy.sh"
