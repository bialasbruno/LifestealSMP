# LifestealCore

`LifestealCore` to plugin Paper odpowiedzialny za podstawową mechanikę serc na
serwerze LifestealSMP. Przechowuje liczbę serc graczy, obsługuje utratę serca po
śmierci PvP, tworzy przedmioty `Broken Heart` i `Heart`, rejestruje recepturę
craftingu oraz udostępnia komendy gracza i administratora.

## Informacje techniczne

| Pole | Wartość |
| --- | --- |
| Nazwa pluginu | `LifestealCore` |
| Wersja | `0.1.0` |
| Platforma | Paper `26.2`, build `112` |
| Java | `25` |
| Klasa główna | `dev.lifesteal.core.LifestealCorePlugin` |
| Baza danych | SQLite |
| Plik danych | `plugins/LifestealCore/data.db` |
| Finalny JAR | `build/libs/LifestealCore-0.1.0.jar` |

Plugin nie wymaga zewnętrznego serwera bazy danych. Sterownik SQLite znajduje
się wewnątrz finalnego JAR-a. Paper API jest dostarczane przez serwer i nie jest
dołączane do artefaktu.

## Mechanika rozgrywki

Domyślne zasady:

- nowy gracz zaczyna z `10` sercami;
- minimalna liczba serc wynosi `1`;
- maksymalna liczba serc wynosi `20`;
- jedno serce odpowiada `2` punktom maksymalnego zdrowia;
- śmierć przypisana przez Paper innemu graczowi odejmuje ofierze jedno serce;
- śmierć środowiskowa, samobójstwo lub śmierć bez rozpoznanego zabójcy nie
  zmienia liczby serc;
- zabójca nie otrzymuje serca bezpośrednio — przy ofierze wypada `Broken Heart`;
- gracz znajdujący się już na minimum nie traci kolejnego serca i nie upuszcza
  `Broken Heart`;
- użycie przedmiotu `Heart` zwiększa limit zdrowia gracza o jedno serce;
- po osiągnięciu maksimum `Heart` nie jest zużywany, a dźwięk użycia nie jest
  odtwarzany.

Zmiana liczby serc jest od razu nakładana na atrybut `MAX_HEALTH`. Jeśli gracz
ma w danej chwili więcej zdrowia niż nowy limit, bieżące zdrowie zostaje obcięte
do tego limitu.

## Przedmioty

### Broken Heart

- bazowy materiał: `GHAST_TEAR`;
- model ServerPacka: `serverpack:broken_heart`;
- posiada własną nazwę, lore i efekt połysku;
- jest oznaczony kluczem PDC `lifestealcore:broken_heart`;
- może wypaść po śmierci PvP i służy do craftingu pełnego serca.

### Heart

- bazowy materiał: `NETHER_STAR`;
- model ServerPacka: `serverpack:heart`;
- posiada własną nazwę, lore i efekt połysku;
- jest oznaczony kluczem PDC `lifestealcore:heart`;
- kliknięcie PPM trwale dodaje jedno maksymalne serce;
- po poprawnym użyciu odtwarza dźwięk `serverpack:heart_consume`.

Plugin rozpoznaje oba przedmioty po materiale i znaczniku PDC. Sama zmiana nazwy
zwykłego przedmiotu w kowadle nie pozwala podrobić `Broken Heart` ani `Heart`.

## Receptura Heart

```text
B D B
D D D
D D D
```

- `B` — prawdziwy `Broken Heart` ze znacznikiem PDC;
- `D` — Diamond;
- wynik — jeden `Heart`.

Receptura ma klucz `lifestealcore:heart_from_broken_hearts`. Wykorzystuje
`RecipeChoice.ExactChoice`, a dodatkowy listener ponownie sprawdza znaczniki PDC
w siatce craftingu. Dzięki temu podobny lub przemianowany przedmiot nie przejdzie
walidacji.

## Komendy

| Komenda | Uprawnienie | Dostęp | Działanie |
| --- | --- | --- | --- |
| `/hearts` | `lifesteal.hearts` | gracz | Pokazuje bieżącą i maksymalną liczbę serc. |
| `/lifesteal sethearts <player> <amount>` | `lifesteal.admin` | admin/console | Ustawia liczbę serc gracza w granicach konfiguracji. |
| `/lifesteal givebrokenheart <player> [amount]` | `lifesteal.admin` | admin/console | Daje od `1` do `64` przedmiotów `Broken Heart`; domyślnie jeden. |
| `/lifesteal giveheart <player> [amount]` | `lifesteal.admin` | admin/console | Daje od `1` do `64` przedmiotów `Heart`; domyślnie jeden. |

W wersji `0.1.0` wszystkie komendy administracyjne wymagają, aby wskazany gracz
był online. Komenda `/hearts` jest przeznaczona wyłącznie dla gracza. Komenda
`/lifesteal` podpowiada subkomendy oraz nazwy graczy online.

## Uprawnienia

| Uprawnienie | Domyślnie | Opis |
| --- | --- | --- |
| `lifesteal.hearts` | wszyscy gracze | Dostęp do `/hearts`. |
| `lifesteal.admin` | operatorzy | Dostęp do wszystkich subkomend `/lifesteal`. |

## Konfiguracja

Plugin tworzy plik `plugins/LifestealCore/config.yml` przy pierwszym
uruchomieniu.

```yaml
hearts:
  starting: 10
  minimum: 1
  maximum: 20

broken-heart:
  drop-on-pvp-death: true

messages:
  maximum-hearts: "You already have the maximum number of hearts."
```

