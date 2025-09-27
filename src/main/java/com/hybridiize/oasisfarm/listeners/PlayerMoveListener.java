package com.hybridiize.oasisfarm.listeners;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.event.OasisEvent;
import com.hybridiize.oasisfarm.farm.Farm;
import com.hybridiize.oasisfarm.managers.EventManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMoveListener implements Listener {
    private final Oasisfarm plugin;
    private final Map<UUID, Map<String, Long>> entryCooldowns = new HashMap<>();
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
        handleEntryExit(player, event.getTo());
        handleBossBar(player, event.getTo());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up data for players who log out
        playerCurrentFarm.remove(event.getPlayer().getUniqueId());
        plugin.getRewardManager().handlePlayerQuit(event.getPlayer());
        // We don't need to manually remove them from boss bars, Bukkit handles that.
    }

    private void handleEntryExit(Player player, Location to) {
        String farmPlayerIsIn = null;
        Farm enteredFarm = null;
        for (Farm farm : plugin.getConfigManager().getFarms().values()) {
            if (farm.getRegion().contains(to)) {
                farmPlayerIsIn = farm.getId();
                enteredFarm = farm;
                break;
            }
        }

        String previouslyInFarm = playerCurrentFarm.get(player.getUniqueId());

        if (farmPlayerIsIn != null && !farmPlayerIsIn.equals(previouslyInFarm)) {
            // Player has entered a new farm zone
            if (enteredFarm != null) {
                processFarmEntry(player, enteredFarm);
            }
        }

        // Update player's current farm status
        if (farmPlayerIsIn != null) {
            playerCurrentFarm.put(player.getUniqueId(), farmPlayerIsIn);
        } else {
            playerCurrentFarm.remove(player.getUniqueId());
        }
    }

    private void processFarmEntry(Player player, Farm farm) {
        // Entry Cooldown Logic
        if (farm.getEntryCooldown() <= 0) return;

        long currentTime = System.currentTimeMillis();
        long cooldownEnd = entryCooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .getOrDefault(farm.getId(), 0L);

        if (currentTime < cooldownEnd) {
            long timeLeft = (cooldownEnd - currentTime) / 1000;
            player.sendMessage(ChatColor.RED + "You cannot enter " + farm.getId() + " yet. Cooldown: " + timeLeft + "s");
            // A safe teleport location can be the world spawn or a configured point
            player.teleport(player.getWorld().getSpawnLocation());
            return;
        }

        long newCooldownEnd = currentTime + (farm.getEntryCooldown() * 1000L);
        entryCooldowns.get(player.getUniqueId()).put(farm.getId(), newCooldownEnd);
        player.sendMessage(ChatColor.AQUA + "You have entered " + farm.getId() + ". Entry cooldown is now active.");
    }

    private void handleBossBar(Player player, Location to) {
        EventManager eventManager = plugin.getEventManager();

        for (OasisEvent event : eventManager.getRunningEvents().values()) {
            Farm farm = plugin.getConfigManager().getFarms().get(event.getTargetFarm());
            if (farm == null) continue;

            BossBar bar = eventManager.getBossBar(event.getId());
            if (bar == null) continue;

            Location farmCenter = farm.getRegion().getCenter();
            double radius = event.getEventRadius();

            if (farmCenter.getWorld().equals(to.getWorld()) && farmCenter.distanceSquared(to) <= radius * radius) {
                // Player is inside the radius of this event, show the bar
                bar.addPlayer(player);
            } else {
                // Player is outside the radius of this event, hide the bar
                bar.removePlayer(player);
            }
        }
    }
}