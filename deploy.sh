#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  echo "Usage: $0 [all|core|scoreboard]"
}

if [[ "$#" -gt 1 ]]; then
  usage >&2
  exit 64
fi

TARGET="${1:-all}"
DEPLOY_CORE=false
DEPLOY_SCOREBOARD=false
DEPLOY_PACK=false

case "$TARGET" in
  all)
    DEPLOY_CORE=true
    DEPLOY_SCOREBOARD=true
    DEPLOY_PACK=true
    ;;
  core)
    DEPLOY_CORE=true
    DEPLOY_PACK=true
    ;;
  scoreboard)
    DEPLOY_SCOREBOARD=true
    ;;
  -h|--help)
    usage
    exit 0
    ;;
  *)
    echo "ERROR: Nieznany cel deploymentu: $TARGET" >&2
    usage >&2
    exit 64
    ;;
esac

if [[ "${EUID}" -eq 0 ]]; then
  echo "ERROR: Uruchom ten skrypt jako zwykly uzytkownik, bez 'sudo'."
  echo "Skrypt sam poprosi o sudo tylko tam, gdzie jest potrzebne."
  exit 1
fi

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

if [[ ! -f "$ROOT/deploy.env" ]]; then
  echo "ERROR: Brak deploy.env"
  exit 1
fi

# shellcheck disable=SC1091
source "$ROOT/deploy.env"

REQUIRED_COMMANDS=(sudo docker)
if [[ "$DEPLOY_PACK" == true ]]; then
  REQUIRED_COMMANDS+=(python3 sha1sum curl)
fi

for cmd in "${REQUIRED_COMMANDS[@]}"; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "ERROR: Brak wymaganej komendy: $cmd"
    exit 1
  fi
done

echo "======================================"
echo " LifestealSMP deploy"
echo "======================================"
echo " Target: $TARGET"
echo
echo "1/7 Autoryzacja sudo..."
sudo -v

if ! sudo test -d "$SERVER_VOL"; then
  echo "ERROR: Nie istnieje volume Pterodactyla:"
  echo "  $SERVER_VOL"
  exit 1
fi

if [[ "$DEPLOY_PACK" == true ]] && ! sudo test -f "$SERVER_VOL/server.properties"; then
  echo "ERROR: Brak server.properties w:"
  echo "  $SERVER_VOL"
  exit 1
fi

if [[ "$DEPLOY_PACK" == true && ! -f "$SERVERPACK_SOURCE/pack.mcmeta" ]]; then
  echo "ERROR: Brak $SERVERPACK_SOURCE/pack.mcmeta"
  exit 1
fi

echo
echo "2/7 Build pluginow + testy..."
# build-vps.sh uses Docker. The user is intentionally not in the docker group,
# so we run only this build step through sudo.
sudo ./build-vps.sh "$TARGET"

EXPECTED_PLUGIN_JARS=()
if [[ "$DEPLOY_CORE" == true ]]; then
  EXPECTED_PLUGIN_JARS+=("$CORE_PLUGIN_BUILD_JAR")
fi
if [[ "$DEPLOY_SCOREBOARD" == true ]]; then
  EXPECTED_PLUGIN_JARS+=("$SCOREBOARD_PLUGIN_BUILD_JAR")
fi

for plugin_jar in "${EXPECTED_PLUGIN_JARS[@]}"; do
  if [[ ! -f "$plugin_jar" ]]; then
    echo "ERROR: Build zakonczyl sie bez oczekiwanego JAR-a:"
    echo "  $plugin_jar"
    exit 1
  fi
done

# Give the normal user ownership of every module build directory touched by
# Gradle. Building Scoreboard can also compile its LifestealCore dependency.
BUILD_DIRS=()
for build_dir in "$ROOT/LifestealCore/build" "$ROOT/LifestealScoreboard/build"; do
  if [[ -d "$build_dir" ]]; then
    BUILD_DIRS+=("$build_dir")
  fi
done
if [[ "${#BUILD_DIRS[@]}" -gt 0 ]]; then
  sudo chown -R "$(id -u):$(id -g)" "${BUILD_DIRS[@]}"
fi

echo
echo "3/7 Budowanie ServerPack.zip..."
if [[ "$DEPLOY_PACK" == true ]]; then
  rm -f "$SERVERPACK_BUILD"

  python3 - "$SERVERPACK_SOURCE" "$SERVERPACK_BUILD" <<'PY'
from pathlib import Path
import sys, zipfile

src = Path(sys.argv[1]).resolve()
dst = Path(sys.argv[2]).resolve()
dst.parent.mkdir(parents=True, exist_ok=True)

