package dev.lifesteal.souls.command;

import dev.lifesteal.souls.config.SoulsSettings;
import dev.lifesteal.souls.message.MessageService;
import dev.lifesteal.souls.service.SoulService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Supplier;

public final class SoulsCommand implements CommandExecutor {

    private final SoulService soulService;
    private final MessageService messages;
    private final Supplier<SoulsSettings> settingsSupplier;

    public SoulsCommand(
            SoulService soulService,
            MessageService messages,
            Supplier<SoulsSettings> settingsSupplier) {
        this.soulService = soulService;
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
        if (args.length == 1 && args[0].equalsIgnoreCase("top")) {
            sender.sendMessage("Top Souls balances:");
            int position = 1;
            for (var account : soulService.top(10)) {
                sender.sendMessage(position++ + ". " + account.lastKnownName()
                        + " - " + account.balance() + " Souls");
            }
            if (position == 1) {
                sender.sendMessage("No player has earned Souls yet.");
            }
            return true;
        }
        if (args.length > 0) {
            sender.sendMessage("Usage: /souls [top]");
            return true;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, settings.playerOnlyMessage());
            return true;
        }
        messages.send(
                player,
                settings.balanceMessage(),
                Map.of("balance", Long.toString(soulService.getSouls(player.getUniqueId()))));
        return true;
    }
}
