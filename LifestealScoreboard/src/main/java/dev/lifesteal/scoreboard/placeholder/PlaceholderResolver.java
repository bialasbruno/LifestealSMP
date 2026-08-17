package dev.lifesteal.scoreboard.placeholder;

import dev.lifesteal.scoreboard.provider.CurrencyProviderRegistry;
import dev.lifesteal.scoreboard.provider.HeartProvider;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Objects;

/** Resolves all built-in scoreboard and externally exposed Lifesteal placeholders. */
public final class PlaceholderResolver {

    private final HeartProvider heartProvider;
    private final CurrencyProviderRegistry currencyProviders;

    public PlaceholderResolver(
            HeartProvider heartProvider,
            CurrencyProviderRegistry currencyProviders) {
        this.heartProvider = Objects.requireNonNull(heartProvider, "heartProvider");
        this.currencyProviders = Objects.requireNonNull(currencyProviders, "currencyProviders");
    }

    public String resolve(PlaceholderTemplate template, PlaceholderContext context) {
        return template.render(key -> resolveValue(context.player(), key));
    }

    public String resolveExternal(Player player, String parameter) {
        String key = "lifesteal_" + parameter.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "lifesteal_hearts", "lifesteal_money", "lifesteal_souls",
                    "lifesteal_kills", "lifesteal_deaths" -> resolveValue(player, key);
            default -> null;
        };
    }

    private String resolveValue(Player player, String key) {
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "player_name" -> player.getName();
            case "player_ping" -> Integer.toString(player.getPing());
            case "lifesteal_hearts" -> Integer.toString(
                    heartProvider.getHearts(player.getUniqueId()));
            case "lifesteal_money" -> NumberFormatter.format(
                    currencyProviders.current().getMoney(player.getUniqueId()));
            case "lifesteal_souls" -> NumberFormatter.format(
                    currencyProviders.current().getSouls(player.getUniqueId()));
            case "lifesteal_kills" -> Integer.toString(
                    player.getStatistic(Statistic.PLAYER_KILLS));
            case "lifesteal_deaths" -> Integer.toString(
                    player.getStatistic(Statistic.DEATHS));
            case "server_online" -> Integer.toString(player.getServer().getOnlinePlayers().size());
            case "server_max" -> Integer.toString(player.getServer().getMaxPlayers());
            default -> null;
        };
    }
}