| Klucz | Znaczenie |
| --- | --- |
| `hearts.starting` | Liczba serc przypisana graczowi bez rekordu w bazie. |
| `hearts.minimum` | Dolna granica serc; musi wynosić co najmniej `1`. |
| `hearts.maximum` | Górna granica serc; nie może być mniejsza od minimum. |
| `broken-heart.drop-on-pvp-death` | Włącza lub wyłącza drop `Broken Heart` po śmierci PvP. Utrata serca nadal działa. |
| `messages.maximum-hearts` | Wiadomość po próbie użycia `Heart` na maksymalnym limicie. |

`hearts.starting` musi znajdować się pomiędzy minimum i maksimum. Niepoprawne
granice zatrzymują inicjalizację pluginu z czytelnym błędem. Po zmianie
konfiguracji należy wykonać pełny restart serwera.

## Dane graczy

SQLite przechowuje tabelę `player_hearts`:

| Kolumna | Znaczenie |
| --- | --- |
| `player_uuid` | UUID gracza i klucz główny rekordu. |
| `last_known_name` | Ostatnia znana nazwa gracza; pole pomocnicze. |
| `hearts` | Trwała liczba maksymalnych serc. |

Podczas wejścia gracza plugin odczytuje jego rekord lub tworzy nowy z wartością
`hearts.starting`. Wartość spoza aktualnych granic konfiguracji zostaje
automatycznie przycięta i zapisana. W trakcie gry stan jest trzymany w pamięci,
a zapisy SQLite trafiają kolejno do pojedynczego wątku
`LifestealCore-Database`. Przy wyłączaniu plugin kończy kolejkę zapisów i
wykonuje końcowy zapis wszystkich załadowanych graczy.

Do backupu wystarczy skopiować `plugins/LifestealCore/data.db` przy zatrzymanym
serwerze.

## Obsługiwane zdarzenia

| Zdarzenie | Zachowanie |
| --- | --- |
| `PlayerJoinEvent` | Wczytuje lub tworzy stan serc i ustawia `MAX_HEALTH`. |
| `PlayerQuitEvent` | Usuwa stan gracza z pamięci; wcześniej zlecone zapisy nadal kończą się w kolejce SQLite. |
| `PlayerRespawnEvent` | Ponownie nakłada limit zdrowia w następnym ticku. |
| `PlayerDeathEvent` | Obsługuje utratę serca i opcjonalny drop po śmierci PvP. |
| `PlayerInteractEvent` | Zużywa prawdziwy `Heart` po PPM i zwiększa limit serc. |
| `PrepareItemCraftEvent` | Blokuje crafting z podrobionym `Broken Heart`. |

## ServerPack

Mechanika pluginu działa po stronie serwera, ale prawidłowy wygląd przedmiotów i
dźwięk użycia wymagają ServerPacka z katalogu `ServerPack/`. Pakiet dostarcza:

- modele `serverpack:broken_heart` i `serverpack:heart`;
- tekstury obu przedmiotów;
- dźwięk `serverpack:heart_consume`.

Skrypt `deploy.sh` buduje `build/ServerPack.zip`, publikuje go i aktualizuje
wpisy resource packa w `server.properties`.

## Instalacja

1. Zbuduj projekt poleceniem `./gradlew clean build` na Javie 25 albo użyj
   `./build-vps.sh` na hoście z Dockerem.
2. Skopiuj `build/libs/LifestealCore-0.1.0.jar` do katalogu `plugins/` serwera
   Paper.
3. Uruchom serwer, aby wygenerować `config.yml` i `data.db`.
4. Skonfiguruj oraz opublikuj ServerPack.
5. Po zmianach konfiguracji lub JAR-a wykonaj pełny restart serwera.

Na skonfigurowanym VPS-ie cały proces wykonuje `./deploy.sh`. Szczegóły znajdują
się w [`DEPLOY_README.md`](../../../DEPLOY_README.md).

## Build i testy

Lokalnie:

```bash
./gradlew clean build
```

Na VPS-ie:

```bash
./build-vps.sh
```

Pełny build kompiluje kod, uruchamia testy JUnit i tworzy dwa artefakty:

- `LifestealCore-0.1.0.jar` — finalny JAR z dołączonym SQLite;
- `LifestealCore-0.1.0-plain.jar` — JAR bez zależności, nieprzeznaczony do
  instalacji na serwerze.

Dodatkową kontrolę struktury źródeł można uruchomić przez:

```bash
./verify-source.sh
```

## Struktura kodu

| Pakiet | Odpowiedzialność |
| --- | --- |
| `dev.lifesteal.core` | Cykl życia pluginu i rejestracja komponentów. |
| `dev.lifesteal.core.command` | Komendy gracza i administratora. |
| `dev.lifesteal.core.config` | Walidowany, niemutowalny widok konfiguracji. |
| `dev.lifesteal.core.data` | Repozytorium danych graczy i implementacja SQLite. |
| `dev.lifesteal.core.heart` | Zasady serc, przedmioty, receptura i serwis domenowy. |
| `dev.lifesteal.core.listener` | Obsługa zdarzeń Paper. |

## Zakres wersji 0.1.0

Plugin celowo nie zawiera jeszcze ekonomii, klanów, revive, bounty, GUI,
eliminacji lub bana przy zerowej liczbie serc ani obsługi administracyjnej
graczy offline. Minimalna liczba serc zawsze wynosi co najmniej jeden.

[Powrót do README projektu](../../../README.md)
