package com.hybridiize.oasisfarm.managers;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.event.EventPhase;
import com.hybridiize.oasisfarm.farm.Farm;
import com.hybridiize.oasisfarm.farm.MobInfo;
import com.hybridiize.oasisfarm.farm.Region;
import com.hybridiize.oasisfarm.farm.TrackedMob;
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
import java.util.Objects;

public class FarmManager {

    private final Oasisfarm plugin;
    // This map tracks which mobs belong to which farm. This is our Optimized Mob Tracking.
    private final Map<UUID, TrackedMob> trackedMobs = new HashMap<>();

    public FarmManager(Oasisfarm plugin) {
        this.plugin = plugin;
        startFarmTicker();
    }

    private void startFarmTicker() {
        // We no longer store the interval as a final variable, as it can change.
        new BukkitRunnable() {
            @Override
            public void run() {
                Map<String, Farm> farms = plugin.getConfigManager().getFarms();
                for (Farm farm : farms.values()) {
                    processFarm(farm);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Check every second (20 ticks) for responsiveness
    }

    private void processFarm(Farm farm) {
        // --- EVENT OVERRIDE LOGIC ---
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

        // Use the default interval from config, modified by the event
        long baseInterval = plugin.getConfig().getLong("farm-check-interval", 100L);
        long effectiveInterval = (long) (baseInterval * spawnIntervalModifier);

        // Check if enough time has passed to spawn for this farm
        if (System.currentTimeMillis() - farm.getLastSpawnTick() < effectiveInterval * 50) { // 50 is ms per tick
            return;
        }
        farm.setLastSpawnTick(System.currentTimeMillis());

        Region region = farm.getRegion();
        World world = region.getPos1().getWorld();

        // --- Chunk Awareness ---
        if (!world.isChunkLoaded(region.getPos1().getBlockX() >> 4, region.getPos1().getBlockZ() >> 4) &&
                !world.isChunkLoaded(region.getPos2().getBlockX() >> 4, region.getPos2().getBlockZ() >> 4)) {
            return; // Skip this farm if no players are nearby
        }

        // --- Mob Confinement ---
        for (Map.Entry<UUID, TrackedMob> entry : new HashMap<>(trackedMobs).entrySet()) {
            if (entry.getValue().getFarmId().equals(farm.getId())) {
                Entity entity = Bukkit.getEntity(entry.getKey());
                if (entity != null && !farm.getRegion().contains(entity.getLocation())) {
                    entity.teleport(getRandomLocationInRegion(farm.getRegion()));
                }
            }
        }

        // --- Mob Counting ---
        trackedMobs.keySet().removeIf(uuid -> Bukkit.getEntity(uuid) == null || Bukkit.getEntity(uuid).isDead());

        long currentMobCount = trackedMobs.values().stream()
                .filter(trackedMob -> trackedMob.getFarmId().equals(farm.getId()))
                .count();

        // Update hologram
        plugin.getHologramManager().createOrUpdateFarmHologram(farm, (int) currentMobCount);

        // Use the potentially overridden maxMobs value
        int mobsToSpawn = maxMobs - (int) currentMobCount;

        if (mobsToSpawn <= 0) {
            return;
        }

        spawnMobInFarm(farm);
    }

    private void spawnMobInFarm(Farm farm) {
        // --- EVENT MOB OVERRIDE ---
        Map<String, Double> mobs = farm.getMobs(); // Get the default mob list

        EventManager eventManager = plugin.getEventManager();
        EventPhase currentPhase = eventManager.isEventActive(farm.getId()) ? eventManager.getCurrentPhase(farm.getId()) : null;

        if (currentPhase != null) {
            // "set-mobs" completely replaces the mob list
            if (currentPhase.getSetMobs() != null && !currentPhase.getSetMobs().isEmpty()) {
                mobs = currentPhase.getSetMobs();
            }
            // "add-mobs" adds to the existing list
            else if (currentPhase.getAddMobs() != null && !currentPhase.getAddMobs().isEmpty()) {
                // Create a mutable copy to add to
                mobs = new HashMap<>(mobs);
                mobs.putAll(currentPhase.getAddMobs());
            }
        }

        // The rest of the method uses the 'mobs' variable, which is now event-aware.
        if (mobs.isEmpty()) {
            return; // No mob types are defined for this farm.
        }

        // --- Mob Selection Logic (New Version) ---
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

        if (chosenTemplateId == null) {
            // Failsafe: if chances don't add up to 1.0, pick one randomly from the list.
            chosenTemplateId = new ArrayList<>(mobs.keySet()).get(0);
        }

        // Get the full MobInfo template from the ConfigManager
        MobInfo mobToSpawnInfo = plugin.getConfigManager().getMobTemplate(chosenTemplateId);
        if (mobToSpawnInfo == null) {
            plugin.getLogger().warning("Attempted to spawn a null mob template: " + chosenTemplateId);
            return;
        }

        // --- Spawning Logic with Safety Checks (same as before) ---
        final MobInfo finalMobToSpawnInfo = mobToSpawnInfo;
        final int MAX_SPAWN_ATTEMPTS = 10;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (int i = 0; i < MAX_SPAWN_ATTEMPTS; i++) {
                    Location spawnLocation = getRandomLocationInRegion(farm.getRegion());

                    if (isSafeLocation(spawnLocation)) {
                        if (!spawnLocation.getChunk().isLoaded()) {
                            return;
                        }

                        LivingEntity spawnedMob = (LivingEntity) spawnLocation.getWorld().spawnEntity(spawnLocation, finalMobToSpawnInfo.getType());
                        // IMPORTANT: We now track mobs by their TEMPLATE ID.
                        trackedMobs.put(spawnedMob.getUniqueId(), new TrackedMob(farm.getId(), finalMobToSpawnInfo.getTemplateId()));
                        applyMobAttributes(spawnedMob, finalMobToSpawnInfo);

                        return;
                    }
                }
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

    public TrackedMob getTrackedMob(Entity entity) {
        return trackedMobs.get(entity.getUniqueId());
    }

    public void untrackMob(Entity entity) {
        trackedMobs.remove(entity.getUniqueId());
    }

    public Collection<UUID> getAllTrackedMobIds() {
        return new ArrayList<>(trackedMobs.keySet());
    }

    public Set<UUID> getTrackedMobIds() {
        return trackedMobs.keySet();
    }
}