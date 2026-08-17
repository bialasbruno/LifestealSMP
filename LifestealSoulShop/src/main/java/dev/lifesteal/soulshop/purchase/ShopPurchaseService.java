package dev.lifesteal.soulshop.purchase;

import dev.lifesteal.souls.api.LifestealSoulsApi;

import java.util.Objects;
import java.util.UUID;

/** Keeps the debit-before-delivery purchase order explicit and testable. */
public final class ShopPurchaseService {

    private final LifestealSoulsApi soulsApi;

    public ShopPurchaseService(LifestealSoulsApi soulsApi) {
        this.soulsApi = Objects.requireNonNull(soulsApi, "soulsApi");
    }

    public Result purchase(
            UUID playerId, long price, boolean inventoryHasSpace, Runnable delivery) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(delivery, "delivery");
        if (price <= 0L) {
            throw new IllegalArgumentException("price must be positive");
        }
        if (!inventoryHasSpace) {
            return Result.INVENTORY_FULL;
        }
        if (!soulsApi.trySpend(playerId, price, "soulshop:diamond_pickaxe")) {
            return Result.INSUFFICIENT_SOULS;
        }
        delivery.run();
        return Result.SUCCESS;
    }

    public enum Result {
        SUCCESS,
        INSUFFICIENT_SOULS,
        INVENTORY_FULL
    }
}
