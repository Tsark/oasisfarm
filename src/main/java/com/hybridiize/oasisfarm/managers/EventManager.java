package com.hybridiize.oasisfarm.managers;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.event.ActiveEventTracker;
import com.hybridiize.oasisfarm.event.EventCondition;
import com.hybridiize.oasisfarm.event.EventPhase;
import com.hybridiize.oasisfarm.event.OasisEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class EventManager {

    private final Oasisfarm plugin;
    private final Map<String, Long> eventCooldowns = new ConcurrentHashMap<>();
    private final Map<String, String> activeFarmEvents = new ConcurrentHashMap<>();
    private final Map<String, ActiveEventTracker> runningEventInstances = new ConcurrentHashMap<>();
    private final Map<String, BossBar> eventBossBars = new ConcurrentHashMap<>();

    public EventManager(Oasisfarm plugin) {
        this.plugin = plugin;
        startEventTicker();
        startPhaseTicker();
    }

    private void startEventTicker() {
        // Get the check interval from events.yml, default to 20 seconds (400 ticks)
        long interval = plugin.getConfigManager().getEventsConfig().getLong("event-check-interval", 400L);

        new BukkitRunnable() {
            @Override
            public void run() {
                checkForTriggerableEvents();
            }
        }.runTaskTimer(plugin, 20L * 15, interval); // Initial delay of 15 seconds
    }

    private void checkForTriggerableEvents() {
        // For now, this is a placeholder. In the next step we will add event activation logic here.
        // System.out.println("Checking for triggerable events...");

        Map<String, OasisEvent> allEvents = plugin.getConfigManager().getEvents();

        for (OasisEvent event : allEvents.values()) {
            // 1. Check Cooldown
            long currentTime = System.currentTimeMillis();
            if (eventCooldowns.getOrDefault(event.getId(), 0L) > currentTime) {
                continue; // Event is on cooldown, skip it
            }

            // 2. Check Conditions
            if (areConditionsMet(event)) {
                // In the future, this is where we will start the event.
                startEvent(event);
                break;
            }
        }
    }

    private void startPhaseTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Use a copy to avoid issues while iterating and removing
                for (ActiveEventTracker tracker : new java.util.ArrayList<>(runningEventInstances.values())) {
                    if (tracker.isPhaseOver()) {
                        advanceEventPhase(tracker);
                    }
                }
                updateBossBars();
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
    }

    private void advanceEventPhase(ActiveEventTracker tracker) {
        // This moves the tracker to the next phase index and returns the new phase object
        EventPhase nextPhase = tracker.advanceToNextPhase();

        // Check if the event is over (no more phases)
        if (nextPhase == null) {
            endEvent(tracker.getEvent().getId());
            return;
        }

        // --- BOSS BAR LOGIC ---
        // Get the Boss Bar associated with this event
        BossBar bar = eventBossBars.get(tracker.getEvent().getId());
        if (bar == null) {
            // If this is the first phase, the bar won't exist yet, so we create it.
            bar = Bukkit.createBossBar("Event Starting...", BarColor.RED, BarStyle.SOLID);
            bar.setVisible(true);
            eventBossBars.put(tracker.getEvent().getId(), bar);
        }

        // Update the list of players who can see the bar.
        // We do this every phase in case players have logged on or off.
        bar.removeAll(); // Clear old players
        for (Player p : Bukkit.getOnlinePlayers()) {
            bar.addPlayer(p);
        }
        // --- END OF BOSS BAR LOGIC ---

        // Run on-start commands for the new phase
        if (nextPhase.getOnStartCommands() != null) {
            for (String command : nextPhase.getOnStartCommands()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ChatColor.translateAlternateColorCodes('&', command));
            }
        }
    }

    public void endEvent(String eventId) {
        OasisEvent event = runningEventInstances.get(eventId).getEvent();
        if (event == null) return;

        plugin.getLogger().info("Ending event: " + event.getId());

        // Remove the event from the active lists
        runningEventInstances.remove(eventId);
        activeFarmEvents.values().removeIf(id -> id.equals(eventId));

        // Put the event on cooldown
        long cooldownMillis = event.getCooldown() * 1000L;
        eventCooldowns.put(eventId, System.currentTimeMillis() + cooldownMillis);

        BossBar bar = eventBossBars.remove(eventId);
        if (bar != null) {
            bar.setVisible(false);
            bar.removeAll();
        }
        // We could add on-end commands here in the future
    }

    private void updateBossBars() {
        for (ActiveEventTracker tracker : runningEventInstances.values()) {
            BossBar bar = eventBossBars.get(tracker.getEvent().getId());
            EventPhase phase = tracker.getCurrentPhase();
            if (bar != null && phase != null) {
                long timeLeft = tracker.getPhaseEndTime() - System.currentTimeMillis();
                double progress = Math.max(0, (double) timeLeft / (phase.getDuration() * 1000.0));
                bar.setProgress(progress);

                String title = String.format("&c&l%s &7- &ePhase %d &7- &fTime Left: %ds",
                        tracker.getEvent().getId().replace("_", " "),
                        tracker.getCurrentPhaseIndex() + 1,
                        timeLeft / 1000);
                bar.setTitle(ChatColor.translateAlternateColorCodes('&', title));
            }
        }
    }

    private boolean areConditionsMet(OasisEvent event) {
        for (EventCondition condition : event.getConditions()) {
            // We use a switch on the condition type to check each one.
            // This is very expandable for the future.
            switch (condition.getType().toUpperCase()) {
                case "MIN_PLAYERS":
                    int minPlayers = Integer.parseInt(condition.getValue());
                    if (Bukkit.getOnlinePlayers().size() < minPlayers) {
                        return false; // Condition not met
                    }
                    break;

                case "TIME_OF_DAY":
                    // We'll check this against the time in the farm's world.
                    World farmWorld = getEventWorld(event);
                    if (farmWorld == null) return false; // Can't find the world, so can't check time.

                    long time = farmWorld.getTime();
                    String requiredTime = condition.getValue().toUpperCase();

                    boolean isDay = time >= 0 && time < 12300;
                    boolean isNight = time >= 12300 && time < 23850;

                    if (requiredTime.equals("DAY") && !isDay) return false;
                    if (requiredTime.equals("NIGHT") && !isNight) return false;
                    break;

                // We will add more conditions like PAPI_CHECK here later.
                default:
                    // Unknown condition type, log a warning and assume it's met.
                    plugin.getLogger().warning("Unknown event condition type: " + condition.getType());
                    break;
            }
        }
        return true; // All conditions were met
    }

    // Helper method to find the world an event is supposed to run in.
    private World getEventWorld(OasisEvent event) {
        if (event.getTargetFarm().equalsIgnoreCase("all")) {
            // If it's for all farms, just default to the main overworld.
            return Bukkit.getWorlds().get(0);
        } else {
            // Find the specific farm and get its world.
            return Objects.requireNonNull(plugin.getConfigManager().getFarms().get(event.getTargetFarm()))
                    .getRegion().getPos1().getWorld();
        }
    }

    public void startEvent(OasisEvent event) {
        if (runningEventInstances.containsKey(event.getId())) return;

        plugin.getLogger().info("Starting event: " + event.getId());

        // Create the tracker for the new event
        ActiveEventTracker tracker = new ActiveEventTracker(event);
        runningEventInstances.put(event.getId(), tracker);

        // Assign the event to its target farm(s)
        if (event.getTargetFarm().equalsIgnoreCase("all")) {
            for (String farmId : plugin.getConfigManager().getFarms().keySet()) {
                activeFarmEvents.put(farmId, event.getId());
            }
        } else {
            activeFarmEvents.put(event.getTargetFarm(), event.getId());
        }

        // Advance to the first phase immediately
        advanceEventPhase(tracker);
    }

    public boolean isEventActive(String farmId) {
        return activeFarmEvents.containsKey(farmId);
    }

    public EventPhase getCurrentPhase(String farmId) {
        String eventId = activeFarmEvents.get(farmId);
        if (eventId == null) return null;

        ActiveEventTracker tracker = runningEventInstances.get(eventId);
        if (tracker == null) return null;

        return tracker.getCurrentPhase();
    }

    public boolean isEventRunning(String eventId) {
        return runningEventInstances.containsKey(eventId);
    }

    public Map<String, String> getRunningEventFarmMap() {
        return activeFarmEvents;
    }

    public Map<String, Long> getEventCooldowns() {
        return eventCooldowns;
    }


}