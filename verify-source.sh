#!/usr/bin/env bash
set -euo pipefail

if grep -R "PrepareItemCraftingEvent" -n src; then
  echo "ERROR: old/nonexistent PrepareItemCraftingEvent still present" >&2
  exit 1
fi
if grep -n "relocate 'org.sqlite'" build.gradle; then
  echo "ERROR: sqlite relocation still present" >&2
  exit 1
fi
grep -q "junit-platform-launcher" build.gradle
grep -q "paper-api:26.2.build.112-stable" build.gradle
grep -q "include('LifestealScoreboard')" settings.gradle
grep -q "placeholderapi:2.12.3" LifestealScoreboard/build.gradle
grep -q "softdepend:" LifestealScoreboard/src/main/resources/plugin.yml
grep -q "PlaceholderAPI" LifestealScoreboard/src/main/resources/plugin.yml
grep -q "implements LifestealCoreApi" src/main/java/dev/lifesteal/core/LifestealCorePlugin.java
grep -q 'setItemModel(BROKEN_HEART_MODEL)' src/main/java/dev/lifesteal/core/heart/HeartItemFactory.java
grep -q 'setItemModel(HEART_MODEL)' src/main/java/dev/lifesteal/core/heart/HeartItemFactory.java
grep -q 'setItemModel(REVIVE_TOTEM_MODEL)' src/main/java/dev/lifesteal/core/heart/HeartItemFactory.java
grep -q '"serverpack:heart_consume"' src/main/java/dev/lifesteal/core/listener/HeartUseListener.java

test -f ServerPack/assets/serverpack/textures/item/broken_heart.png
test -f ServerPack/assets/serverpack/textures/item/heart.png
test -f ServerPack/assets/serverpack/textures/item/revive_totem.png
test -f ServerPack/assets/serverpack/sounds/items/heart_consume.ogg

# Every Paper plugin descriptor must have a dedicated, complete README.
while IFS= read -r descriptor; do
  plugin_name="$(awk -F ': *' '$1 == "name" { print $2; exit }' "$descriptor" | tr -d '\r')"
  if [[ -z "$plugin_name" ]]; then
    echo "ERROR: cannot read plugin name from $descriptor" >&2
    exit 1
  fi

  plugin_readme="docs/plugins/$plugin_name/README.md"
  if [[ ! -f "$plugin_readme" ]]; then
    echo "ERROR: $descriptor requires $plugin_readme" >&2
    exit 1
  fi
done < <(
  find . -type d \( -name '.git' -o -name 'build' -o -name '.gradle' \) -prune -o \
    -type f \( -name 'plugin.yml' -o -name 'paper-plugin.yml' \) -print
)

if find . -type d \( -name '.git' -o -name 'build' -o -name '.gradle' \) -prune -o \
    -type f \( -name '*.jar' -o -name '*.zip' -o -name '*.db' \) -print \
    | grep -v '^./gradle/wrapper/gradle-wrapper.jar$' \
    | grep -q .; then
  echo "ERROR: generated/runtime artifacts are present in the source tree" >&2
  exit 1
fi

echo "Source checks OK."
