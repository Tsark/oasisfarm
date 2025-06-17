package com.hybridiize.oasisfarm.farm;

import java.util.List;

public class Farm {
    private final String id;
    private final Region region;
    private final int maxMobs;
    private final List<MobInfo> mobInfoList;

    public Farm(String id, Region region, int maxMobs, List<MobInfo> mobInfoList) {
        this.id = id;
        this.region = region;
        this.maxMobs = maxMobs;
        this.mobInfoList = mobInfoList;
    }

    // --- NEW GETTER METHODS ---
    public String getId() {
        return id;
    }
    public Region getRegion() {
        return region;
    }
    public int getMaxMobs() {
        return maxMobs;
    }
    public List<MobInfo> getMobInfoList() {
        return mobInfoList;
    }
}