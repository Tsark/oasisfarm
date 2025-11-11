package com.hybridiize.oasisfarm.farm;

public class FarmMobConfig {

    private final String templateId;
    private final double chance;
    private final int maxPerFarm;
    // We can add more here later, like "requiredTime"

    public FarmMobConfig(String templateId, double chance, int maxPerFarm) {
        this.templateId = templateId;
        this.chance = chance;
        this.maxPerFarm = maxPerFarm;
    }

    public String getTemplateId() {
        return templateId;
    }

    public double getChance() {
        return chance;
    }

    public int getMaxPerFarm() {
        return maxPerFarm;
    }
}