package dev.lifesteal.souls.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public final class MessageService {

    private static final long ACTION_BAR_DURATION_TICKS = 40L;

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Plugin plugin;
    private final Logger logger;
    private final Map<UUID, BukkitTask> actionBarClearTasks = new HashMap<>();

    public MessageService(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void send(CommandSender recipient, String template) {
        send(recipient, template, Map.of());
    }

    public void send(CommandSender recipient, String template, Map<String, String> replacements) {
        recipient.sendMessage(render(template, replacements));
    }

    public void sendActionBar(
            Player recipient, String template, Map<String, String> replacements) {
        UUID playerId = recipient.getUniqueId();
        BukkitTask previousTask = actionBarClearTasks.remove(playerId);
        if (previousTask != null) {
            previousTask.cancel();
        }

        recipient.sendActionBar(render(template, replacements));
        BukkitTask clearTask = plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> {
                    actionBarClearTasks.remove(playerId);
                    if (recipient.isOnline()) {
                        recipient.sendActionBar(Component.empty());
                    }
                },
                ACTION_BAR_DURATION_TICKS);
        actionBarClearTasks.put(playerId, clearTask);
    }

    private Component render(String template, Map<String, String> replacements) {
        String rendered = template;
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            rendered = rendered.replace('{' + replacement.getKey() + '}', replacement.getValue());
        }
        try {
            return miniMessage.deserialize(rendered);
        } catch (RuntimeException exception) {
            logger.warning("Invalid MiniMessage text: " + exception.getMessage());
            return Component.text(rendered);
        }
    }
}
