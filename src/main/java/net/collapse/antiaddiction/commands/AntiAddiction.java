package net.collapse.antiaddiction.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.collapse.antiaddiction.AntiaddictionMod;
import net.collapse.antiaddiction.config.PluginConfig;
import net.collapse.antiaddiction.storage.PlayerData;
import net.collapse.antiaddiction.util.TimeParser;

import java.util.ArrayList;
import java.util.List;

public class AntiAddiction {
    public static LiteralCommandNode<CommandSourceStack> register() {
        return Commands.literal("antiaddiction")
            .executes(AntiAddiction::help)
            .then(Commands.literal("reload")
                .executes(AntiAddiction::reload)
                .build())
            .then(Commands.literal("set")
                .then(Commands.literal("x")
                    .then(Commands.argument("time", StringArgumentType.greedyString())
                        .executes(AntiAddiction::setX)
                        .build())
                    .build())
                .then(Commands.literal("y")
                    .then(Commands.argument("time", StringArgumentType.greedyString())
                        .executes(AntiAddiction::setY)
                        .build())
                    .build())
                .build())
            .then(Commands.literal("toggle")
                .then(Commands.literal("mysql")
                    .then(Commands.argument("value", StringArgumentType.word())
                        .executes(AntiAddiction::toggleMysql)
                        .build())
                    .build())
                .then(Commands.literal("redis")
                    .then(Commands.argument("value", StringArgumentType.word())
                        .executes(AntiAddiction::toggleRedis)
                        .build())
                    .build())
                .then(Commands.literal("antiaddiction")
                    .then(Commands.argument("value", StringArgumentType.word())
                        .executes(AntiAddiction::toggleAntiaddiction)
                        .build())
                    .build())
                .build())
            .then(Commands.literal("whitelist")
                .then(Commands.literal("add")
                    .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(AntiAddiction::whitelistAdd)
                        .build())
                    .build())
                .then(Commands.literal("remove")
                    .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(AntiAddiction::whitelistRemove)
                        .build())
                    .build())
                .then(Commands.literal("list")
                    .executes(AntiAddiction::whitelistList)
                    .build())
                .build())
            .build();
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.getSender().isOp()) {
            source.getSender().sendMessage("§cYou do not have permission to use this command.");
            return 0;
        }
        source.getSender().sendMessage("§6=== Anti-Addiction Help ===");
        source.getSender().sendMessage("§e/AntiAddiction reload §7- Reload configuration");
        source.getSender().sendMessage("§e/AntiAddiction set x <time> §7- Set playtime limit");
        source.getSender().sendMessage("§e/AntiAddiction set y <time> §7- Set cooldown duration");
        source.getSender().sendMessage("§e/AntiAddiction toggle mysql <on|off> §7- Enable/disable MySQL");
        source.getSender().sendMessage("§e/AntiAddiction toggle redis <on|off> §7- Enable/disable Redis");
        source.getSender().sendMessage("§e/AntiAddiction toggle antiaddiction <on|off> §7- Enable/disable plugin");
        source.getSender().sendMessage("§e/AntiAddiction whitelist add <name> §7- Add player to whitelist");
        source.getSender().sendMessage("§e/AntiAddiction whitelist remove <name> §7- Remove player from whitelist");
        source.getSender().sendMessage("§e/AntiAddiction whitelist list §7- List whitelisted players");
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.getSender().isOp()) {
            source.getSender().sendMessage("§cYou do not have permission to use this command.");
            return 0;
        }
        AntiaddictionMod.getInstance().getPluginConfig().reload();
        source.getSender().sendMessage("§aConfiguration reloaded.");
        return 1;
    }

    private static int setX(CommandContext<CommandSourceStack> context) {
        return setValue(context, "x");
    }

    private static int setY(CommandContext<CommandSourceStack> context) {
        return setValue(context, "y");
    }

    private static int setValue(CommandContext<CommandSourceStack> context, String key) {
        CommandSourceStack source = context.getSource();
        if (!source.getSender().isOp()) {
            source.getSender().sendMessage("§cYou do not have permission to use this command.");
            return 0;
        }
        String timeStr = context.getArgument("time", String.class);
        try {
            long ticks = TimeParser.parseToTicks(timeStr);
            PluginConfig config = AntiaddictionMod.getInstance().getPluginConfig();
            if ("x".equals(key)) {
                config.setPlaytimeLimitTicks(ticks);
                source.getSender().sendMessage("§aPlaytime limit set to " + timeStr + " (" + ticks + " ticks)");
            } else {
                config.setCooldownDurationTicks(ticks);
                source.getSender().sendMessage("§aCooldown duration set to " + timeStr + " (" + ticks + " ticks)");
            }
        } catch (IllegalArgumentException e) {
            source.getSender().sendMessage("§cInvalid time format: " + e.getMessage());
        }
        return 1;
    }

    private static int toggleMysql(CommandContext<CommandSourceStack> context) {
        return toggle(context, "mysql");
    }

    private static int toggleRedis(CommandContext<CommandSourceStack> context) {
        return toggle(context, "redis");
    }

    private static int toggleAntiaddiction(CommandContext<CommandSourceStack> context) {
        return toggle(context, "antiaddiction");
    }

    private static int toggle(CommandContext<CommandSourceStack> context, String target) {
        CommandSourceStack source = context.getSource();
        if (!source.getSender().isOp()) {
            source.getSender().sendMessage("§cYou do not have permission to use this command.");
            return 0;
        }
        String value = context.getArgument("value", String.class);
        boolean enabled = value.equalsIgnoreCase("on");
        AntiaddictionMod plugin = AntiaddictionMod.getInstance();
        PluginConfig config = plugin.getPluginConfig();

        switch (target) {
            case "mysql":
                config.setMysqlEnabled(enabled);
                if (enabled && plugin.getMysqlProvider() == null) {
                    plugin.initMysql();
                } else if (!enabled) {
                    plugin.setMysqlProvider(null);
                }
                break;
            case "redis":
                config.setRedisEnabled(enabled);
                plugin.initRedis();
                break;
            case "antiaddiction":
                config.setAntiaddictionEnabled(enabled);
                break;
        }
        source.getSender().sendMessage("§a" + target + " " + (enabled ? "enabled" : "disabled"));
        return 1;
    }

    private static int whitelistAdd(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.getSender().isOp()) {
            source.getSender().sendMessage("§cYou do not have permission to use this command.");
            return 0;
        }
        String name = context.getArgument("name", String.class);
        List<String> whitelist = new ArrayList<>(AntiaddictionMod.getInstance().getPluginConfig().getWhitelist());
        if (!whitelist.contains(name)) {
            whitelist.add(name);
            AntiaddictionMod.getInstance().getPluginConfig().setWhitelist(whitelist);
            source.getSender().sendMessage("§aAdded " + name + " to whitelist.");
            for (PlayerData data : AntiaddictionMod.getInstance().getSessionMap().values()) {
                if (data.getName().equalsIgnoreCase(name)) {
                    data.setWhitelisted(true);
                }
            }
        } else {
            source.getSender().sendMessage("§e" + name + " is already whitelisted.");
        }
        return 1;
    }

    private static int whitelistRemove(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.getSender().isOp()) {
            source.getSender().sendMessage("§cYou do not have permission to use this command.");
            return 0;
        }
        String name = context.getArgument("name", String.class);
        List<String> whitelist = new ArrayList<>(AntiaddictionMod.getInstance().getPluginConfig().getWhitelist());
        if (whitelist.remove(name)) {
            AntiaddictionMod.getInstance().getPluginConfig().setWhitelist(whitelist);
            source.getSender().sendMessage("§aRemoved " + name + " from whitelist.");
            for (PlayerData data : AntiaddictionMod.getInstance().getSessionMap().values()) {
                if (data.getName().equalsIgnoreCase(name)) {
                    data.setWhitelisted(false);
                }
            }
        } else {
            source.getSender().sendMessage("§e" + name + " is not whitelisted.");
        }
        return 1;
    }

    private static int whitelistList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.getSender().isOp()) {
            source.getSender().sendMessage("§cYou do not have permission to use this command.");
            return 0;
        }
        List<String> whitelist = AntiaddictionMod.getInstance().getPluginConfig().getWhitelist();
        if (whitelist.isEmpty()) {
            source.getSender().sendMessage("§eWhitelist is empty.");
        } else {
            source.getSender().sendMessage("§aWhitelisted players:");
            for (String name : whitelist) {
                source.getSender().sendMessage("§7- " + name);
            }
        }
        return 1;
    }
}
