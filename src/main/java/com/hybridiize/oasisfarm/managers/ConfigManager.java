package com.hybridiize.oasisfarm.managers;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.event.EventCondition;
import com.hybridiize.oasisfarm.event.EventPhase;
import com.hybridiize.oasisfarm.event.OasisEvent;
import com.hybridiize.oasisfarm.farm.Farm;
import com.hybridiize.oasisfarm.farm.MobInfo;
import com.hybridiize.oasisfarm.farm.Region;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class ConfigManager {

    private final Oasisfarm plugin;
    private final Map<String, Farm> farms = new HashMap<>();
    private final Map<String, MobInfo> mobTemplates = new HashMap<>(); // New map for templates
    private final Map<String, OasisEvent> events = new HashMap<>();

    // New fields for the templates file
    private File mobTemplatesFile;
    private FileConfiguration mobTemplatesConfig;
    private File eventsFile;
    private FileConfiguration eventsConfig;

    public ConfigManager(Oasisfarm plugin) {
        this.plugin = plugin;
        setupTemplateFile();
        setupEventsFile();
    }

    private void setupEventsFile() {
        eventsFile = new File(plugin.getDataFolder(), "events.yml");
        if (!eventsFile.exists()) {
            plugin.saveResource("events.yml", false);
        }
        eventsConfig = YamlConfiguration.loadConfiguration(eventsFile);
    }

    public void saveMobTemplatesConfig() {
        try {
            mobTemplatesConfig.save(mobTemplatesFile);
        } catch (java.io.IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save mob-templates.yml!", e);
        }
    }

    private void setupTemplateFile() {
        mobTemplatesFile = new File(plugin.getDataFolder(), "mob-templates.yml");
        if (!mobTemplatesFile.exists()) {
            plugin.saveResource("mob-templates.yml", false);
        }
        mobTemplatesConfig = YamlConfiguration.loadConfiguration(mobTemplatesFile);
    }

    public void loadAllConfigs() {
        loadMobTemplates();
        loadFarms();
        loadEvents();
    }

    public void loadMobTemplates() {
        mobTemplates.clear();
        mobTemplatesConfig = YamlConfiguration.loadConfiguration(mobTemplatesFile); // Reload from disk

        for (String templateId : mobTemplatesConfig.getKeys(false)) {
            try {
                ConfigurationSection section = mobTemplatesConfig.getConfigurationSection(templateId);
                EntityType type = EntityType.valueOf(section.getString("type").toUpperCase());
                int killCooldown = section.getInt("kill-cooldown", -1);
                String displayName = section.getString("display-name");
                if (displayName != null) {
                    displayName = ChatColor.translateAlternateColorCodes('&', displayName);
                }
                double health = section.getDouble("health", -1);
                List<String> rewards = section.getStringList("rewards");
                String killPermission = section.getString("kill-permission");
                String broadcastKill = section.getString("broadcast-kill");

                Map<String, String> equipment = new HashMap<>();
                ConfigurationSection equipSection = section.getConfigurationSection("equipment");
                if (equipSection != null) {
                    for(String slot : equipSection.getKeys(false)) {
                        equipment.put(slot.toUpperCase(), equipSection.getString(slot));
                    }
                }

                MobInfo mobInfo = new MobInfo(templateId, type, killCooldown, displayName, health, equipment, rewards, killPermission, broadcastKill);
                mobTemplates.put(templateId, mobInfo);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load mob template '" + templateId + "'. Reason: " + e.getMessage());
            }
        }
        plugin.getLogger().info("Successfully loaded " + mobTemplates.size() + " mob template(s).");
    }

    public void loadFarms() {
        farms.clear();
        plugin.reloadConfig();
        ConfigurationSection farmsSection = plugin.getConfig().getConfigurationSection("farms");

        if (farmsSection == null) {
            plugin.getLogger().warning("No 'farms' section found in config.yml.");
            return;
        }

        for (String farmId : farmsSection.getKeys(false)) {
            try {
                ConfigurationSection farmSection = farmsSection.getConfigurationSection(farmId);
                String worldName = farmSection.getString("region.world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) throw new IllegalArgumentException("World '" + worldName + "' not found!");

                Location pos1 = parseLocationString(world, farmSection.getString("region.pos1"));
                Location pos2 = parseLocationString(world, farmSection.getString("region.pos2"));
                Region region = new Region(pos1, pos2);

                int maxMobs = farmSection.getInt("max-mobs");
                int entryCooldown = farmSection.getInt("entry-cooldown", 0);

                // New way of loading mobs
                Map<String, Double> mobs = new HashMap<>();
                ConfigurationSection mobsSection = farmSection.getConfigurationSection("mobs");
                if (mobsSection != null) {
                    for (String templateId : mobsSection.getKeys(false)) {
                        if (!mobTemplates.containsKey(templateId)) {
                            plugin.getLogger().warning("Farm '" + farmId + "' uses unknown mob template '" + templateId + "'. Skipping.");
                            continue;
                        }
                        mobs.put(templateId, mobsSection.getDouble(templateId));
                    }
                }

                Farm farm = new Farm(farmId, region, maxMobs, entryCooldown, mobs);
                farms.put(farmId, farm);

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load farm '" + farmId + "'. Reason: " + e.getMessage(), e);
            }
        }
        plugin.getLogger().info("Successfully loaded " + farms.size() + " farm(s).");
    }

    public void loadEvents() {
        events.clear();
        eventsConfig = YamlConfiguration.loadConfiguration(eventsFile); // Reload from disk
        ConfigurationSection eventsSection = eventsConfig.getConfigurationSection("events");

        if (eventsSection == null) return;

        for (String eventId : eventsSection.getKeys(false)) {
            try {
                ConfigurationSection eventSection = eventsSection.getConfigurationSection(eventId);
                String targetFarm = eventSection.getString("target-farm", "all");
                int cooldown = eventSection.getInt("cooldown", 3600);

                // Load Conditions
                List<EventCondition> conditions = new ArrayList<>();
                ConfigurationSection conditionsSection = eventSection.getConfigurationSection("conditions");
                if (conditionsSection != null) {
                    for (String key : conditionsSection.getKeys(false)) {
                        conditions.add(new EventCondition(key, conditionsSection.getString(key)));
                    }
                }

                // Load Phases
                List<EventPhase> phases = new ArrayList<>();
                List<Map<?, ?>> phasesData = eventSection.getMapList("phases");
                for (Map<?, ?> phaseData : phasesData) {
                    int duration = (int) phaseData.get("duration");
                    List<String> onStartCommands = (List<String>) phaseData.get("on-start-commands");

                    double spawnMod = 1.0;
                    boolean clearMobs = false;
                    int maxMobs = -1;
                    if (phaseData.containsKey("farm-overrides")) {
                        // We safely cast to a specific map type
                        Map<String, Object> farmOverrides = (Map<String, Object>) phaseData.get("farm-overrides");
                        // We use .toString() and then parse, which is safer
                        spawnMod = Double.parseDouble(farmOverrides.getOrDefault("spawn-interval-modifier", "1.0").toString());
                        clearMobs = Boolean.parseBoolean(farmOverrides.getOrDefault("clear-existing-mobs", "false").toString());
                        maxMobs = Integer.parseInt(farmOverrides.getOrDefault("max-mobs", "-1").toString());
                    }

                    // --- Mob Overrides ---
                    Map<String, Double> addMobs = new HashMap<>();
                    Map<String, Double> setMobs = new HashMap<>();
                    Map<String, List<String>> modifyRewards = new HashMap<>();

                    if (phaseData.containsKey("mob-overrides")) {
                        Map<String, Object> mobOverrides = (Map<String, Object>) phaseData.get("mob-overrides");

                        // Manually and safely build the addMobs map
                        if (mobOverrides.containsKey("add-mobs")) {
                            // Cast to a Map, not a ConfigurationSection
                            Map<String, Object> addMobsMap = (Map<String, Object>) mobOverrides.get("add-mobs");
                            for (Map.Entry<String, Object> entry : addMobsMap.entrySet()) {
                                addMobs.put(entry.getKey(), Double.parseDouble(entry.getValue().toString()));
                            }
                        }

                        // Manually and safely build the setMobs map
                        if (mobOverrides.containsKey("set-mobs")) {
                            Map<String, Object> setMobsMap = (Map<String, Object>) mobOverrides.get("set-mobs");
                            for (Map.Entry<String, Object> entry : setMobsMap.entrySet()) {
                                setMobs.put(entry.getKey(), Double.parseDouble(entry.getValue().toString()));
                            }
                        }

                        // Manually and safely build the modifyRewards map
                        if (mobOverrides.containsKey("modify-rewards")) {
                            Map<String, Object> modifyRewardsMap = (Map<String, Object>) mobOverrides.get("modify-rewards");
                            for (Map.Entry<String, Object> entry : modifyRewardsMap.entrySet()) {
                                // Here the value is a List of Strings, so we cast it directly
                                modifyRewards.put(entry.getKey(), (List<String>) entry.getValue());
                            }
                        }
                    }

                    phases.add(new EventPhase(duration, onStartCommands, spawnMod, clearMobs, maxMobs, addMobs, setMobs, modifyRewards));
                }

                OasisEvent event = new OasisEvent(eventId, targetFarm, cooldown, conditions, phases);
                events.put(eventId, event);

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load event '" + eventId + "'. Check formatting.", e);
            }
        }
        plugin.getLogger().info("Successfully loaded " + events.size() + " event(s).");
    }

    private Location parseLocationString(World world, String str) {
        // ... (this method is unchanged)
        if (str == null) throw new IllegalArgumentException("Coordinate string is null");
        String[] parts = str.split(",");
        if (parts.length != 3) throw new IllegalArgumentException("Invalid coordinate format: " + str);
        double x = Double.parseDouble(parts[0]);
        double y = Double.parseDouble(parts[1]);
        double z = Double.parseDouble(parts[2]);
        return new Location(world, x, y, z);
    }

    // --- NEW/UPDATED GETTERS ---
    public Map<String, Farm> getFarms() { return Collections.unmodifiableMap(farms); }
    public MobInfo getMobTemplate(String templateId) { return mobTemplates.get(templateId); }
    public FileConfiguration getMobTemplatesConfig() { return mobTemplatesConfig; }
    public Map<String, OasisEvent> getEvents() { return Collections.unmodifiableMap(events); }
    public FileConfiguration getEventsConfig() { return eventsConfig; }
}