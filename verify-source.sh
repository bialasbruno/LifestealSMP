#!/usr/bin/env bash
set -euo pipefail

if grep -R "PrepareItemCraftingEvent" -n LifestealCore/src; then
  echo "ERROR: old/nonexistent PrepareItemCraftingEvent still present" >&2
  exit 1
fi
if grep -n "relocate 'org.sqlite'" LifestealCore/build.gradle; then
  echo "ERROR: sqlite relocation still present" >&2
  exit 1
fi
if grep -n "relocate 'org.sqlite'" LifestealSouls/build.gradle; then
  echo "ERROR: sqlite relocation still present in LifestealSouls" >&2
  exit 1
fi
grep -q "junit-platform-launcher" LifestealCore/build.gradle
grep -q "paper-api:26.2.build.112-stable" LifestealCore/build.gradle
grep -q "include('LifestealCore')" settings.gradle
grep -q "include('LifestealScoreboard')" settings.gradle
grep -q "include('LifestealSouls')" settings.gradle
grep -q "include('LifestealSoulItems')" settings.gradle
grep -q "include('LifestealSoulShop')" settings.gradle
grep -q "include('LifestealSpawn')" settings.gradle
grep -q "junit-platform-launcher" LifestealSouls/build.gradle
grep -q "paper-api:26.2.build.112-stable" LifestealSouls/build.gradle
grep -q "implements LifestealSoulsApi" LifestealSouls/src/main/java/dev/lifesteal/souls/LifestealSoulsPlugin.java
grep -q "paper-api:26.2.build.112-stable" LifestealSoulItems/build.gradle
grep -q "junit-platform-launcher" LifestealSoulItems/build.gradle
grep -q "implements LifestealSoulItemsApi" LifestealSoulItems/src/main/java/dev/lifesteal/soulitems/LifestealSoulItemsPlugin.java
grep -q 'setItemModel(SOUL_PICKAXE_MODEL)' LifestealSoulItems/src/main/java/dev/lifesteal/soulitems/item/SoulItemFactory.java
grep -q "paper-api:26.2.build.112-stable" LifestealSoulShop/build.gradle
grep -q "junit-platform-launcher" LifestealSoulShop/build.gradle
grep -q "LifestealSoulsApi" LifestealSoulShop/src/main/java/dev/lifesteal/soulshop/LifestealSoulShopPlugin.java
grep -q "paper-api:26.2.build.112-stable" LifestealSpawn/build.gradle
grep -q "junit-platform-launcher" LifestealSpawn/build.gradle
grep -q "DamageCause.VOID" LifestealSpawn/src/main/java/dev/lifesteal/spawn/rescue/VoidRescueListener.java
grep -q "placeholderapi:2.12.3" LifestealScoreboard/build.gradle
grep -q "softdepend:" LifestealScoreboard/src/main/resources/plugin.yml
grep -q "PlaceholderAPI" LifestealScoreboard/src/main/resources/plugin.yml
grep -q "implements LifestealCoreApi" LifestealCore/src/main/java/dev/lifesteal/core/LifestealCorePlugin.java
grep -q 'setItemModel(BROKEN_HEART_MODEL)' LifestealCore/src/main/java/dev/lifesteal/core/heart/HeartItemFactory.java
grep -q 'setItemModel(HEART_MODEL)' LifestealCore/src/main/java/dev/lifesteal/core/heart/HeartItemFactory.java
grep -q 'setItemModel(REVIVE_TOTEM_MODEL)' LifestealCore/src/main/java/dev/lifesteal/core/heart/HeartItemFactory.java
grep -q '"serverpack:heart_consume"' LifestealCore/src/main/java/dev/lifesteal/core/listener/HeartUseListener.java

test -f ServerPack/assets/serverpack/textures/item/broken_heart.png
test -f ServerPack/assets/serverpack/textures/item/heart.png
test -f ServerPack/assets/serverpack/textures/item/revive_totem.png
test -f ServerPack/assets/serverpack/textures/item/soul_pickaxe.png
test -f ServerPack/assets/serverpack/items/soul_pickaxe.json
test -f ServerPack/assets/serverpack/models/item/soul_pickaxe.json
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
