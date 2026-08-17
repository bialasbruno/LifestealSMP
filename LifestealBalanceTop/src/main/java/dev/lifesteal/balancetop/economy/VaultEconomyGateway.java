package dev.lifesteal.balancetop.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.ServicesManager;

import java.util.Objects;

/** Tracks the regular economy provider exposed through VaultUnlocked. */
@SuppressWarnings("deprecation")
public final class VaultEconomyGateway implements Listener {

    private final ServicesManager services;
    private volatile Economy economy;
    private volatile long revision;

    public VaultEconomyGateway(ServicesManager services) {
        this.services = Objects.requireNonNull(services, "services");
        refresh();
    }

    public boolean available() {
        return economy != null;
    }

    public long revision() {
        return revision;
    }

    public double balance(OfflinePlayer player) {
        Economy current = economy;
        if (current == null) {
            throw new IllegalStateException("No Vault economy provider is registered");
        }
        return current.getBalance(player);
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
        Economy previous = economy;
        economy = services.load(Economy.class);
        if (previous != economy) {
            revision++;
        }
    }
}
