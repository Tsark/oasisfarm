package com.hybridiize.oasisfarm.farm;

import org.bukkit.entity.EntityType;
import java.util.List;
import java.util.Map;

// This class now represents a Mob Template
public class MobInfo {
    private final String templateId; // New field: e.g., "starter_zombie"
    private final EntityType type;
    private final int killCooldown;
    private final String displayName;
    private final double health;
    private final Map<String, String> equipment;
    private final List<String> rewards;
    private final String killPermission;
    private final String broadcastKill;

    public MobInfo(String templateId, EntityType type, int killCooldown, String displayName, double health, Map<String, String> equipment, List<String> rewards, String killPermission, String broadcastKill) {
        this.templateId = templateId;
        this.type = type;
        this.killCooldown = killCooldown;
        this.displayName = displayName;
        this.health = health;
        this.equipment = equipment;
        this.rewards = rewards;
        this.killPermission = killPermission;
        this.broadcastKill = broadcastKill;
    }

    // --- GETTERS ---
    public String getTemplateId() { return templateId; }
    public EntityType getType() { return type; }
    public int getKillCooldown() { return killCooldown; }
    public String getDisplayName() { return displayName; }
    public double getHealth() { return health; }
    public Map<String, String> getEquipment() { return equipment; }
    public List<String> getRewards() { return rewards; }
    public String getKillPermission() { return killPermission; }
    public String getBroadcastKill() { return broadcastKill; }
}