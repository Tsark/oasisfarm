package com.hybridiize.oasisfarm.commands.subcommands.mob_subcommands;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.Arrays;
import java.util.stream.Collectors;

public class MobSetNameCommand extends MobSubCommand {
    @Override
    public String getName() { return "setname"; }
    @Override
    public String getDescription() { return "Sets the display name of a mob type."; }
    @Override
    public String getSyntax() { return "/of mob setname <farm> <mob> <name...>"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 5) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }

        String farmId = args[2];
        String mobType = args[3].toUpperCase();
        String name = Arrays.stream(args).skip(4).collect(Collectors.joining(" "));

        FileConfiguration config = plugin.getConfig();
        String path = "farms." + farmId + ".mobs." + mobType;

        if (!config.contains(path)) {
            player.sendMessage(ChatColor.RED + "That mob/farm combination does not exist.");
            return;
        }

        config.set(path + ".display-name", name);
        plugin.saveConfig();
        plugin.getConfigManager().loadFarms();

        String coloredName = ChatColor.translateAlternateColorCodes('&', name);
        player.sendMessage(ChatColor.GREEN + "Set display name for " + mobType + " in " + farmId + " to: " + coloredName);
    }
}