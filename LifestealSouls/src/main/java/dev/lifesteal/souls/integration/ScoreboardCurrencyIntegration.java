package dev.lifesteal.souls.integration;

import dev.lifesteal.souls.api.LifestealSoulsApi;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.UUID;

/** Optional runtime bridge to Scoreboard without a build-time module dependency. */
public final class ScoreboardCurrencyIntegration implements Listener {

    private static final String SCOREBOARD_PLUGIN = "LifestealScoreboard";
    private static final String PROVIDER_CLASS =
            "dev.lifesteal.scoreboard.api.CurrencyProvider";

    private final Plugin plugin;
    private final LifestealSoulsApi soulsApi;
    private Class<?> serviceClass;
    private Object serviceProvider;

    public ScoreboardCurrencyIntegration(Plugin plugin, LifestealSoulsApi soulsApi) {
        this.plugin = plugin;
        this.soulsApi = soulsApi;
    }

    public void start() {
        Plugin scoreboard = plugin.getServer().getPluginManager().getPlugin(SCOREBOARD_PLUGIN);
        if (scoreboard != null && scoreboard.isEnabled()) {
            register(scoreboard);
        }
    }

    public void stop() {
        unregister();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin().getName().equals(SCOREBOARD_PLUGIN)) {
            register(event.getPlugin());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().getName().equals(SCOREBOARD_PLUGIN)) {
            unregister();
        }
    }

    private void register(Plugin scoreboard) {
        if (serviceProvider != null) {
            return;
        }
        try {
            ClassLoader scoreboardLoader = scoreboard.getClass().getClassLoader();
            Class<?> loadedServiceClass = Class.forName(PROVIDER_CLASS, true, scoreboardLoader);
            ServicesManager services = plugin.getServer().getServicesManager();
            CurrencyInvocationHandler handler = new CurrencyInvocationHandler(
                    services, loadedServiceClass, soulsApi);
            Object proxy = Proxy.newProxyInstance(
                    scoreboardLoader, new Class<?>[]{loadedServiceClass}, handler);
            handler.setOwnProvider(proxy);
            registerRaw(services, loadedServiceClass, proxy, plugin);
            serviceClass = loadedServiceClass;
            serviceProvider = proxy;
            plugin.getLogger().info("Connected Souls balance to LifestealScoreboard.");
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning(
                    "Could not connect to LifestealScoreboard: " + exception.getMessage());
        }
    }

    private void unregister() {
        if (serviceClass == null || serviceProvider == null) {
            return;
        }
        unregisterRaw(plugin.getServer().getServicesManager(), serviceClass, serviceProvider);
        serviceClass = null;
        serviceProvider = null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerRaw(
            ServicesManager services, Class service, Object provider, Plugin plugin) {
        services.register(service, provider, plugin, ServicePriority.Highest);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void unregisterRaw(ServicesManager services, Class service, Object provider) {
        services.unregister(service, provider);
    }

    private final class CurrencyInvocationHandler implements InvocationHandler {

        private final ServicesManager services;
        private final Class<?> providerType;
        private final LifestealSoulsApi api;
        private Object ownProvider;

        private CurrencyInvocationHandler(
                ServicesManager services, Class<?> providerType, LifestealSoulsApi api) {
            this.services = services;
            this.providerType = providerType;
            this.api = api;
        }

        private void setOwnProvider(Object ownProvider) {
            this.ownProvider = ownProvider;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "LifestealSoulsCurrencyProvider";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            }
            return switch (method.getName()) {
                case "getSouls" -> api.getSouls((UUID) args[0]);
                case "getMoney" -> delegateMoney(method, args);
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private long delegateMoney(Method method, Object[] args) throws Throwable {
            RegisteredServiceProvider<?> best = null;
            for (RegisteredServiceProvider<?> registration : registrations()) {
                if (registration.getProvider() == ownProvider) {
                    continue;
                }
                if (best == null
                        || registration.getPriority().ordinal() > best.getPriority().ordinal()) {
                    best = registration;
                }
            }
            if (best == null) {
                return 0L;
            }
            try {
                return ((Number) method.invoke(best.getProvider(), args)).longValue();
            } catch (InvocationTargetException exception) {
                throw exception.getCause();
            }
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private Collection<RegisteredServiceProvider<?>> registrations() {
            return (Collection) services.getRegistrations((Class) providerType);
        }
    }
}
