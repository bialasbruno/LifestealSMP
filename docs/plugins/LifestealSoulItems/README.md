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

## Komendy, uprawnienia i konfiguracja

Plugin nie rejestruje komend ani uprawnień i nie tworzy pliku konfiguracyjnego.
Dystrybucja przedmiotów odbywa się przez pluginy korzystające z jego API.

## Build i instalacja

```bash
./gradlew :LifestealSoulItems:clean :LifestealSoulItems:build
./build-vps.sh soulitems
./update.sh soulitems
```

Cel `soulitems` wdraża JAR i publikuje nowy ServerPack. Do instalacji ręcznej
należy wgrać `LifestealSoulItems-0.1.0.jar`, opublikować aktualny resource pack i
wykonać pełny restart serwera.
