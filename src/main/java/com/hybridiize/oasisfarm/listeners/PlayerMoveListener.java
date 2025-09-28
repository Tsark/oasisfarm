package com.hybridiize.oasisfarm.listeners;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.farm.Farm;
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
        playerCurrentFarm.remove(event.getPlayer().getUniqueId());
        plugin.getRewardManager().handlePlayerQuit(event.getPlayer());
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
            if (enteredFarm != null) {
                processFarmEntry(player, enteredFarm);
            }
        }

        if (farmPlayerIsIn != null) {
            playerCurrentFarm.put(player.getUniqueId(), farmPlayerIsIn);
        } else {
            playerCurrentFarm.remove(player.getUniqueId());
        }
    }

    private void processFarmEntry(Player player, Farm farm) {
        if (farm.getEntryCooldown() <= 0) return;

        long currentTime = System.currentTimeMillis();
        long cooldownEnd = entryCooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .getOrDefault(farm.getId(), 0L);

        if (currentTime < cooldownEnd) {
            long timeLeft = (cooldownEnd - currentTime) / 1000;
            player.sendMessage(ChatColor.RED + "You cannot enter " + farm.getId() + " yet. Cooldown: " + timeLeft + "s");
            player.teleport(player.getWorld().getSpawnLocation());
            return;
        }

        long newCooldownEnd = currentTime + (farm.getEntryCooldown() * 1000L);
        entryCooldowns.get(player.getUniqueId()).put(farm.getId(), newCooldownEnd);
        player.sendMessage(ChatColor.AQUA + "You have entered " + farm.getId() + ". Entry cooldown is now active.");
    }

    private void handleBossBar(Player player, Location to) {
        // This is the new, efficient V2 logic
        Map<String, BossBar> activeBars = plugin.getEventManager().getActiveBossBars();

        for (Map.Entry<String, BossBar> entry : activeBars.entrySet()) {
            String farmId = entry.getKey();
            BossBar bar = entry.getValue();
            Farm farm = plugin.getConfigManager().getFarms().get(farmId);

            if (farm == null) continue;

            // We will add a configurable radius back in the future. For now, it's 100 blocks.
            double radius = 100;
            double radiusSquared = radius * radius;

            if (farm.getRegion().getCenter().getWorld().equals(to.getWorld()) &&
                    farm.getRegion().getCenter().distanceSquared(to) <= radiusSquared) {
                // Player is inside the radius, show the bar
                bar.addPlayer(player);
            } else {
                // Player is outside the radius, hide the bar
                bar.removePlayer(player);
            }
        }
    }
}