skip_names = {".DS_Store", "Thumbs.db"}
with zipfile.ZipFile(dst, "w", compression=zipfile.ZIP_DEFLATED) as zf:
    for path in sorted(src.rglob("*")):
        if not path.is_file():
            continue
        rel = path.relative_to(src)
        if any(part.startswith("__MACOSX") for part in rel.parts):
            continue
        if path.name in skip_names:
            continue
        zf.write(path, rel)

with zipfile.ZipFile(dst) as zf:
    names = set(zf.namelist())
    if "pack.mcmeta" not in names:
        raise SystemExit("ServerPack.zip nie ma pack.mcmeta w root ZIP-a")
    if not any(name.startswith("assets/") for name in names):
        raise SystemExit("ServerPack.zip nie zawiera assets/")

print(f"OK: {dst}")
PY

  PACK_SHA1="$(sha1sum "$SERVERPACK_BUILD" | awk '{print $1}')"
else
  echo "Pominieto dla celu '$TARGET'."
fi

echo
echo "4/7 Backup aktualnego deploymentu..."
STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_DIR="$HOME/.lifesteal-deploy-backups/$STAMP"
mkdir -p "$BACKUP_DIR"

PLUGIN_DIR="$SERVER_VOL/plugins"
sudo mkdir -p "$BACKUP_DIR/plugins"
if [[ "$DEPLOY_CORE" == true ]]; then
  while IFS= read -r deployed_plugin; do
    sudo cp -a "$deployed_plugin" "$BACKUP_DIR/plugins/"
  done < <(
    sudo find "$PLUGIN_DIR" -maxdepth 1 -type f \
      \( -name 'LifestealCore.jar' -o -name 'LifestealCore-*.jar' \) -print
  )
fi
if [[ "$DEPLOY_SCOREBOARD" == true ]]; then
  while IFS= read -r deployed_plugin; do
    sudo cp -a "$deployed_plugin" "$BACKUP_DIR/plugins/"
  done < <(
    sudo find "$PLUGIN_DIR" -maxdepth 1 -type f \
      \( -name 'LifestealScoreboard.jar' -o -name 'LifestealScoreboard-*.jar' \) -print
  )
fi
if ! sudo find "$BACKUP_DIR/plugins" -mindepth 1 -print -quit | grep -q .; then
  sudo rmdir "$BACKUP_DIR/plugins"
fi

if [[ "$DEPLOY_PACK" == true ]]; then
  sudo cp -a "$SERVER_VOL/server.properties" "$BACKUP_DIR/server.properties"

  if sudo test -f "$WEB_PACK"; then
    sudo cp -a "$WEB_PACK" "$BACKUP_DIR/ServerPack.zip"
  fi
fi

sudo chown -R "$(id -u):$(id -g)" "$BACKUP_DIR"

echo
echo "5/7 Deploy pluginow i resource packa..."

# Atomic JAR replacement. This is safer even if the Minecraft process is still
# running because the old open file remains available until restart.
if [[ "$DEPLOY_CORE" == true ]]; then
  sudo install -o pterodactyl -g pterodactyl -m 0644 \
    "$CORE_PLUGIN_BUILD_JAR" "$PLUGIN_DIR/.${CORE_PLUGIN_TARGET_NAME}.new"
  sudo mv -f "$PLUGIN_DIR/.${CORE_PLUGIN_TARGET_NAME}.new" \
    "$PLUGIN_DIR/$CORE_PLUGIN_TARGET_NAME"
fi
if [[ "$DEPLOY_SCOREBOARD" == true ]]; then
  sudo install -o pterodactyl -g pterodactyl -m 0644 \
    "$SCOREBOARD_PLUGIN_BUILD_JAR" "$PLUGIN_DIR/.${SCOREBOARD_PLUGIN_TARGET_NAME}.new"
  sudo mv -f "$PLUGIN_DIR/.${SCOREBOARD_PLUGIN_TARGET_NAME}.new" \
    "$PLUGIN_DIR/$SCOREBOARD_PLUGIN_TARGET_NAME"
fi

# Stable target names are now in place, so remove only versioned legacy copies to
# prevent Paper loading the same plugin more than once after a version upgrade.
if [[ "$DEPLOY_CORE" == true ]]; then
  sudo find "$PLUGIN_DIR" -maxdepth 1 -type f -name 'LifestealCore-*.jar' -delete
fi
if [[ "$DEPLOY_SCOREBOARD" == true ]]; then
  sudo find "$PLUGIN_DIR" -maxdepth 1 -type f -name 'LifestealScoreboard-*.jar' -delete
fi

