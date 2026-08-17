package dev.lifesteal.souls.data;

public final class PurchaseConflictException extends IllegalArgumentException {

    public PurchaseConflictException(String transactionId) {
        super("Transaction ID was already used with different purchase data: " + transactionId);
    }
}
