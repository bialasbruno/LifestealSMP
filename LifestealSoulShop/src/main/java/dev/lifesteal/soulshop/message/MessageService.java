package dev.lifesteal.soulshop.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public final class MessageService {

    private static final long ACTION_BAR_DURATION_TICKS = 40L;

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Plugin plugin;
    private final Logger logger;
    private final Map<UUID, BukkitTask> clearTasks = new HashMap<>();

    public MessageService(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void send(CommandSender recipient, String template) {
        recipient.sendMessage(render(template, Map.of()));
    }

    public void sendActionBar(Player player, String template, Map<String, String> replacements) {
        BukkitTask previous = clearTasks.remove(player.getUniqueId());
        if (previous != null) {
            previous.cancel();
        }

        player.sendActionBar(render(template, replacements));
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

    public Component renderItemText(String template, Map<String, String> replacements) {
        return render(template, replacements).decoration(TextDecoration.ITALIC, false);
    }

    public List<Component> renderItemLore(
            List<String> templates, Map<String, String> replacements) {
        List<Component> lore = new ArrayList<>(templates.size());
        for (String template : templates) {
            lore.add(renderItemText(template, replacements));
        }
        return List.copyOf(lore);
    }

    public Component render(String template, Map<String, String> replacements) {
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
