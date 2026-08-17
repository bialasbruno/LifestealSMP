# LifestealCore

`LifestealCore` to plugin Paper odpowiedzialny za podstawową mechanikę serc na
serwerze LifestealSMP. Przechowuje liczbę serc graczy, obsługuje śmierci PvP,
czasowe eliminacje i sezonowe odrodzenia, tworzy przedmioty `Broken Heart`,
`Heart` i `Revive Totem` oraz udostępnia komendy gracza i administratora.

## Informacje techniczne

| Pole | Wartość |
| --- | --- |
| Nazwa pluginu | `LifestealCore` |
| Wersja | `0.2.1` |
| Platforma | Paper `26.2`, build `112` |
| Java | `25` |
| Klasa główna | `dev.lifesteal.core.LifestealCorePlugin` |
| Baza danych | SQLite |
| Plik danych | `plugins/LifestealCore/data.db` |
| Finalny JAR | `build/libs/LifestealCore-0.2.1.jar` |

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
- śmierć PvP przy jednym sercu nie upuszcza `Broken Heart`, tylko eliminuje
  gracza i nakłada profilowy ban na `24` godziny;
- po naturalnym wygaśnięciu bana gracz wraca z `3` sercami;
- gracz zabity przy maksymalnej liczbie serc może upuścić jeden pełny
  `Revive Totem` na każde unikalne `season.id`;
- revive lub kolejne osiągnięcie maksimum nie resetuje sezonowego dropu;
- użycie `Revive Totem` natychmiast odbanowuje wyeliminowanego gracza i ustawia
  mu `10` serc;
- użycie przedmiotu `Heart` zwiększa limit zdrowia gracza o jedno serce;
- po osiągnięciu maksimum `Heart` nie jest zużywany, a dźwięk użycia nie jest
  odtwarzany.

Zmiana liczby serc jest od razu nakładana na atrybut `MAX_HEALTH`. Jeśli gracz
ma w danej chwili więcej zdrowia niż nowy limit, bieżące zdrowie zostaje obcięte
do tego limitu.

## Przedmioty

`Broken Heart`, `Heart` i `Revive Totem` są własnymi przedmiotami pluginu.
Mają unikalne modele z ServerPacka, nazwy, lore oraz chronione znaczniki PDC.
Minecraft wymaga jednak przypisania każdemu itemowi istniejącego materiału, dlatego
plugin używa vanilla materiałów wyłącznie jako niewidocznych technicznych nośników.
Zwykły `Ghast Tear`, `Nether Star` ani `Totem of Undying` nie jest traktowany jak
customowy przedmiot LifestealCore.

### Broken Heart

- własny przedmiot pluginu, wyświetlany jako customowe pęknięte serce;
- techniczny materiał nośny: `GHAST_TEAR`;
- model ServerPacka: `serverpack:broken_heart`;
- posiada własną nazwę, lore i efekt połysku;
- jest oznaczony kluczem PDC `lifestealcore:broken_heart`;
- może wypaść po śmierci PvP i służy do craftingu pełnego serca.

### Heart

- własny przedmiot pluginu, wyświetlany jako customowe pełne serce;
- techniczny materiał nośny: `NETHER_STAR`;
- model ServerPacka: `serverpack:heart`;
- posiada własną nazwę, lore i efekt połysku;
- jest oznaczony kluczem PDC `lifestealcore:heart`;
- kliknięcie PPM trwale dodaje jedno maksymalne serce;
- po poprawnym użyciu odtwarza dźwięk `serverpack:heart_consume`.

### Revive Totem

- własny przedmiot pluginu z dedykowaną grafiką;
- techniczny materiał nośny: `TOTEM_OF_UNDYING`;
- model ServerPacka: `serverpack:revive_totem`;
- jest oznaczony kluczem PDC `lifestealcore:revive_totem`;
- wypada dodatkowo przy pierwszej w sezonie śmierci PvP danego gracza, jeśli
  przed śmiercią posiadał maksymalną liczbę serc;
- podczas użycia musi znajdować się w głównej lub drugiej ręce;
- `/revive <player>` zużywa go dopiero po udanym odbanowaniu celu.
- nie uruchamia vanilla mechaniki Totem of Undying — służy wyłącznie do `/revive`.

Plugin rozpoznaje wszystkie trzy przedmioty po materiale i znaczniku PDC. Sama
zmiana nazwy zwykłego przedmiotu w kowadle nie pozwala ich podrobić.

