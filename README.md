# LifestealSMP

Monorepo projektu serwera Minecraft Lifesteal SMP. Repozytorium zarządza
niezależnymi pluginami Paper, ich wspólnym buildem i testami, ServerPackiem oraz
automatycznym wdrażaniem na VPS z Pterodactylem.

Główny README opisuje pracę z całym repozytorium. Mechaniki, konfiguracja,
komendy i uprawnienia są dokumentowane osobno dla każdego pluginu.

## Struktura repozytorium

```text
LifestealSMP/
├── LifestealCore/       # moduł Gradle pluginu Core
├── LifestealScoreboard/ # moduł Gradle pluginu Scoreboard
├── LifestealSouls/      # moduł Gradle waluty Souls
├── LifestealSoulItems/  # moduł Gradle customowych przedmiotów Souls
├── LifestealSoulShop/   # moduł Gradle sklepu za Souls
├── LifestealSpawn/      # moduł Gradle bezpieczeństwa świata spawn
├── LifestealHomes/      # moduł Gradle domów graczy i GUI
├── ServerPack/          # źródła resource packa
├── docs/plugins/        # dokumentacja poszczególnych pluginów
├── gradle/              # Gradle Wrapper
├── build.gradle         # wspólny build modułów
├── settings.gradle      # lista modułów Gradle
├── build-vps.sh         # build i testy w Dockerze
├── deploy.sh            # backup oraz deployment
├── update.sh            # git pull i uruchomienie deploymentu
├── deploy.env           # niesekretna konfiguracja VPS
├── BUILD_ON_VPS.md
└── DEPLOY_README.md
```

Każdy plugin jest osobnym modułem z własnym `build.gradle`, katalogiem `src/`,
testami i wersją. Główny projekt Gradle pozwala zbudować wszystkie moduły jednym
poleceniem.

## Dokumentacja modułów

| Moduł | Wersja | Dokumentacja |
| --- | --- | --- |
| `LifestealCore` | `0.2.1` | [README pluginu](docs/plugins/LifestealCore/README.md) |
| `LifestealScoreboard` | `0.1.0` | [README pluginu](docs/plugins/LifestealScoreboard/README.md) |
| `LifestealSouls` | `0.1.0` | [README pluginu](docs/plugins/LifestealSouls/README.md) |
| `LifestealSoulItems` | `0.1.0` | [README pluginu](docs/plugins/LifestealSoulItems/README.md) |
| `LifestealSoulShop` | `0.1.0` | [README pluginu](docs/plugins/LifestealSoulShop/README.md) |
| `LifestealSpawn` | `0.1.0` | [README pluginu](docs/plugins/LifestealSpawn/README.md) |
| `LifestealHomes` | `0.1.0` | [README pluginu](docs/plugins/LifestealHomes/README.md) |

## Wymagania

- Paper `26.2`, build `112`
- Java `25`
- Gradle `9.7.0` przez dołączony wrapper
- VPS: Ubuntu `24.04`, Docker oraz Pterodactyl z Wings

Build VPS korzysta z obrazu `gradle:jdk25-noble`, dlatego Java i Gradle nie
muszą być instalowane bezpośrednio na hoście.

## Build i testy

Pełny build lokalny:

```bash
./gradlew clean build
```

Build pojedynczego modułu lokalnie:

```bash
./gradlew :LifestealCore:clean :LifestealCore:build
./gradlew :LifestealScoreboard:clean :LifestealScoreboard:build
./gradlew :LifestealSouls:clean :LifestealSouls:build
./gradlew :LifestealSoulItems:clean :LifestealSoulItems:build
./gradlew :LifestealSoulShop:clean :LifestealSoulShop:build
./gradlew :LifestealSpawn:clean :LifestealSpawn:build
./gradlew :LifestealHomes:clean :LifestealHomes:build
```

Build w Dockerze na VPS:

```bash
./build-vps.sh
./build-vps.sh all
./build-vps.sh core
./build-vps.sh scoreboard
./build-vps.sh souls
./build-vps.sh soulitems
./build-vps.sh soulshop
./build-vps.sh spawn
./build-vps.sh homes
```

Brak argumentu oznacza `all`. Tryb `scoreboard` może skompilować klasy Core,
ponieważ Scoreboard korzysta z jego API, ale nie uruchamia pełnego buildu ani
testów Core. Tak samo tryb `soulshop` może skompilować klasy Souls i SoulItems,
a tryb `spawn` klasy Souls jako zależności, ale nie uruchamia testów tych
modułów.

