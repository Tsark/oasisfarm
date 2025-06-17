package com.hybridiize.oasisfarm.listeners;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.farm.Farm;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMoveListener implements Listener {
    private final Oasisfarm plugin;
    // Map<PlayerUUID, Map<FarmID, CooldownEndTime>>
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    // Tracks which farm a player is currently in to prevent spam
    private final Map<UUID, String> playerCurrentFarm = new HashMap<>();

    public PlayerMoveListener(Oasisfarm plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Optimization: only check if the player moved to a new block
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY()) {
            return;
        }

        Player player = event.getPlayer();
        Location to = event.getTo();
        String farmPlayerIsIn = null;

        for (Farm farm : plugin.getConfigManager().getFarms().values()) {
            if (farm.getRegion().contains(to)) {
                farmPlayerIsIn = farm.getId();
                String previouslyInFarm = playerCurrentFarm.get(player.getUniqueId());

                // Check if player is entering a NEW farm
                if (!farm.getId().equals(previouslyInFarm)) {
                    handleFarmEntry(player, farm);
                }
                break;
            }
        }

        // Update the player's current location status
        if (farmPlayerIsIn != null) {
            playerCurrentFarm.put(player.getUniqueId(), farmPlayerIsIn);
        } else {
            playerCurrentFarm.remove(player.getUniqueId());
        }
    }

    private void handleFarmEntry(Player player, Farm farm) {
        if (farm.getEntryCooldown() <= 0) return; // No cooldown for this farm

        long currentTime = System.currentTimeMillis();
        long cooldownEnd = cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .getOrDefault(farm.getId(), 0L);

        if (currentTime < cooldownEnd) {
            long timeLeft = (cooldownEnd - currentTime) / 1000;
            player.sendMessage(ChatColor.RED + "You cannot enter " + farm.getId() + " yet. Cooldown: " + timeLeft + "s");
            // Teleport player back out (e.g., to their previous location)
            player.teleport(player.getWorld().getSpawnLocation()); // Or some other safe spot
            return;
        }

        // Set a new cooldown for the player for this farm
        long newCooldownEnd = currentTime + (farm.getEntryCooldown() * 1000L);
        cooldowns.get(player.getUniqueId()).put(farm.getId(), newCooldownEnd);
        player.sendMessage(ChatColor.AQUA + "You have entered " + farm.getId() + ". Entry cooldown is now active.");
    }
}