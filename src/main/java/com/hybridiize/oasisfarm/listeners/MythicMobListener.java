package com.hybridiize.oasisfarm.listeners;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.farm.Farm;
import com.hybridiize.oasisfarm.farm.MobInfo;
import com.hybridiize.oasisfarm.farm.TrackedMob;
import com.hybridiize.oasisfarm.rewards.RewardSet;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class MythicMobListener implements Listener {

    private final Oasisfarm plugin;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public MythicMobListener(Oasisfarm plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        if (!(event.getKiller() instanceof Player)) {
            return;
        }

        Player killer = (Player) event.getKiller();

        if (!plugin.getFarmManager().isTrackedMob(event.getEntity())) {
            return;
        }

        TrackedMob trackedMob = plugin.getFarmManager().getTrackedMob(event.getEntity());
        if (trackedMob == null) return;

        String templateId = trackedMob.getTemplateId();
        MobInfo mobInfo = plugin.getConfigManager().getMobTemplate(templateId);

        if (mobInfo == null) {
            plugin.getFarmManager().untrackMob(event.getEntity());
            return;
        }

        if (mobInfo.getKillPermission() != null && !killer.hasPermission(mobInfo.getKillPermission())) {
            killer.sendMessage(ChatColor.RED + "You do not have permission to get rewards for killing this mob.");
            plugin.getFarmManager().untrackMob(event.getEntity());
            return;
        }

        String mobIdentifier = mobInfo.getTemplateId();
        long currentTime = System.currentTimeMillis();
        long cooldownEnd = cooldowns.computeIfAbsent(killer.getUniqueId(), k -> new HashMap<>())
                .getOrDefault(mobIdentifier, 0L);

        if (currentTime < cooldownEnd) {
            long timeLeft = (cooldownEnd - currentTime) / 1000;
            killer.sendMessage(ChatColor.RED + "You are on cooldown for this mob type. Time left: " + timeLeft + "s");
            plugin.getFarmManager().untrackMob(event.getEntity());
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

        if (!plugin.getEventManager().isFarmRunningEvent(trackedMob.getFarmId())) {
            plugin.getFarmDataManager().incrementKillCount(trackedMob.getFarmId());
        }

        plugin.getFarmManager().untrackMob(event.getEntity());
    }

    private void processRewards(Player player, List<RewardSet> rewardSets) {
        if (rewardSets == null || rewardSets.isEmpty()) {
            return;
        }

        for (RewardSet set : rewardSets) {
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