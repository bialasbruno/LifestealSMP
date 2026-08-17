package dev.lifesteal.scoreboard.provider;

import dev.lifesteal.scoreboard.api.BalanceProvider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.ServicesManager;

import java.util.Objects;

/** Tracks the highest-priority regular economy balance provider. */
public final class BalanceProviderRegistry implements Listener {

    private final ServicesManager servicesManager;
    private volatile BalanceProvider provider = FallbackBalanceProvider.INSTANCE;

    public BalanceProviderRegistry(ServicesManager servicesManager) {
        this.servicesManager = Objects.requireNonNull(servicesManager, "servicesManager");
        refresh();
    }

    public BalanceProvider current() {
        return provider;
    }

    @EventHandler
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (event.getProvider().getService() == BalanceProvider.class) {
            refresh();
        }
    }

    @EventHandler
    public void onServiceUnregister(ServiceUnregisterEvent event) {
        if (event.getProvider().getService() == BalanceProvider.class) {
            refresh();
        }
    }

    private void refresh() {
        BalanceProvider registered = servicesManager.load(BalanceProvider.class);
        provider = registered != null ? registered : FallbackBalanceProvider.INSTANCE;
    }
}
