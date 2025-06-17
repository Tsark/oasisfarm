package com.hybridiize.oasisfarm.commands.subcommands.mob_subcommands;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MobSetHealthCommand extends MobSubCommand {
    @Override
    public String getName() { return "sethealth"; }
    @Override
    public String getDescription() { return "Sets the max health of a mob type."; }
    @Override
    public String getSyntax() { return "/of mob sethealth <farm> <mob> <health>"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }

        String templateId = args[2]; // No farm needed now
        String name = Arrays.stream(args).skip(3).collect(Collectors.joining(" "));

        double health;
        try {
            health = Double.parseDouble(args[4]);
            if (health <= 0) {
                player.sendMessage(ChatColor.RED + "Health must be a positive number.");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid health value.");
            return;
        }

        FileConfiguration config = plugin.getConfig();
        String path = "farms." + farmId + ".mobs." + mobType;

        if (!config.contains(path)) {
            player.sendMessage(ChatColor.RED + "That mob/farm combination does not exist.");
            return;
        }

        config.set(path + ".health", health);
        plugin.saveConfig();
        plugin.getConfigManager().loadFarms();
        player.sendMessage(ChatColor.GREEN + "Set health for " + mobType + " in " + farmId + " to " + health + ".");
    }
}