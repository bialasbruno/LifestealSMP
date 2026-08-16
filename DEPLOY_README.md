# LifestealSMP — szybki deploy

Ten projekt zawiera jednocześnie:
- LifestealCore (plugin Paper),
- `ServerPack/` (źródła resource packa),
- `deploy.sh` (build + testy + deploy),
- `update.sh` (opcjonalny `git pull` + deploy).

## Normalna aktualizacja

Po zmianie kodu pluginu lub plików w `ServerPack/`:

```bash
cd ~/LifestealCore
./deploy.sh
```

Nie uruchamiaj `sudo ./deploy.sh`.
Skrypt sam poprosi o hasło sudo.

Skrypt automatycznie:
1. uruchamia Gradle/JDK 25 build w Dockerze,
2. uruchamia testy,
3. tworzy `build/ServerPack.zip`,
4. robi backup poprzedniego deploymentu,
5. atomowo podmienia plugin JAR w Pterodactylu,
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

`update.sh` zrobi:

```text
git pull --ff-only
        ↓
./deploy.sh
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
- JAR: `LifestealCore-0.1.0.jar`

## Backupi

Każdy deploy zapisuje poprzednią wersję w:

```text
~/.lifesteal-deploy-backups/
```

Dzięki temu poprzedni JAR, `server.properties` i ServerPack można łatwo odzyskać.
