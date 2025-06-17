package com.hybridiize.oasisfarm.commands.subcommands;

import com.hybridiize.oasisfarm.managers.SelectionManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class CreateCommand extends SubCommand {
    private final SelectionManager selectionManager;

    public CreateCommand(SelectionManager selectionManager) { this.selectionManager = selectionManager; }

    @Override
    public String getName() { return "create"; }
    @Override
    public String getDescription() { return "Creates a new farm from your selection."; }
    @Override
    public String getSyntax() { return "/of create <farm_name>"; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: " + getSyntax());
            return;
        }

        if (!selectionManager.hasBothPositions(player)) {
            player.sendMessage(ChatColor.RED + "You must select two positions with the wand first!");
            return;
        }

        String farmId = args[1];
        FileConfiguration config = plugin.getConfig();

        if (config.contains("farms." + farmId)) {
            player.sendMessage(ChatColor.RED + "A farm with that name already exists!");
            return;
        }

        Location pos1 = selectionManager.getPos1(player);
        Location pos2 = selectionManager.getPos2(player);
        String worldName = pos1.getWorld().getName();
        String path = "farms." + farmId;

        config.set(path + ".region.world", worldName);
        config.set(path + ".region.pos1", pos1.getBlockX() + "," + pos1.getBlockY() + "," + pos1.getBlockZ());
        config.set(path + ".region.pos2", pos2.getBlockX() + "," + pos2.getBlockY() + "," + pos2.getBlockZ());
        config.set(path + ".max-mobs", 10);
        config.set(path + ".mobs.starter_zombie", 1.0);

        plugin.saveConfig();
        plugin.getConfigManager().loadAllConfigs();

        plugin.getHologramManager().removeFarmHologram(farmId);
        player.sendMessage(ChatColor.GREEN + "Successfully created farm '" + farmId + "'!");
    }
}