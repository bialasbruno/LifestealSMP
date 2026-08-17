# Build na VPS-ie Minecraft

VPS ma Dockera, więc nie trzeba instalować Javy ani Gradle na hoście.

```bash
cd ~/LifestealCore
./build-vps.sh
```

Bez argumentu budowane i testowane są wszystkie pluginy. Można też wybrać
pojedynczy moduł:

```bash
./build-vps.sh core
./build-vps.sh scoreboard
./build-vps.sh souls
./build-vps.sh soulshop
./build-vps.sh spawn
```

Scoreboard korzysta z API Core, więc Gradle może skompilować klasy Core jako
zależność. Nie uruchamia jednak pełnego buildu ani testów Core i nie tworzy jego
produkcyjnego Shadow JAR-a.

Analogicznie SoulShop korzysta z API Souls, więc jego pojedynczy build może
skompilować klasy Souls jako zależność, ale nie uruchamia testów ani pełnego
buildu pluginu Souls.

Pierwsze uruchomienie pobiera oficjalny obraz `gradle:jdk25-noble` oraz zależności Maven.

Po udanym buildzie finalne pluginy znajdują się tutaj:

```text
LifestealCore/build/libs/LifestealCore-0.2.1.jar
LifestealScoreboard/build/libs/LifestealScoreboard-0.1.0.jar
LifestealSouls/build/libs/LifestealSouls-0.1.0.jar
LifestealSoulShop/build/libs/LifestealSoulShop-0.1.0.jar
LifestealSpawn/build/libs/LifestealSpawn-0.1.0.jar
```

Do pełnego deploymentu wszystkich pluginów i ServerPacka użyj `./deploy.sh`.
Selektywny deployment wykonują `./deploy.sh core`, `./deploy.sh scoreboard`,
`./deploy.sh souls`, `./deploy.sh soulshop` i `./deploy.sh spawn`, zgodnie z
`DEPLOY_README.md`.
