package com.hybridiize.oasisfarm.commands.subcommands.mob_subcommands;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class MobAddCommand extends MobSubCommand {
    @Override
    public String getName() { return "add"; }

    @Override
    public String getDescription() { return "Adds a mob template to a farm's spawn list."; }

    @Override
    public String getSyntax() { return "/of mob add <farm_name> <template_id> <chance>"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 5) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }

        String farmId = args[2];
        String templateId = args[3];
        double chance;

        if (plugin.getConfigManager().getMobTemplate(templateId) == null) {
            player.sendMessage(ChatColor.RED + "The mob template '" + templateId + "' does not exist in mob-templates.yml.");
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

        String mobPath = farmPath + ".mobs." + templateId;
        config.set(mobPath, chance);

        plugin.saveConfig();
        plugin.getConfigManager().loadAllConfigs();
        player.sendMessage(ChatColor.GREEN + "Added " + templateId + " to farm " + farmId + " with a " + (chance * 100) + "% chance.");
        player.sendMessage(ChatColor.YELLOW + "You may want to run '/of mob rebalance " + farmId + "' to ensure chances total 100%.");
    }
}