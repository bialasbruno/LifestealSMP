package dev.lifesteal.spawn.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MessageService {

    private static final long ACTION_BAR_DURATION_TICKS = 40L;

    private final Plugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<UUID, BukkitTask> clearTasks = new HashMap<>();

    public MessageService(Plugin plugin) {
        this.plugin = plugin;
    }

    public void send(CommandSender recipient, String template) {
        recipient.sendMessage(render(template));
    }

    public void sendActionBar(Player player, String template) {
        BukkitTask previous = clearTasks.remove(player.getUniqueId());
        if (previous != null) {
            previous.cancel();
        }

        player.sendActionBar(render(template));
        BukkitTask clearTask = plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> {
                    clearTasks.remove(player.getUniqueId());
                    if (player.isOnline()) {
                        player.sendActionBar(Component.empty());
                    }
                },
                ACTION_BAR_DURATION_TICKS);
        clearTasks.put(player.getUniqueId(), clearTask);
    }

    private Component render(String template) {
        try {
            return miniMessage.deserialize(template);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Invalid MiniMessage text: " + exception.getMessage());
            return Component.text(template);
        }
    }
}
