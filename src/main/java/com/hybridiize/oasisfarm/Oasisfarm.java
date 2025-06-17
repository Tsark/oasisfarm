package com.hybridiize.oasisfarm;

import com.hybridiize.oasisfarm.commands.CommandManager;
import com.hybridiize.oasisfarm.listeners.MobKillListener;
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
    private ConfigManager configManager;
    private SelectionManager selectionManager;
    private FarmManager farmManager;
    private HologramManager hologramManager;
    private PendingConfirmationManager confirmationManager;
    private EventManager eventManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.selectionManager = new SelectionManager();
        this.farmManager = new FarmManager(this);
        this.hologramManager = new HologramManager(this);
        this.confirmationManager = new PendingConfirmationManager();
        this.eventManager = new EventManager(this);

        // Load data from config
        this.configManager.loadAllConfigs();

        // Register commands - THIS IS THE FIX FOR THE SECOND ERROR
        getCommand("oasisfarm").setExecutor(new CommandManager(selectionManager, confirmationManager));

        // Register event listeners
        getServer().getPluginManager().registerEvents(new WandListener(selectionManager), this);
        getServer().getPluginManager().registerEvents(new MobKillListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);

        getLogger().info("Oasisfarm has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (hologramManager != null) {
            hologramManager.removeAllHolograms();
        }

        if (farmManager != null) {
            getLogger().info("Removing all tracked OasisFarm mobs...");
            // We get the set of UUIDs directly from the keySet now
            Set<UUID> mobIds = new HashSet<>(farmManager.getTrackedMobIds()); // Use a copy to avoid concurrent modification issues
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

        getLogger().info("Oasisfarm has been disabled.");
    }

    public static Oasisfarm getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
    public FarmManager getFarmManager() {
        return farmManager;
    }
    public HologramManager getHologramManager() {
        return hologramManager;
    }
    public EventManager getEventManager() { return eventManager; }
}