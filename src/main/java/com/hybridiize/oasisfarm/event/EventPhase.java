package com.hybridiize.oasisfarm.event;

import java.util.List;
import java.util.Map;

// A container for a single phase of an event
public class EventPhase {
    private final int duration;
    private final List<String> onStartCommands;
    private final double spawnIntervalModifier;
    private final boolean clearExistingMobs;
    private final int maxMobsOverride;
    private final Map<String, Double> addMobs;
    private final Map<String, Double> setMobs;
    private final Map<String, List<String>> modifyRewards;

    public EventPhase(int duration, List<String> onStartCommands, double spawnIntervalModifier, boolean clearExistingMobs, int maxMobsOverride, Map<String, Double> addMobs, Map<String, Double> setMobs, Map<String, List<String>> modifyRewards) {
        this.duration = duration;
        this.onStartCommands = onStartCommands;
        this.spawnIntervalModifier = spawnIntervalModifier;
        this.clearExistingMobs = clearExistingMobs;
        this.maxMobsOverride = maxMobsOverride;
        this.addMobs = addMobs;
        this.setMobs = setMobs;
        this.modifyRewards = modifyRewards;
    }

    // Getters for all fields...
    public int getDuration() { return duration; }
    public List<String> getOnStartCommands() { return onStartCommands; }
    public double getSpawnIntervalModifier() { return spawnIntervalModifier; }
    public boolean shouldClearExistingMobs() { return clearExistingMobs; }
    public int getMaxMobsOverride() { return maxMobsOverride; }
    public Map<String, Double> getAddMobs() { return addMobs; }
    public Map<String, Double> getSetMobs() { return setMobs; }
    public Map<String, List<String>> getModifyRewards() { return modifyRewards; }
}