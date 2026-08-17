package dev.lifesteal.souls.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

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
        String rendered = template;
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            rendered = rendered.replace('{' + replacement.getKey() + '}', replacement.getValue());
        }
        try {
            recipient.sendMessage(miniMessage.deserialize(rendered));
        } catch (RuntimeException exception) {
            logger.warning("Invalid MiniMessage text: " + exception.getMessage());
            recipient.sendMessage(Component.text(rendered));
        }
    }
}
