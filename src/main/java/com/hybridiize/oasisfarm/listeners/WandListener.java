package com.hybridiize.oasisfarm.listeners;

import com.hybridiize.oasisfarm.managers.SelectionManager;
import com.hybridiize.oasisfarm.util.Constants;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class WandListener implements Listener {

    private final SelectionManager selectionManager;

    public WandListener(SelectionManager selectionManager) {
        this.selectionManager = selectionManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // Check if the player is holding our specific wand
        if (!itemInHand.isSimilar(Constants.WAND_ITEM)) {
            return;
        }

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true); // Prevents the block from breaking
            Location loc = event.getClickedBlock().getLocation();
            selectionManager.setPos1(player, loc);
            player.sendMessage(ChatColor.GREEN + "Position 1 set to (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")");
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true); // Prevents any other action
            Location loc = event.getClickedBlock().getLocation();
            selectionManager.setPos2(player, loc);
            player.sendMessage(ChatColor.GREEN + "Position 2 set to (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")");
        }
    }
}