## Eliminacja i revive

Eliminacja dotyczy wyłącznie poprawnie przypisanej śmierci PvP. Przy jednym
sercu plugin zapisuje w SQLite UUID, ostatnią nazwę oraz dokładny czas końca
bana, dodaje czasowy ban profilu Paper i wyrzuca gracza z serwera.

Plugin co minutę przywraca wygasłe eliminacje. Jeżeli serwer był wyłączony w
chwili wygaśnięcia, stan zostaje naprawiony po uruchomieniu lub przy pierwszym
wejściu gracza. Naturalny powrót ustawia `3` serca. Udany `/revive` usuwa ban
natychmiast i ustawia `10` serc. Obie wartości są konfigurowalne i ograniczane do
aktualnego minimum oraz maksimum.

Sezonowy drop jest zapisywany przed utworzeniem przedmiotu jako unikalna para
`season_id + victim_uuid`. Restart, zmiana nicku, inny zabójca ani revive nie
pozwalają uzyskać drugiego totemu w tym samym sezonie.

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
| `/lifesteal giverevivetotem <player>` | `lifesteal.admin` | admin/console | Daje jeden `Revive Totem` do administracji lub testów. |
| `/revive <player>` | `lifesteal.revive` | gracz z totemem | Przywraca aktywnie wyeliminowanego gracza z konfigurowaną liczbą serc. |

W wersji `0.2.1` wszystkie komendy administracyjne wymagają, aby wskazany gracz
był online. Komenda `/hearts` jest przeznaczona wyłącznie dla gracza. Komenda
`/lifesteal` podpowiada subkomendy oraz nazwy graczy online. `/revive` podpowiada
nazwy graczy z aktywną eliminacją.

## Uprawnienia

| Uprawnienie | Domyślnie | Opis |
| --- | --- | --- |
| `lifesteal.hearts` | wszyscy gracze | Dostęp do `/hearts`. |
| `lifesteal.admin` | operatorzy | Dostęp do wszystkich subkomend `/lifesteal`. |
| `lifesteal.revive` | wszyscy gracze | Dostęp do `/revive`; wymagany jest prawdziwy Revive Totem. |

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

season:
  id: "1"

elimination:
  ban-duration-hours: 24
  return-hearts: 3
  ban-reason: "You were eliminated with one heart. You can return in {hours} hours with {return_hearts} hearts or be revived."

revive:
  return-hearts: 10

messages:
  maximum-hearts: "You already have the maximum number of hearts."