Finalne JAR-y powstają w katalogach modułów:

```text
LifestealCore/build/libs/LifestealCore-0.2.1.jar
LifestealScoreboard/build/libs/LifestealScoreboard-0.1.0.jar
LifestealSouls/build/libs/LifestealSouls-0.1.0.jar
LifestealSoulItems/build/libs/LifestealSoulItems-0.1.0.jar
LifestealSoulShop/build/libs/LifestealSoulShop-0.1.0.jar
LifestealSpawn/build/libs/LifestealSpawn-0.1.0.jar
LifestealHomes/build/libs/LifestealHomes-0.1.0.jar
```

Wygenerowane JAR-y, ZIP-y, bazy danych oraz katalogi `build/` nie są
commitowane.

## Aktualizacja i deployment

Najczęstsza operacja na VPS:

```bash
cd ~/LifestealCore
./update.sh [all|core|scoreboard|souls|soulitems|soulshop|spawn|homes]
```

`update.sh` wykonuje `git pull --ff-only`, a następnie przekazuje wybrany cel do
`deploy.sh`. Brak argumentu oznacza pełną aktualizację.

| Cel | Build i testy | Deployment | ServerPack |
| --- | --- | --- | --- |
| `all` | wszystkie pluginy | wszystkie pluginy | tak |
| `core` | LifestealCore | LifestealCore | tak |
| `scoreboard` | LifestealScoreboard | LifestealScoreboard | nie |
| `souls` | LifestealSouls | LifestealSouls | nie |
| `soulitems` | LifestealSoulItems | LifestealSoulItems | tak |
| `soulshop` | LifestealSoulShop | LifestealSoulShop | nie |
| `spawn` | LifestealSpawn | LifestealSpawn | nie |
| `homes` | LifestealHomes | LifestealHomes | nie |

`deploy.sh` wykonuje backup wybranych plików, atomowo podmienia JAR-y w katalogu
Pterodactyla i sprawdza rezultat. Dla celów korzystających z ServerPacka tworzy
również `build/ServerPack.zip`, publikuje go, aktualizuje SHA-1 w
`server.properties` i weryfikuje publiczny adres paczki.

Skryptów `update.sh` i `deploy.sh` nie uruchamia się przez `sudo`. Same proszą o
uprawnienia tylko dla operacji wymagających dostępu do Dockera, wolumenu
Pterodactyla lub katalogu publikowanego przez serwer WWW.

Pełna instrukcja znajduje się w [DEPLOY_README.md](DEPLOY_README.md), a skrócona
instrukcja buildu VPS w [BUILD_ON_VPS.md](BUILD_ON_VPS.md).

## ServerPack

`ServerPack/` przechowuje wyłącznie źródła resource packa. Archiwum ZIP jest
tworzone podczas deploymentu Core, SoulItems lub pełnego deploymentu i trafia
do `build/ServerPack.zip`.

Zmiana wyłącznie Scoreboardu, Souls, SoulShopu, Spawnu lub Homes nie przebudowuje ani
nie publikuje ServerPacka. Cel SoulItems publikuje paczkę, ponieważ zawiera ona
tekstury i modele customowych przedmiotów.

## Dodawanie kolejnego pluginu

Nowy plugin powinien zostać dodany jako osobny moduł:

1. Utwórz katalog modułu z `build.gradle` i standardowym układem `src/`.
2. Dodaj moduł do `settings.gradle` oraz wspólnego zadania w `build.gradle`.
3. Dodaj jego cel i oczekiwany JAR do skryptów buildu i deploymentu.
4. Dodaj ścieżki deploymentu do `deploy.env`, jeśli plugin jest wdrażany na VPS.
5. Utwórz `docs/plugins/<nazwa-pluginu>/README.md`.
6. Uruchom `./verify-source.sh` oraz pełny build.

`verify-source.sh` pilnuje między innymi, aby każdy deskryptor `plugin.yml` lub
`paper-plugin.yml` posiadał dedykowany README i aby wygenerowane artefakty nie
trafiły do drzewa źródeł.

Konfiguracja deploymentu przechowywana w repozytorium nie zawiera haseł,
tokenów ani innych sekretów.
