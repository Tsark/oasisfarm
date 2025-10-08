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
        //System.out.println("[DEBUG] MythicMobDeathEvent triggered.");

        if (!(event.getKiller() instanceof Player)) {
            System.out.println("[DEBUG] Killer is not a player. Aborting.");
            return;
        }

        Player killer = (Player) event.getKiller();
        //System.out.println("[DEBUG] Killer identified: " + killer.getName());

        if (!plugin.getFarmManager().isTrackedMob(event.getEntity())) {
            //System.out.println("[DEBUG] Mob is not tracked by OasisFarm. Aborting.");
            return;
        }

        TrackedMob trackedMob = plugin.getFarmManager().getTrackedMob(event.getEntity());
        if (trackedMob == null) return;

        String templateId = trackedMob.getTemplateId();
        MobInfo mobInfo = plugin.getConfigManager().getMobTemplate(templateId);
        //System.out.println("[DEBUG] Kill detected for template: " + templateId);

        if (mobInfo == null) {
            plugin.getFarmManager().untrackMob(event.getEntity());
            return;
        }

        // --- All checks passed, proceeding to process rewards ---
        //System.out.println("[DEBUG] All checks passed. Preparing to process rewards.");
        List<RewardSet> killerRewards = new ArrayList<>(mobInfo.getKillerRewards());
        processRewards(killer, killerRewards);
        plugin.getFarmManager().untrackMob(event.getEntity());
    }

    private void processRewards(Player player, List<RewardSet> rewardSets) {
        //System.out.println("[DEBUG] Now inside processRewards for player: " + player.getName());
        if (rewardSets == null || rewardSets.isEmpty()) {
            //System.out.println("[DEBUG] Reward sets list is null or empty. Nothing to process.");
            return;
        }

        //System.out.println("[DEBUG] Processing " + rewardSets.size() + " reward sets...");
        for (RewardSet set : rewardSets) {
            //System.out.println("[DEBUG] -> Checking reward set with chance: " + set.getChance());
            if (ThreadLocalRandom.current().nextDouble() <= set.getChance()) {
                //System.out.println("[DEBUG] --> Reward set PASSED chance check.");
                for (com.hybridiize.oasisfarm.rewards.Reward individualReward : set.getRewards()) {
                    //System.out.println("[DEBUG] ---> Evaluating individual reward of type: " + individualReward.getClass().getSimpleName());
                    if (ThreadLocalRandom.current().nextDouble() <= individualReward.getChance()) {
                        //System.out.println("[DEBUG] ----> Individual reward PASSED chance check. Giving reward to player.");
                        individualReward.give(player);
                    }
                }
            }
        }
    }
}