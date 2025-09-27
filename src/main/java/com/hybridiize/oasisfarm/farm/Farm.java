package com.hybridiize.oasisfarm.farm;

import java.util.Map;

public class Farm {
    private final String id;
    private final Region region;
    private final int maxMobs;
    private final int entryCooldown;
    // Map<TemplateID, SpawnChance>
    private final Map<String, Double> mobs;

    // This value is not stored in config, it's used at runtime by the FarmManager
    private long lastSpawnTick = 0;

    public Farm(String id, Region region, int maxMobs, int entryCooldown, Map<String, Double> mobs) {
        this.id = id;
        this.region = region;
        this.maxMobs = maxMobs;
        this.entryCooldown = entryCooldown;
        this.mobs = mobs;
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

    public Map<String, Double> getMobs() {
        return mobs;
    }

    // --- RUNTIME GETTERS/SETTERS ---
    public long getLastSpawnTick() {
        return lastSpawnTick;
    }

    public void setLastSpawnTick(long lastSpawnTick) {
        this.lastSpawnTick = lastSpawnTick;
    }
}