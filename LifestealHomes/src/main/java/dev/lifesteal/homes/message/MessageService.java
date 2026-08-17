package dev.lifesteal.homes.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class MessageService {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Logger logger;

    public MessageService(Logger logger) {
        this.logger = logger;
    }

    public void send(CommandSender recipient, String template) {
        send(recipient, template, Map.of());
    }

    public void send(CommandSender recipient, String template, Map<String, String> replacements) {
        recipient.sendMessage(render(template, replacements));
    }

    public void actionBar(Player player, String template, Map<String, String> replacements) {
        player.sendActionBar(render(template, replacements));
    }

    public Component itemText(String template, Map<String, String> replacements) {
        return render(template, replacements).decoration(TextDecoration.ITALIC, false);
    }

    public List<Component> itemLore(List<String> templates, Map<String, String> replacements) {
        List<Component> result = new ArrayList<>(templates.size());
        for (String template : templates) {
            result.add(itemText(template, replacements));
        }
        return List.copyOf(result);
    }

    public Component render(String template, Map<String, String> replacements) {
        String rendered = template;
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            rendered = rendered.replace("{" + replacement.getKey() + "}", replacement.getValue());
        }
        try {
            return miniMessage.deserialize(rendered);
        } catch (RuntimeException exception) {
            logger.warning("Invalid MiniMessage text: " + exception.getMessage());
            return Component.text(rendered);
        }
    }
}
