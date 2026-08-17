package dev.lifesteal.soulshop.purchase;

import dev.lifesteal.souls.api.LifestealSoulsApi;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopPurchaseServiceTest {

    private static final UUID PLAYER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void fullInventoryIsRejectedBeforeSoulsAreDebited() {
        FakeSoulsApi api = new FakeSoulsApi(100L);
        AtomicBoolean delivered = new AtomicBoolean();
        ShopPurchaseService service = new ShopPurchaseService(api);

        ShopPurchaseService.Result result =
                service.purchase(PLAYER_ID, 100L, false, () -> delivered.set(true));

        assertEquals(ShopPurchaseService.Result.INVENTORY_FULL, result);
        assertEquals(0, api.spendAttempts);
        assertFalse(delivered.get());
    }

    @Test
    void insufficientBalanceDoesNotDeliverTheItem() {
        FakeSoulsApi api = new FakeSoulsApi(99L);
        AtomicBoolean delivered = new AtomicBoolean();
        ShopPurchaseService service = new ShopPurchaseService(api);

        ShopPurchaseService.Result result =
                service.purchase(PLAYER_ID, 100L, true, () -> delivered.set(true));

        assertEquals(ShopPurchaseService.Result.INSUFFICIENT_SOULS, result);
        assertEquals(1, api.spendAttempts);
        assertFalse(delivered.get());
        assertEquals(99L, api.balance);
    }

    @Test
    void successfulDebitDeliversExactlyOnce() {
        FakeSoulsApi api = new FakeSoulsApi(150L);
        int[] deliveries = {0};
        ShopPurchaseService service = new ShopPurchaseService(api);

        ShopPurchaseService.Result result =
                service.purchase(PLAYER_ID, 100L, true, () -> deliveries[0]++);

        assertEquals(ShopPurchaseService.Result.SUCCESS, result);
        assertEquals(1, api.spendAttempts);
        assertEquals(1, deliveries[0]);
        assertEquals(50L, api.balance);
        assertTrue(api.lastReason.startsWith("soulshop:"));
    }

    @Test
    void rejectsAnInvalidPriceBeforeCallingTheCurrencyApi() {
        FakeSoulsApi api = new FakeSoulsApi(100L);
        ShopPurchaseService service = new ShopPurchaseService(api);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.purchase(PLAYER_ID, 0L, true, () -> {}));
        assertEquals(0, api.spendAttempts);
    }

    private static final class FakeSoulsApi implements LifestealSoulsApi {

        private long balance;
        private int spendAttempts;
        private String lastReason = "";

        private FakeSoulsApi(long balance) {
            this.balance = balance;
        }

        @Override
        public long getSouls(UUID playerId) {
            return balance;
        }

        @Override
        public boolean trySpend(UUID playerId, long amount, String reason) {
            spendAttempts++;
            lastReason = reason;
            if (balance < amount) {
                return false;
            }
            balance -= amount;
            return true;
        }
    }
}
