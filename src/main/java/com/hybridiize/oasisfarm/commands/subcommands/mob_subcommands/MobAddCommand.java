package com.hybridiize.oasisfarm.commands.subcommands.mob_subcommands;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class MobAddCommand extends MobSubCommand {
    @Override
    public String getName() { return "add"; }
    @Override
    public String getDescription() { return "Adds a new mob type to a farm."; }
    @Override
    public String getSyntax() { return "/of mob add <farm_name> <mob_type> <chance>"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 5) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }

        String farmId = args[2];
        String mobTypeStr = args[3].toUpperCase();
        double chance;

        try {
            EntityType.valueOf(mobTypeStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid mob type: " + mobTypeStr);
            return;
        }

        try {
            chance = Double.parseDouble(args[4]);
            if (chance <= 0 || chance > 1.0) {
                player.sendMessage(ChatColor.RED + "Chance must be a decimal between 0.0 and 1.0 (e.g., 0.25 for 25%).");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid chance format. Use a decimal like 0.25.");
            return;
        }

        FileConfiguration config = plugin.getConfig();
        String farmPath = "farms." + farmId;

        if (!config.contains(farmPath)) {
            player.sendMessage(ChatColor.RED + "Farm '" + farmId + "' not found.");
            return;
        }

        String mobPath = farmPath + ".mobs." + mobTypeStr;
        config.set(mobPath + ".spawn-chance", chance);
        // You can add default values for other properties here if you want
        config.set(mobPath + ".rewards", new String[]{"msg %player_name% You killed a " + mobTypeStr});

        plugin.saveConfig();
        plugin.getConfigManager().loadFarms();
        player.sendMessage(ChatColor.GREEN + "Added " + mobTypeStr + " to farm " + farmId + " with a " + (chance * 100) + "% chance.");
    }
}