```

| Klucz | Znaczenie |
| --- | --- |
| `hearts.starting` | Liczba serc przypisana graczowi bez rekordu w bazie. |
| `hearts.minimum` | Dolna granica serc; musi wynosić co najmniej `1`. |
| `hearts.maximum` | Górna granica serc; nie może być mniejsza od minimum. |
| `broken-heart.drop-on-pvp-death` | Włącza lub wyłącza drop `Broken Heart` po śmierci PvP. Utrata serca nadal działa. |
| `season.id` | Identyfikator sezonu. Zmiana wartości ponownie pozwala każdemu UUID upuścić jeden Revive Totem. |
| `elimination.ban-duration-hours` | Długość bana po śmierci PvP przy jednym sercu. |
| `elimination.return-hearts` | Liczba serc po naturalnym wygaśnięciu bana. |
| `elimination.ban-reason` | Powód bana; obsługuje `{hours}` i `{return_hearts}`. |
| `revive.return-hearts` | Liczba serc po użyciu Revive Totem. |
| `messages.maximum-hearts` | Wiadomość po próbie użycia `Heart` na maksymalnym limicie. |

`hearts.starting` musi znajdować się pomiędzy minimum i maksimum. Niepoprawne
granice zatrzymują inicjalizację pluginu z czytelnym błędem. Po zmianie
konfiguracji należy wykonać pełny restart serwera.

## Dane graczy

SQLite przechowuje trzy tabele:

### `player_hearts`

| Kolumna | Znaczenie |
| --- | --- |
| `player_uuid` | UUID gracza i klucz główny rekordu. |
| `last_known_name` | Ostatnia znana nazwa gracza; pole pomocnicze. |
| `hearts` | Trwała liczba maksymalnych serc. |

### `player_eliminations`

| Kolumna | Znaczenie |
| --- | --- |
| `player_uuid` | UUID wyeliminowanego gracza. |
| `last_known_name` | Nazwa używana przez `/revive` i podpowiedzi komendy. |
| `banned_until` | Czas wygaśnięcia bana zapisany jako Unix epoch milliseconds. |

### `revive_totem_drops`

| Kolumna | Znaczenie |
| --- | --- |
| `season_id` | Identyfikator sezonu z konfiguracji. |
| `victim_uuid` | UUID maksymalnego gracza, który upuścił totem. |
| `killer_uuid` | UUID zabójcy zapisany do historii. |
| `dropped_at` | Czas przyznania sezonowego dropu. |

Klucz główny `season_id + victim_uuid` wymusza maksymalnie jeden drop totemu na
gracza i sezon bez polegania na pamięci procesu.

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
| `PlayerJoinEvent` | Naprawia wygasłą eliminację, następnie wczytuje serca i ustawia `MAX_HEALTH`. |
| `PlayerQuitEvent` | Usuwa stan gracza z pamięci; wcześniej zlecone zapisy nadal kończą się w kolejce SQLite. |
| `PlayerRespawnEvent` | Ponownie nakłada limit zdrowia w następnym ticku. |
| `PlayerDeathEvent` | Obsługuje utratę serca, Broken Heart, sezonowy Revive Totem oraz eliminację. |
| `PlayerInteractEvent` | Zużywa prawdziwy `Heart` po PPM i zwiększa limit serc. |
| `PrepareItemCraftEvent` | Blokuje crafting z podrobionym `Broken Heart`. |

## ServerPack

Mechanika pluginu działa po stronie serwera, ale prawidłowy wygląd przedmiotów i
dźwięk użycia wymagają ServerPacka z katalogu `ServerPack/`. Pakiet dostarcza:

- modele `serverpack:broken_heart`, `serverpack:heart` i
  `serverpack:revive_totem`;
- tekstury wszystkich trzech przedmiotów;
- dźwięk `serverpack:heart_consume`.

Skrypt `deploy.sh` buduje `build/ServerPack.zip`, publikuje go i aktualizuje
wpisy resource packa w `server.properties`.

## Instalacja

1. Zbuduj projekt poleceniem `./gradlew clean build` na Javie 25 albo użyj
   `./build-vps.sh` na hoście z Dockerem.
2. Skopiuj `build/libs/LifestealCore-0.2.1.jar` do katalogu `plugins/` serwera
   Paper jako `LifestealCore.jar`.
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

- `LifestealCore-0.2.1.jar` — finalny JAR z dołączonym SQLite;
- `LifestealCore-0.2.1-plain.jar` — JAR bez zależności, nieprzeznaczony do
  instalacji na serwerze.

Dodatkową kontrolę struktury źródeł można uruchomić przez:

```bash
./verify-source.sh
```

## Struktura kodu

| Pakiet | Odpowiedzialność |
| --- | --- |
| `dev.lifesteal.core` | Cykl życia pluginu i rejestracja komponentów. |
| `dev.lifesteal.core.api` | Publiczny, tylko do odczytu kontrakt serc dla innych pluginów. |
| `dev.lifesteal.core.command` | Komendy gracza i administratora. |
| `dev.lifesteal.core.config` | Walidowany, niemutowalny widok konfiguracji. |
| `dev.lifesteal.core.data` | Repozytorium danych graczy i implementacja SQLite. |
| `dev.lifesteal.core.elimination` | Bany czasowe, sezonowe dropy i przywracanie graczy. |
| `dev.lifesteal.core.heart` | Zasady serc, przedmioty, receptura i serwis domenowy. |
| `dev.lifesteal.core.listener` | Obsługa zdarzeń Paper. |

## Publiczne API

`LifestealCorePlugin` implementuje `LifestealCoreApi`. Metoda
`getHearts(UUID)` zwraca aktualny limit zdrowia gracza w sercach, a nie w raw HP.
API korzysta z tej samej pamięci podręcznej co mechanika gameplayowa i nie wykonuje
osobnych zapytań do SQLite. `LifestealScoreboard` używa wyłącznie tego kontraktu.

## Zakres wersji 0.2.1

Plugin zawiera czasową eliminację przy jednym sercu oraz wcześniejszy powrót
przez rzadki Revive Totem. Celowo nie zawiera jeszcze ekonomii, klanów, bounty,
GUI ani ogólnej obsługi administracyjnej graczy offline. Minimalna liczba serc
w aktywnym stanie zawsze wynosi co najmniej jeden.

[Powrót do README projektu](../../../README.md)
