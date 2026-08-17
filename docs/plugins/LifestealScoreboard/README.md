# LifestealScoreboard

LifestealScoreboard is a lightweight, presentation-only Paper plugin that renders a
configurable sidebar for LifestealSMP. It does not own hearts, currencies, kills, or
deaths. Every value is read from LifestealCore, a provider, Paper statistics, or the
live server state.

## Features

- Flicker-free updates without recreating objectives or scoreboards every refresh.
- MiniMessage title and line formatting.
- Blank lines, visually duplicate lines, and correct top-to-bottom ordering.
- Configurable refresh interval with safe validation and fallbacks.
- Session-only player toggle.
- Runtime configuration reload.
- Optional internal PlaceholderAPI expansion.
- A Bukkit services contract for a future economy plugin.
- No database, file persistence, economy logic, or asynchronous database work.

## Requirements

| Requirement | Version or status |
| --- | --- |
| Paper | `26.2`, build `112` |
| Java | `25` |
| LifestealCore | `0.2.1` or newer; required |
| PlaceholderAPI | Optional; compiled against `2.12.3` |

## Installation

1. Build the repository with `./gradlew clean build` or `./build-vps.sh`.
2. Copy `LifestealCore/build/libs/LifestealCore-0.2.1.jar` to the server `plugins/` directory.
3. Copy `LifestealScoreboard/build/libs/LifestealScoreboard-0.1.0.jar` to the same directory.
4. Start or fully restart Paper.
5. Edit `plugins/LifestealScoreboard/config.yml` if needed.
6. Apply configuration changes with `/lifestealscoreboard reload`.

The repository deployment scripts perform the build, backup, and atomic installation
of both plugin JARs automatically.

## Architecture and data ownership

LifestealScoreboard is not a source of truth. Its renderer reads values through small,
read-only providers:

| Value | Source |
| --- | --- |
| Hearts | `LifestealCoreApi#getHearts(UUID)` |
| Money | Active `CurrencyProvider`, or `0` when none is registered |
| Souls | Active `CurrencyProvider`, or `0` when none is registered |
| Kills | Paper `Statistic.PLAYER_KILLS` |
| Deaths | Paper `Statistic.DEATHS` |
| Player name and ping | Live Paper player data |
| Online and maximum players | Live Paper server data |

The hearts value is already measured in hearts. LifestealScoreboard never reads
`plugins/LifestealCore/data.db` and never converts or duplicates LifestealCore state.

## Rendering lifecycle

Each online player receives one private scoreboard and one reusable sidebar objective.
The objective is created on join, updated in place, and removed on quit or plugin
disable. A configuration reload reuses existing objectives and redraws their content.

Line entries are stable and unique even when their visual MiniMessage output is blank
or duplicated. Modern Paper score custom names provide the visible text, so the plugin
does not create scoreboard teams. This avoids modifying TAB/LuckPerms nametag teams,
prefixes, chat formatting, or LPC state.

The renderer caches the last resolved title and line values. MiniMessage is parsed only
when the resolved text changes. Static lines therefore do not get reparsed every update.
The default refresh interval is 20 ticks, and heart reads use LifestealCore's in-memory
cache rather than database queries.

When the sidebar is disabled or the plugin stops, the player's previous Bukkit
scoreboard is restored if LifestealScoreboard still owns the active view.

## Internal placeholders

The following placeholders are available in the configured title and lines:

| Placeholder | Value |
| --- | --- |
| `%player_name%` | Current player name |
| `%player_ping%` | Current ping in milliseconds |
| `%lifesteal_hearts%` | Current maximum hearts from LifestealCore |
| `%lifesteal_money%` | Formatted Money balance, or `0` |
| `%lifesteal_souls%` | Formatted Souls balance, or `0` |
| `%lifesteal_kills%` | Minecraft player-kill statistic |
| `%lifesteal_deaths%` | Minecraft death statistic |
| `%server_online%` | Current online player count |
| `%server_max%` | Configured server player limit |

Money and Souls are formatted as English whole numbers, for example `950`, `1,250`,
`25,000`, and `1,000,000`.

## PlaceholderAPI integration

PlaceholderAPI is a soft dependency. LifestealScoreboard starts and renders normally
when PlaceholderAPI is not installed. When it is enabled, the plugin registers an
internal expansion with the `lifesteal` identifier and exposes:

