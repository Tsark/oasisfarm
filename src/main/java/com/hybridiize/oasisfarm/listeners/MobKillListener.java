package com.hybridiize.oasisfarm.listeners;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.event.v2.ActiveEventTrackerV2;
import com.hybridiize.oasisfarm.farm.Farm;
import com.hybridiize.oasisfarm.farm.MobInfo;
import com.hybridiize.oasisfarm.farm.TrackedMob;
import com.hybridiize.oasisfarm.managers.EventManager;
import com.hybridiize.oasisfarm.rewards.RewardSet;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class MobKillListener implements Listener {

    private final Oasisfarm plugin;
    // Map<PlayerUUID, Map<TemplateID, CooldownEndTime>>
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

        TrackedMob trackedMob = plugin.getFarmManager().getTrackedMob(killedMob);
        if (trackedMob == null) return;

        String templateId = trackedMob.getTemplateId();
        MobInfo mobInfo = plugin.getConfigManager().getMobTemplate(templateId);

        if (mobInfo == null) {
            plugin.getFarmManager().untrackMob(killedMob);
            return;
        }

        // Handle Event Kill Tracking
        EventManager eventManager = plugin.getEventManager();
        if (eventManager.isFarmRunningEvent(trackedMob.getFarmId())) {
            ActiveEventTrackerV2 tracker = eventManager.getActiveEventTracker(trackedMob.getFarmId());
            if (tracker != null) {
                tracker.incrementMobKills(templateId);
            }
        } else {
            // Only increment total kills if NO event is running
            plugin.getFarmDataManager().incrementKillCount(trackedMob.getFarmId());
        }


        if (mobInfo.getKillPermission() != null && !killer.hasPermission(mobInfo.getKillPermission())) {
            killer.sendMessage(ChatColor.RED + "You do not have permission to get rewards for killing this mob.");
            plugin.getFarmManager().untrackMob(killedMob);
            return;
        }

        String mobIdentifier = mobInfo.getTemplateId();
        long currentTime = System.currentTimeMillis();
        long cooldownEnd = cooldowns.computeIfAbsent(killer.getUniqueId(), k -> new HashMap<>())
                .getOrDefault(mobIdentifier, 0L);

        if (currentTime < cooldownEnd) {
            long timeLeft = (cooldownEnd - currentTime) / 1000;
            killer.sendMessage(ChatColor.RED + "You are on cooldown for this mob type. Time left: " + timeLeft + "s");
            plugin.getFarmManager().untrackMob(killedMob);
            return;
        }

        plugin.getRewardManager().incrementKillCount(killer);

        List<RewardSet> killerRewards = new ArrayList<>(mobInfo.getKillerRewards());
        List<RewardSet> farmWideRewards = new ArrayList<>(mobInfo.getFarmWideRewards());

        processRewards(killer, killerRewards);

        if (!farmWideRewards.isEmpty()) {
            Farm farm = plugin.getConfigManager().getFarms().get(trackedMob.getFarmId());
            if (farm != null) {
                for (Player playerInFarm : Bukkit.getOnlinePlayers()) {
                    if (farm.getRegion().contains(playerInFarm.getLocation())) {
                        processRewards(playerInFarm, farmWideRewards);
                    }
                }
            }
        }

        if (mobInfo.getBroadcastKill() != null && !mobInfo.getBroadcastKill().isEmpty()) {
            String broadcastMessage = PlaceholderAPI.setPlaceholders(killer, mobInfo.getBroadcastKill());
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcastMessage));
        }

        if (mobInfo.getKillCooldown() > 0) {
            long newCooldownEnd = currentTime + (mobInfo.getKillCooldown() * 1000L);
            cooldowns.get(killer.getUniqueId()).put(mobIdentifier, newCooldownEnd);
        }

        plugin.getFarmManager().untrackMob(killedMob);
    }

    private void processRewards(Player player, List<com.hybridiize.oasisfarm.rewards.RewardSet> rewardSets) {
        if (rewardSets == null || rewardSets.isEmpty()) {
            return;
        }

        for (com.hybridiize.oasisfarm.rewards.RewardSet set : rewardSets) {
            if (ThreadLocalRandom.current().nextDouble() <= set.getChance()) {
                for (com.hybridiize.oasisfarm.rewards.Reward individualReward : set.getRewards()) {
                    if (ThreadLocalRandom.current().nextDouble() <= individualReward.getChance()) {
                        individualReward.give(player);
                    }
                }
            }
        }
    }
}