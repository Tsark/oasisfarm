package com.hybridiize.oasisfarm.managers;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.farm.Farm;
import com.hybridiize.oasisfarm.farm.MobInfo;
import com.hybridiize.oasisfarm.farm.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material; // new
import org.bukkit.World;
import org.bukkit.attribute.Attribute; // THE IMPORTANT ONE
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment; // new
import org.bukkit.inventory.ItemStack; // new
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class FarmManager {

    private final Oasisfarm plugin;
    // This map tracks which mobs belong to which farm. This is our Optimized Mob Tracking.
    private final Map<UUID, String> trackedMobs = new HashMap<>();

    public FarmManager(Oasisfarm plugin) {
        this.plugin = plugin;
        startFarmTicker();
    }

    private void startFarmTicker() {
        long interval = plugin.getConfig().getLong("farm-check-interval", 100L); // Default to 5 seconds (100 ticks)

        new BukkitRunnable() {
            @Override
            public void run() {
                // Get all farms from the ConfigManager
                Map<String, Farm> farms = plugin.getConfigManager().getFarms();
                for (Farm farm : farms.values()) {
                    processFarm(farm);
                }
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    private void processFarm(Farm farm) {
        Region region = farm.getRegion();
        World world = region.getPos1().getWorld();

        // --- Chunk Awareness ---
        // Check if the farm's region is loaded by checking if the corner chunks are loaded.
        // This is a simple but effective way to save performance.
        if (!world.isChunkLoaded(region.getPos1().getBlockX() >> 4, region.getPos1().getBlockZ() >> 4) &&
                !world.isChunkLoaded(region.getPos2().getBlockX() >> 4, region.getPos2().getBlockZ() >> 4)) {
            return; // Skip this farm if no players are nearby
        }

        // --- Mob Counting ---
        // We first clean up our list of any dead or removed mobs.
        trackedMobs.keySet().removeIf(uuid -> {
            Entity entity = Bukkit.getEntity(uuid);
            return entity == null || entity.isDead();
        });

        // --- Mob Confinement ---
        // Check if any tracked mobs have wandered outside their designated farm.
        for (Map.Entry<UUID, String> entry : new HashMap<>(trackedMobs).entrySet()) {
            if (entry.getValue().equals(farm.getId())) { // Check only mobs belonging to the current farm
                Entity entity = Bukkit.getEntity(entry.getKey());
                if (entity != null && !farm.getRegion().contains(entity.getLocation())) {
                    // Mob has escaped! Teleport it back to a random spot in the region.
                    entity.teleport(getRandomLocationInRegion(farm.getRegion()));
                }
            }
        }

        // Now, count how many of our tracked mobs are currently in this farm.
        long currentMobCount = trackedMobs.values().stream().filter(farmId -> farmId.equals(farm.getId())).count();
        plugin.getHologramManager().createOrUpdateFarmHologram(farm, (int) currentMobCount);
        int mobsToSpawn = farm.getMaxMobs() - (int) currentMobCount;

        if (mobsToSpawn <= 0) {
            return; // The farm is full, nothing to do.
        }

        // --- Staggered Spawning ---
        // We will spawn one mob per tick cycle to avoid lag spikes.
        spawnMobInFarm(farm);
    }

    private void spawnMobInFarm(Farm farm) {
        if (farm.getMobInfoList().isEmpty()) {
            return; // No mob types are defined for this farm.
        }

        // --- Mob Selection Logic (same as before) ---
        double roll = ThreadLocalRandom.current().nextDouble();
        double cumulativeChance = 0.0;
        MobInfo mobToSpawnInfo = null;

        for (MobInfo mobInfo : farm.getMobInfoList()) {
            cumulativeChance += mobInfo.getSpawnChance();
            if (roll <= cumulativeChance) {
                mobToSpawnInfo = mobInfo;
                break;
            }
        }

        if (mobToSpawnInfo == null) {
            mobToSpawnInfo = farm.getMobInfoList().get(0);
        }

        // --- NEW Spawning Logic with Safety Checks ---
        final MobInfo finalMobToSpawnInfo = mobToSpawnInfo;
        final int MAX_SPAWN_ATTEMPTS = 10; // Try 10 times to find a safe spot

        new BukkitRunnable() {
            @Override
            public void run() {
                for (int i = 0; i < MAX_SPAWN_ATTEMPTS; i++) {
                    Location spawnLocation = getRandomLocationInRegion(farm.getRegion());

                    if (isSafeLocation(spawnLocation)) {
                        // Safe location found, proceed with spawning
                        if (!spawnLocation.getChunk().isLoaded()) {
                            return; // Chunk got unloaded during check, abort.
                        }

                        LivingEntity spawnedMob = (LivingEntity) spawnLocation.getWorld().spawnEntity(spawnLocation, finalMobToSpawnInfo.getType());
                        trackedMobs.put(spawnedMob.getUniqueId(), farm.getId());
                        applyMobAttributes(spawnedMob, finalMobToSpawnInfo);

                        return; // Exit the loop and runnable, we're done
                    }
                }
                // If the loop finishes, no safe spot was found after 10 tries.
                // plugin.getLogger().warning("Could not find a safe spawn location in farm '" + farm.getId() + "' after " + MAX_SPAWN_ATTEMPTS + " attempts.");
            }
        }.runTask(plugin);
    }

    private boolean isSafeLocation(Location loc) {
        if (loc.getWorld() == null) return false;

        // Ensure the location is within the world border
        if (!loc.getWorld().getWorldBorder().isInside(loc)) {
            return false;
        }

        // Get the block at the location and the block below it
        org.bukkit.block.Block feetBlock = loc.getBlock();
        org.bukkit.block.Block headBlock = feetBlock.getRelative(org.bukkit.block.BlockFace.UP);
        org.bukkit.block.Block groundBlock = feetBlock.getRelative(org.bukkit.block.BlockFace.DOWN);

        // 1. Ground Check: Must be a solid, full block. Not air, not water, not lava.
        if (!groundBlock.getType().isSolid() || groundBlock.isLiquid()) {
            return false;
        }

        // 2. Body Check: The mob's feet and head space must be empty (e.g., air, tall grass).
        //    This prevents spawning inside walls or under ceilings.
        if (!feetBlock.isPassable() || !headBlock.isPassable()) {
            return false;
        }

        // 3. Liquid Check: The mob's feet or head shouldn't be in liquid.
        if (feetBlock.isLiquid() || headBlock.isLiquid()) {
            return false;
        }

        return true; // If all checks pass, the location is safe.
    }

    private void applyMobAttributes(LivingEntity mob, MobInfo mobInfo) {
        // Set Custom Display Name
        if (mobInfo.getDisplayName() != null && !mobInfo.getDisplayName().isEmpty()) {
            mob.setCustomName(mobInfo.getDisplayName());
            mob.setCustomNameVisible(true);
        }

        // Set Custom Health
        if (mobInfo.getHealth() > 0) {
            // We need to get the attribute instance for MAX_HEALTH and set its base value.
            mob.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(mobInfo.getHealth());
            mob.setHealth(mobInfo.getHealth()); // Heal the mob to its new max health
        }

        // Set Equipment
        EntityEquipment equipment = mob.getEquipment();
        if (equipment != null && mobInfo.getEquipment() != null) {
            for (Map.Entry<String, String> entry : mobInfo.getEquipment().entrySet()) {
                try {
                    Material material = Material.valueOf(entry.getValue().toUpperCase());
                    ItemStack item = new ItemStack(material);
                    String slot = entry.getKey().toUpperCase();

                    switch (slot) {
                        case "HAND":
                            equipment.setItemInMainHand(item);
                            break;
                        case "OFFHAND":
                            equipment.setItemInOffHand(item);
                            break;
                        case "HELMET":
                            equipment.setHelmet(item);
                            break;
                        case "CHESTPLATE":
                            equipment.setChestplate(item);
                            break;
                        case "LEGGINGS":
                            equipment.setLeggings(item);
                            break;
                        case "BOOTS":
                            equipment.setBoots(item);
                            break;
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material name in config for equipment: " + entry.getValue());
                }
            }
        }
    }

    // This is a helper method to find a valid spawn point inside the farm.
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

    public void untrackMob(Entity entity) {
        trackedMobs.remove(entity.getUniqueId());
    }

    public Collection<UUID> getAllTrackedMobIds() {
        return new ArrayList<>(trackedMobs.keySet());
    }
}