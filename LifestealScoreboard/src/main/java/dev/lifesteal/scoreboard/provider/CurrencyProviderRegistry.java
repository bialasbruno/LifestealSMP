package dev.lifesteal.scoreboard.provider;

import dev.lifesteal.scoreboard.api.CurrencyProvider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.ServicesManager;

import java.util.Objects;

/** Tracks the highest-priority CurrencyProvider registered through Bukkit services. */
public final class CurrencyProviderRegistry implements Listener {

    private final ServicesManager servicesManager;
    private volatile CurrencyProvider provider = FallbackCurrencyProvider.INSTANCE;

    public CurrencyProviderRegistry(ServicesManager servicesManager) {
        this.servicesManager = Objects.requireNonNull(servicesManager, "servicesManager");
        refresh();
    }

    public CurrencyProvider current() {
        return provider;
    }

    @EventHandler
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (event.getProvider().getService() == CurrencyProvider.class) {
            refresh();
        }
    }

    @EventHandler
    public void onServiceUnregister(ServiceUnregisterEvent event) {
        if (event.getProvider().getService() == CurrencyProvider.class) {
            refresh();
        }
    }

    private void refresh() {
        CurrencyProvider registered = servicesManager.load(CurrencyProvider.class);
        provider = registered != null ? registered : FallbackCurrencyProvider.INSTANCE;
    }
}
