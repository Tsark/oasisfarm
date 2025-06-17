package com.hybridiize.oasisfarm.commands.subcommands;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class RenameCommand extends SubCommand {
    @Override
    public String getName() { return "rename"; }
    @Override
    public String getDescription() { return "Renames an existing farm."; }
    @Override
    public String getSyntax() { return "/of rename <old_name> <new_name>"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }

        String oldName = args[1];
        String newName = args[2];
        FileConfiguration config = plugin.getConfig();

        String oldPath = "farms." + oldName;
        String newPath = "farms." + newName;

        if (!config.contains(oldPath)) {
            player.sendMessage(ChatColor.RED + "The farm '" + oldName + "' does not exist.");
            return;
        }

        if (config.contains(newPath)) {
            player.sendMessage(ChatColor.RED + "A farm with the name '" + newName + "' already exists.");
            return;
        }

        // Copy the old farm's data to the new name
        ConfigurationSection oldFarmData = config.getConfigurationSection(oldPath);
        config.set(newPath, oldFarmData);

        // Remove the old farm's data
        config.set(oldPath, null);

        // Save and reload
        plugin.saveConfig();
        plugin.getHologramManager().removeFarmHologram(oldName);
        plugin.getConfigManager().loadFarms();

        player.sendMessage(ChatColor.GREEN + "Successfully renamed farm '" + oldName + "' to '" + newName + "'.");
    }
}