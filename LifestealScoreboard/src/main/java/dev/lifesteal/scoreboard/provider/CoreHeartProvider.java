package dev.lifesteal.scoreboard.provider;

import dev.lifesteal.core.api.LifestealCoreApi;

import java.util.Objects;
import java.util.UUID;

/** Delegates heart reads to the public LifestealCore API. */
public final class CoreHeartProvider implements HeartProvider {

    private final LifestealCoreApi coreApi;

    public CoreHeartProvider(LifestealCoreApi coreApi) {
        this.coreApi = Objects.requireNonNull(coreApi, "coreApi");
    }

    @Override
    public int getHearts(UUID playerId) {
        return coreApi.getHearts(playerId);
    }
}
