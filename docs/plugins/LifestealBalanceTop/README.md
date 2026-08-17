# LifestealBalanceTop

`LifestealBalanceTop` zastępuje tekstowy ranking pieniędzy EssentialsX własnym,
czytelnym GUI otwieranym przez `/baltop`. Plugin korzysta z ekonomii
VaultUnlocked, dlatego pokazuje dokładnie tę samą zwykłą walutę co `/bal`,
`/pay`, EconomyShopGUI oraz linia `Balance` w LifestealScoreboard.

Souls nie są częścią tego rankingu. Ich osobny ranking nadal otwiera
`/soulstop`.

## Funkcje

- maksymalnie 100 najbogatszych znanych graczy,
- 45 głów graczy na stronie,
- trzy strony dla pełnego rankingu,
- domyślne sortowanie od największego do najmniejszego salda,
- przełączane sortowanie: saldo malejąco, saldo rosnąco, nazwa A-Z i nazwa Z-A,
- stały numer miejsca w TOP 100 niezależnie od wybranego widoku,
- przyciski poprzedniej i następnej strony,
- ręczne odświeżenie rankingu,
- automatyczny cache ograniczający częste skanowanie kont,
- przejęcie komend EssentialsX `/baltop`, `/balancetop`, `/ebaltop` oraz
  `/ebalancetop`, także w wariancie namespaced.

GUI nie pozwala zabierać przedmiotów ani wkładać własnych itemów. Wszystkie
kliknięcia i przeciąganie w jego obrębie są blokowane.

## Komendy i uprawnienia

| Komenda | Działanie | Uprawnienie | Domyślnie |
| --- | --- | --- | --- |
| `/baltop` | Otwiera GUI TOP 100 | `lifestealbalancetop.use` | każdy |
| `/balancetop` | Alias `/baltop` | `lifestealbalancetop.use` | każdy |

Plugin przechwytuje komendę gracza przed jej wykonaniem, dzięki czemu nowe GUI
otwiera się nawet wtedy, gdy EssentialsX zarejestrował `/baltop` wcześniej.

Po wdrożeniu można usunąć stare uprawnienie EssentialsX z grupy `default`:

```text
lp group default permission unset essentials.balancetop
```

Nie jest ono wymagane przez nowy ranking. Dostęp zapewnia domyślnie
`lifestealbalancetop.use`.

## Układ GUI

- sloty `0-44`: gracze z bieżącej strony,
- slot `45`: poprzednia strona,
- slot `47`: natychmiastowe odświeżenie sald,
- slot `48`: numer strony i liczba zajętych miejsc,
- slot `49`: zmiana sposobu sortowania,
- slot `51`: zamknięcie GUI,
- slot `53`: następna strona.

Każda głowa pokazuje nazwę, saldo oraz faktyczną pozycję gracza w TOP 100. Nie
ma specjalnego podium ani innego rozmieszczenia pierwszej trójki.

## Konfiguracja

Plik `plugins/LifestealBalanceTop/config.yml`:

```yaml
leaderboard:
  cache-seconds: 30
  include-zero-balances: false

messages:
  no-permission: "<red>You do not have permission to use this command.</red>"
  player-only: "<red>This command can only be used by a player.</red>"
  economy-unavailable: "<red>The economy is currently unavailable. Please try again later.</red>"
  usage: "<red>Usage: /baltop</red>"
```

`cache-seconds` określa, przez ile sekund gotowe TOP 100 może być używane bez
ponownego odczytywania wszystkich kont. Przycisk zegara zawsze wymusza
odświeżenie. `include-zero-balances: false` ukrywa konta z zerowym lub ujemnym
saldem.

Limit 100 oraz 45 pozycji na stronę są celowymi zasadami pluginu i nie podlegają
zmianie w konfiguracji.

## Zależności

- Paper `26.2`,
- Java `25`,
- VaultUnlocked (na liście pluginów i w zależnościach widoczny jako `Vault`),
- aktywny provider ekonomii, obecnie EssentialsX Economy.

VaultUnlocked zachowuje nazwę pluginu `Vault` dla kompatybilności ze starszymi
integracjami. Dlatego `plugin.yml` poprawnie wymaga `Vault`, mimo że używany
plik JAR pochodzi z projektu VaultUnlocked.

Gdy VaultUnlocked działa, ale nie ma aktywnego providera ekonomii, plugin nie
wyświetla nieprawdziwych zer. Zamyka GUI i pokazuje komunikat o niedostępnej
ekonomii.

## Build i deployment

Lokalny build z testami:

```bash
./gradlew :LifestealBalanceTop:clean :LifestealBalanceTop:build
```

Aktualizacja na VPS-ie:

```bash
./update.sh balancetop
```

Po podmianie JAR-a wymagany jest pełny restart serwera.
