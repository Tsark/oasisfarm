package com.hybridiize.oasisfarm.farm;

import com.hybridiize.oasisfarm.rewards.Reward;
import com.hybridiize.oasisfarm.rewards.RewardSet;
import org.bukkit.entity.EntityType;
import java.util.List;
import java.util.Map;

/**
 * Represents a Mob Template loaded from mob-templates.yml.
 */
public class MobInfo {
    private final String mobType; // "VANILLA" or "MYTHIC"
    private final String mythicId;
    private final int mythicLevel;
    private final String templateId;
    private final EntityType type;
    private final int killCooldown;
    private final String displayName;
    private final double health;
    private final Map<String, String> equipment;
    private final String killPermission;
    private final String broadcastKill;
    private final List<RewardSet> killerRewards;
    private final List<RewardSet> farmWideRewards;
    private final double movementSpeed;
    private final double attackDamage;
    private final List<String> potionEffects;


    public MobInfo(String templateId, EntityType type, int killCooldown, String displayName, double health, Map<String, String> equipment, String killPermission, String broadcastKill, List<RewardSet> killerRewards, List<RewardSet> farmWideRewards, double movementSpeed, double attackDamage, List<String> potionEffects, String mobType, String mythicId, int mythicLevel) {
        this.templateId = templateId;
        this.type = type;
        this.killCooldown = killCooldown;
        this.displayName = displayName;
        this.health = health;
        this.equipment = equipment;
        this.killPermission = killPermission;
        this.broadcastKill = broadcastKill;
        this.movementSpeed = movementSpeed;
        this.attackDamage = attackDamage;
        this.potionEffects = potionEffects;
        this.killerRewards = killerRewards;
        this.farmWideRewards = farmWideRewards;
        this.mobType = mobType;
        this.mythicId = mythicId;
        this.mythicLevel = mythicLevel;
    }

    // --- GETTERS ---
    public String getTemplateId() { return templateId; }
    public EntityType getType() { return type; }
    public int getKillCooldown() { return killCooldown; }
    public String getDisplayName() { return displayName; }
    public double getHealth() { return health; }
    public Map<String, String> getEquipment() { return equipment; }
    public String getKillPermission() { return killPermission; }
    public String getBroadcastKill() { return broadcastKill; }
    public List<RewardSet> getKillerRewards() { return killerRewards; }
    public List<RewardSet> getFarmWideRewards() { return farmWideRewards; }
    public double getMovementSpeed() { return movementSpeed; }
    public double getAttackDamage() { return attackDamage; }
    public List<String> getPotionEffects() { return potionEffects; }
    public String getMobType() { return mobType; }
    public String getMythicId() { return mythicId; }
    public int getMythicLevel() { return mythicLevel; }
}