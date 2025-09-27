package com.hybridiize.oasisfarm.managers;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.event.ActiveEventTracker;
import com.hybridiize.oasisfarm.event.EventCondition;
import com.hybridiize.oasisfarm.event.EventPhase;
import com.hybridiize.oasisfarm.event.OasisEvent;
import com.hybridiize.oasisfarm.farm.Farm;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventManager {

    private final Oasisfarm plugin;
    // Map<EventID, CooldownEndTimeMillis>
    private final Map<String, Long> eventCooldowns = new ConcurrentHashMap<>();
    // Map<FarmID, ActiveEventID>
    private final Map<String, String> activeFarmEvents = new ConcurrentHashMap<>();
    // Map<EventID, ActiveEventTracker>
    private final Map<String, ActiveEventTracker> runningEventInstances = new ConcurrentHashMap<>();
    // Map<EventID, BossBar>
    private final Map<String, BossBar> eventBossBars = new ConcurrentHashMap<>();

    public EventManager(Oasisfarm plugin) {
        this.plugin = plugin;
        startEventTicker();
        startPhaseTicker();
    }

    private void startEventTicker() {
        long interval = plugin.getConfigManager().getEventsConfig().getLong("event-check-interval", 1200L);
        new BukkitRunnable() {
            @Override
            public void run() {
                checkForTriggerableEvents();
            }
        }.runTaskTimer(plugin, 20L * 15, interval);
    }

    private void startPhaseTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (runningEventInstances.isEmpty()) return;
                for (ActiveEventTracker tracker : new java.util.ArrayList<>(runningEventInstances.values())) {
                    if (tracker.isPhaseOver()) {
                        advanceEventPhase(tracker);
                    }
                }
                updateBossBars();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void checkForTriggerableEvents() {
        Map<String, OasisEvent> allEvents = plugin.getConfigManager().getEvents();
        for (OasisEvent event : allEvents.values()) {
            if (isEventRunning(event.getId())) continue;

            long currentTime = System.currentTimeMillis();
            if (eventCooldowns.getOrDefault(event.getId(), 0L) > currentTime) {
                continue;
            }

            if (areConditionsMet(event)) {
                startEvent(event);
                break;
            }
        }
    }

    private boolean areConditionsMet(OasisEvent event) {
        for (EventCondition condition : event.getConditions()) {
            switch (condition.getType().toUpperCase()) {
                case "MIN_PLAYERS":
                    int minPlayers = Integer.parseInt(condition.getValue());
                    if (Bukkit.getOnlinePlayers().size() < minPlayers) return false;
                    break;
                case "TIME_OF_DAY":
                    World farmWorld = getEventWorld(event);
                    if (farmWorld == null) return false;
                    long time = farmWorld.getTime();
                    String requiredTime = condition.getValue().toUpperCase();
                    boolean isDay = time >= 0 && time < 12300;
                    boolean isNight = time >= 12300 && time < 23850;
                    if (requiredTime.equals("DAY") && !isDay) return false;
                    if (requiredTime.equals("NIGHT") && !isNight) return false;
                    break;
                case "TOTAL_KILLS_IN_FARM":
                    int requiredKills = Integer.parseInt(condition.getValue());
                    String farmId = event.getTargetFarm();
                    if (farmId.equalsIgnoreCase("all")) {
                        plugin.getLogger().warning("TOTAL_KILLS_IN_FARM cannot be used with 'target-farm: all' for event '" + event.getId() + "'. Condition ignored.");
                        break;
                    }
                    int currentKills = plugin.getFarmDataManager().getKillCount(farmId);
                    if (currentKills < requiredKills) return false;
                    break;
                case "INTERVAL":
                    long intervalSeconds = Long.parseLong(condition.getValue());
                    long lastRanTimestamp = plugin.getEventDataManager().getLastRan(event.getId());
                    if (lastRanTimestamp == 0) break;
                    if (System.currentTimeMillis() - lastRanTimestamp < intervalSeconds * 1000L) return false;
                    break;
                default:
                    plugin.getLogger().warning("Unknown event condition type: " + condition.getType());
                    break;
            }
        }
        return true;
    }

    public void startEvent(OasisEvent event) {
        if (isEventRunning(event.getId())) return;

        for (EventCondition condition : event.getConditions()) {
            if (condition.getType().equalsIgnoreCase("INTERVAL")) {
                plugin.getEventDataManager().setLastRan(event.getId(), System.currentTimeMillis());
                break;
            }
        }

        plugin.getLogger().info("Starting event: " + event.getId());
        ActiveEventTracker tracker = new ActiveEventTracker(event);
        runningEventInstances.put(event.getId(), tracker);

        if (event.getTargetFarm().equalsIgnoreCase("all")) {
            for (String farmId : plugin.getConfigManager().getFarms().keySet()) {
                activeFarmEvents.put(farmId, event.getId());
            }
        } else {
            activeFarmEvents.put(event.getTargetFarm(), event.getId());
        }
        advanceEventPhase(tracker);
    }

    private void advanceEventPhase(ActiveEventTracker tracker) {
        EventPhase endedPhase = tracker.getCurrentPhase();
        if (endedPhase != null && endedPhase.getOnEndCommands() != null) {
            for (String command : endedPhase.getOnEndCommands()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ChatColor.translateAlternateColorCodes('&', command));
            }
        }

        EventPhase nextPhase = tracker.advanceToNextPhase();
        if (nextPhase == null) {
            endEvent(tracker.getEvent().getId());
            return;
        }

        BossBar bar = eventBossBars.get(tracker.getEvent().getId());
        if (bar == null) {
            bar = Bukkit.createBossBar("Event Starting...", BarColor.RED, BarStyle.SOLID);
            bar.setVisible(true);
            eventBossBars.put(tracker.getEvent().getId(), bar);
        }

        if (nextPhase.getOnStartCommands() != null) {
            for (String command : nextPhase.getOnStartCommands()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ChatColor.translateAlternateColorCodes('&', command));
            }
        }
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

    public void endEvent(String eventId) {
        endEvent(eventId, false);
    }

    private void endEvent(String eventId, boolean silent) {
        if (!isEventRunning(eventId)) return;
        OasisEvent event = runningEventInstances.get(eventId).getEvent();
        if (event == null) return;

        String farmId = event.getTargetFarm();
        if (!farmId.equalsIgnoreCase("all")) {
            for (EventCondition condition : event.getConditions()) {
                if (condition.getType().equalsIgnoreCase("TOTAL_KILLS_IN_FARM")) {
                    plugin.getFarmDataManager().resetKillCount(farmId);
                    if (!silent) {
                        plugin.getLogger().info("Resetting TOTAL_KILLS_IN_FARM for farm '" + farmId + "'.");
                    }
                    break;
                }
            }
        }

        if (!silent && event.getOnEndCommands() != null) {
            for (String command : event.getOnEndCommands()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ChatColor.translateAlternateColorCodes('&', command));
            }
        }

        if (!silent) {
            plugin.getLogger().info("Ending event: " + event.getId());
        }

        runningEventInstances.remove(eventId);
        activeFarmEvents.values().removeIf(id -> id.equals(eventId));

        BossBar bar = eventBossBars.remove(eventId);
        if (bar != null) {
            bar.setVisible(false);
            bar.removeAll();
        }

        long cooldownMillis = event.getCooldown() * 1000L;
        eventCooldowns.put(eventId, System.currentTimeMillis() + cooldownMillis);
    }

    public void stopAllEvents() {
        for (String eventId : new java.util.HashSet<>(runningEventInstances.keySet())) {
            endEvent(eventId, true);
        }
    }

    // --- HELPER METHODS FOR OTHER CLASSES ---

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

    private World getEventWorld(OasisEvent event) {
        if (event.getTargetFarm().equalsIgnoreCase("all")) {
            return Bukkit.getWorlds().get(0);
        } else {
            Farm farm = plugin.getConfigManager().getFarms().get(event.getTargetFarm());
            if (farm == null) return null;
            return farm.getRegion().getPos1().getWorld();
        }
    }

    public boolean isEventRunning(String eventId) {
        return runningEventInstances.containsKey(eventId);
    }

    public Map<String, OasisEvent> getRunningEvents() {
        Map<String, OasisEvent> events = new HashMap<>();
        for (ActiveEventTracker tracker : runningEventInstances.values()) {
            events.put(tracker.getEvent().getId(), tracker.getEvent());
        }
        return events;
    }

    public BossBar getBossBar(String eventId) {
        return eventBossBars.get(eventId);
    }

    public Map<String, Long> getEventCooldowns() {
        return eventCooldowns;
    }

    public Map<String, String> getActiveFarmEvents() {
        return Collections.unmodifiableMap(activeFarmEvents);
    }
}