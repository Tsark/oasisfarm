package com.hybridiize.oasisfarm.managers;

import com.hybridiize.oasisfarm.Oasisfarm;
import com.hybridiize.oasisfarm.event.EventCondition;
import com.hybridiize.oasisfarm.event.EventPhase;
import com.hybridiize.oasisfarm.event.OasisEvent;
import com.hybridiize.oasisfarm.event.v2.OasisEventV2;
import com.hybridiize.oasisfarm.event.v2.*;
import java.util.Collections;
import java.util.logging.Level;
import com.hybridiize.oasisfarm.farm.Farm;
import com.hybridiize.oasisfarm.farm.MobInfo;
import com.hybridiize.oasisfarm.farm.Region;
import com.hybridiize.oasisfarm.rewards.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.mobs.MythicMob;
import java.util.Optional;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager {

    private final Oasisfarm plugin;
    private final Map<String, Farm> farms = new HashMap<>();
    private final Map<String, MobInfo> mobTemplates = new HashMap<>();
    private final Map<String, OasisEvent> events = new HashMap<>();
    private final Map<String, OasisEventV2> eventsV2 = new HashMap<>();

    private File mobTemplatesFile;
    private FileConfiguration mobTemplatesConfig;
    private File eventsFile;
    private FileConfiguration eventsConfig;
    private File hologramsFile;
    private FileConfiguration hologramsConfig;

    public ConfigManager(Oasisfarm plugin) {
        this.plugin = plugin;
        setupTemplateFile();
        setupEventsFile();
        setupHologramsFile();
    }


    private void setupTemplateFile() {
        mobTemplatesFile = new File(plugin.getDataFolder(), "mob-templates.yml");
        if (!mobTemplatesFile.exists()) {
            plugin.saveResource("mob-templates.yml", false);
        }
        mobTemplatesConfig = YamlConfiguration.loadConfiguration(mobTemplatesFile);
    }

    private void setupEventsFile() {
        eventsFile = new File(plugin.getDataFolder(), "events.yml");
        if (!eventsFile.exists()) {
            plugin.saveResource("events.yml", false);
        }
        eventsConfig = YamlConfiguration.loadConfiguration(eventsFile);
    }

    public void loadAllConfigs() {
        loadMobTemplates();
        loadFarms();
        loadEventsV2();
        hologramsConfig = YamlConfiguration.loadConfiguration(hologramsFile);
    }

    public void loadMobTemplates() {
        mobTemplates.clear();
        mobTemplatesConfig = YamlConfiguration.loadConfiguration(mobTemplatesFile);

        for (String templateId : mobTemplatesConfig.getKeys(false)) {
            try {
                ConfigurationSection section = mobTemplatesConfig.getConfigurationSection(templateId);
                if (section == null) continue;

                EntityType type = EntityType.valueOf(section.getString("type", "ZOMBIE").toUpperCase());
                int killCooldown = section.getInt("kill-cooldown", -1);
                String displayName = section.getString("display-name");
                if (displayName != null) {
                    displayName = ChatColor.translateAlternateColorCodes('&', displayName);
                }
                double health = section.getDouble("health", -1);
                String killPermission = section.getString("kill-permission");
                String broadcastKill = section.getString("broadcast-kill");

                // MythicMobs Loading Logic
                String mobType = section.getString("mob-type", "VANILLA").toUpperCase();
                String mythicId = section.getString("mythic-id");
                int mythicLevel = section.getInt("level", 1);

                // --- ERROR HANDLING ---
                // If the type is MYTHIC, check if the mob actually exists in MythicMobs
                if (mobType.equals("MYTHIC")) {
                    if (!plugin.isMythicMobsEnabled()) {
                        plugin.getLogger().warning("Mob template '" + templateId + "' is set to MYTHIC, but MythicMobs is not installed. This mob will not spawn.");
                    } else if (mythicId == null) {
                        plugin.getLogger().warning("Mob template '" + templateId + "' is set to MYTHIC but has no 'mythic-id' defined. This mob will not spawn.");
                    } else {
                        // Check if the MythicMob exists
                        Optional<MythicMob> mm = MythicProvider.get().getMobManager().getMythicMob(mythicId);
                        if (!mm.isPresent()) {
                            plugin.getLogger().warning("Mythic Mob with ID '" + mythicId + "' (for template '" + templateId + "') was not found in MythicMobs. This mob will not spawn.");
                        }
                    }
                }

                List<RewardSet> killerRewards = parseRewardSets(section.getConfigurationSection("rewards.killer"));
                List<RewardSet> farmWideRewards = parseRewardSets(section.getConfigurationSection("rewards.farm-wide"));

                double movementSpeed = section.getDouble("attributes.movement-speed", -1.0);
                double attackDamage = section.getDouble("attributes.attack-damage", -1.0);
                List<String> potionEffects = section.getStringList("potion-effects");

                Map<String, String> equipment = new HashMap<>();
                ConfigurationSection equipSection = section.getConfigurationSection("equipment");
                if (equipSection != null) {
                    for (String slot : equipSection.getKeys(false)) {
                        equipment.put(slot.toUpperCase(), equipSection.getString(slot));
                    }
                }

                MobInfo mobInfo = new MobInfo(templateId, type, killCooldown, displayName, health, equipment, killPermission, broadcastKill, killerRewards, farmWideRewards, movementSpeed, attackDamage, potionEffects, mobType, mythicId, mythicLevel);
                mobTemplates.put(templateId, mobInfo);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load mob template '" + templateId + "'. Reason: " + e.getMessage(), e);
            }
        }
        plugin.getLogger().info("Successfully loaded " + mobTemplates.size() + " mob template(s).");
    }

    // THIS IS THE CORRECT METHOD NAME. It parses a section of SETS.
    private List<RewardSet> parseRewardSets(ConfigurationSection section) {
        List<RewardSet> rewardSets = new ArrayList<>();
        if (section == null) return rewardSets;

        for (String setKey : section.getKeys(false)) {
            ConfigurationSection setSection = section.getConfigurationSection(setKey);
            if (setSection == null) continue;
            try {
                double setChance = setSection.getDouble("chance", 1.0);
                List<Reward> individualRewards = parseIndividualRewards(setSection);
                rewardSets.add(new RewardSet(setChance, individualRewards));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse reward set '" + setKey + "'. Error: " + e.getMessage());
            }
        }
        return rewardSets;
    }

    @SuppressWarnings("unchecked")
    public void loadFarms() {
        farms.clear();
        plugin.reloadConfig();
        ConfigurationSection farmsSection = plugin.getConfig().getConfigurationSection("farms");
        if (farmsSection == null) return;
        for (String farmId : farmsSection.getKeys(false)) {
            try {
                ConfigurationSection farmSection = farmsSection.getConfigurationSection(farmId);
                if (farmSection == null) continue;
                String worldName = farmSection.getString("region.world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) throw new IllegalArgumentException("World '" + worldName + "' not found!");
                Location pos1 = parseLocationString(world, farmSection.getString("region.pos1"));
                Location pos2 = parseLocationString(world, farmSection.getString("region.pos2"));
                Region region = new Region(pos1, pos2);
                int maxMobs = farmSection.getInt("max-mobs");
                int entryCooldown = farmSection.getInt("entry-cooldown", 0);
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

    @SuppressWarnings("unchecked")
    public void loadEvents() {
        events.clear();
        eventsConfig = YamlConfiguration.loadConfiguration(eventsFile);
        ConfigurationSection eventsSection = eventsConfig.getConfigurationSection("events");

        if (eventsSection == null) return;

        for (String eventId : eventsSection.getKeys(false)) {
            try {
                ConfigurationSection eventSection = eventsSection.getConfigurationSection(eventId);
                if (eventSection == null) continue;
                String targetFarm = eventSection.getString("target-farm", "all");
                int cooldown = eventSection.getInt("cooldown", 3600);
                int eventRadius = eventSection.getInt("event-radius", 100);
                List<String> onEndCommands = eventSection.getStringList("on-end-commands");

                List<EventCondition> conditions = new ArrayList<>();
                ConfigurationSection conditionsSection = eventSection.getConfigurationSection("conditions");
                if (conditionsSection != null) {
                    for (String key : conditionsSection.getKeys(false)) {
                        conditions.add(new EventCondition(key, conditionsSection.getString(key)));
                    }
                }

                List<EventPhase> phases = new ArrayList<>();
                List<Map<?, ?>> phasesData = eventSection.getMapList("phases");
                for (Map<?, ?> phaseData : phasesData) {
                    int duration = (int) phaseData.get("duration");
                    List<String> onStartCommands = (List<String>) phaseData.get("on-start-commands");

                    List<String> onEndPhaseCommands;
                    if (phaseData.containsKey("on-end-commands")) {
                        onEndPhaseCommands = (List<String>) phaseData.get("on-end-commands");
                    } else {
                        onEndPhaseCommands = new ArrayList<>();
                    }

                    double spawnMod = 1.0;
                    boolean clearMobs = false;
                    int maxMobsOverride = -1;
                    if (phaseData.containsKey("farm-overrides")) {
                        Map<String, Object> farmOverrides = (Map<String, Object>) phaseData.get("farm-overrides");
                        spawnMod = Double.parseDouble(farmOverrides.getOrDefault("spawn-interval-modifier", "1.0").toString());
                        clearMobs = Boolean.parseBoolean(farmOverrides.getOrDefault("clear-existing-mobs", "false").toString());
                        maxMobsOverride = Integer.parseInt(farmOverrides.getOrDefault("max-mobs", "-1").toString());
                    }

                    Map<String, Double> addMobs = new HashMap<>();
                    Map<String, Double> setMobs = new HashMap<>();
                    Map<String, Map<String, List<Reward>>> modifyRewards = new HashMap<>();
                    if (phaseData.containsKey("mob-overrides")) {
                        Map<String, Object> mobOverrides = (Map<String, Object>) phaseData.get("mob-overrides");
                        if (mobOverrides.containsKey("add-mobs")) {
                            Map<String, Object> addMobsMap = (Map<String, Object>) mobOverrides.get("add-mobs");
                            for (Map.Entry<String, Object> entry : addMobsMap.entrySet()) {
                                addMobs.put(entry.getKey(), Double.parseDouble(entry.getValue().toString()));
                            }
                        }
                        if (mobOverrides.containsKey("set-mobs")) {
                            Map<String, Object> setMobsMap = (Map<String, Object>) mobOverrides.get("set-mobs");
                            for (Map.Entry<String, Object> entry : setMobsMap.entrySet()) {
                                setMobs.put(entry.getKey(), Double.parseDouble(entry.getValue().toString()));
                            }
                        }
                        if (mobOverrides.containsKey("modify-rewards")) {
                            // Cast to a Map, NOT a ConfigurationSection
                            Map<String, Object> modifyRewardsRootMap = (Map<String, Object>) mobOverrides.get("modify-rewards");
                            for (String templateId : modifyRewardsRootMap.keySet()) {
                                Map<String, List<Reward>> rewardsForTemplate = new HashMap<>();

                                // Get the inner map for the template
                                Map<String, Object> templateRewardsMap = (Map<String, Object>) modifyRewardsRootMap.get(templateId);

                                // Pass the raw List of Maps to our helper
                                if (templateRewardsMap.containsKey("killer")) {
                                    List<Map<String, Object>> killerRewardMaps = (List<Map<String, Object>>) templateRewardsMap.get("killer");
                                    rewardsForTemplate.put("killer", parseRewardMaps(killerRewardMaps));
                                }
                                if (templateRewardsMap.containsKey("farm-wide")) {
                                    List<Map<String, Object>> farmWideRewardMaps = (List<Map<String, Object>>) templateRewardsMap.get("farm-wide");
                                    rewardsForTemplate.put("farm-wide", parseRewardMaps(farmWideRewardMaps));
                                }
                                modifyRewards.put(templateId, rewardsForTemplate);
                            }
                        }
                    }
                    phases.add(new EventPhase(duration, onStartCommands, onEndPhaseCommands, spawnMod, clearMobs, maxMobsOverride, addMobs, setMobs, modifyRewards));
                }
                OasisEvent event = new OasisEvent(eventId, targetFarm, cooldown, conditions, phases, eventRadius, onEndCommands);
                events.put(eventId, event);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load event '" + eventId + "'. Check formatting.", e);
            }
        }
        plugin.getLogger().info("Successfully loaded " + events.size() + " event(s).");
    }

    // In ConfigManager.java, paste this entire block of methods

    public void loadEventsV2() {
        eventsV2.clear();
        eventsFile = new File(plugin.getDataFolder(), "events.yml");
        eventsConfig = YamlConfiguration.loadConfiguration(eventsFile);

        ConfigurationSection eventsSection = eventsConfig.getConfigurationSection("events");
        if (eventsSection == null) {
            plugin.getLogger().info("No V2 events found in events.yml.");
            return;
        }

        for (String eventId : eventsSection.getKeys(false)) {
            try {
                ConfigurationSection eventSection = eventsSection.getConfigurationSection(eventId);
                if (eventSection == null) continue;

                EventTrigger trigger = parseTrigger(eventSection.getConfigurationSection("trigger"));
                List<EventPhaseV2> phases = parsePhases(eventSection.getMapList("phases"));

                OasisEventV2 newEvent = new OasisEventV2(eventId, trigger, phases);
                eventsV2.put(eventId, newEvent);

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load V2 event '" + eventId + "'. Check formatting.", e);
            }
        }
        plugin.getLogger().info("Successfully loaded " + eventsV2.size() + " V2 event(s).");
    }

    private EventTrigger parseTrigger(ConfigurationSection section) {
        if (section == null) return new EventTrigger("AND", Collections.emptyList());
        String mode = section.getString("mode", "AND");
        List<Condition> conditions = parseConditions(section.getMapList("conditions"));
        return new EventTrigger(mode, conditions);
    }

    private PhaseProgression parseProgression(ConfigurationSection progressionSection) {
        if (progressionSection == null) {
            return new PhaseProgression("AND", Collections.emptyList());
        }
        String mode = progressionSection.getString("mode", "AND");
        List<Condition> conditions = parseConditions(progressionSection.getMapList("conditions"));
        return new PhaseProgression(mode, conditions);
    }


    private List<EventPhaseV2> parsePhases(List<Map<?, ?>> phaseDataList) {
        List<EventPhaseV2> phases = new ArrayList<>();
        if (phaseDataList == null) return phases;

        for (Map<?, ?> phaseData : phaseDataList) {
            if (phaseData == null) continue;

            String phaseId = "unknown_phase";
            if (phaseData.get("phase_id") != null) {
                phaseId = String.valueOf(phaseData.get("phase_id"));
            }

            List<PhaseAction> actions = parseActions((List<?>) phaseData.get("actions"));

            // Safely get the progression section
            PhaseProgression progression = null;
            Object rawProgression = phaseData.get("progression");
            if (rawProgression instanceof ConfigurationSection) {
                progression = parseProgression((ConfigurationSection) rawProgression);
            } else if (rawProgression instanceof Map) {
                // This is a fallback, but the main fix is handling ConfigurationSection
                FileConfiguration tempConfig = new YamlConfiguration();
                tempConfig.createSection("temp", (Map<?, ?>) rawProgression);
                progression = parseProgression(tempConfig.getConfigurationSection("temp"));
            }

            phases.add(new EventPhaseV2(phaseId, actions, progression));
        }
        return phases;
    }


    private List<Condition> parseConditions(List<?> conditionDataList) {
        List<Condition> conditions = new ArrayList<>();
        if (conditionDataList == null) return conditions;

        for (Object item : conditionDataList) {
            if (item instanceof Map) {
                Map<?, ?> conditionData = (Map<?, ?>) item;
                String type = String.valueOf(conditionData.get("type"));
                String value = String.valueOf(conditionData.get("value"));

                Map<String, String> properties = new HashMap<>();
                for (Map.Entry<?, ?> entry : conditionData.entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    if (!key.equals("type") && !key.equals("value")) {
                        properties.put(key, String.valueOf(entry.getValue()));
                    }
                }
                conditions.add(new Condition(type, value, properties));
            }
        }
        return conditions;
    }

    private List<PhaseAction> parseActions(List<?> actionDataList) {
        List<PhaseAction> actions = new ArrayList<>();
        if (actionDataList == null) return actions;

        for (Object item : actionDataList) {
            if (item instanceof Map) {
                Map<?, ?> actionData = (Map<?, ?>) item;
                Object rawType = actionData.get("type");
                if (rawType instanceof String) {
                    String type = (String) rawType;
                    switch (type.toUpperCase()) {
                        case "BROADCAST":
                            actions.add(new com.hybridiize.oasisfarm.event.v2.action.BroadcastAction(actionData));
                            break;
                        case "COMMAND":
                            actions.add(new com.hybridiize.oasisfarm.event.v2.action.CommandAction(actionData));
                            break;
                        case "SPAWN_ONCE":
                            actions.add(new com.hybridiize.oasisfarm.event.v2.action.SpawnOnceAction(actionData));
                            break;
                    }
                }
            }
        }
        return actions;
    }

    private void setupHologramsFile() {
        hologramsFile = new File(plugin.getDataFolder(), "holograms.yml");
        if (!hologramsFile.exists()) {
            plugin.saveResource("holograms.yml", false);
        }
        hologramsConfig = YamlConfiguration.loadConfiguration(hologramsFile);
    }


    // The method signature and return type have changed!
    private List<RewardSet> parseRewards(ConfigurationSection section) {
        List<RewardSet> rewardSets = new ArrayList<>();
        if (section == null) {
            return rewardSets;
        }
        System.out.println("[DEBUG] Parsing Reward Sets from section: " + section.getCurrentPath()); // <-- ADD

        for (String setKey : section.getKeys(false)) { // e.g., "common_loot"
            ConfigurationSection setSection = section.getConfigurationSection(setKey);
            if (setSection == null) continue;
            System.out.println("[DEBUG] -> Found Reward Set key: " + setKey); // <-- ADD

            try {
                double setChance = setSection.getDouble("chance", 1.0);
                ConfigurationSection individualRewardsSection = setSection.getConfigurationSection("rewards");

                List<Reward> individualRewards = parseIndividualRewards(individualRewardsSection);

                System.out.println("[DEBUG] --> Set '" + setKey + "' has chance " + setChance + " and contains " + individualRewards.size() + " individual rewards."); // <-- ADD

                rewardSets.add(new RewardSet(setChance, individualRewards));

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse a reward set ('" + setKey + "') in section " + section.getCurrentPath() + ". Error: " + e.getMessage());
            }
        }
        return rewardSets;
    }

    // This helper method is responsible for parsing the list of rewards INSIDE a set.

    private List<Reward> parseIndividualRewards(ConfigurationSection section) {
        List<Reward> rewards = new ArrayList<>();
        if (section == null) return rewards;

        List<Map<?, ?>> rewardMapList = section.getMapList("rewards");

        for (Map<?, ?> rewardData : rewardMapList) {
            try {
                String type = rewardData.containsKey("type") ? rewardData.get("type").toString().toUpperCase() : "";
                double chance = rewardData.containsKey("chance") ? Double.parseDouble(rewardData.get("chance").toString()) : 1.0;

                switch (type) {
                    case "MONEY":
                        rewards.add(new MoneyReward(chance, Double.parseDouble(rewardData.get("amount").toString())));
                        break;
                    case "EXP":
                        rewards.add(new ExpReward(chance, Integer.parseInt(rewardData.get("amount").toString())));
                        break;
                    case "MESSAGE":
                        rewards.add(new MessageReward(chance, rewardData.get("message").toString()));
                        break;
                    case "COMMAND":
                        rewards.add(new CommandReward(chance, rewardData.get("command").toString()));
                        break;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse an individual reward entry in section " + section.getCurrentPath() + ". Error: " + e.getMessage());
            }
        }
        return rewards;
    }

    // This helper method MUST exist in ConfigManager.java
    private List<Reward> parseRewardMaps(List<Map<String, Object>> rewardMaps) {
        List<Reward> rewards = new ArrayList<>();
        if (rewardMaps == null) return rewards;

        for (Map<String, Object> rewardData : rewardMaps) {
            String type = rewardData.getOrDefault("type", "").toString().toUpperCase();
            double chance = Double.parseDouble(rewardData.getOrDefault("chance", "1.0").toString());

            switch (type) {
                case "MONEY":
                    rewards.add(new MoneyReward(chance, Double.parseDouble(rewardData.get("amount").toString())));
                    break;
                case "EXP":
                    rewards.add(new ExpReward(chance, Integer.parseInt(rewardData.get("amount").toString())));
                    break;
                case "MESSAGE":
                    rewards.add(new MessageReward(chance, rewardData.get("message").toString()));
                    break;
                case "COMMAND":
                    rewards.add(new CommandReward(chance, rewardData.get("command").toString()));
                    break;
            }
        }
        return rewards;
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

    public void saveMobTemplatesConfig() {
        try {
            mobTemplatesConfig.save(mobTemplatesFile);
        } catch (java.io.IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save mob-templates.yml!", e);
        }
    }

    public OasisEventV2 getEventV2(String eventId) {
        return eventsV2.get(eventId);
    }

    public List<String> getPossibleEventsForFarm(String farmId) {
        // This assumes you have updated your config.yml as per Step 1.2
        return plugin.getConfig().getStringList("farms." + farmId + ".possible-events");
    }
    public FileConfiguration getMobTemplatesConfig() { return mobTemplatesConfig; }
    public Map<String, Farm> getFarms() { return Collections.unmodifiableMap(farms); }
    public MobInfo getMobTemplate(String templateId) { return mobTemplates.get(templateId); }
    public Map<String, OasisEvent> getEvents() { return Collections.unmodifiableMap(events); }
    public FileConfiguration getEventsConfig() { return eventsConfig; }
    public FileConfiguration getHologramsConfig() { return hologramsConfig; }
}