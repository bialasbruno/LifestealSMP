package dev.lifesteal.scoreboard.integration;

import dev.lifesteal.core.LifestealCorePlugin;
import dev.lifesteal.core.api.LifestealCoreApi;
import dev.lifesteal.scoreboard.LifestealScoreboardPlugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionalDependencyLinkageTest {

    @Test
    void mainPluginClassLoadsWithoutPlaceholderApiOnRuntimeClasspath() {
        assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("me.clip.placeholderapi.PlaceholderAPI"));
        assertDoesNotThrow(() -> Class.forName(
                LifestealScoreboardPlugin.class.getName(), false,
                LifestealScoreboardPlugin.class.getClassLoader()));
    }

    @Test
    void mainPluginClassLoadsWithoutVaultApiOnRuntimeClasspath() {
        assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("net.milkbowl.vault.economy.Economy"));
        assertDoesNotThrow(() -> Class.forName(
                LifestealScoreboardPlugin.class.getName(), false,
                LifestealScoreboardPlugin.class.getClassLoader()));
    }

    @Test
    void corePluginImplementsTheRequiredReadOnlyApi() {
        assertTrue(JavaPlugin.class.isAssignableFrom(LifestealScoreboardPlugin.class));
        assertTrue(LifestealCoreApi.class.isAssignableFrom(LifestealCorePlugin.class));
    }
}
