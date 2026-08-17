# LifestealSell

`LifestealSell` zapewnia trzy proste warianty komendy sprzedaży i korzysta z
cen, uprawnień oraz ekonomii skonfigurowanych w EconomyShopGUI. Plugin nie
posiada własnego cennika ani własnego salda.

## Komendy

| Komenda | Działanie |
| --- | --- |
| `/sell` | Otwiera GUI sprzedaży EconomyShopGUI. |
| `/sell hand` | Sprzedaje wszystkie przedmioty materiału trzymanego w głównej ręce. |
| `/sell help` | Pokazuje angielski opis dostępnych komend. |

`/sell hand` przekazuje materiał przedmiotu do `/sellall <material>` z
EconomyShopGUI, ale na czas transakcji zabezpiecza warianty o innych nazwach,
enchantach lub metadanych. Dzięki temu trzymanie zwykłego przedmiotu nie sprzeda
customowego przedmiotu o tym samym materiale bazowym. Przedmioty oznaczone
własnymi danymi pluginów są odrzucane przez tę komendę.

Jeżeli przedmiot nie ma ceny sprzedaży w sklepie, EconomyShopGUI odrzuci
transakcję swoim standardowym komunikatem.

## Wymagania

- Paper `26.2`,
- EconomyShopGUI,
- ekonomia podłączona do EconomyShopGUI, na przykład EssentialsX przez
  VaultUnlocked.

## Uprawnienia

| Permission | Domyślnie | Działanie |
| --- | --- | --- |
| `lifestealsell.use` | każdy | pozwala używać `/sell` |

Gracze muszą również posiadać standardowe uprawnienia EconomyShopGUI do
`/sellgui` oraz `/sellall <material>`.

Można nadać je grupie domyślnej przez LuckPerms:

```text
/lp group default permission set EconomyShopGUI.sellgui.all true
/lp group default permission set EconomyShopGUI.sellallitem.all true
```

## Konflikt z EssentialsX

EssentialsX również rejestruje `/sell`. Aby publiczna komenda zawsze trafiała do
LifestealSell, dodaj w głównym `commands.yml` serwera, obok istniejących aliasów:

```yaml
aliases:
  sell:
    - "lifestealsell:sell $1-"
```

Jeżeli sekcja `aliases` już istnieje, dodaj do niej tylko wpis `sell`. Następnie
wykonaj pełny restart Paper.

## Build i deployment

Lokalny build oraz testy:

```bash
./gradlew :LifestealSell:clean :LifestealSell:build
```

Build na VPS:

```bash
./build-vps.sh sell
```

Aktualizacja i deployment wyłącznie tego pluginu:

```bash
./update.sh sell
```

Finalny plik:

```text
LifestealSell/build/libs/LifestealSell-0.1.0.jar
```
