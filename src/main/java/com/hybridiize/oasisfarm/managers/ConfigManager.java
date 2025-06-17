package com.hybridiize.oasisfarm.managers;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.farm.Farm;
import com.hybridiize.oasisfarm.farm.MobInfo;
import com.hybridiize.oasisfarm.farm.Region;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager {

    private final Oasisfarm plugin;
    private final Map<String, Farm> farms = new HashMap<>();

    public ConfigManager(Oasisfarm plugin) {
        this.plugin = plugin;
    }

    public void loadFarms() {
        farms.clear(); // Clear any old farm data before loading new data
        plugin.reloadConfig(); // Make sure we have the latest version of the config
        ConfigurationSection farmsSection = plugin.getConfig().getConfigurationSection("farms");

        if (farmsSection == null) {
            plugin.getLogger().warning("No 'farms' section found in config.yml. No farms will be loaded.");
            return;
        }

        for (String farmId : farmsSection.getKeys(false)) {
            String farmPath = "farms." + farmId;
            try {
                // --- Parse Region ---
                String worldName = farmsSection.getString(farmId + ".region.world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    throw new IllegalArgumentException("World '" + worldName + "' not found!");
                }
                Location pos1 = parseLocationString(world, farmsSection.getString(farmId + ".region.pos1"));
                Location pos2 = parseLocationString(world, farmsSection.getString(farmId + ".region.pos2"));
                Region region = new Region(pos1, pos2);

                // --- Parse Basic Farm Info ---
                int maxMobs = farmsSection.getInt(farmId + ".max-mobs");

                // --- Parse Mob List ---
                List<MobInfo> mobInfoList = new ArrayList<>();
                ConfigurationSection mobsSection = farmsSection.getConfigurationSection(farmId + ".mobs");
                if (mobsSection != null) {
                    for (String mobTypeName : mobsSection.getKeys(false)) {
                        String mobPath = farmId + ".mobs." + mobTypeName;
                        EntityType entityType = EntityType.valueOf(mobTypeName.toUpperCase());
                        double spawnChance = mobsSection.getDouble(mobTypeName + ".spawn-chance");
                        int killCooldown = mobsSection.getInt(mobTypeName + ".kill-cooldown", -1);
                        String displayName = mobsSection.getString(mobTypeName + ".display-name");
                        if (displayName != null) {
                            displayName = ChatColor.translateAlternateColorCodes('&', displayName);
                        }
                        double health = mobsSection.getDouble(mobTypeName + ".health", -1); // -1 means use default
                        List<String> rewards = mobsSection.getStringList(mobTypeName + ".rewards");

                        // Parse Equipment
                        Map<String, String> equipment = new HashMap<>();
                        ConfigurationSection equipSection = mobsSection.getConfigurationSection(mobTypeName + ".equipment");
                        if (equipSection != null) {
                            for(String slot : equipSection.getKeys(false)) {
                                equipment.put(slot.toUpperCase(), equipSection.getString(slot));
                            }
                        }

                        mobInfoList.add(new MobInfo(entityType, spawnChance, killCooldown, displayName, health, equipment, rewards));
                    }
                }

                Farm farm = new Farm(farmId, region, maxMobs, mobInfoList);
                farms.put(farmId, farm);

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load farm '" + farmId + "'. Reason: " + e.getMessage());
            }
        }
        plugin.getLogger().info("Successfully loaded " + farms.size() + " farm(s).");
    }

    private Location parseLocationString(World world, String str) {
        if (str == null) throw new IllegalArgumentException("Coordinate string is null");
        String[] parts = str.split(",");
        if (parts.length != 3) throw new IllegalArgumentException("Invalid coordinate format: " + str);
        double x = Double.parseDouble(parts[0]);
        double y = Double.parseDouble(parts[1]);
        double z = Double.parseDouble(parts[2]);
        return new Location(world, x, y, z);
    }

    public Map<String, Farm> getFarms() {
        return Collections.unmodifiableMap(farms);
    }
}