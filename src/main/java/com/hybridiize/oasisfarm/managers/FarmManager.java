package com.hybridiize.oasisfarm.managers;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.event.EventPhase;
import com.hybridiize.oasisfarm.farm.Farm;
import com.hybridiize.oasisfarm.farm.MobInfo;
import com.hybridiize.oasisfarm.farm.Region;
import com.hybridiize.oasisfarm.farm.TrackedMob;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import io.lumine.mythic.api.Mythic;
import io.lumine.mythic.api.exceptions.InvalidMobTypeException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FarmManager {

    private final Oasisfarm plugin;
    private final Map<UUID, TrackedMob> trackedMobs = new HashMap<>();

    public FarmManager(Oasisfarm plugin) {
        this.plugin = plugin;
        startFarmTicker();
    }

    private void startFarmTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Farm farm : plugin.getConfigManager().getFarms().values()) {
                    processFarm(farm);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Check every second
    }

    private void processFarm(Farm farm) {
        EventManager eventManager = plugin.getEventManager();
        EventPhase currentPhase = eventManager.isEventActive(farm.getId()) ? eventManager.getCurrentPhase(farm.getId()) : null;

        double spawnIntervalModifier = 1.0;
        int maxMobs = farm.getMaxMobs();

        if (currentPhase != null) {
            spawnIntervalModifier = currentPhase.getSpawnIntervalModifier();
            if (currentPhase.getMaxMobsOverride() > -1) {
                maxMobs = currentPhase.getMaxMobsOverride();
            }
        }

        long baseInterval = plugin.getConfig().getLong("farm-check-interval", 100L);
        long effectiveInterval = (long) (baseInterval * spawnIntervalModifier);

        if (System.currentTimeMillis() - farm.getLastSpawnTick() < effectiveInterval * 50) {
            return;
        }
        farm.setLastSpawnTick(System.currentTimeMillis());

        Region region = farm.getRegion();
        World world = region.getPos1().getWorld();

        if (!world.isChunkLoaded(region.getPos1().getBlockX() >> 4, region.getPos1().getBlockZ() >> 4) &&
                !world.isChunkLoaded(region.getPos2().getBlockX() >> 4, region.getPos2().getBlockZ() >> 4)) {
            return;
        }

        // Mob Confinement logic has been removed as requested.

        trackedMobs.keySet().removeIf(uuid -> Bukkit.getEntity(uuid) == null || Bukkit.getEntity(uuid).isDead());

        long currentMobCount = trackedMobs.values().stream()
                .filter(trackedMob -> trackedMob.getFarmId().equals(farm.getId()))
                .count();

        plugin.getHologramManager().createOrUpdateFarmHologram(farm, (int) currentMobCount);

        int mobsToSpawn = maxMobs - (int) currentMobCount;
        if (mobsToSpawn <= 0) {
            return;
        }

        spawnMobInFarm(farm);
    }

    private void spawnMobInFarm(Farm farm) {
        Map<String, Double> mobs = farm.getMobs();
        EventManager eventManager = plugin.getEventManager();
        EventPhase currentPhase = eventManager.isEventActive(farm.getId()) ? eventManager.getCurrentPhase(farm.getId()) : null;

        if (currentPhase != null) {
            if (currentPhase.getSetMobs() != null && !currentPhase.getSetMobs().isEmpty()) {
                mobs = currentPhase.getSetMobs();
            } else if (currentPhase.getAddMobs() != null && !currentPhase.getAddMobs().isEmpty()) {
                mobs = new HashMap<>(mobs);
                mobs.putAll(currentPhase.getAddMobs());
            }
        }

        if (mobs.isEmpty()) {
            return;
        }

        double roll = ThreadLocalRandom.current().nextDouble();
        double cumulativeChance = 0.0;
        String chosenTemplateId = null;

        for (Map.Entry<String, Double> entry : mobs.entrySet()) {
            cumulativeChance += entry.getValue();
            if (roll <= cumulativeChance) {
                chosenTemplateId = entry.getKey();
                break;
            }
        }

        if (chosenTemplateId == null && !mobs.isEmpty()) {
            chosenTemplateId = new ArrayList<>(mobs.keySet()).get(0);
        }

        if (chosenTemplateId == null) return;

        MobInfo mobToSpawnInfo = plugin.getConfigManager().getMobTemplate(chosenTemplateId);
        if (mobToSpawnInfo == null) {
            plugin.getLogger().warning("Attempted to spawn a null mob template: " + chosenTemplateId);
            return;
        }

        final MobInfo finalMobToSpawnInfo = mobToSpawnInfo;
        final int MAX_SPAWN_ATTEMPTS = 10;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (int i = 0; i < MAX_SPAWN_ATTEMPTS; i++) {
                    Location spawnLocation = getRandomLocationInRegion(farm.getRegion());
                    if (isSafeLocation(spawnLocation)) {
                        if (!spawnLocation.getChunk().isLoaded()) return;
                        LivingEntity spawnedMob = null;

                        // Check if MythicMobs integration is enabled AND the template is a Mythic type.
                        if (plugin.isMythicMobsEnabled() && finalMobToSpawnInfo.getMobType().equals("MYTHIC")) {
                            try {
                                // Use the MythicMobs API to spawn the mob
                                Entity entity = Mythic.get().spawnMythicMob(finalMobToSpawnInfo.getMythicId(), spawnLocation, finalMobToSpawnInfo.getMythicLevel());
                                if (entity instanceof LivingEntity) {
                                    spawnedMob = (LivingEntity) entity;
                                }
                            } catch (InvalidMobTypeException e) {
                                plugin.getLogger().severe("Attempted to spawn a Mythic Mob with an invalid ID: " + finalMobToSpawnInfo.getMythicId());
                            }
                        } else {
                            // Fallback to vanilla spawning if MythicMobs is off or mob-type is VANILLA
                            spawnedMob = (LivingEntity) spawnLocation.getWorld().spawnEntity(spawnLocation, finalMobToSpawnInfo.getType());
                        }

                        // If a mob was successfully spawned (either vanilla or mythic)...
                        if (spawnedMob != null) {
                            trackMob(spawnedMob, farm.getId(), finalMobToSpawnInfo.getTemplateId());

                            // For VANILLA mobs, we apply our own custom attributes.
                            // For MYTHIC mobs, we let MythicMobs handle its own attributes, but we can still override
                            // things if needed here. For now, we'll only apply attributes to vanilla mobs.
                            if (finalMobToSpawnInfo.getMobType().equals("VANILLA")) {
                                applyMobAttributes(spawnedMob, finalMobToSpawnInfo);
                            }
                        }
                        return;
                    }
                }
            }
        }.runTask(plugin);
    }

    private boolean isSafeLocation(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getWorldBorder().isInside(loc)) {
            return false;
        }
        org.bukkit.block.Block feetBlock = loc.getBlock();
        org.bukkit.block.Block headBlock = feetBlock.getRelative(BlockFace.UP);
        org.bukkit.block.Block groundBlock = feetBlock.getRelative(BlockFace.DOWN);

        if (!groundBlock.getType().isSolid() || groundBlock.isLiquid()) return false;
        if (!feetBlock.isPassable() || !headBlock.isPassable()) return false;
        return !feetBlock.isLiquid() && !headBlock.isLiquid();
    }

    public void applyMobAttributes(LivingEntity mob, MobInfo mobInfo) {
        if (mobInfo.getDisplayName() != null && !mobInfo.getDisplayName().isEmpty()) {
            mob.setCustomName(mobInfo.getDisplayName());
            mob.setCustomNameVisible(true);
        }
        if (mobInfo.getHealth() > 0) {
            Objects.requireNonNull(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(mobInfo.getHealth());
            mob.setHealth(mobInfo.getHealth());
        }
        if (mobInfo.getMovementSpeed() > 0) {
            Objects.requireNonNull(mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(mobInfo.getMovementSpeed());
        }
        if (mobInfo.getAttackDamage() > 0 && mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            Objects.requireNonNull(mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(mobInfo.getAttackDamage());
        }

        if (mobInfo.getPotionEffects() != null && !mobInfo.getPotionEffects().isEmpty()) {
            for (String effectString : mobInfo.getPotionEffects()) {
                try {
                    String[] parts = effectString.split(":");
                    PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
                    int amplifier = (parts.length > 1) ? Integer.parseInt(parts[1]) : 0;
                    if (type != null) {
                        mob.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, true, false));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid potion effect format in '" + mobInfo.getTemplateId() + "': " + effectString);
                }
            }
        }

        EntityEquipment equipment = mob.getEquipment();
        if (equipment != null && mobInfo.getEquipment() != null) {
            for (Map.Entry<String, String> entry : mobInfo.getEquipment().entrySet()) {
                try {
                    Material material = Material.valueOf(entry.getValue().toUpperCase());
                    ItemStack item = new ItemStack(material);
                    switch (entry.getKey().toUpperCase()) {
                        case "HAND": equipment.setItemInMainHand(item); break;
                        case "OFFHAND": equipment.setItemInOffHand(item); break;
                        case "HELMET": equipment.setHelmet(item); break;
                        case "CHESTPLATE": equipment.setChestplate(item); break;
                        case "LEGGINGS": equipment.setLeggings(item); break;
                        case "BOOTS": equipment.setBoots(item); break;
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material name for equipment in '" + mobInfo.getTemplateId() + "': " + entry.getValue());
                }
            }
        }
    }

    private Location getRandomLocationInRegion(Region region) {
        Location pos1 = region.getPos1();
        Location pos2 = region.getPos2();
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        int randomX = ThreadLocalRandom.current().nextInt(minX, maxX + 1);
        int randomY = ThreadLocalRandom.current().nextInt(minY, maxY + 1);
        int randomZ = ThreadLocalRandom.current().nextInt(minZ, maxZ + 1);
        return new Location(pos1.getWorld(), randomX + 0.5, randomY, randomZ + 0.5);
    }

    public boolean isTrackedMob(Entity entity) {
        return trackedMobs.containsKey(entity.getUniqueId());
    }

    public TrackedMob getTrackedMob(Entity entity) {
        return trackedMobs.get(entity.getUniqueId());
    }

    public void untrackMob(Entity entity) {
        trackedMobs.remove(entity.getUniqueId());
    }



    public void trackMob(LivingEntity mob, String farmId, String templateId) {
        if (mob == null || farmId == null || templateId == null) return;
        trackedMobs.put(mob.getUniqueId(), new TrackedMob(farmId, templateId));
    }

    public Set<UUID> getTrackedMobIds() {
        return trackedMobs.keySet();
    }
}