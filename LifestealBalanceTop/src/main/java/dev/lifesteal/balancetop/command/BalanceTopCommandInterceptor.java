package dev.lifesteal.balancetop.command;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/** Makes the GUI win even when Essentials registered /baltop before this plugin. */
public final class BalanceTopCommandInterceptor implements Listener {

    private static final Set<String> LABELS = Set.of(
            "baltop", "balancetop", "ebaltop", "ebalancetop");

    private final BalanceTopCommand command;

    public BalanceTopCommandInterceptor(BalanceTopCommand command) {
        this.command = command;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message.length() < 2) {
            return;
        }

        String[] parts = message.substring(1).trim().split("\\s+");
        if (parts.length == 0 || !isBalanceTopLabel(parts[0])) {
            return;
        }

        event.setCancelled(true);
        command.execute(event.getPlayer(), Arrays.copyOfRange(parts, 1, parts.length));
    }

    public static boolean isBalanceTopLabel(String input) {
        String label = input.toLowerCase(Locale.ROOT);
        int namespaceSeparator = label.indexOf(':');
        if (namespaceSeparator >= 0) {
            label = label.substring(namespaceSeparator + 1);
        }
        return LABELS.contains(label);
    }
}
