# LifestealSoulItems

`LifestealSoulItems` definiuje customowe przedmioty kupowane za Souls. Pierwszym
przedmiotem jest minimalistyczny `Soul Pickaxe` — netherite pickaxe z widocznymi
duszami uwięzionymi w głowicy i trzonku.

## Wymagania

| Element | Wartość |
| --- | --- |
| Platforma | Paper `26.2`, build `112` |
| Java | `25` |
| Wersja pluginu | `0.1.0` |
| Finalny JAR | `LifestealSoulItems/build/libs/LifestealSoulItems-0.1.0.jar` |
| Resource pack | `ServerPack` z modelem `serverpack:soul_pickaxe` |

Plugin nie ma zależności od `LifestealSouls`. Udostępnia definicje przedmiotów,
a sklep korzysta z nich przez Bukkit Services.

## Soul Pickaxe

Soul Pickaxe bazuje na `NETHERITE_PICKAXE` i zawsze otrzymuje:

| Enchant | Poziom |
| --- | --- |
| Efficiency | V |
| Fortune | III |
| Unbreaking | III |
| Mending | I |

Po rozbiciu bloku kilof niszczy również osiem sąsiednich bloków, tworząc obszar
`3×3`. Płaszczyzna jest prostopadła do strony środkowego bloku, w którą patrzył
gracz: można więc kopać ściany, podłogę i sufit. Mechanika obejmuje wyłącznie
bloki przeznaczone do wydobywania kilofem.

Każdy z ośmiu dodatkowych bloków jest rozbijany przez gracza osobno. Dzięki temu
działają Fortune, Mending, zużycie kilofa, dropy i doświadczenie, a pluginy
ochrony regionów mogą anulować rozbicie dowolnego bloku.

Przedmiot ma nazwę i lore w stylu Souls, własny model
`serverpack:soul_pickaxe` oraz trwały znacznik PDC `soul_pickaxe`. Dzięki temu
inne pluginy mogą rozpoznać oryginalny przedmiot niezależnie od jego nazwy.

## API

`LifestealSoulItemsApi` jest rejestrowane w Bukkit Services i udostępnia:

- `createSoulPickaxe()` — tworzy kompletny, nowy Soul Pickaxe,
- `isSoulPickaxe(ItemStack)` — sprawdza materiał i trwały znacznik przedmiotu.

`LifestealSoulShop` używa tego API zamiast kopiować definicję kilofa.

## ServerPack

Warstwa wizualna składa się z:

- `ServerPack/assets/serverpack/items/soul_pickaxe.json`,
- `ServerPack/assets/serverpack/models/item/soul_pickaxe.json`,
- `ServerPack/assets/serverpack/textures/item/soul_pickaxe.png`.

Źródłowa grafika koncepcyjna jest przechowywana w
`art/concepts/soul-pickaxe-concept-v1.png`. Po zmianie modelu lub tekstury należy
wdrożyć jednocześnie plugin i ServerPack.

## Konfiguracja nazwy i opisu

Przy pierwszym uruchomieniu plugin tworzy
`plugins/LifestealSoulItems/config.yml`. Nazwę i opis faktycznego przedmiotu
można edytować bez przebudowywania pluginu:

```yaml
soul-pickaxe:
  name: "<gradient:#22d3ee:#8b5cf6><bold>Soul Pickaxe</bold></gradient>"
  lore:
    - "<gray>A netherite tool inhabited by restless souls.</gray>"
    - "<dark_purple>Their whispers guide every strike.</dark_purple>"
    - "<aqua>Mines a 3x3 area.</aqua>"
```

Teksty obsługują MiniMessage, a pusta lista `lore: []` całkowicie usuwa opis.
Po zapisaniu pliku wykonaj `/soulitems reload`. Zmiana dotyczy nowych kilofów
tworzonych po przeładowaniu, w tym kolejnych zakupów w SoulShopie. Przedmioty
już zapisane w ekwipunkach zachowują wcześniejsze metadane.

SoulShop ma osobny opis ikony oferty w
`plugins/LifestealSoulShop/config.yml`. Nie zmienia on opisu przedmiotu
wydawanego po zakupie.

## Komenda i uprawnienie

| Komenda | Permission | Domyślnie | Opis |
| --- | --- | --- | --- |
| `/soulitems reload` | `lifestealsoulitems.admin` | operator | Przeładowuje nazwę, lore i komunikaty. |

Dystrybucja przedmiotów odbywa się przez pluginy korzystające z API SoulItems.

## Build i instalacja

```bash
./gradlew :LifestealSoulItems:clean :LifestealSoulItems:build
./build-vps.sh soulitems
./update.sh soulitems
```

Cel `soulitems` wdraża JAR i publikuje nowy ServerPack. Do instalacji ręcznej
należy wgrać `LifestealSoulItems-0.1.0.jar`, opublikować aktualny resource pack i
wykonać pełny restart serwera.
