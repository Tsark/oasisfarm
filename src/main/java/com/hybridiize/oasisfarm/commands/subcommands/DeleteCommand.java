package com.hybridiize.oasisfarm.commands.subcommands;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class DeleteCommand extends SubCommand {
    @Override
    public String getName() { return "delete"; }
    @Override
    public String getDescription() { return "Deletes a farm."; }
    @Override
    public String getSyntax() { return "/of delete <farm_name>"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }

        String farmId = args[1];
        FileConfiguration config = plugin.getConfig();

        if (!config.contains("farms." + farmId)) {
            player.sendMessage(ChatColor.RED + "A farm with that name does not exist!");
            return;
        }

        // Remove from config and save
        config.set("farms." + farmId, null);
        plugin.saveConfig();

        // Reload farm data in memory
        plugin.getConfigManager().loadAllConfigs();

        // Remove the hologram
        plugin.getHologramManager().removeFarmHologram(farmId);

        player.sendMessage(ChatColor.GREEN + "Successfully deleted farm '" + farmId + "'!");
    }
}