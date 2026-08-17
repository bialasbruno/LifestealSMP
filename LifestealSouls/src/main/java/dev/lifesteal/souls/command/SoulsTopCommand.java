package dev.lifesteal.souls.command;

import dev.lifesteal.souls.config.SoulsSettings;
import dev.lifesteal.souls.menu.SoulLeaderboardMenu;
import dev.lifesteal.souls.message.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/** Opens the read-only Souls leaderboard. */
public final class SoulsTopCommand implements CommandExecutor {

    private final SoulLeaderboardMenu leaderboardMenu;
    private final MessageService messages;
    private final Supplier<SoulsSettings> settingsSupplier;

    public SoulsTopCommand(
            SoulLeaderboardMenu leaderboardMenu,
            MessageService messages,
            Supplier<SoulsSettings> settingsSupplier) {
        this.leaderboardMenu = leaderboardMenu;
        this.messages = messages;
        this.settingsSupplier = settingsSupplier;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        SoulsSettings settings = settingsSupplier.get();
        if (!(sender instanceof Player player)) {
            messages.send(sender, settings.playerOnlyMessage());
            return true;
        }
        if (args.length > 0) {
            sender.sendMessage("Usage: /soulstop");
            return true;
        }
        leaderboardMenu.open(player);
        return true;
    }
}
