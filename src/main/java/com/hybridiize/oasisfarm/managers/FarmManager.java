package com.hybridiize.oasisfarm.managers;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.farm.Farm;
import com.hybridiize.oasisfarm.farm.MobInfo;
import com.hybridiize.oasisfarm.farm.Region;
import com.hybridiize.oasisfarm.farm.TrackedMob;
import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.mobs.MobManager;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter; // Modern API requires this for location conversion
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

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

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
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void processFarm(Farm farm) {
        long baseInterval = plugin.getConfig().getLong("farm-check-interval", 100L);
        if (System.currentTimeMillis() - farm.getLastSpawnTick() < baseInterval * 50) {
            return;
        }
        farm.setLastSpawnTick(System.currentTimeMillis());

        Region region = farm.getRegion();
        World world = region.getPos1().getWorld();
        if (world == null || (!world.isChunkLoaded(region.getPos1().getBlockX() >> 4, region.getPos1().getBlockZ() >> 4) && !world.isChunkLoaded(region.getPos2().getBlockX() >> 4, region.getPos2().getBlockZ() >> 4))) {
            return;
        }

        trackedMobs.keySet().removeIf(uuid -> Bukkit.getEntity(uuid) == null || Bukkit.getEntity(uuid).isDead());
        long currentMobCount = trackedMobs.values().stream().filter(trackedMob -> trackedMob.getFarmId().equals(farm.getId())).count();
        plugin.getHologramManager().createOrUpdateFarmHologram(farm, (int) currentMobCount);

        if (farm.getMaxMobs() - currentMobCount > 0) {
            spawnMobInFarm(farm);
        }
    }

    private void spawnMobInFarm(Farm farm) {
        Map<String, Double> mobs = farm.getMobs();
        if (mobs.isEmpty()) return;

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

                        if (plugin.isMythicMobsEnabled() && "MYTHIC".equals(finalMobToSpawnInfo.getMobType())) {
                            try {
                                MobManager mobManager = MythicProvider.get().getMobManager();
                                Optional<MythicMob> mythicMobOptional = mobManager.getMythicMob(finalMobToSpawnInfo.getMythicId());
                                if (mythicMobOptional.isPresent()) {
                                    MythicMob mythicMob = mythicMobOptional.get();

                                    // This is the correct, modern way to convert a Bukkit Location
                                    AbstractLocation mythicLocation = BukkitAdapter.adapt(spawnLocation);

                                    // Spawn the mob and capture the result to track it
                                    io.lumine.mythic.core.mobs.ActiveMob activeMob = mythicMob.spawn(mythicLocation, finalMobToSpawnInfo.getMythicLevel());

                                    if (activeMob != null && activeMob.getEntity().getBukkitEntity() instanceof LivingEntity) {
                                        trackMob((LivingEntity) activeMob.getEntity().getBukkitEntity(), farm.getId(), finalMobToSpawnInfo.getTemplateId());
                                    }
                                } else {
                                    plugin.getLogger().warning("Attempted to spawn Mythic Mob '" + finalMobToSpawnInfo.getMythicId() + "' but it was not found.");
                                }
                            } catch (Exception e) {
                                plugin.getLogger().log(Level.SEVERE, "An unexpected error occurred while spawning Mythic Mob '" + finalMobToSpawnInfo.getMythicId() + "'", e);
                            }
                        } else {
                            LivingEntity spawnedMob = (LivingEntity) spawnLocation.getWorld().spawnEntity(spawnLocation, finalMobToSpawnInfo.getType());
                            if (spawnedMob != null) {
                                trackMob(spawnedMob, farm.getId(), finalMobToSpawnInfo.getTemplateId());
                                applyMobAttributes(spawnedMob, finalMobToSpawnInfo);
                            }
                        }
                        return;
                    }
                }
            }
        }.runTask(plugin);
    }

    public void spawnSpecificMob(Farm farm, String mobId, int amount) {
        MobInfo mobInfo = plugin.getConfigManager().getMobTemplate(mobId);
        if (mobInfo == null) {
            plugin.getLogger().warning("Event tried to spawn specific mob with unknown template ID: " + mobId);
            return;
        }

        for (int i = 0; i < amount; i++) {
            Location spawnLocation = farm.getRegion().getCenter();

            if (plugin.isMythicMobsEnabled() && "MYTHIC".equals(mobInfo.getMobType())) {
                try {
                    MobManager mobManager = MythicProvider.get().getMobManager();
                    Optional<MythicMob> mythicMobOptional = mobManager.getMythicMob(mobInfo.getMythicId());
                    if (mythicMobOptional.isPresent()) {
                        MythicMob mythicMob = mythicMobOptional.get();

                        // Use the modern location adapter here as well
                        AbstractLocation mythicLocation = BukkitAdapter.adapt(spawnLocation);

                        mythicMob.spawn(mythicLocation, mobInfo.getMythicLevel());
                    } else {
                        plugin.getLogger().warning("Event tried to spawn specific Mythic Mob '" + mobInfo.getMythicId() + "' but it was not found.");
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Event failed to spawn specific Mythic Mob: " + mobInfo.getMythicId(), e);
                }
            } else {
                LivingEntity spawnedMob = (LivingEntity) spawnLocation.getWorld().spawnEntity(spawnLocation, mobInfo.getType());
                if (spawnedMob != null) {
                    applyMobAttributes(spawnedMob, mobInfo);
                }
            }
        }
    }

    // --- All other helper methods remain the same ---
    // (isSafeLocation, applyMobAttributes, getRandomLocationInRegion, tracking methods, etc.)
    private boolean isSafeLocation(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getWorldBorder().isInside(loc)) return false;
        org.bukkit.block.Block feetBlock = loc.getBlock();
        org.bukkit.block.Block headBlock = feetBlock.getRelative(BlockFace.UP);
        org.bukkit.block.Block groundBlock = feetBlock.getRelative(BlockFace.DOWN);
        return groundBlock.getType().isSolid() && !groundBlock.isLiquid() && feetBlock.isPassable() && !feetBlock.isLiquid() && headBlock.isPassable() && !headBlock.isLiquid();
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
        if (mobInfo.getPotionEffects() != null) {
            for (String effectString : mobInfo.getPotionEffects()) {
                try {
                    String[] parts = effectString.split(":");
                    PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
                    int amplifier = parts.length > 1 ? Integer.parseInt(parts[1]) - 1 : 0;
                    if (type != null) mob.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, true, false));
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
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        return new Location(pos1.getWorld(), ThreadLocalRandom.current().nextDouble(minX, maxX), ThreadLocalRandom.current().nextDouble(minY, maxY), ThreadLocalRandom.current().nextDouble(minZ, maxZ));
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