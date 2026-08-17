# LifestealSMP

Główne repozytorium projektu serwera Minecraft Lifesteal SMP. Zawiera plugin
`LifestealCore`, `LifestealScoreboard`, źródła ServerPacka, testy oraz skrypty
służące do budowania i wdrażania projektu na VPS-ie z Pterodactylem.

Projekt jest aktywnie rozwijany. Obecny zakres `v0.2` obejmuje rdzeń Lifesteal,
czasowe eliminacje oraz rzadki, sezonowy system revive. Ekonomia, klany, bounty
i GUI pozostają poza bieżącym zakresem.

## Pluginy

Każdy plugin ma własny README z pełnym opisem mechaniki, konfiguracji, komend,
uprawnień, danych, instalacji i budowania.

| Plugin | Wersja | Opis | Dokumentacja |
| --- | --- | --- | --- |
| `LifestealCore` | `0.2.1` | Serca, śmierci PvP, eliminacje, Revive Totem, crafting, SQLite i publiczne API serc. | [README pluginu](docs/plugins/LifestealCore/README.md) |
| `LifestealScoreboard` | `0.1.0` | Flicker-free sidebar, placeholdery, statystyki oraz integracja z Core i PlaceholderAPI. | [README pluginu](docs/plugins/LifestealScoreboard/README.md) |

## Wymagania

- Paper `26.2`, build `112`
- Java `25`
- Gradle `9.7.0`
- SQLite (sterownik jest dołączany do finalnego JAR-a)
- VPS: Ubuntu `24.04`, Docker, Pterodactyl + Wings

Build VPS-a używa oficjalnego obrazu `gradle:jdk25-noble`, więc host nie musi
mieć lokalnie zainstalowanej Javy ani Gradle.

## Zasady Lifesteal

- Gracz zaczyna z `10` sercami.
- Minimum to `1` serce, maksimum to `20` serc.
- Śmierć PvP odejmuje ofierze jedno maksymalne serce.
- Jeśli ofiara miała więcej niż jedno serce, wypada prawdziwy `Broken Heart`.
- Śmierć PvP przy jednym sercu powoduje eliminację i ban na `24` godziny.
- Po naturalnym wygaśnięciu bana gracz wraca z `3` sercami.
- Gracz zabity przy maksymalnej liczbie serc może upuścić jeden `Revive Totem`
  na sezon.
- `/revive <player>` zużywa trzymany totem, natychmiast odbanowuje cel i ustawia
  mu `10` serc.
- Customowy Revive Totem nie działa jak zwykły Totem of Undying i nie może ominąć
  eliminacji właściciela.
- Śmierci inne niż PvP nie zmieniają liczby serc.
- Serce gracza jest zapisywane po UUID w `plugins/LifestealCore/data.db`.

### Craft Heart

```text
B D B
D D D
D D D
```

`B` oznacza prawdziwy `Broken Heart` oznaczony przez PDC, a `D` oznacza Diamond.
PPM z gotowym `Heart` daje `+1` maksymalne serce (`+2 HP`). Przy limicie `20`
przedmiot nie jest zużywany i customowy dźwięk nie jest odtwarzany.

## Komendy i permissions

| Komenda | Permission | Opis |
| --- | --- | --- |
| `/hearts` | `lifesteal.hearts` | Pokazuje aktualną liczbę maksymalnych serc. |
| `/lifesteal sethearts <player> <amount>` | `lifesteal.admin` | Ustawia serca gracza online. |
| `/lifesteal givebrokenheart <player> [amount]` | `lifesteal.admin` | Daje Broken Heart. |
| `/lifesteal giveheart <player> [amount]` | `lifesteal.admin` | Daje gotowy Heart. |
| `/lifesteal giverevivetotem <player>` | `lifesteal.admin` | Daje jeden Revive Totem do testów lub administracji. |
| `/revive <player>` | `lifesteal.revive` | Zużywa trzymany Revive Totem i przywraca wyeliminowanego gracza. |

`lifesteal.hearts` i `lifesteal.revive` są domyślnie dostępne dla graczy, a
`lifesteal.admin` dla operatorów.

## Build i testy

Lokalnie, z Java 25:

```bash
./gradlew clean build
```

Na VPS-ie z Dockerem:

```bash
./build-vps.sh
```

Obie ścieżki uruchamiają testy JUnit obu pluginów. Finalne JAR-y powstają jako:

```text
LifestealCore/build/libs/LifestealCore-0.2.1.jar
LifestealScoreboard/build/libs/LifestealScoreboard-0.1.0.jar
```

Pakiet `org.sqlite` celowo nie jest relokowany. Relokacja psuje powiązanie z
natywną biblioteką SQLite i prowadzi do `UnsatisfiedLinkError`.

## ServerPack

Katalog `ServerPack/` zawiera źródła, a nie wygenerowany ZIP. Zapewnia modele:

- `serverpack:broken_heart`
- `serverpack:heart`
- `serverpack:revive_totem`

oraz dźwięk:

- `serverpack:heart_consume`

`deploy.sh` automatycznie tworzy `build/ServerPack.zip`. Wygenerowany ZIP nie
jest commitowany.

## Deployment

Pełny opis znajduje się w [DEPLOY_README.md](DEPLOY_README.md). Standardowa
aktualizacja na VPS-ie:

```bash
cd ~/LifestealCore
./update.sh
```

`update.sh` wykonuje `git pull --ff-only`, a następnie `deploy.sh`. Skryptu nie
uruchamia się przez `sudo`; sam korzysta z `sudo` wyłącznie tam, gdzie wymaga
tego Docker, Pterodactyl lub katalog publikowany przez Nginx.

## Struktura

```text
LifestealSMP/
├── LifestealCore/       # plugin Core, konfiguracja Gradle i testy
├── LifestealScoreboard/ # plugin Scoreboard, konfiguracja Gradle i testy
├── gradle/              # Gradle Wrapper
├── ServerPack/          # źródła resource packa
├── docs/plugins/        # osobny README dla każdego pluginu
├── build.gradle         # wspólny build wszystkich modułów
├── settings.gradle
├── build-vps.sh
├── deploy.sh
├── update.sh
├── deploy.env
├── README.md
└── DEPLOY_README.md
```

Konfiguracja deploymentu nie zawiera haseł, tokenów ani innych sekretów.
