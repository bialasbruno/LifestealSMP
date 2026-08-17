# LifestealSpawn

`LifestealSpawn` przechowuje niewielkie mechaniki bezpieczeństwa związane ze
światem spawn. Pierwsza wersja ratuje gracza spadającego do voida i teleportuje
go z powrotem na spawn.

Plugin chroni również świat skonfigurowany dla `/afk`. Gdy gracz spadnie w nim
poniżej `Y=3`, zostaje przeniesiony z powrotem do aktualnego środka strefy AFK.

Mechanika jest osobnym pluginem, ponieważ nie należy do systemu serc, waluty,
sklepu ani scoreboardu.

## Wymagania

| Element | Wartość |
| --- | --- |
| Platforma | Paper `26.2`, build `112` |
| Java | `25` |
| Wersja pluginu | `0.1.0` |
| Multiverse-Core | obsługiwany, ale niewymagany jako zależność |
| Wymagana zależność | LifestealSouls `0.1.0` |
| Finalny JAR | `LifestealSpawn/build/libs/LifestealSpawn-0.1.0.jar` |

Plugin korzysta ze światów już załadowanych przez Paper. Dzięki temu świat
utworzony i ładowany przez Multiverse działa bez bezpośredniego wiązania z jego
API.

## Ratowanie z voida

Domyślnie chroniony jest świat o nazwie `spawn`. Gdy pozycja gracza spadnie do
wartości pięć bloków poniżej minimalnej wysokości tego świata, plugin:

1. odnajduje załadowany świat docelowy `spawn`,
2. pobiera jego punkt spawnu,
3. teleportuje gracza,
4. zeruje prędkość i fall distance,
5. pokazuje komunikat nad hotbarem oraz odtwarza dźwięk.

Dodatkowy listener przechwytuje obrażenie typu `VOID`. Jest to zabezpieczenie na
wypadek nietypowej wysokości świata albo ingerencji innego pluginu.

Teleportacja może zostać anulowana przez inny plugin. W takim przypadku
LifestealSpawn nie anuluje obrażenia od voida. Plugin nie wykona również
teleportacji, jeżeli świat docelowy nie jest załadowany albo jego spawn znajduje
się poniżej progu ratunkowego. Zapobiega to nieskończonej pętli teleportów, a
problem jest zapisywany w logu najwyżej raz na 30 sekund.

## Ratowanie w świecie AFK

LifestealSpawn korzysta z tej samej konfiguracji `afk-zone` i tej samej metody
wyznaczania środka co komenda `/afk`. Nie trzeba wpisywać świata ani koordynatów
po raz drugi. Po zmianie granic strefy przez `/soulsadmin reload` kolejne
teleportacje ratunkowe automatycznie użyją nowego środka.

Ratunek uruchamia się dopiero poniżej progu, więc przy domyślnym `trigger-y: 3.0`
pozycja dokładnie `Y=3` jest bezpieczna, a `Y=2.999` uruchamia teleport. Prędkość
i fall distance są zerowane, a gracz zachowuje swój kierunek patrzenia.

```yaml
afk-rescue:
  enabled: true
  trigger-y: 3.0
```

Jeżeli strefa AFK jest wyłączona, jej świat nie jest załadowany albo środek
znajduje się poniżej progu ratunkowego, teleport nie zostanie wykonany.

## Konfiguracja Multiverse

Domyślna konfiguracja zakłada, że nazwa świata Multiverse to dokładnie `spawn`:

```yaml
void-rescue:
  enabled: true
  enabled-worlds:
    - spawn
  trigger-offset-below-min-height: 5
  destination:
    world: spawn
    use-world-spawn: true
```

Nazwy chronionych światów są porównywane bez uwzględniania wielkości liter. Jeśli
Twój świat ma inną nazwę, zmień zarówno `enabled-worlds`, jak i
`destination.world`.

Przy `use-world-spawn: true` używany jest punkt spawnu świata zwracany przez
Paper. Jeżeli chcesz wymusić dokładne współrzędne niezależnie od ustawień świata,
ustaw `use-world-spawn: false` i uzupełnij `x`, `y`, `z`, `yaw` oraz `pitch`.

Tekst ratunku, dźwięk, głośność i ton również można zmienić w `config.yml`.
Wiadomości obsługują MiniMessage.

## Komenda i uprawnienie

| Komenda | Permission | Domyślnie | Opis |
| --- | --- | --- | --- |
| `/lifestealspawn reload` | `lifestealspawn.admin` | operator | Przeładowuje konfigurację. |

Ratowanie graczy nie wymaga permission i obejmuje wszystkich graczy w
skonfigurowanych światach.

## Build i instalacja

Build tylko tego modułu:

```bash
./gradlew :LifestealSpawn:clean :LifestealSpawn:build
```

Na VPS-ie:

```bash
./build-vps.sh spawn
./update.sh spawn
```

Po wdrożeniu `LifestealSpawn.jar` razem z `LifestealSouls.jar` wymagany jest
pełny restart serwera. Jeżeli świat Multiverse nie nazywa się `spawn`, po
pierwszym uruchomieniu zmień `plugins/LifestealSpawn/config.yml`, wykonaj
`/lifestealspawn reload` i dopiero wtedy przetestuj upadek do voida.
