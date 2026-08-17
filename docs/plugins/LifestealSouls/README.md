# LifestealSouls

`LifestealSouls` to niezależny plugin Paper zarządzający przypisaną do gracza
walutą `Souls`. Waluty nie można przelewać ani przekazywać innym graczom.

Plugin odpowiada wyłącznie za saldo, sposoby zarabiania, trwałość danych,
historię transakcji i publiczne API. Sklep z customowymi przedmiotami powinien
powstać jako osobny moduł, np. `LifestealSoulItems`, korzystający z API Souls.

## Wymagania

| Element | Wartość |
| --- | --- |
| Platforma | Paper `26.2`, build `112` |
| Java | `25` |
| Wersja pluginu | `0.1.0` |
| Baza danych | SQLite w `plugins/LifestealSouls/data.db` |
| Opcjonalna integracja | LifestealScoreboard |
| Finalny JAR | `LifestealSouls/build/libs/LifestealSouls-0.1.0.jar` |

Sterownik SQLite jest dołączony do produkcyjnego JAR-a.

## Saldo

- Saldo jest przypisane do UUID gracza.
- Nie istnieje komenda `/souls pay` ani inne API do przelewów między graczami.
- Saldo nie może spaść poniżej zera.
- Domyślny limit salda wynosi `1 000 000 000` Souls.
- Każda zmiana salda jest zapisywana w historii wraz ze źródłem, wartością po
  operacji, czasem i opcjonalnym identyfikatorem.
- Informacje o saldzie i zdobytych Souls pojawiają się przez `2` sekundy nad
  hotbarem zamiast zajmować miejsce na chacie.

## Nagroda za aktywny playtime

Gracz otrzymuje jednorazowo `50 Souls` po zgromadzeniu pełnej godziny aktywnej
gry. Plugin nie wypłaca `1 Soul` co 72 sekundy.

Postęp:

- jest liczony tylko, kiedy gracz pozostaje online i jest aktywny,
- zostaje wstrzymany po domyślnie `5` minutach bez ruchu lub interakcji,
- jest zapisywany okresowo i przy wyjściu gracza,
- nie zeruje się po wyjściu ani restarcie serwera,
- wypłaca pełne paczki po przekroczeniu kolejnych godzin.

Aktywność odświeżają między innymi ruch, interakcje, używanie przedmiotów,
niszczenie i stawianie bloków, klikanie w ekwipunku oraz komendy.

## Nagroda za zabójstwo

- Kwalifikujące się zabójstwo gracza daje zabójcy `3 Souls`.
- Ponowne zabicie tej samej ofiary przez tego samego zabójcę nie daje nagrody
  przez `1` godzinę.
- Cooldown jest kierunkowy: zapis `A → B` nie blokuje nagrody za `B → A`.
- Cooldown jest zapisany w SQLite i pozostaje aktywny po restarcie.
- Śmierci bez przypisanego zabójcy nie dają Souls.

## Strefa AFK

Stawka wynosi domyślnie `1 Soul` co `2` minuty ciągłego pobytu w dedykowanej
strefie AFK. Strefa jest cuboidem obejmującym obie graniczne pozycje bloków.

Po wejściu gracz widzi nad hotbarem interaktywny licznik, np. `02:00`, `01:59`,
`01:58`. Po dojściu do zera plugin wypłaca nagrodę, pokazuje komunikat i od razu
rozpoczyna następne odliczanie. Cykl trwa tak długo, jak gracz pozostaje w
strefie.

Komenda `/afk` teleportuje gracza dokładnie na geometryczny środek cuboidu,
z zachowaniem kierunku, w którym patrzył. Jeżeli inny plugin zajmie nazwę
`/afk`, dostępny jest również alias `/soulafk` oraz namespaced command
`/lifestealsouls:afk`.

PvP jest domyślnie wyłączone dla każdej walki, w której atakujący albo ofiara
znajduje się w strefie. Ochrona obejmuje bezpośrednie uderzenia, pociski gracza
(w tym strzały i trójzęby), oswojone moby oraz TNT odpalone przez gracza. Dzięki
sprawdzaniu obu stron nie można również stać wewnątrz strefy i atakować gracza
znajdującego się za jej granicą.

Opuszczenie strefy, wyjście z serwera lub restart zeruje nieukończone
odliczanie. Pobyt w strefie AFK nie nalicza jednocześnie aktywnego playtime.
Powiadomienia o saldzie, zabójstwie lub wypłacie mają na action barze priorytet
przez `2` sekundy, a następnie licznik wraca automatycznie.

Zwykłe stanie bezczynnie poza strefą nie nalicza aktywnego playtime po
upływie `idle-timeout-seconds`.

## Zakupy internetowe

Sklep powinien wykonywać z konsoli serwera:

```text
/soulsadmin purchase <UUID> <amount> <transaction-id>
```

`transaction-id` musi być unikalnym identyfikatorem zamówienia ze sklepu.
Ponowienie dokładnie tej samej transakcji nie doda waluty drugi raz. Próba użycia
tego samego identyfikatora dla innego UUID lub innej kwoty zostanie odrzucona.

Komenda działa również dla gracza offline. Zakupy mogą być stosowane wyłącznie
przez konsolę, nie przez gracza.

## Komendy

