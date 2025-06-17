package com.hybridiize.oasisfarm.listeners;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.farm.Farm;
import com.hybridiize.oasisfarm.farm.MobInfo;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MobKillListener implements Listener {

    private final Oasisfarm plugin;
    // Map<PlayerUUID, Map<MobType, CooldownEndTime>>
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public MobKillListener(Oasisfarm plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity killedMob = event.getEntity();
        Player killer = killedMob.getKiller(); // This is null if it wasn't killed by a player

        // --- Step 1: Basic Checks ---
        // Was it killed by a player? Is the mob one of ours?
        if (killer == null || !plugin.getFarmManager().isTrackedMob(killedMob)) {
            return;
        }

        // --- Step 2: Identify the Mob and its Farm ---
        // We need to find the specific MobInfo that corresponds to the mob that was just killed.
        // This is a bit complex, but necessary to get the right rewards and cooldowns.
        Farm farm = null;
        MobInfo mobInfo = null;

        for (Farm currentFarm : plugin.getConfigManager().getFarms().values()) {
            for (MobInfo currentMobInfo : currentFarm.getMobInfoList()) {
                if (currentMobInfo.getType() == killedMob.getType()) {
                    // This logic assumes mob types are unique per farm for now.
                    // We can refine this later if needed.
                    farm = currentFarm;
                    mobInfo = currentMobInfo;
                    break;
                }
            }
            if (farm != null) break;
        }

        if (farm == null || mobInfo == null) {
            return; // Couldn't find matching farm/mob info, something is wrong.
        }

        // --- Step 3: Cooldown Check ---
        String mobIdentifier = mobInfo.getType().name(); // e.g., "ZOMBIE"
        long currentTime = System.currentTimeMillis();
        long cooldownEnd = cooldowns.computeIfAbsent(killer.getUniqueId(), k -> new HashMap<>())
                .getOrDefault(mobIdentifier, 0L);

        if (currentTime < cooldownEnd) {
            long timeLeft = (cooldownEnd - currentTime) / 1000;
            killer.sendMessage(ChatColor.RED + "You are on cooldown for this mob type. Time left: " + timeLeft + "s");
            return;
        }

        // --- Step 4: Execute Reward Commands ---
        for (String command : mobInfo.getRewards()) {
            // Replace placeholders using PlaceholderAPI
            String parsedCommand = PlaceholderAPI.setPlaceholders(killer, command);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
        }

        // --- Step 5: Set New Cooldown & Untrack Mob ---
        if (mobInfo.getKillCooldown() > 0) {
            long newCooldownEnd = currentTime + (mobInfo.getKillCooldown() * 1000L);
            cooldowns.get(killer.getUniqueId()).put(mobIdentifier, newCooldownEnd);
        }

        // The mob is dead, so we remove it from our tracking system.
        plugin.getFarmManager().untrackMob(killedMob);
    }
}