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
        Player killer = killedMob.getKiller();

        if (killer == null || !plugin.getFarmManager().isTrackedMob(killedMob)) {
            return;
        }

        // --- NEW: Identify the Mob Template ---
        String templateId = plugin.getFarmManager().getTrackedMobTemplateId(killedMob);
        if (templateId == null) return;

        MobInfo mobInfo = plugin.getConfigManager().getMobTemplate(templateId);
        if (mobInfo == null) {
            // This should not happen if the mob was tracked, but it's a good safety check.
            plugin.getFarmManager().untrackMob(killedMob);
            return;
        }

        // --- Permission Check ---
        if (mobInfo.getKillPermission() != null && !killer.hasPermission(mobInfo.getKillPermission())) {
            killer.sendMessage(ChatColor.RED + "You do not have permission to get rewards for killing this mob.");
            plugin.getFarmManager().untrackMob(killedMob);
            return;
        }

        // --- Cooldown Check ---
        // IMPORTANT: We now use the templateId for the cooldown key
        String mobIdentifier = mobInfo.getTemplateId();
        long currentTime = System.currentTimeMillis();
        long cooldownEnd = cooldowns.computeIfAbsent(killer.getUniqueId(), k -> new HashMap<>())
                .getOrDefault(mobIdentifier, 0L);

        if (currentTime < cooldownEnd) {
            long timeLeft = (cooldownEnd - currentTime) / 1000;
            killer.sendMessage(ChatColor.RED + "You are on cooldown for this mob type. Time left: " + timeLeft + "s");
            return;
        }

        // --- Broadcast Logic ---
        if (mobInfo.getBroadcastKill() != null && !mobInfo.getBroadcastKill().isEmpty()) {
            String broadcastMessage = PlaceholderAPI.setPlaceholders(killer, mobInfo.getBroadcastKill());
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcastMessage));
        }

        // --- Execute Reward Commands ---
        for (String command : mobInfo.getRewards()) {
            String parsedCommand = PlaceholderAPI.setPlaceholders(killer, command);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
        }

        // --- Set New Cooldown ---
        if (mobInfo.getKillCooldown() > 0) {
            long newCooldownEnd = currentTime + (mobInfo.getKillCooldown() * 1000L);
            cooldowns.get(killer.getUniqueId()).put(mobIdentifier, newCooldownEnd);
        }

        plugin.getFarmManager().untrackMob(killedMob);
    }
}