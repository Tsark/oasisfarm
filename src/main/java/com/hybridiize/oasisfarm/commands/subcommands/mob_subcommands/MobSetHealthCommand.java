package com.hybridiize.oasisfarm.commands.subcommands.mob_subcommands;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class MobSetHealthCommand extends MobSubCommand {
    @Override
    public String getName() { return "sethealth"; }
    @Override
    public String getDescription() { return "Sets the max health of a mob template."; }
    @Override
    public String getSyntax() { return "/of mob sethealth <template_id> <health>"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }

        String templateId = args[2];
        double health;
        try {
            health = Double.parseDouble(args[3]);
            if (health <= 0) {
                player.sendMessage(ChatColor.RED + "Health must be a positive number.");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid health value.");
            return;
        }

        FileConfiguration mobTemplatesConfig = plugin.getConfigManager().getMobTemplatesConfig();
        String path = templateId;

        if (!mobTemplatesConfig.contains(path)) {
            player.sendMessage(ChatColor.RED + "The mob template '" + templateId + "' does not exist.");
            return;
        }

        mobTemplatesConfig.set(path + ".health", health);
        plugin.getConfigManager().saveMobTemplatesConfig();
        plugin.getConfigManager().loadAllConfigs();
        player.sendMessage(ChatColor.GREEN + "Set health for template '" + templateId + "' to " + health + ".");
    }
}