package dev.lifesteal.souls.command;

import dev.lifesteal.souls.data.SoulAccount;
import dev.lifesteal.souls.service.SoulService;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

final class PlayerTargetResolver {

    private final Server server;
    private final SoulService soulService;

    PlayerTargetResolver(Server server, SoulService soulService) {
        this.server = server;
        this.soulService = soulService;
    }

    Optional<PlayerTarget> resolve(String input) {
        try {
            UUID playerId = UUID.fromString(input);
            String name = soulService.find(playerId)
                    .map(SoulAccount::lastKnownName)
                    .orElse(playerId.toString());
            return Optional.of(new PlayerTarget(playerId, name));
        } catch (IllegalArgumentException ignored) {
            // The target can still be an online or previously stored player name.
        }

        Player online = server.getPlayerExact(input);
        if (online != null) {
            return Optional.of(new PlayerTarget(online.getUniqueId(), online.getName()));
        }
        return soulService.findByName(input)
                .map(account -> new PlayerTarget(account.playerId(), account.lastKnownName()));
    }
}
