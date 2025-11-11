package com.hybridiize.oasisfarm.managers;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.farm.Farm;
import com.hybridiize.oasisfarm.farm.FarmMobConfig; // --- NEW IMPORT ---
import com.hybridiize.oasisfarm.farm.MobInfo;
import com.hybridiize.oasisfarm.farm.Region;
import com.hybridiize.oasisfarm.farm.TrackedMob;
import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.mobs.MobManager;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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
                    if (farm.getSpawningType().equals("efficient") || farm.getSpawningType().equals("static")) {
                        processEfficientOrStaticFarm(farm);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Ticks every second
    }

    private void processEfficientOrStaticFarm(Farm farm) {

        long baseInterval = plugin.getConfig().getLong("farm-check-interval", 100L);
        if (farm.getSpawningType().equals("static")) {
            baseInterval = 600L;
        }

        if (System.currentTimeMillis() - farm.getLastSpawnTick() < baseInterval * 50) {
            return;
        }
        farm.setLastSpawnTick(System.currentTimeMillis());

        Region region = farm.getRegion();
        World world = region.getPos1().getWorld();
        if (world == null) {
            return;
        }

        List<Player> playersInFarm = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            if (farm.getRegion().contains(player.getLocation())) {
                playersInFarm.add(player);
            }
        }

        if (playersInFarm.isEmpty() && farm.getSpawningType().equals("efficient")) {
            return;
        }

        trackedMobs.keySet().removeIf(uuid -> Bukkit.getEntity(uuid) == null || Bukkit.getEntity(uuid).isDead());
        long currentMobCount = trackedMobs.values().stream().filter(trackedMob -> trackedMob.getFarmId().equals(farm.getId())).count();
        plugin.getHologramManager().createOrUpdateFarmHologram(farm, (int) currentMobCount);

        if (farm.getMaxMobs() - currentMobCount > 0) {

            Location spawnCenter;
            if (!playersInFarm.isEmpty()) {
                spawnCenter = playersInFarm.get(ThreadLocalRandom.current().nextInt(playersInFarm.size())).getLocation();
            } else {
                spawnCenter = farm.getRegion().getCenter();
            }

            spawnMobInFarm(farm, spawnCenter, (int) currentMobCount); // --- Pass currentMobCount ---
        }
    }

    // --- NEW HELPER FUNCTION ---
    /**
     * Counts all tracked mobs in a farm, grouped by their template ID.
     * @param farmId The farm to check.
     * @return A map of [TemplateID -> Count]
     */
    private Map<String, Integer> countMobsByType(String farmId) {
        Map<String, Integer> counts = new HashMap<>();
        for (TrackedMob mob : trackedMobs.values()) {
            if (mob.getFarmId().equals(farmId)) {
                counts.put(mob.getTemplateId(), counts.getOrDefault(mob.getTemplateId(), 0) + 1);
            }
        }
        return counts;
    }

    // --- SIGNATURE UPDATED: Now takes current mob count ---
    private void spawnMobInFarm(Farm farm, Location spawnCenter, int currentMobCount) {

        // --- NEW LOGIC: Filter mobs based on per-mob cap ---
        Map<String, Integer> currentMobCounts = countMobsByType(farm.getId());

        List<FarmMobConfig> spawnableMobs = new ArrayList<>();
        double totalChance = 0.0;

        for (FarmMobConfig mobConfig : farm.getMobs().values()) {
            int maxPerFarm = mobConfig.getMaxPerFarm();
            int currentPerMob = currentMobCounts.getOrDefault(mobConfig.getTemplateId(), 0);

            // Check if this mob type is eligible to spawn
            if (maxPerFarm == -1 || currentPerMob < maxPerFarm) {
                spawnableMobs.add(mobConfig);
                totalChance += mobConfig.getChance();
            }
        }
        // --- END NEW LOGIC ---

        if (spawnableMobs.isEmpty()) {
            // Nothing is eligible to spawn (e.g., all mobs are at their per-mob cap)
            return;
        }

        // --- UPDATED LOGIC: Roll based on the *filtered* list ---
        double roll = ThreadLocalRandom.current().nextDouble() * totalChance;
        double cumulativeChance = 0.0;
        FarmMobConfig chosenMobConfig = null;
        for (FarmMobConfig mobConfig : spawnableMobs) {
            cumulativeChance += mobConfig.getChance();
            if (roll <= cumulativeChance) {
                chosenMobConfig = mobConfig;
                break;
            }
        }

        if (chosenMobConfig == null) {
            // Fallback just in case of floating point rounding errors
            chosenMobConfig = spawnableMobs.get(ThreadLocalRandom.current().nextInt(spawnableMobs.size()));
        }
        // --- END UPDATED LOGIC ---

        MobInfo mobToSpawnInfo = plugin.getConfigManager().getMobTemplate(chosenMobConfig.getTemplateId());
        if (mobToSpawnInfo == null) {
            plugin.getLogger().warning("Attempted to spawn a null mob template: " + chosenMobConfig.getTemplateId());
            return;
        }

        final MobInfo finalMobToSpawnInfo = mobToSpawnInfo;
        final int MAX_SPAWN_ATTEMPTS = 10;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (int i = 0; i < MAX_SPAWN_ATTEMPTS; i++) {
                    Location spawnLocation = findRandomSafeLocationNear(spawnCenter, farm.getRegion());
                    if (spawnLocation != null) {
                        if (!spawnLocation.getChunk().isLoaded()) {
                            continue;
                        }

                        LivingEntity spawnedMob = null;

                        if (plugin.isMythicMobsEnabled() && "MYTHIC".equals(finalMobToSpawnInfo.getMobType())) {
                            try {
                                MobManager mobManager = MythicProvider.get().getMobManager();
                                Optional<MythicMob> mythicMobOptional = mobManager.getMythicMob(finalMobToSpawnInfo.getMythicId());
                                if (mythicMobOptional.isPresent()) {
                                    MythicMob mythicMob = mythicMobOptional.get();
                                    AbstractLocation mythicLocation = BukkitAdapter.adapt(spawnLocation);
                                    io.lumine.mythic.core.mobs.ActiveMob activeMob = mythicMob.spawn(mythicLocation, finalMobToSpawnInfo.getMythicLevel());

                                    if (activeMob != null && activeMob.getEntity().getBukkitEntity() instanceof LivingEntity) {
                                        spawnedMob = (LivingEntity) activeMob.getEntity().getBukkitEntity();
                                    }
                                } else {
                                    plugin.getLogger().warning("Attempted to spawn Mythic Mob '" + finalMobToSpawnInfo.getMythicId() + "' but it was not found.");
                                }
                            } catch (Exception e) {
                                plugin.getLogger().log(Level.SEVERE, "An unexpected error occurred while spawning Mythic Mob '" + finalMobToSpawnInfo.getMythicId() + "'", e);
                            }
                        } else {
                            spawnedMob = (LivingEntity) spawnLocation.getWorld().spawnEntity(spawnLocation, finalMobToSpawnInfo.getType());
                        }

                        if (spawnedMob != null) {
                            trackMob(spawnedMob, farm.getId(), finalMobToSpawnInfo.getTemplateId());

                            if (!"MYTHIC".equals(finalMobToSpawnInfo.getMobType())) {
                                applyMobAttributes(spawnedMob, finalMobToSpawnInfo);
                            }

                            if (farm.getSpawningType().equals("static")) {
                                spawnedMob.setPersistent(true);
                            }
                        }

                        return;
                    }
                }
            }
        }.runTask(plugin);
    }

    // ... (rest of FarmManager.java is unchanged: findRandomSafeLocationNear, killTrackedMobs, spawnSpecificMob, helpers, etc.)

    private Location findRandomSafeLocationNear(Location center, Region region) {
        Location pos1 = region.getPos1();
        Location pos2 = region.getPos2();
        World world = center.getWorld();
        if (world == null) return null;

        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        double x = center.getX() + (ThreadLocalRandom.current().nextDouble(64.0) - 32.0);
        double z = center.getZ() + (ThreadLocalRandom.current().nextDouble(64.0) - 32.0);

        x = Math.max(minX, Math.min(maxX, x));
        z = Math.max(minZ, Math.min(maxZ, z));

        for (int y = (int) Math.floor(maxY); y >= (int) Math.floor(minY); y--) {
            Location loc = new Location(world, x, y, z);
            if (isSafeLocation(loc)) {
                return loc;
            }
        }

        return null;
    }

    public int killTrackedMobs(String farmIdFilter, String templateIdFilter) {
        int killCount = 0;
        Set<UUID> mobIds = new HashSet<>(trackedMobs.keySet());

        for (UUID mobId : mobIds) {
            TrackedMob trackedInfo = trackedMobs.get(mobId);
            if (trackedInfo == null) {
                continue;
            }

            Farm farm = plugin.getConfigManager().getFarms().get(trackedInfo.getFarmId());
            if (farm != null && farm.getSpawningType().equals("static")) {
                continue;
            }

            boolean farmMatch = farmIdFilter.equalsIgnoreCase("all") || trackedInfo.getFarmId().equalsIgnoreCase(farmIdFilter);
            boolean templateMatch = templateIdFilter.equalsIgnoreCase("all") || trackedInfo.getTemplateId().equalsIgnoreCase(templateIdFilter);

            if (farmMatch && templateMatch) {
                Entity mob = Bukkit.getEntity(mobId);
                if (mob != null && !mob.isDead()) {
                    mob.remove();
                    killCount++;
                }
                trackedMobs.remove(mobId);
            }
        }
        return killCount;
    }

    public void spawnSpecificMob(Farm farm, String mobId, int amount) {
        MobInfo mobInfo = plugin.getConfigManager().getMobTemplate(mobId);
        if (mobInfo == null) {
            plugin.getLogger().warning("Event tried to spawn specific mob with unknown template ID: " + mobId);
            return;
        }

        for (int i = 0; i < amount; i++) {
            Location spawnLocation = findRandomSafeLocationNear(farm.getRegion().getCenter(), farm.getRegion());
            if (spawnLocation == null) {
                spawnLocation = farm.getRegion().getCenter();
            }

            LivingEntity spawnedMob = null;

            if (plugin.isMythicMobsEnabled() && "MYTHIC".equals(mobInfo.getMobType())) {
                try {
                    MobManager mobManager = MythicProvider.get().getMobManager();
                    Optional<MythicMob> mythicMobOptional = mobManager.getMythicMob(mobInfo.getMythicId());
                    if (mythicMobOptional.isPresent()) {
                        MythicMob mythicMob = mythicMobOptional.get();
                        AbstractLocation mythicLocation = BukkitAdapter.adapt(spawnLocation);
                        io.lumine.mythic.core.mobs.ActiveMob activeMob = mythicMob.spawn(mythicLocation, mobInfo.getMythicLevel());

                        if (activeMob != null && activeMob.getEntity().getBukkitEntity() instanceof LivingEntity) {
                            spawnedMob = (LivingEntity) activeMob.getEntity().getBukkitEntity();
                        }
                    } else {
                        plugin.getLogger().warning("Event tried to spawn specific Mythic Mob '" + mobInfo.getMythicId() + "' but it was not found.");
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Event failed to spawn specific Mythic Mob: " + mobInfo.getMythicId(), e);
                }
            } else {
                spawnedMob = (LivingEntity) spawnLocation.getWorld().spawnEntity(spawnLocation, mobInfo.getType());
            }

            if (spawnedMob != null) {
                if (farm.getSpawningType().equals("static")) {
                    spawnedMob.setPersistent(true);
                }

                if (!"MYTHIC".equals(mobInfo.getMobType())) {
                    applyMobAttributes(spawnedMob, mobInfo);
                }
            }
        }
    }

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

    public int getTrackedMobCount(String farmId) {
        return (int) trackedMobs.values().stream()
                .filter(trackedMob -> trackedMob.getFarmId().equals(farmId))
                .count();
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