package com.hybridiize.oasisfarm.event;

import com.hybridiize.oasisfarm.rewards.Reward;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A container for all the data defining a single phase of an event.
 */
public class EventPhase {
    private final int duration;
    private final List<String> onStartCommands;
    private final List<String> onEndCommands;
    private final double spawnIntervalModifier;
    private final boolean clearExistingMobs;
    private final int maxMobsOverride;
    private final Map<String, Double> addMobs;
    private final Map<String, Double> setMobs;
    private final Map<String, Map<String, List<Reward>>> modifyRewards;

    public EventPhase(int duration, List<String> onStartCommands, List<String> onEndCommands, double spawnIntervalModifier, boolean clearExistingMobs, int maxMobsOverride, Map<String, Double> addMobs, Map<String, Double> setMobs, Map<String, Map<String, List<Reward>>> modifyRewards) {
        this.duration = duration;
        this.onStartCommands = onStartCommands;
        this.onEndCommands = (onEndCommands != null) ? onEndCommands : new java.util.ArrayList<>();
        this.spawnIntervalModifier = spawnIntervalModifier;
        this.clearExistingMobs = clearExistingMobs;
        this.maxMobsOverride = maxMobsOverride;
        this.addMobs = addMobs;
        this.setMobs = setMobs;
        this.modifyRewards = (modifyRewards != null) ? modifyRewards : new HashMap<>();
    }

    // --- GETTERS ---
    public int getDuration() { return duration; }
    public List<String> getOnStartCommands() { return onStartCommands; }
    public List<String> getOnEndCommands() { return onEndCommands; }
    public double getSpawnIntervalModifier() { return spawnIntervalModifier; }
    public boolean shouldClearExistingMobs() { return clearExistingMobs; }
    public int getMaxMobsOverride() { return maxMobsOverride; }
    public Map<String, Double> getAddMobs() { return addMobs; }
    public Map<String, Double> getSetMobs() { return setMobs; }
    public Map<String, Map<String, List<Reward>>> getModifyRewards() { return modifyRewards; }
}