| Komenda | Opis |
| --- | --- |
| `/afk` | Teleportuje na środek skonfigurowanej strefy AFK. |
| `/souls` | Pokazuje własne saldo nad hotbarem przez 2 sekundy. |
| `/souls top` | Otwiera GUI z 10 najwyższymi saldami. |
| `/soulsadmin balance <player\|uuid>` | Pokazuje saldo gracza. |
| `/soulsadmin add <player\|uuid> <amount>` | Dodaje Souls administracyjnie. |
| `/soulsadmin remove <player\|uuid> <amount>` | Usuwa Souls bez zejścia poniżej zera. |
| `/soulsadmin set <player\|uuid> <balance>` | Ustawia dokładne saldo. |
| `/soulsadmin purchase <uuid> <amount> <transaction-id>` | Stosuje zakup ze sklepu; tylko konsola. |
| `/soulsadmin history <player\|uuid> [limit]` | Pokazuje ostatnie transakcje, maksymalnie 100. |
| `/soulsadmin reload` | Przeładowuje konfigurację nagród i wiadomości. |

## Uprawnienia

| Permission | Domyślnie | Opis |
| --- | --- | --- |
| `lifestealsouls.afk` | każdy gracz | Pozwala teleportować się do strefy AFK. |
| `lifestealsouls.balance` | każdy gracz | Pozwala używać `/souls`. |
| `lifestealsouls.admin` | operator | Pozwala zarządzać saldami i historią. |

Nie istnieje permission ani komenda umożliwiająca przelew waluty.

## Konfiguracja

Najważniejsze wartości w `plugins/LifestealSouls/config.yml`:

```yaml
balance:
  maximum: 1000000000

playtime:
  enabled: true
  reward-amount: 50
  reward-interval-seconds: 3600
  idle-timeout-seconds: 300
  flush-interval-seconds: 60

player-kill:
  enabled: true
  reward-amount: 3
  same-victim-cooldown-seconds: 3600

afk-zone:
  enabled: false
  disable-pvp: true
  reward-amount: 1
  reward-interval-seconds: 120
  world: ""
  minimum:
    x: 0
    y: 0
    z: 0
  maximum:
    x: 0
    y: 0
    z: 0
```

Aby uruchomić strefę, wpisz nazwę świata, ustaw współrzędne dwóch przeciwległych
narożników, zmień `enabled` na `true` i wykonaj `/soulsadmin reload`. Kolejność
narożników nie ma znaczenia — plugin sam normalizuje wartości minimalne i
maksymalne. Pusta wartość `world` bezpiecznie blokuje naliczanie nagród.

Wiadomości obsługują MiniMessage oraz placeholdery `{balance}`, `{amount}` i
`{victim}` zależnie od komunikatu. Licznik `afk-countdown` obsługuje dodatkowo
`{time}`. Szablony `balance`, `playtime-reward`, `kill-reward` i `afk-reward`
są wyświetlane jako action bar przez `40` ticków, czyli `2` sekundy.

## GUI rankingu

Komenda `/souls top` otwiera ekwipunek tylko do odczytu o rozmiarze trzech
rzędów. Pierwsze trzy miejsca tworzą podium, a miejsca `4–10` znajdują się w
drugim rzędzie. Każda pozycja używa głowy gracza i pokazuje jego aktualne saldo.

Klikanie oraz przeciąganie przedmiotów w otwartym rankingu jest blokowane. Jeśli
żaden gracz nie zdobył jeszcze Souls, GUI pokazuje informację o pustym rankingu.

## Dane i odporność na duplikację

Baza zawiera:

- konta i postęp aktywnego playtime,
- pełną historię zmian salda,
- nagrody odebrane w strefie AFK,
- cooldowny par zabójca–ofiara,
- wykorzystane identyfikatory zakupów internetowych.

Zmiany salda, wpis historii, cooldown i identyfikator zakupu są zapisywane w
transakcjach SQLite. Awaria pojedynczej operacji wycofuje cały jej zapis.

## Integracja ze Scoreboardem

Jeżeli `LifestealScoreboard` jest włączony, Souls automatycznie rejestruje
provider waluty przez Bukkit Services. Istniejący placeholder:

```text
%lifesteal_souls%
```

zaczyna wtedy pokazywać saldo z `LifestealSouls`. Integracja jest opcjonalna;
brak Scoreboardu nie blokuje działania waluty.

## API dla LifestealSoulItems

Plugin rejestruje w Bukkit Services interfejs:

```java
LifestealSoulsApi
```

Udostępnia on:

- `getSouls(UUID)` — odczyt salda,
- `trySpend(UUID, amount, reason)` — atomowy zakup bez możliwości ujemnego
  salda.

Przyszły `LifestealSoulItems` powinien mieć twardą zależność od
`LifestealSouls`, odczytywać API z Bukkit Services i wydawać przedmiot dopiero
po udanym `trySpend`.

## Build i instalacja

Build tylko tego modułu:

```bash
./gradlew :LifestealSouls:clean :LifestealSouls:build
```

Na VPS-ie:

```bash
./build-vps.sh souls
./update.sh souls
```

Do katalogu `plugins/` należy wgrać produkcyjny Shadow JAR bez sufiksu
`-plain`. Po instalacji lub aktualizacji wymagany jest pełny restart serwera.
