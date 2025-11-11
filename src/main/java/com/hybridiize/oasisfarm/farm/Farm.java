package com.hybridiize.oasisfarm.farm;

import java.util.Map;

public class Farm {
    private final String id;
    private final Region region;
    private final int maxMobs;
    private final int entryCooldown;
    private final boolean hologramEnabled;
    private final String spawningType;
    // --- UPDATED FIELD ---
    // Map<TemplateID, MobConfig>
    private final Map<String, FarmMobConfig> mobs;

    // This value is not stored in config, it's used at runtime by the FarmManager
    private long lastSpawnTick = 0;

    public Farm(String id, Region region, int maxMobs, int entryCooldown, Map<String, FarmMobConfig> mobs, boolean hologramEnabled, String spawningType) {
        this.id = id;
        this.region = region;
        this.maxMobs = maxMobs;
        this.entryCooldown = entryCooldown;
        this.mobs = mobs;
        this.hologramEnabled = hologramEnabled;
        this.spawningType = spawningType;
    }

    // --- GETTERS ---
    public String getId() {
        return id;
    }

    public Region getRegion() {
        return region;
    }

    public int getMaxMobs() {
        return maxMobs;
    }

    public int getEntryCooldown() {
        return entryCooldown;
    }

    // --- UPDATED GETTER ---
    public Map<String, FarmMobConfig> getMobs() {
        return mobs;
    }

    public boolean isHologramEnabled() {
        return hologramEnabled;
    }

    public String getSpawningType() {
        return spawningType;
    }

    // --- RUNTIME GETTERS/SETTERS ---
    public long getLastSpawnTick() {
        return lastSpawnTick;
    }

    public void setLastSpawnTick(long lastSpawnTick) {
        this.lastSpawnTick = lastSpawnTick;
    }
}