if [[ "$DEPLOY_PACK" == true ]]; then
  WEB_DIR="$(dirname "$WEB_PACK")"
  sudo mkdir -p "$WEB_DIR"
  sudo install -o www-data -g www-data -m 0644 \
    "$SERVERPACK_BUILD" "$WEB_DIR/.ServerPack.zip.new"
  sudo mv -f "$WEB_DIR/.ServerPack.zip.new" "$WEB_PACK"
fi

echo
echo "6/7 Aktualizacja server.properties..."

if [[ "$DEPLOY_PACK" == true ]]; then
  # Modify in-place to preserve the Pterodactyl file ownership/inode metadata.
  sudo python3 - \
  "$SERVER_VOL/server.properties" \
  "$PACK_URL" \
  "$PACK_SHA1" \
  "$REQUIRE_RESOURCE_PACK" \
  "$RESOURCE_PACK_PROMPT" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
values = {
    "resource-pack": sys.argv[2],
    "resource-pack-sha1": sys.argv[3],
    "require-resource-pack": sys.argv[4],
    "resource-pack-prompt": sys.argv[5],
}

text = path.read_text(encoding="utf-8")
lines = text.splitlines()
seen = set()
out = []

for line in lines:
    if "=" in line and not line.lstrip().startswith("#"):
        key = line.split("=", 1)[0]
        if key in values:
            out.append(f"{key}={values[key]}")
            seen.add(key)
            continue
    out.append(line)

for key, value in values.items():
    if key not in seen:
        out.append(f"{key}={value}")

with path.open("w", encoding="utf-8", newline="\n") as f:
    f.write("\n".join(out) + "\n")
PY
else
  echo "Pominieto dla celu '$TARGET'."
fi

echo
echo "7/7 Weryfikacja..."
if [[ "$DEPLOY_CORE" == true ]] && ! sudo test -s "$PLUGIN_DIR/$CORE_PLUGIN_TARGET_NAME"; then
  echo "ERROR: Brak wdrozonego pluginu Core." >&2
  exit 1
fi
if [[ "$DEPLOY_SCOREBOARD" == true ]] && ! sudo test -s "$PLUGIN_DIR/$SCOREBOARD_PLUGIN_TARGET_NAME"; then
  echo "ERROR: Brak wdrozonego pluginu Scoreboard." >&2
  exit 1
fi

if [[ "$DEPLOY_PACK" == true ]]; then
  DEPLOYED_SHA1="$(sudo sha1sum "$WEB_PACK" | awk '{print $1}')"

  if [[ "$DEPLOYED_SHA1" != "$PACK_SHA1" ]]; then
    echo "ERROR: SHA-1 pliku po deployu nie zgadza sie."
    echo "Build:  $PACK_SHA1"
    echo "Deploy: $DEPLOYED_SHA1"
    exit 1
  fi

  HTTP_COPY="$(mktemp)"
  trap 'rm -f "$HTTP_COPY"' EXIT

  if ! curl -fsSL --retry 3 --retry-delay 1 "$PACK_URL" -o "$HTTP_COPY"; then
    echo "ERROR: Publiczny ServerPack nie jest dostepny pod adresem:"
    echo "  $PACK_URL"
    exit 1
  fi

  HTTP_SHA1="$(sha1sum "$HTTP_COPY" | awk '{print $1}')"
  if [[ "$HTTP_SHA1" != "$PACK_SHA1" ]]; then
    echo "ERROR: Publicznie pobrany ServerPack ma niepoprawny SHA-1."
    echo "Build: $PACK_SHA1"
    echo "HTTP:  $HTTP_SHA1"
    exit 1
  fi

  rm -f "$HTTP_COPY"
  trap - EXIT
  HTTP_STATUS="OK (pobrano plik i potwierdzono SHA-1)"
fi

echo
echo "======================================"
echo " DEPLOY SUCCESSFUL"
echo "======================================"
echo "Target:       $TARGET"
if [[ "$DEPLOY_CORE" == true ]]; then
  echo "Core plugin:  $PLUGIN_DIR/$CORE_PLUGIN_TARGET_NAME"
fi
if [[ "$DEPLOY_SCOREBOARD" == true ]]; then
  echo "Scoreboard:   $PLUGIN_DIR/$SCOREBOARD_PLUGIN_TARGET_NAME"
fi
if [[ "$DEPLOY_PACK" == true ]]; then
  echo "ServerPack:   $WEB_PACK"
  echo "Pack URL:     $PACK_URL"
  echo "Pack SHA-1:   $PACK_SHA1"
  echo "HTTP check:   $HTTP_STATUS"
fi
echo "Backup:       $BACKUP_DIR"
echo
echo "Teraz wykonaj pelny Restart serwera w Pterodactylu."
if [[ "$DEPLOY_PACK" == true ]]; then
  echo "Po restarcie klient Minecraft pobierze nowa wersje packa po nowym SHA-1."
fi
