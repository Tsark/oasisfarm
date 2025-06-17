package com.hybridiize.oasisfarm.commands.subcommands;

import com.hybridiize.oasisfarm.managers.PendingConfirmationManager;
import com.hybridiize.oasisfarm.managers.SelectionManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ConfirmCommand extends SubCommand {
    private final PendingConfirmationManager confirmationManager;
    private final SelectionManager selectionManager;

    public ConfirmCommand(PendingConfirmationManager confirmationManager, SelectionManager selectionManager) {
        this.confirmationManager = confirmationManager;
        this.selectionManager = selectionManager;
    }

    @Override
    public String getName() { return "confirm"; }
    @Override
    public String getDescription() { return "Confirms a pending action, like setregion."; }
    @Override
    public String getSyntax() { return "/of confirm"; }

    @Override
    public void perform(Player player, String[] args) {
        String farmId = confirmationManager.getPendingResize(player);
        if (farmId == null) {
            player.sendMessage(ChatColor.RED + "You have no pending action to confirm.");
            return;
        }

        if (!selectionManager.hasBothPositions(player)) {
            player.sendMessage(ChatColor.RED + "You must select two new positions with the wand before confirming.");
            return;
        }

        FileConfiguration config = plugin.getConfig();
        Location pos1 = selectionManager.getPos1(player);
        Location pos2 = selectionManager.getPos2(player);
        String path = "farms." + farmId + ".region";

        config.set(path + ".pos1", pos1.getBlockX() + "," + pos1.getBlockY() + "," + pos1.getBlockZ());
        config.set(path + ".pos2", pos2.getBlockX() + "," + pos2.getBlockY() + "," + pos2.getBlockZ());

        plugin.saveConfig();
        confirmationManager.clearPending(player);

        // Reload farm data in memory and update hologram
        plugin.getHologramManager().removeFarmHologram(farmId);
        plugin.getConfigManager().loadFarms();

        player.sendMessage(ChatColor.GREEN + "Successfully redefined region for farm '" + farmId + "'!");
    }
}