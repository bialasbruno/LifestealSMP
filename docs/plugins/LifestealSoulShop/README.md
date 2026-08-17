# LifestealSoulShop

`LifestealSoulShop` to prosty sklep GUI wykorzystujący przypisaną do gracza
walutę z `LifestealSouls`. Aktualna oferta zawiera jeden customowy produkt:
`Soul Pickaxe` za domyślnie `2500 Souls`.

Plugin odpowiada za prezentację oferty, sprawdzenie miejsca w ekwipunku,
atomowe pobranie ceny i wydanie przedmiotu utworzonego przez `LifestealSoulItems`.
Nie nalicza waluty i nie definiuje przedmiotów samodzielnie.

## Wymagania

| Element | Wartość |
| --- | --- |
| Platforma | Paper `26.2`, build `112` |
| Java | `25` |
| Wersja pluginu | `0.1.0` |
| Wymagane zależności | LifestealSouls `0.1.0`, LifestealSoulItems `0.1.0` |
| Finalny JAR | `LifestealSoulShop/build/libs/LifestealSoulShop-0.1.0.jar` |

Oba pluginy zależne muszą być zainstalowane i włączone. SoulShop pobiera ich
API przez Bukkit Services i bez któregoś z nich bezpiecznie się wyłączy.

## GUI

Komenda `/soulshop` otwiera menu o rozmiarze trzech rzędów:

- głowa gracza u góry pokazuje aktualne saldo,
- Soul Pickaxe znajduje się na środku,
- Barrier na dole zamyka sklep,
- pozostałe pola wypełniają szare szyby bez nazwy.

Klikanie i przeciąganie przedmiotów w menu jest zablokowane. Ikona produktu
korzysta z prawdziwego modelu Soul Pickaxe. Po udanym zakupie menu pozostaje
otwarte i natychmiast pokazuje nowe saldo.

## Przebieg zakupu

1. `LifestealSoulItemsApi` tworzy oryginalny Soul Pickaxe.
2. Plugin sprawdza, czy ekwipunek pomieści przedmiot.
3. `LifestealSoulsApi.trySpend` atomowo próbuje pobrać pełną cenę.
4. Dopiero po udanym pobraniu plugin wydaje przedmiot.
5. Gracz otrzymuje komunikat nad hotbarem i krótki dźwięk.

Przy pełnym ekwipunku Souls nie są pobierane. Za małe saldo również nie powoduje
wydania przedmiotu. Kliknięcia wykonane w odstępie krótszym niż `300 ms` są
ignorowane, aby podwójny klik nie wykonał przypadkiem dwóch zakupów.

Każdy udany zakup pojawia się w historii `LifestealSouls` jako transakcja
`ITEM_PURCHASE` z referencją `soulshop:soul_pickaxe`.

## Komendy i uprawnienia

| Komenda | Permission | Domyślnie | Opis |
| --- | --- | --- | --- |
| `/soulshop` | `lifestealsoulshop.use` | każdy gracz | Otwiera GUI sklepu. |
| `/soulshop reload` | `lifestealsoulshop.admin` | operator | Przeładowuje konfigurację i odświeża otwarte menu. |

Konsola nie może otworzyć GUI, ale może wykonać przeładowanie konfiguracji.

## Konfiguracja

Cena produktu znajduje się w `plugins/LifestealSoulShop/config.yml`:

```yaml
product:
  price: 2500
```

Konfigurowalne są również tytuł menu, materiał tła, nazwy i lore ikon, wszystkie
komunikaty oraz dźwięki sukcesu i błędu. Teksty używają MiniMessage, a dostępne
placeholdery to `{balance}` i `{price}`. Niepoprawna cena lub parametry dźwięku
są zastępowane bezpiecznymi wartościami domyślnymi i raportowane w logu.

Przy pierwszym uruchomieniu wersji z Soul Pickaxe konfiguracja startowego
Diamond Pickaxe jest automatycznie migrowana. Domyślna stara cena `100` zmienia
się na `2500`, ale wcześniej ustawiona własna cena zostaje zachowana.

## Build i instalacja

```bash
./gradlew :LifestealSoulShop:clean :LifestealSoulShop:build
./build-vps.sh soulshop
./update.sh soulshop
```

Do katalogu `plugins/` należy wgrać `LifestealSoulShop-0.1.0.jar` razem z
`LifestealSouls.jar` oraz `LifestealSoulItems.jar`. Soul Pickaxe wymaga również
aktualnego ServerPacka. Po instalacji lub aktualizacji wymagany jest pełny
restart serwera.
