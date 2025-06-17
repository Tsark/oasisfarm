package com.hybridiize.oasisfarm.farm;

import java.util.Map;

public class Farm {
    private final String id;
    private final Region region;
    private final int maxMobs;
    private final int entryCooldown;
    // This is the major change: Map<TemplateID, SpawnChance>
    private final Map<String, Double> mobs;

    public Farm(String id, Region region, int maxMobs, int entryCooldown, Map<String, Double> mobs) {
        this.id = id;
        this.region = region;
        this.maxMobs = maxMobs;
        this.entryCooldown = entryCooldown;
        this.mobs = mobs;
    }

    // --- GETTERS ---
    public String getId() { return id; }
    public Region getRegion() { return region; }
    public int getMaxMobs() { return maxMobs; }
    public int getEntryCooldown() { return entryCooldown; }
    public Map<String, Double> getMobs() { return mobs; }
}