package dev.lifesteal.scoreboard.provider;

import dev.lifesteal.scoreboard.api.BalanceProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;

import java.util.Objects;
import java.util.UUID;

/** Read-only bridge from Vault/VaultUnlocked economy to the scoreboard balance contract. */
@SuppressWarnings("deprecation") // EssentialsX exposes its balance through the legacy Vault API.
public final class VaultBalanceProvider implements BalanceProvider, Listener {

    private final Server server;
    private final ServicesManager services;
    private volatile Economy economy;

    public VaultBalanceProvider(Server server) {
        this.server = Objects.requireNonNull(server, "server");
        this.services = server.getServicesManager();
        refresh();
    }

    public void register(Plugin plugin) {
        services.register(BalanceProvider.class, this, plugin, ServicePriority.Normal);
    }

    public void unregister() {
        services.unregister(BalanceProvider.class, this);
        economy = null;
    }

    public boolean available() {
        return economy != null;
    }

    @Override
    public double getBalance(UUID playerId) {
        Economy current = economy;
        if (current == null) {
            return 0.0D;
        }
        double balance = current.getBalance(server.getOfflinePlayer(playerId));
        return Double.isFinite(balance) ? balance : 0.0D;
    }

    @EventHandler
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (event.getProvider().getService() == Economy.class) {
            refresh();
        }
    }

    @EventHandler
    public void onServiceUnregister(ServiceUnregisterEvent event) {
        if (event.getProvider().getService() == Economy.class) {
            refresh();
        }
    }

    private void refresh() {
        economy = services.load(Economy.class);
    }
}
