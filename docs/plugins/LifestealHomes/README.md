# LifestealHomes

`LifestealHomes` to samodzielny plugin Paper zapewniający prywatne domy graczy,
czytelne GUI, limity zależne od rangi i bezpieczne odliczanie przed teleportem.
Nie wymaga Essentials ani bezpośredniej integracji z LuckPerms.

## Funkcje

- zapis domów w trwałej bazie SQLite `plugins/LifestealHomes/homes.db`,
- obsługa wielu światów, w tym światów ładowanych przez Multiverse,
- nazwy domów niewrażliwe na wielkość liter,
- tworzenie i aktualizowanie domu w aktualnej pozycji gracza,
- GUI pokazujące zajęte, dostępne i zablokowane sloty,
- stronicowanie dla rang z dużą liczbą domów,
- teleport po konfigurowalnym odliczaniu na action barze,
- anulowanie teleportu po ruchu lub otrzymaniu obrażeń,
- osobne GUI potwierdzające usunięcie domu,
- konfigurowalne teksty MiniMessage, materiały GUI i parametry teleportu.

## Komendy

| Komenda | Działanie |
| --- | --- |
| `/sethome <nazwa>` | Tworzy dom lub przenosi istniejący dom w aktualne miejsce. |
| `/home <nazwa>` | Rozpoczyna teleport do domu. |
| `/home` | Otwiera GUI domów. |
| `/homes` | Otwiera GUI domów. |
| `/delhome <nazwa>` | Usuwa wskazany dom. |
| `/lifestealhomes reload` | Przeładowuje `config.yml`. |

W GUI lewy przycisk myszy rozpoczyna teleport. `Shift + prawy przycisk` otwiera
potwierdzenie usunięcia domu.

Nazwa domu może zawierać litery, cyfry, `_` i `-`. Domy `Base` i `base` są
traktowane jako ten sam dom.

## Uprawnienia

| Permission | Domyślnie | Działanie |
| --- | --- | --- |
| `lifestealhomes.use` | każdy | `/home` i `/homes` |
| `lifestealhomes.sethome` | każdy | `/sethome` |
| `lifestealhomes.delete` | każdy | `/delhome` i usuwanie w GUI |
| `lifestealhomes.admin` | operator | przeładowanie konfiguracji |
| `lifestealhomes.limit.<liczba>` | nikt | ustawia limit domów rangi |
| `lifestealhomes.limit.unlimited` | operator | usuwa limit domów |

Jeżeli gracz ma kilka permissionów limitu, plugin wybiera najwyższy. Przykładowa
konfiguracja LuckPerms:

```text
/lp group default permission set lifestealhomes.limit.1 true
/lp group vip permission set lifestealhomes.limit.3 true
/lp group svip permission set lifestealhomes.limit.5 true
/lp group sponsor permission set lifestealhomes.limit.10 true
```

Nie trzeba usuwać niższego permissionu odziedziczonego z poprzedniej rangi.
Zakres sprawdzanych wartości określa `limits.maximum-permission-limit`.

## Konflikt komend z EssentialsX

EssentialsX zazwyczaj oddaje kolidującą komendę innemu pluginowi, ale najbardziej
jednoznaczną konfiguracją jest przypięcie publicznych nazw w pliku
`commands.yml` znajdującym się w głównym katalogu serwera:

```yaml
aliases:
  home:
    - "lifestealhomes:home $1-"
  homes:
    - "lifestealhomes:homes $1-"
  sethome:
    - "lifestealhomes:sethome $1-"
  delhome:
    - "lifestealhomes:delhome $1-"
```

Jeżeli w `plugins/Essentials/config.yml` sekcja `overridden-commands` zawiera
którąkolwiek z tych czterech nazw, należy ją stamtąd usunąć. Po zmianie
`commands.yml` wykonaj pełny restart Paper. Dane domów zapisane wcześniej przez
EssentialsX mogą pozostać na dysku — LifestealHomes używa własnej bazy SQLite.

## Konfiguracja

Plik `plugins/LifestealHomes/config.yml` pozwala zmienić:

- domyślny limit i najwyższą sprawdzaną wartość permissionu,
- maksymalną długość nazwy domu,
- długość odliczania przed teleportem,
- anulowanie teleportu po ruchu lub obrażeniach,
- tolerancję ruchu,
- tytuły, opisy, kolory i materiały GUI,
- wszystkie wiadomości pluginu.

Zmiany można wczytać komendą `/lifestealhomes reload`. Baza `homes.db` nie jest
przeładowywana ani usuwana podczas reloadu lub restartu.

## Build i deployment

Lokalny build oraz testy:

```bash
./gradlew :LifestealHomes:clean :LifestealHomes:build
```

Build na VPS:

```bash
./build-vps.sh homes
```

Aktualizacja i deployment wyłącznie tego pluginu:

```bash
./update.sh homes
```

Finalny plik:

```text
LifestealHomes/build/libs/LifestealHomes-0.1.0.jar
```

Przed większą aktualizacją serwera warto wykonać dodatkową kopię katalogu
`plugins/LifestealHomes/`, ponieważ znajduje się w nim baza wszystkich domów.
