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
grep -q 'setItemModel(BROKEN_HEART_MODEL)' src/main/java/dev/lifesteal/core/heart/HeartItemFactory.java
grep -q 'setItemModel(HEART_MODEL)' src/main/java/dev/lifesteal/core/heart/HeartItemFactory.java
grep -q '"serverpack:heart_consume"' src/main/java/dev/lifesteal/core/listener/HeartUseListener.java

test -f ServerPack/assets/serverpack/textures/item/broken_heart.png
test -f ServerPack/assets/serverpack/textures/item/heart.png
test -f ServerPack/assets/serverpack/sounds/items/heart_consume.ogg

if find . \( -path './.git' -o -path './build' -o -path './.gradle' \) -prune -o \
    -type f \( -name '*.jar' -o -name '*.zip' -o -name '*.db' \) -print \
    | grep -v '^./gradle/wrapper/gradle-wrapper.jar$' \
    | grep -q .; then
  echo "ERROR: generated/runtime artifacts are present in the source tree" >&2
  exit 1
fi

echo "Source checks OK."
