# LifestealSoulShop

`LifestealSoulShop` to prosty sklep GUI wykorzystujący przypisaną do gracza
walutę z pluginu `LifestealSouls`. Pierwsza wersja celowo zawiera jeden produkt:
zwykły `Diamond Pickaxe` za domyślnie `100 Souls`.

Plugin odpowiada za prezentację oferty, sprawdzenie miejsca w ekwipunku,
atomowe pobranie ceny przez API Souls i wydanie produktu. Nie nalicza waluty,
nie udostępnia przelewów i nie tworzy jeszcze customowych przedmiotów.

## Wymagania

| Element | Wartość |
| --- | --- |
| Platforma | Paper `26.2`, build `112` |
| Java | `25` |
| Wersja pluginu | `0.1.0` |
| Wymagana zależność | LifestealSouls `0.1.0` |
| Finalny JAR | `LifestealSoulShop/build/libs/LifestealSoulShop-0.1.0.jar` |

`LifestealSouls.jar` musi być zainstalowany i włączony. SoulShop pobiera jego
publiczne `LifestealSoulsApi` przez Bukkit Services i bez niego bezpiecznie się
wyłączy.

## GUI

Komenda `/soulshop` otwiera menu o rozmiarze trzech rzędów:

- głowa gracza u góry pokazuje aktualne saldo,
- Diamond Pickaxe znajduje się na środku,
- Barrier na dole zamyka sklep,
- pozostałe pola wypełniają szare szyby bez nazwy.

Klikanie i przeciąganie przedmiotów w menu jest zablokowane. Ikona produktu ma
opis sklepu, ale kupujący otrzymuje czysty przedmiot Minecrafta bez nazwy oraz
lore z GUI. Po udanym zakupie menu pozostaje otwarte i natychmiast pokazuje nowe
saldo.

## Przebieg zakupu

1. Plugin sprawdza, czy ekwipunek pomieści cały produkt.
2. `LifestealSoulsApi.trySpend` atomowo próbuje pobrać pełną cenę.
3. Dopiero po udanym pobraniu plugin wydaje przedmiot.
4. Gracz otrzymuje komunikat nad hotbarem i krótki dźwięk.

Przy pełnym ekwipunku Souls nie są pobierane. Za małe saldo również nie powoduje
wydania przedmiotu. Kliknięcia wykonane w odstępie krótszym niż `300 ms` są
ignorowane, aby podwójny klik nie wykonał przypadkiem dwóch zakupów.

Każdy udany zakup pojawia się w historii `LifestealSouls` jako transakcja
`ITEM_PURCHASE` z referencją `soulshop:diamond_pickaxe`.

## Komendy i uprawnienia

| Komenda | Permission | Domyślnie | Opis |
| --- | --- | --- | --- |
| `/soulshop` | `lifestealsoulshop.use` | każdy gracz | Otwiera GUI sklepu. |
| `/soulshop reload` | `lifestealsoulshop.admin` | operator | Przeładowuje konfigurację i odświeża otwarte menu. |

Konsola nie może otworzyć GUI, ale może wykonać przeładowanie konfiguracji.

## Konfiguracja

Najważniejsze ustawienia w `plugins/LifestealSoulShop/config.yml`:

```yaml
product:
  material: DIAMOND_PICKAXE
  amount: 1
  price: 100
```

Konfigurowalne są również:

- tytuł menu, materiał tła, nazwy oraz lore ikon,
- wszystkie komunikaty,
- dźwięki sukcesu i błędu wraz z głośnością oraz tonem.

Teksty używają MiniMessage. W zależności od pola dostępne są placeholdery
`{balance}`, `{price}` i `{amount}`. Niepoprawna cena, liczba przedmiotów,
materiał albo parametry dźwięku zostaną zastąpione bezpiecznymi wartościami
domyślnymi i zapisane jako ostrzeżenie w logu.

## Przyszłe SoulItems

Customowe przedmioty nie należą do pierwszej wersji sklepu. Zostaną dodane w
osobnym pluginie `LifestealSoulItems`. SoulShop pozostanie warstwą GUI i płatności,
dzięki czemu mechanika waluty oraz definicje przedmiotów nie będą ze sobą
pomieszane.

## Build i instalacja

Build tylko tego modułu:

```bash
./gradlew :LifestealSoulShop:clean :LifestealSoulShop:build
```

Na VPS-ie:

```bash
./build-vps.sh soulshop
./update.sh soulshop
```

Do katalogu `plugins/` należy wgrać `LifestealSoulShop-0.1.0.jar` razem z
`LifestealSouls.jar`. Po instalacji lub aktualizacji wymagany jest pełny restart
serwera.
