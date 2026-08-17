# LifestealSMP — szybki deploy

Ten projekt zawiera jednocześnie:
- LifestealCore (plugin Paper),
- LifestealScoreboard (plugin Paper),
- LifestealSouls (plugin Paper),
- LifestealSoulItems (plugin Paper),
- LifestealSoulShop (plugin Paper),
- LifestealSpawn (plugin Paper),
- LifestealHomes (plugin Paper),
- `ServerPack/` (źródła resource packa),
- `deploy.sh` (build + testy + deploy),
- `update.sh` (opcjonalny `git pull` + deploy).

## Normalna aktualizacja

Po zmianie kodu pluginów lub plików w `ServerPack/`:

```bash
cd ~/LifestealCore
./deploy.sh
```

Bez argumentu wykonywany jest pełny deployment. Dostępne cele:

```bash
./deploy.sh all
./deploy.sh core
./deploy.sh scoreboard
./deploy.sh souls
./deploy.sh soulitems
./deploy.sh soulshop
./deploy.sh spawn
./deploy.sh homes
```

- `all` — wszystkie pluginy i ServerPack,
- `core` — LifestealCore i ServerPack,
- `scoreboard` — wyłącznie LifestealScoreboard, bez przebudowy i publikacji
  ServerPacka,
- `souls` — wyłącznie LifestealSouls, bez przebudowy i publikacji ServerPacka.
- `soulitems` — LifestealSoulItems oraz ServerPack zawierający modele i tekstury
  customowych przedmiotów.
- `soulshop` — wyłącznie LifestealSoulShop, bez przebudowy i publikacji
  ServerPacka.
- `spawn` — wyłącznie LifestealSpawn, bez przebudowy i publikacji ServerPacka.
- `homes` — wyłącznie LifestealHomes, bez przebudowy i publikacji ServerPacka.

Nie uruchamiaj `sudo ./deploy.sh`.
Skrypt sam poprosi o hasło sudo.

W trybie `all` skrypt automatycznie:
1. uruchamia Gradle/JDK 25 build w Dockerze,
2. uruchamia testy,
3. tworzy `build/ServerPack.zip`,
4. robi backup poprzedniego deploymentu,
5. atomowo podmienia wszystkie pluginy JAR w Pterodactylu,
6. publikuje ServerPack przez Nginx,
7. liczy SHA-1,
8. aktualizuje `server.properties`,
9. sprawdza publiczny URL resource packa.

Na końcu pozostaje tylko **Restart** w Pterodactylu.

## Aktualizacja z GitHub

Repozytorium ma skonfigurowany `origin`, więc wystarczy:

```bash
cd ~/LifestealCore
./update.sh
```

`update.sh` przyjmuje te same cele:

```bash
./update.sh all
./update.sh core
./update.sh scoreboard
./update.sh souls
./update.sh soulitems
./update.sh soulshop
./update.sh spawn
./update.sh homes
```

`update.sh` zrobi:

```text
git pull --ff-only
        ↓
./deploy.sh <wybrany cel>
        ↓
Restart w Pterodactylu
```

## Konfiguracja

Stałe dotyczące VPS-a są w:

```text
deploy.env
```

Obecnie skonfigurowane są:
- Pterodactyl volume: `89c0f685-a4cd-4c68-b2cc-dc1338f04837`
- pack URL: `http://159.195.42.157/resourcepacks/ServerPack.zip`
- build Core JAR: `LifestealCore/build/libs/LifestealCore-0.2.1.jar`
- build Scoreboard JAR: `LifestealScoreboard/build/libs/LifestealScoreboard-0.1.0.jar`
- build Souls JAR: `LifestealSouls/build/libs/LifestealSouls-0.1.0.jar`
- build SoulItems JAR: `LifestealSoulItems/build/libs/LifestealSoulItems-0.1.0.jar`
- build SoulShop JAR: `LifestealSoulShop/build/libs/LifestealSoulShop-0.1.0.jar`
- build Spawn JAR: `LifestealSpawn/build/libs/LifestealSpawn-0.1.0.jar`
- build Homes JAR: `LifestealHomes/build/libs/LifestealHomes-0.1.0.jar`
- docelowe JAR-y Paper: `LifestealCore.jar`, `LifestealScoreboard.jar`,
  `LifestealSouls.jar`, `LifestealSoulItems.jar`, `LifestealSoulShop.jar` i
  `LifestealSpawn.jar` i `LifestealHomes.jar`

## Backupi

Każdy deploy zapisuje poprzednią wersję wybranych pluginów w:

```text
~/.lifesteal-deploy-backups/
```

W trybach `all`, `core` i `soulitems` backup obejmuje również
`server.properties` oraz ServerPack. Dzięki temu poprzednią wdrożoną wersję
można łatwo odzyskać.
