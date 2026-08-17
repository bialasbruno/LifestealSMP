package dev.lifesteal.homes.command;

import dev.lifesteal.homes.config.HomesSettings;
import dev.lifesteal.homes.data.HomeRepository;
import dev.lifesteal.homes.data.StoredHome;
import dev.lifesteal.homes.menu.HomesMenu;
import dev.lifesteal.homes.message.MessageService;
import dev.lifesteal.homes.rules.HomeLimitRules;
import dev.lifesteal.homes.rules.HomeNameRules;
import dev.lifesteal.homes.teleport.HomeTeleportService;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class HomeCommands implements CommandExecutor, TabCompleter {

    private final HomeRepository repository;
    private final HomesMenu menu;
    private final HomeTeleportService teleports;
    private final MessageService messages;
    private final Supplier<HomesSettings> settingsSupplier;

    public HomeCommands(
            HomeRepository repository,
            HomesMenu menu,
            HomeTeleportService teleports,
            MessageService messages,
            Supplier<HomesSettings> settingsSupplier) {
        this.repository = repository;
        this.menu = menu;
        this.teleports = teleports;
        this.messages = messages;
        this.settingsSupplier = settingsSupplier;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, settingsSupplier.get().message("player-only"));
            return true;
        }

        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "sethome" -> setHome(player, args);
            case "delhome" -> deleteHome(player, args);
            case "homes" -> openMenu(player, args);
            case "home" -> home(player, args);
            default -> false;
        };
    }

    private boolean setHome(Player player, String[] args) {
        HomesSettings settings = settingsSupplier.get();
        if (args.length != 1) {
            messages.send(player, settings.message("sethome-usage"));
            return true;
        }
        String name = args[0];
        if (!HomeNameRules.isValid(name, settings.maximumNameLength())) {
            messages.send(
                    player,
                    settings.message("invalid-name"),
                    Map.of("maximum", Integer.toString(settings.maximumNameLength())));
            return true;
        }

        String key = HomeNameRules.key(name);
        Optional<StoredHome> existing = repository.find(player.getUniqueId(), key);
        int limit = resolveLimit(player, settings);
        if (existing.isEmpty() && limit != HomeLimitRules.UNLIMITED
                && repository.count(player.getUniqueId()) >= limit) {
            messages.send(
                    player,
                    settings.message("limit-reached"),
                    Map.of("limit", Integer.toString(limit)));
            return true;
        }

        Location location = player.getLocation();
        long now = System.currentTimeMillis();
        StoredHome home = new StoredHome(
                player.getUniqueId(),
                key,
                name,
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch(),
                existing.map(StoredHome::createdAt).orElse(now),
                now);
        repository.save(home);
        messages.send(
                player,
                settings.message(existing.isPresent() ? "home-updated" : "home-created"),
                Map.of("home", name));
        return true;
    }

    private boolean deleteHome(Player player, String[] args) {
        HomesSettings settings = settingsSupplier.get();
        if (args.length != 1) {
            messages.send(player, settings.message("delhome-usage"));
            return true;
        }
        Optional<StoredHome> home = repository.find(player.getUniqueId(), HomeNameRules.key(args[0]));
        if (home.isEmpty()) {
            messages.send(player, settings.message("home-not-found"), Map.of("home", args[0]));
            return true;
        }
        repository.delete(player.getUniqueId(), home.get().key());
        messages.send(player, settings.message("home-deleted"), Map.of("home", home.get().name()));
        return true;
    }

    private boolean home(Player player, String[] args) {
        HomesSettings settings = settingsSupplier.get();
        if (args.length == 0) {
            menu.open(player, 0);
            return true;
        }
        if (args.length != 1) {
            messages.send(player, settings.message("home-usage"));
            return true;
        }
        Optional<StoredHome> home = repository.find(player.getUniqueId(), HomeNameRules.key(args[0]));
        if (home.isEmpty()) {
            messages.send(player, settings.message("home-not-found"), Map.of("home", args[0]));
            return true;
        }
        teleports.start(player, home.get());
        return true;
    }

    private boolean openMenu(Player player, String[] args) {
        if (args.length != 0) {
            messages.send(player, settingsSupplier.get().message("homes-usage"));
            return true;
        }
        menu.open(player, 0);
        return true;
    }

    private int resolveLimit(Player player, HomesSettings settings) {
        return HomeLimitRules.resolve(
                settings.defaultLimit(),
                settings.maximumPermissionLimit(),
                player::hasPermission);
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)
                || args.length != 1
                || command.getName().equalsIgnoreCase("sethome")
                || command.getName().equalsIgnoreCase("homes")) {
            return List.of();
        }

        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (StoredHome home : repository.findAll(player.getUniqueId())) {
            if (home.name().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(home.name());
            }
        }
        return matches;
    }
}
