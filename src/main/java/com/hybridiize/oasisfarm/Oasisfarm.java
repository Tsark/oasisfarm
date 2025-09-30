package com.hybridiize.oasisfarm;

import com.hybridiize.oasisfarm.commands.CommandManager;
import com.hybridiize.oasisfarm.listeners.MobKillListener;
import com.hybridiize.oasisfarm.listeners.MythicMobListener;
import com.hybridiize.oasisfarm.listeners.PlayerMoveListener;
import com.hybridiize.oasisfarm.listeners.WandListener;
import com.hybridiize.oasisfarm.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Oasisfarm extends JavaPlugin {

    private static Oasisfarm instance;

    // Managers
    private ConfigManager configManager;
    private ConditionManager conditionManager;
    private EconomyManager economyManager;
    private EventDataManager eventDataManager;
    private EventManager eventManager;
    private FarmDataManager farmDataManager;
    private FarmManager farmManager;
    private HologramManager hologramManager;
    private PendingConfirmationManager confirmationManager;
    private RewardManager rewardManager;
    private SelectionManager selectionManager;
    private boolean mythicMobsEnabled = false;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // Initialize all managers
        this.configManager = new ConfigManager(this);
        this.conditionManager = new ConditionManager(this);
        this.economyManager = new EconomyManager(this);
        this.eventDataManager = new EventDataManager(this);
        this.farmDataManager = new FarmDataManager(this);
        this.selectionManager = new SelectionManager();
        this.confirmationManager = new PendingConfirmationManager();
        this.rewardManager = new RewardManager(this);
        this.farmManager = new FarmManager(this);
        this.eventManager = new EventManager(this);
        this.hologramManager = new HologramManager(this);

        // MythicMobs Integration Check
        if (getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            this.mythicMobsEnabled = true;
            getLogger().info("Successfully hooked into MythicMobs! Mythic Mob spawning is enabled.");
        } else {
            this.mythicMobsEnabled = false;
            getLogger().info("MythicMobs not found. Spawning will be limited to vanilla mobs.");
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("Successfully hooked into PlaceholderAPI!");
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
        }

        // Load all data from configuration files
        this.configManager.loadAllConfigs();

        // Register commands
        getCommand("oasisfarm").setExecutor(new CommandManager(selectionManager, confirmationManager));

        // Register event listeners
        getServer().getPluginManager().registerEvents(new WandListener(selectionManager), this);
        getServer().getPluginManager().registerEvents(new MobKillListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        if (mythicMobsEnabled) {
            getServer().getPluginManager().registerEvents(new MythicMobListener(this), this);
        }

        getLogger().info("OasisFarm has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Stop all events gracefully
        if (eventManager != null) {
            eventManager.stopAllEvents();
            getLogger().info("All active events have been stopped.");
        }

        // Remove all plugin-spawned mobs from the world
        if (farmManager != null) {
            getLogger().info("Removing all tracked OasisFarm mobs...");
            Set<UUID> mobIds = new HashSet<>(farmManager.getTrackedMobIds());
            int removedCount = 0;
            for (UUID mobId : mobIds) {
                Entity mob = Bukkit.getEntity(mobId);
                if (mob != null) {
                    mob.remove();
                    removedCount++;
                }
            }
            getLogger().info("Successfully removed " + removedCount + " mobs.");
        }

        // Remove all holograms
        if (hologramManager != null) {
            hologramManager.removeAllHolograms();
        }

        // Save all persistent data to disk
        if (farmDataManager != null) {
            farmDataManager.saveData();
            getLogger().info("Saved farm data.");
        }
        if (eventDataManager != null) {
            eventDataManager.saveData();
            getLogger().info("Saved event data.");
        }

        getLogger().info("OasisFarm has been disabled.");
    }

    // --- GETTERS ---

    public static Oasisfarm getInstance() {
        return instance;
    }

    public ConditionManager getConditionManager() { return conditionManager; }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public EventDataManager getEventDataManager() {
        return eventDataManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public FarmDataManager getFarmDataManager() {
        return farmDataManager;
    }

    public FarmManager getFarmManager() {
        return farmManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public boolean isMythicMobsEnabled() {
        return mythicMobsEnabled;
    }
}