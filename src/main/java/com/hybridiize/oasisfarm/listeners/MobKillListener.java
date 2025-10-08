package com.hybridiize.oasisfarm.listeners;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.farm.MobInfo;
import com.hybridiize.oasisfarm.farm.TrackedMob;
import com.hybridiize.oasisfarm.rewards.RewardSet;
import io.lumine.mythic.bukkit.MythicBukkit; // Import the MythicMobs API
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MobKillListener implements Listener {

    private final Oasisfarm plugin;

    public MobKillListener(Oasisfarm plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity killedMob = event.getEntity();
        Player killer = killedMob.getKiller();

        // ** THE CRUCIAL FIX IS HERE **
        // Check if MythicMobs is enabled and if the killed entity is a MythicMob.
        // If it is, we MUST ignore this event and let MythicMobListener handle it.
        if (plugin.isMythicMobsEnabled() && MythicBukkit.inst().getAPIHelper().isMythicMob(killedMob)) {
            return; // Stop processing, let the other listener take over.
        }

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

        List<RewardSet> killerRewards = new ArrayList<>(mobInfo.getKillerRewards());
        processRewards(killer, killerRewards);
        plugin.getFarmManager().untrackMob(killedMob);
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