- `%lifesteal_hearts%`
- `%lifesteal_money%`
- `%lifesteal_souls%`
- `%lifesteal_kills%`
- `%lifesteal_deaths%`

These external placeholders require an online player context. The expansion is
explicitly unregistered when LifestealScoreboard is disabled.

## Configuration

Default `config.yml`:

```yaml
enabled: true

update:
  interval-ticks: 20

scoreboard:
  title: "<gradient:#ff3b3b:#ff8c42><bold>LIFESTEAL SMP</bold></gradient>"

  lines:
    - ""
    - "<red>❤</red> Hearts: <white>%lifesteal_hearts%</white>"
    - "<green>$</green> Money: <white>%lifesteal_money%</white>"
    - "<aqua>✦</aqua> Souls: <white>%lifesteal_souls%</white>"
    - ""
    - "<yellow>⚔</yellow> Kills: <white>%lifesteal_kills%</white>"
    - "<gray>☠</gray> Deaths: <white>%lifesteal_deaths%</white>"
    - ""
    - "<green>Online:</green> <white>%server_online%/%server_max%</white>"
    - "<gray>play.lifesteal.pl</gray>"
```

`update.interval-ticks` accepts values from `10` through `1200`. Invalid types or
out-of-range values log a warning and fall back to `20`. MiniMessage is validated in
strict mode. Invalid title formatting falls back to the default title, while invalid
lines are ignored. If no valid configured lines remain, the default layout is used.
Minecraft sidebars support at most 15 lines, so additional lines are ignored with a
warning.

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/scoreboard` | `lifestealscoreboard.toggle` | Toggles the player's sidebar for the current session. |
| `/lsboard` | `lifestealscoreboard.toggle` | Alias for `/scoreboard`. |
| `/lifestealscoreboard reload` | `lifestealscoreboard.admin` | Reloads configuration and redraws all visible sidebars. |

The toggle is intentionally session-only in version `0.1.0`. It resets when the player
disconnects or the plugin restarts.

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `lifestealscoreboard.toggle` | Everyone | Allows toggling the personal sidebar. |
| `lifestealscoreboard.admin` | Server operators | Allows reloading the plugin configuration. |

## Money and Souls provider

`CurrencyProvider` is a public, read-only service contract:

```java
public interface CurrencyProvider {
    long getMoney(UUID playerId);
    long getSouls(UUID playerId);
}
```

A future LifestealEconomy plugin can implement the interface and register it through
the Bukkit services manager:

```java
Bukkit.getServicesManager().register(
        CurrencyProvider.class,
        provider,
        economyPlugin,
        ServicePriority.Normal);
```

LifestealScoreboard automatically switches to the highest-priority registered provider
and returns to its zero-value fallback when that service is removed. It never queries or
writes another plugin's database directly.

## Build

From the repository root:

```bash
./gradlew clean build
```

The production JAR is created at:

```text
LifestealScoreboard/build/libs/LifestealScoreboard-0.1.0.jar
```

Unit tests cover placeholder replacement, unknown placeholders, static template reuse,
duplicate visual lines, blank lines, ordering, the 15-line limit, number formatting, and
the fallback Money/Souls provider. A runtime-linkage test also confirms that the main
plugin class loads when PlaceholderAPI is absent and that LifestealCore implements the
required read-only API.

## Deployment

On the configured Ubuntu/Pterodactyl host:

```bash
./update.sh
```

The workflow pulls with fast-forward only, builds and tests both plugins, backs up all
existing LifestealCore and LifestealScoreboard JARs, installs stable target names
atomically, deploys ServerPack, validates its public SHA-1, and then requests a full
Paper restart.

## Current limitations

- The player toggle is not persisted across sessions.
- Money and Souls remain `0` until a `CurrencyProvider` is registered.
- Kills and deaths use Minecraft statistics rather than a custom PvP database.
- PlaceholderAPI placeholders are available only for online player contexts.
- Minecraft exposes one sidebar display slot. Running another sidebar plugin at the same
  time may cause one plugin to replace the other's visible objective.
- `/scoreboard` shares its name with a vanilla command. `/lsboard` remains available as
  an unambiguous alias if another command takes precedence.
