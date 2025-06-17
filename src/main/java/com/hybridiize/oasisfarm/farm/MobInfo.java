package com.hybridiize.oasisfarm.farm;

import org.bukkit.entity.EntityType;
import java.util.List;
import java.util.Map;

public class MobInfo {
    private final EntityType type;
    private final double spawnChance;
    private final int killCooldown;
    private final String displayName;
    private final double health;
    private final Map<String, String> equipment;
    private final List<String> rewards;

    public MobInfo(EntityType type, double spawnChance, int killCooldown, String displayName, double health, Map<String, String> equipment, List<String> rewards) {
        this.type = type;
        this.spawnChance = spawnChance;
        this.killCooldown = killCooldown;
        this.displayName = displayName;
        this.health = health;
        this.equipment = equipment;
        this.rewards = rewards;
    }

    // --- NEW GETTER METHODS ---
    public EntityType getType() { return type; }
    public double getSpawnChance() { return spawnChance; }
    public int getKillCooldown() { return killCooldown; }
    public String getDisplayName() { return displayName; }
    public double getHealth() { return health; }
    public Map<String, String> getEquipment() { return equipment; }
    public List<String> getRewards() { return rewards; }
}