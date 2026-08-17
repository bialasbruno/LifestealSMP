package dev.lifesteal.souls.listener;

import dev.lifesteal.souls.config.SoulsSettings;
import dev.lifesteal.souls.data.KillRewardResult;
import dev.lifesteal.souls.message.MessageService;
import dev.lifesteal.souls.service.SoulService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Map;
import java.util.function.Supplier;

public final class KillRewardListener implements Listener {

    private final SoulService soulService;
    private final MessageService messages;
    private final Supplier<SoulsSettings> settingsSupplier;

    public KillRewardListener(
            SoulService soulService,
            MessageService messages,
            Supplier<SoulsSettings> settingsSupplier) {
        this.soulService = soulService;
        this.messages = messages;
        this.settingsSupplier = settingsSupplier;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        SoulsSettings settings = settingsSupplier.get();
        if (!settings.killRewardEnabled()) {
            return;
        }
        Player victim = event.getPlayer();
        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        KillRewardResult result = soulService.rewardKill(
                killer.getUniqueId(), killer.getName(), victim.getUniqueId());
        if (result.rewarded()) {
            messages.send(
                    killer,
                    settings.killRewardMessage(),
                    Map.of(
                            "amount", Long.toString(result.credited()),
                            "balance", Long.toString(result.balance()),
                            "victim", victim.getName()));
        }
    }
}
