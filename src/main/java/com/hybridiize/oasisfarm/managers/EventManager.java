package com.hybridiize.oasisfarm.managers;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.event.EventCondition;
import com.hybridiize.oasisfarm.event.EventPhase;
import com.hybridiize.oasisfarm.event.OasisEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class EventManager {

    private final Oasisfarm plugin;
    private final Map<String, Long> eventCooldowns = new ConcurrentHashMap<>();
    private final Map<String, String> activeFarmEvents = new ConcurrentHashMap<>();
    private final Map<String, OasisEvent> runningEventInstances = new ConcurrentHashMap<>();

    public EventManager(Oasisfarm plugin) {
        this.plugin = plugin;
        startEventTicker();
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
        // Prevent starting an event that's somehow already running
        if (runningEventInstances.containsKey(event.getId())) return;

        plugin.getLogger().info("Starting event: " + event.getId());
        runningEventInstances.put(event.getId(), event);

        // Assign the event to its target farm(s)
        if (event.getTargetFarm().equalsIgnoreCase("all")) {
            for (String farmId : plugin.getConfigManager().getFarms().keySet()) {
                activeFarmEvents.put(farmId, event.getId());
            }
        } else {
            activeFarmEvents.put(event.getTargetFarm(), event.getId());
        }

        // For now, we'll just handle the first phase's on-start commands.
        // In the next step, we'll build the full phase-transition logic.
        if (!event.getPhases().isEmpty()) {
            EventPhase firstPhase = event.getPhases().get(0);
            if (firstPhase.getOnStartCommands() != null) {
                for (String command : firstPhase.getOnStartCommands()) {
                    // We can use PAPI here too if we want, but for now, we'll keep it simple
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ChatColor.translateAlternateColorCodes('&', command));
                }
            }
        }

        // Put the event on cooldown
        long cooldownMillis = event.getCooldown() * 1000L;
        eventCooldowns.put(event.getId(), System.currentTimeMillis() + cooldownMillis);
    }

    public boolean isEventActive(String farmId) {
        return activeFarmEvents.containsKey(farmId);
    }

    public EventPhase getCurrentPhase(String farmId) {
        String eventId = activeFarmEvents.get(farmId);
        if (eventId == null) return null;

        OasisEvent event = runningEventInstances.get(eventId);
        if (event == null || event.getPhases().isEmpty()) return null;

        // For now, we only have one phase. We will expand this in the next step.
        return event.getPhases().get(0);
    }
}