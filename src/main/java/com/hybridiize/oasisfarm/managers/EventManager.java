package com.hybridiize.oasisfarm.managers;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.event.v2.ActiveEventTrackerV2;
import com.hybridiize.oasisfarm.event.v2.OasisEventV2;
import com.hybridiize.oasisfarm.farm.Farm;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventManager {

    private final Oasisfarm plugin;
    // Map<FarmID, ActiveEventTrackerV2> - Now tracks events on a per-farm basis.
    private final Map<String, ActiveEventTrackerV2> activeFarmEvents = new ConcurrentHashMap<>();
    private final Map<String, BossBar> activeBossBars = new ConcurrentHashMap<>();

    public EventManager(Oasisfarm plugin) {
        this.plugin = plugin;
        startEventTicker();
        startBossBarTicker();
    }
    private void startBossBarTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeBossBars.isEmpty()) return;

                for (Map.Entry<String, ActiveEventTrackerV2> entry : activeFarmEvents.entrySet()) {
                    String farmId = entry.getKey();
                    ActiveEventTrackerV2 tracker = entry.getValue();
                    BossBar bar = activeBossBars.get(farmId);

                    if (bar != null && tracker.getCurrentPhase() != null) {
                        String eventName = tracker.getEvent().getId().replace("_", " ");
                        String phaseName = tracker.getCurrentPhase().getPhaseId().replace("_", " ");

                        // We will add a timer back in a future update if needed.
                        String title = String.format("&c&l%s &7- &e%s", eventName, phaseName);
                        bar.setTitle(ChatColor.translateAlternateColorCodes('&', title));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Update every second
    }

    public Map<String, BossBar> getActiveBossBars() {
        return java.util.Collections.unmodifiableMap(activeBossBars);
    }

    private void startEventTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // The new ticker is farm-centric
                for (Farm farm : plugin.getConfigManager().getFarms().values()) {
                    if (activeFarmEvents.containsKey(farm.getId())) {
                        // If farm has an event, check if it should advance to the next phase
                        checkPhaseProgression(activeFarmEvents.get(farm.getId()));
                    } else {
                        // If farm has no event, check if a new one should start
                        checkForTriggerableEvents(farm);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L * 5, 20L * 5); // Check every 5 seconds
    }

    private void checkForTriggerableEvents(Farm farm) {
        // This code will now work!
        List<String> possibleEventIds = plugin.getConfigManager().getPossibleEventsForFarm(farm.getId());
        for (String eventId : possibleEventIds) {
            OasisEventV2 event = plugin.getConfigManager().getEventV2(eventId);
            if (event != null) {
                // Pass null for the tracker since no event is active yet
                boolean triggered = plugin.getConditionManager().areConditionsMet(event.getTrigger().getConditions(), event.getTrigger().getMode(), farm, null);
                if (triggered) {
                    startEvent(event, farm);
                    break; // Start only one event per check
                }
            }
        }
    }

    private void checkPhaseProgression(ActiveEventTrackerV2 tracker) {
        if (tracker.hasEnded()) {
            endEvent(tracker);
            return;
        }

        // Get the current phase's progression rules
        com.hybridiize.oasisfarm.event.v2.PhaseProgression progression = tracker.getCurrentPhase().getProgression();
        if (progression == null || progression.getConditions().isEmpty()) {
            return; // This phase has no way to end, it's a "forever" phase until stopped manually.
        }

        // Ask the ConditionManager if it's time to advance, passing the tracker
        boolean shouldAdvance = plugin.getConditionManager().areConditionsMet(progression.getConditions(), progression.getMode(), tracker.getFarm(), tracker);

        if (shouldAdvance) {
            advanceEventPhase(tracker);
        }
    }

    public void startEvent(OasisEventV2 event, Farm farm) {
        plugin.getLogger().info("Starting event '" + event.getId() + "' in farm '" + farm.getId() + "'!");
        ActiveEventTrackerV2 tracker = new ActiveEventTrackerV2(event, farm);
        activeFarmEvents.put(farm.getId(), tracker);
        advanceEventPhase(tracker);
        BossBar bar = Bukkit.createBossBar("Event Starting...", BarColor.RED, BarStyle.SOLID);
        bar.setVisible(true);
        activeBossBars.put(farm.getId(), bar);
    }

    /**
     * Checks if a specific farm currently has an active V2 event.
     * @param farmId The ID of the farm to check.
     * @return True if an event is running in the farm, false otherwise.
     */
    public boolean isFarmRunningEvent(String farmId) {
        return activeFarmEvents.containsKey(farmId);
    }

    /**
     * Gets the active event tracker for a specific farm.
     * @param farmId The ID of the farm.
     * @return The ActiveEventTrackerV2, or null if no event is running.
     */
    public ActiveEventTrackerV2 getActiveEventTracker(String farmId) {
        return activeFarmEvents.get(farmId);
    }


    /**
     * Gets an unmodifiable view of the currently active farm events.
     * @return A map where the key is the Farm ID and the value is the event tracker.
     */
    public Map<String, com.hybridiize.oasisfarm.event.v2.ActiveEventTrackerV2> getActiveFarmEvents() {
        return java.util.Collections.unmodifiableMap(activeFarmEvents);
    }

    private void advanceEventPhase(ActiveEventTrackerV2 tracker) {
        // Execute end-of-phase actions if this is not the first phase
        if (tracker.getCurrentPhase() != null) {
            // We will implement "on-end" actions later if needed.
        }

        // Advance to the next phase
        com.hybridiize.oasisfarm.event.v2.EventPhaseV2 nextPhase = tracker.advanceToNextPhase();

        if (nextPhase == null) {
            endEvent(tracker);
            return;
        }

        plugin.getLogger().info("Event '" + tracker.getEvent().getId() + "' in farm '" + tracker.getFarm().getId() + "' advancing to phase '" + nextPhase.getPhaseId() + "'.");

        // Execute the actions for the new phase
        // This part needs to be built in Step 4.
        executePhaseActions(nextPhase.getActions(), tracker.getFarm());
    }

    private void endEvent(ActiveEventTrackerV2 tracker) {
        plugin.getLogger().info("Event '" + tracker.getEvent().getId() + "' has ended in farm '" + tracker.getFarm().getId() + "'.");
        activeFarmEvents.remove(tracker.getFarm().getId());
        BossBar bar = activeBossBars.remove(tracker.getFarm().getId());
        if (bar != null) {
            bar.setVisible(false);
            bar.removeAll(); // Clear all players from the bar
        }
    }

    // In EventManager.java

    /**
     * Manually stops an event running in a specific farm.
     * @param farmId The ID of the farm where the event should be stopped.
     */
    public void stopEventInFarm(String farmId) {
        ActiveEventTrackerV2 tracker = activeFarmEvents.get(farmId);
        if (tracker != null) {
            endEvent(tracker);
        }
    }

    public void stopAllEvents() {
        if (activeFarmEvents.isEmpty()) {
            return;
        }

        plugin.getLogger().info("Stopping all active OasisFarm events...");
        // Create a copy of the trackers to avoid issues while modifying the map
        java.util.Collection<com.hybridiize.oasisfarm.event.v2.ActiveEventTrackerV2> trackers = new java.util.ArrayList<>(activeFarmEvents.values());

        for (com.hybridiize.oasisfarm.event.v2.ActiveEventTrackerV2 tracker : trackers) {
            endEvent(tracker);
        }
        plugin.getLogger().info("All events have been stopped.");
    }

    // In EventManager.java
    private void executePhaseActions(java.util.List<com.hybridiize.oasisfarm.event.v2.PhaseAction> actions, Farm farm) {
        if (actions == null) return;

        for (com.hybridiize.oasisfarm.event.v2.PhaseAction action : actions) {
            if (action instanceof com.hybridiize.oasisfarm.event.v2.action.BroadcastAction) {
                com.hybridiize.oasisfarm.event.v2.action.BroadcastAction broadcastAction = (com.hybridiize.oasisfarm.event.v2.action.BroadcastAction) action;
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcastAction.getMessage()));

            } else if (action instanceof com.hybridiize.oasisfarm.event.v2.action.CommandAction) {
                com.hybridiize.oasisfarm.event.v2.action.CommandAction commandAction = (com.hybridiize.oasisfarm.event.v2.action.CommandAction) action;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandAction.getCommand());

            } else if (action instanceof com.hybridiize.oasisfarm.event.v2.action.SpawnOnceAction) {
                com.hybridiize.oasisfarm.event.v2.action.SpawnOnceAction spawnOnceAction = (com.hybridiize.oasisfarm.event.v2.action.SpawnOnceAction) action;
                for (Map<String, Object> mobData : spawnOnceAction.getMobsToSpawn()) {
                    String mobId = (String) mobData.get("mob_id");
                    int amount = (int) mobData.get("amount");
                    plugin.getFarmManager().spawnSpecificMob(farm, mobId, amount);
                }
            }
        }
    }

}