package dev.lifesteal.souls.service;

import dev.lifesteal.souls.config.SoulsSettings;
import dev.lifesteal.souls.data.KillRewardResult;
import dev.lifesteal.souls.data.PlaytimeRewardResult;
import dev.lifesteal.souls.data.PurchaseResult;
import dev.lifesteal.souls.data.SoulAccount;
import dev.lifesteal.souls.data.SoulMutation;
import dev.lifesteal.souls.data.SoulRepository;
import dev.lifesteal.souls.data.SoulTransaction;
import dev.lifesteal.souls.data.SoulTransactionType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Coordinates atomic storage operations and the online balance cache. */
public final class SoulService {

    private final SoulRepository repository;
    private final Map<UUID, Long> onlineBalances = new ConcurrentHashMap<>();
    private volatile SoulsSettings settings;

    public SoulService(SoulRepository repository, SoulsSettings settings) {
        this.repository = repository;
        this.settings = settings;
    }

    public void updateSettings(SoulsSettings settings) {
        this.settings = settings;
    }

    public synchronized SoulAccount loadPlayer(UUID playerId, String playerName) {
        SoulAccount account = repository.loadOrCreate(playerId, playerName);
        onlineBalances.put(playerId, account.balance());
        return account;
    }

    public void unloadPlayer(UUID playerId) {
        onlineBalances.remove(playerId);
    }

    public long getSouls(UUID playerId) {
        Long cached = onlineBalances.get(playerId);
        if (cached != null) {
            return cached;
        }
        return repository.find(playerId).map(SoulAccount::balance).orElse(0L);
    }

    public Optional<SoulAccount> find(UUID playerId) {
        return repository.find(playerId);
    }

    public Optional<SoulAccount> findByName(String playerName) {
        return repository.findByName(playerName);
    }

    public List<SoulAccount> top(int limit) {
        return repository.top(limit);
    }

    public synchronized PlaytimeRewardResult addActivePlaytime(
            UUID playerId, String playerName, long elapsedMillis) {
        SoulsSettings current = settings;
        PlaytimeRewardResult result = repository.addActivePlaytime(
                playerId,
                playerName,
                elapsedMillis,
                current.playtimeRewardIntervalMillis(),
                current.playtimeRewardAmount(),
                Instant.now(),
                current.maximumBalance());
        onlineBalances.put(playerId, result.balance());
        return result;
    }

    public synchronized KillRewardResult rewardKill(
            UUID killerId, String killerName, UUID victimId) {
        SoulsSettings current = settings;
        KillRewardResult result = repository.rewardKill(
                killerId,
                killerName,
                victimId,
                Instant.now(),
                current.killCooldownMillis(),
                current.killRewardAmount(),
                current.maximumBalance());
        onlineBalances.put(killerId, result.balance());
        return result;
    }

    public synchronized SoulMutation addAdmin(
            UUID playerId, String playerName, long amount, String reference) {
        SoulMutation result = repository.add(
                playerId,
                playerName,
                amount,
                SoulTransactionType.ADMIN_ADD,
                normalizeReference(reference),
                Instant.now(),
                settings.maximumBalance());
        updateCachedBalance(playerId, result.balance());
        return result;
    }

    public synchronized Optional<SoulMutation> removeAdmin(
            UUID playerId, String playerName, long amount, String reference) {
        Optional<SoulMutation> result = repository.tryDebit(
                playerId,
                playerName,
                amount,
                SoulTransactionType.ADMIN_REMOVE,
                normalizeReference(reference),
                Instant.now());
        result.ifPresent(mutation -> updateCachedBalance(playerId, mutation.balance()));
        return result;
    }

    public synchronized SoulMutation setAdmin(UUID playerId, String playerName, long balance) {
        SoulMutation result = repository.setBalance(
                playerId, playerName, balance, Instant.now(), settings.maximumBalance());
        updateCachedBalance(playerId, result.balance());
        return result;
    }

    public synchronized PurchaseResult applyPurchase(
            String transactionId, UUID playerId, long amount) {
        String playerName = repository.find(playerId)
                .map(SoulAccount::lastKnownName)
                .orElse(playerId.toString());
        PurchaseResult result = repository.applyPurchase(
                transactionId,
                playerId,
                playerName,
                amount,
                Instant.now(),
                settings.maximumBalance());
        updateCachedBalance(playerId, result.balance());
        return result;
    }

    public synchronized boolean trySpend(UUID playerId, long amount, String reason) {
        String playerName = repository.find(playerId)
                .map(SoulAccount::lastKnownName)
                .orElse(playerId.toString());
        Optional<SoulMutation> result = repository.tryDebit(
                playerId,
                playerName,
                amount,
                SoulTransactionType.ITEM_PURCHASE,
                normalizeReference(reason),
                Instant.now());
        result.ifPresent(mutation -> updateCachedBalance(playerId, mutation.balance()));
        return result.isPresent();
    }

    public List<SoulTransaction> history(UUID playerId, int limit) {
        return repository.history(playerId, limit);
    }

    private void updateCachedBalance(UUID playerId, long balance) {
        if (onlineBalances.containsKey(playerId)) {
            onlineBalances.put(playerId, balance);
        }
    }

    private String normalizeReference(String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }
        return reference.length() > 128 ? reference.substring(0, 128) : reference;
    }
}
