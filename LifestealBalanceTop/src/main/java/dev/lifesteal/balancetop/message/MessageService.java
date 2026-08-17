package dev.lifesteal.balancetop.message;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

/** Sends configurable MiniMessage messages. */
public final class MessageService {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public void send(CommandSender sender, String message) {
        sender.sendMessage(miniMessage.deserialize(message));
    }
}
