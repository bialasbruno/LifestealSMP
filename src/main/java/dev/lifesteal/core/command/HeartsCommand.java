package dev.lifesteal.core.command;

import dev.lifesteal.core.heart.HeartService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** {@code /hearts} - shows the sender's current maximum hearts. */
public final class HeartsCommand implements CommandExecutor {

    private final HeartService heartService;
    private final int maximumHearts;

    public HeartsCommand(HeartService heartService, int maximumHearts) {
        this.heartService = heartService;
        this.maximumHearts = maximumHearts;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                              @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        int hearts = heartService.getHearts(player.getUniqueId());
        player.sendMessage(Component.text(
                "You currently have " + hearts + "/" + maximumHearts + " hearts.",
                NamedTextColor.LIGHT_PURPLE));
        return true;
    }
}
